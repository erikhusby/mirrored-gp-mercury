package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.Query;

import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

public class BillingLedgerFixupTest extends Arquillian {

    @Inject
    private BillingLedgerDao billingLedgerDao;

    // When you run this on prod, change to PROD and prod
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }


    @Test(enabled = true)
    public void testBackfillLedgerQuotes() {

        Query query = billingLedgerDao.getEntityManager().createNativeQuery(
                // This outermost select * is needed to grab the rnk alias for picking out only the rows
                // with the oldest dates for a jira_ticket / quote_id combination.
                " SELECT" +
                "   *" +
                " FROM (" +
                // Pick out the fields of interest and a ranking of a given row within a jira_ticket_key / quote_id
                // combination, ordering by rev_date ascending.
                "   SELECT" +
                "     jira_ticket_key," +
                "     quote_id," +
                "     rev_date," +
                "     dense_rank()" +
                "     OVER (PARTITION BY jira_ticket_key, quote_id" +
                "       ORDER BY rev_date ASC) rnk" +
                "   FROM" +
                // It seems that for some period in Mercury's history the modified_date in product_order was not
                // recording the actual date of modification.  Use the rev_date on the Envers rev_info table instead.
                "     (SELECT" +
                "        jira_ticket_key," +
                "        quote_id," +
                "        ri.REV_DATE" +
                "      FROM athena.product_order_aud pdo2, mercury.rev_info ri" +
                "      WHERE pdo2.rev = ri.rev_info_id" +
                // Filter null JIRA ticket keys and messaging test debris.
                "            AND pdo2.jira_ticket_key IS NOT null AND pdo2.jira_ticket_key NOT LIKE '%MsgTest%')" +
                " )" +
                // Only take the top-ranked (oldest) rows for a given jira_ticket_key / quote_id combination.
                " WHERE rnk = 1" +
                // The ordering here is just for visual inspection of the results and doesn't numerically sort JIRA
                // ticket keys anyway.
                " ORDER BY jira_ticket_key, quote_id");


        @SuppressWarnings("unchecked")
        List<Object[]> resultList = query.getResultList();
        Assert.assertNotNull(resultList);
    }
}
