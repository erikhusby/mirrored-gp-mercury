package org.broadinstitute.gpinformatics.athena.control.dao;


import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ResearchProjectEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductOrderJiraUtil;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST;

@Test(groups = TestGroups.STANDARD)
public class ProductOrderJiraUtilTest extends Arquillian {

    @Inject
    private BSPUserList userList;

    @Inject
    ResearchProjectEjb researchProjectEjb;

    @Inject
    JiraService jiraService;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(TEST);
    }

    @Test
    public void testCreatePDOTicketInJira() throws Exception {
        ProductOrder pdo = ProductOrderContainerTest.createSimpleProductOrder(researchProjectEjb,userList);

        Assert.assertTrue(StringUtils.isEmpty(pdo.getJiraTicketKey()),
                "PDO already has a jira id, but we want it blank for our test.");

        ProductOrderJiraUtil.createIssueForOrder(pdo, jiraService);

        Assert.assertFalse(StringUtils.isEmpty(pdo.getJiraTicketKey()),
                "When the PDO is created in jira, we expected the PDO's business key to be assigned.");
        Assert.assertEquals(pdo.getBusinessKey(), pdo.getJiraTicketKey(),
                "When the PDO is created in jira, we expected the PDO's business key to be assigned.");

        JiraIssue jiraIssue = jiraService.getIssue(pdo.getBusinessKey());

        Assert.assertNotNull(jiraIssue, "Looks like jira ticket creation for a PDO is broken");
        Assert.assertEquals(jiraIssue.getField(ProductOrder.JiraField.PRODUCT.getName()), pdo.getProduct().getName(),
                "Product field is not displayed correctly in jira");
        Assert.assertEquals(jiraIssue.getField(ProductOrder.JiraField.DESCRIPTION.getName()), pdo.getComments(),
                "Details field is not displayed correctly in jira");
        Assert.assertEquals(jiraIssue.getField(ProductOrder.JiraField.SAMPLE_IDS.getName()),pdo.getSampleString(),"Samples are not properly displayed in jira");
        Assert.assertEquals(jiraIssue.getField(ProductOrder.JiraField.PRODUCT_FAMILY.getName()),pdo.getProduct().getProductFamily().getName(),"Product family is not properly displayed in jira");
    }
}
