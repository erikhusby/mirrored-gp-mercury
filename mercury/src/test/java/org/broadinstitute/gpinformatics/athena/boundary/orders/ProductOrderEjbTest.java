package org.broadinstitute.gpinformatics.athena.boundary.orders;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDaoTest;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.List;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = false)
public class ProductOrderEjbTest extends ContainerTest {

    @Inject
    ProductOrderEjb productOrderEjb;

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    ResearchProjectDao researchProjectDao;

    @Inject
    ProductOrderSampleDao productOrderSampleDao;

    @Inject
    ProductDao productDao;

    @Inject
    UserTransaction utx;

    private String testOrderOneSampleKey;
    private String testOrderTwoSamplesKey;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, since we're not running in container.
        if (utx == null) {
            return;
        }

        utx.begin();

        ProductOrder order = ProductOrderDaoTest.createTestProductOrder(researchProjectDao, productDao,
                BSPSampleSearchServiceStub.STOCK_ID);
        testOrderOneSampleKey = order.getBusinessKey();
        productOrderDao.persist(order);

        order = ProductOrderDaoTest.createTestProductOrder(researchProjectDao, productDao,
                BSPSampleSearchServiceStub.STOCK_ID, BSPSampleSearchServiceStub.STOCK_ID);
        testOrderTwoSamplesKey = order.getBusinessKey();

        productOrderDao.persist(order);
        productOrderDao.flush();
        productOrderDao.clear();
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, since we're not running in container.
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    public void testMapAliquotIdToSample() throws Exception {
        // Test case where sample has not yet been mapped to an aliquot.
        ProductOrder order = productOrderDao.findByBusinessKey(testOrderOneSampleKey);
        List<ProductOrderSample> samples = productOrderSampleDao.findByOrderAndName(order, BSPSampleSearchServiceStub.STOCK_ID);
        Assert.assertTrue(samples.size() == 1);
        Assert.assertTrue(samples.get(0).getAliquotId() == null);

        // Now map it.
        ProductOrderSample sample = productOrderEjb.mapAliquotIdToSample(order, BSPSampleSearchServiceStub.ALIQUOT_ID_1);
        Assert.assertNotNull(sample);
        Assert.assertEquals(sample.getAliquotId(), BSPSampleSearchServiceStub.ALIQUOT_ID_1);

        // Test case where sample has already been mapped, should return same sample again.
        sample = productOrderEjb.mapAliquotIdToSample(order, BSPSampleSearchServiceStub.ALIQUOT_ID_1);
        Assert.assertNotNull(sample);
        Assert.assertEquals(sample.getAliquotId(), BSPSampleSearchServiceStub.ALIQUOT_ID_1);

        // Try to map another aliquot, should get an exception.
        try {
            sample = productOrderEjb.mapAliquotIdToSample(order, BSPSampleSearchServiceStub.ALIQUOT_ID_2);
            Assert.fail("Exception should be thrown");
        } catch (RuntimeException e) {
            // Error is expected.
        }

        // Test case where there are multiple samples, each one should map to a different aliquot.
        order = productOrderDao.findByBusinessKey(testOrderTwoSamplesKey);
        sample = productOrderEjb.mapAliquotIdToSample(order, BSPSampleSearchServiceStub.ALIQUOT_ID_1);
        ProductOrderSample sample2 = productOrderEjb.mapAliquotIdToSample(order, BSPSampleSearchServiceStub.ALIQUOT_ID_2);
        Assert.assertEquals(sample.getAliquotId(), BSPSampleSearchServiceStub.ALIQUOT_ID_1);
        Assert.assertEquals(sample2.getAliquotId(), BSPSampleSearchServiceStub.ALIQUOT_ID_2);
    }
}
