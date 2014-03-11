package org.broadinstitute.gpinformatics.athena.boundary.orders;


import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.Consent;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.broadinstitute.gpinformatics.mocks.HappyQuoteServiceMock;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;

public class ProductOrderEjbContainerTest extends Arquillian {

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

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testNullQuotePropagatesToJira() throws Exception {
        String pdoName = "PDO-310";
        ProductOrder pdo = pdoDao.findByBusinessKey(pdoName);
        pdo.setQuoteId("CASH");
        pdo.setSkipQuoteReason("Because of GPLIM-2462");

        pdoEjb.updateJiraIssue(pdo);
        String quoteFromJira = getQuoteFieldFromJiraTicket(pdo);

        Assert.assertEquals(quoteFromJira,pdo.getQuoteId());

        pdo.setQuoteId(null);
        pdoEjb.updateJiraIssue(pdo);
        quoteFromJira = getQuoteFieldFromJiraTicket(pdo);

        Assert.assertEquals(quoteFromJira, ProductOrder.QUOTE_TEXT_USED_IN_JIRA_WHEN_QUOTE_FIELD_IS_EMPTY);
    }

    /**
     * Gets the text of the quote field from
     * the jira ticket that corresponds to the given pdo.
     */
    private String getQuoteFieldFromJiraTicket(ProductOrder pdo) throws IOException {
        return (String)jiraService.getIssue(pdo.getBusinessKey()).getField(ProductOrder.JiraField.QUOTE_ID.getName());
    }
}
