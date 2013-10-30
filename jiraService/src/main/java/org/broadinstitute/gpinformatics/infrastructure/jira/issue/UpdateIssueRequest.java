package org.broadinstitute.gpinformatics.infrastructure.jira.issue;

import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;

import java.util.Collection;

/**
 * @author breilly
 */
public class UpdateIssueRequest {

    private String key;

    private UpdateFields fields = new UpdateFields();

    public UpdateIssueRequest(String key, Collection<CustomField> customFields) {
        this.key = key;
        if (customFields != null) {
            fields.getCustomFields().addAll(customFields);
        }
    }

    public String getUrl(String baseUrl) {
        return baseUrl + "/issue/" + key;
    }

    public UpdateFields getFields() {
        return fields;
    }

    public void setFields(UpdateFields fields) {
        this.fields = fields;
    }
}
