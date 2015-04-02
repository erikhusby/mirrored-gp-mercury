package org.broadinstitute.gpinformatics.athena.boundary.billing;

import com.google.common.collect.HashMultimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Billing Adaptor was derived to provide the ability to provide singular interface calls related to billingEJB
 * methods when simply calling BillingEJB will not suffice
 */
@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class BillingAdaptor implements Serializable {

    private static final Log log = LogFactory.getLog(BillingAdaptor.class);

    BillingEjb billingEjb;

    BillingSessionDao billingSessionDao;

    PriceListCache priceListCache;

    ProductOrderEjb productOrderEjb;

    QuoteService quoteService;

    BillingSessionAccessEjb billingSessionAccessEjb;

    private static final FastDateFormat BILLING_DATE_FORMAT =
            FastDateFormat.getDateTimeInstance(FastDateFormat.SHORT, FastDateFormat.SHORT);

    @Inject
    public BillingAdaptor(BillingEjb billingEjb, BillingSessionDao billingSessionDao,PriceListCache priceListCache,
                          QuoteService quoteService,BillingSessionAccessEjb billingSessionAccessEjb) {
        this.billingEjb = billingEjb;
        this.billingSessionDao = billingSessionDao;
        this.priceListCache = priceListCache;
        this.quoteService = quoteService;
        this.billingSessionAccessEjb = billingSessionAccessEjb;
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

        BillingSession billingSession = billingSessionAccessEjb.findAndLockSession(sessionKey);
        try {
            List<QuoteImportItem> unBilledQuoteImportItems =
                    billingSession.getUnBilledQuoteImportItems(priceListCache);

            if (unBilledQuoteImportItems.isEmpty()) {
                billingEjb.endSession(billingSession);
                throw new BillingException(BillingEjb.NO_ITEMS_TO_BILL_ERROR_TEXT);
            }

            HashMultimap<String, String> quoteItemsByQuote = HashMultimap.create();
            for (QuoteImportItem item : unBilledQuoteImportItems) {

                BillingEjb.BillingResult result = new BillingEjb.BillingResult(item);
                results.add(result);

                Quote quote = new Quote();
                quote.setAlphanumericId(item.getQuoteId());

                // The price item that we are billing.
                QuotePriceItem priceItemBeingBilled = QuotePriceItem.convertMercuryPriceItem(item.getPriceItem());

                // Get the quote PriceItem that this is replacing, if it is a replacement.
                QuotePriceItem primaryPriceItemIfReplacement = item.getPrimaryForReplacement(priceListCache);

                try {
                    // Get the quote items on the quote, adding to the quote item cache, if not there.
                    Collection<String> quoteItemNames = getQuoteItems(quoteItemsByQuote, item.getQuoteId());

                    // If this is a replacement, but the primary is not on the quote, set the primary to null so it will
                    // be billed as if it is a primary.
                    if (primaryPriceItemIfReplacement != null) {
                        if (!quoteItemNames.contains(primaryPriceItemIfReplacement.getName()) &&
                                quoteItemNames.contains(priceItemBeingBilled.getName())) {
                            primaryPriceItemIfReplacement = null;
                        }
                    }

                    String workId = quoteService.registerNewWork(quote, priceItemBeingBilled, primaryPriceItemIfReplacement,
                                                                 item.getWorkCompleteDate(), item.getQuantity(),
                                                                 pageUrl, "billingSession", sessionKey);

                    result.setWorkId(workId);
                    Set<String> billedPdoKeys = getBilledPdoKeys(result);
                    logBilling(workId, item, priceItemBeingBilled, billedPdoKeys);
                    billingEjb.updateLedgerEntries(item, primaryPriceItemIfReplacement, workId);
                } catch (Exception ex) {

                    String errorMessage;
                    if (StringUtils.isBlank(result.getWorkId())) {
                        errorMessage = "A problem occurred attempting to post to the quote server for " +
                                       billingSession.getBusinessKey() + ".";
                    } else {
                        errorMessage = "A problem occurred saving the ledger entries for " +
                                       billingSession.getBusinessKey() + " with work id of " + result.getWorkId()
                                       + ".  " +
                                       "The quote for this item may have been successfully sent to the quote server";
                    }

                    log.error(errorMessage + ex.toString());

                    item.setBillingMessages(errorMessage + ex.getMessage());
                    result.setErrorMessage(errorMessage + ex
                            .getMessage());
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

    private Collection<String> getQuoteItems(HashMultimap<String, String> quoteItemsByQuote, String quoteId) throws Exception {
        if (!quoteItemsByQuote.containsKey(quoteId)) {
            Quote quote = quoteService.getQuoteWithPriceItems(quoteId);
            for (QuoteItem quoteItem : quote.getQuoteItems()) {
                quoteItemsByQuote.put(quoteId, quoteItem.getName());
            }
        }

        return quoteItemsByQuote.get(quoteId);
    }

    void logBilling(String workId, QuoteImportItem quoteImportItem, QuotePriceItem quotePriceItem, Set<String> billedPdoKeys) {
        String billingLogText = String.format(
                "Work item '%s' with completion date of '%s' posted at '%s' for '%2.2f' units of '%s' on behalf of %s in '%s'",
                workId,
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

    @Inject
    public void setProductOrderEjb(ProductOrderEjb productOrderEjb) {
        this.productOrderEjb = productOrderEjb;
    }
}
