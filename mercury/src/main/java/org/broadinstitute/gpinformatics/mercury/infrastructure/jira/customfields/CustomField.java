package org.broadinstitute.gpinformatics.mercury.infrastructure.jira.customfields;

public class CustomField {

    private final CustomFieldDefinition definition;

    private final Object value;


    public CustomField(CustomFieldDefinition fieldDefinition,Object value) {
        if (fieldDefinition == null) {
            throw new NullPointerException("fieldDefinition cannot be null");
        }
        if (value == null) {
            throw new NullPointerException("value cannot be null");
        }
        this.definition = fieldDefinition;
        this.value = value;
    }

    public CustomFieldDefinition getFieldDefinition() {
        return definition;
    }

    public Object getValue() {
        return value;
    }
}