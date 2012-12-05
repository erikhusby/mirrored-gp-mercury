package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.meanbean.test.BeanTester;
import org.meanbean.test.Configuration;
import org.meanbean.test.ConfigurationBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/1/12
 * Time: 4:37 PM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderTest {

    private static final Long TEST_CREATOR = 1111L;

    private static final String PDO_JIRA_KEY = "PDO-1";
    private ProductOrder productOrder;

    @BeforeMethod
    public void setUp() throws Exception {
        productOrder = AthenaClientServiceStub.createDummyProductOrder ();
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void test_basic_beaniness() {

        //TODO hmc need to ignore the samples list as meanbean is by default using a list of Strings
        // to test the samples setter. There may be a api solution for this.
        BeanTester tester = new BeanTester();
        Configuration configuration = new ConfigurationBuilder().ignoreProperty("samples").build();
        tester.testBean(ProductOrder.class, configuration);

    }

    @Test
    public void testOrder() throws Exception {

        //TODO hmc To be completed commented out now for change of priority.
        /**
        List<OrderSample> orderSamples = orderTest.createSampleList(
                "SM-2ACGC,SM-2ABDD,SM-2ACKV,SM-2AB1B,SM-2ACJC,SM-2AD5D", billableItems ) ;

        ProductOrder productOrder = new ProductOrder("title", orderSamples, "quoteId", product, "researchProjectName" );

        //TODO hmc Under construction
        Assert.assertEquals(productOrder.getSamples().size(), 6);
        Assert.assertTrue(productOrder.getSamples().get(0).getLedgerItems().size() == 1);

         **/

        Assert.assertNull(productOrder.getJiraTicketKey());

        Assert.assertEquals(productOrder.fetchJiraIssueType(), CreateFields.IssueType.PRODUCT_ORDER);

        Assert.assertEquals(productOrder.fetchJiraProject(), CreateFields.ProjectType.Product_Ordering);

        productOrder.setJiraTicketKey(PDO_JIRA_KEY);

        Assert.assertNotNull(productOrder.getJiraTicketKey());

        Assert.assertEquals(productOrder.getJiraTicketKey(), PDO_JIRA_KEY);
    }

    private final List<ProductOrderSample> sixBspSamplesNoDupes =
            createDBFreeSampleList("SM-2ACGC","SM-2ABDD","SM-2ACKV","SM-2AB1B","SM-2ACJC","SM-2AD5D");

    private final List<ProductOrderSample> fourBspSamplesWithDupes =
            createDBFreeSampleList("SM-2ACGC","SM-2ABDD","SM-2ACGC","SM-2AB1B","SM-2ACJC","SM-2ACGC");

    private final List<ProductOrderSample> sixMixedSampleProducts =
            createDBFreeSampleList("SM-2ACGC","SM2ABDD","SM2ACKV","SM-2AB1B","SM-2ACJC","SM-2AD5D");

    private final List<ProductOrderSample> nonBspSampleProducts =
            createDBFreeSampleList("SSM-2ACGC1","SM--2ABDDD","SM-2AB","SM-2AB1B","SM-2ACJCACB","SM-SM-SM");

    @Test
    public void testGetUniqueSampleCount() throws Exception {
        productOrder = new ProductOrder(TEST_CREATOR, "title", sixBspSamplesNoDupes, "quote", null, null);
        Assert.assertEquals(productOrder.getUniqueSampleCount(), 6);

        productOrder = new ProductOrder(TEST_CREATOR, "title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertEquals(productOrder.getUniqueSampleCount(), 4);
    }

    @Test
    public void testGetTotalSampleCount() throws Exception {
        productOrder = new ProductOrder(TEST_CREATOR, "title", sixBspSamplesNoDupes, "quote", null, null);
        Assert.assertEquals(productOrder.getTotalSampleCount (), 6);

        productOrder = new ProductOrder(TEST_CREATOR, "title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertEquals(productOrder.getTotalSampleCount (), 6);
    }

    @Test
    public void testGetDuplicateCount() throws Exception {
        productOrder = new ProductOrder(TEST_CREATOR, "title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertEquals(productOrder.getDuplicateCount (), 2);
    }

    @Test
    public void testAreAllSampleBSPFormat() throws Exception {
        productOrder = new ProductOrder(TEST_CREATOR, "title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertTrue(productOrder.areAllSampleBSPFormat());

        productOrder = new ProductOrder(TEST_CREATOR, "title", sixBspSamplesNoDupes, "quote", null, null);
        Assert.assertTrue(productOrder.areAllSampleBSPFormat());

        productOrder = new ProductOrder(TEST_CREATOR, "title", nonBspSampleProducts, "quote", null, null);
        Assert.assertFalse(productOrder.areAllSampleBSPFormat());

        productOrder = new ProductOrder(TEST_CREATOR, "title", sixMixedSampleProducts, "quote", null, null);
        Assert.assertFalse ( productOrder.areAllSampleBSPFormat () );
    }

    public static List<ProductOrderSample> createSampleList(String... sampleList) {
        return createSampleList(sampleList, new HashSet<BillableItem>(), false);
    }

    public static List<ProductOrderSample> createDBFreeSampleList(String... sampleList) {
        return createSampleList(sampleList, new HashSet<BillableItem>(), true);
    }

    public static List<ProductOrderSample> createSampleList(String[] sampleArray,
                                                            Set<BillableItem> billableItems,
                                                            boolean dbFree) {
        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>(sampleArray.length);
        for (String sampleName : sampleArray) {
            ProductOrderSample productOrderSample;
            if (dbFree) {
                productOrderSample = new ProductOrderSample(sampleName, BSPSampleDTO.DUMMY);
            } else {
                productOrderSample = new ProductOrderSample(sampleName);
            }

            productOrderSample.setSampleComment("athenaComment");

            productOrderSamples.add(productOrderSample);
        }
        return productOrderSamples;
    }
}
