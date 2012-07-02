package org.broadinstitute.sequel.infrastructure.jira;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.sequel.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.sequel.infrastructure.jira.issue.Visibility;

import javax.enterprise.inject.Alternative;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Dummy implementation that writes calls
 * to {@link #addComment(String, String)}  and
 * {@link #addComment(String, String, org.broadinstitute.sequel.infrastructure.jira.issue.Visibility.Type, org.broadinstitute.sequel.infrastructure.jira.issue.Visibility.Value)}
 * to a logger.
 */
@Alternative
public class DummyJiraService implements JiraService {

    private Log logger = LogFactory.getLog(DummyJiraService.class);

    @Override
    public CreateIssueResponse createIssue(String projectPrefix, CreateIssueRequest.Fields.Issuetype issuetype, String summary, String description, Collection<CustomField> customFields) throws IOException {
        return new CreateIssueResponse("123",projectPrefix + "123");
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
    public List<CustomFieldDefinition> getCustomFields(CreateIssueRequest.Fields.Project project, CreateIssueRequest.Fields.Issuetype issueType) throws IOException {
        final List<CustomFieldDefinition> customFields = new ArrayList<CustomFieldDefinition>();
        for (String requiredFieldName : JiraCustomFieldsUtil.REQUIRED_FIELD_NAMES) {
            customFields.add(new CustomFieldDefinition("stub_custom_field_" + requiredFieldName,requiredFieldName,true));
        }
        return customFields;
    }

    @Override
    public String createTicketUrl(String jiraTicketName) {
        return "http://dummy-jira-service.blah/" + jiraTicketName;
    }
}
