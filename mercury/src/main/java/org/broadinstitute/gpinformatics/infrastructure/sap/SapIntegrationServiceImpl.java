package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOnPriceAdjustment;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderPriceAdjustment;
import org.broadinstitute.gpinformatics.athena.entity.orders.SapOrderDetail;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.sap.entity.Condition;
import org.broadinstitute.sap.entity.OrderCalculatedValues;
import org.broadinstitute.sap.entity.OrderCriteria;
import org.broadinstitute.sap.entity.SAPDeliveryDocument;
import org.broadinstitute.sap.entity.SAPDeliveryItem;
import org.broadinstitute.sap.entity.SAPMaterial;
import org.broadinstitute.sap.entity.SAPOrder;
import org.broadinstitute.sap.entity.SAPOrderItem;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Impl
public class SapIntegrationServiceImpl implements SapIntegrationService {

    @Inject
    private SapConfig sapConfig;

    @Inject
    private QuoteService quoteService;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private PriceListCache priceListCache;

    private SapIntegrationClientImpl wrappedClient;

    private final static Log log = LogFactory.getLog(SapIntegrationServiceImpl.class);

    public SapIntegrationServiceImpl() {
    }

    public SapIntegrationServiceImpl(SapConfig sapConfigIn) {
        if(sapConfig == null) {
            this.sapConfig = sapConfigIn;
        }
    }

    /**
     * Helper method to initialize the common client this service will utilize in order to communicate to SAP
     */
    protected void initializeClient() {

        SapIntegrationClientImpl.SAPEnvironment environment;

        switch (sapConfig.getExternalDeployment()) {
        case PROD:
            environment = SapIntegrationClientImpl.SAPEnvironment.PRODUCTION;
            break;
        case DEV:
            environment = SapIntegrationClientImpl.SAPEnvironment.DEV;
            break;
        case TEST:
            environment = SapIntegrationClientImpl.SAPEnvironment.DEV_400;
            break;
        case QA:
        default:
            environment = SapIntegrationClientImpl.SAPEnvironment.QA;
            break;
        }

        log.debug("Config environment is: " + sapConfig.getExternalDeployment().name());
        log.debug("Sending to SAP environment of: " + environment.name());

        wrappedClient = new SapIntegrationClientImpl(sapConfig.getLogin(), sapConfig.getPassword(),
                environment);
    }

    /**
     * Getter for the common sap client utilized to communicate to SAP
     * @return
     */
    private SapIntegrationClientImpl getClient() {
        if(wrappedClient == null) {
            initializeClient();
        }
        return wrappedClient;
    }

    @Override
    public String createOrder(ProductOrder placedOrder) throws SAPIntegrationException {

        SAPOrder newOrder = initializeSAPOrder(placedOrder);

        return getClient().createSAPOrder(newOrder);
    }

    @Override
    public void updateOrder(ProductOrder placedOrder) throws SAPIntegrationException {

        SAPOrder newOrder = initializeSAPOrder(placedOrder);

        if(placedOrder.getSapOrderNumber() == null) {
            throw new SAPIntegrationException("Cannot update an order in SAP since this product order does not have "
                                              + "an SAP Order number with which to reference an order.");
        }
        newOrder.setSapOrderNumber(placedOrder.getSapOrderNumber());

        getClient().updateSAPOrder(newOrder);
    }

