package org.broadinstitute.gpinformatics.infrastructure.jira.issue;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.IssueTransitionResponse;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

public class JiraIssue implements Serializable {

    private final String key;

    private String summary;
    private String description;

    private final JiraService jiraService;

    public JiraIssue(String key, JiraService jiraService) {
        this.key = key;
        this.jiraService = jiraService;
    }

    public String getKey() {
        return key;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Updates an issue, modifying the custom fields supplied.
     *
     * @param customFields    the fields to modify
     */
    public void updateIssue(Collection<CustomField> customFields) throws IOException {
        jiraService.updateIssue(key, customFields);
    }

    /**
     * Add a publicly visible comment to this issue.
     *
     * @param body the comment text. If empty, no change will occur.
     */
    public void addComment(String body) throws IOException {
        if (!StringUtils.isBlank(body)) {
            jiraService.addComment(key, body);
        }
    }

    /**
     * Add a link between this and another issue.
     * @param type the type of link to create
     * @param targetIssueIn the issue to link this one to
     * @throws IOException
     */
    public void addLink(AddIssueLinkRequest.LinkType type, String targetIssueIn) throws IOException {
        jiraService.addLink(type, key, targetIssueIn);
    }

    /**
     * Add a link between this and another issue, using the Related link type.
     * @param targetIssueIn the issue to link this one to
     * @throws IOException
     */
    public void addLink(String targetIssueIn) throws IOException {
        addLink(AddIssueLinkRequest.LinkType.Related, targetIssueIn);
    }

    // Workaround for BSP users whose username includes their email address. This is an interim fix until
    // we can fixup existing usernames.
    private static final String BROADINSTITUTE_ORG = "@broadinstitute.org";

    /**
     * Add a watcher to this issue.
     * @param watcherId the username of the watcher; must be a valida JIRA user
     * @throws IOException
     */
    public void addWatcher(String watcherId) throws IOException {
        try {
            jiraService.addWatcher(key, watcherId);
        } catch (Exception e) {
            if (watcherId.endsWith(BROADINSTITUTE_ORG)) {
                // Retry after stripping off the email suffix.
                jiraService.addWatcher(key, watcherId.replace(BROADINSTITUTE_ORG, ""));
            }
        }
    }

    /**
     * @return a list of all available workflow transitions for this ticket in its current state
     */
    public IssueTransitionResponse findAvailableTransitions() {
        return jiraService.findAvailableTransitions(key);
    }

    /**
     * Transition a given Jira Ticket to a new Transition state.
     * @param transitionId id representing the next transition state
     */
    public void postNewTransition(String transitionId) throws IOException {
        jiraService.postNewTransition(key, transitionId);
    }

    @Override
    public String toString() {
        return "JiraIssue{" +
                "key='" + key + '\'' +
                '}';
    }
}
