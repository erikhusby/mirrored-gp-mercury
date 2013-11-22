package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;


@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class ProductOrderKitTest extends ContainerTest {

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductDao productDao;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    private ProductOrder order;
    private final String dna = "DNA:DNA";

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (utx == null) {
            return;
        }

        utx.begin();

        order = ProductOrderDBTestFactory.createTestProductOrder(researchProjectDao, productDao);
        order.setProductOrderKit(new ProductOrderKit(17L, KitType.DNA_MATRIX, 0L, 0L, "7CC", dna, null));
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

    public void testProductOrderKit() throws Exception {
        ProductOrder pdo = productOrderDao.findByBusinessKey(order.getBusinessKey());
        ProductOrderKit kit = pdo.getProductOrderKit();
        Assert.assertNotNull(kit);
        Assert.assertNotNull(kit.getProductOrderKitId());
        Assert.assertEquals(kit.getMaterialInfo(), dna);
    }

}
