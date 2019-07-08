package org.broadinstitute.gpinformatics.infrastructure.sap;

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
import org.broadinstitute.gpinformatics.athena.entity.orders.SapQuoteItemReference;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
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
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.jetbrains.annotations.NotNull;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.MoreCollectors.toOptional;
import static org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService.Option.Type;
import static org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService.Option.create;
import static org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService.Option.isClosing;
import static org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService.Option.isForValueQuery;
import static org.broadinstitute.sap.services.SapIntegrationClientImpl.SAPCompanyConfiguration;
import static org.broadinstitute.sap.services.SapIntegrationClientImpl.SAPEnvironment;

@Dependent
@Default
public class SapIntegrationServiceImpl implements SapIntegrationService {

    public static final Set<SAPCompanyConfiguration> EXTENDED_PLATFORMS =
            EnumSet.of(SAPCompanyConfiguration.PRISM,SAPCompanyConfiguration.GPP,
                    SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES);
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

    /**
     * Helper method to generate the Quantity of SAP order line items based on the product order, product for the line
     * item, and the context in which it is called.
     *
     * This method will be smart enough to support the varying needs for for line item quantity:
     *  <ul>
     *      <li>Creating a new order -- This will be the scenario with which we are attempting to create a new SAP
     *          order.  the value should be the effective quantity for the product MINUS any billing that may have been
     *          done on a previous order and the current order</li>
     *      <li>Closing an order -- Similar to creating a new order, this scenario will set the line item quantity
     *          to be the effective quantity for the product minusany billing that may have been done on the
     *          current order</li>
     *      <li>Order value query AND None --  This case will return the current state of the SAP Order.  The value will
     *          match what Create was when it was first call in that it excludes the quantity of what was billed on any
     *          previous orders</li>
     *  </ul>
     * @param placedOrder           Order from which the SAP order is created
     * @param product               Product which is associate to the SAP order line item of which this sample count
     *                              is being requested.
     * @param additionalSampleCount Additional quantity to be added to the total sample count.  Typically used in the
     *                              case of adding samples to an existing PDO
     * @param serviceOptions        Enum to determine what scenario this sample count request is for.
     * @return  Total count, based on scenario, to be used for the line item of an SAP order
     */
    public static BigDecimal getSampleCount(ProductOrder placedOrder, Product product, int additionalSampleCount,
                                            SapIntegrationService.Option serviceOptions) {
        double sampleCount = 0d;

        final PriceAdjustment adjustmentForProduct = placedOrder.getAdjustmentForProduct(product);
        Integer adjustmentQuantity = null;
        if (adjustmentForProduct != null) {
            adjustmentQuantity = adjustmentForProduct.getAdjustmentQuantity();
        }

        double previousBilledCount = 0;

        boolean creatingNewOrder = serviceOptions.hasOption(Type.CREATING);
        boolean closingOrder = serviceOptions.hasOption(Type.CLOSING);
        boolean forOrderValueQuery = serviceOptions.hasOption(Type.ORDER_VALUE_QUERY);

        for (SapOrderDetail sapOrderDetail : placedOrder.getSapReferenceOrders()) {

            // If we are creating a new order, we want the SAP Order quantity to be :
            // Non abandoned product order quantity MINUS anything that has been billed on this order previously

            // If we are NOT creating a new order (General order update or order value query)
            // then the current SAP order quantity will be:
            // Non abandoned product order quantity MINUS anything that has been billed on a previous SAP order.

            if (!sapOrderDetail.equals(placedOrder.latestSapOrderDetail()) || creatingNewOrder) {

                final Map<Product, Double> numberOfBilledEntriesByProduct =
                        sapOrderDetail.getNumberOfBilledEntriesByProduct();
                if (numberOfBilledEntriesByProduct.containsKey(product)) {
                    previousBilledCount += numberOfBilledEntriesByProduct.get(product);
                }
            }
        }


        //When closing the SAP order, the SAP order quantity will become however much has been billed on the current
        // order for the given product
        if (closingOrder && !placedOrder.isPriorToSAP1_5()) {
            if (!placedOrder.isSavedInSAP()) {
                throw new InformaticsServiceException(
                    "To close out the order in SAP, an SAP order should have been created");
            }
            sampleCount += placedOrder.latestSapOrderDetail().getBilledSampleQuantity(product);
        } else if (product.getSupportsNumberOfLanes() && placedOrder.getLaneCount() > 0) {
            sampleCount += (adjustmentQuantity != null) ? adjustmentQuantity : placedOrder.getLaneCount();
        } else {
            sampleCount += (adjustmentQuantity != null) ? adjustmentQuantity :
                placedOrder.getTotalNonAbandonedCount(ProductOrder.CountAggregation.SHARE_SAP_ORDER_AND_BILL_READY)
                + additionalSampleCount;
        }

        BigDecimal countResults = BigDecimal.valueOf(sampleCount);
        if(!closingOrder) {
            countResults = countResults.subtract(BigDecimal.valueOf(previousBilledCount));
            if (countResults.compareTo(BigDecimal.ZERO) < 0) {
                countResults = BigDecimal.ZERO;
            }
        }
        return countResults;
    }

