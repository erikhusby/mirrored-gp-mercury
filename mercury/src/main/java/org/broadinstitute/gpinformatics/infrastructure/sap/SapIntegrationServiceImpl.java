package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.PriceAdjustment;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOnPriceAdjustment;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderPriceAdjustment;
import org.broadinstitute.gpinformatics.athena.entity.orders.SapOrderDetail;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.sap.entity.Condition;
import org.broadinstitute.sap.entity.DeliveryCondition;
import org.broadinstitute.sap.entity.OrderCalculatedValues;
import org.broadinstitute.sap.entity.OrderCriteria;
import org.broadinstitute.sap.entity.SAPDeliveryDocument;
import org.broadinstitute.sap.entity.SAPDeliveryItem;
import org.broadinstitute.sap.entity.SAPOrder;
import org.broadinstitute.sap.entity.SAPOrderItem;
import org.broadinstitute.sap.entity.material.SAPChangeMaterial;
import org.broadinstitute.sap.entity.material.SAPMaterial;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.jetbrains.annotations.NotNull;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceImpl.SSF_PRICE_LIST_NAME;

@Dependent
@Default
public class SapIntegrationServiceImpl implements SapIntegrationService {

    private SapConfig sapConfig;

    private QuoteService quoteService;

    private BSPUserList bspUserList;

    private PriceListCache priceListCache;

    private SAPProductPriceCache productPriceCache;

    private SAPAccessControlEjb accessControlEjb;

    private SapIntegrationClientImpl wrappedClient;

    private final static Log log = LogFactory.getLog(SapIntegrationServiceImpl.class);

    public SapIntegrationServiceImpl() {
    }

    @Inject
    public SapIntegrationServiceImpl(SapConfig sapConfigIn, QuoteService quoteService, BSPUserList bspUserList,
                                     PriceListCache priceListCache, SAPProductPriceCache productPriceCache,
                                     SAPAccessControlEjb accessControlEjb) {
        if(sapConfig == null) {
            this.sapConfig = sapConfigIn;
        }
        this.quoteService = quoteService;
        this.bspUserList = bspUserList;
        this.priceListCache = priceListCache;
        this.productPriceCache = productPriceCache;
        this.accessControlEjb = accessControlEjb;
    }

    /**
     * Helper method to initialize the common client this service will utilize in order to communicate to SAP
     */
    private void initializeClient() {

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
            case RC:
                environment = SapIntegrationClientImpl.SAPEnvironment.QA_400;
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

        SAPOrder newOrder = initializeSAPOrder(placedOrder, true, false);

        return getClient().createSAPOrder(newOrder);
    }

    @Override
    public String createOrderWithQuote(ProductOrder placedOrder) throws SAPIntegrationException {
        throw new SAPIntegrationException("SAP Quotes are not available at this time");
    }

    @Override
    public void updateOrder(ProductOrder placedOrder, boolean closingOrder) throws SAPIntegrationException {

        SAPOrder newOrder = initializeSAPOrder(placedOrder, false, closingOrder);

        if(placedOrder.getSapOrderNumber() == null) {
            throw new SAPIntegrationException("Cannot update an order in SAP since this product order does not have "
                                              + "an SAP Order number with which to reference an order.");
        }
        newOrder.setSapOrderNumber(placedOrder.getSapOrderNumber());

        getClient().updateSAPOrder(newOrder);
    }

    @Override
    public void updateOrderWithQuote(ProductOrder placedOrder, boolean closingOrder) throws SAPIntegrationException {
        throw new SAPIntegrationException("SAP Quotes are not available at this time");
    }

