package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Test(enabled = true)
public class ProductOrderSampleDaoTest extends ContainerTest {

    @Inject
    ProductOrderDao productOrderDao;

    @Inject ProductOrderSampleDao
    pdoSampleDao;

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

        ProductOrder order = ProductOrderDBTestFactory
                .createTestProductOrder(researchProjectDao, productDao, sampleNames);
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
            Assert.assertEquals(samplesFromDb.get(i).getName(), sampleNames[i]);
        }
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void testFindByPDOKeyAndSampleNames() {
        String[] sampleNames = { "MS-1111", "MS-2222", "MS-3333" };

        ProductOrder order = ProductOrderDBTestFactory.createTestProductOrder(researchProjectDao, productDao,sampleNames);

        productOrderDao.persist(order);
        productOrderDao.flush();
        productOrderDao.clear();

        Set<String> sampleNamesSubset = new HashSet<>();
        sampleNamesSubset.add(sampleNames[0]);
        sampleNamesSubset.add(sampleNames[2]);
        List<ProductOrderSample> pdoSamplesFromDb = pdoSampleDao.findByOrderKeyAndSampleNames(order.getBusinessKey(),
                sampleNamesSubset);

        Assert.assertNotNull(pdoSamplesFromDb);
        Assert.assertEquals(pdoSamplesFromDb.size(), sampleNamesSubset.size());

        List<String> returnedSampleNames = new ArrayList<>();
        for (ProductOrderSample productOrderSample : pdoSamplesFromDb) {
            Assert.assertFalse(sampleNames[1].equals(productOrderSample.getName()));  // our PDO has 3 samples, but we only queried for two of them
            Assert.assertEquals(productOrderSample.getProductOrder().getBusinessKey(),order.getBusinessKey());
            returnedSampleNames.add(productOrderSample.getName());
        }

        Assert.assertEquals(returnedSampleNames,sampleNamesSubset);
    }
}
