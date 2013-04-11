package org.broadinstitute.gpinformatics.infrastructure.jira;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.IssueFieldsResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.IssueTransitionListResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.NextTransition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.Transition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.inject.Alternative;
import java.io.IOException;
import java.util.*;

/**
 * Dummy implementation that writes calls
 * to {@link #addComment(String, String)}  and
 * {@link #addComment(String, String, org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility.Type, org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility.Value)}
 * to a logger.
 */
@Stub
@Alternative
public class JiraServiceStub implements JiraService {

    public static final String CREATED_ISSUE_SUFFIX = "-123";

    private Log logger = LogFactory.getLog(JiraServiceStub.class);

    @Override
    public JiraIssue createIssue(String projectPrefix, String reporter, CreateFields.IssueType issueType,
                                 String summary, String description, Collection<CustomField> customFields) throws
            IOException {
        return new JiraIssue(projectPrefix + CREATED_ISSUE_SUFFIX, this);
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
    public void updateIssue(String key, Collection<CustomField> customFields) throws IOException {
        logger.info("Dummy jira service! Updating " + key);
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
        Map<String, CustomFieldDefinition> customFields = new HashMap<String, CustomFieldDefinition>();
        for (String requiredFieldName : JiraCustomFieldsUtil.REQUIRED_FIELD_NAMES) {
            customFields.put(requiredFieldName, new CustomFieldDefinition(
                    "stub_custom_field_" + requiredFieldName, requiredFieldName, true));
        }
        return customFields;
    }

    @Override
    public Map<String, CustomFieldDefinition> getCustomFields(String... fieldNames) throws IOException {
        Map<String, CustomFieldDefinition> customFields = new HashMap<String, CustomFieldDefinition>();
        for (String requiredFieldName : JiraCustomFieldsUtil.REQUIRED_FIELD_NAMES) {
            customFields.put(requiredFieldName, new CustomFieldDefinition("stub_custom_field_" + requiredFieldName,
                    requiredFieldName, true));
        }
        return customFields;
    }

    @Override
    public String createTicketUrl(String jiraTicketName) {
        return "http://dummy-jira-service.blah/" + jiraTicketName;
    }


    @Override
    public IssueTransitionListResponse findAvailableTransitions(String jiraIssueKey) {
        Transition[] transitions = new Transition[] {
                new Transition("1", "Open", new NextTransition("", "In Progress", "In Progress", "", "2")),
                new Transition("3", "Complete", new NextTransition("", "Closed", "Closed", "", "4")),
                new Transition("5", "Cancel", new NextTransition("", "Closed", "Closed", "", "6")),
                new Transition("7", "Start Progress", new NextTransition("", "In Progress", "In Progress", "", "8")),
                new Transition("9", "Put On Hold", new NextTransition("", "held", "held", "", "10")),
                new Transition("11", "Order Complete", new NextTransition("", "Complete", "Complete", "", "12"))
        };

        return new IssueTransitionListResponse("", Arrays.asList(transitions));
    }


    @Override
    public Transition findAvailableTransitionByName(String jiraIssueKey, String transitionName) {
        IssueTransitionListResponse availableTransitions = findAvailableTransitions(jiraIssueKey);

        for (Transition transition : availableTransitions.getTransitions()) {
            if (transition.getName().equals(transitionName)) {
                return transition;
            }
        }

        return null;
    }

    @Override
    public void postNewTransition(String jiraIssueKey, Transition transition,
                                  @Nonnull Collection<CustomField> customFields,
                                  @Nullable String comment) throws IOException {

    }

    @Override
    public void postNewTransition(String jiraIssueKey, Transition transition, @Nullable String comment) throws IOException {

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
        return null;
    }

    @Override
    public String getResolution(String jiraIssueKey) throws IOException {
        return "";
    }
}
