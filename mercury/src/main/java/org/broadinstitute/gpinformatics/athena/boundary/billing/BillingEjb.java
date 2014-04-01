package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Stateful
@RequestScoped
public class BillingEjb {

    private static final Log log = LogFactory.getLog(BillingEjb.class);

    public static final String NO_ITEMS_TO_BILL_ERROR_TEXT =
            "There are no items available to bill in this billing session";
    public static final String LOCKED_SESSION_TEXT=
            "This billing session is currently in the process of being processed for billing.  If you believe this " +
            "is in error, please contact the informatics group for assistance";

    /**
     * Encapsulates the results of a billing attempt on a {@link QuoteImportItem}, successful or otherwise.
     */
    public static class BillingResult {

        private QuoteImportItem quoteImportItem;

        private String workId;

        private String errorMessage;

        public QuoteImportItem getQuoteImportItem() {
            return quoteImportItem;
        }

        void setQuoteImportItem(QuoteImportItem quoteImportItem) {
            this.quoteImportItem = quoteImportItem;
        }

        public String getWorkId() {
            return workId;
        }

        void setWorkId(String workId) {
            this.workId = workId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public boolean isError() {
            return errorMessage != null;
        }
    }

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private QuoteService quoteService;

    @Inject
    private PriceListCache priceListCache;

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private ProductOrderEjb productOrderEjb;

    /**
     * Transactional method to end a billing session with appropriate handling for complete or partial failure.
     *
     * @param sessionKey The billing session's key
     */
    public void endSession(@Nonnull String sessionKey) {
        BillingSession billingSession = billingSessionDao.findByBusinessKeyWithLock(sessionKey);
        endSession(billingSession);
    }

    /**
     * Transactional method to end a billing session with appropriate handling for complete or partial failure.
     *
     * @param billingSession BillingSession to be ended.
     */
    public void endSession(@Nonnull BillingSession billingSession) {

        // Remove all the sessions from the non-billed items.
        boolean allFailed = billingSession.cancelSession();

        if (allFailed) {
            // If all removed then remove the session, totally.
            billingSessionDao.remove(billingSession);
        } else {
            // If some or all are billed, then just persist the updates.
            billingSessionDao.persist(billingSession);
        }
    }

    /**
     * Obtains an advisory lock on a billing session for the purpose of performing billing against the Quote Server. It
     * is EXTREMELY important that this method be called outside of a transactional context so that it will begin and
     * commit its own transaction. It is, in fact, so important that I think we should consider marking this method as
     * TransactionAttributeType.NEVER and have it call a TransactionAttributeType.REQUIRED method to do the actual work.
     *
     * @param billingSessionKey    business key of the billing session which is to be found and locked
     * @return the locked billing session if this thread was able to successfully lock it for billing
     * @throws BillingException if the billing session is locked for billing by another thread
     */
    public BillingSession findAndLockSession(@Nonnull String billingSessionKey) {
        BillingSession session = billingSessionDao.findByBusinessKeyWithLock(billingSessionKey);

        if (session.isSessionLocked()) {
            throw new BillingException(BillingEjb.LOCKED_SESSION_TEXT);
        }

        session.lockSession();

        return session;
    }

    /**
     * Transactional method to unlock a billing session.  The end of the transaction will save the contents of the
     * billing session entity
     * @param billingSession    billing session to be unlocked
     */
    public void saveAndUnlockSession(@Nonnull BillingSession billingSession) {
        log.info("Setting billing session BILL-" + billingSession.getBillingSessionId() + " to unlocked");
        billingSession.unlockSession();
    }

