package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD;

/**
 * This "test" is an example of how to fixup some data.  Each fix method includes the JIRA ticket ID.
 * Set @Test(enabled=false) after running once.
 */
public class ProductOrderFixupTest extends Arquillian {

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    JiraService jiraService;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(PROD, "prod");
    }

    /**
     * Fixed up data per JIRA ticket.
     */
    @Test(enabled = false)
    public void fixupGplim123() {
        ProductOrder productOrder = productOrderDao.findById(1001L);
        productOrder.setComments(productOrder.getComments() + " fixup");
        // The entity is already persistent, this call to persist is solely to begin and end a transaction, so the
        // change gets flushed.  This is an artifact of the test environment.
        productOrderDao.persist(productOrder);
    }

    /**
     * Change quote for a PDO, see http://prodinfojira.broadinstitute.org:8080/jira/browse/GPLIM-365
     * @throws Exception
     */
    @Test(enabled = false)
    public void change_quote_for_pdo() throws Exception {
        String[] pdos = new String[] {
                "PDO-8",
                "PDO-23",
                "PDO-35" ,
                "PDO-42"  ,
                "PDO-57"   ,
                "PDO-60"    ,
                "PDO-74"
        };

        String newQuote = "GP85N";

        for (String jiraKey : pdos) {
            ProductOrder productOrder = productOrderDao.findByBusinessKey(jiraKey);
            productOrder.setQuoteId(newQuote);
            // The entity is already persistent, this call to persist is solely to begin and end a transaction, so the
            // change gets flushed.  This is an artifact of the test environment.
            productOrderDao.persist(productOrder);
        }

    }
}
