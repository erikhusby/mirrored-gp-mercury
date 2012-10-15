package org.broadinstitute.gpinformatics.infrastructure.jira;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Dummy implementation that writes calls
 * to {@link #addComment(String, String)}  and
 * {@link #addComment(String, String, org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility.Type, org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility.Value)}
 * to a logger.
 */
@Stub
public class JiraServiceStub implements JiraService {

    private Log logger = LogFactory.getLog(JiraServiceStub.class);

    @Override
    public CreateIssueResponse createIssue(String projectPrefix, CreateIssueRequest.Fields.Issuetype issuetype, String summary, String description, Collection<CustomField> customFields) throws IOException {
        return new CreateIssueResponse("123",projectPrefix + "-123");
    }

    @Override
    public void addComment(String key, String body) throws IOException {
        logger.info("Dummy jira service! " + body + " for " + key);
    }

    @Override
    public void addComment(String key, String body, Visibility.Type visibilityType, Visibility.Value visibilityValue) throws IOException {
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
    public Map<String, CustomFieldDefinition> getRequiredFields(CreateIssueRequest.Fields.Project project,
                                                                CreateIssueRequest.Fields.Issuetype issueType) throws IOException {
        final Map<String, CustomFieldDefinition> customFields = new HashMap<String, CustomFieldDefinition>();
        for (String requiredFieldName : JiraCustomFieldsUtil.REQUIRED_FIELD_NAMES) {
            customFields.put(requiredFieldName,new CustomFieldDefinition("stub_custom_field_" + requiredFieldName,requiredFieldName,true));
        }
        return customFields;
    }

    @Override
    public Map<String, CustomFieldDefinition> getCustomFields ( ) throws IOException {
        final Map<String, CustomFieldDefinition> customFields = new HashMap<String, CustomFieldDefinition>();
        for (String requiredFieldName : JiraCustomFieldsUtil.REQUIRED_FIELD_NAMES) {
            customFields.put ( requiredFieldName, new CustomFieldDefinition ( "stub_custom_field_" + requiredFieldName,
                                                                              requiredFieldName, true ) );
        }
        return customFields;
    }

    @Override
    public String createTicketUrl(String jiraTicketName) {
        return "http://dummy-jira-service.blah/" + jiraTicketName;
    }
}
