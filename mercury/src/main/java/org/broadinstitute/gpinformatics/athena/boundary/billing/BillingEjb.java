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
import java.util.Date;

@Stateful
@RequestScoped
public class BillingEjb {

    private static final Log log = LogFactory.getLog(BillingEjb.class);

    public static final String NO_ITEMS_TO_BILL_ERROR_TEXT =
            "There are no items available to bill in this billing session";
    public static final String LOCKED_SESSION_TEXT =
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
     * @param billingSessionKey business key of the billing session which is to be found and locked
     *
     * @return the locked billing session if this thread was able to successfully lock it for billing
     *
     * @throws BillingException if the billing session is locked for billing by another thread
     */
    public BillingSession findAndLockSession(@Nonnull String billingSessionKey) {

        BillingSession session;
        session = billingSessionDao.findByBusinessKeyWithLock(billingSessionKey);

        if (session.isSessionLocked()) {
            throw new BillingException(BillingEjb.LOCKED_SESSION_TEXT);
        }

        session.lockSession();
        billingSessionDao.persist(session);
        return session;
    }

    /**
     * Transactional method to unlock a billing session.  The end of the transaction will save the contents of the
     * billing session entity
     *
     * @param billingSession billing session to be unlocked
     */
    public void saveAndUnlockSession(@Nonnull BillingSession billingSession) {
        log.info("Setting billing session BILL-" + billingSession.getBillingSessionId() + " to unlocked");
        billingSession.unlockSession();
        billingSessionDao.persist(billingSession);
    }

    /**
     * Separation of the action of calling the quote server and updating the associated ledger entries.  This is to
     * separate the steps of billing a session into smaller finite transactions so we can record more to the database
     * sooner
     *
     * @param item             Representation of the quote and its ledger entries that are to be billed
     * @param quoteIsReplacing Set if the price item is replacing a previously defined item.
     */
    public void updateQuoteItem(QuoteImportItem item, QuotePriceItem quoteIsReplacing) {

        // Now that we have successfully billed, update the Ledger Entries associated with this QuoteImportItem
        // with the quote for the QuoteImportItem, add the priceItemType, and the success message.
        item.updateQuoteIntoLedgerEntries(quoteIsReplacing, BillingSession.SUCCESS);
        billingSessionDao.flush();
    }
}
