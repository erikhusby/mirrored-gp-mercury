package org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition;

import java.util.List;

/**
* @author Scott Matthews
*         Date: 10/11/12
*         Time: 9:31 AM
*/
public class TransitionFields {

    private String required;
    private FieldSchema schema;
    private String name;
    private List<String> operations;
    private List<AllowedValues> allowedValues;
    private String autoCompleteUrl;
    private String fieldId;

    public TransitionFields() {
    }

    public String getRequired() {
        return required;
    }

    public void setRequired(String required) {
        this.required = required;
    }

    public FieldSchema getSchema() {
        return schema;
    }

    public void setSchema(FieldSchema schema) {
        this.schema = schema;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getOperations() {
        return operations;
    }

    public void setOperations(List<String> operations) {
        this.operations = operations;
    }

    public List<AllowedValues> getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(List<AllowedValues> allowedValues) {
        this.allowedValues = allowedValues;
    }

    public String getAutoCompleteUrl() {
        return autoCompleteUrl;
    }

    public void setAutoCompleteUrl(String autoCompleteUrl) {
        this.autoCompleteUrl = autoCompleteUrl;
    }

    public String getFieldId() { return fieldId; }

    public void setFieldId(String fieldId) { this.fieldId = fieldId; }
}
