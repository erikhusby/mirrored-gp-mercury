package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
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

    @Inject
    UserBean userBean;

    private String testKey;

    private static final int NUM_TEST_SAMPLES = 20;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        userBean.loginTestUser();

        utx.begin();

        String[] names = new String[NUM_TEST_SAMPLES];
        for (int i = 0; i < names.length; i++) {
            names[i] = "SM-TEST" + i;
        }

        ProductOrder order = ProductOrderDBTestFactory.createTestProductOrder(researchProjectDao, productDao, names);
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

        userBean.logout();
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
        ProductOrder order = productOrderDao.findByBusinessKey(testKey);
        List<ProductOrderSample> samples = order.getSamples();
        List<ProductOrderSample> samplesToAbandon = Arrays.asList(samples.get(1), samples.get(3), samples.get(5));
        productOrderEjb.abandonSamples(testKey, samplesToAbandon);
        Assert.assertEquals(order.getOrderStatus(), ProductOrder.OrderStatus.Draft);
        Assert.assertEquals(samples.size(), NUM_TEST_SAMPLES);
        for (ProductOrderSample sample : samples) {
            ProductOrderSample.DeliveryStatus status = ProductOrderSample.DeliveryStatus.NOT_STARTED;
            if (samplesToAbandon.contains(sample)) {
                status = ProductOrderSample.DeliveryStatus.ABANDONED;
            }
            Assert.assertEquals(sample.getDeliveryStatus(), status);
        }
    }

    @Test
    public void testCompleteSamples() throws Exception{
        ProductOrder order = productOrderDao.findByBusinessKey(testKey);
        List<ProductOrderSample> samples = order.getSamples();
        List<ProductOrderSample> samplesToComplete = Arrays.asList(samples.get(12), samples.get(14), samples.get(16));
        productOrderEjb.completeSamples(testKey, samplesToComplete);
        Assert.assertEquals(order.getOrderStatus(), ProductOrder.OrderStatus.Draft);
        Assert.assertEquals(samples.size(), NUM_TEST_SAMPLES);
        for (ProductOrderSample sample : samples) {
            ProductOrderSample.DeliveryStatus status = ProductOrderSample.DeliveryStatus.NOT_STARTED;
            if (samplesToComplete.contains(sample)) {
                status = ProductOrderSample.DeliveryStatus.DELIVERED;
            }
            Assert.assertEquals(sample.getDeliveryStatus(), status);
        }
    }
}
