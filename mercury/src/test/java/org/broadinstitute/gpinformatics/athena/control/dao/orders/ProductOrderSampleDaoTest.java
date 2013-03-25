package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.List;

@Test(enabled = true)
public class ProductOrderSampleDaoTest extends ContainerTest {

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    ProductDao productDao;

    @Inject
    ResearchProjectDao researchProjectDao;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    UserTransaction utx;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void testFindByProductOrder() throws Exception {
        String[] sampleNames = { "MS-1111", "MS-2222", "MS-3333" };

        ProductOrder order = ProductOrderDBFactory.createTestProductOrder(researchProjectDao, productDao, sampleNames);
        String testProductOrderKey = order.getBusinessKey();

        productOrderDao.persist(order);
        productOrderDao.flush();
        productOrderDao.clear();

        ProductOrder orderFromDb = productOrderDao.findByBusinessKey(testProductOrderKey);

        // Find the list of samples for a given product Order.
        List<ProductOrderSample> samplesFromDb = orderFromDb.getSamples();
        Assert.assertNotNull(samplesFromDb);
        Assert.assertEquals(samplesFromDb.size(), sampleNames.length);
        // check the sample order, should be the same.
        for (int i = 0; i < sampleNames.length; i++) {
            Assert.assertEquals(samplesFromDb.get(i).getSampleName(), sampleNames[i]);
        }
    }
}
