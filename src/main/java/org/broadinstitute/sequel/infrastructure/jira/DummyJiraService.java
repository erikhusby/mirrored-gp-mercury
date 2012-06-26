package org.broadinstitute.sequel.infrastructure.jira;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.sequel.infrastructure.jira.issue.Visibility;

import javax.enterprise.inject.Alternative;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Dummy implementation that writes calls
 * to {@link #addComment(String, String)}  and
 * {@link #addComment(String, String, org.broadinstitute.sequel.infrastructure.jira.issue.Visibility.Type, org.broadinstitute.sequel.infrastructure.jira.issue.Visibility.Value)}
 * to a logger.  {@link #createIssue(String, String, String, String)}  throws an exception.
 */
@Alternative
public class DummyJiraService implements JiraService {

    private Log logger = LogFactory.getLog(DummyJiraService.class);

    @Override
    public CreateIssueResponse createIssue(String projectPrefix, CreateIssueRequest.Fields.Issuetype issuetype, String summary, String description) throws IOException {
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
        return Collections.<CustomFieldDefinition>emptyList();
    }
}