    /**
     * This appears to currently only be used for a single fixup test
     *
     * @param placedOrder
     * @param product
     * @param closingOrder
     *
     * @return
     */
    public static BigDecimal getSampleCount(ProductOrder placedOrder, Product product, boolean closingOrder) {

        return getSampleCount(placedOrder, product, 0, create(isClosing(closingOrder)));
    }

    @Override
    public String createOrder(ProductOrder placedOrder) throws SAPIntegrationException {
        SAPOrder newOrder =
            initializeSAPOrder(placedOrder.getSapQuote(this), placedOrder, create(Type.CREATING));
        return getClient().createSAPOrder(newOrder);
    }

    @Override
    public void updateOrder(ProductOrder placedOrder, boolean closeOrder) throws SAPIntegrationException {
        SAPOrder order =
            initializeSAPOrder(placedOrder.getSapQuote(this), placedOrder, Option.create(isClosing(closeOrder)));

        if(placedOrder.getSapOrderNumber() == null) {
            throw new SAPIntegrationException("Cannot update an order in SAP since this product order does not have "
                                              + "an SAP Order number with which to reference an order.");
        }

        getClient().updateSAPOrder(order);
    }

    /**
     * Helper method to compile the order object which will be transmitted to SAP for either creation up to be
     * updated
     * @param placedOrder The ProductOrder from which a JAXB representation of an SAP order will be created
     * @return JAXB representation of a Product Order
     * @throws SAPIntegrationException
     */
    protected SAPOrder initializeSAPOrder(SapQuote sapQuote, ProductOrder placedOrder, Option serviceOptions) {
        ProductOrder orderToUpdate = placedOrder;
        String sapOrderNumber = null;
        if (!serviceOptions.hasOption(Type.CREATING)) {
            sapOrderNumber = orderToUpdate.getSapOrderNumber();
        }

        SAPCompanyConfiguration sapCompanyConfiguration =
                placedOrder.getSapCompanyConfigurationForProductOrder(sapQuote);
        String userFullName = bspUserList.getUserFullName(orderToUpdate.getCreatedBy());
        List<SAPOrderItem> orderItems = getOrderItems(sapQuote, placedOrder, placedOrder.getProduct(), serviceOptions);

        return new SAPOrder(sapCompanyConfiguration, orderToUpdate.getQuoteId(), userFullName, sapOrderNumber,
            orderToUpdate.getJiraTicketKey(), orderToUpdate.getResearchProject().getJiraTicketKey(), null,
            orderItems.toArray(new SAPOrderItem[0]));
    }

    private List<SAPOrderItem> getOrderItems(SapQuote sapQuote, ProductOrder placedOrder, Product primaryProduct,
                                             Option serviceOptions) {
        List<SAPOrderItem> orderItems = new ArrayList<>();
        orderItems.add(getOrderItem(sapQuote, placedOrder, primaryProduct, 0, serviceOptions));
        for (ProductOrderAddOn addon : placedOrder.getAddOns()) {
            orderItems.add(getOrderItem(sapQuote, placedOrder, addon.getAddOn(), 0, serviceOptions));
        }
        return orderItems;
    }

