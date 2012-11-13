package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectTest;
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

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/1/12
 * Time: 4:37 PM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderTest {

    private static final Long TEST_CREATOR = 10L;

    private static final String PDO_JIRA_KEY = "PDO-1";
    private ProductOrder productOrder;

    @BeforeMethod
    public void setUp() throws Exception {
        productOrder = createDummyProductOrder();
    }

    public static ProductOrder createDummyProductOrder() {
        PriceItem priceItem = new PriceItem(PriceItem.Platform.GP, PriceItem.Category.EXOME_SEQUENCING_ANALYSIS,
                                    PriceItem.Name.EXOME_EXPRESS, "testQuoteId");
        Product dummyProduct = createDummyProduct();
        dummyProduct.addPriceItem(priceItem);
        ProductOrder order = new ProductOrder( TEST_CREATOR, "title",
                new ArrayList<ProductOrderSample>(), "quote", dummyProduct,
                ResearchProjectTest.createDummyResearchProject());

        ProductOrderSample sample = new ProductOrderSample("SM-1234", order);
        sample.addBillableItem(new BillableItem(priceItem, new BigDecimal("1")));
        order.setSamples(Collections.singletonList(sample));

        order.updateAddOnProducts(Collections.singletonList(createDummyProduct()));
        return order;
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
        Assert.assertTrue(productOrder.getSamples().get(0).getBillableItems().size() == 1);

         **/

        Assert.assertNull(productOrder.getJiraTicketKey());

        Assert.assertEquals(productOrder.fetchJiraIssueType(), CreateFields.IssueType.Product_Order);

        Assert.assertEquals(productOrder.fetchJiraProject(), CreateFields.ProjectType.Product_Ordering);

        productOrder.setJiraTicketKey(PDO_JIRA_KEY);

        Assert.assertNotNull(productOrder.getJiraTicketKey());

        Assert.assertEquals(productOrder.getJiraTicketKey(), PDO_JIRA_KEY);
    }

    public static Product createDummyProduct() {
        return new Product("productName", new ProductFamily("ProductFamily"), "description",
            "partNumber", new Date(), new Date(), 12345678, 123456, 100, 96, "inputRequirements", "deliverables",
            true, "workflowName", false);
    }

    private final List<ProductOrderSample> sixBspSamplesNoDupes =
            createDBFreeSampleList("SM-2ACGC,SM-2ABDD,SM-2ACKV,SM-2AB1B,SM-2ACJC,SM-2AD5D", productOrder);

    private final List<ProductOrderSample> fourBspSamplesWithDupes =
            createDBFreeSampleList("SM-2ACGC,SM-2ABDD,SM-2ACGC,SM-2AB1B,SM-2ACJC,SM-2ACGC", productOrder);

    private final List<ProductOrderSample> sixMixedSampleProducts =
            createDBFreeSampleList("SM-2ACGC,SM2ABDD,SM2ACKV,SM-2AB1B,SM-2ACJC,SM-2AD5D", productOrder);

    private final List<ProductOrderSample> nonBspSampleProducts =
            createDBFreeSampleList("SSM-2ACGC1,SM--2ABDDD,SM-2AB,SM-2AB1B,SM-2ACJCACB,SM-SM-SM", productOrder);

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
        Assert.assertEquals(productOrder.getTotalSampleCount(), 6);

        productOrder = new ProductOrder(TEST_CREATOR, "title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertEquals(productOrder.getTotalSampleCount(), 6);
    }

    @Test
    public void testGetDuplicateCount() throws Exception {
        productOrder = new ProductOrder(TEST_CREATOR, "title", fourBspSamplesWithDupes, "quote", null, null);
        Assert.assertEquals(productOrder.getDuplicateCount(), 2);
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
        Assert.assertFalse(productOrder.areAllSampleBSPFormat());
    }

    public static List<ProductOrderSample> createSampleList(String sampleListStr, ProductOrder productOrder) {
        return createSampleList(sampleListStr, new HashSet<BillableItem>(), productOrder, false);
    }

    public static List<ProductOrderSample> createDBFreeSampleList(String sampleListStr, ProductOrder productOrder) {
        return createSampleList(sampleListStr, new HashSet<BillableItem>(), productOrder, true);
    }

    public static List<ProductOrderSample> createSampleList(String sampleListStr,
                                                            Set<BillableItem> billableItems,
                                                            ProductOrder productOrder,
                                                            boolean dbFree) {
        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>();
        String[] sampleArray = sampleListStr.split(",");
        for (String sampleName : sampleArray) {
            ProductOrderSample productOrderSample;
            if (dbFree) {
                productOrderSample = new ProductOrderSample(sampleName, productOrder, BSPSampleDTO.DUMMY);
            } else {
                productOrderSample = new ProductOrderSample(sampleName, productOrder);
            }
            productOrderSample.setSampleComment("athenaComment");
            for (BillableItem billableItem : billableItems) {
                productOrderSample.addBillableItem(billableItem);
            }
            productOrderSamples.add(productOrderSample);
        }
        return productOrderSamples;
    }
}
