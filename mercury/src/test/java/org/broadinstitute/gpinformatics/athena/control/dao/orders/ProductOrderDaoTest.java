package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.apache.commons.lang3.time.DateUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderTest;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.text.MessageFormat;
import java.util.*;


@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class ProductOrderDaoTest extends ContainerTest {

    public static final String TEST_ORDER_TITLE_PREFIX = "TestProductOrder_";
    public static final long TEST_CREATOR_ID = new Random().nextInt(Integer.MAX_VALUE);
    public static final String MS_1111 = "MS-1111";
    public static final String MS_1112 = "MS-1112";

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductDao productDao;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    private static final String TEST_PRODUCT_ORDER_KEY_PREFIX = "DRAFT-";

    ProductOrder order;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (utx == null) {
            return;
        }

        utx.begin();

        order = createTestProductOrder(researchProjectDao, productDao);
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

    public static ProductOrder createTestProductOrder(ResearchProjectDao researchProjectDao, ProductDao productDao) {
        return createTestProductOrder(researchProjectDao, productDao, MS_1111, MS_1112);
    }

    public static ProductOrder createTestProductOrder(ResearchProjectDao researchProjectDao, ProductDao productDao,
                                                      String... sampleNames) {
        // Find a research project in the DB.
        List<ResearchProject> projects = researchProjectDao.findAllResearchProjects();
        Assert.assertTrue(projects != null && !projects.isEmpty());
        ResearchProject project = projects.get(new Random().nextInt(projects.size()));

        List<Product> products = productDao.findTopLevelProductsForProductOrder();
        Assert.assertTrue(products != null && !products.isEmpty());
        Product product = products.get(new Random().nextInt(products.size()));

        // Try to create a Product Order and persist it.
        String testProductOrderTitle = TEST_ORDER_TITLE_PREFIX + UUID.randomUUID();
        ProductOrder order =
                new ProductOrder(TEST_CREATOR_ID, testProductOrderTitle, ProductOrderTest.createSampleList(sampleNames),
                                        "quoteId", product, project);

        order.setJiraTicketKey(TEST_PRODUCT_ORDER_KEY_PREFIX + UUID.randomUUID());
        BspUser testUser = new BspUser();
        testUser.setUserId(TEST_CREATOR_ID);
        order.prepareToSave(testUser, true);

        return order;
    }

    public static ProductOrder createTestExExProductOrder(ResearchProjectDao researchProjectDao, ProductDao productDao,
                                                          String... sampleNames) {
        // Find a research project in the DB.
        List<ResearchProject> projects = researchProjectDao.findAllResearchProjects();
        Assert.assertTrue(projects != null && !projects.isEmpty());
        ResearchProject project = projects.get(new Random().nextInt(projects.size()));

        List<Product> products = productDao.findList(Product.class, Product_.workflowName,
                                                            WorkflowName.EXOME_EXPRESS.getWorkflowName());
        Assert.assertTrue(products != null && !products.isEmpty());
        Product product = products.get(new Random().nextInt(products.size()));

        // Try to create a Product Order and persist it.
        String testProductOrderTitle = TEST_ORDER_TITLE_PREFIX + UUID.randomUUID();
        ProductOrder order =
                new ProductOrder(TEST_CREATOR_ID, testProductOrderTitle, ProductOrderTest.createSampleList(sampleNames),
                                        "quoteId", product, project);

        order.setJiraTicketKey(TEST_PRODUCT_ORDER_KEY_PREFIX + UUID.randomUUID());
        BspUser testUser = new BspUser();
        testUser.setUserId(TEST_CREATOR_ID);
        order.prepareToSave(testUser, true);

        return order;
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

        // Try to find a non-existing ProductOrder.
        productOrderFromDb = productOrderDao.findByResearchProjectAndTitle(order.getResearchProject(),
                                                                                  "NonExistingProductOrder_" + UUID.randomUUID());
        Assert.assertNull(productOrderFromDb,
                                 "Should have thrown exception when trying to retrieve an non-existing product Order.");

        // Try to find an existing ProductOrder by ResearchProject.
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

    public void testFindModifiedAfter() {
        Date date = new Date();
        // Yesterday
        date.setTime(date.getTime() - DateUtils.MILLIS_PER_DAY);
        List<ProductOrder> orders = productOrderDao.findModifiedAfter(date);
        Assert.assertFalse(orders.isEmpty());

        // Tomorrow
        date.setTime(new Date().getTime() + DateUtils.MILLIS_PER_DAY);
        orders = productOrderDao.findModifiedAfter(date);
        Assert.assertTrue(orders.isEmpty());
    }

    public void testFindAll() {
        List<ProductOrder> orders = productOrderDao.findAll();
        Assert.assertNotNull(orders);
        Assert.assertFalse(orders.isEmpty());
    }

    public void testFindByWorkflow() {
        ProductOrder testOrder = createTestExExProductOrder(researchProjectDao, productDao);

        productOrderDao.persist(testOrder);
        productOrderDao.flush();
        productOrderDao.clear();

        Collection<ProductOrder> orders =
                productOrderDao.findByWorkflowName(WorkflowName.EXOME_EXPRESS.getWorkflowName());

        Assert.assertFalse(orders.isEmpty());
    }


    /**
     * Helper method for {@link #testFindBySampleBarcodes} test method.
     *
     * @param productOrderMap Input map of PDO barcodes to PDOs.
     * @param productOrderKey PDO to search.
     * @param sampleBarcode Sample barcode we expect to find in this PDO.
     */
    private void assertContains(Map<String, ProductOrder> productOrderMap, String productOrderKey, String sampleBarcode) {
        Assert.assertNotNull(productOrderMap);
        Assert.assertTrue(productOrderMap.containsKey(productOrderKey));

        ProductOrder productOrder = productOrderMap.get(productOrderKey);
        for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
            if (productOrderSample.getSampleName().equals(sampleBarcode)) {
                return;
            }
        }
        Assert.fail(MessageFormat.format("Sample {0} not found in {1}", sampleBarcode, productOrderKey));
    }


    /**
     * Ugly positive test method for finding {@link ProductOrder}s by sample barcode, uses real data.
     */
    public void testFindBySampleBarcodes() {

        List<ProductOrder> productOrders = productOrderDao.findBySampleBarcodes("NTC", "SC_9001");
        Assert.assertNotNull(productOrders);
        Assert.assertEquals(productOrders.size(), 3);

        Map<String, ProductOrder> productOrderMap = new HashMap<String, ProductOrder>();
        for (ProductOrder productOrder : productOrders) {
            productOrderMap.put(productOrder.getBusinessKey(), productOrder);
        }

        assertContains(productOrderMap, "PDO-359", "NTC");
        assertContains(productOrderMap, "PDO-532", "NTC");
        assertContains(productOrderMap, "PDO-373", "SC_9001");
    }

}
