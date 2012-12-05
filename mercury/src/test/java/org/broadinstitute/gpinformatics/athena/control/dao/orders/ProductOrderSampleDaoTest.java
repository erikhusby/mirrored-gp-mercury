package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderTest;
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
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Test(enabled = true)
public class ProductOrderSampleDaoTest  extends ContainerTest {

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    ProductDao productDao;

    @Inject
    ResearchProjectDao researchProjectDao;

    @Inject
    UserTransaction utx;

    private final String testProductOrderKey = "DRAFT-" + UUID.randomUUID();

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

    // FIXME: refactor duplicate code, from here and ProductOrderDaoTest. Create an injectable object that creates
    // test product orders.
    public ProductOrder createTestProductOrder() {
        // Find a research project in the DB.
        List<ResearchProject> projectsList = researchProjectDao.findAllResearchProjects();
        Assert.assertTrue(projectsList != null && !projectsList.isEmpty());
        ResearchProject foundResearchProject = projectsList.get(new Random().nextInt(projectsList.size()));

        Product product = null;
        List<Product> productsList = productDao.findTopLevelProductsForProductOrder();
        if (productsList != null && !productsList.isEmpty()) {
            product = productsList.get(new Random().nextInt(productsList.size()));
        }

        // Try to create a Product Order and persist it.
        String testProductOrderTitle = ProductOrderDaoTest.TEST_ORDER_TITLE_PREFIX + UUID.randomUUID();
        ProductOrder newProductOrder = new ProductOrder(ProductOrderDaoTest.TEST_CREATOR_ID, testProductOrderTitle,
                ProductOrderTest.createSampleList("MS-1111", "MS-1112"), "quoteId",
                product, foundResearchProject);
        newProductOrder.setJiraTicketKey(testProductOrderKey);
        return newProductOrder;
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled=true)
    public void testFindByProductOrder() throws Exception {
        ProductOrder order = createTestProductOrder();

        List<ProductOrderSample> sampleList = ProductOrderTest.createSampleList("MS-1111", "MS-2222", "MS-3333");
        order.setSamples(sampleList);
        productOrderDao.persist(order);
        productOrderDao.flush();
        productOrderDao.clear();

        ProductOrder orderFromDb = productOrderDao.findByBusinessKey(testProductOrderKey);

        // Try to find the list of samples for a given product Order
        List<ProductOrderSample> productOrderSamplesFromDb = orderFromDb.getSamples();
        Assert.assertNotNull(productOrderSamplesFromDb);
        Assert.assertEquals(productOrderSamplesFromDb.size(), sampleList.size());
        // check the sample order, should be the same.
        Assert.assertEquals(productOrderSamplesFromDb.get(0).getSampleName(), "MS-1111");
        Assert.assertEquals(productOrderSamplesFromDb.get(1).getSampleName(), "MS-2222");
        Assert.assertEquals(productOrderSamplesFromDb.get(2).getSampleName(), "MS-3333");
    }
}
