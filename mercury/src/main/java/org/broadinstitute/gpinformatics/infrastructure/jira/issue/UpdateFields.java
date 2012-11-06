package org.broadinstitute.gpinformatics.infrastructure.jira.issue;

import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.UpdateJiraIssueUpdateSerializer;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.Collection;
import java.util.HashSet;

/**
 * Fields that can be sent to JIRA in an update request.
 *
 * @author breilly
 */
@JsonSerialize(using = UpdateJiraIssueUpdateSerializer.class)
public class UpdateFields {

    private String summary;

    private String description;

    private final Collection<CustomField> customFields = new HashSet<CustomField>();

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

    public Collection<CustomField> getCustomFields() {
        return customFields;
    }
}
