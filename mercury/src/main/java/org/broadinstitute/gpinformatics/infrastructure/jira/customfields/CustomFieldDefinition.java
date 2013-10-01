package org.broadinstitute.gpinformatics.infrastructure.jira.customfields;

import org.codehaus.jackson.annotate.JsonIgnore;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CustomFieldDefinition {

    public static final List<String> NULL_ALLOWED_VALUES = null;
    private final String fieldId;

    private final String fieldName;

    private final boolean isRequired;

    private Collection<String> allowedValues;

    public CustomFieldDefinition(String fieldId, String fieldName, boolean isRequired,
                                 @Nullable Collection<String> allowedValues) {
        this.allowedValues = allowedValues;
        if (fieldId == null) {
            throw new NullPointerException("fieldId cannot be null");
        }
        if (fieldName == null) {
            throw new NullPointerException("fieldName cannot be null");
        }
        this.fieldId = fieldId;
        this.fieldName = fieldName;
        this.isRequired = isRequired;
    }

    @JsonIgnore
    public String getName() {
        return fieldName;
    }

    public String getJiraCustomFieldId() {
        return fieldId;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public Collection<String> getAllowedValues() {
        if (allowedValues == null) {
            allowedValues = Collections.emptyList();
        }
        return allowedValues;
    }
}