    /**
     * Helper method to compile the order object which will be transmitted to SAP for either creation up to be
     * updated
     * @param placedOrder The ProductOrder from which a JAXB representation of an SAP order will be created
     * @param creatingOrder
     * @param closingOrder
     * @return JAXB representation of a Product Order
     * @throws SAPIntegrationException
     */
    protected SAPOrder initializeSAPOrder(ProductOrder placedOrder, boolean creatingOrder, boolean closingOrder) throws SAPIntegrationException {

        ProductOrder orderToUpdate = placedOrder;

        SAPOrder newOrder =
                new SAPOrder(SapIntegrationClientImpl.SystemIdentifier.MERCURY, orderToUpdate.getSapCompanyConfigurationForProductOrder(),
                        orderToUpdate.getQuoteId(), bspUserList.getUserFullName(orderToUpdate.getCreatedBy()),
                        placedOrder.isPriorToSAP1_5());

        newOrder.setExternalOrderNumber(orderToUpdate.getJiraTicketKey());

        newOrder.setResearchProjectNumber(orderToUpdate.getResearchProject().getJiraTicketKey());

        Product primaryProduct = placedOrder.getProduct();
        newOrder.addOrderItem(getOrderItem(placedOrder, primaryProduct, 0, creatingOrder, closingOrder));

        for (ProductOrderAddOn addon : placedOrder.getAddOns()) {
            newOrder.addOrderItem(getOrderItem(placedOrder, addon.getAddOn(), 0, creatingOrder,
                    closingOrder));
        }

        return newOrder;
    }

    /**
     * Helper method to extract the sub elements of a Product order to represent a product and the quantity of which
     * is expected to be charged
     * @param placedOrder Order from which the quantities are defined
     * @param product Product that is to be eventually charged when work on the product order is completed
     * @param additionalSampleCount
     * @param creatingNewOrder
     * @param closingOrder
     * @return JAXB sub element of the SAP order to represent the Product that will be charged and the quantity that
     * is expected of it.
     */
    protected SAPOrderItem getOrderItem(ProductOrder placedOrder, Product product,
                                        int additionalSampleCount, boolean creatingNewOrder, boolean closingOrder) throws SAPIntegrationException {
            BigDecimal sampleCount =
                getSampleCount(placedOrder, product, additionalSampleCount, creatingNewOrder, closingOrder);

            final SAPOrderItem sapOrderItem = new SAPOrderItem(product.getPartNumber(), sampleCount);


                defineConditionsForOrderItem(placedOrder, product, sapOrderItem);

            return sapOrderItem;

    }

    private void defineConditionsForOrderItem(ProductOrder placedOrder, Product product, SAPOrderItem sapOrderItem) {

        if(placedOrder.getProduct().equals(product)) {

            final ProductOrderPriceAdjustment singlePriceAdjustment = placedOrder.getSinglePriceAdjustment();
            if(singlePriceAdjustment != null && singlePriceAdjustment.hasPriceAdjustment()) {

                singlePriceAdjustment.setListPrice(
                        new BigDecimal(productPriceCache.findByProduct(product,
                                placedOrder.getSapCompanyConfigurationForProductOrder()).getBasePrice()));

                if (singlePriceAdjustment.getAdjustmentValue() != null) {
                    sapOrderItem.addCondition(singlePriceAdjustment.deriveAdjustmentCondition(),
                            singlePriceAdjustment.getAdjustmentDifference());
                }
                if(StringUtils.isNotBlank(singlePriceAdjustment.getCustomProductName())) {
                    sapOrderItem.setProductAlias(singlePriceAdjustment.getCustomProductName());
                }
            } else {
                for (ProductOrderPriceAdjustment productOrderPriceAdjustment : placedOrder.getQuotePriceMatchAdjustments()) {
                    if (productOrderPriceAdjustment.hasPriceAdjustment()) {
                        sapOrderItem.addCondition(productOrderPriceAdjustment.deriveAdjustmentCondition(),
                                productOrderPriceAdjustment.getAdjustmentDifference());
                    }
                }
            }
        } else {
            for (ProductOrderAddOn productOrderAddOn : placedOrder.getAddOns()) {
                final ProductOrderAddOnPriceAdjustment singleCustomPriceAdjustment =
                        productOrderAddOn.getSingleCustomPriceAdjustment();
                if(productOrderAddOn.getAddOn().equals(product)) {
                    if(singleCustomPriceAdjustment != null &&
                       singleCustomPriceAdjustment.hasPriceAdjustment()) {
                        singleCustomPriceAdjustment.setListPrice(new BigDecimal(productPriceCache.findByProduct(productOrderAddOn.getAddOn(),
                                placedOrder.getSapCompanyConfigurationForProductOrder()).getBasePrice()));
                        if (singleCustomPriceAdjustment.getAdjustmentValue() != null) {
                            sapOrderItem.addCondition(
                                    singleCustomPriceAdjustment.deriveAdjustmentCondition(),
                                    singleCustomPriceAdjustment.getAdjustmentDifference());
                        }
                        if(StringUtils.isNotBlank(singleCustomPriceAdjustment.getCustomProductName())) {
                            sapOrderItem.setProductAlias(singleCustomPriceAdjustment.getCustomProductName());
                        }
                    } else {
                        for (ProductOrderAddOnPriceAdjustment productOrderAddOnPriceAdjustment : productOrderAddOn
                                .getQuotePriceAdjustments()) {
                            if (productOrderAddOnPriceAdjustment.hasPriceAdjustment()) {
                                sapOrderItem.addCondition(productOrderAddOnPriceAdjustment.deriveAdjustmentCondition(),
                                        productOrderAddOnPriceAdjustment.getAdjustmentDifference());
                            }
                        }
                    }
                }
            }
        }
    }

