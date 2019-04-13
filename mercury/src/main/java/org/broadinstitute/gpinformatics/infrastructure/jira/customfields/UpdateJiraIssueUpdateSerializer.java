package org.broadinstitute.gpinformatics.infrastructure.jira.customfields;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.UpdateFields;

import java.io.IOException;
import java.util.Collection;

/**
 * @author breilly
 */
public class UpdateJiraIssueUpdateSerializer extends JsonSerializer<UpdateFields> {

    @Override
    public void serialize(UpdateFields fields, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        writeCustomFields(fields.getCustomFields(), jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    public static void writeCustomFields(Collection<CustomField> customFields, JsonGenerator jsonGenerator) throws IOException {
        for (CustomField customField : customFields) {
            String fieldId = customField.getFieldDefinition().getJiraCustomFieldId();
            jsonGenerator.writeObjectField(fieldId, customField.getValue());
        }
    }
}
