package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.io.IOException;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST;

@Test(groups = TestGroups.ALTERNATIVES)
public class ProductOrderEjbJiraTransitionTest extends Arquillian {

    private static final String PDO = "PDO-4458";

    private static final ProductOrderEjb.JiraTransition TARGET_STATE = ProductOrderEjb.JiraTransition.OPEN;

    @Inject
    private JiraService jiraService;

    @Inject
    private ProductOrderEjb productOrderEjb;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(TEST);
    }

    @BeforeMethod
    public void setupJiraStatus() throws IOException {
        if (jiraService != null) {
            resetJiraTicketState();
        }
    }

    @Test
    public void testCancellation() throws Exception {
        productOrderEjb.transitionJiraTicket(PDO, ProductOrderEjb.JiraResolution.CANCELLED,ProductOrderEjb.JiraTransition.CANCEL, "testing");
        Assert.assertEquals(jiraService.getIssue(PDO).getResolution().toUpperCase(),ProductOrderEjb.JiraResolution.CANCELLED.toString().toUpperCase());
    }


    private void resetJiraTicketState() throws IOException {
        productOrderEjb.transitionJiraTicket(PDO, null,ProductOrderEjb.JiraTransition.DEVELOPER_EDIT, "testing");
        productOrderEjb.transitionJiraTicket(PDO, null,TARGET_STATE, "testing");
        Assert.assertEquals(getJiraTicketState(),TARGET_STATE.getStateName(),"Could not reset state of ticket " + PDO + " to " + TARGET_STATE + ".  Maybe the" +
                                                                             "workflow has changed?  Or the pdo ticket was updated out of band?");
    }

    private String getJiraTicketState() throws IOException{
        return (String)((Map)jiraService.getIssue(PDO).getField("Status")).get("name");
    }




}
