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

            for (QuoteImportItem item : unBilledQuoteImportItems) {

                BillingEjb.BillingResult result = new BillingEjb.BillingResult(item);
                results.add(result);

                Quote quote = new Quote();
                quote.setAlphanumericId(item.getQuoteId());

                QuotePriceItem quotePriceItem = QuotePriceItem.convertMercuryPriceItem(item.getPriceItem());

                // Get the quote PriceItem that this is replacing, if it is a replacement.
                QuotePriceItem quoteIsReplacing = item.getPrimaryForReplacement(priceListCache);

                try {
                    String workId = quoteService.registerNewWork(quote, quotePriceItem, quoteIsReplacing,
                                                                 item.getWorkCompleteDate(), item.getQuantity(),
                                                                 pageUrl, "billingSession", sessionKey);

                    result.setWorkId(workId);
                    item.setWorkItems(workId);
                    log.info("workId" + workId + " for " + item.getLedgerItems().size() + " ledger items at "
                             + new Date());

                    billingEjb.updateQuoteItem(item, quoteIsReplacing);
                } catch (Exception ex) {

                    String errorMessage;
                    if (StringUtils.isBlank(result.getWorkId())) {
                        errorMessage = "A Problem occurred attempting to post to the quote server for " +
                                       billingSession.getBusinessKey() + ".";
                    } else {
                        errorMessage = "A problem occurred saving the ledger entries for " +
                                       billingSession.getBusinessKey() + " with work id of " + result.getWorkId()
                                       + ".  " +
                                       "The quote for this item may have been successfully sent to the quote server";
                    }

                    log.error(errorMessage, ex);

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
