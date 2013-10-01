package org.broadinstitute.gpinformatics.infrastructure.jira.customfields;

import org.codehaus.jackson.annotate.JsonIgnore;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CustomFieldDefinition {

    public static final List<String> NULL_ALLOWED_VALUES = null;
    private final String fieldId;

    private final String fieldName;

    private final boolean isRequired;

    private final Collection<String> allowedValues;

    public CustomFieldDefinition(@Nonnull String fieldId, @Nonnull String fieldName, boolean isRequired) {
        this(fieldId, fieldName, isRequired, Collections.<String>emptyList());
    }

    public CustomFieldDefinition(@Nonnull String fieldId, @Nonnull String fieldName, boolean isRequired,
                                 Collection<String> allowedValues) {
        if (fieldId == null) {
            throw new NullPointerException("fieldId cannot be null");
        }
        if (fieldName == null) {
            throw new NullPointerException("fieldName cannot be null");
        }
        this.fieldId = fieldId;
        this.fieldName = fieldName;
        this.isRequired = isRequired;
        this.allowedValues = allowedValues;
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
        return allowedValues;
    }
}
