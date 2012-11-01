package org.broadinstitute.gpinformatics.infrastructure.jira.issue;

import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.UpdateJiraIssueUpdateSerializer;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author breilly
 */
public class UpdateIssueRequest {

    @JsonSerialize(using = UpdateJiraIssueUpdateSerializer.class)
    public static class Update {

        private final Collection<CustomField> customFields = new HashSet<CustomField>();

        public Collection<CustomField> getCustomFields() {
            return customFields;
        }
    }

    private Update fields = new Update();

    public UpdateIssueRequest() {
    }

    public UpdateIssueRequest(Collection<CustomField> customFields) {
        if (customFields != null) {
            fields.getCustomFields().addAll(customFields);
        }
    }

    public Update getFields() {
        return fields;
    }

    public void setFields(Update fields) {
        this.fields = fields;
    }
}
