package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.deltaspike.cdise.api.ContextControl;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingEjb;
import org.broadinstitute.gpinformatics.athena.boundary.billing.ConcurrentBaseTest;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserListTest;
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
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProdOrderKitTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class ConcurrentProductOrderDoubleCreateTest extends ConcurrentBaseTest {

    private static final Log logger = LogFactory.getLog(ConcurrentProductOrderDoubleCreateTest.class);

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    ProductOrderEjb productOrderEjb;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    private static Long productOrderId;
    private static String productOrderKey;
    private int numPlaceOrderThreadsRun = 0;
    private static Long basePdoKey;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(DummyJiraService.class);
    }

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {

        if(utx == null) {
            return;
        }

        basePdoKey = (new Date()).getTime();
        utx.begin();

        ProductOrder testOrder = ProductOrderTestFactory.createProductOrder("SM-test2", "SM-243w");
        testOrder.setCreatedBy(1933584925L);
        productOrderEjb.persistProductOrder(ProductOrder.SaveType.CREATING, testOrder, new ArrayList<String>(),
                new ArrayList<ProductOrderKitDetail>());

        productOrderId = testOrder.getProductOrderId();
        productOrderKey = testOrder.getBusinessKey();
        utx.commit();
    }


    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testMultithreaded() throws Exception {
        Throwable pdoJiraError = null;
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
            numErrors++;
        }
        if (pdoLookupThread2.getError() != null) {
            pdoJiraError = pdoLookupThread2.getError();
            numErrors++;
        }

        Assert.assertEquals(numErrors, 0,
                "At least one of the calls to place order called to Jira and created a new ticket when it wasn't expected");
    }

    public class PDOLookupThread implements Runnable {

        private Throwable error;

        @Override
        public void run() {
            ProductOrderEjb threadEjb= null;
            ContextControl ctxCtrl = BeanProvider.getContextualReference(ContextControl.class);
            ctxCtrl.startContext(RequestScoped.class);
            try {
                threadEjb= getBeanFromJNDI(ProductOrderEjb.class);
                numPlaceOrderThreadsRun++;

                MessageCollection messageCollection = new MessageCollection();

                ProductOrder placedProductOrder =
                        threadEjb.placeProductOrder(productOrderKey, productOrderId, messageCollection);

                if(!placedProductOrder.getBusinessKey().equals("PDO-"+basePdoKey)) {
                    throw new RuntimeException("Unexpected PDO key returned");
                }
            }
            catch(RuntimeException e) {
                error = e;
                throw e;
            }
            finally {
                try {
                    ctxCtrl.stopContext(RequestScoped.class);
                }
                catch(Throwable t) {
                    // not much to be done about this!
                    logger.error("Failed to stop context",t);
                }
            }
        }

        public Throwable getError() {
            return error;
        }
    }

    @Alternative
    private static class DummyJiraService implements JiraService {

        @Override
        public JiraIssue createIssue(CreateFields.ProjectType projectType, @Nullable String reporter,
                                     CreateFields.IssueType issueType, String summary,
                                     Collection<CustomField> customFields)
                throws IOException {

            JiraIssue dummyIssue = new JiraIssue("PDO-"+basePdoKey,this);
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
            return null;
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
