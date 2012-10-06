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
public class ProductOrderTest {

    private static final String PDO_JIRA_KEY = "PDO-1";
    private ProductOrder productOrder;

    @BeforeMethod
    public void setUp() throws Exception {
        List<ProductOrderSample> sampleProducts = new ArrayList<ProductOrderSample>();
        productOrder = new ProductOrder("title", sampleProducts, "quote", null, null);
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

        ProductOrder productOrder = new ProductOrder("title", orderSamples, "quoteId", product, "researchProjectName" );

        //TODO hmc Under construction
        Assert.assertEquals(productOrder.getSamples().size(), 6);
        Assert.assertTrue(productOrder.getSamples().get(0).getBillableItems().size() == 1);

         **/


        Assert.assertNull(productOrder.getJiraTicketKey());

        Assert.assertEquals(productOrder.fetchJiraIssueType(), CreateIssueRequest.Fields.Issuetype.Product_Order);

        Assert.assertEquals(productOrder.fetchJiraProject(), CreateIssueRequest.Fields.ProjectType.Product_Ordering);

        try {
            productOrder.setJiraTicketKey(null);
            Assert.fail();
        } catch(NullPointerException npe) {
            /*
            Ensuring Null is thrown for setting null
             */
        } finally {
            productOrder.setJiraTicketKey(PDO_JIRA_KEY);
        }

        Assert.assertNotNull(productOrder.getJiraTicketKey());

        Assert.assertEquals(productOrder.getJiraTicketKey(),PDO_JIRA_KEY);
    }


    private List<ProductOrderSample> sixBspSamplesNoDupes = createSampleList("SM-2ACGC,SM-2ABDD,SM-2ACKV,SM-2AB1B,SM-2ACJC,SM-2AD5D",
                    new HashSet<BillableItem>() ) ;

    private List<ProductOrderSample> fourBspSamplesWithDupes = createSampleList("SM-2ACGC,SM-2ABDD,SM-2ACGC,SM-2AB1B,SM-2ACJC,SM-2ACGC",
                    new HashSet<BillableItem>() ) ;

    private List<ProductOrderSample> sixMixedSampleProducts = createSampleList("SM-2ACGC,SM2ABDD,SM2ACKV,SM-2AB1B,SM-2ACJC,SM-2AD5D",
                    new HashSet<BillableItem>() ) ;

    private List<ProductOrderSample> nonBspSampleProducts = createSampleList("SSM-2ACGC1,SM--2ABDDD,SM-2AB,SM-2AB1B,SM-2ACJCACB,SM-SM-SM",
                    new HashSet<BillableItem>() ) ;


    @Test
    public void testGetUniqueSampleCount() throws Exception {

        productOrder = new ProductOrder("title", sixBspSamplesNoDupes, "quote", null, null);
        Assert.assertEquals(productOrder.getUniqueSampleCount(), 6);

        productOrder = new ProductOrder("title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertEquals(productOrder.getUniqueSampleCount(), 4);

    }

    @Test
    public void testGetTotalSampleCount() throws Exception {

        productOrder = new ProductOrder("title", sixBspSamplesNoDupes, "quote", null, null);
        Assert.assertEquals(productOrder.getTotalSampleCount(), 6);

        productOrder = new ProductOrder("title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertEquals(productOrder.getTotalSampleCount(), 6);
    }

    @Test
    public void testGetDuplicateCount() throws Exception {

        productOrder = new ProductOrder("title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertEquals(productOrder.getDuplicateCount(), 2);
    }


    @Test
    public void testAreAllSampleBSPFormat() throws Exception {

        productOrder = new ProductOrder("title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertTrue(productOrder.areAllSampleBSPFormat());

        productOrder = new ProductOrder("title", sixBspSamplesNoDupes, "quote", null, null);
        Assert.assertTrue(productOrder.areAllSampleBSPFormat());

        productOrder = new ProductOrder("title", nonBspSampleProducts, "quote", null, null);
        Assert.assertFalse(productOrder.areAllSampleBSPFormat());

        productOrder = new ProductOrder("title", sixMixedSampleProducts, "quote", null, null);
        Assert.assertFalse(productOrder.areAllSampleBSPFormat());

    }


    public static List<ProductOrderSample>  createSampleList( String sampleListStr, HashSet<BillableItem> billableItems) {
        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>();
        String [] sampleArray = sampleListStr.split(",");
        for ( String sampleName : sampleArray) {
            ProductOrderSample productOrderSample = new ProductOrderSample(sampleName);
            productOrderSample.setComment("athenaComment");
            for ( BillableItem billableItem : billableItems ) {
                productOrderSample.addBillableItem(billableItem);
            }
            productOrderSamples.add(productOrderSample);
        }
        return productOrderSamples;
    }


}
