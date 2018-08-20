package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Arrays;
import java.util.List;

@Test(groups = TestGroups.STUBBY, singleThreaded = true)
@Dependent
public class ProductOrderSampleStatusTest extends StubbyContainerTest {

    public ProductOrderSampleStatusTest(){}

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

    @BeforeMethod(groups = TestGroups.STUBBY)
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

    @AfterMethod(groups = TestGroups.STUBBY)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();

        userBean.logout();
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

        final MessageCollection messageCollection = new MessageCollection();

        productOrderEjb.abandonSamples(testKey, samplesToAbandon, "Why I abandoned you let me count the ways...",
                messageCollection);
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
}
