package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.apache.commons.lang3.time.DateUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.text.MessageFormat;
import java.util.*;


@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class ProductOrderDaoTest extends ContainerTest {

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

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
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

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
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
                productOrderDao.findListByBusinessKeyList(Collections.singletonList(order.getBusinessKey()));
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

        allBusinessKeys.addAll(allBusinessKeys);

        Assert.assertTrue(allBusinessKeys.size() > 1000);

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
    private void assertContains(Map<String, ProductOrder> productOrderMap, String productOrderKey, String sampleBarcode) {
        Assert.assertNotNull(productOrderMap);
        Assert.assertTrue(productOrderMap.containsKey(productOrderKey));

        ProductOrder productOrder = productOrderMap.get(productOrderKey);
        for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
            if (productOrderSample.getSampleName().equals(sampleBarcode)) {
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
        Assert.assertEquals(productOrders.size(), 3);

        Map<String, ProductOrder> productOrderMap = new HashMap<>();
        for (ProductOrder productOrder : productOrders) {
            productOrderMap.put(productOrder.getBusinessKey(), productOrder);
        }

        assertContains(productOrderMap, "PDO-359", "NTC");
        assertContains(productOrderMap, "PDO-532", "NTC");
        assertContains(productOrderMap, "PDO-373", "SC_9001");
    }

}
