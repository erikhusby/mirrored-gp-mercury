package org.broadinstitute.gpinformatics.athena.boundary.orders;


import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderSampleTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mocks.HappyQuoteServiceMock;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST;
import static org.broadinstitute.gpinformatics.infrastructure.matchers.NullOrEmptyCollection.nullOrEmptyCollection;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.ALTERNATIVES)
@Dependent
public class ProductOrderEjbContainerTest extends Arquillian {

    public ProductOrderEjbContainerTest(){}

    private static final String TEST_PDO = "PDO-312";
    @Inject
    ProductOrderEjb pdoEjb;

    @Inject
    ProductOrderDao pdoDao;

    @Inject
    ProductDao productDao;

    @Inject
    ResearchProjectDao researchProjectDao;

    @Inject
    JiraService jiraService;

    @Inject
    private UserBean userBean;

    @Deployment
    public static WebArchive buildMercuryWar() {
        // it's test because we need a real jira.  And the mock quote server is here because the stub explodes
        // in intellij with "Failed to read quotes from disk", presumably because the arquillian deployment
        // doesn't have the right working directory.
        return DeploymentBuilder.buildMercuryWarWithAlternatives(TEST, HappyQuoteServiceMock.class);
    }

    @Test(groups = TestGroups.ALTERNATIVES)
    public void testNullQuotePropagatesToJira() throws Exception {
        String pdoName = TEST_PDO;
        ProductOrder pdo = pdoDao.findByBusinessKey(pdoName);
        pdo.setQuoteId("CASH");
        pdo.setSkipQuoteReason("Because of GPLIM-2462");

        pdoEjb.updateJiraIssue(pdo);
        String quoteFromJira = getQuoteFieldFromJiraTicket(pdo);

        Assert.assertEquals(quoteFromJira, pdo.getQuoteId());

        pdo.setQuoteId(null);
        pdoEjb.updateJiraIssue(pdo);
        quoteFromJira = getQuoteFieldFromJiraTicket(pdo);

        Assert.assertEquals(quoteFromJira, ProductOrder.QUOTE_TEXT_USED_IN_JIRA_WHEN_QUOTE_FIELD_IS_EMPTY);
    }

    @Test(groups = TestGroups.ALTERNATIVES)
    public void testSummaryFieldPropagatesToJira() throws Exception {
        String pdoName = TEST_PDO;
        ProductOrder pdo = pdoDao.findByBusinessKey(pdoName);
        String newTitle = "And now for something different " + System.currentTimeMillis();
        pdo.setTitle(newTitle);

        pdoEjb.updateJiraIssue(pdo);
        String titleFromJira = getSummaryFieldFromJiraTicket(pdo);

        Assert.assertEquals(titleFromJira, newTitle, "jira summary field is not synchronized with pdo title.");
    }

    /**
     * This test is disabled because of data setup. Once run, the samples it is adding will no longer hit the code
     * this test is testing.
     */
    @Test(enabled=false)
    public void test_Add_Samples_Without_Bound_Mercury_Samples() throws Exception {
        userBean.loginTestUser();
        MessageReporter mockReporter = Mockito.mock(MessageReporter.class);
        String[] sampleNames = {"SM-XADF"};//"SM-XADE", "SM-XADF", "SM-XADG", "SM-XADH", "SM-XADI", "SM-XADJ", "SM-XADK"
        ProductOrder order =
                ProductOrderDBTestFactory.createTestExExProductOrder(researchProjectDao, productDao, sampleNames);

        order.setCreatedBy(userBean.getBspUser().getUserId());
        productDao.persist(order);
        productDao.flush();
        MessageCollection messageCollection = new MessageCollection();

        pdoEjb.placeProductOrder(order.getProductOrderId(), order.getBusinessKey(), messageCollection);
        pdoEjb.removeSamples(order.getJiraTicketKey(), order.getSamples(), MessageReporter.UNUSED);

        List<ProductOrderSample> samples = ProductOrderSampleTestFactory.createSampleList(sampleNames);
        pdoEjb.addSamples(order.getJiraTicketKey(), samples, mockReporter);

        assertThat(order.getSamples(), is(not(empty())));
        for (ProductOrderSample sample : order.getSamples()) {
            assertThat(sample.getMercurySample(), is(not(nullValue())));
            assertThat(sample.getMercurySample().getSampleKey(), is(equalTo(sample.getBusinessKey())));
        }

    }

    @Test
    public void test_Calculate_risk_without_error() throws Exception {
        userBean.loginTestUser();
        MessageReporter mockReporter = Mockito.mock(MessageReporter.class);
        String[] sampleNames = {"SM-XADE", "SM-XADF", "SM-XADG", "SM-XADH", "SM-XADI", "SM-XADJ", "SM-XADK"};

        ResearchProject dummy = researchProjectDao.findByTitle("ADHD");

        List<Product> products = productDao.findList(Product.class, Product_.workflowName, Workflow.AGILENT_EXOME_EXPRESS
                .getWorkflowName());
        assertThat(products, is(not(nullOrEmptyCollection())));
        Product product = products.get(new Random().nextInt(products.size()));

        ProductOrder order =
                ProductOrderDBTestFactory.createTestProductOrder(dummy, product, sampleNames);

        order.setCreatedBy(userBean.getBspUser().getUserId());
        productDao.persist(order);
        productDao.flush();
        MessageCollection messageCollection = new MessageCollection();

        pdoEjb.placeProductOrder(order.getProductOrderId(), order.getBusinessKey(), messageCollection);

        pdoEjb.calculateRisk(order.getBusinessKey());
    }


    /**
     * Gets the text of the quote field from
     * the jira ticket that corresponds to the given pdo.
     */
    private String getQuoteFieldFromJiraTicket(ProductOrder pdo) throws IOException {
        return (String) jiraService.getIssue(pdo.getBusinessKey()).getField(ProductOrder.JiraField.QUOTE_ID.getName());
    }

    private String getSummaryFieldFromJiraTicket(ProductOrder pdo) throws IOException {
        return (String) jiraService.getIssue(pdo.getBusinessKey()).getField(ProductOrder.JiraField.SUMMARY.getName());
    }
}
