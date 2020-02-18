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
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraUser;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.IssueTransitionListResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.Transition;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Test(groups = TestGroups.ALTERNATIVES)
@Dependent
public class ConcurrentProductOrderDoubleCreateTest extends ConcurrentBaseTest {

    public ConcurrentProductOrderDoubleCreateTest(){}

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

    @BeforeMethod(groups = TestGroups.ALTERNATIVES)
    public void setUp() throws Exception {

        if (productOrderEjb == null) {
            return;
        }

        userBean.loginTestUser();

        basePdoKey = (new Date()).getTime();

        ProductOrder testOrder = ProductOrderTestFactory.createProductOrder("SM-"+basePdoKey, "SM-"+basePdoKey+1);
        testOrder.setQuoteId("MMM4AM");
        testOrder.setCreatedBy(BSPManagerFactoryStub.QA_DUDE_USER_ID);
        testOrder.setJiraTicketKey("");
        testOrder.setOrderStatus(ProductOrder.OrderStatus.Draft);
        productOrderEjb.persistProductOrder(ProductOrder.SaveType.CREATING, testOrder, new ArrayList<String>(),
                new ArrayList<ProductOrderKitDetail>());

        productOrderId = testOrder.getProductOrderId();
        productOrderKey = testOrder.getBusinessKey();
    }

    @Test(groups = TestGroups.ALTERNATIVES)
    public void testMultithreaded() throws Exception {
        Throwable pdoJiraError = null;
        startingKey = "PDO-" + basePdoKey;
        PlacePDOThread placePdoThread = new PlacePDOThread();
        PlacePDOThread placePdoThread2 = new PlacePDOThread();
        Thread thread1 = new Thread(placePdoThread);
        Thread thread2 = new Thread(placePdoThread2);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        int numErrors = 0;
        if (placePdoThread.getError() != null) {
            pdoJiraError = placePdoThread.getError();
            logger.info("Error found in Thread 1: " + pdoJiraError.getMessage());
            numErrors++;
        }
        if (placePdoThread2.getError() != null) {
            pdoJiraError = placePdoThread2.getError();
            logger.info("Error found in Thread 2: " + pdoJiraError.getMessage());
            numErrors++;
        }

        logger.info("Finding PDO for order id: " + productOrderId);


        Assert.assertEquals(numErrors, 1,
                "One of two concurrent calls to place an order should have been rejected.");

        productOrderDao.clear();
        ProductOrder alteredOrder = productOrderDao.findById(productOrderId);
        Assert.assertEquals(alteredOrder.getBusinessKey(), startingKey,
                "The Product order key should not have been reset by a second thread after being submitted.");
    }

    public class PlacePDOThread implements Runnable {

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


    @Dependent
    @Alternative
    public static class ControlBusinessKeyJiraService implements JiraService {

        public ControlBusinessKeyJiraService(){}

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
        public void updateAssignee(String key, String name) {
        }


        @Override
        public JiraIssue getIssue(String key) throws IOException {
            // This mock object is required to support the initial transition of a PDO from Submitted to Open.
            return new JiraIssue(key, this) {
                @Override
                public Object getField(String fieldName) throws IOException {
                    Assert.assertEquals(fieldName, ProductOrder.JiraField.STATUS.getName());
                    return Collections.singletonMap("name", ProductOrderEjb.JiraStatus.OPEN.name());
                }
            };
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
        public void deleteLink(String jiraIssueLinkId) throws IOException {

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

        @Override
        public List<JiraUser> getJiraUsers(String key) {
            return Collections.emptyList();
        }
    }

}
