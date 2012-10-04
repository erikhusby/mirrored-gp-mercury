package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/1/12
 * Time: 4:37 PM
 */
@Test(groups = {TestGroups.DATABASE_FREE})
public class OrderTest {

    private static final String PDO_JIRA_KEY = "PDO-1";

    @BeforeMethod
    public void setUp() throws Exception {
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void testOrder() throws Exception {

        BillableItem billableItem1 = new BillableItem( "NewProductName1", BillingStatus.NotYetBilled, 1);
        BillableItem billableItem2 = new BillableItem( "NewProductName2", BillingStatus.NotYetBilled, 1);
        HashSet<BillableItem> billableItems = new HashSet<BillableItem>();
        billableItems.add(billableItem1);
        billableItems.add(billableItem2);

        List<AthenaSample> orderSamples = SampleSheetTest.createSampleList(
                "SM-2ACGC,SM-2ABDD,SM-2ACKV,SM-2AB1B,SM-2ACJC,SM-2AD5D",
                billableItems ) ;

        SampleSheet sampleSheet = new SampleSheet();
        sampleSheet.setSamples(orderSamples);
        Order order = new Order("title", "researchProjectName", "quoteId", sampleSheet);

        //TODO hmc Under construction
        Assert.assertEquals(order.getSampleSheet().getSamples().size(), 6);
        Assert.assertTrue(order.getSampleSheet().getSamples().get(0).getBillableItems().size() == 2);

        Assert.assertNull(order.getJiraTicketKey());

        Assert.assertEquals(order.fetchJiraIssueType(), CreateIssueRequest.Fields.Issuetype.Product_Order);

        Assert.assertEquals(order.fetchJiraProject(), CreateIssueRequest.Fields.ProjectType.Product_Ordering);

        try {
            order.setJiraTicketKey(null);
            Assert.fail();
        } catch(NullPointerException npe) {
            /*
            Ensuring Null is thrown for setting null
             */
        } finally {
            order.setJiraTicketKey(PDO_JIRA_KEY);
        }

        Assert.assertNotNull(order.getJiraTicketKey());

        Assert.assertEquals(order.getJiraTicketKey(),PDO_JIRA_KEY);

    }


}
