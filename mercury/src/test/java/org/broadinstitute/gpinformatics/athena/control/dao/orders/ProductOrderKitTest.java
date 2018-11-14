package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.bsp.client.sample.MaterialInfoDto;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.UserTransaction;


@Test(groups = TestGroups.STUBBY, enabled = true)
@Dependent
public class ProductOrderKitTest extends StubbyContainerTest {

    public ProductOrderKitTest(){}

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
    public static final KitType kitType = KitType.DNA_MATRIX;
    public static final String bspName = "adsfasdf";
    public static final MaterialInfoDto materialInfoDto = new MaterialInfoDto(kitType.getKitName(), bspName);

    @BeforeMethod(groups = TestGroups.STUBBY)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (utx == null) {
            return;
        }

        utx.begin();

        order = ProductOrderDBTestFactory.createTestProductOrder(researchProjectDao, productDao);
        ProductOrderKit productOrderKit = new ProductOrderKit(2L, 4L);
        productOrderKit.addKitOrderDetail(new ProductOrderKitDetail(1L, kitType, 3L,
                materialInfoDto));
        order.setProductOrderKit(productOrderKit);

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

    public void testProductOrderKit() throws Exception {
        ProductOrder pdo = productOrderDao.findByBusinessKey(order.getBusinessKey());
        ProductOrderKit kit = pdo.getProductOrderKit();
        Assert.assertNotNull(kit);
        Assert.assertNotNull(kit.getProductOrderKitId());
        for (ProductOrderKitDetail kitDetail : kit.getKitOrderDetails()) {
            Assert.assertEquals(kitDetail.getMaterialInfo().getBspName(), bspName);
            Assert.assertEquals(kitDetail.getMaterialInfo().getKitType(), kitType.getKitName());
        }
    }

}