    protected SAPOrderItem getOrderItem(ProductOrder placedOrder, Product product, int additionalSampleCount,
                                        boolean closingOrder)
            throws SAPIntegrationException {

        final SAPOrderItem sapOrderItem =
                new SAPOrderItem(product.getPartNumber(), getSampleCount(placedOrder, product, additionalSampleCount,
                        false, closingOrder));
        defineConditionsForOrderItem(placedOrder, product, sapOrderItem);
        return sapOrderItem;
    }


    public static BigDecimal getSampleCount(ProductOrder placedOrder, Product product, boolean closingOrder) {

        return getSampleCount(placedOrder, product, 0, false, closingOrder);
    }

    public static BigDecimal getSampleCount(ProductOrder placedOrder, Product product, int additionalSampleCount,
                                            boolean creatingNewOrder, boolean closingOrder) {
        double sampleCount = 0d;

        final PriceAdjustment adjustmentForProduct = placedOrder.getAdjustmentForProduct(product);
        Integer adjustmentQuantity = null;
        if(adjustmentForProduct != null) {
            adjustmentQuantity = adjustmentForProduct.getAdjustmentQuantity();
        }

        int previousBilledCount = 0;

        for (SapOrderDetail sapOrderDetail : placedOrder.getSapReferenceOrders()) {
            if(sapOrderDetail.equals(placedOrder.latestSapOrderDetail()) && !creatingNewOrder) {
                break;
            }

            final Map<Product, Integer> numberOfBilledEntriesByProduct =
                    sapOrderDetail.getNumberOfBilledEntriesByProduct();
            if(numberOfBilledEntriesByProduct.containsKey(product)) {
                previousBilledCount+= numberOfBilledEntriesByProduct.get(product);
            }
        }

        if (closingOrder && !placedOrder.isPriorToSAP1_5()) {
            if(!placedOrder.isSavedInSAP()) {
                throw new InformaticsServiceException("To close out the order in SAP, an SAP order should have been created");
            }
            sampleCount += placedOrder.latestSapOrderDetail().getBilledSampleQuantity(product);
        } else  if (product.getSupportsNumberOfLanes() && placedOrder.getLaneCount() > 0) {
            sampleCount += (adjustmentQuantity != null) ?adjustmentQuantity :placedOrder.getLaneCount();
        } else {
            ProductOrder targetSapPdo = placedOrder;
            sampleCount += (adjustmentQuantity != null)?adjustmentQuantity:targetSapPdo.getTotalNonAbandonedCount(ProductOrder.CountAggregation.SHARE_SAP_ORDER_AND_BILL_READY) + additionalSampleCount;
        }
        return BigDecimal.valueOf(sampleCount-previousBilledCount);
    }

