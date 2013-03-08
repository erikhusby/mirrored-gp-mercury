package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

public class BillingLedgerFixupTest extends Arquillian {

    @Inject
    private BillingLedgerDao billingLedgerDao;

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
    @Test(enabled = true)
    public void testBackfillLedgerQuotes() {

        for (BillingLedger ledger : billingLedgerDao.findWithoutQuoteId()) {
            String quoteId = ledger.getProductOrderSample().getProductOrder().getQuoteId();

            ledger.setQuoteId(quoteId);
            billingLedgerDao.persist(ledger);
        }
    }
}
