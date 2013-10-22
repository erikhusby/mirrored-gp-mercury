package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderSampleTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.meanbean.lang.EquivalentFactory;
import org.meanbean.test.BeanTester;
import org.meanbean.test.Configuration;
import org.meanbean.test.ConfigurationBuilder;
import org.meanbean.test.EqualsMethodTester;
import org.meanbean.test.HashCodeMethodTester;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder.OrderStatus;
import static org.broadinstitute.gpinformatics.infrastructure.matchers.InBspFormatSample.inBspFormat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;


@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderTest {

    private static final Long TEST_CREATOR = 1111L;
    private static final String PDO_JIRA_KEY = "PDO-1";
    private final List<ProductOrderSample> sixBspSamplesNoDupes =
            ProductOrderSampleTestFactory
                    .createDBFreeSampleList("SM-2ACGC", "SM-2ABDD", "SM-2ACKV", "SM-2AB1B", "SM-2ACJC", "SM-2AD5D");
    private final List<ProductOrderSample> fourBspSamplesWithDupes =
            ProductOrderSampleTestFactory
                    .createDBFreeSampleList("SM-2ACGC", "SM-2ABDD", "SM-2ACGC", "SM-2AB1B", "SM-2ACJC", "SM-2ACGC");
    private final List<ProductOrderSample> sixMixedSampleProducts =
            ProductOrderSampleTestFactory
                    .createDBFreeSampleList("SM-2ACGC", "SM2ABDD", "SM2ACKV", "SM-2AB1B", "SM-2ACJC", "SM-2AD5D");
    private final List<ProductOrderSample> nonBspSampleProducts =
            ProductOrderSampleTestFactory
                    .createDBFreeSampleList("SSM-2ACGC1", "SM--2ABDDD", "SM-2AB", "SM-2AB1B-", "SM-2ACJCACB",
                            "SM-SM-SM");
    private ProductOrder productOrder;

    public static ProductOrder createOrderWithSamples(List<ProductOrderSample> samples, OrderStatus status) {
        ProductOrder order = new ProductOrder();
        order.addSamples(samples);
        order.setOrderStatus(status);
        return order;
    }

    @BeforeMethod
    public void setUp() throws Exception {
        productOrder = ProductOrderTestFactory.createDummyProductOrder(PDO_JIRA_KEY);
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void testBeaniness() {

        BeanTester tester = new BeanTester();
        // Currently ProductOrder is equivalent based only on RP and title.
        Configuration configuration = new ConfigurationBuilder()
                .ignoreProperty("samples")
                .ignoreProperty("title")
                        // TODO: jiraTicketKey is part of businessKey which is what equals() uses. should it really be ignored?
                .ignoreProperty("jiraTicketKey")
                .ignoreProperty("orderStatus")
                .ignoreProperty("laneCount")
                .ignoreProperty("modifiedBy")
                .ignoreProperty("createdBy")
                .ignoreProperty("createdDate")
                .ignoreProperty("modifiedDate")
                .ignoreProperty("placedDate")
                .ignoreProperty("quoteId")
                .ignoreProperty("product")
                .ignoreProperty("researchProject")
                .ignoreProperty("comments")
                .ignoreProperty("sampleList")
                .ignoreProperty("fundingDeadline")
                .ignoreProperty("requisitionKey")
                .ignoreProperty("requisitionName")
                .ignoreProperty("publicationDeadline").build();
        tester.testBean(ProductOrder.class, configuration);

        class ProductOrderFactory implements EquivalentFactory<ProductOrder> {
            public final long ID = new Random().nextInt(Integer.MAX_VALUE);
            public String title = "title";

            @Override
            public ProductOrder create() {
                Product product = new Product("Exome Express", null, "Exome Express", "P-EX-0002", new Date(), null,
                        1814400, 1814400, 184, null, null, null, true, Workflow.AGILENT_EXOME_EXPRESS, false, "agg type");
                ResearchProject researchProject =
                        new ResearchProject(ID, title, "RP title", ResearchProject.IRB_NOT_ENGAGED);

                return new ProductOrder(ID, "PO title", sixBspSamplesNoDupes, "quoteId", product, researchProject);
            }
        }

        // Currently ProductOrder is equivalent based only on business key (productOrderId, jiraTicketKey)
        new EqualsMethodTester().testEqualsMethod(new ProductOrderFactory(), configuration);

        new HashCodeMethodTester().testHashCodeMethod(new ProductOrderFactory());

    }

    @Test
    public void testOrder() throws Exception {
        assertThat(productOrder.getJiraTicketKey(), is(equalTo(PDO_JIRA_KEY)));
    }

    @Test
    public void testGetUniqueSampleCount() throws Exception {
        productOrder = new ProductOrder(TEST_CREATOR, "title", sixBspSamplesNoDupes, "quote", null, null);
        assertThat(productOrder.getUniqueSampleCount(), is(equalTo(6)));

        productOrder = new ProductOrder(TEST_CREATOR, "title", fourBspSamplesWithDupes, "quote", null, null);
        assertThat(productOrder.getUniqueSampleCount(), is(equalTo(4)));
    }

    @Test
    public void testGetTotalSampleCount() throws Exception {
        productOrder = new ProductOrder(TEST_CREATOR, "title", sixBspSamplesNoDupes, "quote", null, null);
        assertThat(productOrder.getTotalSampleCount(), is(equalTo(6)));

        productOrder = new ProductOrder(TEST_CREATOR, "title", fourBspSamplesWithDupes, "quote", null, null);
        assertThat(productOrder.getTotalSampleCount(), is(equalTo(6)));
    }

    @Test
    public void testGetDuplicateCount() throws Exception {
        productOrder = new ProductOrder(TEST_CREATOR, "title", fourBspSamplesWithDupes, "quote", null, null);
        assertThat(productOrder.getDuplicateCount(), is(equalTo(2)));
    }

    @Test
    public void testAreAllSampleBSPFormat() throws Exception {
        assertThat(fourBspSamplesWithDupes, everyItem(is(inBspFormat())));
        assertThat(sixBspSamplesNoDupes, everyItem(is(inBspFormat())));
        assertThat(nonBspSampleProducts, everyItem(is(not(inBspFormat()))));
        assertThat(sixMixedSampleProducts, not(everyItem(is(inBspFormat()))));
    }

    @DataProvider(name = "testUpdateOrderStatus")
    public Object[][] createUpdateOrderStatusData() {
        List<ProductOrderSample> billedSamples = Arrays.asList(ProductOrderSampleTest.createBilledSample("ABC"),
                ProductOrderSampleTest.createBilledSample("DEF"));
        List<ProductOrderSample> abandonedSamples = ProductOrderSampleTestFactory.createDBFreeSampleList("123", "456");
        for (ProductOrderSample sample : abandonedSamples) {
            sample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.ABANDONED);
        }
        List<ProductOrderSample> notBilledSamples = ProductOrderSampleTestFactory.createDBFreeSampleList("ZZZ", "YYY");
        List<ProductOrderSample> atLeastOneNotBilled = Arrays.asList(ProductOrderSampleTest.createBilledSample("ABC"),
                new ProductOrderSample("ZZZ"));
        List<ProductOrderSample> billedToAddOnSamples = Arrays.asList(ProductOrderSampleTest.createBilledSample("ABC"),
                ProductOrderSampleTest.createBilledSample("ZZZ", LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM));
        return new Object[][]{
                // Can't transition from Draft or Abandoned, regardless of the sample state.
                {createOrderWithSamples(billedSamples, OrderStatus.Draft), false, OrderStatus.Draft},
                {createOrderWithSamples(billedSamples, OrderStatus.Abandoned), false, OrderStatus.Abandoned},
                // If all samples are billed, transition to Completed.
                {createOrderWithSamples(billedSamples, OrderStatus.Submitted), true, OrderStatus.Completed},
                {createOrderWithSamples(billedSamples, OrderStatus.Completed), false, OrderStatus.Completed},
                // If all samples are abandoned, transition to Completed.
                {createOrderWithSamples(abandonedSamples, OrderStatus.Submitted), true, OrderStatus.Completed},
                {createOrderWithSamples(abandonedSamples, OrderStatus.Completed), false, OrderStatus.Completed},
                // If none are billed, transition to Submitted.
                {createOrderWithSamples(notBilledSamples, OrderStatus.Submitted), false, OrderStatus.Submitted},
                {createOrderWithSamples(notBilledSamples, OrderStatus.Completed), true, OrderStatus.Submitted},
                // If at least one is not billed, transition to Submitted.
                {createOrderWithSamples(atLeastOneNotBilled, OrderStatus.Submitted), false, OrderStatus.Submitted},
                {createOrderWithSamples(atLeastOneNotBilled, OrderStatus.Completed), true, OrderStatus.Submitted},
                // Sample is billed to add-on price item, should treat as unbilled and transition to Submitted.
                {createOrderWithSamples(billedToAddOnSamples, OrderStatus.Submitted), false, OrderStatus.Submitted},
                {createOrderWithSamples(billedToAddOnSamples, OrderStatus.Completed), true, OrderStatus.Submitted},
        };
    }

    @Test(dataProvider = "testUpdateOrderStatus")
    public void testUpdateOrderStatus(ProductOrder order, boolean result, OrderStatus status) {
        Assert.assertEquals(order.updateOrderStatus(), result);
        Assert.assertEquals(order.getOrderStatus(), status);
    }
}
