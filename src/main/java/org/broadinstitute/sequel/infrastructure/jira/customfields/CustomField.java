package org.broadinstitute.sequel.infrastructure.jira.customfields;

public class CustomField {

    private final String fieldId;

    private final String fieldName;

    private final boolean isRequired;

    public CustomField(String fieldId,String fieldName,boolean isRequired) {
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

    public String getName() {
        return fieldName;
    }

    public String getJiraCustomFieldId() {
        return fieldId;
    }

    public boolean isRequired() {
        return isRequired;
    }
}
