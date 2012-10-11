package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/9/12
 * Time: 3:47 PM
 */
@Test
public class ProductOrderDaoTest extends ContainerTest {


    public static final String TEST_ORDER_TITLE_PREFIX = "TestProductOrder_";
    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    ResearchProjectDao researchProjectDao;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void  findOrders() {

        // Find a research project in the DB
        ResearchProject firstFoundResearchProject = null;
        List<ResearchProject> projectsList = researchProjectDao.findAllResearchProjects();
        if ((projectsList != null) && (projectsList.size() > 0 )) {
            firstFoundResearchProject = projectsList.get(0);
        } else {
            Assert.fail("Could not find any research Projects in the Database. Need to create an ResearchProject in the DB.");
        }

        // Try to create a Product Order and persist it.
        String testProductOrderTitle = TEST_ORDER_TITLE_PREFIX + UUID.randomUUID();
        //TODO hmc When there are products in the DB can then persist the Product wit the order.
        List<ProductOrderSample> sampleList = new ArrayList<ProductOrderSample>();
        sampleList.add(new ProductOrderSample("MS-1111"));
        sampleList.add(new ProductOrderSample("MS-1112"));
        ProductOrder newProductOrder = new ProductOrder(testProductOrderTitle, sampleList, "quoteId", null, firstFoundResearchProject );
        productOrderDao.persist(newProductOrder);
        productOrderDao.flush();
        productOrderDao.clear();

        // Try to find the created ProductOrder by it's researchProject and title.
        ProductOrder productOrderFromDb = productOrderDao.findByResearchProjectAndTitle(firstFoundResearchProject, testProductOrderTitle);
        Assert.assertNotNull(productOrderFromDb);
        Assert.assertNotNull(productOrderFromDb.getTitle());
        Assert.assertEquals(testProductOrderTitle, productOrderFromDb.getTitle());
        Assert.assertEquals("quoteId", productOrderFromDb.getQuoteId());
        Assert.assertEquals(0, productOrderFromDb.getTotalSampleCount() );
        productOrderFromDb = null;

        // Try to find a non-existing ProductOrder
        try {
            productOrderFromDb = productOrderDao.findByResearchProjectAndTitle(firstFoundResearchProject, "NonExistingProductOrder_" + UUID.randomUUID());
            Assert.fail("Should have thrown exception when trying to retrieve an non-existing product Order.");
        }
        catch (EJBException ejbx) {
            Assert.assertTrue(NoResultException.class.isAssignableFrom(ejbx.getCause().getClass()));
            Assert.assertNull(productOrderFromDb);
        }
        productOrderFromDb = null;

        System.out.println(" RP ID is " + firstFoundResearchProject.getResearchProjectId()  + " title is " + firstFoundResearchProject.getTitle());

        // Try to find an existing ProductOrder by ResearchProject
        List<ProductOrder> orders = productOrderDao.findByResearchProject( firstFoundResearchProject );
        Assert.assertNotNull(orders);
        if ( orders.size() > 0 ) {
            productOrderFromDb = orders.get(0);
        }
        Assert.assertNotNull(productOrderFromDb);
        productOrderFromDb = null;
        orders = null;

        // Delete all created Product Orders
        // TODO hmc commented out for now.
//        orders = productOrderDao.findAllOrders();
//        for (ProductOrder foundProductOrder : orders) {
//            if (foundProductOrder.getTitle().startsWith(TEST_ORDER_TITLE_PREFIX)) {
//                productOrderDao.delete(foundProductOrder);
//            }
//        }
    }

}