    /**
     * Helper method to extract the sub elements of a Product order to represent a product and the quantity of which
     * is expected to be charged
     * @param placedOrder Order from which the quantities are defined
     * @param product Product that is to be eventually charged when work on the product order is completed
     * @param additionalSampleCount
     * @return JAXB sub element of the SAP order to represent the Product that will be charged and the quantity that
     * is expected of it.
     */
    protected SAPOrderItem getOrderItem(SapQuote sapQuote, ProductOrder placedOrder, Product product,
                                        int additionalSampleCount, SapIntegrationService.Option serviceOptions) {
        BigDecimal sampleCount = getSampleCount(placedOrder, product, additionalSampleCount, serviceOptions);
        if (sapQuote != null) {

            final Optional<SapQuoteItemReference> quoteItemReference = placedOrder.getQuoteReferences().stream()
                    .filter(sapQuoteItemReference -> sapQuoteItemReference.getMaterialReference().equals(product))
                    .collect(toOptional());
            if(quoteItemReference.isPresent()) {
                final SAPOrderItem sapOrderItem = new SAPOrderItem(product.getPartNumber(),
                        Integer.valueOf(quoteItemReference.get().getQuoteLineReference()),
                        product.getProductName(), sampleCount, null, null);
                defineConditionsForOrderItem(placedOrder, product, sapOrderItem);
                return sapOrderItem;
            }
        }
        return null;
    }

    private void defineConditionsForOrderItem(ProductOrder placedOrder, Product product, SAPOrderItem sapOrderItem) {

        if(placedOrder.getProduct().equals(product)) {
            final ProductOrderPriceAdjustment singlePriceAdjustment = placedOrder.getSinglePriceAdjustment();
            if (singlePriceAdjustment != null && singlePriceAdjustment.hasPriceAdjustment()) {

                if (singlePriceAdjustment.getAdjustmentValue() != null) {
                    sapOrderItem.addCondition(singlePriceAdjustment.getAdjustmentCondition(),
                        singlePriceAdjustment.getAdjustmentValue());
                }
                if (StringUtils.isNotBlank(singlePriceAdjustment.getCustomProductName())) {
                    sapOrderItem.setProductAlias(singlePriceAdjustment.getCustomProductName());
                }
            } else {
                for (ProductOrderPriceAdjustment productOrderPriceAdjustment : placedOrder.getQuotePriceMatchAdjustments()) {
                    if (productOrderPriceAdjustment.hasPriceAdjustment()) {
                        sapOrderItem.addCondition(productOrderPriceAdjustment.getAdjustmentCondition(),
                            productOrderPriceAdjustment.getAdjustmentValue());
                    }
                }
            }
        } else {
            for (ProductOrderAddOn productOrderAddOn : placedOrder.getAddOns()) {
                final ProductOrderAddOnPriceAdjustment singleCustomPriceAdjustment = productOrderAddOn.getSingleCustomPriceAdjustment();
                final Product addOn = productOrderAddOn.getAddOn();
                if (addOn.equals(product)) {
                    if (singleCustomPriceAdjustment != null &&
                        singleCustomPriceAdjustment.hasPriceAdjustment()) {
                        if (singleCustomPriceAdjustment.getAdjustmentValue() != null) {
                            sapOrderItem.addCondition(
                                singleCustomPriceAdjustment.getAdjustmentCondition(),
                                singleCustomPriceAdjustment.getAdjustmentValue());
                        }
                        if (StringUtils.isNotBlank(singleCustomPriceAdjustment.getCustomProductName())) {
                            sapOrderItem.setProductAlias(singleCustomPriceAdjustment.getCustomProductName());
                        }
                    } else {
                        for (ProductOrderAddOnPriceAdjustment productOrderAddOnPriceAdjustment : productOrderAddOn
                            .getQuotePriceAdjustments()) {
                            if (productOrderAddOnPriceAdjustment.hasPriceAdjustment()) {
                                sapOrderItem.addCondition(productOrderAddOnPriceAdjustment.getAdjustmentCondition(),
                                        productOrderAddOnPriceAdjustment.getAdjustmentValue());
                            }
                        }
                    }
                }
            }
        }
        if(StringUtils.isBlank(sapOrderItem.getProductAlias())) {
            if (placedOrder.getOrderType() == ProductOrder.OrderAccessType.COMMERCIAL &&
                StringUtils.isNotBlank(product.getAlternateExternalName())) {
                sapOrderItem.setProductAlias(product.getAlternateExternalName());
            }
        }
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
        SAPCompanyConfiguration companyCode = SAPCompanyConfiguration.BROAD;
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

        publishProductInSAP(product, true, PublishType.CREATE_AND_UPDATE);
    }

