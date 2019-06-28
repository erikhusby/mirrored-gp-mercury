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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JiraIssue implements Serializable {

    private final String key;

    private String summary;
    private String status;
    private String parent;
    private String description;
    private List<String> subTasks;

    private class Conditions {
        public List<String> subTaskSummaries = new ArrayList<>();
        public List<String> subTaskKeys = new ArrayList<>();
    }

    private Conditions conditions = new Conditions();

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
            copyFromJiraIssue(null);
        }
        return summary;
    }

    public void setSummary(@Nonnull String summary) {
        this.summary = summary;
    }

    public String getStatus() throws IOException {
        if(status == null) {
            copyFromJiraIssue(null);
        }
        return status;
    }

    public String getParent() throws IOException {
        return parent;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getDescription() throws IOException {

        if(description == null && summary == null) {
            copyFromJiraIssue(null);
        }

        return description;
    }

    public void setDescription(@Nonnull String description) {
        this.description = description;
    }

    public Date getDueDate() throws IOException {

        if(dueDate == null && summary == null) {
            copyFromJiraIssue(null);
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
            copyFromJiraIssue(null);
        }
        return created;
    }

    public String getReporter() throws IOException {
        if (this.reporter == null && summary == null) {
            copyFromJiraIssue(null);
        }
        return reporter;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public Object getFieldValue(@Nonnull String fieldName) throws IOException{

        Object foundValue;

        if(!extraFields.containsKey(fieldName)) {
            copyFromJiraIssue(fieldName);
        }
        foundValue = extraFields.get(fieldName);
        return foundValue;
    }

    public static List<String> parseSubTasks(List<Object> fields) throws IOException{
        ArrayList<String> idList = new ArrayList<String>();
        for (int index = 0; index < fields.size(); index++)
        {
            Map<String, Object> id = (Map<String, Object>)fields.get(index);
            idList.add(id.get("id").toString());
        }
        return idList;
    }


    public static List<String> parseSubTaskSummaries(List<Object> fields) throws IOException{
        ArrayList<Object> fieldList = new ArrayList<>();
        ArrayList<String> summaryList = new ArrayList<String>();
        for (int index = 0; index < fields.size(); index++)
        {
            Map<String, Object> id = (Map<String, Object>)fields.get(index);
            fieldList.add(id.get("fields"));
        }
        for (int sumaryIndex = 0; sumaryIndex < fieldList.size(); sumaryIndex++) {
            Map<String, Object> item = (Map<String, Object>)fieldList.get(sumaryIndex);
            summaryList.add(item.get("summary").toString());
        }

        return summaryList;
    }

    public static List<String> parseSubTasKeys(List<Object> fields) throws IOException{
        ArrayList<String> keyList = new ArrayList<String>();
        for (int index = 0; index < fields.size(); index++)
        {
            Map<String, Object> id = (Map<String, Object>)fields.get(index);
            keyList.add(id.get("key").toString());
        }
        return keyList;
    }

    public List<String> getSubTasks(){
        return this.subTasks;
    }

    public List<String> getSubTaskSummaries(){
        return this.conditions.subTaskSummaries;
    }

    public List<String> getSubTaskKeys(){
        return this.conditions.subTaskKeys;
    }

    public void setSubTasks(@Nonnull List<String> subTaskList){
        this.subTasks = subTaskList;
    }

    public void setConditions(@Nonnull List<String> subTaskSummaryList, @Nonnull List<String> subTaskKeyList) {
     this.conditions.subTaskKeys = subTaskKeyList;
     this.conditions.subTaskSummaries = subTaskSummaryList;
    }

    private void copyFromJiraIssue(String fieldName) throws IOException {
        JiraIssue tempIssue = jiraService.getIssueInfo(key, fieldName);
        if(tempIssue == null) {
            return;
        }
        extraFields.put(fieldName,tempIssue.getFieldValue(fieldName));
        summary = tempIssue.getSummary();
        description = tempIssue.getDescription();
        dueDate = tempIssue.getDueDate();
        created = tempIssue.getCreated();
        reporter = tempIssue.getReporter();
        status = tempIssue.getStatus();
        subTasks = tempIssue.getSubTasks();
        conditions = tempIssue.conditions;
        parent = tempIssue.getParent();
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
     * Add List of jira users to the issue's watcher list.
     * @see #addWatcher(String)
     */
    public void addWatchers(List<String> watchers) throws IOException {
        for (String watcher : watchers) {
            addWatcher(watcher);
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
     * Returns the value of a JIRA field that is contained in a JIRA map field, such as "Issue Type".
     * @param mapFieldName the name of the map object.
     * @param mapKey the key used to lookup a value in the map object.
     * @return the value from the map object, or null if not found or if the field is not a map object.
     * @throws IOException
     */
    public String getMappedField(String mapFieldName, String mapKey) throws IOException {
        Object mapObject = getField(mapFieldName);
        if (mapObject != null && mapObject instanceof Map) {
            return ((Map<String, String>)mapObject).get(mapKey);
        } else {
            return null;
        }
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
