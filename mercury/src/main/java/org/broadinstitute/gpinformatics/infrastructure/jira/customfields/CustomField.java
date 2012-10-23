package org.broadinstitute.gpinformatics.infrastructure.jira.customfields;

public class CustomField {

    private final CustomFieldDefinition definition;

    private final Object value;

    private final SingleFieldType fieldType;


    public CustomField ( CustomFieldDefinition fieldDefinition, Object value, SingleFieldType fieldType ) {
        if (fieldDefinition == null) {
            throw new NullPointerException("fieldDefinition cannot be null");
        }
        if (value == null) {
            throw new NullPointerException("value cannot be null");
        }
        this.definition = fieldDefinition;
        this.value = value;
        this.fieldType = fieldType;
    }

    public CustomFieldDefinition getFieldDefinition() {
        return definition;
    }

    public Object getValue() {
        return value;
    }

    public SingleFieldType getFieldType () {
        return fieldType;
    }

    public enum SingleFieldType {
        TEXT,
        RADIO_BUTTON,
        SINGLE_SELECT;
    }

}
