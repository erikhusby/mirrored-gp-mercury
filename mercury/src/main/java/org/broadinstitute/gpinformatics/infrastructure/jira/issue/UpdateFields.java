package org.broadinstitute.gpinformatics.infrastructure.jira.issue;

import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.UpdateJiraIssueUpdateSerializer;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Fields that can be sent to JIRA in an update request.
 *
 * @author breilly
 */
@JsonSerialize(using = UpdateJiraIssueUpdateSerializer.class)
public class UpdateFields {

    private final Collection<CustomField> customFields;

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
