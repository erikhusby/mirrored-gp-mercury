package org.broadinstitute.gpinformatics.athena.boundary.orders;


import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mocks.HappyQuoteServiceMock;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST;

@Test(groups = TestGroups.ALTERNATIVES)
public class ProductOrderEjbContainerTest extends Arquillian {

    private static final String TEST_PDO = "PDO-312";
    @Inject
    ProductOrderEjb pdoEjb;

    @Inject
    ProductOrderDao pdoDao;

    @Inject
    JiraService jiraService;

    @Deployment
    public static WebArchive buildMercuryWar() {
        // it's test because we need a real jira.  And the mock quote server is here because the stub explodes
        // in intellij with "Failed to read quotes from disk", presumably because the arquillian deployment
        // doesn't have the right working directory.
        return DeploymentBuilder.buildMercuryWarWithAlternatives(TEST, HappyQuoteServiceMock.class);
    }

    @Test(groups = TestGroups.ALTERNATIVES)
    public void testNullQuotePropagatesToJira() throws Exception {
        String pdoName = TEST_PDO;
        ProductOrder pdo = pdoDao.findByBusinessKey(pdoName);
        pdo.setQuoteId("CASH");
        pdo.setSkipQuoteReason("Because of GPLIM-2462");

        pdoEjb.updateJiraIssue(pdo);
        String quoteFromJira = getQuoteFieldFromJiraTicket(pdo);

        Assert.assertEquals(quoteFromJira, pdo.getQuoteId());

        pdo.setQuoteId(null);
        pdoEjb.updateJiraIssue(pdo);
        quoteFromJira = getQuoteFieldFromJiraTicket(pdo);

        Assert.assertEquals(quoteFromJira, ProductOrder.QUOTE_TEXT_USED_IN_JIRA_WHEN_QUOTE_FIELD_IS_EMPTY);
    }

    @Test(groups = TestGroups.ALTERNATIVES)
    public void testSummaryFieldPropagatesToJira() throws Exception {
        String pdoName = TEST_PDO;
        ProductOrder pdo = pdoDao.findByBusinessKey(pdoName);
        String newTitle = "And now for something different " + System.currentTimeMillis();
        pdo.setTitle(newTitle);

        pdoEjb.updateJiraIssue(pdo);
        String titleFromJira = getSummaryFieldFromJiraTicket(pdo);

        Assert.assertEquals(titleFromJira, newTitle, "jira summary field is not synchronized with pdo title.");
    }


    /**
     * Gets the text of the quote field from
     * the jira ticket that corresponds to the given pdo.
     */
    private String getQuoteFieldFromJiraTicket(ProductOrder pdo) throws IOException {
        return (String) jiraService.getIssue(pdo.getBusinessKey()).getField(ProductOrder.JiraField.QUOTE_ID.getName());
    }

    private String getSummaryFieldFromJiraTicket(ProductOrder pdo) throws IOException {
        return (String) jiraService.getIssue(pdo.getBusinessKey()).getField(ProductOrder.JiraField.SUMMARY.getName());
    }
}
