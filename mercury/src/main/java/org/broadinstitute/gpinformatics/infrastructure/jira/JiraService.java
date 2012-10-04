package org.broadinstitute.gpinformatics.infrastructure.jira;


import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public interface JiraService extends Serializable {


    /**
     * Create an issue with a project prefix specified by projectPrefix; i.e. for this method projectPrefix would be 'TP' and not 'TP-5' for
     * the JIRA project TestProject having prefix TP.
     *
     *
     * @param projectPrefix
     * @param issuetype
     * @param summary
     * @param description
     * @param customFields
     * @return
     * @throws IOException
     */
    CreateIssueResponse createIssue(String projectPrefix, CreateIssueRequest.Fields.Issuetype issuetype, String summary, String description, Collection<CustomField> customFields) throws IOException;


    /**
     * Add a publicly visible comment to the specified issue.
     * 
     * @param key
     * 
     * @param body
     */
    void addComment(String key, String body) throws IOException;


    /**
     * Add a comment to the specified issue whose visibility is restricted by the {@link Visibility} specifiers.
     *
     * @param key
     * @param body
     * @param visibilityType
     * @param visibilityValue
     * @throws IOException
     */
    void addComment(String key, String body, Visibility.Type visibilityType, Visibility.Value visibilityValue) throws IOException;

    /**
     * Finds all the custom fields for the given project and issue type
     *
     * @param project
     * @param issueType
     * @return
     */
    public Map<String, CustomFieldDefinition> getRequiredFields(CreateIssueRequest.Fields.Project project,
                                                                CreateIssueRequest.Fields.Issuetype issueType) throws IOException;

    public String createTicketUrl(String jiraTicketName);

    public Map<String, CustomFieldDefinition> getCustomFields(CreateIssueRequest.Fields.Project project,
                                                              CreateIssueRequest.Fields.Issuetype issueType) throws IOException;

    void addLink(AddIssueLinkRequest.LinkType type, String sourceIssueIn, String targetIssueIn) throws IOException;

    void addLink(AddIssueLinkRequest.LinkType type, String sourceIssueIn, String targetIssueIn, String commentBody,
                 Visibility.Type availabilityType, Visibility.Value availabilityValue) throws IOException;

    void addWatcher(String key, String watcherId) throws IOException;
}