    /**
     * Helper method to compile the order object which will be transmitted to SAP for either creation up to be
     * updated
     * @param placedOrder The ProductOrder from which a JAXB representation of an SAP order will be created
     * @return JAXB representation of a Product Order
     * @throws SAPIntegrationException
     */
    protected SAPOrder initializeSAPOrder(ProductOrder placedOrder) throws SAPIntegrationException {

        ProductOrder orderToUpdate = (placedOrder.isChildOrder() && placedOrder.getParentOrder().getSapOrderNumber().equals(placedOrder.getSapOrderNumber()))?placedOrder.getParentOrder():placedOrder;

        Quote foundQuote = null;
        try {
            foundQuote = quoteService.getQuoteByAlphaId(orderToUpdate.getQuoteId());
        } catch (QuoteServerException | QuoteNotFoundException e) {
            throw new SAPIntegrationException("Unable to get information for the Quote from the quote server", e);
        }

        SAPOrder newOrder =
                new SAPOrder(SapIntegrationClientImpl.SystemIdentifier.MERCURY, determineCompanyCode(orderToUpdate),
                        orderToUpdate.getQuoteId(), bspUserList.getUserFullName(orderToUpdate.getCreatedBy()),
                        placedOrder.isPriorToSAP1_5());

        newOrder.setExternalOrderNumber(orderToUpdate.getJiraTicketKey());

        FundingLevel fundingLevel = foundQuote.getFirstRelevantFundingLevel();

        if (fundingLevel == null) {
            // Too many funding sources to allow this to work with SAP.  Keep using the Quote Server as the definition
            // of funding
            throw new SAPIntegrationException(
                    "Unable to continue with SAP.  The associated quote has either too few or too many funding sources");
        }

        for (Funding funding:fundingLevel.getFunding()) {
            if (funding.getFundingType().equals(Funding.PURCHASE_ORDER)) {
                String customerNumber = findCustomer(determineCompanyCode(orderToUpdate), fundingLevel);

                newOrder.setSapCustomerNumber(customerNumber);
                newOrder.setFundingSource(funding.getPurchaseOrderNumber(), SAPOrder.FundingType.PURCHASE_ORDER);
            } else {
                newOrder.setFundingSource(funding.getFundsReservationNumber(), SAPOrder.FundingType.FUNDS_RESERVATION);
            }

            /*
            This really only needs to loop once since the information that is retrieved will be the same for each
            funding instance under fundingLevel
             */
            break;
        }

        newOrder.setResearchProjectNumber(orderToUpdate.getResearchProject().getJiraTicketKey());

        Product primaryProduct = placedOrder.getProduct();
        newOrder.addOrderItem(getOrderItem(placedOrder, primaryProduct, foundQuote, 0));

        for (ProductOrderAddOn addon : placedOrder.getAddOns()) {
            newOrder.addOrderItem(getOrderItem(placedOrder, addon.getAddOn(), foundQuote, 0));
        }

        return newOrder;
    }

    /**
     * Helper method to extract the sub elements of a Product order to represent a product and the quantity of which
     * is expected to be charged
     * @param placedOrder Order from which the quantities are defined
     * @param product Product that is to be eventually charged when work on the product order is completed
     * @param quote
     * @param additionalSampleCount
     * @return JAXB sub element of the SAP order to represent the Product that will be charged and the quantity that
     * is expected of it.
     */
    protected SAPOrderItem getOrderItem(ProductOrder placedOrder, Product product, Quote quote,
                                        int additionalSampleCount) throws SAPIntegrationException {
        try {
            //TODO SGM:  This has to change!!!
            String price = priceListCache.getEffectivePrice(product.getPrimaryPriceItem(), quote);

            final SAPOrderItem sapOrderItem = new SAPOrderItem(product.getPartNumber(),
                    getSampleCount(placedOrder, product, additionalSampleCount));

            if(placedOrder.isPriorToSAP1_5()) {
                sapOrderItem.addCondition(Condition.MATERIAL_PRICE, new BigDecimal(price));
            } else {
                if(placedOrder.getProduct().equals(product)) {
                    for (ProductOrderPriceAdjustment productOrderPriceAdjustment : placedOrder.getQuotePriceMatchAdjustments()) {
                        sapOrderItem.addCondition(productOrderPriceAdjustment.getPriceAdjustmentCondition(), productOrderPriceAdjustment.getAdjustmentValue());
                    }

                }
            }

            return sapOrderItem;

        } catch (InvalidProductException e) {
            throw new SAPIntegrationException("For " + product.getPartNumber() + " " + e.getMessage());
        }
    }

