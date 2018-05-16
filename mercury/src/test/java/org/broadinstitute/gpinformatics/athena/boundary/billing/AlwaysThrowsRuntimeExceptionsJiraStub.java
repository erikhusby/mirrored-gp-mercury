package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang.NotImplementedException;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Completely 'angry' JIRA implementation that will throw a {@link RuntimeException} on any method invocation.
 */
@Alternative
@Dependent
class AlwaysThrowsRuntimeExceptionsJiraStub implements JiraService {

    public AlwaysThrowsRuntimeExceptionsJiraStub(){}

    /**
     * The invocation count variable feels like something that would normally be accomplished with a mock, but an
     * instance of this class needs to be injectable by CDI.  It would be nice to find a way to accomplish the
     * invocation count tracking that was more DRY.
     */
    private static int invocationCount;

    @Override
    public JiraIssue createIssue(CreateFields.ProjectType projectType, String reporter, CreateFields.IssueType issueType,
                                 String summary, Collection<CustomField> customFields)
            throws IOException {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public void updateIssue(String key, Collection<CustomField> customFields) throws IOException {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public JiraIssue getIssue(String key) throws IOException {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public void addComment(String key, String body) throws IOException {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public void addComment(String key, String body, Visibility.Type visibilityType,
                           Visibility.Value visibilityValue)
            throws IOException {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public Map<String, CustomFieldDefinition> getRequiredFields(@Nonnull CreateFields.Project project,
                                                                @Nonnull CreateFields.IssueType issueType)
            throws IOException {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public String createTicketUrl(String jiraTicketName) {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public Map<String, CustomFieldDefinition> getCustomFields(String... fieldNames) throws IOException {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public void addLink(AddIssueLinkRequest.LinkType type, String sourceIssueIn, String targetIssueIn)
            throws IOException {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public void addLink(AddIssueLinkRequest.LinkType type, String sourceIssueIn, String targetIssueIn,
                        String commentBody, Visibility.Type availabilityType, Visibility.Value availabilityValue)
            throws IOException {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public void addWatcher(String key, String watcherId) throws IOException {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public IssueTransitionListResponse findAvailableTransitions(String jiraIssueKey) {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public Transition findAvailableTransitionByName(String jiraIssueKey, String transitionName) {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public void postNewTransition(String jiraIssueKey, Transition transition, @Nullable String comment)
            throws IOException {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public void postNewTransition(String jiraIssueKey, Transition transition,
                                  @Nonnull Collection<CustomField> customFields, @Nullable String comment)
            throws IOException {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public IssueFieldsResponse getIssueFields(String jiraIssueKey,
                                              Collection<CustomFieldDefinition> customFieldDefinitions)
            throws IOException {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public void deleteLink(String jiraIssueLinkId) throws IOException {

    }

    @Override
    public String getResolution(String jiraIssueKey) throws IOException {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public boolean isValidUser(String username) {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public JiraIssue getIssueInfo(String key, String... fields) throws IOException {
        invocationCount++;
        throw new NotImplementedException();
    }

    @Override
    public List<JiraUser> getJiraUsers(String key) {
        throw new NotImplementedException();
    }

    public static int getInvocationCount() {
        return invocationCount;
    }
}