    /**
     * Transactional method to bill each previously unbilled {@link QuoteImportItem} on the BillingSession to the quote
     * server and update billing entities as appropriate to the results of the billing attempt.  Results
     * for each billing attempt correspond to a returned BillingResult.  If there was an exception billing a QuoteImportItem,
     * the {@link BillingResult#isError()} will return
     * true and {@link BillingResult#getErrorMessage()}
     * will describe the cause of the problem.  On successful billing
     * {@link BillingResult#getWorkId()} will contain
     * the work id result.
     *
     * @param pageUrl    URL to be included in the call to the quote server.
     * @param sessionKey Key to be included in the call to the quote server.
     *
     * @return List of BillingResults describing the success or failure of billing for each previously unbilled QuoteImportItem
     * associated with the BillingSession.
     *
     * @deprecated This method will no longer be used.  Instead, the method defined in BillingAdaptor should be used
     */
    @Deprecated
    public List<BillingResult> bill(@Nonnull String pageUrl, @Nonnull String sessionKey) {

        boolean errorsInBilling = false;

        List<BillingResult> results = new ArrayList<>();

        BillingSession billingSession = billingSessionDao.findByBusinessKeyWithLock(sessionKey);
        List<QuoteImportItem> unBilledQuoteImportItems =
                billingSession.getUnBilledQuoteImportItems(priceListCache);

        if (unBilledQuoteImportItems.isEmpty()) {
            endSession(billingSession);
            throw new BillingException(NO_ITEMS_TO_BILL_ERROR_TEXT);
        }

        for (QuoteImportItem item : unBilledQuoteImportItems) {

            BillingResult result = new BillingResult();
            results.add(result);
            result.setQuoteImportItem(item);

            Quote quote = new Quote();
            quote.setAlphanumericId(item.getQuoteId());

            QuotePriceItem quotePriceItem = QuotePriceItem.convertMercuryPriceItem(item.getPriceItem());

            // Get the quote PriceItem that this is replacing, if it is a replacement.
            QuotePriceItem quoteIsReplacing = item.getPrimaryForReplacement(priceListCache);

            try {
                callQuoteAndUpdateQuoteItem(pageUrl, sessionKey, item, result, quote, quotePriceItem, quoteIsReplacing);

            } catch (Exception ex) {
                // Any exceptions in sending to the quote server will just be reported and will continue
                // on to the next one.
                item.setBillingMessages(ex.getMessage());
                result.setErrorMessage(ex.getMessage());
                errorsInBilling = true;
            }
        }

        // If there were no errors in billing, then end the session, which will add the billed date and remove
        // all sessions from the ledger.
        if (!errorsInBilling) {
            endSession(billingSession);
        }

        return results;
    }

    /**
     * Separation of the action of calling the quote server and updating the associated ledger entries.  This is to
     * separate the steps of billing a session into smaller finite transactions so we can record more to the database
     * sooner
     *
     * @param pageUrl          URL to be included in the call to the quote server.
     * @param sessionKey       Business key of the Billing session of which the price item is associated
     * @param item             Representation of the quote and its ledger entries that are to be billed
     * @param result           Outcome of to be reported back to the user of the attempt to bill the quote item
     * @param quote            in depth representation of quote to which the item is being charged
     * @param quotePriceItem   Quote server representation of the Price item that is represented in mercury.
     * @param quoteIsReplacing Set if the price item is replacing a previously defined item.
     */
    public void callQuoteAndUpdateQuoteItem(String pageUrl, String sessionKey, QuoteImportItem item,
                                            BillingResult result, Quote quote, QuotePriceItem quotePriceItem,
                                            QuotePriceItem quoteIsReplacing) {

        try {
            String workId = quoteService.registerNewWork(
                    quote, quotePriceItem, quoteIsReplacing, item.getWorkCompleteDate(), item.getQuantity(),
                    pageUrl, "billingSession", sessionKey);

            result.setWorkId(workId);
            log.info("workId" + workId + " for " + item.getLedgerItems().size() + " ledger items at " + new Date());
        } catch (RuntimeException e) {
            throw new BillingException(e.getMessage(), e);
        }

        // Now that we have successfully billed, update the Ledger Entries associated with this QuoteImportItem
        // with the quote for the QuoteImportItem, add the priceItemType, and the success message.
        item.updateQuoteIntoLedgerEntries(quoteIsReplacing, BillingSession.SUCCESS);
    }

    /**
     * Separation of the action of calling to Jira and updating
     *
     * @param billingResults
     *
     * @deprecated This is deprecated.  Reference method in BillingAdaptor
     */
    @Deprecated
    public void updateBilledPdos(Collection<BillingResult> billingResults) {

        Collection<String> updatedPDOs = new ArrayList<>();
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
}
