package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
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
    private  Order order ;

    @BeforeMethod
    public void setUp() throws Exception {
        List<OrderSample> samples = new ArrayList<OrderSample>();
        order = new Order("title", samples, "quote", null, null);
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void testOrder() throws Exception {

        Product product = new Product( "productName", new ProductFamily("ProductFamily"), "description",
                "partNumber", new Date(), new Date(), 12345678, 123456, 100, "inputRequirements", "deliverables",
                true, "workflowName");

        PriceItem priceItem1 = new PriceItem(PriceItem.Platform.GP, PriceItem.Category.EXOME_SEQUENCING_ANALYSIS,
                            PriceItem.Name.EXOME_EXPRESS, "testQuoteId");

        HashSet<BillableItem> billableItems = new HashSet<BillableItem>();
        BillableItem billableItem1 = new BillableItem( priceItem1, new BigDecimal("1") );
        billableItems.add(billableItem1);

        //TODO hmc To be completed commented out now for change of priority.
        /**
        List<OrderSample> orderSamples = orderTest.createSampleList(
                "SM-2ACGC,SM-2ABDD,SM-2ACKV,SM-2AB1B,SM-2ACJC,SM-2AD5D", billableItems ) ;

        Order order = new Order("title", orderSamples, "quoteId", product, "researchProjectName" );

        //TODO hmc Under construction
        Assert.assertEquals(order.getSamples().size(), 6);
        Assert.assertTrue(order.getSamples().get(0).getBillableItems().size() == 1);

         **/


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


    private List<OrderSample> sixBspSamplesNoDupes = createSampleList("SM-2ACGC,SM-2ABDD,SM-2ACKV,SM-2AB1B,SM-2ACJC,SM-2AD5D",
                    new HashSet<BillableItem>() ) ;

    private List<OrderSample> fourBspSamplesWithDupes = createSampleList("SM-2ACGC,SM-2ABDD,SM-2ACGC,SM-2AB1B,SM-2ACJC,SM-2ACGC",
                    new HashSet<BillableItem>() ) ;

    private List<OrderSample> sixMixedSamples = createSampleList("SM-2ACGC,SM2ABDD,SM2ACKV,SM-2AB1B,SM-2ACJC,SM-2AD5D",
                    new HashSet<BillableItem>() ) ;

    private List<OrderSample> nonBspSamples = createSampleList("SSM-2ACGC1,SM--2ABDDD,SM-2AB,SM-2AB1B,SM-2ACJCACB,SM-SM-SM",
                    new HashSet<BillableItem>() ) ;


    @Test
    public void testGetUniqueSampleCount() throws Exception {

        order = new Order("title", sixBspSamplesNoDupes, "quote", null, null);
        Assert.assertEquals(order.getUniqueSampleCount(), 6);

        order = new Order("title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertEquals(order.getUniqueSampleCount(), 4);

    }

    @Test
    public void testGetTotalSampleCount() throws Exception {

        order = new Order("title", sixBspSamplesNoDupes, "quote", null, null);
        Assert.assertEquals(order.getTotalSampleCount(), 6);

        order = new Order("title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertEquals(order.getTotalSampleCount(), 6);
    }

    @Test
    public void testGetDuplicateCount() throws Exception {

        order = new Order("title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertEquals(order.getDuplicateCount(), 2);
    }


    @Test
    public void testAreAllSampleBSPFormat() throws Exception {

        order = new Order("title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertTrue(order.areAllSampleBSPFormat());

        order = new Order("title", sixBspSamplesNoDupes, "quote", null, null);
        Assert.assertTrue(order.areAllSampleBSPFormat());

        order = new Order("title", nonBspSamples, "quote", null, null);
        Assert.assertFalse(order.areAllSampleBSPFormat());

        order = new Order("title", sixMixedSamples, "quote", null, null);
        Assert.assertFalse(order.areAllSampleBSPFormat());

    }


    public static List<OrderSample>  createSampleList( String sampleListStr, HashSet<BillableItem> billableItems) {
        List<OrderSample> orderSamples = new ArrayList<OrderSample>();
        String [] sampleArray = sampleListStr.split(",");
        for ( String sampleName : sampleArray) {
            OrderSample orderSample = new OrderSample(sampleName);
            orderSample.setComment("athenaComment");
            for ( BillableItem billableItem : billableItems ) {
                orderSample.addBillableItem(billableItem);
            }
            orderSamples.add(orderSample);
        }
        return orderSamples;
    }


}
