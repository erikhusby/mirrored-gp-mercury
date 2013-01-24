package org.broadinstitute.gpinformatics.infrastructure.jira.customfields;

import javax.annotation.Nonnull;
import java.util.Map;

public class CustomField {

    @Nonnull
    private final CustomFieldDefinition definition;

    @Nonnull
    private final Object value;

    public static class RadioButton {

        public RadioButton() {
        }

        public RadioButton(boolean value) {
            this.value = value ? "Yes" : "No";
        }

        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public CustomField(@Nonnull CustomFieldDefinition definition, @Nonnull Object value) {
        if (definition == null) {
            throw new NullPointerException("fieldDefinition cannot be null");
        }
        if (value == null) {
            throw new NullPointerException("value cannot be null");
        }

        this.definition = definition;
        this.value = value;
    }

    public interface SubmissionField {
        @Nonnull String getFieldName();
    }

    public CustomField(@Nonnull Map<String, CustomFieldDefinition> submissionFields,
                       @Nonnull SubmissionField field,
                       @Nonnull Object value) {
        this(submissionFields.get(field.getFieldName()), value);
    }

    public CustomField(@Nonnull Map<String, CustomFieldDefinition> submissionFields,
                       @Nonnull SubmissionField field,
                       boolean value) {
        this(submissionFields.get(field.getFieldName()), new RadioButton(value));
    }

    @Nonnull
    public CustomFieldDefinition getFieldDefinition() {
        return definition;
    }

    @Nonnull
    public Object getValue() {
        return value;
    }
}
