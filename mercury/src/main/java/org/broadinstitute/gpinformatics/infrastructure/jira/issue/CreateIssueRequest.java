package org.broadinstitute.gpinformatics.infrastructure.jira.issue;


import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;

import java.util.Collection;


public class CreateIssueRequest {

    private CreateFields fields;

    public CreateFields getFields() {
        return fields;
    }

    public void setFields(CreateFields fields) {
        this.fields = fields;
    }


    public CreateIssueRequest() {
        this.fields = new CreateFields();
        // todo arz move these out to JiraService params
//        this.fields.customFields.add(new CustomField(new CustomFieldDefinition("customfield_10020","Protocol",true),"test protocol"));
//        this.fields.customFields.add(new CustomField(new CustomFieldDefinition("customfield_10011","Work Request ID(s)",true),"WR 1 Billion!"));
    }

    public CreateIssueRequest(Collection<CustomField> customFields) {
        this();
        if (customFields != null) {
            fields.getCustomFields().addAll(customFields);
        }
    }


    public static CreateIssueRequest create(String key,
                                            String reporter,
                                            CreateFields.IssueType issueType,
                                            String summary,
                                            Collection<CustomField> customFields) {

        CreateIssueRequest ret = new CreateIssueRequest(customFields);

        CreateFields fields = ret.getFields();

        fields.getProject().setKey(key);

        if (reporter != null) {
            fields.getReporter().setName(reporter);
        } else {
            fields.setReporter(null);
        }

        fields.setIssueType(issueType);
        fields.setSummary(summary);

        return ret;
    }
}
