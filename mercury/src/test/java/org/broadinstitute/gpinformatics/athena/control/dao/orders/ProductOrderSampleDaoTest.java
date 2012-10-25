package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.apache.commons.lang.math.RandomUtils;
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
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * User: mccrory
 * Date: 10/10/12
 * Time: 2:20 PM
 */
@Test(enabled = true)
public class ProductOrderSampleDaoTest  extends ContainerTest {

    @Inject
    ProductOrderSampleDao productOrderSampleDao;

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    ProductDao productDao;

    @Inject
    ResearchProjectDao researchProjectDao;

    private final String testProductOrderKey = "DRAFT-" + UUID.randomUUID();

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        if (productOrderDao == null || testProductOrderKey == null) {
            // Not running on the server, ignore.
            return;
        }
        ProductOrder productOrder = productOrderDao.findByBusinessKey(testProductOrderKey);
        if (productOrder != null) {
            productOrderDao.remove(productOrder);
            productOrderDao.flush();
        }
    }

    // FIXME: refactor duplicate code, from here and ProductOrderDaoTest. Create an injectable object that creates
    // test product orders.
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
        String testProductOrderTitle = ProductOrderDaoTest.TEST_ORDER_TITLE_PREFIX + UUID.randomUUID();
        ProductOrder newProductOrder = new ProductOrder(ProductOrderDaoTest.TEST_CREATOR_ID, testProductOrderTitle, sampleList, "quoteId",
                product, foundResearchProject);
        sampleList.add(new ProductOrderSample("MS-1111", newProductOrder));
        sampleList.add(new ProductOrderSample("MS-1112", newProductOrder));
        newProductOrder.setJiraTicketKey(testProductOrderKey);
        return newProductOrder;
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testFindByProductOrder() throws Exception {
        ProductOrder order = createTestProductOrder();

        List<ProductOrderSample> sampleList = new ArrayList<ProductOrderSample>();
        sampleList.add(new ProductOrderSample("MS-1111", order));
        sampleList.add(new ProductOrderSample("MS-2222", order));
        sampleList.add(new ProductOrderSample("MS-3333", order));
        order.setSamples(sampleList);
        productOrderDao.persist(order);
        productOrderDao.flush();
        productOrderDao.clear();

        // Try to find the list of samples for a given product Order
        List<ProductOrderSample> productOrderSamplesFromDb = productOrderSampleDao.findByProductOrder(order);
        Assert.assertNotNull(productOrderSamplesFromDb);
        Assert.assertEquals(productOrderSamplesFromDb.size(), sampleList.size());
    }
}
