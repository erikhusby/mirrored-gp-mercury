package org.broadinstitute.gpinformatics.athena.boundary.billing;

import com.google.common.collect.HashMultimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.PriceAdjustment;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.sap.services.SAPIntegrationException;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
    public static final String BILLING_LOG_TEXT_FORMAT =
        "Work item '%s' and SAP Document '%s' and SAP Return Order '%s' with completion date of '%s' posted at '%s' for '%2.2f' units of '%s' on behalf of %s in '%s'";
    public static final String CREDIT_QUANTITY_INVALID = "Credit value must be less than zero.";
    public static final String NEGATIVE_BILL_ERROR =
        "Attempt to create a billing credit for a quantity exceeding the previously billed quantity.";
    public static final String INVOICE_NOT_FOUND = "Invoice not found against the entered delivery";
    private static final FastDateFormat BILLING_DATE_FORMAT =
        FastDateFormat.getDateTimeInstance(FastDateFormat.SHORT, FastDateFormat.SHORT);
    public static final String NON_SAP_ITEM_ERROR_MESSAGE = "Attempt to bill a non-SAP item in SAP";
    public static final String POSITIVE_QTY_ERROR_MESSAGE = "Attempt credit an item which has a positive quantity.";
    private BillingEjb billingEjb;

    private PriceListCache priceListCache;

    private ProductOrderEjb productOrderEjb;

    private QuoteService quoteService;

    private BillingSessionAccessEjb billingSessionAccessEjb;

    private SapIntegrationService sapService;

    private SAPProductPriceCache    productPriceCache;

    private SAPAccessControlEjb sapAccessControlEjb;

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

        List<BillingEjb.BillingResult> allResults = new ArrayList<>();

        BillingSession billingSession = billingSessionAccessEjb.findAndLockSession(sessionKey);

        try {
            List<QuoteImportItem> unBilledQuoteImportItems = null;
            PriceList priceItemsForDate = null;
            try {
                unBilledQuoteImportItems = billingSession.getUnBilledQuoteImportItems(priceListCache);
            } catch (QuoteServerException e) {
                throw new BillingException("Getting unbilled items failed because::" + e.getMessage(), e);
            }

            if (unBilledQuoteImportItems.isEmpty()) {
                billingEjb.endSession(billingSession);
                throw new BillingException(BillingEjb.NO_ITEMS_TO_BILL_ERROR_TEXT);
            }

            HashMultimap<String, String> quoteItemsByQuote = HashMultimap.create();
            for (QuoteImportItem item : unBilledQuoteImportItems) {
                Set<BillingEjb.BillingResult> itemResults = new HashSet<>();
                try {
                    if (item.isSapOrder()) {
                        item.setSapQuote(item.getProductOrder().getSapQuote(sapService));
                    }

                    // TODO SGM -- Need an isfunded for SAP /\
                    if (!item.isBillingCredit()) {
                        if (item.getProductOrder().hasSapQuote()) {
                            ProductOrder.checkSapQuoteValidity(item.getSapQuote(),
                                DateUtils.truncate(item.getWorkCompleteDate(),
                                    Calendar.DATE));

                        } else {
                            ProductOrder
                                .checkQuoteValidity(item.getQuote(), DateUtils.truncate(item.getWorkCompleteDate(),
                                    Calendar.DATE));
                        }
                    }

                    if (item.getProductOrder().getQuoteSource() != null) {
                        if (item.getProductOrder().hasQuoteServerQuote()) {
                            itemResults.add(billQuoteServer(item, quoteItemsByQuote, pageUrl, sessionKey));
                        }

                        if (item.getProductOrder().hasSapQuote()) {
                            BigDecimal quantityForSAP = item.getQuantityForSAP();
                            if (StringUtils.isBlank(item.getSapItems()) &&
                                CollectionUtils.isEmpty(item.getSapReturnOrders())) {

                                if (!item.getProductOrder().getOrderStatus().canPlace()) {
                                    if (StringUtils.isNotBlank(item.getProductOrder().getSapOrderNumber())) {

                                        if (quantityForSAP.compareTo(BigDecimal.ZERO) > 0) {
                                            itemResults.add(billSap(item));
                                        } else if (item.isBillingCredit()) {
                                            Collection<BillingCredit> billingCredits = handleBillingCredit(item);
                                            Set<BillingEjb.BillingResult> creditResults = billingCredits.stream()
                                                .map(BillingCredit::getBillingResult).collect(Collectors.toSet());
                                            itemResults.addAll(creditResults);
                                        }
                                        item.getProductOrder().latestSapOrderDetail()
                                            .addLedgerEntries(item.getLedgerItems());
                                    } else {
                                        throw new BillingException("This order"
                                                                   + " is not associated with an SAP Order.  Please "
                                                                   + "return to the Product Order view page and "
                                                                   + "publish to SAP");
                                    }
                                } else {
                                    throw new BillingException("This order has a status of " +
                                                               item.getProductOrder().getOrderStatus().getDisplayName()
                                                               + " and is not in a state to bill.  The order must not "
                                                               + "be Draft or Pending to proceed.");
                                }
                            }
                        }
                    } else {
                        throw new BillingException("Unable to determine the source of the Quote for the order.");
                    }
                } catch (Exception ex) {

                    for (BillingEjb.BillingResult result : itemResults) {
                        StringBuilder errorMessageBuilder = new StringBuilder();
                        String workId = result.getWorkId();
                        QuoteImportItem quoteImportItem = result.getQuoteImportItem();
                        if (!result.isBilledInQuoteServer() && StringUtils.isBlank(workId)
                            && quoteImportItem.isQuoteServerOrder()) {
                            errorMessageBuilder.append("A problem occurred attempting to post to the quote server for ")
                                .append(billingSession.getBusinessKey()).append(".");

                        } else if (!result.isBilledInSap() && quoteImportItem.isQuoteFunded() && quoteImportItem
                            .isSapOrder()) {
                            errorMessageBuilder.append("A problem occurred attempting to post to SAP for ")
                                .append(billingSession.getBusinessKey()).append(".");

                        } else {
                            errorMessageBuilder.append("A problem occurred saving the ledger entries for ")
                                .append(billingSession.getBusinessKey()).append(" with an SAP ID of ")
                                .append(result.isBilledInSap() ? result.getSapBillingId() : "").append(",")
                                .append(" with work id of ")
                                .append(result.isBilledInQuoteServer() ? workId : workId)
                                .append(".  ")
                                .append("The quote for this item may have been successfully sent to ")
                                .append(result.isBilledInSap() ? "SAP" : "the quote server");
                        }

                        log.error(errorMessageBuilder + " " + ex.toString(), ex);

                        String errorMessage = errorMessageBuilder + " " + ex.getMessage();
                        quoteImportItem.setBillingMessages(errorMessage);
                        result.setErrorMessage(errorMessage);
                        errorsInBilling = true;
                    }
                }
                if (!errorsInBilling) {
                    errorsInBilling = itemResults.stream().anyMatch(BillingEjb.BillingResult::isError);
                }
                allResults.addAll(itemResults);
            }
        } finally {
            billingSessionAccessEjb.saveAndUnlockSession(billingSession);
        }

        // If there were no errors in billing, then end the session, which will add the billed date and remove
        // all sessions from the ledger.
        if (!errorsInBilling) {
            billingEjb.endSession(billingSession);
        }

        return allResults;
    }

    /**
     * Handle billing of an SAP item.
     *
     * @return BillingResult with the result of billing attempt.
     */
    private BillingEjb.BillingResult billSap(QuoteImportItem item) throws Exception {
        if (!item.isSapOrder()) {
            throw new Exception("Attempt to bill non-SAP item in SAP");
        }
        BillingEjb.BillingResult result = new BillingEjb.BillingResult(item);
        try {
            String sapBillingId;
            sapBillingId = sapService.billOrder(item, null, item.getWorkCompleteDate());
            result.setSapBillingId(sapBillingId);
            billingEjb.updateSapQuoteImportItem(item, null, sapBillingId, BillingSession.SUCCESS);
            logSapBilling(item, getBilledPdoKeys(Collections.singleton(result)), sapBillingId);
        } catch (SAPIntegrationException e) {
            result.setErrorMessage(e.getMessage());
        }
        return result;
    }

    /**
     * Handle billing an item in the Quote Server.
     *
     * @return BillingResult with results of billing attempt.
     */
    private BillingEjb.BillingResult billQuoteServer(QuoteImportItem item,
                                                     HashMultimap<String, String> quoteItemsByQuote, String pageUrl,
                                                     String sessionKey) throws Exception {
        if (item.isSapOrder()) {
            throw new Exception("Attempt to bill a non-Quote Server item in the Quote Server");
        }
        BillingEjb.BillingResult result = new BillingEjb.BillingResult(item);
        try {
            PriceList priceItemsForDate = quoteService.getPriceItemsForDate(Collections.singletonList(item));

            item.setPriceOnWorkDate(priceItemsForDate);
            item.setQuote(item.getProductOrder().getQuote(quoteService));

            // The price item that we are billing.
            // need to set the price on the Price Item before this step
            QuotePriceItem priceItemBeingBilled = QuotePriceItem.convertMercuryPriceItem(item.getPriceItem());

            // Get the quote PriceItem that this is replacing, if it is a replacement.
            // need to set the price on the Price Item before this step
            QuotePriceItem primaryPriceItemIfReplacement = item.getPrimaryForReplacement(priceItemsForDate);

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

            PriceAdjustment singlePriceAdjustment = item.getProductOrder().getAdjustmentForProduct(item.getProduct());
            String workId;
            if (singlePriceAdjustment == null) {
                workId = quoteService
                    .registerNewWork(item.getQuote(), priceItemBeingBilled, primaryPriceItemIfReplacement,
                        item.getWorkCompleteDate(), item.getQuantity(), pageUrl, "billingSession", sessionKey, null);
            } else {
                workId = quoteService
                    .registerNewWork(item.getQuote(), priceItemBeingBilled, primaryPriceItemIfReplacement,
                        item.getWorkCompleteDate(), item.getQuantity(), pageUrl, "billingSession", sessionKey,
                        singlePriceAdjustment.getAdjustmentValue());
            }

            billingEjb.updateLedgerEntries(item, primaryPriceItemIfReplacement, workId, NOT_ELIGIBLE_FOR_SAP_INDICATOR,
                BillingSession.SUCCESS);
            result.setWorkId(workId);

            Set<String> billedPdoKeys = getBilledPdoKeys(Collections.singleton(result));
            logQuoteServerBilling(workId, item, priceItemBeingBilled, billedPdoKeys);
        } catch (Exception e) {
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Handle billing credits for an SAP item.
     *
     * @return BillingResult with results of billing credit attempt.
     */
    public Collection<BillingCredit> handleBillingCredit(final QuoteImportItem item) throws Exception {
        Set<BillingCredit> errorCredits = new HashSet<>();
        if (!item.isSapOrder()) {
            item.setBillingMessages(NON_SAP_ITEM_ERROR_MESSAGE);

            BillingEjb.BillingResult billingResult = new BillingEjb.BillingResult(item);
            billingResult.setErrorMessage(NON_SAP_ITEM_ERROR_MESSAGE);
            BillingCredit billingCredit = new BillingCredit(null, null);
            billingCredit.setBillingResult(billingResult);
            errorCredits.add(billingCredit);
            return errorCredits;
        }
        if (!item.isBillingCredit()) {
            item.setBillingMessages(POSITIVE_QTY_ERROR_MESSAGE);
            BillingEjb.BillingResult billingResult = new BillingEjb.BillingResult(item);
            billingResult.setErrorMessage(POSITIVE_QTY_ERROR_MESSAGE);
            BillingCredit billingCredit = new BillingCredit(null, null);
            billingCredit.setBillingResult(billingResult);
            errorCredits.add(billingCredit);
            return errorCredits;
        }

        Collection<BillingCredit> billingCredits = new HashSet<>();
        try {
            billingCredits.addAll(BillingCredit.setupSapCredits(item));
        } catch (Exception e) {
            BillingEjb.BillingResult billingResult = new BillingEjb.BillingResult(item);
            item.setBillingMessages(e.getMessage());
            billingResult.setErrorMessage(e.getMessage());
            BillingCredit billingCredit = new BillingCredit(null, null);
            billingCredit.setBillingResult(billingResult);
            errorCredits.add(billingCredit);
        }

        // There are cases where a single QuoteImportItem will need to be credited to more than one sapDocumentId.
        for (BillingCredit billingCredit : billingCredits) {
            try {
                Set<LedgerEntry> updatedLedgers = billingCredit.getReturnLines().stream()
                    .map(BillingCredit.LineItem::getLedgerEntry).collect(Collectors.toSet());

                BillingEjb.BillingResult billingResult = new BillingEjb.BillingResult(item);
                billingCredit.setBillingResult(billingResult);
                String billingMessage;
                try {
                    String returnOrderId = sapService.creditDelivery(billingCredit);
                    if (StringUtils.isNotBlank(returnOrderId)) {
                        billingCredit.setReturnOrderId(returnOrderId);
                        billingMessage = BillingSession.SUCCESS;
                    } else {
                        billingMessage = handleBillingCreditInvoiceNotFound(billingCredit);
                    }
                    billingResult.setSapBillingId(billingCredit.getReturnOrderId());

                } catch (SAPIntegrationException e) {
                    billingMessage = e.getLocalizedMessage();
                    if (billingMessage.contains(INVOICE_NOT_FOUND)) {
                        billingMessage = handleBillingCreditInvoiceNotFound(billingCredit);
                        billingResult.setSapBillingId(billingCredit.getSourceLedger().getSapDeliveryDocumentId());
                    } else {

                        // the billing credit failed so clear out the sapDeliveryDocumentId to prevent the ledgers
                        // from being incorrectly updated.
                        billingCredit.setSourceLedger(null);
                        billingResult.setErrorMessage(billingMessage);
                    }
                }
                if (Arrays.asList(BillingSession.SUCCESS, BillingSession.BILLING_CREDIT).contains(billingMessage)) {
                    billingCredit.getReturnLines().forEach(lineItem -> {
                        billingCredit.getSourceLedger().addCredit(lineItem.getLedgerEntry(), lineItem.getQuantity());
                    });
                }

                billingEjb.updateSapLedgerEntries(updatedLedgers, item.getQuoteId(), item.getSingleWorkItem(),
                    billingCredit.getSourceLedger().getSapDeliveryDocumentId(), billingCredit.getReturnOrderId(),
                    billingMessage);
                Set<String> billedPdos = billingCredit.getBillingResult().getQuoteImportItem().getLedgerItems()
                    .stream().map(le -> le.getProductOrderSample().getProductOrder().getBusinessKey())
                    .collect(Collectors.toSet());
                if (!billingResult.isError()) {
                    logSapBillingCredit(item, billedPdos, billingCredit.getSourceLedger().getSapDeliveryDocumentId(),
                        billingCredit.getReturnOrderId());
                }
            } catch (BillingException billingException) {
                BillingEjb.BillingResult billingResult = billingCredit.getBillingResult();
                billingResult.setErrorMessage(billingException.getMessage());
                billingEjb.updateSapQuoteImportItem(item, null, null, billingException.getMessage());
            }
        }
        billingCredits.addAll(errorCredits);
        return billingCredits;
    }

    public String handleBillingCreditInvoiceNotFound(BillingCredit billingCredit) {
        billingCredit.setReturnOrderInvoiceNotFound();
        return BillingSession.BILLING_CREDIT;
    }

    /**
     * This grabs the quote items for a quoteId from the quote server. It maintains a cache that keeps it from going
     * to the quote server multiple times for the same quote id.
     *
     * @param quoteItemsByQuote The cache of quote Ids to the quote items.
     * @param quoteId           The quote to look up
     *
     * @return The list of quote items (price item names that belong to the quote).
     */
    private Collection<String> getQuoteItems(HashMultimap<String, String> quoteItemsByQuote, String quoteId)
        throws Exception {
        if (!quoteItemsByQuote.containsKey(quoteId)) {
            Quote quote = quoteService.getQuoteWithPriceItems(quoteId);
            for (QuoteItem quoteItem : quote.getQuoteItems()) {
                quoteItemsByQuote.put(quoteId, quoteItem.getName());
            }
        }

        return quoteItemsByQuote.get(quoteId);
    }
    private void logSapBilling(QuoteImportItem quoteImportItem, Set<String> billedPdoKeys, String sapDocumentId) {
        logBilling(null, quoteImportItem, null, billedPdoKeys, sapDocumentId, null);
    }

    private void logSapBillingCredit(QuoteImportItem quoteImportItem, Set<String> billedPdoKeys, String sapDocumentId,
                                     String sapReturnOrderId) {
        logBilling(null, quoteImportItem, null, billedPdoKeys, sapDocumentId, sapReturnOrderId);
    }

    private void logQuoteServerBilling(String workId, QuoteImportItem quoteImportItem, QuotePriceItem quotePriceItem,
                        Set<String> billedPdoKeys) {
        logBilling(workId, quoteImportItem, quotePriceItem, billedPdoKeys, null, null);
    }

    void logBilling(String workId, QuoteImportItem quoteImportItem, QuotePriceItem quotePriceItem,
                    Set<String> billedPdoKeys, String sapDocumentId, String sapReturnOrderId) {
        String priceItemName = "";
        if (quotePriceItem != null) {
            priceItemName = quoteImportItem.getProduct().getProductName();
        }
        String billingLogText = String.format(BILLING_LOG_TEXT_FORMAT,
            StringUtils.defaultIfBlank(workId, "N/A"),
            StringUtils.defaultIfBlank(sapDocumentId, "N/A"),
            StringUtils.defaultIfBlank(sapReturnOrderId, "N/A"),
            BILLING_DATE_FORMAT.format(quoteImportItem.getWorkCompleteDate()),
            BILLING_DATE_FORMAT.format(new Date()),
            quoteImportItem.getQuantity(),
            priceItemName,
            quoteImportItem.getNumSamples(),
            billedPdoKeys);
        log.info(billingLogText);
    }

    /**
     * @return returns a list of unique PDO keys for a list of billing results.
     */
    private Set<String> getBilledPdoKeys(Collection<BillingEjb.BillingResult> billingResults) {
        Set<String> pdoKeys = new HashSet<>();
        billingResults.forEach(billingResult->{
            billingResult.getQuoteImportItem().getLedgerItems().forEach(
                ledgerEntry -> pdoKeys.add(ledgerEntry.getProductOrderSample().getProductOrder().getBusinessKey()));
        });

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
            if (result.isSuccessfullyBilled()) {
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
     * @throws InvalidProductException Thrown if the price item from the product orders product is not found on the
     *                                 price list
     */
    public String getEffectivePrice(PriceItem primaryPriceItem, Quote orderQuote, PriceList sourceOfPrices)
        throws InvalidProductException {

        final QuotePriceItem cachedPriceItem = sourceOfPrices.findByKeyFields(primaryPriceItem);
        if (cachedPriceItem == null) {
            throw new InvalidProductException(
                "The price item " + primaryPriceItem.getDisplayName() + " does not exist");
        }
        return getEffectivePrice(cachedPriceItem, orderQuote);
    }

    public String getEffectivePrice(QuotePriceItem cachedPriceItem, Quote orderQuote) {
        String price = cachedPriceItem.getPrice();
        QuoteItem foundMatchingQuoteItem = orderQuote.findCachedQuoteItem(cachedPriceItem.getPlatformName(),
            cachedPriceItem.getCategoryName(), cachedPriceItem.getName());
        if (foundMatchingQuoteItem != null && !orderQuote.getExpired()) {
            if (new BigDecimal(foundMatchingQuoteItem.getPrice()).compareTo(new BigDecimal(cachedPriceItem.getPrice()))
                < 0) {
                price = foundMatchingQuoteItem.getPrice();
            }
        }
        return price;
    }

    @Inject
    public void setProductOrderEjb(ProductOrderEjb productOrderEjb) {
        this.productOrderEjb = productOrderEjb;
    }

    public SAPAccessControlEjb getSapAccessControlEjb() {
        return sapAccessControlEjb;
    }

}
