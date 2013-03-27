package org.broadinstitute.gpinformatics.infrastructure.test.withdb;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderSampleTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.broadinstitute.gpinformatics.infrastructure.matchers.NullOrEmptyCollection.nullOrEmptyCollection;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ProductOrderDBTestFactory {
    public static final String TEST_ORDER_TITLE_PREFIX = "TestProductOrder_";
    public static final long TEST_CREATOR_ID = new Random().nextInt(Integer.MAX_VALUE);
    public static final String MS_1111 = "MS-1111";
    public static final String MS_1112 = "MS-1112";
    private static final String TEST_PRODUCT_ORDER_KEY_PREFIX = "DRAFT-";

    public static ProductOrder createTestExExProductOrder(ResearchProjectDao researchProjectDao, ProductDao productDao,
                                                          String... sampleNames) {
        // Find a research project in the DB.
        List<ResearchProject> projects = researchProjectDao.findAllResearchProjects();
        assertThat(projects, is(not(nullOrEmptyCollection())));
        ResearchProject project = projects.get(new Random().nextInt(projects.size()));

        List<Product> products = productDao.findList(Product.class, Product_.workflowName,
                WorkflowName.EXOME_EXPRESS.getWorkflowName());
        assertThat(products, is(not(nullOrEmptyCollection())));
        Product product = products.get(new Random().nextInt(products.size()));

        // Try to create a Product Order and persist it.
        String testProductOrderTitle = TEST_ORDER_TITLE_PREFIX + UUID.randomUUID();
        ProductOrder order =
                new ProductOrder(TEST_CREATOR_ID, testProductOrderTitle, ProductOrderSampleTestFactory
                        .createSampleList(sampleNames),
                                        "quoteId", product, project);

        order.setJiraTicketKey(TEST_PRODUCT_ORDER_KEY_PREFIX + UUID.randomUUID());
        BspUser testUser = new BspUser();
        testUser.setUserId(TEST_CREATOR_ID);
        order.prepareToSave(testUser, true);

        return order;
    }

    public static ProductOrder createTestProductOrder(ResearchProjectDao researchProjectDao, ProductDao productDao) {
        return createTestProductOrder(researchProjectDao, productDao, MS_1111, MS_1112);
    }

    public static ProductOrder createTestProductOrder(ResearchProjectDao researchProjectDao, ProductDao productDao,
                                                      String... sampleNames) {
        // Find a research project in the DB.
        List<ResearchProject> projects = researchProjectDao.findAllResearchProjects();
        assertThat(projects, is(not(nullOrEmptyCollection())));
        ResearchProject project = projects.get(new Random().nextInt(projects.size()));

        List<Product> products = productDao.findTopLevelProductsForProductOrder();
        assertThat(products, is(not(nullOrEmptyCollection())));
        Product product = products.get(new Random().nextInt(products.size()));

        // Try to create a Product Order and persist it.
        String testProductOrderTitle = TEST_ORDER_TITLE_PREFIX + UUID.randomUUID();
        ProductOrder order =
                new ProductOrder(TEST_CREATOR_ID, testProductOrderTitle, ProductOrderSampleTestFactory
                        .createSampleList(sampleNames),
                                        "quoteId", product, project);

        order.setJiraTicketKey(TEST_PRODUCT_ORDER_KEY_PREFIX + UUID.randomUUID());
        BspUser testUser = new BspUser();
        testUser.setUserId(TEST_CREATOR_ID);
        order.prepareToSave(testUser, true);

        return order;
    }
}
