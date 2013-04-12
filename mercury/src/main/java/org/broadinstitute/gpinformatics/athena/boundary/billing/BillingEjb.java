package org.broadinstitute.gpinformatics.athena.boundary.billing;


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
     * the {@link org.broadinstitute.gpinformatics.athena.boundary.billing.BillingEjb.BillingResult#isError()} will return
     * true and {@link org.broadinstitute.gpinformatics.athena.boundary.billing.BillingEjb.BillingResult#getErrorMessage()}
     * will describe the cause of the problem.  On successful billing
     * {@link org.broadinstitute.gpinformatics.athena.boundary.billing.BillingEjb.BillingResult#getWorkId()} will contain
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

        List<BillingResult> results = new ArrayList<BillingResult>();
        Set<String> updatedPDOs = new HashSet<String>();

        for (QuoteImportItem item : billingSession.getUnBilledQuoteImportItems(priceListCache)) {

            BillingResult result = new BillingResult();
            results.add(result);
            result.setQuoteImportItem(item);

            Quote quote = new Quote();
            quote.setAlphanumericId(item.getQuoteId());

            QuotePriceItem quotePriceItem = QuotePriceItem.convertMercuryPriceItem(item.getPriceItem());

            // Calculate whether this is a replacement item and if it is, send the itemIsReplacing field, otherwise
            // the itemIsReplacing field will be null.
            org.broadinstitute.gpinformatics.athena.entity.products.PriceItem mercuryIsReplacing =
                    item.calculateIsReplacing(priceListCache);

            // Get the quote version of the price item for the item that is being replaced.
            QuotePriceItem quoteIsReplacing = null;
            if (mercuryIsReplacing != null) {
                quoteIsReplacing = QuotePriceItem.convertMercuryPriceItem(mercuryIsReplacing);
            }

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
        try {
            productOrderEjb.updateOrderStatus(updatedPDOs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return results;
    }
}
