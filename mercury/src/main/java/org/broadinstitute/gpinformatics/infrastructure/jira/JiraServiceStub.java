package org.broadinstitute.gpinformatics.infrastructure.jira;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.IssueFieldsResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraUser;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.IssueTransitionListResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.NextTransition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.NoJiraTransitionException;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.Transition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dummy implementation that writes calls to {@link #addComment(String, String)}  and
 * {@link #addComment(String, String, Visibility.Type, Visibility.Value)} to a logger. <br />
 * Injected as an alternate into all TestGroups.STUBBY tests
 */
@Dependent
@Alternative
public class JiraServiceStub implements JiraService {

    public JiraServiceStub(){}

    /**
     * Controls the suffix of the new batch name e.g. "LCSET-123"
     */
    public static String createdIssueSuffix = "-123";

    private Log logger = LogFactory.getLog(JiraServiceStub.class);

    @Override
    public JiraIssue createIssue(CreateFields.ProjectType projectType, String reporter, CreateFields.IssueType issueType,
                                 String summary, Collection<CustomField> customFields) throws
            IOException {
        return new JiraIssue(projectType.getKeyPrefix() + createdIssueSuffix, this);
    }

    @Override
    public JiraIssue getIssue(String key) {
        return new JiraIssue(key, this);
    }

    @Override
    public JiraIssue getIssueInfo(String key, String... fields) throws IOException {

        JiraIssue testValue = new JiraIssue(key, this);
        testValue.setDescription("Test synopsis");
        testValue.setSummary("Test Summary");
        if (fields != null) {
            for (String currField : fields) {
                testValue.addFieldValue(currField, currField + "Test value");
            }
        }
        return testValue;
    }

    @Override
    public List<JiraUser> getJiraUsers(String key) {
        return null;
    }

    @Override
    public void updateIssue(String key, Collection<CustomField> customFields) throws IOException {
        logger.info("Dummy jira service! Updating " + key);
    }

    @Override
    public void updateAssignee(String key, String name) {
        logger.info("Dummy jira service! Updating assignee " + key);
    }

    @Override
    public void addComment(String key, String body) throws IOException {
        logger.info("Dummy jira service! " + body + " for " + key);
    }

    @Override
    public void addComment(String key, String body, Visibility.Type visibilityType,
                           Visibility.Value visibilityValue) throws IOException {
        logger.info("Dummy jira service! " + body + " for " + key);
    }

    @Override
    public void addLink(AddIssueLinkRequest.LinkType type, String sourceIssueIn, String targetIssueIn)
            throws IOException {
        logger.info("Dummy jira service! " + type + " Link from " + sourceIssueIn + " to " + targetIssueIn);
    }

    @Override
    public void addLink(AddIssueLinkRequest.LinkType type, String sourceIssueIn, String targetIssueIn,
                        String commentBody, Visibility.Type availabilityType, Visibility.Value availabilityValue)
            throws IOException {
        logger.info("Dummy jira service! " + type + " Link from " + sourceIssueIn + " to " +
                    targetIssueIn + " with comments " + commentBody);
    }

    @Override
    public void addWatcher(String key, String watcherId) throws IOException {
        logger.info("Dummy jira service! Add watcher " + watcherId + " for " + key);
    }

    @Override
    public Map<String, CustomFieldDefinition> getRequiredFields(@Nonnull CreateFields.Project project,
                                                                @Nonnull CreateFields.IssueType issueType) throws
            IOException {
        Map<String, CustomFieldDefinition> customFields = new HashMap<>();
        for (String requiredFieldName : JiraCustomFieldsUtil.REQUIRED_FIELD_NAMES) {
            customFields.put(requiredFieldName, new CustomFieldDefinition(
                    "stub_custom_field_" + requiredFieldName, requiredFieldName, true));
        }
        return customFields;
    }

    @Override
    public Map<String, CustomFieldDefinition> getCustomFields(String... fieldNames) throws IOException {
        Map<String, CustomFieldDefinition> customFields = new HashMap<>();
        for (String requiredFieldName : JiraCustomFieldsUtil.REQUIRED_FIELD_NAMES) {
            customFields.put(requiredFieldName, new CustomFieldDefinition("stub_custom_field_" + requiredFieldName,
                    requiredFieldName, true));
        }

        for (String fieldName : fieldNames) {
            customFields.put(fieldName, new CustomFieldDefinition("stub_custom_field_" + fieldName, fieldName, true));
        }

        return customFields;
    }

    @Override
    public String createTicketUrl(String jiraTicketName) {
        return "http://dummy-jira-service.blah/" + jiraTicketName;
    }


    @Override
    public IssueTransitionListResponse findAvailableTransitions(String jiraIssueKey) {
        Transition[] transitions = new Transition[]{
                new Transition("1", ProductOrderEjb.JiraTransition.OPEN.getStateName(), new NextTransition("", "In Progress", "In Progress", "", "2")),
                new Transition("3", ProductOrderEjb.JiraTransition.COMPLETE_ORDER.getStateName(), new NextTransition("", "Closed", "Closed", "", "4")),
                new Transition("15", "Complete", new NextTransition("", "Closed", "Closed", "", "4")),
                new Transition("5", "Cancel", new NextTransition("", "Closed", "Closed", "", "6")),
                new Transition("7", "Start Progress", new NextTransition("", "In Progress", "In Progress", "", "8")),
                new Transition("9", "Put On Hold", new NextTransition("", "held", "held", "", "10")),
                new Transition("11", ProductOrderEjb.JiraTransition.ORDER_COMPLETE.getStateName(), new NextTransition("", "Complete", "Complete", "", "12")),
                new Transition("13", ProductOrderEjb.JiraTransition.DEVELOPER_EDIT.getStateName(), new NextTransition("", "In Progress", "In Progress", "", "14"))
        };

        return new IssueTransitionListResponse("", Arrays.asList(transitions));
    }


    @Override
    public Transition findAvailableTransitionByName(@Nonnull String jiraIssueKey, @Nonnull String transitionName) {
        IssueTransitionListResponse availableTransitions = findAvailableTransitions(jiraIssueKey);
        List<Transition> transitions = availableTransitions.getTransitions();
        if (transitions == null || transitions.isEmpty()) {
            throw new NoJiraTransitionException("No transitions found for issue key " + jiraIssueKey);
        }
        for (Transition transition : transitions) {
            if (transition.getName().equals(transitionName)) {
                return transition;
            }
        }
        throw new NoJiraTransitionException(transitionName, jiraIssueKey);
    }

    @Override
    public void postNewTransition(String jiraIssueKey, Transition transition,
                                  @Nonnull Collection<CustomField> customFields,
                                  @Nullable String comment) throws IOException {

    }

    @Override
    public void postNewTransition(String jiraIssueKey, Transition transition, @Nullable String comment)
            throws IOException {

    }

    @Override
    public boolean isValidUser(String username) {
        // Pretend all users are valid for test config.
        return true;
    }

    @Override
    public IssueFieldsResponse getIssueFields(String jiraIssueKey,
                                              Collection<CustomFieldDefinition> customFieldDefinitions) throws
            IOException {
        final IssueFieldsResponse issueFieldsResponse = new IssueFieldsResponse();

        Map<String, Object> customFields = new HashMap<>();
        for (String requiredFieldName : JiraCustomFieldsUtil.REQUIRED_FIELD_NAMES) {
            customFields.put("stub_custom_field_" + requiredFieldName,"Open");
        }
        customFields.put("stub_custom_field_"+ProductOrder.JiraField.STATUS.getName(), Collections.singletonMap("name","Open"));


        issueFieldsResponse.setFields(customFields);

        return issueFieldsResponse;
    }

    @Override
    public void deleteLink(String jiraIssueLinkId) throws IOException {

    }

    @Override
    public String getResolution(String jiraIssueKey) throws IOException {
        return "";
    }

    public static void setCreatedIssueSuffix(String createdIssueSuffix) {
        JiraServiceStub.createdIssueSuffix = createdIssueSuffix;
    }

    public static String getCreatedIssueSuffix() {
        return createdIssueSuffix;

    }
}
