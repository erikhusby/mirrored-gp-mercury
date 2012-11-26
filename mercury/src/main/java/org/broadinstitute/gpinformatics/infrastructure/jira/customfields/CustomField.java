package org.broadinstitute.gpinformatics.infrastructure.jira.customfields;

import javax.annotation.Nonnull;
import java.util.Map;

public class CustomField {

    private final CustomFieldDefinition definition;

    private final Object value;

    private final SingleFieldType fieldType;

    public CustomField(@Nonnull CustomFieldDefinition definition, @Nonnull Object value,
                       @Nonnull SingleFieldType fieldType) {
        if (definition == null) {
            throw new NullPointerException("fieldDefinition cannot be null");
        }
        if (value == null) {
            throw new NullPointerException("value cannot be null");
        }

        this.definition = definition;
        this.value = value;
        this.fieldType = fieldType;
    }

    public interface SubmissionField {
        @Nonnull String getFieldName();
    }

    public CustomField(@Nonnull Map<String, CustomFieldDefinition> submissionFields,
                       @Nonnull SubmissionField field,
                       @Nonnull String value) {
        this(submissionFields.get(field.getFieldName()), value, CustomField.SingleFieldType.TEXT);
    }

    public CustomField(@Nonnull Map<String, CustomFieldDefinition> submissionFields,
                       @Nonnull SubmissionField field,
                       boolean value) {
        this(submissionFields.get(field.getFieldName()), value ? "Yes" : "No", SingleFieldType.RADIO_BUTTON);
    }

    public CustomFieldDefinition getFieldDefinition() {
        return definition;
    }

    @Nonnull
    public Object getValue() {
        return value;
    }

    @Nonnull
    public SingleFieldType getFieldType() {
        return fieldType;
    }

    public enum SingleFieldType {
        TEXT,
        RADIO_BUTTON,
        SINGLE_SELECT
    }
}
