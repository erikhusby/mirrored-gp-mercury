package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.apache.commons.lang.math.RandomUtils;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ResearchProjectResourceTest;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/9/12
 * Time: 3:47 PM
 */
@Test(enabled = false)
public class ProductOrderDaoTest extends ContainerTest {

    public static final String TEST_ORDER_TITLE_PREFIX = "TestProductOrder_";
    public static final long TEST_CREATOR_ID = 1L;

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    ResearchProjectDao researchProjectDao;

    @Inject
    ProductDao productDao;

    private final String testResearchProjectKey = "TestResearchProject_" + UUID.randomUUID();
    private final String testProductOrderKey = "DRAFT-" + UUID.randomUUID();

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        if (researchProjectDao != null) {
            List<ResearchProject> projectsList = researchProjectDao.findAllResearchProjects();
            if ((projectsList != null) && projectsList.isEmpty()) {
                ResearchProject researchProject =
                        ResearchProjectResourceTest.createDummyResearchProject(testResearchProjectKey);
                researchProjectDao.persist(researchProject);
            }
        }
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        if (researchProjectDao == null) {
            // Not running on the server, ignore.
            return;
        }
        ResearchProject researchProject = researchProjectDao.findByBusinessKey(testResearchProjectKey);
        if (researchProject != null) {
            researchProjectDao.remove(researchProject);
            researchProjectDao.flush();
        }

        ProductOrder productOrder = productOrderDao.findByBusinessKey(testProductOrderKey);
        if (productOrder != null) {
            productOrderDao.remove(productOrder);
            productOrderDao.flush();
        }
    }

    public ProductOrder createTestProductOrder() {
        // Find a research project in the DB.
        List<ResearchProject> projectsList = researchProjectDao.findAllResearchProjects();
        Assert.assertTrue(projectsList != null && !projectsList.isEmpty());
        ResearchProject foundResearchProject = projectsList.get(RandomUtils.nextInt(projectsList.size()));

        Product product = null;
        List<Product> productsList = productDao.findProducts();
        if (productsList != null && !productsList.isEmpty()) {
            product = productsList.get(RandomUtils.nextInt(productsList.size()));
        }

        // Try to create a Product Order and persist it.
        List<ProductOrderSample> sampleList = new ArrayList<ProductOrderSample>();
        String testProductOrderTitle = TEST_ORDER_TITLE_PREFIX + UUID.randomUUID();
        ProductOrder newProductOrder = new ProductOrder(TEST_CREATOR_ID, testProductOrderTitle, sampleList, "quoteId",
                product, foundResearchProject);
        sampleList.add(new ProductOrderSample("MS-1111", newProductOrder));
        sampleList.add(new ProductOrderSample("MS-1112", newProductOrder));
        newProductOrder.setJiraTicketKey(testProductOrderKey);
        return newProductOrder;
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = false)
    public void findOrders() {
        ProductOrder order = createTestProductOrder();
        productOrderDao.persist(order);
        productOrderDao.flush();
        productOrderDao.clear();

        // Try to find the created ProductOrder by it's researchProject and title.
        ProductOrder productOrderFromDb =
                productOrderDao.findByResearchProjectAndTitle(order.getResearchProject(), order.getTitle());
        Assert.assertNotNull(productOrderFromDb);
        Assert.assertEquals(productOrderFromDb.getTitle(), order.getTitle());
        Assert.assertEquals(productOrderFromDb.getQuoteId(), order.getQuoteId());
        Assert.assertEquals(productOrderFromDb.getTotalSampleCount(), order.getTotalSampleCount());

        // Try to find a non-existing ProductOrder
        productOrderFromDb = productOrderDao.findByResearchProjectAndTitle(order.getResearchProject(),
                "NonExistingProductOrder_" + UUID.randomUUID());
        Assert.assertNull(productOrderFromDb,
                "Should have thrown exception when trying to retrieve an non-existing product Order.");

        // Try to find an existing ProductOrder by ResearchProject
        List<ProductOrder> orders = productOrderDao.findByResearchProject(order.getResearchProject());
        Assert.assertNotNull(orders);
        if (!orders.isEmpty()) {
            productOrderFromDb = orders.get(0);
        }
        Assert.assertNotNull(productOrderFromDb);

        Assert.assertEquals(productOrderFromDb.getSamples().size(), order.getSamples().size());
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = false)
    public void findOrdersCreatedBy() {
        List<ProductOrder> orders = productOrderDao.findByCreatedPersonId(TEST_CREATOR_ID);
        Assert.assertNotNull(orders);
        Assert.assertFalse(orders.isEmpty());
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = false)
    public void findOrdersModifiedBy() {
        List<ProductOrder> orders = productOrderDao.findByModifiedPersonId(TEST_CREATOR_ID);
        Assert.assertNotNull(orders);
        Assert.assertFalse(orders.isEmpty());
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = false)
    public void findAll() {
        List<ProductOrder> orders = productOrderDao.findAll();
        Assert.assertNotNull(orders);
        Assert.assertFalse(orders.isEmpty());
    }
}