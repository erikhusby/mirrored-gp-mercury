package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;

import javax.annotation.Nonnull;
import javax.ejb.EJBTransactionRolledbackException;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The Billing Adaptor was derived to provide the ability to provide singular interface calls related to billingEJB
 * methods when simply calling BillingEJB will not suffice
 */
public class BillingAdaptor implements Serializable {

    private static final Log log = LogFactory.getLog(BillingAdaptor.class);

    BillingEjb billingEjb;

    BillingSessionDao billingSessionDao;

    PriceListCache priceListCache;

    ProductOrderEjb productOrderEjb;

    @Inject
    public BillingAdaptor(BillingEjb billingEjb, BillingSessionDao billingSessionDao, PriceListCache priceListCache,
                          ProductOrderEjb productOrderEjb) {
        this.billingEjb = billingEjb;
        this.billingSessionDao = billingSessionDao;
        this.priceListCache = priceListCache;
        this.productOrderEjb = productOrderEjb;
    }

    protected BillingAdaptor() {
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

        List<BillingEjb.BillingResult> billingResults = new ArrayList<>();
        try {

            billingResults = bill(pageUrl, sessionKey);
            updateBilledPdos(billingResults);
        } catch (EJBTransactionRolledbackException e) {
            throw new RuntimeException("An error occurred during this billing Session.");
        }

        return billingResults;
    }

    /**
     * Non-Transactional method to bill each previously unbilled {@link QuoteImportItem} on the BillingSession to the
     * quote server and update billing entities as appropriate to the results of the billing attempt.  Results
     * for each billing attempt correspond to a returned BillingResult.  If there was an exception billing a
     * QuoteImportItem, the {@link BillingResult#isError()} will return true and {@link BillingResult#getErrorMessage()}
     * will describe the cause of the problem.  On successful billing {@link BillingResult#getWorkId()} will contain
     * the work id result.
     *
     * @param pageUrl    URL to be included in the call to the quote server.
     * @param sessionKey Key to be included in the call to the quote server.
     *
     * @return List of BillingResults describing the success or failure of billing for each previously unbilled QuoteImportItem
     * associated with the BillingSession.
     */
    private List<BillingEjb.BillingResult> bill(@Nonnull String pageUrl, @Nonnull String sessionKey) {

        boolean errorsInBilling = false;

        List<BillingEjb.BillingResult> results = new ArrayList<>();

        BillingSession billingSession = billingEjb.findAndLockSession(sessionKey);

        if (billingSession.isSessionLocked()) {
            return Collections.emptyList();
        }

        List<QuoteImportItem> unBilledQuoteImportItems =
                billingSession.getUnBilledQuoteImportItems(priceListCache);

        if (unBilledQuoteImportItems.isEmpty()) {
            billingEjb.endSession(billingSession);
            throw new BillingException(BillingEjb.NO_ITEMS_TO_BILL_ERROR_TEXT);
        }

        for (QuoteImportItem item : unBilledQuoteImportItems) {

            BillingEjb.BillingResult result = new BillingEjb.BillingResult();
            results.add(result);
            result.setQuoteImportItem(item);

            Quote quote = new Quote();
            quote.setAlphanumericId(item.getQuoteId());

            QuotePriceItem quotePriceItem = QuotePriceItem.convertMercuryPriceItem(item.getPriceItem());

            // Get the quote PriceItem that this is replacing, if it is a replacement.
            QuotePriceItem quoteIsReplacing = item.getPrimaryForReplacement(priceListCache);

            try {
                billingEjb.callQuoteAndUpdateQuoteItem(pageUrl, sessionKey, item, result, quote, quotePriceItem,
                                                       quoteIsReplacing);
            } catch (EJBTransactionRolledbackException rolledbackException) {

                String errorMessage;
                if(StringUtils.isBlank(result.getWorkId())) {
                    errorMessage = "A Problem occurred attempting to post to the quote server for " +
                                   billingSession.getBusinessKey() + ".";
                } else {
                    errorMessage = "A problem occurred saving the ledger entries for " +
                                   billingSession.getBusinessKey() + " with work id of " + result.getWorkId() + ".  " +
                                   "The quote for this item may have been successfully sent to the quote server";
                }

                log.error(errorMessage, rolledbackException);

                item.setBillingMessages(errorMessage + rolledbackException.getMessage());
                result.setErrorMessage(errorMessage + rolledbackException
                        .getMessage());
                errorsInBilling = true;
            } catch (Exception ex) {
                log.error("A problem occurred while submitting and logging a quote for " +
                          billingSession.getBusinessKey() + " that has " + item.getLedgerItems().size()
                          + " ledger items");
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
            billingEjb.endSession(billingSession);
        }

        // FIxMe  What happens if we get an exception at this point?  Do we need to have some better handling on the
        // Action bean to display to the user that there is a Locked session and it will need Informatics Intervention?
        billingEjb.saveAndUnlockSession(billingSession);

        return results;
    }

    /**
     * Separation of the action of calling to Jira and updating PDOs associated with Billing Items.  Broken up to allow
     * each call to have it's own Transaction since Jira may take a very long time to respond.
     *
     * @param billingResults collection of "Billing Results".  Each one represents the aggregation of billing
     *                       charges and will record the success or failure of the billing attempt
     */
    private void updateBilledPdos(Collection<BillingEjb.BillingResult> billingResults) {

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
