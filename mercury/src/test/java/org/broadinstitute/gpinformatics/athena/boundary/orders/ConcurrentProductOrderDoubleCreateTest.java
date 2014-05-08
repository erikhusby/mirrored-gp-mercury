package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.deltaspike.cdise.api.ContextControl;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.boundary.billing.ConcurrentBaseTest;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraCustomFieldsUtil;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.IssueFieldsResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.IssueTransitionListResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.Transition;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ConcurrentProductOrderDoubleCreateTest extends ConcurrentBaseTest {

    private static final Log logger = LogFactory.getLog(ConcurrentProductOrderDoubleCreateTest.class);

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    ProductOrderEjb productOrderEjb;

    @Inject
    UserBean userBean;

    private Long productOrderId;
    private String productOrderKey;
    private int numPlaceOrderThreadsRun = 0;
    private static Long basePdoKey;
    private String startingKey;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(ControlBusinessKeyJiraService.class);
    }

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {

        if (productOrderEjb == null) {
            return;
        }

        userBean.loginTestUser();

        basePdoKey = (new Date()).getTime();

        ProductOrder testOrder = ProductOrderTestFactory.createProductOrder("SM-test2", "SM-243w");
        testOrder.setQuoteId("MMM4AM");
        testOrder.setCreatedBy(BSPManagerFactoryStub.QA_DUDE_USER_ID);
        testOrder.setJiraTicketKey("");
        testOrder.setOrderStatus(ProductOrder.OrderStatus.Draft);
        productOrderEjb.persistProductOrder(ProductOrder.SaveType.CREATING, testOrder, new ArrayList<String>(),
                new ArrayList<ProductOrderKitDetail>());

        productOrderId = testOrder.getProductOrderId();
        productOrderKey = testOrder.getBusinessKey();
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testMultithreaded() throws Exception {
        Throwable pdoJiraError = null;
        startingKey = "PDO-" + basePdoKey;
        PDOLookupThread pdoLookupThread = new PDOLookupThread();
        PDOLookupThread pdoLookupThread2 = new PDOLookupThread();
        Thread thread1 = new Thread(pdoLookupThread);
        Thread thread2 = new Thread(pdoLookupThread2);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        int numErrors = 0;
        if (pdoLookupThread.getError() != null) {
            pdoJiraError = pdoLookupThread.getError();
            Assert.assertTrue(pdoJiraError instanceof InformaticsServiceException);
            logger.info("Error found in Thread 1: " + pdoJiraError.getMessage());
            numErrors++;
        }
        if (pdoLookupThread2.getError() != null) {
            pdoJiraError = pdoLookupThread2.getError();
            Assert.assertTrue(pdoJiraError instanceof InformaticsServiceException);
            logger.info("Error found in Thread 2: " + pdoJiraError.getMessage());
            numErrors++;
        }

        logger.info("Finding PDO for order id: " + productOrderId);


        Assert.assertEquals(numErrors, 1,
                "At least one of the calls to place order called to Jira and created a new ticket when it wasn't expected");

        // Removing these lines (For now) because it seems that a query for the Product order does not yield the
        // updated product order state.  Maybe because this test case is wrapped in a transaction that it is not able
        // to see the change within the thread

        productOrderDao.clear();
        ProductOrder alteredOrder = productOrderDao.findById(productOrderId);
        Assert.assertEquals(alteredOrder.getBusinessKey(), startingKey,
                "The Product order key was reset by a Second thread after being submitted.");
    }

    public class PDOLookupThread implements Runnable {

        private Throwable error;

        @Override
        public void run() {
            ProductOrderEjb threadEjb = null;
            ContextControl ctxCtrl = BeanProvider.getContextualReference(ContextControl.class);
            ctxCtrl.startContext(RequestScoped.class);
            ctxCtrl.startContext(SessionScoped.class);
            try {
                threadEjb = getBeanFromJNDI(ProductOrderEjb.class);
                int threadVal = numPlaceOrderThreadsRun++;

                MessageCollection messageCollection = new MessageCollection();

                logger.info("Thread " + threadVal + ": coming in, jira reference is: " + productOrderKey);

                userBean.loginTestUser();

                ProductOrder placedProductOrder =
                        threadEjb.placeProductOrder(productOrderId, productOrderKey, messageCollection);

                logger.info("Thread " + threadVal + ": coming out, jira reference is: " +
                            placedProductOrder.getBusinessKey());
                if (!placedProductOrder.getBusinessKey().equals(startingKey)) {
                    throw new RuntimeException("Unexpected PDO key returned");
                }
            } catch (RuntimeException e) {
                logger.info(e.getMessage());
                error = e;
                throw e;
            } finally {
                try {
                    ctxCtrl.stopContext(SessionScoped.class);
                    ctxCtrl.stopContext(RequestScoped.class);
                } catch (Throwable t) {
                    // not much to be done about this!
                    logger.error("Failed to stop context", t);
                }
            }
        }

        public Throwable getError() {
            return error;
        }
    }

    @Alternative
    public static class ControlBusinessKeyJiraService implements JiraService {

        @Override
        public JiraIssue createIssue(CreateFields.ProjectType projectType, @Nullable String reporter,
                                     CreateFields.IssueType issueType, String summary,
                                     Collection<CustomField> customFields)
                throws IOException {

            JiraIssue dummyIssue = new JiraIssue("PDO-" + basePdoKey, this);
            basePdoKey++;
            return dummyIssue;
        }

        @Override
        public void updateIssue(String key, Collection<CustomField> customFields) throws IOException {

        }

        @Override
        public JiraIssue getIssue(String key) throws IOException {
            return null;
        }

        @Override
        public void addComment(String key, String body) throws IOException {

        }

        @Override
        public void addComment(String key, String body, Visibility.Type visibilityType,
                               Visibility.Value visibilityValue)
                throws IOException {

        }

        @Override
        public Map<String, CustomFieldDefinition> getRequiredFields(@Nonnull CreateFields.Project project,
                                                                    @Nonnull CreateFields.IssueType issueType)
                throws IOException {
            return null;
        }

        @Override
        public String createTicketUrl(String jiraTicketName) {
            return null;
        }

        @Override
        public Map<String, CustomFieldDefinition> getCustomFields(String... fieldNames) throws IOException {
            Map<String, CustomFieldDefinition> customFields = new HashMap<>();
            for (String requiredFieldName : JiraCustomFieldsUtil.REQUIRED_FIELD_NAMES) {
                customFields.put(requiredFieldName, new CustomFieldDefinition("stub_custom_field_" + requiredFieldName,
                        requiredFieldName, true));
            }
            return customFields;
        }

        @Override
        public void addLink(AddIssueLinkRequest.LinkType type, String sourceIssueIn, String targetIssueIn)
                throws IOException {

        }

        @Override
        public void addLink(AddIssueLinkRequest.LinkType type, String sourceIssueIn, String targetIssueIn,
                            String commentBody, Visibility.Type availabilityType, Visibility.Value availabilityValue)
                throws IOException {

        }

        @Override
        public void addWatcher(String key, String watcherId) throws IOException {

        }

        @Override
        public IssueTransitionListResponse findAvailableTransitions(String jiraIssueKey) {
            return null;
        }

        @Override
        public Transition findAvailableTransitionByName(String jiraIssueKey, String transitionName) {
            return null;
        }

        @Override
        public void postNewTransition(String jiraIssueKey, Transition transition, @Nullable String comment)
                throws IOException {

        }

        @Override
        public void postNewTransition(String jiraIssueKey, Transition transition,
                                      @Nonnull Collection<CustomField> customFields, @Nullable String comment)
                throws IOException {

        }

        @Override
        public IssueFieldsResponse getIssueFields(String jiraIssueKey,
                                                  Collection<CustomFieldDefinition> customFieldDefinitions)
                throws IOException {
            return null;
        }

        @Override
        public String getResolution(String jiraIssueKey) throws IOException {
            return null;
        }

        @Override
        public boolean isValidUser(String username) {
            return false;
        }

        @Override
        public JiraIssue getIssueInfo(String key, String... fields) throws IOException {
            return null;
        }
    }

}
