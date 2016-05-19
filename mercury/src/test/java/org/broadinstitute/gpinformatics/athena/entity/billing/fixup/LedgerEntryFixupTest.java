package org.broadinstitute.gpinformatics.athena.entity.billing.fixup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryFixupDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

@Test(groups = TestGroups.FIXUP)
public class LedgerEntryFixupTest extends Arquillian {

    private static final Log logger = LogFactory.getLog(LedgerEntryFixupTest.class);

    @Inject
    private UserBean userBean;

    @Inject
    private UserTransaction utx;

    @Inject
    private LedgerEntryFixupDao ledgerEntryFixupDao;

    @Inject
    private PriceListCache priceListCache;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private ProductOrderEjb productOrderEjb;

    // Use (RC, "rc"), (PROD, "prod") to push the backfill to RC and production respectively.
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }


    /**
     * Per the query below, no billing ledger records were found corresponding to billing sessions with a billed date
     * older than the date a quote was set into a PDO, including PDO quote updates.  Since PDO quote editing is
     * presently locked down, we know that all ledger entries should be backfilled with the quote currently set into the
     * PDO for the ledger entries' PDO samples.
     *
     *
     *  SELECT
     *    bs.BILLED_DATE,
     *    pdo.rev_date,
     *    pdo.JIRA_TICKET_KEY,
     *    pdo.QUOTE_ID
     *  FROM athena.BILLING_SESSION bs, athena.billing_ledger bl, athena.product_order_sample pdo_s,
     *  -- Outer select that enables us to pick out only the rows from the inner query with rank = 1.  This yields the
     *  -- oldest row for each jira_ticket_key + quote_id combination.
     *    (SELECT
     *       *
     *     FROM (
     *       -- Select the fields corresponding to the audit record for a PDO, including a dense_rank() of this record
     *       -- among all records with this combination of jira_ticket_key + quote_id.  The ranking is ordered by
     *       -- rev_date ASC, which means the oldest record for this jira_ticket_key + quote_id combination would
     *       -- have rank 1.
     *       SELECT
     *         product_order_id,
     *         jira_ticket_key,
     *         quote_id,
     *         rev_date,
     *         dense_rank()
     *         OVER (PARTITION BY jira_ticket_key, quote_id
     *           ORDER BY rev_date ASC) rnk
     *       FROM
     *       -- Use the rev date from the Envers rev_info table for this audit record, the PDO modified_dates were not
     *       -- being properly set for a period in Mercury's history.
     *         (SELECT
     *            product_order_id,
     *            jira_ticket_key,
     *            quote_id,
     *            ri.REV_DATE
     *          FROM athena.product_order_aud pdo2, mercury.rev_info ri
     *          WHERE pdo2.rev = ri.rev_info_id
     *                -- Filter drafts PDOs and messaging tests.
     *                AND pdo2.jira_ticket_key IS NOT null AND pdo2.jira_ticket_key NOT LIKE '%MsgTest%')
     *     )
     *     WHERE rnk = 1
     *     ORDER BY jira_ticket_key, quote_id) pdo
     *
     *  -- Look for billing ledger entries belonging to a billing session with a billed date less than any quote
     *  -- assignment date for the billing ledger's PDO sample's PDO.
     *  WHERE pdo_s.product_order = pdo.product_order_id AND
     *        bl.PRODUCT_ORDER_SAMPLE_ID = pdo_s.PRODUCT_ORDER_SAMPLE_ID AND
     *        bl.BILLING_SESSION = bs.BILLING_SESSION_ID AND
     *        bs.BILLED_DATE < pdo.rev_date
     *
     */
    @Test(enabled = false)
    public void backfillLedgerQuotes() {
        int counter = 0;
        for (LedgerEntry ledger : ledgerEntryFixupDao.findSuccessfullyBilledLedgerEntriesWithoutQuoteId()) {
            String quoteId = ledger.getProductOrderSample().getProductOrder().getQuoteId();

            final int BATCH_SIZE = 1000;
            ledger.setQuoteId(quoteId);
            if (++counter % BATCH_SIZE == 0) {
                // Only create a transaction for every BATCH_SIZE ledger entries, otherwise this test runs
                // excruciatingly slowly.
                ledgerEntryFixupDao.persistAll(Collections.emptyList());
                logger.info(MessageFormat.format("Issued persist at record {0}", counter));
            }
        }
        // We need the transaction that #persistAll gives us to get the last set of modulus BATCH_SIZE ledger
        // entries.  It doesn't matter what we pass this method, it will create the transaction around the
        // extended persistence context that holds the entities we modified above.
        ledgerEntryFixupDao.persistAll(Collections.emptyList());
    }

    /**
     * Add all appropriate price item types to any ledger entry items that were billed to the quote server.
     */
    @Test(enabled = false)
    public void populatePriceItemTypes() {
        int counter = 1;
        final int BATCH_SIZE = 2000;
        for (LedgerEntry ledger : ledgerEntryFixupDao.findAllBilledEntries()) {
            if (ledger.getPriceItem().equals(
                    ledger.getProductOrderSample().getProductOrder().getProduct().getPrimaryPriceItem())) {
                // This is a primary price item.
                ledger.setPriceItemType(LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM);
            } else if (isEntryAReplacementItem(ledger)) {
                ledger.setPriceItemType(LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM);
            } else {
                ledger.setPriceItemType(LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM);
            }

            if (counter % BATCH_SIZE == 0) {
                // Only create a transaction for every BATCH_SIZE ledger entries.
                ledgerEntryFixupDao.persistAll(Collections.emptyList());
                logger.info(MessageFormat.format("Issued persist at record {0}", counter));
            }

            counter++;
        }

        // We need the transaction that #persistAll gives us to get the last set of modulus BATCH_SIZE ledger
        // entries.  It doesn't matter what we pass this method, it will create the transaction that persists
        // the entities modified above.
        ledgerEntryFixupDao.persistAll(Collections.emptyList());
    }

    private boolean isEntryAReplacementItem(LedgerEntry ledger) {

        Collection<QuotePriceItem> quotePriceItems =
            priceListCache.getReplacementPriceItems(ledger.getProductOrderSample().getProductOrder().getProduct());

        for (QuotePriceItem quotePriceItem : quotePriceItems) {
            if (quotePriceItem.getName().equals(ledger.getPriceItem().getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Largely copy/pasted from #backfillLedgerQuotes above, used to null out ledger quote IDs when we
     * discover we didn't assign them properly before and need to revise the assignments.
     */
    @Test(enabled = false)
    public void nullOutAllQuotes() {

        int counter = 0;

        List<LedgerEntry> ledgerEntries =
                ledgerEntryFixupDao.findAll(LedgerEntry.class, new GenericDao.GenericDaoCallback<LedgerEntry>() {
                    @Override
                    public void callback(CriteriaQuery<LedgerEntry> criteriaQuery, Root<LedgerEntry> root) {
                        // This runs much more slowly without the fetch as these would otherwise be singleton selected.
                        root.fetch(LedgerEntry_.productOrderSample);
                    }
                });

        for (LedgerEntry ledger : ledgerEntries) {

            final int BATCH_SIZE = 2000;
            ledger.setQuoteId(null);

            if (++counter % BATCH_SIZE == 0) {
                // Only create a transaction for every BATCH_SIZE ledger entries, otherwise this test runs
                // excruciatingly slowly.
                ledgerEntryFixupDao.persistAll(Collections.emptyList());
                logger.info(MessageFormat.format("Issued persist at record {0}", counter));
            }
        }

        // We need the transaction that #persistAll gives us to get the last set of modulus BATCH_SIZE ledger
        // entries.  It doesn't matter what we pass this method, it will create the transaction that persists
        // the entities modified above.
        ledgerEntryFixupDao.persistAll(Collections.emptyList());
    }

    /**
     * At one point the name of the session column in ledger was changed and that implicitly dropped the
     * foreign key constraint on session. Somehow there was an orphaned session entry, 1591. This needs to be
     * removed from the ledger so that we can place the constraint back. This required going in and changing the actual
     * LedgerEntry object to make the session a LONG instead of a BillingSession. To get that to run I also needed to
     * comment out a lot of DAO calls on the ledger
     */
    @Test(enabled = false)
    public void removeOrphanedSession() {
        List<Long> ledgerEntryIds = ledgerEntryFixupDao.getEntriesWithOrphanedSession(1591);
        for (Long entryId : ledgerEntryIds) {
            LedgerEntry entry = ledgerEntryFixupDao.findById(LedgerEntry.class, entryId);
            entry.setBillingSession(null);
        }

        // Persist all the clears
        ledgerEntryFixupDao.persistAll(Collections.emptyList());
    }

    @Test(enabled = true)
    public void gplim4143FixEntryBilledAsAddOnInsteadOfPrimary()
            throws IOException, ProductOrderEjb.NoSuchPDOException, SystemException, NotSupportedException,
            HeuristicRollbackException, HeuristicMixedException, RollbackException {
        userBean.loginOSUser();

        /*
         * Use a user transaction for this test because it calls an EJB method. Without the user transaction, the EJB's
         * transaction would save the entity modifications while the FixupCommentary would be persisted in a separate
         * transaction with a different rev ID.
         */
        utx.begin();

        String pdoKey = "PDO-8360";
        String sample = "SM-4AZSZ";

        List<ProductOrderSample> samples =
                productOrderSampleDao.findByOrderKeyAndSampleNames(pdoKey, Collections.singleton(sample));
        assertThat(samples, hasSize(1));
        assertThat(samples.get(0).getLedgerItems(), hasSize(1));
        LedgerEntry ledgerEntry = samples.get(0).getLedgerItems().iterator().next();

        assertThat(ledgerEntry.getPriceItemType(), equalTo(LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM));
        ledgerEntry.setPriceItemType(LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM);

        MessageReporter.LogReporter reporter = new MessageReporter.LogReporter(logger);
        productOrderEjb.updateOrderStatus(pdoKey, reporter);

        ledgerEntryFixupDao.persist(new FixupCommentary(
            String.format("GPLIM-4143 Correcting price item type for %s on %s", sample, pdoKey)));

        utx.commit();
    }
}
