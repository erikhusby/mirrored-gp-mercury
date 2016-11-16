package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderSampleTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.hamcrest.Matchers;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder.OrderStatus;
import static org.broadinstitute.gpinformatics.infrastructure.matchers.InBspFormatSample.inBspFormat;
import static org.broadinstitute.gpinformatics.infrastructure.matchers.MetadataMatcher.isMetadataSource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;


@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderTest {

    private static final Long TEST_CREATOR = 1111L;
    public static final String PDO_JIRA_KEY = "PDO-8";
    private static final String PDO_TITLE = "title";
    private static final String QUOTE = "quote";
    private final List<ProductOrderSample> sixSamplesWithNamesInBspFormatNoDupes =
            ProductOrderSampleTestFactory
                    .createDBFreeSampleList(MercurySample.MetadataSource.BSP, "SM-2ACGC", "SM-2ABDD", "SM-2ACKV", "SM-2AB1B", "SM-2ACJC", "SM-2AD5D");
    private final List<ProductOrderSample> sixMercurySamplesNoDupes =
            ProductOrderSampleTestFactory
                    .createDBFreeSampleList(MercurySample.MetadataSource.MERCURY, "SM-2ACGC", "SM-2ABDD", "SM-2ACKV", "SM-2AB1B", "SM-2ACJC", "SM-2AD5D");
    private final List<ProductOrderSample> fourBspSamplesWithDupes =
            ProductOrderSampleTestFactory
                    .createDBFreeSampleList(MercurySample.MetadataSource.BSP, "SM-2ACGC", "SM-2ABDD", "SM-2ACGC", "SM-2AB1B", "SM-2ACJC", "SM-2ACGC");
    private final List<ProductOrderSample> fourBspSamplesWithNoDupes =
            ProductOrderSampleTestFactory
                    .createDBFreeSampleList(MercurySample.MetadataSource.BSP, "SM-2ABDD", "SM-2AB1B", "SM-2ACJC", "SM-2ACGC");
    private final List<ProductOrderSample> sixSamplesWithNotAllNamesInBspFormat =
            ProductOrderSampleTestFactory
                    .createDBFreeSampleList(MercurySample.MetadataSource.BSP, "SM-2ACGC", "SM2ABDD", "SM2ACKV", "SM-2AB1B", "SM-2ACJC", "SM-2AD5D");
    private final List<ProductOrderSample> sixSamplesWithNoNamesInBspFormat =
            ProductOrderSampleTestFactory
                    .createDBFreeSampleList(MercurySample.MetadataSource.BSP, "SSM-2ACGC1", "SM--2ABDDD", "SM-2AB", "SM-2AB1B-", "SM-2ACJCACB",
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

    public void testBeaniness() {

        BeanTester tester = new BeanTester();
        // Currently ProductOrder is equivalent based only on RP and title.
        Configuration configuration = new ConfigurationBuilder()
                .ignoreProperty("samples")
                .ignoreProperty(PDO_TITLE)
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
                .ignoreProperty("publicationDeadline")
                .ignoreProperty("productOrderKit")
                .ignoreProperty("skipQuoteReason")
                .ignoreProperty("skipRegulatoryReason")
                .ignoreProperty("attestationConfirmed")
                .ignoreProperty("regulatoryInfos")
                .ignoreProperty("squidWorkRequest")
                .ignoreProperty("sapOrderNumber")
                .ignoreProperty("sapReferenceOrders")
                .ignoreProperty("childOrders")
                .ignoreProperty("parentOrder")
                .build();
        tester.testBean(ProductOrder.class, configuration);

        class ProductOrderFactory implements EquivalentFactory<ProductOrder> {
            public final long ID = new Random().nextInt(Integer.MAX_VALUE);

            @Override
            public ProductOrder create() {
                Product product = new Product("Exome Express", null, "Exome Express", "P-EX-0002", new Date(), null,
                        1814400, 1814400, 184, null, null, null, true, Workflow.AGILENT_EXOME_EXPRESS, false, "agg type");
                ResearchProject researchProject =
                        new ResearchProject(ID, PDO_TITLE, "RP title", ResearchProject.IRB_NOT_ENGAGED,
                                            ResearchProject.RegulatoryDesignation.RESEARCH_ONLY);

                return new ProductOrder(ID, "PO title", sixSamplesWithNamesInBspFormatNoDupes, "quoteId", product, researchProject);
            }
        }

        // Currently ProductOrder is equivalent based only on business key (productOrderId, jiraTicketKey)
        new EqualsMethodTester().testEqualsMethod(new ProductOrderFactory(), configuration);

        new HashCodeMethodTester().testHashCodeMethod(new ProductOrderFactory());

    }

    public void testOrder() throws Exception {
        assertThat(productOrder.getJiraTicketKey(), is(equalTo(PDO_JIRA_KEY)));
    }

    public void testGetUniqueSampleCount() throws Exception {
        productOrder = new ProductOrder(TEST_CREATOR, PDO_TITLE, sixSamplesWithNamesInBspFormatNoDupes, QUOTE, null, null);
        assertThat(productOrder.getUniqueSampleCount(), is(equalTo(6)));

        productOrder = new ProductOrder(TEST_CREATOR, PDO_TITLE, fourBspSamplesWithDupes, QUOTE, null, null);
        assertThat(productOrder.getUniqueSampleCount(), is(equalTo(4)));
    }

    public void testGetTotalSampleCount() throws Exception {
        productOrder = new ProductOrder(TEST_CREATOR, PDO_TITLE, sixSamplesWithNamesInBspFormatNoDupes, QUOTE, null, null);
        assertThat(productOrder.getTotalSampleCount(), is(equalTo(6)));

        productOrder = new ProductOrder(TEST_CREATOR, PDO_TITLE, fourBspSamplesWithDupes, QUOTE, null, null);
        assertThat(productOrder.getTotalSampleCount(), is(equalTo(6)));
    }

    public void testGetDuplicateCount() throws Exception {
        productOrder = new ProductOrder(TEST_CREATOR, PDO_TITLE, fourBspSamplesWithDupes, QUOTE, null, null);
        assertThat(productOrder.getDuplicateCount(), is(equalTo(2)));
    }

    @SuppressWarnings("unchecked")
    public void testGetProductOrderWithMixedSampleMetadata() throws Exception {
        List<ProductOrderSample> sampleList = ProductOrderSampleTestFactory
                .createDBFreeSampleList(MercurySample.MetadataSource.BSP, "SM-2ACGC", "SM-2ABDD", "SM-2ACKV");
        sampleList.addAll(ProductOrderSampleTestFactory
                .createDBFreeSampleList(MercurySample.MetadataSource.MERCURY, "SM-2AB1B", "SM-2ACJC", "SM-2AD5D"));

        productOrder = new ProductOrder(TEST_CREATOR, PDO_TITLE, sampleList, QUOTE, null, null);
        assertThat(productOrder.getSamples().size(), Matchers.is(6));
        assertThat(productOrder.getSamples(), not(everyItem(isMetadataSource(MercurySample.MetadataSource.BSP))));
        assertThat(productOrder.getSamples(), not(everyItem(isMetadataSource(MercurySample.MetadataSource.MERCURY))));

        List<String> sampleSummaryComments = productOrder.getSampleSummaryComments();
        assertThat(sampleSummaryComments, hasItems(
                "Total: 6",
                "Unique: All",
                "Duplicate: None",
                "Unique BSP: 3",
                "Unique Mercury: 3",
                "On Risk: None"));
    }

    @SuppressWarnings("unchecked")
    public void testGetProductOrderBspSampleMetadata() throws Exception {
        productOrder = new ProductOrder(TEST_CREATOR, PDO_TITLE, sixSamplesWithNamesInBspFormatNoDupes, QUOTE, null, null);
        assertThat(productOrder.getSamples(), everyItem(isMetadataSource(MercurySample.MetadataSource.BSP)));
        List<String> sampleSummaryComments = productOrder.getSampleSummaryComments();
        assertThat(sampleSummaryComments, hasItems(
                "Total: 6",
                "Unique: All",
                "Duplicate: None",
                "From BSP: All",
                "Unique BSP: 6",
                "On Risk: None"
        ));
    }

    @SuppressWarnings("unchecked")
    public void testGetProductOrderMercurySampleMetadata() throws Exception {
        productOrder = new ProductOrder(TEST_CREATOR, PDO_TITLE, sixMercurySamplesNoDupes, QUOTE, null, null);
        assertThat(productOrder.getSamples(), everyItem(isMetadataSource(MercurySample.MetadataSource.MERCURY)));

        List<String> sampleSummaryComments = productOrder.getSampleSummaryComments();
        assertThat(sampleSummaryComments, hasItems(
                "Total: 6",
                "Unique: All",
                "Duplicate: None",
                "From Mercury: All",
                "Unique Mercury: 6",
                "On Risk: None"
                ));
    }

    public void testAreAllSampleBSPFormat() throws Exception {
        assertThat(fourBspSamplesWithDupes, everyItem(is(inBspFormat())));
        assertThat(sixSamplesWithNamesInBspFormatNoDupes, everyItem(is(inBspFormat())));
        assertThat(sixSamplesWithNoNamesInBspFormat, everyItem(is(not(inBspFormat()))));
        assertThat(sixSamplesWithNotAllNamesInBspFormat, not(everyItem(is(inBspFormat()))));
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

    public void testQuoteStringForJira() {
        ProductOrder pdo = new ProductOrder();
        Assert.assertEquals(pdo.getQuoteStringForJiraTicket(),ProductOrder.QUOTE_TEXT_USED_IN_JIRA_WHEN_QUOTE_FIELD_IS_EMPTY);
        String quoteId = "BLAH";
        pdo.setQuoteId(quoteId);
        Assert.assertEquals(pdo.getQuoteStringForJiraTicket(),quoteId);
    }

    public void testQuoteRequired() {
        boolean hasQuote = false;
        String quoteOrNoQuoteString = "just because";
        int numberOfSamples = 4;
        ProductOrder pdo = ProductOrderTestFactory.buildSampleInitiationProductOrder(numberOfSamples);

        Assert.assertTrue(pdo.canSkipQuote());
        Assert.assertEquals(
                TestUtils.getFirst(pdo.getProductOrderKit().getKitOrderDetails()).getNumberOfSamples().longValue(),
                numberOfSamples);
    }

    public void testSetResearchProject() {
        ResearchProject researchProject = new ResearchProject(0L, "ProductOrderTest Research Project", "Test", true,
                                                              ResearchProject.RegulatoryDesignation.RESEARCH_ONLY);

        productOrder.setResearchProject(researchProject);
        assertThat(productOrder.getResearchProject(), equalTo(researchProject));
        assertThat(researchProject.getProductOrders(), hasItem(productOrder));

        productOrder.setResearchProject(null);
        assertThat(productOrder.getResearchProject(), nullValue());
        assertThat(researchProject.getProductOrders(), not(hasItem(productOrder)));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testNullRegulatoryDesignationThrowsException() {
        new ProductOrder("Foo","Bar","Baz").getRegulatoryDesignationCodeForPipeline();
    }

    public void testRegulatoryInfoEditAllowedForDraftPDO() {
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Draft);
        Assert.assertTrue(productOrder.isRegulatoryInfoEditAllowed());
    }

    public void testRegulatoryInfoEditAllowedForPendingPDO() {
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Pending);
        Assert.assertTrue(productOrder.isRegulatoryInfoEditAllowed());
    }

    public void testRegulatoryInfoEditNotAllowedForSubmittedPDO() {
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        Assert.assertFalse(productOrder.isRegulatoryInfoEditAllowed());
    }

    public void testAllSamplesHaveBeenBilled() {

        ProductOrder testProductOrder = ProductOrderTestFactory.createDummyProductOrder(2, PDO_JIRA_KEY);

        ProductOrderSample sample1 = testProductOrder.getSamples().get(0);
        ProductOrderSample sample2 = testProductOrder.getSamples().get(1);

        Product addonProduct = ProductTestFactory.createDummyProduct(Workflow.ICE_CRSP, "test-product-addon");
        PriceItem exExPriceItem =
                new PriceItem(testProductOrder.getQuoteId(), PriceItem.PLATFORM_GENOMICS, PriceItem.CATEGORY_EXOME_SEQUENCING_ANALYSIS,
                        PriceItem.NAME_STANDARD_WHOLE_EXOME);
        addonProduct.setPrimaryPriceItem(exExPriceItem);

        testProductOrder.getProduct().getAddOns().add(addonProduct);
        testProductOrder.setProductOrderAddOns(Collections.singletonList(new ProductOrderAddOn(addonProduct, productOrder)));

        Assert.assertEquals(testProductOrder.getUnbilledSampleCount(), 2);

        billSampleOut(testProductOrder, sample1, 2);

        Assert.assertEquals(testProductOrder.getUnbilledSampleCount(), 1);

        billSampleOut(testProductOrder, sample2, 1);

        Assert.assertEquals(testProductOrder.getUnbilledSampleCount(), 0);
    }

    public void testNonAbandonedCount() {
        ProductOrder testParentOrder = new ProductOrder(TEST_CREATOR, "Test order with Abandoned Count",sixMercurySamplesNoDupes, QUOTE,null, null);

        Assert.assertEquals(6, testParentOrder.getNonAbandonedCount());
        Assert.assertEquals(0, testParentOrder.getNumberForReplacement());

        int numberOfAbandoned = 4;
        for(ProductOrderSample sampleToAbandon:testParentOrder.getSamples()) {

            if (numberOfAbandoned == 0) {
                break;
            }
            sampleToAbandon.setDeliveryStatus(ProductOrderSample.DeliveryStatus.ABANDONED);
            numberOfAbandoned--;
        }
        Assert.assertEquals(2, testParentOrder.getNonAbandonedCount());
        Assert.assertEquals(4, testParentOrder.getNumberForReplacement());

        ProductOrder cloneOrder = new ProductOrder(testParentOrder, true);

        Assert.assertEquals(2, testParentOrder.getNonAbandonedCount());
        Assert.assertEquals(4, testParentOrder.getNumberForReplacement());
        cloneOrder.addSamples(fourBspSamplesWithNoDupes);

        Assert.assertEquals(6, testParentOrder.getNonAbandonedCount());
        Assert.assertEquals(0, testParentOrder.getNumberForReplacement());
    }

    public void testLatestSapOrder() {
        ProductOrder testProductOrder = new ProductOrder(TEST_CREATOR, "Test SAPReference orders",
                sixMercurySamplesNoDupes, QUOTE, null,null);

        assertThat(testProductOrder.latestSapOrderDetail(), is(nullValue()));
        assertThat(testProductOrder.getSapOrderNumber(), isEmptyOrNullString());

        final String sapOrderNumber = "SAP_001";
        final SapOrderDetail orderDetail1 = new SapOrderDetail(sapOrderNumber, 5, QUOTE,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode());
        orderDetail1.getUpdateData().setCreatedDate(new Date());
        final SapOrderDetail orderDetail2 = new SapOrderDetail(sapOrderNumber+"2", 5, QUOTE,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode());
        orderDetail2.getUpdateData().setCreatedDate(new Date());
        testProductOrder.addSapOrderDetail(orderDetail1);

        assertThat(testProductOrder.latestSapOrderDetail(), is(not(nullValue())));
        assertThat(testProductOrder.getSapOrderNumber(), is(equalTo(sapOrderNumber)));

        testProductOrder.addSapOrderDetail(orderDetail2);
        assertThat(testProductOrder.latestSapOrderDetail(), is(not(nullValue())));
        assertThat(testProductOrder.getSapOrderNumber(), is(equalTo(sapOrderNumber+"2")));
    }

    private void billSampleOut(ProductOrder productOrder, ProductOrderSample sample, int expected) {

        LedgerEntry primaryItemSampleEntry = new LedgerEntry(sample,
                productOrder.getProduct().getPrimaryPriceItem(), new Date(), /*productOrder.getProduct(),*/ 1);
        primaryItemSampleEntry.setPriceItemType(LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM);

        LedgerEntry addonItemSampleEntry = new LedgerEntry(sample,
                productOrder.getAddOns().iterator().next().getAddOn().getPrimaryPriceItem(),
                new Date(), /*productOrder.getProduct(),*/ 1);
        addonItemSampleEntry.setPriceItemType(LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM);
        sample.getLedgerItems().add(primaryItemSampleEntry);
        sample.getLedgerItems().add(addonItemSampleEntry);


        Assert.assertEquals(productOrder.getUnbilledSampleCount(), expected);


        BillingSession billingSession =
                new BillingSession(4L, sample.getLedgerItems());


        Assert.assertEquals(productOrder.getUnbilledSampleCount(), expected);


        addonItemSampleEntry.setBillingMessage(BillingSession.SUCCESS);
        Assert.assertEquals(productOrder.getUnbilledSampleCount(), expected);
        primaryItemSampleEntry.setBillingMessage(BillingSession.SUCCESS);
        billingSession.setBilledDate(new Date());
    }
}
