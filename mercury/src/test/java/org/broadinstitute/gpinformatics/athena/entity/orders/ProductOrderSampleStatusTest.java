package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDaoTest;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Arrays;
import java.util.List;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class ProductOrderSampleStatusTest extends ContainerTest {

    @Inject
    ProductOrderEjb productOrderEjb;

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    ProductDao productDao;

    @Inject
    ResearchProjectDao researchProjectDao;

    @Inject
    UserTransaction utx;

    private String testKey;

    private static final int NUM_TEST_SAMPLES = 20;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();

        String[] names = new String[NUM_TEST_SAMPLES];
        for (int i = 0; i < names.length; i++) {
            names[i] = "SM-TEST" + i;
        }

        ProductOrder order = ProductOrderDaoTest.createTestProductOrder(researchProjectDao, productDao, names);
        testKey = order.getBusinessKey();
        productOrderDao.persist(order);
        productOrderDao.flush();
        productOrderDao.clear();
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    /**
     * This should work if there was actually a 'Close' or 'Complete' transition, which as of the time of this writing
     * does not exist
     */
    @Test
    public void testCompletePDO() throws Exception {
        productOrderEjb.complete(testKey, "This has been completed by my test!");
        ProductOrder order = productOrderDao.findByBusinessKey(testKey);
        Assert.assertEquals(order.getOrderStatus(), ProductOrder.OrderStatus.Complete);
        for (ProductOrderSample sample : order.getSamples()) {
            Assert.assertEquals(sample.getDeliveryStatus(), ProductOrderSample.DeliveryStatus.DELIVERED);
        }
    }

    @Test
    public void testAbandonPDO() throws Exception {
        productOrderEjb.abandon(testKey, "This has been abandoned by my test!");
        ProductOrder order = productOrderDao.findByBusinessKey(testKey);
        Assert.assertEquals(order.getOrderStatus(), ProductOrder.OrderStatus.Abandoned);
        for (ProductOrderSample sample : order.getSamples()) {
            Assert.assertEquals(sample.getDeliveryStatus(), ProductOrderSample.DeliveryStatus.ABANDONED);
        }
    }

    @Test
    public void testAbandonSamples() throws Exception {
        List<Integer> indices = Arrays.asList(1, 3, 5);
        List<String> comments =
                Arrays.asList("Abandoning sample uno", "And sample tres", "And sample 5 too");
        productOrderEjb.abandonSamples(testKey, indices, comments);
        ProductOrder order = productOrderDao.findByBusinessKey(testKey);
        Assert.assertEquals(order.getOrderStatus(), ProductOrder.OrderStatus.Draft);
        List<ProductOrderSample> samples = order.getSamples();
        Assert.assertEquals(samples.size(), NUM_TEST_SAMPLES);
        for (int i = 0; i < samples.size(); i++) {
            ProductOrderSample sample = samples.get(i);
            ProductOrderSample.DeliveryStatus status = ProductOrderSample.DeliveryStatus.NOT_STARTED;
            if (indices.contains(i)) {
                status = ProductOrderSample.DeliveryStatus.ABANDONED;
            }
            Assert.assertEquals(sample.getDeliveryStatus(), status);
        }
    }

    @Test
    public void testCompleteSamples() throws Exception{
        List<Integer> indices = Arrays.asList(12, 14, 16);
        List<String> comments = Arrays.asList("Completing sample 12", "And sample 14", "And sample 16 too");
        productOrderEjb.completeSamples(testKey, indices, comments);
        ProductOrder order = productOrderDao.findByBusinessKey(testKey);
        Assert.assertEquals(order.getOrderStatus(), ProductOrder.OrderStatus.Draft);
        List<ProductOrderSample> samples = order.getSamples();
        Assert.assertEquals(samples.size(), NUM_TEST_SAMPLES);
        for (int i = 0; i < samples.size(); i++) {
            ProductOrderSample sample = samples.get(i);
            ProductOrderSample.DeliveryStatus status = ProductOrderSample.DeliveryStatus.NOT_STARTED;
            if (indices.contains(i)) {
                status = ProductOrderSample.DeliveryStatus.DELIVERED;
            }
            Assert.assertEquals(sample.getDeliveryStatus(), status);
        }
    }
}