    /**
     * With the introduction of a direct communication to SAP from Mercury, we will do away with Price Items for a
     * representation of the price of work.  Products are now directly reflected in the System which manages our
     * financial records (SAP).  This method will allow Mercury to create a representation of a Product within SAP
     * for the purpose of tracking projected and actual work
     *
     * For an existing product, this method will also allow Mercury to update that product with any changes made within
     * Mercury
     * @param product                           The Product information to be reflected in SAP
     * @param extendProductsToOtherPlatforms    Flag to help determine if Mercury should extend the resulting Material to platforms
     *                                          other than GP SSF or GP LLC product list.  True means we wish to extend the product
     * @param publishType                Flag to help determine if Mercury should should attempt to create a new Material
     *                                          if the Product it is not found in SAP.  When true, do not create material
     * @throws SAPIntegrationException
     */
    @Override
    public void publishProductInSAP(Product product, boolean extendProductsToOtherPlatforms,
                                    PublishType publishType) throws SAPIntegrationException {
        SAPMaterial sapMaterial = initializeSapMaterialObject(product);

        applyMaterialUpdate(product, publishType, sapMaterial);

        Set<SAPMaterial> extendedProducts = new HashSet<>();

        Set<SAPCompanyConfiguration> platformsToExtend = EXTENDED_PLATFORMS
                .stream()
                .filter(companyConfiguration ->
                        extendProductsToOtherPlatforms ||
                        StringUtils.equals(SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES.getSalesOrganization(),
                                        companyConfiguration.getSalesOrganization()))
                .collect(Collectors.toSet());

        for (SAPCompanyConfiguration sapCompanyConfiguration : platformsToExtend) {
            log.debug("Current company config is " + sapCompanyConfiguration.name());
            SAPMaterial tempMaterial = null;
            if (sapCompanyConfiguration == SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES) {

                tempMaterial = initializeSapMaterialObject(product);
            } else {

                if(!product.isExternalOnlyProduct() && !product.isClinicalProduct()) {
                    log.debug("Saving material for " + sapCompanyConfiguration.name());
                    tempMaterial = initializeSapMaterialObject(product);
                } else {
                    log.debug("current product is either External or Clinical");
                }
            }

            if(tempMaterial != null) {
                tempMaterial.setSalesOrg(sapCompanyConfiguration.getSalesOrganization());
                tempMaterial.setDistributionChannel(sapCompanyConfiguration.getDistributionChannel());
                extendedProducts.add(tempMaterial);
            }
        }

        for (SAPMaterial extendedProduct : extendedProducts) {
            applyMaterialUpdate(product, publishType, extendedProduct);
        }
    }

    /**
     * Helper method to encapsulate the previously duplicated logic of determining if a product should be created or
     * updated
     * @param product               The Mercury product which is intended to be saved to SAP and represented as a
     *                              Material
     * @param publishType    Flag to help determine if Mercury should should attempt to create a new Material
     *                              if the Product it is not found in SAP.  When true, do not create material
     * @param extendedProduct       Flag to help determine if Mercury should extend the resulting Material to platforms
     *                              other than GP SSF or GP LLC product list.  True means we wish to extend the product
     * @throws SAPIntegrationException
     */
    private void applyMaterialUpdate(Product product, PublishType publishType, SAPMaterial extendedProduct)
            throws SAPIntegrationException {
        if (productPriceCache.findByProduct(product,
                SAPCompanyConfiguration.fromSalesOrgForMaterial(extendedProduct.getSalesOrg()).getSalesOrganization()) == null) {
            if (publishType != PublishType.UPDATE_ONLY) {

                //TODO SGM Unsure about this
                if(product.isSSFProduct() && !product.getOfferedAsCommercialProduct() &&
                   StringUtils.equals(extendedProduct.getSalesOrg(),SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES.getSalesOrganization())) {
                    return;
                }
                log.debug("Creating product " + extendedProduct.getMaterialIdentifier());
                getClient().createMaterial(extendedProduct);
            }
        } else {
            if (publishType != PublishType.CREATE_ONLY) {
                log.debug("Updating product " + extendedProduct.getMaterialIdentifier());
                if(product.isSSFProduct() && !product.getOfferedAsCommercialProduct()) {
                    extendedProduct.setStatus(SAPMaterial.MaterialStatus.DISABLED);
                }
                getClient().changeMaterialDetails(SAPChangeMaterial.fromSAPMaterial(extendedProduct));
            }
        }
    }