    protected SAPOrderItem getOrderItem(ProductOrder placedOrder, Product product, int additionalSampleCount) {

            return new SAPOrderItem(product.getPartNumber(), getSampleCount(placedOrder, product, additionalSampleCount));
    }


    public static int getSampleCount(ProductOrder placedOrder, Product product) {

        return getSampleCount(placedOrder, product, 0);
    }

    public static int getSampleCount(ProductOrder placedOrder, Product product, int additionalSampleCount) {
        int sampleCount = 0;

        if (product.getSupportsNumberOfLanes() && placedOrder.getLaneCount() > 0) {
            sampleCount += placedOrder.getLaneCount();
        } else {
            ProductOrder targetSapPdo = ProductOrder.getTargetSAPProductOrder(placedOrder);
            sampleCount += targetSapPdo.getTotalNonAbandonedCount(ProductOrder.CountAggregation.SHARE_SAP_ORDER_AND_BILL_READY) + additionalSampleCount;
        }
        return sampleCount;
    }

    @Override
    public String findCustomer(SapIntegrationClientImpl.SAPCompanyConfiguration companyCode, FundingLevel fundingLevel) throws SAPIntegrationException {

        String customerNumber = null;
        if (fundingLevel == null) {
            // Too many funding sources to allow this to work with SAP.  Keep using the Quote Server as the definition
            // of funding
            throw new SAPIntegrationException(
                    "Unable to continue with SAP.  The associated quote has either too few or too many funding sources");
        } else {
            for (Funding funding : fundingLevel.getFunding()) {

                if (funding.getFundingType().equals(Funding.PURCHASE_ORDER)) {
                    try {
                        customerNumber =
                                getClient().findCustomerNumber(funding.getPurchaseOrderContact(), companyCode);
                    } catch (SAPIntegrationException e) {
                        if (e.getMessage().equals(SapIntegrationClientImpl.MISSING_CUSTOMER_RESULT)) {
                            throw new SAPIntegrationException(
                                    "Your order cannot be placed in SAP because the email address "
                                    + "specified on the Quote is not attached to any SAP Customer account.\n"
                                    + "An email has been sent to Amber Kennedy in AR to initiate "
                                    + "this SAP Customer Creation process. Please contact Amber "
                                    + "Kennedy to follow this up.\n"
                                    + "Once the Customer has been created in SAP you will need "
                                    + "to resubmit this order to ensure that your work is "
                                    + "properly processed.\n"
                                    + "For further questions please contact Mercury support");
                        } else if (e.getMessage().equals(SapIntegrationClientImpl.TOO_MANY_ACCOUNTS_RESULT)) {
                            throw new SAPIntegrationException(
                                    "Your order cannot be placed because the email address specified "
                                    + "on the Quote is associated with more than 1 SAP Customer account.\n"
                                    + "An email has been sent to Amber Kennedy in AR to initiate "
                                    + "this SAP Customer Creation process. Please contact Amber "
                                    + "Kennedy to follow this up.\n"
                                    + "Once the SAP Customer account has been corrected you will "
                                    + "need to resubmit this order to ensure that your work is "
                                    + "properly processed.\n"
                                    + "For further questions please contact Mercury support");
                        } else {
                            throw e;
                        }
                    }
                }
                /*
                This really only needs to loop once since the information that is retrieved will be the same for each
                funding instance under fundingLevel
                */

                break;
            }
        }

        return customerNumber;
    }

    @Override
    public String billOrder(QuoteImportItem quoteItemForBilling, BigDecimal quantityOverride)
            throws SAPIntegrationException {

        SAPDeliveryDocument deliveryDocument =
                new SAPDeliveryDocument(SapIntegrationClientImpl.SystemIdentifier.MERCURY,
                        determineCompanyCode(quoteItemForBilling.getProductOrder()),
                        quoteItemForBilling.getProductOrder().getSapOrderNumber());

        SAPDeliveryItem lineItem =
                new SAPDeliveryItem(quoteItemForBilling.getProduct().getPartNumber(),
                        (quantityOverride == null)?new BigDecimal(quoteItemForBilling.getQuantityForSAP()):quantityOverride);
        deliveryDocument.addDeliveryItem(lineItem);


        return getClient().createDeliveryDocument(deliveryDocument);
    }

