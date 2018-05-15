package org.broadinstitute.gpinformatics.athena.boundary.orders;


import com.google.common.base.Function;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import edu.mit.broad.prodinfo.bean.generated.AutoWorkRequestInput;
import edu.mit.broad.prodinfo.bean.generated.AutoWorkRequestOutput;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductOrderJiraUtil;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessItem;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.orders.SapOrderDetail;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingProductOrderMapping;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.presentation.orders.CustomizationValues;
import org.broadinstitute.gpinformatics.infrastructure.ValidationWithRollbackException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.BSPKitRequestService;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.IssueFieldsResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.Transition;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BadBusinessKeyException;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPInterfaceException;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapConfig;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnector;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.sap.entity.SAPMaterial;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder.OrderStatus;
import static org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample.DeliveryStatus;

@Stateful
@RequestScoped
/**
 * Transactional manager for {@link ProductOrder}s.
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ProductOrderEjb {

    private ProductOrderDao productOrderDao;

    private ProductDao productDao;

    private QuoteService quoteService;

    private JiraService jiraService;

    private UserBean userBean;

    private BSPUserList userList;

    private BucketEjb bucketEjb;

    private SquidConnector squidConnector;

    @Inject
    private BSPKitRequestService bspKitRequestService;

    private ProductOrderSampleDao productOrderSampleDao;

    private MercurySampleDao mercurySampleDao;

    private ProductOrderJiraUtil productOrderJiraUtil;

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    private SapIntegrationService sapService;

    private AppConfig appConfig;
    private SapConfig sapConfig;

    private EmailSender emailSender;

    private SAPAccessControlEjb accessController;

    private Deployment deployment;

    private PriceListCache priceListCache;

    private SAPProductPriceCache productPriceCache;

    // EJBs require a no arg constructor.
    @SuppressWarnings("unused")
    public ProductOrderEjb() {
    }

    @Inject
    public ProductOrderEjb(ProductOrderDao productOrderDao,
                           ProductDao productDao,
                           QuoteService quoteService,
                           JiraService jiraService,
                           UserBean userBean,
                           BSPUserList userList,
                           BucketEjb bucketEjb,
                           SquidConnector squidConnector,
                           MercurySampleDao mercurySampleDao,
                           ProductOrderJiraUtil productOrderJiraUtil,
                           SapIntegrationService sapService, PriceListCache priceListCache,
                           SAPProductPriceCache productPriceCache) {
        this.productOrderDao = productOrderDao;
        this.productDao = productDao;
        this.quoteService = quoteService;
        this.jiraService = jiraService;
        this.userBean = userBean;
        this.userList = userList;
        this.bucketEjb = bucketEjb;
        this.squidConnector = squidConnector;
        this.mercurySampleDao = mercurySampleDao;
        this.productOrderJiraUtil = productOrderJiraUtil;
        this.sapService = sapService;
        this.priceListCache = priceListCache;
        this.productPriceCache = productPriceCache;
    }

    private final Log log = LogFactory.getLog(ProductOrderEjb.class);

    /**
     * Remove all non-received samples from the order.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeNonReceivedSamples(ProductOrder editOrder,
                                         MessageReporter reporter)
            throws NoSuchPDOException, IOException, SAPInterfaceException {
        // Note that calling getReceivedSampleCount() will cause the sample data for all samples to be
        // fetched if it hasn't been already. This is good because without it each call to getSampleData() below
        // would fetch it one sample at a time.
        List<ProductOrderSample> samplesToRemove =
                new ArrayList<>(editOrder.getSampleCount() - editOrder.getReceivedSampleCount());
        for (ProductOrderSample sample : editOrder.getSamples()) {
            if (!sample.isSampleAvailable()) {
                samplesToRemove.add(sample);
            }
        }

        if (!samplesToRemove.isEmpty()) {
            removeSamples(editOrder.getJiraTicketKey(), samplesToRemove, reporter);
        }
    }

    /**
     * Persisting a Product order.  This method's primary job is to Support a call from the Product Order Action bean
     * to wrap the persistence of an order in a transaction.
     *
     * @param saveType            Indicates what type of persistence this is:  Save, Update, etc
     * @param editedProductOrder  The product order entity to be persisted
     * @param deletedIds          a collection that represents the ID's of productOrderKitDetails to be deleted form
     *                            the kit collection
     * @param kitDetailCollection a collection of product order details that have been created or updated from
     *                            the UI
     *
     * @throws IOException
     * @throws QuoteNotFoundException
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void persistProductOrder(ProductOrder.SaveType saveType, ProductOrder editedProductOrder,
                                    @Nonnull Collection<String> deletedIds,
                                    @Nonnull Collection<ProductOrderKitDetail> kitDetailCollection)
            throws IOException, QuoteNotFoundException, SAPInterfaceException {

        persistProductOrder(saveType, editedProductOrder, deletedIds, kitDetailCollection, Collections.<CustomizationValues>emptyList(), new MessageCollection());
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void persistProductOrder(ProductOrder.SaveType saveType, ProductOrder editedProductOrder,
                                    @Nonnull Collection<String> deletedIds,
                                    @Nonnull Collection<ProductOrderKitDetail> kitDetailCollection,
                                    List<CustomizationValues> customizationValues,
                                    MessageCollection messageCollection)
            throws IOException, QuoteNotFoundException, SAPInterfaceException {

        kitDetailCollection.removeAll(Collections.singleton(null));
        deletedIds.removeAll(Collections.singleton(null));

        editedProductOrder.updateCustomSettings(customizationValues);

        editedProductOrder.prepareToSave(userBean.getBspUser(), saveType);

        if (editedProductOrder.isDraft()) {
            if (editedProductOrder.isSampleInitiation()) {
                Map<Long, ProductOrderKitDetail> mapKitDetailsByIDs = new HashMap<>();
                Iterator<ProductOrderKitDetail> kitDetailIterator =
                        editedProductOrder.getProductOrderKit().getKitOrderDetails().iterator();
                while (kitDetailIterator.hasNext()) {
                    ProductOrderKitDetail kitDetail = kitDetailIterator.next();
                    if (deletedIds.contains(kitDetail.getProductOrderKitDetailId().toString())) {
                        kitDetailIterator.remove();
                    } else {
                        mapKitDetailsByIDs.put(kitDetail.getProductOrderKitDetailId(), kitDetail);
                    }
                }

                for (ProductOrderKitDetail kitDetailUpdate : kitDetailCollection) {
                    if (kitDetailUpdate.getProductOrderKitDetailId() != null &&
                        mapKitDetailsByIDs.containsKey(kitDetailUpdate.getProductOrderKitDetailId())) {

                        mapKitDetailsByIDs.get(kitDetailUpdate.getProductOrderKitDetailId()).updateDetailValues(
                                kitDetailUpdate);

                    } else {
                        editedProductOrder.getProductOrderKit().addKitOrderDetail(kitDetailUpdate);

                    }
                }
            }
        } else {
            updateJiraIssue(editedProductOrder);
            for (ProductOrder childProductOrder : editedProductOrder.getChildOrders()) {
                if(!childProductOrder.getOrderStatus().canPlace()) {
                    updateJiraIssue(childProductOrder);
                }
            }
            publishProductOrderToSAP(editedProductOrder, messageCollection, false);
        }
        attachMercurySamples(editedProductOrder.getSamples());
        for (ProductOrder childProductOrder : editedProductOrder.getChildOrders()) {
            attachMercurySamples(childProductOrder.getSamples());
        }

        productOrderDao.persist(editedProductOrder);
    }


    /**
     * Modified version of persistProductOrder to deal with the new concept of child ProductOrders introduced with the
     * SAP integration.  The main difference here is that the Parent order has persist directly called on it instead of
     * the child order to enable the cascades to correctly save the entities instead of getting confused.
     *
     * Whether the issues (GPLIM-4513, GPLIM-4514) that led to this being implemented are a Hibernate bug or an
     * implementation issue is not known now, but should continue to be investigated.  Hopefully the upgrade to Java 8
     * and Wildfly will show some improvement on this
     *
     * @param saveType indicates what state the ProductOrder is in when this method is called
     * @param editedProductOrder
     * @param messageCollection
     * @throws IOException
     * @throws QuoteNotFoundException
     * @throws SAPInterfaceException
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void persistClonedProductOrder(ProductOrder.SaveType saveType, ProductOrder editedProductOrder,
                                    MessageCollection messageCollection)
            throws IOException, QuoteNotFoundException, SAPInterfaceException {


        editedProductOrder.prepareToSave(userBean.getBspUser(), saveType);

        if (!editedProductOrder.isDraft()) {
            updateJiraIssue(editedProductOrder);

            publishProductOrderToSAP(editedProductOrder, messageCollection, false);
        }
        attachMercurySamples(editedProductOrder.getSamples());

        productOrderDao.persist(editedProductOrder.getParentOrder());
    }

    /**
     * Takes care of the logic to publish the Product Order to SAP for the purposes of either creating or updating
     * an SAP order.  This must be done in order to directly bill to SAP from mercury
     *
     * @param editedProductOrder Product order entity which intends to be reflected in SAP
     * @param messageCollection  Storage for error/success messages that happens during the publishing process
     * @param allowCreateOrder   Helper flag to know indicate if the scenario by which the method is called intends to
     *                           allow a new order to be replaced (e.g. an order previously was associated with an SAP
     *                           order but needs a new one)
     *
     * @throws SAPInterfaceException
     */
    public void publishProductOrderToSAP(ProductOrder editedProductOrder, MessageCollection messageCollection,
                                         boolean allowCreateOrder) throws SAPInterfaceException {
         publishProductOrderToSAP(editedProductOrder, messageCollection, allowCreateOrder, new Date());
    }

    /**
     * Takes care of the logic to publish the Product Order to SAP for the purposes of either creating or updating
     * an SAP order.  This must be done in order to directly bill to SAP from mercury
     *
     * @param editedProductOrder Product order entity which intends to be reflected in SAP
     * @param messageCollection  Storage for error/success messages that happens during the publishing process
     * @param allowCreateOrder   Helper flag to know indicate if the scenario by which the method is called intends to
     *                           allow a new order to be replaced (e.g. an order previously was associated with an SAP
     *                           order but needs a new one)
     * @param effectiveDate
     *
     * @throws SAPInterfaceException
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void publishProductOrderToSAP(ProductOrder editedProductOrder, MessageCollection messageCollection,
                                         boolean allowCreateOrder, Date effectiveDate) throws SAPInterfaceException {
        ProductOrder orderToPublish = editedProductOrder;

        final List<Product> allProductsOrdered = ProductOrder.getAllProductsOrdered(orderToPublish);
        if(editedProductOrder.getParentOrder() != null && editedProductOrder.getSapOrderNumber() != null) {
            orderToPublish = editedProductOrder.getParentOrder();
        }
        if (!areProductsOnOrderBlocked(orderToPublish)) {
            try {
                if (isOrderEligibleForSAP(orderToPublish, effectiveDate)
                    && !orderToPublish.getOrderStatus().canPlace()) {
                    Quote quote = orderToPublish.getQuote(quoteService);
                    final List<String> effectivePricesForProducts = productPriceCache
                            .getEffectivePricesForProducts(allProductsOrdered,editedProductOrder, quote);

                    final boolean quoteIdChange = orderToPublish.isSavedInSAP() &&
                                                  !orderToPublish.getQuoteId()
                                                          .equals(orderToPublish.latestSapOrderDetail().getQuoteId());

                    boolean priceChangeForNewOrder = false;
                    if(orderToPublish.isSavedInSAP() && orderToPublish.isPriorToSAP1_5()) {
                        priceChangeForNewOrder = !StringUtils.equals(orderToPublish.latestSapOrderDetail().getOrderPricesHash(),
                                TubeFormation.makeDigest(StringUtils.join(effectivePricesForProducts, ",")))
                                                 && orderToPublish.hasAtLeastOneBilledLedgerEntry();
                    }

                    if ((!orderToPublish.isSavedInSAP() && allowCreateOrder) || quoteIdChange || priceChangeForNewOrder) {
                        final String newSapOrderNumber = createOrderInSAP(orderToPublish, quoteIdChange,allProductsOrdered,
                                effectivePricesForProducts, messageCollection, priceChangeForNewOrder, true);

                        // Create orders for any Child orders that does not share
                        for (ProductOrder childOrder : orderToPublish.getChildOrders()) {
                            if(!ProductOrder.sharesSAPOrderWithParent(childOrder, newSapOrderNumber)) {
                                createOrderInSAP(childOrder, quoteIdChange, allProductsOrdered, effectivePricesForProducts,
                                        messageCollection, priceChangeForNewOrder, true);
                            }
                        }

                } else if(orderToPublish.isSavedInSAP()){

                        updateOrderInSap(orderToPublish, allProductsOrdered, effectivePricesForProducts, messageCollection,
                            CollectionUtils.containsAny(Arrays.asList(OrderStatus.Abandoned, OrderStatus.Completed),
                                    Collections.singleton(orderToPublish.getOrderStatus()))
                            && !orderToPublish.isPriorToSAP1_5());

                        for (ProductOrder childProductOrder : orderToPublish.getChildOrders()) {

                            if (childProductOrder.isSubmitted() &&
                                !StringUtils.equals(childProductOrder.getSapOrderNumber(),
                                        orderToPublish.getSapOrderNumber())) {

                                updateOrderInSap(childProductOrder, allProductsOrdered, effectivePricesForProducts,
                                        messageCollection,
                            CollectionUtils.containsAny(Arrays.asList(OrderStatus.Abandoned,
                                            OrderStatus.Completed),
                                            Collections.singleton(orderToPublish.getOrderStatus()))
                                    && !orderToPublish.isPriorToSAP1_5());
                        }
                    }
                }
                productOrderDao.persist(orderToPublish);
            } else {
                final String inelligiblOrderError = "This order is ineligible to post to SAP: ";
                if(orderToPublish.isSavedInSAP()) {
                    throw new SAPInterfaceException(inelligiblOrderError);
                } else {
                    messageCollection.addInfo(inelligiblOrderError);
                }
            }
        } catch (SAPIntegrationException | QuoteServerException | QuoteNotFoundException | InvalidProductException e) {
            StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Unable to ");
            if (!orderToPublish.isSavedInSAP()) {
                errorMessage.append("create ");
            } else {
                errorMessage.append("update ");
            }
            errorMessage.append("this order in SAP at this point in time: ").append(e.getMessage());
            messageCollection.addError(errorMessage.toString());
            log.error(errorMessage, e);
            if(orderToPublish.isSavedInSAP()) {
                throw new SAPInterfaceException(errorMessage.toString(), e);}
            }
        }
    }

    /**
     * Encapsulates the series of calls to make when updating an order in SAP.  This is created to support updating
     * the child orders of a product order for which abandoned samples have been replaced.
     *
     * @param orderToUpdate     A product order from which an SAP order is to be updated
     * @param messageCollection To collect any errors or success messages that may occur during the process
     * @param closingOrder
     * @throws SAPIntegrationException
     */
    private void updateOrderInSap(ProductOrder orderToUpdate, List<Product> allProductsOrdered,
                                  List<String> effectivePricesForProducts, MessageCollection messageCollection,
                                  boolean closingOrder)
            throws SAPIntegrationException {
        sapService.updateOrder(orderToUpdate, closingOrder);
        BigDecimal sampleCount = BigDecimal.ZERO ;
        if(orderToUpdate.isPriorToSAP1_5()) {
            sampleCount = SapIntegrationServiceImpl.getSampleCount(orderToUpdate,
                    orderToUpdate.getProduct(), 0, false, closingOrder);
        }
        orderToUpdate.updateSapDetails(sampleCount.intValue(),
                TubeFormation.makeDigest(StringUtils.join(allProductsOrdered, ",")),
                TubeFormation.makeDigest(StringUtils.join(effectivePricesForProducts, ",")));
        messageCollection.addInfo("Order "+orderToUpdate.getJiraTicketKey() +
                                  " has been successfully updated in SAP");

    }

    /**
     * This method contains the specific logic surrounding the creation and persistence of an SAP order
     * @param priceChangeForNewOrder
     * @param closingOrder
     * @param orderToPublish  The Product Order from which a new SAP order will be created
     * @param quoteIdChange   Indicates that creating the SAP order is as a result of the Quote being changed
     * @return  The order ID of the newly created SAP Order
     * @throws SAPIntegrationException
     */
    private String createOrderInSAP(ProductOrder orderToPublish, boolean quoteIdChange,
                                    List<Product> allProductsOrdered,
                                    List<String> effectivePricesForProducts,
                                    MessageCollection messageCollection, boolean priceChangeForNewOrder,
                                    boolean closingOrder)
            throws SAPIntegrationException {

        if(closingOrder && orderToPublish.isSavedInSAP()) {
            updateOrderInSap(orderToPublish, allProductsOrdered, effectivePricesForProducts, messageCollection,
                    closingOrder);
        }

        String sapOrderIdentifier = sapService.createOrder(orderToPublish);

        String oldNumber = null;
        if(StringUtils.isNotBlank(orderToPublish.getSapOrderNumber())) {
            oldNumber = orderToPublish.getSapOrderNumber();
        }
        orderToPublish.addSapOrderDetail(new SapOrderDetail(sapOrderIdentifier,0,
                orderToPublish.getQuoteId(),
                SapIntegrationServiceImpl.determineCompanyCode(orderToPublish).getCompanyCode(),
                TubeFormation.makeDigest(StringUtils.join(allProductsOrdered, ",")),
                TubeFormation.makeDigest(StringUtils.join(effectivePricesForProducts, ","))));

        if(quoteIdChange || priceChangeForNewOrder) {
            String body = "The SAP order " + oldNumber + " for PDO "+ orderToPublish.getBusinessKey()+
                          " is being associated with a new quote by "+
                          userBean.getBspUser().getFullName() +" and needs" + " to be short closed.";
            for (ProductOrder childOrder : orderToPublish.getChildOrders()) {
                if(ProductOrder.sharesSAPOrderWithParent(childOrder, oldNumber)) {
                    childOrder.addSapOrderDetail(orderToPublish.latestSapOrderDetail());
                }
            }

//            sendSapOrderShortCloseRequest(body);
        }
        orderToPublish.setPriorToSAP1_5(false);
        messageCollection.addInfo("Order "+orderToPublish.getJiraTicketKey() +
                                  " has been successfully created in SAP");

        return sapOrderIdentifier;
    }

    /**
     * Helper method to determine if, based on certain criteria, the order is allowed to be pushed to SAP at the time
     * that the method is called.
     *
     * @param editedProductOrder The order to be tested for SAP eligibility
     * @return Boolean indicator identifying SAP eligibility
     * @throws QuoteServerException
     * @throws QuoteNotFoundException
     */
    public boolean isOrderEligibleForSAP(ProductOrder editedProductOrder)
            throws QuoteServerException, QuoteNotFoundException, InvalidProductException {
        return isOrderEligibleForSAP(editedProductOrder, new Date());
    }
    /**
     * Helper method to determine if, based on certain criteria, the order is allowed to be pushed to SAP at the time
     * that the method is called.
     *
     * @param editedProductOrder The order to be tested for SAP eligibility
     * @param effectiveDate
     * @return Boolean indicator identifying SAP eligibility
     * @throws QuoteServerException
     * @throws QuoteNotFoundException
     */
    public boolean isOrderEligibleForSAP(ProductOrder editedProductOrder, Date effectiveDate)
            throws QuoteServerException, QuoteNotFoundException, InvalidProductException {
        Quote orderQuote = editedProductOrder.getQuote(quoteService);
        SAPAccessControl accessControl = accessController.getCurrentControlDefinitions();
        boolean eligibilityResult = false;

        Set<AccessItem> priceItemNameList = new HashSet<>();

        final boolean priceItemsValid = areProductPricesValid(editedProductOrder, priceItemNameList, orderQuote);

        if(orderQuote != null && accessControl.isEnabled()) {

            eligibilityResult = editedProductOrder.getProduct()!=null &&
                                editedProductOrder.getProduct().getPrimaryPriceItem() != null &&
                                orderQuote != null && orderQuote.isEligibleForSAP(effectiveDate) &&
                                !CollectionUtils.containsAny(accessControl.getDisabledItems(), priceItemNameList) ;
        }

        if(eligibilityResult && !priceItemsValid) {
            throw new InvalidProductException("One of the Price items associated with " +
                                              editedProductOrder.getBusinessKey() + ": " +
                                              editedProductOrder.getName() + " is invalid");
        }
        return eligibilityResult;
    }

    private boolean areProductsOnOrderBlocked(ProductOrder targetOrder) {
        Set<AccessItem> priceItemNameList = new HashSet<>();

        if(targetOrder.getProduct() != null) {
            priceItemNameList.add(new AccessItem(targetOrder.getProduct().getPrimaryPriceItem().getName()));
        }
        for (ProductOrderAddOn productOrderAddOn : targetOrder.getAddOns()) {
            priceItemNameList.add(new AccessItem(productOrderAddOn.getAddOn().getPrimaryPriceItem().getName()));
        }

        return areProductsBlocked(priceItemNameList);

    }

    public boolean areProductsBlocked(Set<AccessItem> priceItemNameList) {
        SAPAccessControl accessControl = accessController.getCurrentControlDefinitions();
        return !accessControl.isEnabled() ||
               CollectionUtils.containsAny(accessControl.getDisabledItems(), priceItemNameList);
    }

    public boolean areProductPricesValid(ProductOrder editedProductOrder, Set<AccessItem> priceItemNameList,
                                         Quote orderQuote)
            throws InvalidProductException {
        Set<Product> productListFromOrder = new HashSet<>();
        if (editedProductOrder.getProduct() != null) {
            productListFromOrder.add(editedProductOrder.getProduct());
            PriceItem priceItem = editedProductOrder.determinePriceItemByCompanyCode(editedProductOrder.getProduct());
            priceItemNameList.add(new AccessItem(priceItem.getName()));
        }
        for (ProductOrderAddOn productOrderAddOn : editedProductOrder.getAddOns()) {
            productListFromOrder.add(productOrderAddOn.getAddOn());
            PriceItem addonPricItem = editedProductOrder.determinePriceItemByCompanyCode(productOrderAddOn.getAddOn());
            priceItemNameList.add(new AccessItem(addonPricItem.getName()));
        }
        return determineProductAndPriceItemValidity(productListFromOrder, orderQuote, editedProductOrder);
    }

    private boolean determineProductAndPriceItemValidity(Collection<Product> productsToConsider,
                                                         Quote orderQuote, ProductOrder productOrder) throws InvalidProductException {
        boolean allItemsValid = true;
        QuotePriceItem primaryPriceItem;
        if (CollectionUtils.isNotEmpty(productsToConsider)) {
            for (Product product : productsToConsider) {
                try {
                    PriceItem priceItem = productOrder.determinePriceItemByCompanyCode(product);
                    
                    primaryPriceItem =
                            priceListCache.findByKeyFields(priceItem.getPlatform(),
                                    priceItem.getCategory(),
                                    priceItem.getName());
                    if (primaryPriceItem == null) {
                        allItemsValid = false;
                        break;
                    }
                } catch (Exception e) {
                    allItemsValid = false;
                    break;
                }

                validateSAPAndQuoteServerPrices(orderQuote, product, productOrder);
            }

        }
        return allItemsValid;
    }

    public String validateSAPAndQuoteServerPrices(Quote orderQuote, Product product,
                                                  ProductOrder productOrder)
            throws InvalidProductException {
        SAPMaterial sapMaterial = null;
        SapIntegrationClientImpl.SAPCompanyConfiguration companyCode = null;

        SAPAccessControl accessControl = accessController.getCurrentControlDefinitions();

        try {
            companyCode = SapIntegrationServiceImpl.determineCompanyCode(productOrder);
        } catch (SAPIntegrationException e) {
            throw new InvalidProductException(e);
        }

        // todo sgm check quote and throw exception if it is null

        if(accessControl.isEnabled() &&
           !CollectionUtils.containsAny(accessControl.getDisabledItems(),
                   Collections.singleton(new AccessItem(product.getPrimaryPriceItem().getName())))) {
            sapMaterial = productPriceCache.findByProduct(product, companyCode);
        }

        PriceItem priceItem = productOrder.determinePriceItemByCompanyCode(product);
        final QuotePriceItem priceListItem = priceListCache.findByKeyFields(priceItem);
        if (priceListItem != null) {
            final BigDecimal effectivePrice = new BigDecimal(priceListItem.getPrice());
            if (sapMaterial != null && StringUtils.isNotBlank(sapMaterial.getBasePrice())) {
                final BigDecimal basePrice = new BigDecimal(sapMaterial.getBasePrice());
                if (basePrice.compareTo(effectivePrice) != 0) {
                    throw new InvalidProductException("Unable to continue since the price for the product " +
                                                      product.getDisplayName() + " has not been properly set up in SAP");
                }
            }
        } else {
            throw new InvalidProductException("Unable to continue since the price list item " +
                                              priceItem.getDisplayName() + " for " + product.getDisplayName() +
                                              " is invalid.");
        }
        return priceListCache.getEffectivePrice(priceItem, orderQuote);
    }

    /**
     * Looks up the quote for the pdo (if the pdo has one) in the
     * quote server.
     */
    void validateQuote(ProductOrder productOrder, QuoteService quoteService) throws QuoteNotFoundException {
        if (!StringUtils.isEmpty(productOrder.getQuoteId())) {
            try {
                productOrder.getQuote(quoteService);
            } catch (QuoteServerException e) {
                throw new RuntimeException("Failed to find quote for " + productOrder.getQuoteId(), e);
            }
        }
    }

    /**
     * Calculate the risk for all samples on the product order specified by business key.
     *
     * @param productOrderKey The product order business key.
     *
     * @throws Exception Any errors in reporting the risk.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void calculateRisk(String productOrderKey) throws Exception {
        calculateRisk(productOrderKey, null);
    }

    /**
     * Calculate the risk for all samples on the product order specified by business key. If the samples
     * parameter is null, then all samples on the order will be used.
     *
     * @param productOrderKey The product order business key.
     * @param samples         The samples to calculate risk on. Null if all.
     *
     * @throws Exception Any errors in reporting the risk.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void calculateRisk(String productOrderKey, List<ProductOrderSample> samples) throws Exception {
        ProductOrder editOrder = productOrderDao.findByBusinessKey(productOrderKey);
        if (editOrder == null) {
            String message = MessageFormat.format("Invalid PDO key ''{0}'', for calculating risk.", productOrderKey);
            throw new BadBusinessKeyException(message);
        }

        try {
            // If null, then it will calculate for all samples.
            if (samples == null) {
                editOrder.calculateRisk();
            } else {
                editOrder.calculateRisk(samples);
            }
        } catch (Exception ex) {
            String message = "Could not calculate risk.";
            log.error(message, ex);
            throw new InformaticsServiceException(message, ex);
        }

        // Set the create and modified information.
        editOrder.prepareToSave(userBean.getBspUser());
    }

    /**
     * Manually update risk of samples in a product order
     *
     * @param user            User performing action
     * @param productOrderKey Key of project being updated.
     * @param orderSamples    Samples who's risk is to be manually updated.
     * @param riskStatus      New risk status
     * @param riskComment     Comment of reason why samples' risk is being updated
     *
     * @throws IOException
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void addManualOnRisk(@Nonnull BspUser user, @Nonnull String productOrderKey,
                                List<ProductOrderSample> orderSamples, boolean riskStatus, @Nonnull String riskComment)
            throws IOException {
        ProductOrder editOrder = productOrderDao.findByBusinessKey(productOrderKey);

        // If we are creating a manual on risk, then need to set it up and persist it for reuse.
        RiskCriterion criterion = null;
        if (riskStatus) {
            criterion = RiskCriterion.createManual();
            productOrderDao.persist(criterion);
        }

        for (ProductOrderSample sample : orderSamples) {
            if (riskStatus) {
                sample.setManualOnRisk(criterion, riskComment);
            } else {
                sample.setManualNotOnRisk(riskComment);
            }
        }

        // Set the create and modified information.
        editOrder.prepareToSave(user);

        // Add comment about the risk status to jira.
        updateJiraCommentForRisk(productOrderKey, user, riskComment, orderSamples.size(), riskStatus);
    }

    /**
     * Set the Proceed if Out of Spec indicator.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void proceedOos(@Nonnull BspUser user, @Nonnull List<ProductOrderSample> orderSamples,
                           @Nonnull ProductOrder productOrder,
                           @Nonnull ProductOrderSample.ProceedIfOutOfSpec proceedIfOutOfSpec) {

        for (ProductOrderSample orderSample : orderSamples) {
            orderSample.setProceedIfOutOfSpec(proceedIfOutOfSpec);
        }
        productOrder.prepareToSave(user);
        // Recruit the persistence context into the transaction
        productOrderDao.flush();
    }

    /**
     * Post a comment in jira about risk.
     *
     * @param jiraKey     Key of jira issue to add comment to.
     * @param comment     User supplied comment on change of risk for samples
     * @param sampleCount number of effected samples
     * @param isRisk      new risk of samples
     *
     * @throws IOException
     */
    private void updateJiraCommentForRisk(@Nonnull String jiraKey, @Nonnull BspUser bspUser, @Nonnull String comment,
                                          int sampleCount, boolean isRisk) throws IOException {
        String issueComment = buildJiraCommentForRiskString(comment, bspUser.getUsername(), sampleCount, isRisk);
        jiraService.addComment(jiraKey, issueComment);
    }

    String buildJiraCommentForRiskString(String comment, String username, int sampleCount, boolean isRisk) {
        return String.format("%s set manual on risk to %s for %d samples with comment:\n%s",
                username, isRisk, sampleCount, comment);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void handleSamplesAdded(@Nonnull String productOrderKey, @Nonnull Collection<ProductOrderSample> newSamples,
                                   @Nonnull MessageReporter reporter) {
        ProductOrder order = productOrderDao.findByBusinessKey(productOrderKey);
        // Only add samples to the LIMS bucket if the order is ready for lab work.
        if (order.getOrderStatus().readyForLab()) {
            Map<String, Collection<ProductOrderSample>> samples = bucketEjb.addSamplesToBucket(order, newSamples,
                    ProductWorkflowDefVersion.BucketingSource.PDO_SUBMISSION);
            if (!samples.isEmpty()) {
                Set<String> bucketedSampleNames = new HashSet<>();
                for (Map.Entry<String, Collection<ProductOrderSample>> bucketSampleEntry : samples.entrySet()) {
                    String bucketName = bucketSampleEntry.getKey();
                    bucketedSampleNames.addAll(ProductOrderSample.getSampleNames(bucketSampleEntry.getValue()));
                    bucketName = bucketName.toLowerCase().endsWith("bucket") ? bucketName : bucketName + " Bucket";
                    reporter.addMessage("{0} samples have been added to the {1}.",
                            bucketSampleEntry.getValue().size(), bucketName);
                }

                Collection<String> unBucketedSamples =
                        CollectionUtils.subtract(ProductOrderSample.getSampleNames(newSamples), bucketedSampleNames);
                if (!unBucketedSamples.isEmpty()) {
                    reporter.addMessage("No valid buckets found {0}", unBucketedSamples);
                }
            }
        }
    }

    /**
     * Update the JIRA issue, executing the 'Developer Edit' transition to effect edits of fields that are read-only
     * on the UI.  Add a comment to the issue to indicate what was changed and by whom.
     *
     * @param productOrder Product Order.
     *
     * @throws IOException
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateJiraIssue(ProductOrder productOrder) throws IOException, QuoteNotFoundException {
        validateQuote(productOrder, quoteService);

        Transition transition = jiraService.findAvailableTransitionByName(productOrder.getJiraTicketKey(),
                JiraTransition.DEVELOPER_EDIT.getStateName());

        List<PDOUpdateField> pdoUpdateFields = new ArrayList<>(Arrays.asList(
                new PDOUpdateField(ProductOrder.JiraField.PRODUCT, productOrder.getProduct().getProductName()),
                new PDOUpdateField(ProductOrder.JiraField.PRODUCT_FAMILY,
                        productOrder.getProduct().getProductFamily().getName()),
                new PDOUpdateField(ProductOrder.JiraField.SAMPLE_IDS, productOrder.getSampleString(), true),
                new PDOUpdateField(ProductOrder.JiraField.REPORTER,
                        new CreateFields.Reporter(userList.getById(productOrder.getCreatedBy())
                                .getUsername()))));

        if (productOrder.getProduct().getSupportsNumberOfLanes()) {
            pdoUpdateFields.add(
                    new PDOUpdateField(ProductOrder.JiraField.LANES_PER_SAMPLE, productOrder.getLaneCount()));
        }

        pdoUpdateFields.add(new PDOUpdateField(ProductOrder.JiraField.SUMMARY, productOrder.getTitle()));

        pdoUpdateFields.add(PDOUpdateField.createPDOUpdateFieldForQuote(productOrder));

        List<String> addOnList = new ArrayList<>(productOrder.getAddOns().size());
        for (ProductOrderAddOn addOn : productOrder.getAddOns()) {
            addOnList.add(addOn.getAddOn().getDisplayName());
        }
        Collections.sort(addOnList);
        pdoUpdateFields.add(new PDOUpdateField(ProductOrder.JiraField.ADD_ONS, StringUtils.join(addOnList, "\n")));

        if (!StringUtils.isBlank(productOrder.getComments())) {
            pdoUpdateFields.add(new PDOUpdateField(ProductOrder.JiraField.DESCRIPTION, productOrder.getComments()));
        }

        if (productOrder.getFundingDeadline() == null) {
            pdoUpdateFields.add(PDOUpdateField.clearedPDOUpdateField(ProductOrder.JiraField.FUNDING_DEADLINE));
        } else {
            pdoUpdateFields.add(new PDOUpdateField(ProductOrder.JiraField.FUNDING_DEADLINE,
                    JiraService.JIRA_DATE_FORMAT.format(
                            productOrder.getFundingDeadline())));
        }

        if (productOrder.getPublicationDeadline() == null) {
            pdoUpdateFields.add(
                    PDOUpdateField.clearedPDOUpdateField(ProductOrder.JiraField.PUBLICATION_DEADLINE));
        } else {
            pdoUpdateFields.add(new PDOUpdateField(ProductOrder.JiraField.PUBLICATION_DEADLINE,
                    JiraService.JIRA_DATE_FORMAT.format(
                            productOrder.getPublicationDeadline())));
        }

        String[] customFieldNames = new String[pdoUpdateFields.size()];

        int i = 0;
        for (PDOUpdateField pdoUpdateField : pdoUpdateFields) {
            customFieldNames[i++] = pdoUpdateField.getDisplayName();
        }

        Map<String, CustomFieldDefinition> customFieldDefinitions = jiraService.getCustomFields(customFieldNames);

        IssueFieldsResponse issueFieldsResponse =
                jiraService.getIssueFields(productOrder.getJiraTicketKey(), customFieldDefinitions.values());

        List<CustomField> customFields = new ArrayList<>();

        StringBuilder updateCommentBuilder = new StringBuilder();

        for (PDOUpdateField field : pdoUpdateFields) {
            String message = field.getUpdateMessage(productOrder, customFieldDefinitions, issueFieldsResponse);
            if (!message.isEmpty()) {
                customFields.add(field.createCustomField(customFieldDefinitions));
                updateCommentBuilder.append(message);
            }
        }
        String updateComment = updateCommentBuilder.toString();

        // If we detect from the comment that nothing has changed, make a note of that.  The user may have changed
        // something in the PDO that is not reflected in JIRA, like add-ons.
        String comment = "\n" + productOrder.getJiraTicketKey() + " was edited by "
                         + userBean.getLoginUserName() + "\n\n"
                         + (updateComment.isEmpty() ? "No JIRA Product Order fields were updated\n\n" : updateComment);

        jiraService.postNewTransition(productOrder.getJiraTicketKey(), transition, customFields, comment);
    }

    /**
     * Allow updated quotes, products, and add-ons.
     *
     * @param productOrder     product order
     * @param addOnPartNumbers add-on part numbers
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void update(ProductOrder productOrder, List<String> addOnPartNumbers) throws QuoteNotFoundException {
        // update JIRA ticket with new quote
        // GPLIM-488
        try {
            updateJiraIssue(productOrder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // In the PDO edit UI, if the user goes through and edits the quote and then hits 'Submit', this works
        // without the merge.  But if the user tabs out of the quote field before hitting 'Submit', this merge
        // is required because this method receives a detached ProductOrder instance.

        // Also we would get a detached instance after quote validation failures GPLIM-488
        ProductOrder updatedProductOrder = productOrderDao.getEntityManager().merge(productOrder);

        // update add-ons, first remove old
        for (ProductOrderAddOn productOrderAddOn : updatedProductOrder.getAddOns()) {
            productOrderDao.remove(productOrderAddOn);
        }

        // set new add-ons in
        Set<ProductOrderAddOn> productOrderAddOns = new HashSet<>();
        for (Product addOn : productDao.findByPartNumbers(addOnPartNumbers)) {
            ProductOrderAddOn productOrderAddOn = new ProductOrderAddOn(addOn, updatedProductOrder);
            productOrderDao.persist(productOrderAddOn);
            productOrderAddOns.add(productOrderAddOn);
        }

        updatedProductOrder.setProductOrderAddOns(productOrderAddOns);

    }

    public static class NoSuchPDOException extends Exception {
        private static final long serialVersionUID = -5418019063691592665L;

        public NoSuchPDOException(String s) {
            super(s);
        }
    }

    public static class SampleDeliveryStatusChangeException extends Exception {
        private static final long serialVersionUID = 8651172992194864707L;

        protected SampleDeliveryStatusChangeException(DeliveryStatus targetStatus,
                                                      @Nonnull List<ProductOrderSample> samples) {
            super(createErrorMessage(targetStatus, samples));
        }

        protected static String createErrorMessage(DeliveryStatus status,
                                                   List<ProductOrderSample> samples) {
            List<String> messages = new ArrayList<>();

            for (ProductOrderSample sample : samples) {
                messages.add(sample.getName() + " @ " + sample.getSamplePosition()
                             + " : current status " + (StringUtils.isNotBlank(
                        sample.getDeliveryStatus().getDisplayName()) ? sample.getDeliveryStatus().getDisplayName() :
                        "Not Started"));
            }

            return "Cannot transition samples to status " + (StringUtils.isNotBlank(status.getDisplayName()) ?
                    status.getDisplayName() : "Not Started") + ": " + StringUtils.join(messages, ", ");
        }
    }

    /**
     * Utility method to find PDO by JIRA ticket key and throw exception if it is not found.
     *
     * @param jiraTicketKey JIRA ticket key.
     *
     * @return {@link ProductOrder}
     *
     * @throws NoSuchPDOException
     */
    @Nonnull
    private ProductOrder findProductOrder(@Nonnull String jiraTicketKey) throws NoSuchPDOException {
        ProductOrder productOrder = productOrderDao.findByBusinessKey(jiraTicketKey);

        if (productOrder == null) {
            throw new NoSuchPDOException(jiraTicketKey);
        }

        return productOrder;
    }


    /**
     * THis should have transactionAttributeType.REQUIRED in order to work.  It was removed because it is causing an
     * undesired side effect of saving the Product order on ProductOrderActionBean before it should.  Will need to
     * figure out another locking strategey for all entities that is more generic.
     *
     * @param jiraTicket
     * @param fetchSpecs
     * @return
     */
    public ProductOrder findProductOrderByBusinessKeySafely(@Nonnull String jiraTicket,
                                                            ProductOrderDao.FetchSpec... fetchSpecs) {
        return productOrderDao.findByBusinessKey(jiraTicket, LockModeType.PESSIMISTIC_READ, fetchSpecs);
    }

    /**
     * Transition the delivery statuses of the specified samples in the DB.
     *
     * @param order                      PDO containing the samples in question.
     * @param acceptableStartingStatuses A Set of {@link DeliveryStatus}es
     *                                   in which samples are allowed to be before undergoing this transition.
     * @param targetStatus               The status into which the samples will be transitioned.
     * @param samples                    The samples in question.
     *
     * @throws SampleDeliveryStatusChangeException Thrown if any samples are found to not be in an acceptable starting status.
     */
    private void transitionSamples(ProductOrder order,
                                   Set<ProductOrderSample.DeliveryStatus> acceptableStartingStatuses,
                                   DeliveryStatus targetStatus,
                                   Collection<ProductOrderSample> samples) throws SampleDeliveryStatusChangeException {

        Set<ProductOrderSample> transitionSamples = new HashSet<>(samples);

        List<ProductOrderSample> untransitionableSamples = new ArrayList<>();

        for (ProductOrderSample sample : order.getSamples()) {
            // If the transition sample set is empty we try to transition all samples in the PDO.
            if (CollectionUtils.isEmpty(transitionSamples) || transitionSamples.contains(sample)) {
                if (!acceptableStartingStatuses.contains(sample.getDeliveryStatus())) {
                    untransitionableSamples.add(sample);
                    // Keep looping, find all the untransitionable samples and then throw a descriptive exception.
                } else {
                    sample.setDeliveryStatus(targetStatus);
                }
            }
        }

        if (!untransitionableSamples.isEmpty()) {
            throw new SampleDeliveryStatusChangeException(targetStatus, untransitionableSamples);
        }

        // Update the PDO as modified.
        order.prepareToSave(userBean.getBspUser());
    }

    /**
     * Transition the specified samples to the specified target status, adding a comment to the JIRA ticket, does NOT
     * transition the JIRA ticket status as this is called from sample transition methods only and not whole PDO transition
     * methods.  Per GPLIM-655 we need to update per-sample comments too, but not currently doing that.
     *
     * @param jiraTicketKey              JIRA ticket key.
     * @param acceptableStartingStatuses Acceptable staring statuses for samples.
     * @param targetStatus               New status for samples.
     * @param samples                    Samples to change.
     * @param comment                    optional user supplied comment about this action.
     *
     * @param messageCollection
     * @throws NoSuchPDOException
     * @throws SampleDeliveryStatusChangeException
     * @throws IOException
     */
    private void transitionSamplesAndUpdateTicket(String jiraTicketKey,
                                                  Set<DeliveryStatus> acceptableStartingStatuses,
                                                  DeliveryStatus targetStatus,
                                                  Collection<ProductOrderSample> samples, String comment,
                                                  MessageCollection messageCollection)
            throws NoSuchPDOException, SampleDeliveryStatusChangeException, IOException, SAPInterfaceException {
        ProductOrder order = findProductOrder(jiraTicketKey);

        transitionSamples(order, acceptableStartingStatuses, targetStatus, samples);

        try {
            publishProductOrderToSAP(order, messageCollection, false);
        } catch (SAPInterfaceException e) {
            log.error("SAP Error when attempting to abandon samples", e);
            throw e;
        }

        JiraIssue issue = jiraService.getIssue(order.getJiraTicketKey());
        issue.addComment(MessageFormat.format("{0} transitioned samples to status {1}: {2}\n\n{3}",
                getUserName(), targetStatus.getDisplayName(),
                StringUtils.join(ProductOrderSample.getSampleNames(samples), ","),
                StringUtils.stripToEmpty(comment)));
    }

    /**
     * @return The name of the currently logged-in user or 'Mercury' if no logged in user (e.g. in a fixup test context).
     */
    private String getUserName() {
        String user = userBean.getLoginUserName();
        return user == null ? "Mercury" : user;
    }

    /**
     * JIRA Transition states used by PDOs.
     */
    public enum JiraTransition {
        ORDER_COMPLETE("Order Complete"),
        OPEN("Open"),
        CLOSED("Closed"),
        CANCEL("Cancel"),
        COMPLETE_ORDER("Complete Order"),
        CREATE_WORK_REQUEST("Create Work Request"),
        DEVELOPER_EDIT("Developer Edit");

        /**
         * The text that represents this transition state in JIRA.
         */
        private final String stateName;

        JiraTransition(String stateName) {
            this.stateName = stateName;
        }

        public String getStateName() {
            return stateName;
        }
    }

    /**
     * JIRA Resolutions used by PDOs.
     */
    enum JiraResolution {
        UNRESOLVED("Unresolved"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled");

        /**
         * The text that represents this resolution in JIRA.
         */
        private final String text;

        JiraResolution(String text) {
            this.text = text;
        }

        static JiraResolution fromString(String text) {
            for (JiraResolution status : values()) {
                if (status.text.equalsIgnoreCase(text)) {
                    return status;
                }
            }
            return null;
        }
    }

    /**
     * JIRA Status used by PDOs. Each status contains two transitions. One will transition to Open, one to Closed.
     */
    protected enum JiraStatus {
        PENDING("Pending", JiraTransition.OPEN, JiraTransition.CLOSED),
        OPEN("Open", null, JiraTransition.COMPLETE_ORDER),
        // This status is only set in the JIRA UI. In Mercury it has the same behavior as Open.
        WORK_REQUEST_CREATED("Work Request Created", null, JiraTransition.ORDER_COMPLETE),
        CLOSED("Closed", JiraTransition.OPEN, null),
        REOPENED("Reopened", JiraTransition.OPEN, JiraTransition.CLOSED),
        CANCELLED("Cancelled", JiraTransition.OPEN, JiraTransition.CLOSED),
        UNKNOWN(null, null, null);

        /**
         * The text that represents this status in JIRA.
         */
        private final String text;

        /**
         * Transition to use to get to 'Open'. Null indicates a transition is not possible.
         */
        private final JiraTransition toOpen;

        /**
         * Transition to use to get to 'Closed'. Null indicates a transition is not possible.
         */
        private final JiraTransition toClosed;

        JiraStatus(String text, JiraTransition toOpen, JiraTransition toClosed) {
            this.text = text;
            this.toOpen = toOpen;
            this.toClosed = toClosed;
        }

        /**
         * Given a JIRA issue, return its status as a JiraStatus.
         */
        public static JiraStatus fromIssue(JiraIssue issue) throws IOException {
            Object statusValue = issue.getField(ProductOrder.JiraField.STATUS.getName());
            String text = ((Map<?, ?>) statusValue).get("name").toString();
            for (JiraStatus status : values()) {
                if (status.text.equalsIgnoreCase(text)) {
                    return status;
                }
            }
            return UNKNOWN;
        }

        /**
         * Given a PDO status, return the transition required to get to the corresponding JIRA status from this one.
         */
        public JiraTransition getTransitionTo(OrderStatus orderStatus) {
            if (orderStatus == OrderStatus.Completed) {
                return toClosed;
            }
            return toOpen;
        }
    }

    /**
     * This is a version of {@link #updateOrderStatus} that does not propagate RuntimeExceptions since those cause a
     * transaction in progress to be marked for rollback.
     * Rollback on failures to update JIRA tickets with status changes is undesirable in billing as the status change is
     * fairly inconsequential in comparison to persisting database records of whether work was billed to the quote server.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateOrderStatusNoRollback(@Nonnull String jiraTicketKey)
            throws NoSuchPDOException, IOException, SAPInterfaceException {
        try {
            updateOrderStatus(jiraTicketKey, MessageReporter.UNUSED);
        } catch (RuntimeException e) {
            // If we failed to update the order status be sure to log it here as the caller will not be receiving a
            // RuntimeException due to transaction rollback issues and will not be aware there was a problem.
            log.error("Error updating order status for " + jiraTicketKey, e);
        }
    }

    /**
     * Update the order status of a PDO, based on the rules in {@link ProductOrder#updateOrderStatus}.  Any status
     * changes are pushed to JIRA as well, with a comment about the change and the current user.
     *
     * @param jiraTicketKey the key to update
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateOrderStatus(@Nonnull String jiraTicketKey, @Nonnull MessageReporter reporter)
            throws NoSuchPDOException, IOException, SAPInterfaceException {
        // Since we can't directly change the JIRA status of a PDO, we need to use a JIRA transition which in turn will
        // update the status.
        ProductOrder order = findProductOrder(jiraTicketKey);
        if (order.updateOrderStatus()) {
            transitionIssueToSameOrderStatus(order);
            // The status was changed, let the user know.
            reporter.addMessage("The order status of ''{0}'' is now {1}.", jiraTicketKey, order.getOrderStatus());
            conditionallyShortCloseOrder(order);
        }
    }

    /**
     * Encapsulates the call necessary to request that SAP short close an order.  The logic here will also send to
     * test users in a non Production environment
     *
     * @param body The body of the short close request email
     */
    private void sendSapOrderShortCloseRequest(String body) {

        Collection<String> currentUserForCC = Collections.singletonList(userBean.getBspUser().getEmail());

        final boolean isProduction = deployment.equals(Deployment.PROD);
        final Collection<String> ccAddrdesses = new ArrayList<>();
        if(isProduction) {
            ccAddrdesses .addAll(currentUserForCC);
        }

        emailSender.sendHtmlEmail(appConfig, sapConfig.getSapShortCloseRecipientEmail(), ccAddrdesses,
                sapConfig.getSapShortCloseEmailSubject(), body, !isProduction);
    }

    /**
     * If possible, update the JIRA issue so its status matches the status of the PDO in Mercury. No error is
     * generated if the transition is not possible, or if the JIRA status already matches Mercury.
     *
     * @param order the order to transition
     *
     * @throws IOException
     */
    private void transitionIssueToSameOrderStatus(@Nonnull ProductOrder order) throws IOException {
        JiraIssue issue = jiraService.getIssue(order.getJiraTicketKey());
        JiraTransition transition = JiraStatus.fromIssue(issue).getTransitionTo(order.getOrderStatus());
        if (transition != null) {
            issue.postTransition(transition.stateName, getUserName() + " transitioned to " + transition.stateName);
        }
    }

    /**
     * Transition the specified JIRA ticket using the specified transition, adding the specified comment.
     *
     * @param jiraTicketKey      JIRA ticket key.
     * @param currentResolution  if the JIRA's resolution is already this value, do not do anything.
     * @param state              The transition to use.
     * @param transitionComments Comments to include as part of the transition, will be appended to the JIRA ticket.
     *
     * @throws IOException
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void transitionJiraTicket(String jiraTicketKey, JiraResolution currentResolution, JiraTransition state,
                                     @Nullable String transitionComments) throws IOException {
        JiraIssue issue = jiraService.getIssue(jiraTicketKey);
        JiraResolution resolution = JiraResolution.fromString(issue.getResolution());
        if (currentResolution != resolution) {
            String jiraCommentText = getUserName() + " performed " + state.getStateName() + " transition";
            if (transitionComments != null) {
                jiraCommentText = jiraCommentText + ": " + transitionComments;
            }
            issue.postTransition(state.getStateName(), jiraCommentText);
        }
    }

    /**
     * Abandon the whole PDO with a comment.
     *
     * @param jiraTicketKey   JIRA ticket key.
     * @param abandonComments Transition comments.
     *
     * @throws NoSuchPDOException
     * @throws SampleDeliveryStatusChangeException
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void abandon(@Nonnull String jiraTicketKey, @Nullable String abandonComments)
            throws NoSuchPDOException, SampleDeliveryStatusChangeException, IOException, SAPInterfaceException {

        ProductOrder productOrder = findProductOrder(jiraTicketKey);

        productOrder.setOrderStatus(OrderStatus.Abandoned);

        // For Pending orders, don't change the sample states. This is so reporting can distinguish
        // abandoned samples vs samples that were removed because the PDO was never placed.
        if (!productOrder.isPending()) {
            transitionSamples(productOrder, EnumSet.of(DeliveryStatus.ABANDONED, DeliveryStatus.NOT_STARTED),
                    DeliveryStatus.ABANDONED, productOrder.getSamples());
        }

        // Currently not setting abandon comments into PDO comments, that seems too intrusive.  We will record the comments
        // with the JIRA ticket.
        transitionJiraTicket(jiraTicketKey, JiraResolution.CANCELLED, JiraTransition.CANCEL, abandonComments);


        conditionallyShortCloseOrder(productOrder);
    }

    private void conditionallyShortCloseOrder(ProductOrder productOrder) throws SAPInterfaceException {

        ProductOrder targetSapPdo = ProductOrder.getTargetSAPProductOrder(productOrder);

        if(targetSapPdo.isSavedInSAP() &&
           ((targetSapPdo.allOrdersAreComplete() &&
             targetSapPdo.getTotalNonAbandonedCount(ProductOrder.CountAggregation.SHARE_SAP_ORDER_AND_BILL_READY) < targetSapPdo.latestSapOrderDetail().getPrimaryQuantity()
           ) || CollectionUtils.containsAny(Arrays.asList(OrderStatus.Abandoned, OrderStatus.Completed),Collections.singleton(targetSapPdo.getOrderStatus())))) {

            boolean orderEligibleForSAP = true;
            try {
                orderEligibleForSAP = isOrderEligibleForSAP(productOrder);
            } catch (QuoteServerException | QuoteNotFoundException | InvalidProductException e) {
                orderEligibleForSAP = false;
            }
            if(productOrder.isPriorToSAP1_5() || !orderEligibleForSAP) {
                sendSapOrderShortCloseRequest(
                        "The SAP order " + productOrder.getSapOrderNumber() + " for PDO "+productOrder.getBusinessKey() +
                        " has been marked as completed in Mercury by " +
                        userBean.getBspUser().getFullName() + " and may need to be short closed.");

            }   else {
                publishProductOrderToSAP(productOrder, new MessageCollection(), false);
            }
        }
    }

    /**
     * Abandon a list of samples and add a message to the JIRA ticket to reflect this change.
     *
     * @param jiraTicketKey the order's JIRA key
     * @param samples       the samples to abandon
     * @param comment       optional user supplied comment about this action.
     * @param messageCollection
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void abandonSamples(@Nonnull String jiraTicketKey, @Nonnull Collection<ProductOrderSample> samples,
                               @Nonnull String comment, MessageCollection messageCollection)
            throws IOException, SampleDeliveryStatusChangeException, NoSuchPDOException, SAPInterfaceException {
        transitionSamplesAndUpdateTicket(jiraTicketKey,
                EnumSet.of(DeliveryStatus.ABANDONED, DeliveryStatus.NOT_STARTED),
                DeliveryStatus.ABANDONED, samples,
                comment, messageCollection);
    }

    /**
     * Un-abandon a list of samples and add a message to the JIRA ticket to reflect this change.
     * @param jiraTicketKey the order's JIRA key
     * @param sampleIds     the samples to un-abandon
     * @param comment       optional user supplied comment about this action.
     * @param reporter
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void unAbandonSamples(@Nonnull String jiraTicketKey, @Nonnull Collection<Long> sampleIds,
                                 @Nonnull String comment, @Nonnull MessageCollection reporter)
            throws IOException, SampleDeliveryStatusChangeException, NoSuchPDOException, SAPInterfaceException {

        List<ProductOrderSample> samples = productOrderSampleDao.findListByList(ProductOrderSample.class,
                ProductOrderSample_.productOrderSampleId, sampleIds);

        if (!StringUtils.isBlank(comment)) {
            for (ProductOrderSample sample : samples) {
                if (sample.getDeliveryStatus() == DeliveryStatus.ABANDONED) {
                    sample.setSampleComment(comment);
                }
            }
        }

        transitionSamplesAndUpdateTicket(jiraTicketKey, EnumSet.of(DeliveryStatus.ABANDONED),
                DeliveryStatus.NOT_STARTED, samples, comment, reporter);

        reporter.addInfo("Un-Abandoned samples: {0}.",
                StringUtils.join(ProductOrderSample.getSampleNames(samples), ", "));
    }

    /**
     * Update JIRA state of an order based on a sample change operation.
     * <ul>
     * <li>add a comment with the operation and the list of samples changed</li>
     * <li>update the Sample IDs and Number of Samples fields</li>
     * <li>output a message to the user about the operation</li>
     * <li>if necessary, update the order status based on the new list of samples</li>
     * </ul>
     */
    private void updateSamples(ProductOrder order, Collection<ProductOrderSample> samples, MessageReporter reporter,
                               String operation) throws IOException, NoSuchPDOException, SAPInterfaceException {
        JiraIssue issue = jiraService.getIssue(order.getJiraTicketKey());

        String nameList = StringUtils.join(ProductOrderSample.getSampleNames(samples), ",");
        issue.addComment(MessageFormat.format("{0} {1} samples: {2}.",
                userBean.getLoginUserName(), operation, nameList));
        productOrderJiraUtil.setCustomField(issue, ProductOrder.JiraField.SAMPLE_IDS, order.getSampleString());
        productOrderJiraUtil.setCustomField(issue, ProductOrder.JiraField.NUMBER_OF_SAMPLES, order.getSamples().size());

        MessageCollection collection = new MessageCollection();
        publishProductOrderToSAP(order, collection, false);
        for (String error : collection.getErrors()) {
            reporter.addMessage(error);
        }
        for (String warn : collection.getWarnings()) {
            reporter.addMessage("Warning: " + warn);
        }
        for (String info : collection.getInfos()) {
            reporter.addMessage(info);
        }

        reporter.addMessage("{0} samples: {1}.", WordUtils.capitalize(operation), nameList);

        updateOrderStatus(order.getJiraTicketKey(), reporter);


    }

    /**
     * Used only for a fixup test.
     *
     * Update JIRA state of an order based on a sample change operation.
     * <ul>
     * <li>add a comment with the operation and the list of samples changed</li>
     * <li>update the Sample IDs and Number of Samples fields</li>
     * <li>output a message to the user about the operation</li>
     * <li>if necessary, update the order status based on the new list of samples</li>
     * </ul>
     */
    private void updateSamplesNoSap(ProductOrder order, Collection<ProductOrderSample> samples, MessageReporter reporter,
                               String operation) throws IOException, NoSuchPDOException, SAPInterfaceException {
        JiraIssue issue = jiraService.getIssue(order.getJiraTicketKey());

        String nameList = StringUtils.join(ProductOrderSample.getSampleNames(samples), ",");
        issue.addComment(MessageFormat.format("{0} {1} samples: {2}.",
                userBean.getLoginUserName(), operation, nameList));
        productOrderJiraUtil.setCustomField(issue, ProductOrder.JiraField.SAMPLE_IDS, order.getSampleString());
        productOrderJiraUtil.setCustomField(issue, ProductOrder.JiraField.NUMBER_OF_SAMPLES, order.getSamples().size());

        MessageCollection collection = new MessageCollection();
        for (String error : collection.getErrors()) {
            reporter.addMessage(error);
        }
        for (String warn : collection.getWarnings()) {
            reporter.addMessage("Warning: " + warn);
        }
        for (String info : collection.getInfos()) {
            reporter.addMessage(info);
        }

        reporter.addMessage("{0} samples: {1}.", WordUtils.capitalize(operation), nameList);

        updateOrderStatus(order.getJiraTicketKey(), reporter);


    }

    /**
     * Given a PDO ID, add a list of samples to the PDO.  This will update the PDO's JIRA with a comment that the
     * samples have been added, and will notify LIMS that the samples are now present, and update the PDO's status
     * if necessary.
     *
     * @param jiraTicketKey the PDO key
     * @param samples       the samples to add. this argument must not be changed to Collection, or
     *                      ImmutableListMultiMap does not work correctly.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void addSamples(@Nonnull String jiraTicketKey, @Nonnull List<ProductOrderSample> samples,
                           @Nonnull MessageReporter reporter)
            throws NoSuchPDOException, IOException, SAPInterfaceException {
        ProductOrder order = findProductOrder(jiraTicketKey);
        order.addSamples(samples);

        attachMercurySamples(samples);

        order.prepareToSave(userBean.getBspUser());
        productOrderDao.persist(order);
        handleSamplesAdded(jiraTicketKey, samples, reporter);

        updateSamples(order, samples, reporter, "added");
    }

    /**
     * Used only for a fixup test
     *
     * Given a PDO ID, add a list of samples to the PDO.  This will update the PDO's JIRA with a comment that the
     * samples have been added, and will notify LIMS that the samples are now present, and update the PDO's status
     * if necessary.
     *
     * @param jiraTicketKey the PDO key
     * @param samples       the samples to add. this argument must not be changed to Collection, or
     *                      ImmutableListMultiMap does not work correctly.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void addSamplesNoSap(@Nonnull String jiraTicketKey, @Nonnull List<ProductOrderSample> samples,
                           @Nonnull MessageReporter reporter)
            throws NoSuchPDOException, IOException, SAPInterfaceException {
        ProductOrder order = findProductOrder(jiraTicketKey);
        order.addSamples(samples);

        attachMercurySamples(samples);

        order.prepareToSave(userBean.getBspUser());
        productOrderDao.persist(order);
        handleSamplesAdded(jiraTicketKey, samples, reporter);

        updateSamplesNoSap(order, samples, reporter, "added");
    }


    /**
     * Makes the association between ProductOrderSample and MercurySample.
     */
    private void attachMercurySamples(@Nonnull List<ProductOrderSample> samples) {
        ImmutableListMultimap<String, ProductOrderSample> samplesBySampleId =
                Multimaps.index(samples, new Function<ProductOrderSample, String>() {
                    @Override
                    public String apply(ProductOrderSample productOrderSample) {
                        return productOrderSample.getSampleKey();
                    }
                });

        Map<String, MercurySample> mercurySampleMap = mercurySampleDao.findMapIdToMercurySample(
                samplesBySampleId.keySet());

        for (Map.Entry<String, ProductOrderSample> sampleMapEntry : samplesBySampleId.entries()) {
            MercurySample mercurySample = mercurySampleMap.get(sampleMapEntry.getKey());
            if (sampleMapEntry.getValue().getMercurySample() == null && mercurySample != null) {
                mercurySample.addProductOrderSample(sampleMapEntry.getValue());
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeSamples(@Nonnull String jiraTicketKey, @Nonnull Collection<ProductOrderSample> samples,
                              @Nonnull MessageReporter reporter)
            throws IOException, NoSuchPDOException, SAPInterfaceException {
        ProductOrder productOrder = findProductOrder(jiraTicketKey);

        // If removeAll returns false, no samples were removed -- should never happen.
        if (productOrder.getSamples().removeAll(samples)) {
            for (ProductOrderSample sample : samples) {
                sample.remove();
            }
            String nameList = StringUtils.join(ProductOrderSample.getSampleNames(samples), ",");
            productOrder.prepareToSave(userBean.getBspUser());
            productOrderDao.persist(productOrder);

            updateSamples(productOrder, samples, reporter, "deleted");
        }
    }

    /**
     * Encapsulates the logic for placing a product order.  This encompasses:
     * <ul>
     * <li>creating the PDO ticket in Jira</li>
     * <li>(if sample initiation) creating and submitting a kit request to BSP</li>
     * </ul>
     * <p/>
     * To avoid double ticket creations, we are employing a pessimistic lock on the Product order record.
     *
     * @param productOrderID    Database identifier of the Product order which we wish to find.  Used because at this
     *                          phase, the business key could change from draft-xxx to PDO-yyy.
     * @param businessKey       Business key by which to reference the currently persisted Product order
     * @param messageCollection Used to transmit errors or successes to the caller (Action bean) without returning
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ProductOrder placeProductOrder(@Nonnull Long productOrderID, String businessKey,
                                          @Nonnull MessageCollection messageCollection) {
        ProductOrder editOrder =
                productOrderDao.findByIdSafely(productOrderID, LockModeType.PESSIMISTIC_WRITE);

        if (editOrder == null) {
            throw new InformaticsServiceException(
                    "The product order was not found: " + ((businessKey == null) ? "" : businessKey));
        }
        if (editOrder.isSubmitted()) {
            throw new InformaticsServiceException("This product order " + editOrder.getBusinessKey() +
                                                  " has already been submitted to Jira");
        }
        editOrder.prepareToSave(userBean.getBspUser());
        try {
            if (editOrder.isDraft()) {
                // Only Draft orders are not already created in JIRA.
                productOrderJiraUtil.createIssueForOrder(editOrder);
            }
            editOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
            editOrder.setPlacedDate(new Date());
            transitionIssueToSameOrderStatus(editOrder);

            // Now that the order is placed, add the comments about the samples to the issue.
            productOrderJiraUtil.addSampleComments(editOrder);

        } catch (IOException e) {
            String message = "Unable to create the Product Order in Jira";
            log.error(message, e);
            throw new InformaticsServiceException(message, e);
        }

        if (editOrder.isSampleInitiation()) {
            try {
                submitSampleKitRequest(editOrder, messageCollection);
            } catch (Exception e) {
                String errorMessage = "Unable to successfully complete the sample kit creation: ";
                log.error(errorMessage, e);
                messageCollection.addError(errorMessage + e.getMessage());
            }
        }

        return editOrder;
    }

    /**
     * Helper method to separate sample kit submission to its own transaction.  This will allow Product order creation
     * to succeed and commit the transition from Product Order draft to Submit even if the attempt to create a
     * sample kit in Bsp results in an exception, which should not roll back product order submission
     *
     * @param order             Order to which the new sample kit is to be associated
     * @param messageCollection Used to transmit errors or successes to the caller (Action bean) without returning
     *                          a value or throwing an exception.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void submitSampleKitRequest(@Nonnull ProductOrder order, @Nonnull MessageCollection messageCollection) {
        String workRequestBarcode = bspKitRequestService.createAndSubmitKitRequestForPDO(order);
        order.getProductOrderKit().setWorkRequestId(workRequestBarcode);
        messageCollection.addInfo("Created BSP work request ''{0}'' for this order.", workRequestBarcode);
    }

    /**
     * This method will post the basic squid work request details entered by a user to create a project and work
     * request within squid for the product order represented in the input.
     *
     * @param productOrderKey Unique Jira key representing the product order for the resultant work request
     * @param squidInput      Basic information needed for squid to automatically create a work request for the
     *                        material information represented by the samples in the referenced product order
     *
     * @return work request output
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public AutoWorkRequestOutput createSquidWorkRequest(@Nonnull String productOrderKey,
                                                        @Nonnull AutoWorkRequestInput squidInput) {

        ProductOrder order = productOrderDao.findByBusinessKey(productOrderKey);

        if (order == null) {
            throw new RuntimeException("Unable to find a product order with a key of " + productOrderKey);
        }

        AutoWorkRequestOutput workRequestOutput = squidConnector.createSquidWorkRequest(squidInput);

        order.setSquidWorkRequest(workRequestOutput.getWorkRequestId());

        try {
            addWorkRequestNotification(order, workRequestOutput, squidInput);
        } catch (IOException e) {
            log.info("Unable to post work request creation of " + workRequestOutput.getWorkRequestId() + " to Jira for "
                     + productOrderKey);
        }
        return workRequestOutput;
    }

    private void addWorkRequestNotification(@Nonnull ProductOrder pdo,
                                            @Nonnull AutoWorkRequestOutput createdWorkRequestResults,
                                            AutoWorkRequestInput squidInput)
            throws IOException {

        JiraIssue pdoIssue = jiraService.getIssue(pdo.getBusinessKey());

        pdoIssue.addComment(String.format("Created new Squid project %s for new Squid work request %s",
                createdWorkRequestResults.getProjectId(), createdWorkRequestResults.getWorkRequestId()));

        if (StringUtils.isNotBlank(squidInput.getLcsetId())) {
            pdoIssue.addLink(squidInput.getLcsetId());
//            pdoIssue.addComment(String.format("Work request %s is associated with LCSet %s",
//                    createdWorkRequestResults.getWorkRequestId(), squidInput.getLcsetId()));
        }
    }

    /**
     * Apply a collection of ledger updates for some product order samples. The update requests have the previous
     * quantities as well as the new quantities being requested. The previous quantities represent those that were in
     * effect when the decision to change the quantity was made. Across a sequence of web requests, it is possible that
     * those quantities have changed for other reasons, so this protects users from making billing decisions based on
     * outdated information.
     *
     * @param ledgerUpdates a map of PDO sample to a collection of ledger updates
     *
     * @throws ValidationWithRollbackException to capture and relay multiple errors that may have occurred while
     * creating or updating ledger entries
     * @throws QuoteNotFoundException if the quote is not found
     * @throws QuoteServerException if any errors occurs during the attempt to access the quote server during this
     * method
     *
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateSampleLedgers(Map<ProductOrderSample, Collection<ProductOrderSample.LedgerUpdate>> ledgerUpdates)
            throws ValidationWithRollbackException, QuoteNotFoundException, QuoteServerException {
        List<String> errorMessages = new ArrayList<>();

        Map<String, Quote> usedQuotesMiniCache = new HashMap<>();

        Map<String, Boolean> updatedOrderMap = new HashMap<>();

        for (Map.Entry<ProductOrderSample, Collection<ProductOrderSample.LedgerUpdate>> entry : ledgerUpdates
                .entrySet()) {
            ProductOrderSample productOrderSample = entry.getKey();

            if(!updatedOrderMap.containsKey(productOrderSample.getProductOrder().getBusinessKey()) ||
               !updatedOrderMap.get(productOrderSample.getProductOrder().getBusinessKey())) {

                Quote orderQuote = usedQuotesMiniCache.get(productOrderSample.getProductOrder().getQuoteId());

                if(orderQuote == null) {
                    orderQuote = quoteService.getQuoteByAlphaId(productOrderSample.getProductOrder().getQuoteId());
                    usedQuotesMiniCache.put(orderQuote.getAlphanumericId(), orderQuote);
                }

                updatedOrderMap.put(productOrderSample.getProductOrder().getBusinessKey(), Boolean.TRUE);
            }

            Collection<ProductOrderSample.LedgerUpdate> updates = entry.getValue();
            for (ProductOrderSample.LedgerUpdate update : updates) {
                try {
                    productOrderSample.applyLedgerUpdate(update);
                } catch (Exception e) {
                    errorMessages.add(e.getMessage());
                }
            }
        }

        if (!errorMessages.isEmpty()) {
            throw new ValidationWithRollbackException("Error updating ledger quantities", errorMessages);
        }
        productOrderDao.flush();
    }

    @Inject
    public void setProductOrderSampleDao(ProductOrderSampleDao productOrderSampleDao) {
        this.productOrderSampleDao = productOrderSampleDao;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public GenotypingProductOrderMapping findOrCreateGenotypingChipProductOrderMapping(Long productOrderId) {
        GenotypingProductOrderMapping mapping =
                attributeArchetypeDao.findGenotypingProductOrderMapping(productOrderId);
        if (mapping == null) {
            mapping = new GenotypingProductOrderMapping(productOrderId.toString(), null, null);
            attributeArchetypeDao.persist(mapping);
            attributeArchetypeDao.flush();
        }
        return mapping;
    }

    @Inject
    public void setAppConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Inject
    public void setEmailSender(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Inject
    public void setAccessController(
            SAPAccessControlEjb accessController) {
        this.accessController = accessController;
    }

    @Inject
    public void setSapConfig(SapConfig sapConfig) {
        this.sapConfig = sapConfig;
    }

    @Inject
    public void setDeployment(Deployment deployment) {
        this.deployment = deployment;
    }
}