    private boolean isNewMaterial(Product product) {
        return (productPriceCache.findByProduct(product,
                SAPCompanyConfiguration.BROAD.getSalesOrganization()) == null)
            && (productPriceCache.findByProduct(product,
                SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES.getSalesOrganization()) == null);
    }

    @Override
    public Set<SAPMaterial> findProductsInSap() throws SAPIntegrationException {
        Set<SAPMaterial> materials = new HashSet<>();
        final List<SAPCompanyConfiguration> extendedPlatformsPlusBroad = new ArrayList<>(EXTENDED_PLATFORMS);
        extendedPlatformsPlusBroad.add(SAPCompanyConfiguration.BROAD);
        for (SAPCompanyConfiguration sapCompanyConfiguration : extendedPlatformsPlusBroad) {
            log.debug("finding for " + sapCompanyConfiguration.getSalesOrganization());
            materials.addAll(findMaterials(sapCompanyConfiguration));
        }

        return materials;
    }

    private Set<SAPMaterial> findMaterials(SAPCompanyConfiguration sapCompanyConfiguration)
        throws SAPIntegrationException {
        return getClient()
            .findMaterials(sapCompanyConfiguration.getPlant(), sapCompanyConfiguration.getSalesOrganization());
    }

    @Override
    public OrderCalculatedValues calculateOpenOrderValues(int addedSampleCount, SapQuote sapQuote,
                                                          ProductOrder productOrder) throws SAPIntegrationException {
        OrderCalculatedValues orderCalculatedValues = null;
        OrderCriteria potentialOrderCriteria = null;
            potentialOrderCriteria = generateOrderCriteria(sapQuote, productOrder, addedSampleCount,
                create(Type.ORDER_VALUE_QUERY));

        orderCalculatedValues =
            getClient().calculateOrderValues(sapQuote.getQuoteHeader().getQuoteNumber(), potentialOrderCriteria);
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
        Option notForValueQuery = create(isForValueQuery(false));
        return generateOrderCriteria(productOrder.getSapQuote(this), productOrder, 0, notForValueQuery);
    }

    protected OrderCriteria generateOrderCriteria(SapQuote sapQuote, ProductOrder productOrder, int addedSampleCount,
                                                  SapIntegrationService.Option orderOption) {

        final Set<SAPOrderItem> sapOrderItems = new HashSet<>();
        String sapOrderNumber=null;
        if (productOrder != null && productOrder.getProduct() != null) {
            SAPOrderItem orderItem = getOrderItem(sapQuote, productOrder, productOrder.getProduct(), addedSampleCount,
                orderOption);
            sapOrderItems.add(orderItem);

            for (ProductOrderAddOn productOrderAddOn : productOrder.getAddOns()) {
                final SAPOrderItem orderSubItem =
                    getOrderItem(sapQuote, productOrder, productOrderAddOn.getAddOn(), addedSampleCount,
                        orderOption);
                sapOrderItems.add(orderSubItem);
            }
            sapOrderNumber = productOrder.getSapOrderNumber();
        }
        OrderCriteria orderCriteria = new OrderCriteria(sapOrderNumber, sapOrderItems);
        return orderCriteria;
    }

    /**
     * Helper method to figure out which company code to associate with a given product order
     * @param companyProductOrder Product Order from which the company code is to be determined
     * @return an indicator that represents one of the configured companies within SAP
     */
    @Deprecated
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
