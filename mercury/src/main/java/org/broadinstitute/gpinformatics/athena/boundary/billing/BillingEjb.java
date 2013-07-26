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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Stateful
@RequestScoped
public class BillingEjb {

    private static final Log log = LogFactory.getLog(BillingEjb.class);

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
        BillingSession billingSession = billingSessionDao.findByBusinessKey(sessionKey);
        endSession(billingSession);
    }

    /**
     * Transactional method to end a billing session with appropriate handling for complete or partial failure.
     *
     * @param billingSession BillingSession to be ended.
     */
    private void endSession(@Nonnull BillingSession billingSession) {

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
     * Transactional method to bill each previously unbilled {@link QuoteImportItem} on the BillingSession to the quote
     * server and update billing entities as appropriate to the results of the billing attempt.  Results
     * for each billing attempt correspond to a returned BillingResult.  If there was an exception billing a QuoteImportItem,
     * the {@link BillingResult#isError()} will return
     * true and {@link BillingResult#getErrorMessage()}
     * will describe the cause of the problem.  On successful billing
     * {@link BillingResult#getWorkId()} will contain
     * the work id result.
     *
     *
     * @param pageUrl        URL to be included in the call to the quote server.
     * @param sessionKey     Key to be included in the call to the quote server.
     *
     * @return List of BillingResults describing the success or failure of billing for each previously unbilled QuoteImportItem
     *         associated with the BillingSession.
     */
    public List<BillingResult> bill(@Nonnull String pageUrl, @Nonnull String sessionKey) {

        BillingSession billingSession = billingSessionDao.findByBusinessKey(sessionKey);

        boolean errorsInBilling = false;

        List<BillingResult> results = new ArrayList<>();
        Set<String> updatedPDOs = new HashSet<>();

        for (QuoteImportItem item : billingSession.getUnBilledQuoteImportItems(priceListCache)) {

            BillingResult result = new BillingResult();
            results.add(result);
            result.setQuoteImportItem(item);

            Quote quote = new Quote();
            quote.setAlphanumericId(item.getQuoteId());

            QuotePriceItem quotePriceItem = QuotePriceItem.convertMercuryPriceItem(item.getPriceItem());

            // Get the quote PriceItem that this is replacing, if it is a replacement.
            QuotePriceItem quoteIsReplacing = item.getPrimaryForReplacement(priceListCache);

            try {
                String workId = quoteService.registerNewWork(
                        quote, quotePriceItem, quoteIsReplacing, item.getWorkCompleteDate(), item.getQuantity(),
                        pageUrl, "billingSession", sessionKey);

                result.setWorkId(workId);

                // Now that we have successfully billed, update the Ledger Entries associated with this QuoteImportItem
                // with the quote for the QuoteImportItem, add the priceItemType, and the success message.
                item.updateQuoteIntoLedgerEntries(quoteIsReplacing, BillingSession.SUCCESS);

                updatedPDOs.addAll(item.getOrderKeys());

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

        return results;
    }
}
