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
import javax.transaction.UserTransaction;
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
@Test(groups = TestGroups.EXTERNAL_INTEGRATION,enabled=true)
public class ProductOrderDaoTest extends ContainerTest {

    public static final String TEST_ORDER_TITLE_PREFIX = "TestProductOrder_";
    public static final long TEST_CREATOR_ID = RandomUtils.nextInt(Integer.MAX_VALUE);

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private UserTransaction utx;

    private final String testResearchProjectKey = "TestResearchProject_" + UUID.randomUUID();
    private final static String testProductOrderKeyPrefix = "DRAFT-";

    ProductOrder order;

    private static String getTestProductOrderKey() {
        return testProductOrderKeyPrefix + UUID.randomUUID();
    }

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();

        List<ResearchProject> projectsList = researchProjectDao.findAllResearchProjects();
        if ((projectsList != null) && projectsList.isEmpty()) {
            ResearchProject researchProject =
                    ResearchProjectResourceTest.createDummyResearchProject(testResearchProjectKey);
            researchProjectDao.persist(researchProject);
        }
        order = createTestProductOrder(researchProjectDao, productDao, getTestProductOrderKey());
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
    }

    public static ProductOrder createTestProductOrder(ResearchProjectDao researchProjectDao, ProductDao productDao, String key) {
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
        sampleList.add(new ProductOrderSample("MS-1111"));
        sampleList.add(new ProductOrderSample("MS-1112"));
        newProductOrder.setJiraTicketKey(getTestProductOrderKey());
        return newProductOrder;
    }

    public void testFindOrders() {
        // Try to find the created ProductOrder by its researchProject and title.
        ProductOrder productOrderFromDb =
                productOrderDao.findByResearchProjectAndTitle(order.getResearchProject(), order.getTitle());
        Assert.assertNotNull(productOrderFromDb);
        Assert.assertEquals(productOrderFromDb.getTitle(), order.getTitle());
        Assert.assertEquals(productOrderFromDb.getQuoteId(), order.getQuoteId());
        Assert.assertEquals(productOrderFromDb.getTotalSampleCount(), order.getTotalSampleCount());
        Assert.assertEquals(productOrderFromDb.getSamples().size(), order.getSamples().size());

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
    }

    public void testFindOrdersCreatedBy() {
        List<ProductOrder> orders = productOrderDao.findByCreatedPersonId(TEST_CREATOR_ID);
        Assert.assertNotNull(orders);
        Assert.assertFalse(orders.isEmpty());
    }

    public void testFindOrdersModifiedBy() {
        List<ProductOrder> orders = productOrderDao.findByModifiedPersonId(TEST_CREATOR_ID);
        Assert.assertNotNull(orders);
        Assert.assertFalse(orders.isEmpty());
    }

    public void testFindAll() {
        List<ProductOrder> orders = productOrderDao.findAll();
        Assert.assertNotNull(orders);
        Assert.assertFalse(orders.isEmpty());
    }
}
