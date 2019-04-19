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
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.sap.entity.Condition;
import org.broadinstitute.sap.entity.DeliveryCondition;
import org.broadinstitute.sap.entity.OrderCalculatedValues;
import org.broadinstitute.sap.entity.OrderCriteria;
import org.broadinstitute.sap.entity.SAPDeliveryDocument;
import org.broadinstitute.sap.entity.SAPDeliveryItem;
import org.broadinstitute.sap.entity.SAPReturnOrder;
import org.broadinstitute.sap.entity.material.SAPChangeMaterial;
import org.broadinstitute.sap.entity.material.SAPMaterial;
import org.broadinstitute.sap.entity.order.SAPOrder;
import org.broadinstitute.sap.entity.order.SAPOrderItem;
import org.broadinstitute.sap.entity.order.SAPUpdateOrder;
import org.broadinstitute.sap.entity.quote.QuoteItem;
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.jetbrains.annotations.NotNull;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.broadinstitute.sap.services.SapIntegrationClientImpl.MISSING_CUSTOMER_RESULT;
import static org.broadinstitute.sap.services.SapIntegrationClientImpl.SAPCompanyConfiguration;
import static org.broadinstitute.sap.services.SapIntegrationClientImpl.SAPEnvironment;
import static org.broadinstitute.sap.services.SapIntegrationClientImpl.SystemIdentifier;
import static org.broadinstitute.sap.services.SapIntegrationClientImpl.TOO_MANY_ACCOUNTS_RESULT;

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

        SAPEnvironment environment;

        switch (sapConfig.getExternalDeployment()) {
        case PROD:
            environment = SAPEnvironment.PRODUCTION;
            break;
            case DEV:
                environment = SAPEnvironment.DEV;
                break;
            case TEST:
                environment = SAPEnvironment.DEV_400;
                break;
            case RC:
                environment = SAPEnvironment.QA_400;
                break;
            case QA:
            default:
                environment = SAPEnvironment.QA;
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
        SAPOrder newOrder = initializeSAPOrder(placedOrder, Option.create(Option.Type.CREATING));
        return getClient().createSAPOrder(newOrder);
    }

    @Override
    public String createOrderWithQuote(ProductOrder placedOrder) throws SAPIntegrationException {
        throw new SAPIntegrationException("SAP Quotes are not available at this time");
    }

    @Override
    public void updateOrder(ProductOrder placedOrder, Option serviceOptions) throws SAPIntegrationException {

        SAPUpdateOrder order = (SAPUpdateOrder) initializeSAPOrder(placedOrder, serviceOptions);

        if(placedOrder.getSapOrderNumber() == null) {
            throw new SAPIntegrationException("Cannot update an order in SAP since this product order does not have "
                                              + "an SAP Order number with which to reference an order.");
        }

        getClient().updateSAPOrder(order);
    }

    @Override
    public void updateOrderWithQuote(ProductOrder placedOrder, Option options) throws SAPIntegrationException {
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
    protected SAPOrder initializeSAPOrder(ProductOrder placedOrder, Option serviceOptions)
        throws SAPIntegrationException {
//        Option serviceOptions = Option.create(Option.isClosing(closingOrder), Option.isCreating(creatingOrder));
        ProductOrder orderToUpdate = placedOrder;
        String sapOrderNumber = orderToUpdate.getSapOrderNumber();

        SAPCompanyConfiguration sapCompanyConfiguration = orderToUpdate.getSapCompanyConfigurationForProductOrder();
        String userFullName = bspUserList.getUserFullName(orderToUpdate.getCreatedBy());
        List<SAPOrderItem> orderItems =
            getOrderItems(placedOrder, placedOrder.getProduct(), serviceOptions);

        SAPOrder newOrder =
            new SAPUpdateOrder(sapCompanyConfiguration, orderToUpdate.getQuoteId(), userFullName, sapOrderNumber,
                orderToUpdate.getJiraTicketKey(), orderToUpdate.getResearchProject().getJiraTicketKey(), null,
                orderItems.toArray(new SAPOrderItem[0]));

        if (serviceOptions.hasOption(Option.Type.CREATING)) {
            return (SAPOrder) newOrder;
        }
        return newOrder;
    }

    private List<SAPOrderItem> getOrderItems(ProductOrder placedOrder, Product primaryProduct, Option serviceOptions)
        throws SAPIntegrationException {
        List<SAPOrderItem> orderItems = new ArrayList<>();
        if (placedOrder.getSapQuote(this) != null) {
            orderItems.add(getOrderItem(placedOrder, primaryProduct, 0, serviceOptions));
            for (ProductOrderAddOn addon : placedOrder.getAddOns()) {
                orderItems.add(getOrderItem(placedOrder, addon.getAddOn(), 0, serviceOptions));
            }
        }
        return orderItems;
    }

    /**
     * Helper method to extract the sub elements of a Product order to represent a product and the quantity of which
     * is expected to be charged
     * @param placedOrder Order from which the quantities are defined
     * @param product Product that is to be eventually charged when work on the product order is completed
     * @param additionalSampleCount
     * @param creatingNewOrder
     * @param closingOrder
     * @param forOrderValueQuery
     * @return JAXB sub element of the SAP order to represent the Product that will be charged and the quantity that
     * is expected of it.
     */
    protected SAPOrderItem getOrderItem(ProductOrder placedOrder, Product product,
                                        int additionalSampleCount, SapIntegrationService.Option serviceOptions) throws SAPIntegrationException {
            BigDecimal sampleCount =
                getSampleCount(placedOrder, product, additionalSampleCount, serviceOptions);
        SapQuote sapQuote = placedOrder.getSapQuote(this);

        if (sapQuote != null) {
            Collection<QuoteItem> quoteItems = sapQuote.getQuoteItemMap().get(product.getPartNumber());
            if (quoteItems.size() != 1) {
                throw new SAPIntegrationException("Could not determint the line item of the quote");
            }
            QuoteItem quoteItem = quoteItems.iterator().next();
            SAPOrderItem sapOrderItem =
                new SAPOrderItem(quoteItem.getMaterialNumber(), quoteItem.getQuoteItemNumber(),
                    quoteItem.getMaterialDescription(), sampleCount, quoteItem.getMaterialDescription(),
                    String.format("%s qty of %s", sampleCount.toPlainString(), quoteItem.getMaterialNumber()));

            defineConditionsForOrderItem(placedOrder, product, sapOrderItem);
            return sapOrderItem;
        }
        return null;
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
                                        boolean closingOrder, boolean forOrderValueQuery) throws SAPIntegrationException {
        SapQuote sapQuote = placedOrder.getSapQuote(this);
        Collection<QuoteItem> quoteItems = sapQuote.getQuoteItemMap().get(product.getPartNumber());

        // todo: GPLIM-6224 line item should be stored so this method returns the correct one!
        if (CollectionUtils.isEmpty(quoteItems)) {
            return null;
        }
        QuoteItem quoteLineItem = quoteItems.iterator().next();
        Option serviceOptions = Option.create(Option.isClosing(closingOrder), Option.isForValueQuery(forOrderValueQuery));
        BigDecimal sampleCount = getSampleCount(placedOrder, product, additionalSampleCount, serviceOptions);
        SAPOrderItem sapOrderItem =
            new SAPOrderItem(quoteLineItem.getMaterialNumber(), quoteLineItem.getQuoteItemNumber(),
                product.getProductName(), sampleCount, product.getProductName(),
                String.format("%s qty of %s", sampleCount.toPlainString(), product.getPartNumber()));
        defineConditionsForOrderItem(placedOrder, product, sapOrderItem);
        return sapOrderItem;
    }

    /**
     * This appears to currently only be used for a single fixup test
     * @param placedOrder
     * @param product
     * @param closingOrder
     * @return
     */
    public static BigDecimal getSampleCount(ProductOrder placedOrder, Product product, boolean closingOrder) {

        return getSampleCount(placedOrder, product, 0, Option.create(Option.isClosing(closingOrder)));
    }

    public static BigDecimal getSampleCount(ProductOrder placedOrder, Product product, int additionalSampleCount,
                                            SapIntegrationService.Option serviceOptions) {
        double sampleCount = 0d;

        final PriceAdjustment adjustmentForProduct = placedOrder.getAdjustmentForProduct(product);
        Integer adjustmentQuantity = null;
        if(adjustmentForProduct != null) {
            adjustmentQuantity = adjustmentForProduct.getAdjustmentQuantity();
        }

        double previousBilledCount = 0;

        for (SapOrderDetail sapOrderDetail : placedOrder.getSapReferenceOrders()) {
            if(sapOrderDetail.equals(placedOrder.latestSapOrderDetail()) && !serviceOptions.hasOption(Option.Type.CREATING)) {
                previousBilledCount = 0;
                break;
            }

            final Map<Product, Double> numberOfBilledEntriesByProduct =
                    sapOrderDetail.getNumberOfBilledEntriesByProduct();
            if(numberOfBilledEntriesByProduct.containsKey(product)) {
                previousBilledCount+= numberOfBilledEntriesByProduct.get(product);
            }
        }

        if (serviceOptions.hasOption(Option.Type.CLOSING) && !placedOrder.isPriorToSAP1_5()) {
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

        if(serviceOptions.hasOption(Option.Type.ORDER_VALUE_QUERY)) {
            previousBilledCount = (int) ProductOrder.getBilledSampleCount(placedOrder, product);
        }
        return BigDecimal.valueOf(sampleCount-previousBilledCount);
    }

    @Override
    public String findCustomer(SAPCompanyConfiguration companyCode, FundingLevel fundingLevel) throws SAPIntegrationException {

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
                        if (e.getMessage().equals(MISSING_CUSTOMER_RESULT)) {
                            throw new SAPIntegrationException(
                                    "Your order cannot be placed in SAP because the email address "
                                    + "specified on the Quote is not attached to any SAP Customer account.\n"
                                    + "An email has been sent to Dan Warrington in AR to initiate "
                                    + "this SAP Customer Creation process. Please contact Dan Warrington to follow this up.\n"
                                    + "Once the Customer has been created in SAP you will need "
                                    + "to resubmit this order to ensure that your work is "
                                    + "properly processed.\n"
                                    + "For further questions please contact Mercury support");
                        } else if (e.getMessage().equals(TOO_MANY_ACCOUNTS_RESULT)) {
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
        SAPCompanyConfiguration companyCode = product.isExternalProduct() ?
            SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES :
            SAPCompanyConfiguration.BROAD;
        String productHeirarchy = SAPCompanyConfiguration.BROAD.getSalesOrganization();

        if (product.isExternalOnlyProduct() || product.isClinicalProduct()) {
            productHeirarchy = SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES.getSalesOrganization();
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
        SAPChangeMaterial sapMaterial = SAPChangeMaterial.fromSAPMaterial(initializeSapMaterialObject(product));
        if (isNewMaterial(product)) {
            getClient().createMaterial(sapMaterial);
        } else {
            getClient().changeMaterialDetails(sapMaterial);
        }
        if(sapMaterial.getCompanyCode() == SAPCompanyConfiguration.BROAD) {
            sapMaterial.setCompanyCode(SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES);

            String materialName = StringUtils.defaultString(product.getAlternateExternalName(), product.getName());
            sapMaterial.setMaterialName(materialName);
            if (productPriceCache.findByProduct(product, SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES) == null) {
                getClient().createMaterial(sapMaterial);
            } else {
                getClient().changeMaterialDetails(sapMaterial);
            }

        }

    }

    private boolean isNewMaterial(Product product) {
        return (productPriceCache.findByProduct(product, SAPCompanyConfiguration.BROAD) == null)
            && (productPriceCache.findByProduct(product, SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES) == null);
    }

    @Override
    public Set<SAPMaterial> findProductsInSap() throws SAPIntegrationException {
        Set<SAPMaterial> materials = new HashSet<>();
        materials.addAll(findMaterials(SAPCompanyConfiguration.BROAD));
        materials.addAll(findMaterials(SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES));
        return materials;
    }

    private Set<SAPMaterial> findMaterials(SAPCompanyConfiguration sapCompanyConfiguration)
        throws SAPIntegrationException {
        return getClient()
            .findMaterials(sapCompanyConfiguration.getPlant(), sapCompanyConfiguration.getSalesOrganization());
    }

    @Override
    public OrderCalculatedValues calculateOpenOrderValues(int addedSampleCount, String quoteId,
                                                          ProductOrder productOrder) throws SAPIntegrationException {
        OrderCalculatedValues orderCalculatedValues = null;
        OrderCriteria potentialOrderCriteria = null;
        if (productOrder != null && productOrder.getProduct() != null && productsFoundInSap(productOrder)) {
            potentialOrderCriteria = generateOrderCriteria(productOrder, addedSampleCount,
                SapIntegrationService.Option.create(Option.Type.ORDER_VALUE_QUERY));
        }

        orderCalculatedValues =
                getClient().calculateOrderValues(quoteId, SystemIdentifier.MERCURY, potentialOrderCriteria);
        return orderCalculatedValues;
    }

    @Override
    public SapQuote findSapQuote(String sapQuoteId) throws SAPIntegrationException {
        final SapIntegrationClientImpl sapClient = getClient();

        return getClient().findQuoteDetails(sapQuoteId);

    }

    @Override
    public String creditDelivery(String deliveryDocumentId, QuoteImportItem quoteItemForBilling)
            throws SAPIntegrationException {

        SAPOrderItem returnLine = new SAPOrderItem(quoteItemForBilling.getProduct().getPartNumber(), BigDecimal.valueOf(quoteItemForBilling.getQuantity()));
        SAPReturnOrder returnOrder = new SAPReturnOrder(deliveryDocumentId, Collections.singleton(returnLine));
        return getClient().createReturnOrder(returnOrder);
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
        return generateOrderCriteria(productOrder, 0, Option.NONE);
    }

    protected OrderCriteria generateOrderCriteria(ProductOrder productOrder, int addedSampleCount,
                                                  SapIntegrationService.Option orderOption) throws SAPIntegrationException {

        final Set<SAPOrderItem> sapOrderItems = new HashSet<>();
        final Map<Condition, String> conditionStringMap = Collections.emptyMap();
        final SAPOrderItem orderItem = getOrderItem(productOrder, productOrder.getProduct(), addedSampleCount,
            orderOption);

        sapOrderItems.add(orderItem);

        for (ProductOrderAddOn productOrderAddOn : productOrder.getAddOns()) {
            final SAPOrderItem orderSubItem = getOrderItem(productOrder, productOrderAddOn.getAddOn(), addedSampleCount,
                orderOption);
            sapOrderItems.add(orderSubItem);
        }

        String customerNumber = null;
        Optional <SapQuote> foundQuote = null;
        OrderCriteria orderCriteria = null;

        //todo  The customer number is no longer necessary for the order criteria.
        orderCriteria = new OrderCriteria(customerNumber, productOrder.getSapCompanyConfigurationForProductOrder(),
                    sapOrderItems);
        return orderCriteria;
    }

    /**
     * Helper method to figure out which company code to associate with a given product order
     * @param companyProductOrder Product Order from which the company code is to be determined
     * @return an indicator that represents one of the configured companies within SAP
     */
    public static SAPCompanyConfiguration determineCompanyCode(ProductOrder companyProductOrder)
            throws SAPIntegrationException
    {
        SAPCompanyConfiguration companyCode = companyProductOrder.getSapCompanyConfigurationForProductOrder();

        final SapOrderDetail latestSapOrderDetail = companyProductOrder.latestSapOrderDetail();
        if(latestSapOrderDetail != null && latestSapOrderDetail.getCompanyCode()!= null
           && !latestSapOrderDetail.getCompanyCode().equals(companyCode.getCompanyCode())) {
            throw new SAPIntegrationException("This combination of Product and Order is attempting to change the "
                                              + "company code to which this order will be associated.");
        }

        return companyCode;
    }

    public static SapIntegrationClientImpl.SAPCompanyConfiguration determineCompanyCode(SapQuote quote) {
        SapIntegrationClientImpl.SAPCompanyConfiguration sapCompanyConfiguration =
                SapIntegrationClientImpl.SAPCompanyConfiguration
                        .fromSalesOrgForMaterial(quote.getQuoteHeader().getSalesOrganization());

        if(!Arrays.asList(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD).contains(sapCompanyConfiguration)) {
            sapCompanyConfiguration = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;
        }
        return sapCompanyConfiguration;
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
