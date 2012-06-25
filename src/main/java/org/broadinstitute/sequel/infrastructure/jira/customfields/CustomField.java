package org.broadinstitute.sequel.infrastructure.jira.customfields;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import java.io.IOException;

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