    @Override
    public void createProductInSAP(Product product) throws SAPIntegrationException {
        SAPMaterial newMaterial = initializeSapMaterialObject(product);

        getClient().createMaterial(newMaterial);

        if(product.hasExternalCounterpart()) {
            newMaterial.setCompanyCode(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES);
            newMaterial.setMaterialName(product.getExternalProductName());
            getClient().createMaterial(newMaterial);
        }

    }

    @NotNull
    protected SAPMaterial initializeSapMaterialObject(Product product) {
        SAPMaterial newMaterial = new SAPMaterial(product.getPartNumber(),
                SapIntegrationClientImpl.SystemIdentifier.MERCURY, product.getAvailabilityDate(),
                product.getAvailabilityDate());
        newMaterial.setCompanyCode(
//                product.isExternalProduct() ? SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES :
                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        newMaterial.setMaterialName(product.getProductName());
        newMaterial.setDescription(product.getDescription());
        newMaterial.setDeliverables(product.getDeliverables());
        newMaterial.setInputRequirements(product.getInputRequirements());
        newMaterial.setBaseUnitOfMeasure("EA");
        BigDecimal minimumOrderQuantity = product.getMinimumOrderSize() != null?new BigDecimal(product.getMinimumOrderSize()):BigDecimal.ONE;
        newMaterial.setMinimumOrderQuantity(minimumOrderQuantity);

        newMaterial.setStatus(SAPMaterial.MaterialStatus.ENABLED);

        return newMaterial;
    }

    @Override
    public void changeProductInSAP(Product product) throws SAPIntegrationException {

        SAPMaterial existingMaterial = initializeSapMaterialObject(product);

        getClient().changeMaterialDetails(existingMaterial);

        if(product.hasExternalCounterpart()) {
            existingMaterial.setCompanyCode(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES);
            existingMaterial.setMaterialName(product.getExternalProductName());
            getClient().changeMaterialDetails(existingMaterial);
        }

    }

    @Override
    public Set<SAPMaterial> findProductsInSap() throws SAPIntegrationException {

        Set<SAPMaterial> materials = new HashSet<>();
        Set<SAPMaterial> researchMaterials =
                getClient().findMaterials(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getPlant(),
                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization());
        Set<SAPMaterial> externalMaterials =
                getClient().findMaterials(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getPlant(),
                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES
                                .getSalesOrganization());
        materials.addAll(researchMaterials);
        materials.addAll(externalMaterials);
        return materials;
    }

    @Override
    public OrderCalculatedValues calculateOpenOrderValues(ProductOrder productOrder, int addedSampleCount) throws SAPIntegrationException {
        OrderCriteria potentialOrderCriteria = null;
            potentialOrderCriteria = generateOrderCriteria(productOrder, addedSampleCount);
        return getClient().calculateOrderValues(productOrder.getQuoteId(),
                SapIntegrationClientImpl.SystemIdentifier.MERCURY, potentialOrderCriteria);
    }

    protected OrderCriteria generateOrderCriteria(ProductOrder productOrder) throws SAPIntegrationException {
        return generateOrderCriteria(productOrder, 0);
    }

    protected OrderCriteria generateOrderCriteria(ProductOrder productOrder, int addedSampleCount) throws SAPIntegrationException {


        final Set<SAPOrderItem> sapOrderItems = new HashSet<>();
        final Map<Condition, String> conditionStringMap = Collections.emptyMap();
        final SAPOrderItem orderItem = getOrderItem(productOrder, productOrder.getProduct(), addedSampleCount);
        for (ProductOrderPriceAdjustment productOrderPriceAdjustment : productOrder.getQuotePriceMatchAdjustments()) {
            orderItem.addCondition(productOrderPriceAdjustment.getPriceAdjustmentCondition(),
                    productOrderPriceAdjustment.getAdjustmentValue());
        }

        sapOrderItems.add(orderItem);

        for (ProductOrderAddOn productOrderAddOn : productOrder.getAddOns()) {
            final SAPOrderItem orderSubItem = getOrderItem(productOrder, productOrderAddOn.getAddOn(), addedSampleCount);
            for (ProductOrderAddOnPriceAdjustment productOrderAddOnPriceAdjustment : productOrderAddOn
                    .getQuotePriceAdjustments()) {
                orderItem.addCondition(productOrderAddOnPriceAdjustment.getPriceAdjustmentCondition(),
                        productOrderAddOnPriceAdjustment.getAdjustmentValue());
            }

            sapOrderItems.add(orderSubItem);
        }

        Quote foundQuote = null;
        try {
            foundQuote = quoteService.getQuoteByAlphaId(productOrder.getQuoteId());
        } catch (QuoteServerException | QuoteNotFoundException e) {
            throw new SAPIntegrationException("Unable to get information for the Quote from the quote server", e);
        }
        FundingLevel fundingLevel = foundQuote.getFirstRelevantFundingLevel();

        if (fundingLevel == null) {
            // Too many funding sources to allow this to work with SAP.  Keep using the Quote Server as the definition
            // of funding
            throw new SAPIntegrationException(
                    "Unable to continue with SAP.  The associated quote has either too few or too many funding sources");
        }

        String customerNumber = null;
        if(fundingLevel.getFunding().size() >1) {
            throw new SAPIntegrationException("This order is ineligible to save to SAP since there are multiple "
                                              + "funding sources associated with the given quote " +
                                              productOrder.getQuoteId());
        }
        for (Funding funding : fundingLevel.getFunding()) {
            if (funding.getFundingType().equals(Funding.PURCHASE_ORDER)) {
                customerNumber = findCustomer(determineCompanyCode(productOrder), fundingLevel);
            } else {
                customerNumber = SapIntegrationClientImpl.INTERNAL_ORDER_CUSTOMER_NUMBER;
            }
        }

        return new OrderCriteria(customerNumber,
                //This will probably change with the implementation of the product/pricing caching and/or selling a product from both research and external
                determineCompanyCode(productOrder), sapOrderItems);
    }

    /**
     * Helper method to figure out which company code to associate with a given product order
     * @param companyProductOrder Product Order from which the company code is to be determined
     * @return an indicator that represents one of the configured companies within SAP
     */
    public static SapIntegrationClientImpl.SAPCompanyConfiguration determineCompanyCode(ProductOrder companyProductOrder)
            throws SAPIntegrationException {
        SapIntegrationClientImpl.SAPCompanyConfiguration companyCode =
                getSapCompanyConfigurationForProduct(companyProductOrder.getProduct());

        final SapOrderDetail latestSapOrderDetail = companyProductOrder.latestSapOrderDetail();
        if(latestSapOrderDetail != null && latestSapOrderDetail.getCompanyCode()!= null
           && !latestSapOrderDetail.getCompanyCode().equals(companyCode.getCompanyCode())) {
            throw new SAPIntegrationException("Unable to update the order in SAP.  "
                                              + "This combination of Product and Order is attempting to change the "
                                              + "company code to which this order will be associated.");
        }

        return companyCode;
    }

    @NotNull
    public static SapIntegrationClientImpl.SAPCompanyConfiguration getSapCompanyConfigurationForProduct(Product product) {
        SapIntegrationClientImpl.SAPCompanyConfiguration companyCode = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;
        if (product.isExternalProduct()) {
            companyCode = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES;
        }
        return companyCode;
    }

    protected void setQuoteService(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    protected void setBspUserList(BSPUserList bspUserList) {
        this.bspUserList = bspUserList;
    }

    protected void setWrappedClient(SapIntegrationClientImpl wrappedClient) {
        this.wrappedClient = wrappedClient;
    }

    protected void setPriceListCache(PriceListCache priceListCache) {
        this.priceListCache = priceListCache;
    }
}
