package org.broadinstitute.gpinformatics.infrastructure.jira.issue;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.IssueTransitionListResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.NoJiraTransitionException;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.Transition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class JiraIssue implements Serializable {

    private final String key;

    private String summary;
    private String description;

    private final Map<String, Object> extraFields = new HashMap<>();

    private Date created;
    private Date dueDate;

    private String reporter;

    private final JiraService jiraService;

    public JiraIssue(String key, JiraService jiraService) {
        this.key = key;
        this.jiraService = jiraService;
    }

    public String getKey() {
        return key;
    }

    public String getSummary() throws IOException{

        if(summary == null) {
            JiraIssue tempIssue = jiraService.getIssueInfo(key, (String []) null);
            summary = tempIssue.getSummary();
            description = tempIssue.getDescription();
            dueDate = tempIssue.getDueDate();
            created = tempIssue.getCreated();
            reporter = tempIssue.getReporter();
        }
        return summary;
    }

    public void setSummary(@Nonnull String summary) {
        this.summary = summary;
    }

    public String getDescription() throws IOException {

        if(description == null && summary == null) {
            JiraIssue tempIssue = jiraService.getIssueInfo(key, (String []) null);
            summary = tempIssue.getSummary();
            description = tempIssue.getDescription();
            dueDate = tempIssue.getDueDate();
            created = tempIssue.getCreated();
            reporter = tempIssue.getReporter();
        }

        return description;
    }

    public void setDescription(@Nonnull String description) {
        this.description = description;
    }

    public Date getDueDate() throws IOException {

        if(dueDate == null && summary == null) {
            JiraIssue tempIssue = jiraService.getIssueInfo(key, (String []) null);
            summary = tempIssue.getSummary();
            description = tempIssue.getDescription();
            dueDate = tempIssue.getDueDate();
            created = tempIssue.getCreated();
            reporter = tempIssue.getReporter();
        }

        return dueDate;
    }

    public void setDueDate(@Nonnull Date dueDate) {
        this.dueDate = dueDate;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getCreated() throws IOException {

        if(created == null && summary == null) {
            JiraIssue tempIssue = jiraService.getIssueInfo(key, (String []) null);
            summary = tempIssue.getSummary();
            description = tempIssue.getDescription();
            dueDate = tempIssue.getDueDate();
            created = tempIssue.getCreated();
            reporter = tempIssue.getReporter();
        }
        return created;
    }

    public String getReporter() throws IOException {
        if (this.reporter == null && summary == null) {
            JiraIssue tempIssue = jiraService.getIssueInfo(key, (String []) null);
            summary = tempIssue.getSummary();
            description = tempIssue.getDescription();
            dueDate = tempIssue.getDueDate();
            reporter = tempIssue.getReporter();

        }
        return reporter;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public Object getFieldValue(@Nonnull String fieldName) throws IOException{

        Object foundValue;

        if(!extraFields.containsKey(fieldName)) {
            JiraIssue tempIssue = jiraService.getIssueInfo(key, fieldName);
            extraFields.put(fieldName,tempIssue.getFieldValue(fieldName));
            summary = tempIssue.getSummary();
            description = tempIssue.getDescription();
            dueDate = tempIssue.getDueDate();
            reporter = tempIssue.getReporter();
        }
        foundValue = extraFields.get(fieldName);
        return foundValue;
    }

    public <TV> void addFieldValue(String filedName, TV value) {
        extraFields.put(filedName, value);
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

    public void deleteLink(String linkId) throws IOException {
        jiraService.deleteLink(linkId);
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
     * Return the value of a JIRA field by name.
     *
     * @param fieldName  the field to return
     * @return the field's value
     * @throws IOException
     */
    public Object getField(String fieldName) throws IOException {
        CustomFieldDefinition definition = jiraService.getCustomFields(fieldName).get(fieldName);
        IssueFieldsResponse response = jiraService.getIssueFields(key, Collections.singleton(definition));
        return response.getFields().get(definition.getJiraCustomFieldId());
    }

    /**
     * @return a list of all available workflow transitions for this ticket in its current state
     */
    public IssueTransitionListResponse findAvailableTransitions() {
        return jiraService.findAvailableTransitions(key);
    }

    /**
     * Post a transition for this issue.
     *
     * @param transitionName the name of the transition to post
     */
    public void postTransition(@Nonnull String transitionName, @Nullable String comment) throws IOException {
        Transition transition = jiraService.findAvailableTransitionByName(key, transitionName);
        if (transition == null) {
            throw new NoJiraTransitionException(transitionName, key);
        }
        jiraService.postNewTransition(key, transition, comment);
    }

    public Map<String, CustomFieldDefinition> getCustomFields(String... fieldNames) throws IOException {
        return jiraService.getCustomFields(fieldNames);
    }

    public void setCustomFieldUsingTransition(CustomField.SubmissionField field, Object value, String transitionName)
            throws IOException {
        Transition transition = jiraService.findAvailableTransitionByName(key, transitionName);
        Map<String, CustomFieldDefinition> definitionMap = getCustomFields(field.getName());
        List<CustomField> customFields = Collections.singletonList(new CustomField(definitionMap, field, value));
        jiraService.postNewTransition(key, transition, customFields, null);
    }

    @Override
    public String toString() {
        return "JiraIssue{" +
                "key='" + key + '\'' +
                '}';
    }

    public String getResolution() throws IOException {
        return jiraService.getResolution(key);
    }

    public void updateIssueLink(String receiptKey, String oldReceiptKey) throws IOException {
        JiraIssue jiraIssue = this;

        List<Map> issuelinks = (List<Map>) jiraIssue.getFieldValue("issuelinks");
        if(issuelinks != null) {
            for(Map<String, Object> links : issuelinks) {
                if(links.get("outwardIssue") != null) {
                    Map<String, Object> otherIssue = (Map<String, Object>) links.get("outwardIssue");
                    if (otherIssue.get("key").equals(oldReceiptKey)) {
                        jiraIssue.deleteLink((String) links.get("id"));
                    }
                }
            }
        }
        jiraIssue.addLink(receiptKey);
    }
}