    @Override
    public String findCustomer(SapIntegrationClientImpl.SAPCompanyConfiguration companyCode, FundingLevel fundingLevel) throws SAPIntegrationException {

        String customerNumber = null;
        if (fundingLevel == null || CollectionUtils.isEmpty(fundingLevel.getFunding())) {
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
                                    + "An email has been sent to Dan Warrington in AR to initiate "
                                    + "this SAP Customer Creation process. Please contact Dan Warrington to follow this up.\n"
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
    public String billOrder(QuoteImportItem quoteItemForBilling, BigDecimal quantityOverride, Date workCompleteDate)
            throws SAPIntegrationException {

        SAPDeliveryDocument deliveryDocument =
                new SAPDeliveryDocument(quoteItemForBilling.getProductOrder().getSapOrderNumber(), workCompleteDate);

        SAPDeliveryItem lineItem =
                new SAPDeliveryItem(quoteItemForBilling.getProduct().getPartNumber(),
                        (quantityOverride == null)?new BigDecimal(quoteItemForBilling.getQuantityForSAP()):quantityOverride);

        if(StringUtils.equals(quoteItemForBilling.getQuotePriceType(), LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM.getQuoteType())) {
            lineItem.addCondition(DeliveryCondition.LATE_DELIVERY_DISCOUNT);
        }

        deliveryDocument.addDeliveryItem(lineItem);

        return getClient().createDeliveryDocument(deliveryDocument);
    }

    @NotNull
    protected SAPMaterial initializeSapMaterialObject(Product product) throws SAPIntegrationException {
        SapIntegrationClientImpl.SAPCompanyConfiguration companyCode = product.isExternalProduct() ?
            SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES :
            SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;
        String productHeirarchy = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization();
        if (!product.getPrimaryPriceItem().getPlatform().equals(SSF_PRICE_LIST_NAME)) {
            productHeirarchy = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES.getSalesOrganization();
        }
        BigDecimal minimumOrderQuantity =
            product.getMinimumOrderSize() != null ? new BigDecimal(product.getMinimumOrderSize()) : BigDecimal.ONE;

        return new SAPMaterial(product.getPartNumber(), companyCode, companyCode.getDefaultWbs(),
            product.getProductName(), null, SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, minimumOrderQuantity,
            product.getDescription(), product.getDeliverables(), product.getInputRequirements(),new Date(), new Date(),
            Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, productHeirarchy);
    }

    @Override
    public void publishProductInSAP(Product product) throws SAPIntegrationException {
        SAPChangeMaterial newMaterial = SAPChangeMaterial.fromSAPMaterial(initializeSapMaterialObject(product));

        if (isNewMaterial(product)) {
            getClient().createMaterial(newMaterial);
        } else {
            getClient().changeMaterialDetails(newMaterial);
        }

        if(product.hasExternalCounterpart() || product.isClinicalProduct() || product.isExternalOnlyProduct()) {
            newMaterial.setCompanyCode(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES);
            newMaterial.setMaterialName(StringUtils.isNotBlank(product.getAlternateExternalName())?product.getAlternateExternalName():product.getName());
            if (productPriceCache.findByProduct(product, SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES) == null) {
                getClient().createMaterial(newMaterial);
            } else {
                getClient().changeMaterialDetails(newMaterial);
            }

        }

    }

    private boolean isNewMaterial(Product product) {
        return (productPriceCache.findByProduct(product, SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD) == null)
            && (productPriceCache.findByProduct(product, SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES) == null);
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
    public OrderCalculatedValues calculateOpenOrderValues(int addedSampleCount, String quoteId,
                                                          ProductOrder productOrder) throws SAPIntegrationException {
        OrderCalculatedValues orderCalculatedValues = null;
        if (accessControlEjb.getCurrentControlDefinitions().isEnabled()) {
            OrderCriteria potentialOrderCriteria = null;
            if (productOrder != null && productOrder.getProduct() != null && productsFoundInSap(productOrder)) {
                potentialOrderCriteria = generateOrderCriteria(productOrder, addedSampleCount, true);
            }

            orderCalculatedValues =
                    getClient().calculateOrderValues(quoteId, SapIntegrationClientImpl.SystemIdentifier.MERCURY,
                            potentialOrderCriteria);
        }
        return orderCalculatedValues;
    }

    @Override
    public Quote findSapQuote(String sapQuoteId) throws SAPIntegrationException {
        throw new SAPIntegrationException("SAP Quotes are not available at this time");
    }

    private boolean productsFoundInSap(ProductOrder productOrder) {
        boolean result = true;

        if(!productPriceCache.productExists(productOrder.getProduct().getPartNumber())) {
            result = false;
        } else {
            for (ProductOrderAddOn addOn : productOrder.getAddOns()) {
                if(!productPriceCache.productExists(addOn.getAddOn().getPartNumber())) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    protected OrderCriteria generateOrderCriteria(ProductOrder productOrder) throws SAPIntegrationException {
        return generateOrderCriteria(productOrder, 0, false);
    }

    protected OrderCriteria generateOrderCriteria(ProductOrder productOrder, int addedSampleCount,
                                                  boolean forOrderValueQuery) throws SAPIntegrationException {

        final Set<SAPOrderItem> sapOrderItems = new HashSet<>();
        final Map<Condition, String> conditionStringMap = Collections.emptyMap();
        final SAPOrderItem orderItem = getOrderItem(productOrder, productOrder.getProduct(), addedSampleCount,
            false);

        sapOrderItems.add(orderItem);

        for (ProductOrderAddOn productOrderAddOn : productOrder.getAddOns()) {
            final SAPOrderItem orderSubItem = getOrderItem(productOrder, productOrderAddOn.getAddOn(), addedSampleCount,
                false);
            sapOrderItems.add(orderSubItem);
        }

        String customerNumber = null;
        Optional <Quote> foundQuote = null;
        OrderCriteria orderCriteria = null;

        try {
            foundQuote = Optional.ofNullable(productOrder.getQuote(quoteService));
        } catch (QuoteServerException | QuoteNotFoundException e) {
            if(!forOrderValueQuery) {
                throw new SAPIntegrationException("Unable to get information for the Quote from the quote server", e);
            }
        }
        if(foundQuote.isPresent()) {
            Optional<FundingLevel> fundingLevel = Optional.ofNullable(foundQuote.get().getFirstRelevantFundingLevel());

            if (fundingLevel.isPresent() && CollectionUtils.isEmpty(fundingLevel.get().getFunding())) {
                // Too many funding sources to allow this to work with SAP.  Keep using the Quote Server as the definition
                // of funding
                if (!forOrderValueQuery) {
                    throw new SAPIntegrationException(
                            "Unable to continue with SAP.  The associated quote has either too few or too many funding sources");
                }
            }

            if (fundingLevel.isPresent()) {
                if (!forOrderValueQuery && fundingLevel.get().getFunding().size() > 1) {
                    throw new SAPIntegrationException(
                            "This order is ineligible to save to SAP since there are multiple "
                            + "funding sources associated with the given quote " +
                            productOrder.getQuoteId());
                }
                for (Funding funding : fundingLevel.get().getFunding()) {
                    if (funding.getFundingType().equals(Funding.PURCHASE_ORDER)) {
                        customerNumber =
                                findCustomer(productOrder.getSapCompanyConfigurationForProductOrder(),
                                        fundingLevel.get());
                    } else {
                        customerNumber = SapIntegrationClientImpl.INTERNAL_ORDER_CUSTOMER_NUMBER;
                    }
                }
            }
        }

        if(customerNumber != null) {
            orderCriteria = new OrderCriteria(customerNumber, productOrder.getSapCompanyConfigurationForProductOrder(),
                    sapOrderItems);
        }
        return orderCriteria;
    }

    /**
     * Helper method to figure out which company code to associate with a given product order
     * @param companyProductOrder Product Order from which the company code is to be determined
     * @return an indicator that represents one of the configured companies within SAP
     */
    public static SapIntegrationClientImpl.SAPCompanyConfiguration determineCompanyCode(ProductOrder companyProductOrder)
            throws SAPIntegrationException
    {
        SapIntegrationClientImpl.SAPCompanyConfiguration companyCode =
                companyProductOrder.getSapCompanyConfigurationForProductOrder();

        final SapOrderDetail latestSapOrderDetail = companyProductOrder.latestSapOrderDetail();
        if(latestSapOrderDetail != null && latestSapOrderDetail.getCompanyCode()!= null
           && !latestSapOrderDetail.getCompanyCode().equals(companyCode.getCompanyCode())) {
            throw new SAPIntegrationException("This combination of Product and Order is attempting to change the "
                                              + "company code to which this order will be associated.");
        }

        return companyCode;
    }

    protected void setQuoteService(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    protected void setBspUserList(BSPUserList bspUserList) {
        this.bspUserList = bspUserList;
    }

    public void setWrappedClient(SapIntegrationClientImpl wrappedClient) {
        this.wrappedClient = wrappedClient;
    }

    protected void setPriceListCache(PriceListCache priceListCache) {
        this.priceListCache = priceListCache;
    }

    public void setProductPriceCache(SAPProductPriceCache productPriceCache) {
        this.productPriceCache = productPriceCache;
    }
}
