package org.broadinstitute.gpinformatics.infrastructure.jira.issue;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.UpdateJiraIssueUpdateSerializer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Fields that can be sent to JIRA in an update request.
 */
@JsonSerialize(using = UpdateJiraIssueUpdateSerializer.class)
public class UpdateFields {

    private final Set<CustomField> customFields;

    public Collection<CustomField> getCustomFields() {
        return customFields;
    }

    public UpdateFields() {
        this(Collections.<CustomField>emptySet());
    }

    public UpdateFields(Collection<CustomField> customFields) {
        this.customFields = new HashSet<>(customFields);
    }
}
