package org.broadinstitute.gpinformatics.athena.boundary.billing;

import com.google.common.collect.HashMultimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.PriceAdjustment;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPInterfaceException;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.sap.entity.DeliveryCondition;
import org.broadinstitute.sap.entity.SAPMaterial;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Billing Adaptor was derived to provide the ability to provide singular interface calls related to billingEJB
 * methods when simply calling BillingEJB will not suffice
 */
@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class BillingAdaptor implements Serializable {

    private static final long serialVersionUID = 8829737083883106155L;

    private static final Log log = LogFactory.getLog(BillingAdaptor.class);
    public static final String NOT_ELIGIBLE_FOR_SAP_INDICATOR = "NotEligible";
    public static final String BILLING_CREDIT_REQUESTED_INDICATOR = "Credit Requested";

    private BillingEjb billingEjb;

    private PriceListCache priceListCache;

    private ProductOrderEjb productOrderEjb;

    private QuoteService quoteService;

    private BillingSessionAccessEjb billingSessionAccessEjb;

    private SapIntegrationService sapService;

    private static final FastDateFormat BILLING_DATE_FORMAT =
            FastDateFormat.getDateTimeInstance(FastDateFormat.SHORT, FastDateFormat.SHORT);

    private SAPProductPriceCache productPriceCache;

    private SAPAccessControlEjb sapAccessControlEjb;

    public static final String NEGATIVE_BILL_ERROR =
        "Attempt to create billing credit when sample has not been billed.";

    @Inject
    public BillingAdaptor(BillingEjb billingEjb, PriceListCache priceListCache,
                          QuoteService quoteService, BillingSessionAccessEjb billingSessionAccessEjb,
                          SapIntegrationService sapService,
                          SAPProductPriceCache productPriceCache,
                          SAPAccessControlEjb sapAccessControlEjb) {
        this.billingEjb = billingEjb;
        this.priceListCache = priceListCache;
        this.quoteService = quoteService;
        this.billingSessionAccessEjb = billingSessionAccessEjb;
        this.sapService = sapService;
        this.productPriceCache = productPriceCache;
        this.sapAccessControlEjb = sapAccessControlEjb;
    }

    public BillingAdaptor() {
    }

    /**
     * Provides the ability to have finite transactional calls specific to updating quote information and updating PDO
     * Information.  A larger transaction around all the calls represented in this method ran the risk of having none
     * of the information logged to the database.
     *
     * @param pageUrl    Page from which billing request was initiated
     * @param sessionKey Key of the Billing session entity for which the system will attempt to file and record
     *                   billing charges
     *
     * @return collection of "Billing Results".  Each one represents the aggregation of billing charges and will
     * record the success or failure of the billing attempt
     */
    public List<BillingEjb.BillingResult> billSessionItems(@Nonnull String pageUrl, @Nonnull String sessionKey) {

        List<BillingEjb.BillingResult> billingResults;

            billingResults = bill(pageUrl, sessionKey);
            updateBilledPdos(billingResults);

        return billingResults;
    }

    /**
     * Non-Transactional method to bill each previously unbilled {@link QuoteImportItem} on the BillingSession to the
     * quote server and update billing entities as appropriate to the results of the billing attempt.  Results
     * for each billing attempt correspond to a returned BillingResult.  If there was an exception billing a
     * QuoteImportItem, the
     * {@link org.broadinstitute.gpinformatics.athena.boundary.billing.BillingEjb.BillingResult#isError()} will return
     * true and
     * {@link org.broadinstitute.gpinformatics.athena.boundary.billing.BillingEjb.BillingResult#getErrorMessage()}
     * will describe the cause of the problem.  On successful billing
     * {@link org.broadinstitute.gpinformatics.athena.boundary.billing.BillingEjb.BillingResult#getWorkId()} will
     * contain the work id result.
     *
     * @param pageUrl    URL to be included in the call to the quote server.
     * @param sessionKey Key to be included in the call to the quote server.
     *
     * @return List of BillingResults describing the success or failure of billing for each previously un-billed
     * QuoteImportItem associated with the BillingSession.
     */
    private List<BillingEjb.BillingResult> bill(@Nonnull String pageUrl, @Nonnull String sessionKey) {

        boolean errorsInBilling = false;

        List<BillingEjb.BillingResult> results = new ArrayList<>();
        Quote quote;

        BillingSession billingSession = billingSessionAccessEjb.findAndLockSession(sessionKey);
        try {
            List<QuoteImportItem> unBilledQuoteImportItems = null;
            PriceList priceItemsForDate;
            try {
                unBilledQuoteImportItems = billingSession.getUnBilledQuoteImportItems(priceListCache);
            } catch (QuoteServerException e) {
                throw new BillingException("Getting unbilled items failed because::" + e.getMessage(), e);
            }

            if (unBilledQuoteImportItems.isEmpty()) {
                billingEjb.endSession(billingSession);
                throw new BillingException(BillingEjb.NO_ITEMS_TO_BILL_ERROR_TEXT);
            }

            for(QuoteImportItem itemForPriceUpdate : unBilledQuoteImportItems) {
                final List<Product> allProductsOrdered = ProductOrder.getAllProductsOrdered(itemForPriceUpdate.getProductOrder());
                List<String> effectivePricesForProducts;

                try {

                    // get and set the priceItem (reallly PriceList) for this quoteImportItem
                    priceItemsForDate = quoteService.getPriceItemsForDate(Collections.singletonList(itemForPriceUpdate));
                    itemForPriceUpdate.setPriceOnWorkDate(priceItemsForDate);

                    quote = itemForPriceUpdate.getProductOrder().getQuote(quoteService);
                    //todo SGM is this call really necessary?  Is it just for DBFree tests?
                    quote.setAlphanumericId(itemForPriceUpdate.getQuoteId());
                    itemForPriceUpdate.setQuote(quote);
                    ProductOrder.checkQuoteValidity(quote);

                    // publish order to SAP if ...
                    // 1 products are not blacklisted
                    // 2 Prices are valid and products are not black listed
                    // 3 order is in sap
                    // ?? does funding matter here ??
                    if (productOrderEjb.isOrderEligibleForSAP(itemForPriceUpdate.getProductOrder())
                        && itemForPriceUpdate.getProductOrder().isSavedInSAP()) {
                        MessageCollection messageCollection = new MessageCollection();
                        effectivePricesForProducts = getEffectivePricesForProducts(allProductsOrdered, quote, priceItemsForDate,
                                itemForPriceUpdate.getProductOrder());
                        if (itemForPriceUpdate.getProductOrder().isPriorToSAP1_5()) {

                            // price and effective prices differ
                            if (!StringUtils.equals(itemForPriceUpdate.getProductOrder().latestSapOrderDetail().getOrderPricesHash(),
                                    TubeFormation.makeDigest(StringUtils.join(effectivePricesForProducts, ",")))) {
                                productOrderEjb.publishProductOrderToSAP(itemForPriceUpdate.getProductOrder(), messageCollection, true);
                            }
                        } else {
                            final String price =
                                    priceListCache.findByKeyFields(itemForPriceUpdate.getPriceItem()).getPrice();

                            BigDecimal listPrice = new BigDecimal(price);
                            BigDecimal effectivePrice = new BigDecimal(itemForPriceUpdate.getEffectivePrice());

                            // not commercial
                            // not custom
                            // list and effective prices differ
                            if(!itemForPriceUpdate.getProductOrder().isCommercial()
                               && !itemForPriceUpdate.needsCustomization() && listPrice.compareTo(effectivePrice) != 0) {
                                itemForPriceUpdate.getProductOrder().addQuoteAdjustment(itemForPriceUpdate.getProduct(), effectivePrice, listPrice);
                            }
                            productOrderEjb.publishProductOrderToSAP(itemForPriceUpdate.getProductOrder(), messageCollection, false);
                        }
                    }
                } catch (QuoteServerException|QuoteNotFoundException|InvalidProductException|SAPInterfaceException e) {
                    BillingEjb.BillingResult result = new BillingEjb.BillingResult(itemForPriceUpdate);

                    final String errorMessage = "Failed Price check for " +itemForPriceUpdate.getProductOrder().getBusinessKey()+": "+e.getMessage();
                    itemForPriceUpdate.setBillingMessages(errorMessage);
                    result.setErrorMessage(errorMessage);
                    errorsInBilling = true;

                    results.add(result);

                    log.error(errorMessage);
                }
            }

            if(!results.isEmpty()) {
                throw new BillingException("Pricing Validation Failed.  Unable to complete Billing Session at this time");
            }

            HashMultimap<String, String> quoteItemsByQuote = HashMultimap.create();
            for (QuoteImportItem item : unBilledQuoteImportItems) {

                BillingEjb.BillingResult result = new BillingEjb.BillingResult(item);
                results.add(result);

                quote = null;
                String sapBillingId = null;
                String workId = null;
                boolean isQuoteFunded = false;
                try {
                    priceItemsForDate = item.getPriceOnWorkDate();
                    quote = item.getQuote();
                    isQuoteFunded = quote.isFunded(item.getWorkCompleteDate());// && quote.isFunded(item.getWorkCompleteDate());

//                    //todo SGM is this call really necessary?  Is it just for DBFree tests?
//                    quote.setAlphanumericId(item.getQuoteId());

                    workId = CollectionUtils.isEmpty(item.getWorkItems())? null: item.getWorkItems().toArray(new String[0])[0];
                    sapBillingId = quote.isEligibleForSAP() && isQuoteFunded ? item.getSapItems(): NOT_ELIGIBLE_FOR_SAP_INDICATOR;

                    // The price item that we are billing.
                    // todo need to set the price on the Price Item before this step
                    QuotePriceItem priceItemBeingBilled = QuotePriceItem.convertMercuryPriceItem(item.getPriceItem());

                    //Replace with the New price list call for effective date
                    String price = item.getEffectivePrice();

                    // Get the quote PriceItem that this is replacing, if it is a replacement.
                    // todo need to set the price on the Price Item before this step
                    QuotePriceItem primaryPriceItemIfReplacement = item.getPrimaryForReplacement(priceItemsForDate);

                    // todo need to set the price on the Price Item before this step
                    QuotePriceItem primaryPriceItemIfReplacementForSAP =item.getPrimaryForReplacement(priceItemsForDate);

                    // Get the quote items on the quote, adding to the quote item cache, if not there.
                    Collection<String> quoteItemNames = getQuoteItems(quoteItemsByQuote, item.getQuoteId());

                    //Using the first item in the set of work items because, even though it's a set all ledger items
                    // in this quote item should have the same work item
                    // If this is a replacement, the primary is not on the quote and the replacement IS on the quote,
                    // set the primary to null so it will be billed as if it is a primary.
                    if (primaryPriceItemIfReplacement != null) {
                        if (!quoteItemNames.contains(primaryPriceItemIfReplacement.getName()) &&
                            quoteItemNames.contains(priceItemBeingBilled.getName())) {
                            primaryPriceItemIfReplacement = null;

                        }
                    }

                    BigDecimal replacementMultiplier = null;
                    double quantityForSAP = item.getQuantityForSAP();
                    if(primaryPriceItemIfReplacementForSAP != null) {
                        BigDecimal primaryPrice = new BigDecimal(primaryPriceItemIfReplacementForSAP.getPrice());
                        BigDecimal replacementPrice  = new BigDecimal(price);

                        if(item.getProductOrder().isPriorToSAP1_5() && quantityForSAP > 0) {
                            replacementMultiplier = (replacementPrice.divide(primaryPrice, 3, BigDecimal.ROUND_DOWN))
                                    .multiply(BigDecimal.valueOf(quantityForSAP))
                                    .setScale(3, BigDecimal.ROUND_DOWN);
                        }
                    }

                    boolean orderEligibleForSAP = productOrderEjb.isOrderEligibleForSAP(item.getProductOrder());
                    boolean canBeBilledInSap = !item.getProductOrder().getOrderStatus().canPlace() &&
                                   StringUtils.isNotBlank(item.getProductOrder().getSapOrderNumber()) &&
                                   StringUtils.isBlank(item.getSapItems());

                    if(orderEligibleForSAP && canBeBilledInSap) {
                        final SAPMaterial material = productPriceCache.findByProduct(item.getProduct(),
                                item.getProductOrder().getSapCompanyConfigurationForProductOrder());

                        BigDecimal replacementPrice = new BigDecimal(price);

                        BigDecimal sapPrimaryPrice = new BigDecimal(material.getBasePrice());

                        if (StringUtils.equals(item.getQuotePriceType(),
                                LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM.getQuoteType())) {
                            if (!material.getPossibleDeliveryConditions()
                                    .containsKey(DeliveryCondition.LATE_DELIVERY_DISCOUNT)) {
                                throw new InvalidProductException(
                                        "Pricing in SAP has not been set up correctly: Late Delivery charge is not set for "
                                        + item.getProduct().getPartNumber());
                            }
                            if (replacementPrice.compareTo(sapPrimaryPrice.add(material.getPossibleDeliveryConditions()
                                    .get(DeliveryCondition.LATE_DELIVERY_DISCOUNT))) != 0) {
                                throw new InvalidProductException(
                                        "Pricing in SAP has not been set up correctly: Late Delivery charge in SAP differs from Quotes for "
                                        + item.getProduct().getPartNumber());
                            }
                        }

                    }

                    if (StringUtils.isBlank(workId)) {

                        PriceAdjustment singlePriceAdjustment =
                                item.getProductOrder().getAdjustmentForProduct(item.getProduct());

                        if (orderEligibleForSAP && item.getProductOrder().isSavedInSAP()) {
                            if (singlePriceAdjustment == null) {
                                workId = quoteService
                                        .registerNewSAPWork(quote, priceItemBeingBilled, primaryPriceItemIfReplacement,
                                                item.getWorkCompleteDate(), item.getQuantity(),
                                                pageUrl, "billingSession", sessionKey, null);
                            } else {
                                workId = quoteService
                                        .registerNewSAPWork(quote, priceItemBeingBilled,
                                                primaryPriceItemIfReplacement,
                                                item.getWorkCompleteDate(), item.getQuantity(),
                                                pageUrl, "billingSession", sessionKey,
                                                singlePriceAdjustment.getAdjustmentValue());
                            }
                        } else {
                            if (singlePriceAdjustment == null) {
                                workId = quoteService
                                        .registerNewWork(quote, priceItemBeingBilled, primaryPriceItemIfReplacement,
                                                item.getWorkCompleteDate(), item.getQuantity(),
                                                pageUrl, "billingSession", sessionKey, null);
                            } else {
                                workId = quoteService
                                        .registerNewWork(quote, priceItemBeingBilled, primaryPriceItemIfReplacement,
                                                item.getWorkCompleteDate(), item.getQuantity(),
                                                pageUrl, "billingSession", sessionKey,
                                                singlePriceAdjustment.getAdjustmentValue());
                            }
                        }

                        billingEjb.updateLedgerEntries(item, primaryPriceItemIfReplacement, workId, sapBillingId,
                                BillingSession.BILLED_FOR_QUOTES);
                    }

                    if (orderEligibleForSAP && canBeBilledInSap  && quote.isFunded(item.getWorkCompleteDate())) {
                        if (quantityForSAP > 0) {
                            sapBillingId = sapService.billOrder(item, replacementMultiplier, new Date());
                            result.setSAPBillingId(sapBillingId);
                            billingEjb
                                .updateLedgerEntries(item, primaryPriceItemIfReplacementForSAP, workId, sapBillingId,
                                    BillingSession.BILLED_FOR_SAP + " " + BillingSession.BILLED_FOR_QUOTES);
                        } else {
                            Set<LedgerEntry> priorSapBillings = new HashSet<>();
                            item.getBillingCredits().stream()
                                .filter(ledgerEntry -> ledgerEntry.getPriceItem().equals(item.getPriceItem()))
                                .forEach(ledgerEntry -> {
                                    priorSapBillings.addAll(ledgerEntry.getPreviouslyBilled().stream()
                                        .filter(previousBilled -> previousBilled.getSapDeliveryDocumentId() != null)
                                        .collect(Collectors.toSet()));
                                });

                            // Negative billing is allowed if the same positive number has been previously billed.
                            // When this is not the case throw an exception.
                            double previouslyBilledQty =
                                priorSapBillings.stream().map(LedgerEntry::getQuantity).mapToDouble(Double::doubleValue)
                                    .sum();
                            if (quantityForSAP + previouslyBilledQty < 0) {
                                result.setErrorMessage(NEGATIVE_BILL_ERROR);
                                throw new BillingException(NEGATIVE_BILL_ERROR);
                            } else if (quantityForSAP < 0) {
                                billingEjb.sendBillingCreditRequestEmail(item, priorSapBillings, billingSession.getCreatedBy());
                                item.setBillingMessages(BillingSession.BILLING_CREDIT);
                                sapBillingId = BILLING_CREDIT_REQUESTED_INDICATOR;
                                result.setSAPBillingId(sapBillingId);
                                billingEjb
                                    .updateLedgerEntries(item, primaryPriceItemIfReplacement, workId, sapBillingId,
                                        BillingSession.BILLING_CREDIT);
                            }
                        }
                        item.getProductOrder().latestSapOrderDetail().addLedgerEntries(item.getLedgerItems());
                    }

                    Set<String> billedPdoKeys = getBilledPdoKeys(result);

                    // Not sure I see the point of the next two lines!!!!
                    result.setWorkId(workId);
                    logBilling(workId, item, priceItemBeingBilled, billedPdoKeys, sapBillingId);
                    billingEjb.updateLedgerEntries(item, primaryPriceItemIfReplacement, workId, sapBillingId,
                            BillingSession.SUCCESS);
                } catch (Exception ex) {

                    StringBuilder errorMessage = new StringBuilder();
                    if (StringUtils.isBlank(result.getWorkId()) && StringUtils.isBlank(workId)) {


                        errorMessage.append("A problem occurred attempting to post to the quote server for ")
                                .append(billingSession.getBusinessKey()).append(".");

                    } else if (StringUtils.isBlank(result.getSAPBillingId()) && quote != null
                               && isQuoteFunded && item.getProductOrder().isSavedInSAP()) {

                        errorMessage.append("A problem occured attempting to post to SAP for ")
                                .append(billingSession.getBusinessKey()).append(".");

                    }
                    else {
                        errorMessage.append("A problem occurred saving the ledger entries for ")
                                .append(billingSession.getBusinessKey()).append(" with an SAP ID of ")
                                .append(StringUtils.isNotBlank(result.getSAPBillingId())?result.getSAPBillingId():sapBillingId).append(",")
                                .append(" with work id of ").append(StringUtils.isNotBlank(result.getWorkId())?result.getWorkId():workId)
                                .append(".  ")
                                .append("The quote for this item may have been successfully sent to the quote server");
                    }

                    log.error(errorMessage + ex.toString(), ex);

                    item.setBillingMessages(errorMessage + ex.getMessage());
                    result.setErrorMessage(errorMessage + ex.getMessage());
                    errorsInBilling = true;
                }
            }

        } finally {
            billingSessionAccessEjb.saveAndUnlockSession(billingSession);
        }
        // If there were no errors in billing, then end the session, which will add the billed date and remove
        // all sessions from the ledger.
        if (!errorsInBilling) {
            billingEjb.endSession(billingSession);
        }

        return results;
    }

    /**
     * This grabs the quote items for a quoteId from the quote server. It maintains a cache that keeps it from going
     * to the quote server multiple times for the same quote id.
     *
     * @param quoteItemsByQuote The cache of quote Ids to the quote items.
     * @param quoteId The quote to look up
     *
     * @return The list of quote items (price item names that belong to the quote).
     */
    private Collection<String> getQuoteItems(HashMultimap<String, String> quoteItemsByQuote, String quoteId) throws Exception {
        if (!quoteItemsByQuote.containsKey(quoteId)) {
            Quote quote = quoteService.getQuoteWithPriceItems(quoteId);
            for (QuoteItem quoteItem : quote.getQuoteItems()) {
                quoteItemsByQuote.put(quoteId, quoteItem.getName());
            }
        }

        return quoteItemsByQuote.get(quoteId);
    }

    void logBilling(String workId, QuoteImportItem quoteImportItem, QuotePriceItem quotePriceItem,
                    Set<String> billedPdoKeys, Object sapDocumentID) {
        String billingLogText = String.format(
                "Work item '%s' and SAP Document '%s' with completion date of '%s' posted at '%s' for '%2.2f' units of '%s' on behalf of %s in '%s'",
                workId,
                sapDocumentID,
                BILLING_DATE_FORMAT.format(quoteImportItem.getWorkCompleteDate()),
                BILLING_DATE_FORMAT.format(new Date()),
                quoteImportItem.getQuantity(),
                quotePriceItem.getName(),
                quoteImportItem.getNumSamples(),
                billedPdoKeys);
        log.info(billingLogText);
    }

    /**
     * @return returns a list of unique PDO keys for a list of billing results.
     */
    private Set<String> getBilledPdoKeys(BillingEjb.BillingResult billingResult) {
        Set<String> pdoKeys = new HashSet<>();
        for (LedgerEntry ledgerEntry : billingResult.getQuoteImportItem().getLedgerItems()) {
            pdoKeys.add(ledgerEntry.getProductOrderSample().getProductOrder().getBusinessKey());
        }

        return pdoKeys;
    }

    /**
     * Separation of the action of calling to Jira and updating PDOs associated with Billing Items.  Broken up to allow
     * each call to have it's own Transaction since Jira may take a very long time to respond.
     *
     * @param billingResults collection of "Billing Results".  Each one represents the aggregation of billing
     *                       charges and will record the success or failure of the billing attempt
     */
    private void updateBilledPdos(Collection<BillingEjb.BillingResult> billingResults) {

        Set<String> updatedPDOs = new HashSet<>();
        for (BillingEjb.BillingResult result : billingResults) {
            if (result.getQuoteImportItem().getBillingMessage().equals(BillingSession.SUCCESS)) {
                updatedPDOs.addAll(result.getQuoteImportItem().getOrderKeys());
            }
        }

        // Update the state of all PDOs affected by this billing session.
        for (String key : updatedPDOs) {
            try {
                // Update the order status using the ProductOrderEjb with a version of the order status update
                // method that does not mark transactions for rollback in the event that JIRA-related RuntimeExceptions
                // are thrown.  It is still possible that this method will throw a checked exception,
                // but these will not mark the transaction for rollback.
                productOrderEjb.updateOrderStatusNoRollback(key);
            } catch (Exception e) {
                // Errors are just logged here because the current user doesn't work with PDOs, and wouldn't
                // be able to resolve these issues.  Exceptions should only occur if a required resource,
                // such as JIRA, is missing.
                log.error("Failed to update PDO status after billing: " + key, e);
            }
        }
    }

    /**
     * Given a price item, this method will compare the price of the price item on the price list with the Quote
     * line item (if one exists) on a given quote to see which price is lower.  The lower of the two is returned
     *
     * @param primaryPriceItem Price item defined on a product
     * @param orderQuote       Quote associated with the product order from which the product that defined the
     *                         price item is associated
     * @return Lowest price between the pricelist item and the quote item (if one exists)
     * @throws InvalidProductException   Thrown if the price item from the product orders product is not found on the
     * price list
     */
    public String getEffectivePrice(PriceItem primaryPriceItem, Quote orderQuote, PriceList sourceOfPrices) throws InvalidProductException {

        final QuotePriceItem cachedPriceItem = sourceOfPrices.findByKeyFields(primaryPriceItem);
        if(cachedPriceItem == null) {
            throw new InvalidProductException("The price item "+primaryPriceItem.getDisplayName()+" does not exist");
        }
        return getEffectivePrice(cachedPriceItem, orderQuote);
    }

    public String getEffectivePrice(QuotePriceItem cachedPriceItem, Quote orderQuote) {
        String price = cachedPriceItem.getPrice();
        QuoteItem foundMatchingQuoteItem = orderQuote.findCachedQuoteItem(cachedPriceItem.getPlatformName(),
                cachedPriceItem.getCategoryName(), cachedPriceItem.getName());
        if (foundMatchingQuoteItem  != null && !orderQuote.getExpired()) {
            if (new BigDecimal(foundMatchingQuoteItem .getPrice()).compareTo(new BigDecimal(cachedPriceItem.getPrice())) < 0) {
                price = foundMatchingQuoteItem .getPrice();
            }
        }
        return price;
    }

    public List<String> getEffectivePricesForProducts(List<Product> products, Quote orderQuote,
                                                      PriceList sourceOfPrices,
                                                      ProductOrder order)
            throws InvalidProductException {
        List<String> orderedPrices = new ArrayList<>();

        for (Product product : products) {

            PriceItem priceItem = order.determinePriceItemByCompanyCode(product);

            orderedPrices.add(getEffectivePrice(priceItem, orderQuote, sourceOfPrices));
        }

        return orderedPrices;
    }

    @Inject
    public void setProductOrderEjb(ProductOrderEjb productOrderEjb) {
        this.productOrderEjb = productOrderEjb;
    }

    public SAPAccessControlEjb getSapAccessControlEjb() {
        return sapAccessControlEjb;
    }

}
