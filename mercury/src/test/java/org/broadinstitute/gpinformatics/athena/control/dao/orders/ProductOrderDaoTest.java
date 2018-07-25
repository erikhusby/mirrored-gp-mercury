package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.apache.commons.lang3.time.DateUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderCompletionStatus;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.infrastructure.common.BaseSplitter;
import org.broadinstitute.gpinformatics.infrastructure.jpa.ThreadEntityManager;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.PersistenceUnitUtil;
import javax.transaction.UserTransaction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;


@Test(groups = TestGroups.STUBBY, enabled = true, singleThreaded = true)
@Dependent
public class ProductOrderDaoTest extends StubbyContainerTest {

    public ProductOrderDaoTest(){}

    @Inject
    private ThreadEntityManager entityManager;


    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductDao productDao;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    ProductOrder order;

    @BeforeMethod(groups = TestGroups.STUBBY)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (utx == null) {
            return;
        }

        utx.begin();

        order = ProductOrderDBTestFactory.createTestProductOrder(researchProjectDao, productDao);
        productOrderDao.persist(order);
        productOrderDao.flush();
        productOrderDao.clear();
    }

    @AfterMethod(groups = TestGroups.STUBBY)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    public void testFindOrders() {
        // Try to find the created ProductOrder by business key.
        ProductOrder productOrderFromDb = productOrderDao.findByBusinessKey(order.getBusinessKey());
        Assert.assertNotNull(productOrderFromDb);
        Assert.assertEquals(productOrderFromDb.getTitle(), order.getTitle());
        Assert.assertEquals(productOrderFromDb.getQuoteId(), order.getQuoteId());
        Assert.assertEquals(productOrderFromDb.getTotalSampleCount(), order.getTotalSampleCount());
        Assert.assertEquals(productOrderFromDb.getSamples().size(), order.getSamples().size());

        // Try to find a non-existing ProductOrder.
        productOrderFromDb = productOrderDao.findByBusinessKey("NonExistingProductOrder_" + UUID.randomUUID());
        Assert.assertNull(productOrderFromDb);

        // Try to find by list of keys.
        List<ProductOrder> orders =
                productOrderDao.findListByBusinessKeys(Collections.singletonList(order.getBusinessKey()));
        Assert.assertNotNull(orders);
        if (!orders.isEmpty()) {
            productOrderFromDb = orders.get(0);
        }
        Assert.assertNotNull(productOrderFromDb);
    }

    public void testFindModifiedAfter() {
        Date date = new Date();
        // Yesterday
        date.setTime(date.getTime() - DateUtils.MILLIS_PER_DAY);
        List<ProductOrder> orders = productOrderDao.findModifiedAfter(date);
        Assert.assertFalse(orders.isEmpty());

        // Tomorrow
        date.setTime(new Date().getTime() + DateUtils.MILLIS_PER_DAY);
        orders = productOrderDao.findModifiedAfter(date);
        Assert.assertTrue(orders.isEmpty());
    }

    public void testFindAll() {
        List<ProductOrder> orders = productOrderDao.findAll();
        Assert.assertNotNull(orders);
        Assert.assertFalse(orders.isEmpty());
    }

    public void testFindByWorkflow() {
        ProductOrder testOrder = ProductOrderDBTestFactory.createTestExExProductOrder(researchProjectDao, productDao);

        productOrderDao.persist(testOrder);
        productOrderDao.flush();
        productOrderDao.clear();

        Collection<ProductOrder> orders = productOrderDao.findByWorkflow(Workflow.AGILENT_EXOME_EXPRESS);

        Assert.assertFalse(orders.isEmpty());
    }

    public void testSplitterCriteriaFind() throws Exception {
        List<ProductOrder> allOrders = productOrderDao.findAll();

        List<String> allBusinessKeys = new ArrayList<>();
        for (ProductOrder order : allOrders) {
            if (order.getJiraTicketKey() != null) {
                allBusinessKeys.add(order.getBusinessKey());
            }
        }

        int originalSize = allBusinessKeys.size();

        Assert.assertTrue(allBusinessKeys.size() > BaseSplitter.DEFAULT_SPLIT_SIZE);

        List<ProductOrder> pdoList =
            productOrderDao.findListByList(ProductOrder.class, ProductOrder_.jiraTicketKey, allBusinessKeys);

        Set<ProductOrder> uniqueOrders = new HashSet<>(pdoList);
        Assert.assertEquals(
            uniqueOrders.size(), originalSize, "The number of unique orders should be the same as original size");
    }

    /**
     * Helper method for {@link #testFindBySampleBarcodes} test method.
     *
     * @param productOrderMap Input map of PDO barcodes to PDOs.
     * @param productOrderKey PDO to search.
     * @param sampleBarcode Sample barcode we expect to find in this PDO.
     */
    private static void assertContains(Map<String, ProductOrder> productOrderMap, String productOrderKey,
                                       String sampleBarcode) {
        Assert.assertNotNull(productOrderMap);
        Assert.assertTrue(productOrderMap.containsKey(productOrderKey));

        ProductOrder productOrder = productOrderMap.get(productOrderKey);
        for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
            if (productOrderSample.getName().equals(sampleBarcode)) {
                return;
            }
        }

        Assert.fail(MessageFormat.format("Sample {0} not found in {1}", sampleBarcode, productOrderKey));
    }

    /**
     * Ugly positive test method for finding {@link ProductOrder}s by sample barcode, uses real data.
     */
    public void testFindBySampleBarcodes() {

        List<ProductOrder> productOrders = productOrderDao.findBySampleBarcodes("NTC", "SC_9001");
        Assert.assertNotNull(productOrders);
        Assert.assertTrue(productOrders.size() >= 3);

        Map<String, ProductOrder> productOrderMap = new HashMap<>();
        for (ProductOrder productOrder : productOrders) {
            productOrderMap.put(productOrder.getBusinessKey(), productOrder);
        }

        assertContains(productOrderMap, "PDO-359", "NTC");
        assertContains(productOrderMap, "PDO-532", "NTC");
        assertContains(productOrderMap, "PDO-373", "SC_9001");
    }

    /**
     * Verify that things are pre-fetched properly for billing performance.  See GPLIM-832.
     */
    @Test
    public void testThatPDOFetchedForBillingHasHadRelatedEntitiesPrefetched() throws Exception {
        List<ProductOrder> pdos = productOrderDao.findListForBilling(Collections.singletonList("PDO-127"));
        Assert.assertEquals(pdos.size(), 1);

        PersistenceUnitUtil persistenceUtil = entityManager.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil();

        ProductOrder pdo = pdos.iterator().next();

        Assert.assertTrue(persistenceUtil.isLoaded(pdo,"samples"),"Samples should be pre-fetched so that the billing tracker download doesn't take forever to download.");

        for (ProductOrderSample productOrderSample : pdo.getSamples()) {
            Assert.assertTrue(persistenceUtil.isLoaded(productOrderSample, "ledgerItems"),
                    "Ledger items should be pre-fetched so that the billing tracker download doesn't take forever to download.");
            Assert.assertTrue(persistenceUtil.isLoaded(productOrderSample, "riskItems"),
                    "Risk items should be pre-fetched so that the billing tracker download doesn't take forever to download.");
        }
    }

    @Test(enabled = false)
    public void testFindUnconvertedSampleInitiationPdos() throws Exception {
        List<ProductOrder> unconvertedProductOrders = productOrderDao.findSampleInitiationPDOsNotConverted();

        Assert.assertEquals(unconvertedProductOrders.size(), 4);
    }

    public void testGetStatusWithSplit() throws Exception {
        List<ProductOrder> allOrders = productOrderDao.findAll();

        List<Long> allOrderIds = new ArrayList<>();
        for (ProductOrder order : allOrders) {
            allOrderIds.add(order.getProductOrderId());
        }

        // Make sure we have enough PDOs to test that the Splitter API is being used correctly.
        // This is also why we don't use getAllProgress() here instead.
        Assert.assertTrue(allOrderIds.size() > BaseSplitter.DEFAULT_SPLIT_SIZE);

        Map<String, ProductOrderCompletionStatus> statusMap = productOrderDao.getProgress(allOrderIds);

        // Need to use x2 here because of the list duplication above.
        Assert.assertEquals(statusMap.keySet().size(), allOrderIds.size(),
                "There should be statuses for every item");
    }

    /**
     * Test that {@link ProductOrderDao#getProgress(Collection)} returns counts equal to the result of iterating over
     * all of the samples. Adapted from test for GPLIM-1206, previously in CompletionStatusFetcherTest.
     */
    public void testGetProgressCounts() {
        ProductOrder productOrder = ProductOrderDBTestFactory
                .createProductOrder(productOrderDao, "SM-0001", "SM-0002", "SM-0003", "SM-0004", "SM-0005", "SM-0006");

        productOrder.getSamples().get(0).setDeliveryStatus(ProductOrderSample.DeliveryStatus.ABANDONED);
        productOrder.getSamples().get(1).setDeliveryStatus(ProductOrderSample.DeliveryStatus.ABANDONED);
        productOrder.getSamples().get(2).setDeliveryStatus(ProductOrderSample.DeliveryStatus.NOT_STARTED);

        Set<LedgerEntry> ledgerEntries = new HashSet<>();
        for (int i = 3; i < 6; i++) {
            ProductOrderSample productOrderSample = productOrder.getSamples().get(i);
            productOrderSample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.DELIVERED);
            LedgerEntry ledgerEntry =
                    new LedgerEntry(productOrderSample, productOrder.getProduct().getPrimaryPriceItem(), new Date(), 1);
            ledgerEntry.setPriceItemType(LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM);
            ledgerEntries.add(ledgerEntry);
        }
        BillingSession billingSession = new BillingSession(1L, ledgerEntries);
        billingSession.setBilledDate(new Date());
        productOrderDao.persist(billingSession);

        productOrderDao.flush();
        productOrderDao.clear();

        Map<String, ProductOrderCompletionStatus> statusMap =
                productOrderDao.getProgress(Collections.singleton(productOrder.getProductOrderId()));
        assertThat(statusMap.size(), equalTo(1));
        ProductOrderCompletionStatus status = statusMap.values().iterator().next();

        assertThat(status.getNumberCompleted(), is(equalTo(3)));
        assertThat(status.getNumberAbandoned(), is(greaterThanOrEqualTo(2)));
        assertThat(status.getNumberInProgress(), is(equalTo(1)));
    }

    public void testUpdateQuoteAfterOrderSaved() {
        ProductOrder newOrder = ProductOrderDBTestFactory.createTestProductOrder(researchProjectDao, productDao);
        newOrder.setQuoteId("");
        productOrderDao.persist(newOrder);
        productOrderDao.flush();
        productOrderDao.clear();
        newOrder = productOrderDao.findByBusinessKey(newOrder.getBusinessKey());

        assertThat(newOrder.getQuoteId(), nullValue());
        newOrder.setQuoteId("newquote");
        assertThat(newOrder.getQuoteId(), not(nullValue()));
    }

}
