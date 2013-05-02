package org.broadinstitute.gpinformatics.athena.boundary.billing;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.commons.lang.NotImplementedException;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
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
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quotes;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.matchers.SuccessfullyBilled.successfullyBilled;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class BillingEjbJiraDownTest extends Arquillian {

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private BillingEjb billingEjb;


    @Alternative
    private static class HappyQuoteServiceStub implements QuoteService {

        private static int counter = 0;

        @Override
        public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException {
            throw new NotImplementedException();
        }

        @Override
        public Quotes getAllSequencingPlatformQuotes() throws QuoteServerException, QuoteNotFoundException {
            throw new NotImplementedException();
        }

        @Override
        public String registerNewWork(Quote quote, QuotePriceItem quotePriceItem, QuotePriceItem itemIsReplacing,
                                      Date reportedCompletionDate, double numWorkUnits, String callbackUrl,
                                      String callbackParameterName, String callbackParameterValue) {
            return "workItemId\t" + (1000 + counter++);
        }

        @Override
        public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {
            throw new NotImplementedException();
        }
    }

    @Alternative
    private static class AngryJiraStub implements JiraService {

        @Override
        public JiraIssue createIssue(String projectPrefix, String reporter, CreateFields.IssueType issueType,
                                     String summary, String description, Collection<CustomField> customFields)
                throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public void updateIssue(String key, Collection<CustomField> customFields) throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public JiraIssue getIssue(String key) throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public void addComment(String key, String body) throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public void addComment(String key, String body, Visibility.Type visibilityType,
                               Visibility.Value visibilityValue)
                throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public Map<String, CustomFieldDefinition> getRequiredFields(@Nonnull CreateFields.Project project,
                                                                    @Nonnull CreateFields.IssueType issueType)
                throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public String createTicketUrl(String jiraTicketName) {
            throw new NotImplementedException();
        }

        @Override
        public Map<String, CustomFieldDefinition> getCustomFields(String... fieldNames) throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public void addLink(AddIssueLinkRequest.LinkType type, String sourceIssueIn, String targetIssueIn)
                throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public void addLink(AddIssueLinkRequest.LinkType type, String sourceIssueIn, String targetIssueIn,
                            String commentBody, Visibility.Type availabilityType, Visibility.Value availabilityValue)
                throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public void addWatcher(String key, String watcherId) throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public IssueTransitionListResponse findAvailableTransitions(String jiraIssueKey) {
            throw new NotImplementedException();
        }

        @Override
        public Transition findAvailableTransitionByName(String jiraIssueKey, String transitionName) {
            throw new NotImplementedException();
        }

        @Override
        public void postNewTransition(String jiraIssueKey, Transition transition, @Nullable String comment)
                throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public void postNewTransition(String jiraIssueKey, Transition transition,
                                      @Nonnull Collection<CustomField> customFields, @Nullable String comment)
                throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public IssueFieldsResponse getIssueFields(String jiraIssueKey,
                                                  Collection<CustomFieldDefinition> customFieldDefinitions)
                throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public String getResolution(String jiraIssueKey) throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public boolean isValidUser(String username) {
            throw new NotImplementedException();
        }

        @Override
        public JiraIssue getIssueInfo(String key, String... fields) throws IOException {
            throw new NotImplementedException();
        }
    }


    @Deployment
    public static WebArchive buildMercuryDeployment() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(HappyQuoteServiceStub.class, AngryJiraStub.class);
    }


    private BillingSession writeFixtureData() {

        final String SM_A = "SM-1234A";
        final String SM_B = "SM-1234B";
        ProductOrder productOrder = ProductOrderDBTestFactory.createProductOrder(billingSessionDao, SM_A, SM_B);

        Multimap<String, ProductOrderSample> samplesByName = productOrder.groupBySampleId();

        final ProductOrderSample sampleA = samplesByName.get(SM_A).iterator().next();
        final ProductOrderSample sampleB = samplesByName.get(SM_B).iterator().next();

        LedgerEntry ledgerEntryA =
                new LedgerEntry(sampleA, productOrder.getProduct().getPrimaryPriceItem(), new Date(), 3);
        LedgerEntry ledgerEntryB =
                new LedgerEntry(sampleB, productOrder.getProduct().getPrimaryPriceItem(), new Date(), 3);

        BillingSession
                billingSession = new BillingSession(-1L, Sets.newHashSet(ledgerEntryA, ledgerEntryB));
        billingSessionDao.persist(billingSession);

        billingSessionDao.flush();
        billingSessionDao.clear();

        return billingSession;
    }


    public void testPositive() {

        BillingSession billingSession = writeFixtureData();
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        billingEjb.bill("http://www.broadinstitute.org", billingSession.getBusinessKey());

        billingSessionDao.clear();

        // Re-fetch the updated BillingSession from the database.
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        assertThat(billingSession, is(not(nullValue())));

        List<LedgerEntry> ledgerEntryItems = billingSession.getLedgerEntryItems();
        assertThat(ledgerEntryItems, is(not(nullValue())));
        assertThat(ledgerEntryItems, hasSize(2));

        assertThat(ledgerEntryItems, everyItem(is(successfullyBilled())));
    }
}
