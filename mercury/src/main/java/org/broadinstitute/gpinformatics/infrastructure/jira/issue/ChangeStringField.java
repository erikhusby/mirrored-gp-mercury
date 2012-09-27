package org.broadinstitute.gpinformatics.infrastructure.jira.issue;

public class ChangeStringField {

    private String fieldName;

    private String fieldValue;

    public ChangeStringField(String fieldName,String fieldValue) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }
}
