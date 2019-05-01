package org.broadinstitute.gpinformatics.infrastructure.jira.customfields;


import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

public class CustomFieldDefinition {

    private final String fieldId;

    private final String fieldName;

    private final boolean isRequired;
    private final Collection<CustomField.ValueContainer> allowedValues;

    public CustomFieldDefinition(@Nonnull String fieldId, @Nonnull String fieldName, boolean isRequired) {
        this(fieldId, fieldName, isRequired, Collections.<CustomField.ValueContainer>emptyList());
    }

    public CustomFieldDefinition(@Nonnull String fieldId, @Nonnull String fieldName, boolean isRequired,
                                 Collection<CustomField.ValueContainer> allowedValues) {
        if (fieldId == null) {
            throw new NullPointerException("fieldId cannot be null");
        }
        if (fieldName == null) {
            throw new NullPointerException("fieldName cannot be null");
        }
        this.fieldId = fieldId;
        this.fieldName = fieldName;
        this.isRequired = isRequired;
        this.allowedValues=allowedValues;
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

    @JsonIgnore
    public Collection<CustomField.ValueContainer> getAllowedValues() {
        return allowedValues;
    }
}
