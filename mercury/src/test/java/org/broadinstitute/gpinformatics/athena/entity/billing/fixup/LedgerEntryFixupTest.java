package org.broadinstitute.gpinformatics.athena.entity.billing.fixup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

public class LedgerEntryFixupTest extends Arquillian {

    private static final Log logger = LogFactory.getLog(LedgerEntryFixupTest.class);

    @Inject
    private LedgerEntryDao ledgerEntryDao;

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
        for (LedgerEntry ledger : ledgerEntryDao.findSuccessfullyBilledLedgerEntriesWithoutQuoteId()) {
            String quoteId = ledger.getProductOrderSample().getProductOrder().getQuoteId();

            final int BATCH_SIZE = 1000;
            ledger.setQuoteId(quoteId);
            if (++counter % BATCH_SIZE == 0) {
                // Only create a transaction for every BATCH_SIZE ledger entries, otherwise this test runs
                // excruciatingly slowly.
                ledgerEntryDao.persist(ledger);
                logger.info(MessageFormat.format("Issued persist at record {0}", counter));
            }
        }
        // We need the transaction that #persistAll gives us to get the last set of modulus BATCH_SIZE ledger
        // entries.  It doesn't matter what we pass this method, it will create the transaction around the
        // extended persistence context that holds the entities we modified above.
        ledgerEntryDao.persistAll(Collections.emptyList());
    }


    /**
     * Largely copy/pasted from #backfillLedgerQuotes above, used to null out ledger quote IDs when we
     * discover we didn't assign them properly before and need to revise the assignments.
     */
    @Test(enabled = false)
    public void nullOutAllQuotes() {

        int counter = 0;

        List<LedgerEntry> ledgerEntries =
                ledgerEntryDao.findAll(LedgerEntry.class, new GenericDao.GenericDaoCallback<LedgerEntry>() {
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
                ledgerEntryDao.persist(ledger);
                logger.info(MessageFormat.format("Issued persist at record {0}", counter));
            }
        }

        // We need the transaction that #persistAll gives us to get the last set of modulus BATCH_SIZE ledger
        // entries.  It doesn't matter what we pass this method, it will create the transaction that persists
        // the entities modified above.
        ledgerEntryDao.persistAll(Collections.emptyList());
    }
}
