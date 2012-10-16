package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderTest;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

/**
 *
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/9/12
 * Time: 3:47 PM
 */
@Test(enabled = true)
public class ProductOrderDaoTest extends ContainerTest {

    public static final String TEST_ORDER_TITLE_PREFIX = "TestProductOrder_";
    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    ResearchProjectDao researchProjectDao;

    private final static String titlePrefix = "TestResearchProject_";
    private final static String testTitle = titlePrefix + UUID.randomUUID();
    private ProductOrder productOrder;
    private Long testResearchProjectId;


    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        if ( productOrderDao != null ) {
            productOrder = ProductOrderTest.createDummyProductOrder();
            productOrderDao.persist(productOrder);
        }
    }


    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        if ( productOrderDao != null ) {
            List<ProductOrder> orders = productOrderDao.findAll();
            for (ProductOrder foundProductOrder : orders) {
                if (foundProductOrder.getTitle().startsWith(TEST_ORDER_TITLE_PREFIX)) {
                    productOrderDao.remove(foundProductOrder);
                }
            }
        }
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void  findOrders() {

        ResearchProject aResearchProject = productOrder.getResearchProject();

        // Try to find the created ProductOrder by it's researchProject and title.
        ProductOrder productOrderFromDb = productOrderDao.findByResearchProjectAndTitle(aResearchProject, productOrder.getTitle());
        Assert.assertNotNull(productOrderFromDb);
        Assert.assertNotNull(productOrderFromDb.getTitle());
        Assert.assertEquals(productOrder.getTitle(), productOrderFromDb.getTitle());
        Assert.assertEquals(productOrder.getQuoteId(), productOrderFromDb.getQuoteId());
        Assert.assertEquals(productOrder.getTotalSampleCount(), productOrderFromDb.getTotalSampleCount() );
        productOrderFromDb = null;

        // Try to find a non-existing ProductOrder
        productOrderFromDb = productOrderDao.findByResearchProjectAndTitle(aResearchProject, "NonExistingProductOrder_" + UUID.randomUUID());
        Assert.assertNull(productOrderFromDb, "Should be null when trying to retrieve an non-existing product Order.");

        // Try to find an existing ProductOrder by ResearchProject
        List<ProductOrder> orders = productOrderDao.findByResearchProject( aResearchProject );
        Assert.assertNotNull(orders);
        if ( orders.size() > 0 ) {
            productOrderFromDb = orders.get(0);
        }
        Assert.assertNotNull(productOrderFromDb);

    }


    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void  findOrdersCreatedBy () {
        List<ProductOrder> ordersCreatedbyList = null;
        ordersCreatedbyList = productOrderDao.findByCreatedPersonId( 1L );
        Assert.assertNotNull(ordersCreatedbyList);
        Assert.assertTrue(ordersCreatedbyList.size() > 0 );
    }

}
