package org.broadinstitute.gpinformatics.infrastructure.test.withdb;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderSampleTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.broadinstitute.gpinformatics.infrastructure.matchers.NullOrEmptyCollection.nullOrEmptyCollection;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

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
        long time = new Date().getTime();
        RegulatoryInfo regulatoryInfo = new RegulatoryInfo("IRB Consent for - " + time
                , RegulatoryInfo.Type.IRB, ""+time);
                project.getRegulatoryInfos().add(regulatoryInfo);
        regulatoryInfo = new RegulatoryInfo("Non-Human Subjects Research for - " + time
                , RegulatoryInfo.Type.ORSP_NOT_HUMAN_SUBJECTS_RESEARCH, ""+time);
                project.getRegulatoryInfos().add(regulatoryInfo);

        List<Product> products = productDao.findList(Product.class, Product_.workflowName,
                Workflow.AGILENT_EXOME_EXPRESS);
        assertThat(products, is(not(nullOrEmptyCollection())));
        Product product = products.get(new Random().nextInt(products.size()));

        // Try to create a Product Order and persist it.
        return createTestProductOrder(project, product, sampleNames);
    }

    public static ProductOrder createTestProductOrder(String... sampleNames) {
        return createTestProductOrder(ResearchProjectTestFactory.createTestResearchProject(),
                ProductTestFactory.createTestProduct(), sampleNames);
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

        return createTestProductOrder(project, product, sampleNames);
    }

    public static ProductOrder createTestProductOrder(ResearchProject project, Product product, String... sampleNames) {
        // Try to create a Product Order and persist it.
        String testProductOrderTitle = TEST_ORDER_TITLE_PREFIX + UUID.randomUUID();
        ProductOrder order =
                new ProductOrder(TEST_CREATOR_ID, testProductOrderTitle, ProductOrderSampleTestFactory
                        .createSampleList(sampleNames),
                        "quoteId", product, project);

        order.setJiraTicketKey(TEST_PRODUCT_ORDER_KEY_PREFIX + UUID.randomUUID());
        BspUser testUser = new BspUser();
        testUser.setUserId(TEST_CREATOR_ID);
        order.prepareToSave(testUser, ProductOrder.SaveType.CREATING);

        return order;
    }


    /**
     * Creates a {@link ProductOrder} with the specified sample names and persists everything using the
     * dao parameter.
     */
    public static ProductOrder createProductOrder(GenericDao dao, ProductOrder.QuoteSourceType quoteSource, String... sampleNames) {
        ProductOrder productOrder = ProductOrderTestFactory.createProductOrder(sampleNames);
//        productOrder.setQuoteSource(quoteSource);
        dao.persist(productOrder.getResearchProject());
        dao.persist(productOrder.getProduct());
        dao.persist(productOrder);
        return productOrder;
    }

    public static ProductOrder createProductOrder(GenericDao dao, String... sampleNames) {
        return createProductOrder(dao, ProductOrder.QuoteSourceType.QUOTE_SERVER, sampleNames);
    }
}
