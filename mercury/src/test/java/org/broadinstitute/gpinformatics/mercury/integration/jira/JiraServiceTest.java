package org.broadinstitute.gpinformatics.mercury.integration.jira;


import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ResearchProjectEjb;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceTestProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraUser;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.NoJiraTransitionException;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.Transition;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

@Test(groups = EXTERNAL_INTEGRATION, singleThreaded = true)
public class JiraServiceTest {
    final String TEST_PDO_ISSUE = "PDO-8";
    final String TEST_LCSET_ISSUE = "LCSET-1678";

    private JiraService service;

    @BeforeMethod
    public void setUp() {
        service = JiraServiceTestProducer.testInstance();
    }

    /**
     * Disabled this because had to change createIssue to pass the Reporter field. We should allow null for jira types
     * that do not expose the reporter, so change the API to do that later.
     */
    @Test
    public void testCreation() throws IOException {

        Map<String, CustomFieldDefinition> requiredFields =
                service.getRequiredFields(new CreateFields.Project(CreateFields.ProjectType.LCSET_PROJECT),
                        CreateFields.IssueType.WHOLE_EXOME_HYBSEL);

        Collection<CustomField> customFieldList = new LinkedList<>();

        customFieldList.add(new CustomField(requiredFields.get("Protocol"), "test protocol"));
        customFieldList.add(new CustomField(requiredFields.get("Work Request ID(s)"), "WR 1 Billion!"));
        customFieldList.add(new CustomField((requiredFields.get("Description")), "Description created from Mercury"));

        JiraIssue jiraIssue =
                service.createIssue(CreateFields.ProjectType.LCSET_PROJECT, null,
                        CreateFields.IssueType.WHOLE_EXOME_HYBSEL,
                        "Summary created from Mercury",
                        customFieldList);


        Assert.assertNotNull(jiraIssue.getKey());
    }

    public void testFindUser() {
        List<JiraUser> jiraUsers = service.getJiraUsers("qadude");
        boolean foundUser=false;

        assertThat(jiraUsers.size(), is(greaterThan(1)));
        String displayName = "QADude PDM";
        for (JiraUser jiraUser : jiraUsers) {
            if (jiraUser.getDisplayName().equals(displayName)) {
                foundUser=true;
                break;
            }
        }

        assertThat(String.format("Could not find %s", displayName), foundUser, is(true));
    }

    public void testFindUserNoSuchUser() {
        List<JiraUser> jiraUsers = service.getJiraUsers("noperdoodles!");
        assertThat(jiraUsers, emptyCollectionOf(JiraUser.class));
    }

    public void testFindUserNullUser() {
        List<JiraUser> jiraUsers = service.getJiraUsers(null);
        assertThat(jiraUsers, emptyCollectionOf(JiraUser.class));
    }

    public void testCreatePdoTicket() throws IOException {
        Collection<CustomField> customFieldList = new LinkedList<>();

        CreateFields.ProjectType productOrdering = CreateFields.ProjectType.PRODUCT_ORDERING;

        CreateFields.IssueType productOrder = CreateFields.IssueType.PRODUCT_ORDER;

        Map<String, CustomFieldDefinition> requiredFields =
                service.getRequiredFields(new CreateFields.Project(productOrdering),productOrder);

        Assert.assertTrue(requiredFields.keySet().contains(ProductOrder.JiraField.PRODUCT_FAMILY.getName()));


        customFieldList
                .add(new CustomField(requiredFields.get(ProductOrder.JiraField.PRODUCT_FAMILY.getName()),
                        "Test Exome Express"));
        customFieldList.add(new CustomField(requiredFields.get("Description"),
                "Athena Test Case:  Test description setting"));

        JiraIssue jiraIssue =
                service.createIssue(productOrdering, "bspuser", productOrder,
                        "Athena Test case:::  Test new Summary Addition", customFieldList);

        Assert.assertNotNull(jiraIssue.getKey());
    }

    public void testUpdateTicket() throws IOException {
        Map<String, CustomFieldDefinition> requiredFields =
                service.getRequiredFields(
                        new CreateFields.Project(
                                CreateFields.ProjectType.PRODUCT_ORDERING),
                        CreateFields.IssueType.PRODUCT_ORDER);
        Collection<CustomField> customFieldList = new LinkedList<>();
        customFieldList.add(new CustomField(requiredFields.get("Description"),
                "Athena Test Case:  Test description setting"));
        JiraIssue issue = service.createIssue(
                CreateFields.ProjectType.RESEARCH_PROJECTS, "breilly",
                CreateFields.IssueType.RESEARCH_PROJECT,
                "JiraServiceTest.testUpdateTicket", customFieldList);

        Map<String, CustomFieldDefinition> allCustomFields = service.getCustomFields();

        CustomField mercuryUrlField = new CustomField(
                allCustomFields.get(ResearchProjectEjb.RequiredSubmissionFields.MERCURY_URL.getName()),
                "http://www.broadinstitute.org/");
        issue.updateIssue(Collections.singletonList(mercuryUrlField));
    }

    public void testAddWatcher() throws IOException {
        service.addWatcher(TEST_PDO_ISSUE, "squid");
    }

    @Test(enabled = false)
    public void testLinkTicket() throws IOException {
        service.addLink(AddIssueLinkRequest.LinkType.Related, TEST_PDO_ISSUE, "RP-1");
    }

    public void testAddPublicComment() throws IOException {
        service.addComment(TEST_LCSET_ISSUE, "Publicly visible comment added from Mercury");
    }


    @Test(enabled = false)
    // disabled until we can get test jira to keep squid user as an admin
    public void testAddRestrictedComment() throws IOException {
        service.addComment(TEST_LCSET_ISSUE, "jira-users only comment added from Mercury", Visibility.Type.role,
                Visibility.Value.Administrators);
    }

    public void testCustomFields() throws IOException {
        Map<String, CustomFieldDefinition> customFields = service.getRequiredFields(new CreateFields.Project(CreateFields.ProjectType.LCSET_PROJECT),
                CreateFields.IssueType.WHOLE_EXOME_HYBSEL);
        Assert.assertFalse(customFields.isEmpty());
        boolean foundLanesRequestedField = false;
        for (CustomFieldDefinition customField : customFields.values()) {
            if (customField.getName().equals("Lanes Requested")) {
                foundLanesRequestedField = true;
            }
        }
        Assert.assertTrue(foundLanesRequestedField);
    }

    @Test(expectedExceptions = NoJiraTransitionException.class)
    public void testTransitionNotFound() {
        service.findAvailableTransitionByName(TEST_PDO_ISSUE, "Going Nowhere Fast!");
        Assert.fail("You should not see this because I would have thrown a NoJiraTransitionException first.");
    }

    public void testTransitionWasFound() {
        String developerEditTransition = ProductOrderEjb.JiraTransition.DEVELOPER_EDIT.getStateName();
        Transition availableTransitionByName =
                service.findAvailableTransitionByName(TEST_PDO_ISSUE, developerEditTransition);
        Assert.assertEquals(availableTransitionByName.getName(), developerEditTransition);
    }
}
