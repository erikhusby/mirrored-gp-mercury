package org.broadinstitute.gpinformatics.infrastructure.jira.customfields;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import java.io.IOException;

import org.broadinstitute.gpinformatics.infrastructure.jira.issue.UpdateFields;

/**
 * @author breilly
 */
public class UpdateJiraIssueUpdateSerializer extends JsonSerializer<UpdateFields> {

    @Override
    public void serialize(UpdateFields fields, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {

        jsonGenerator.writeStartObject();
        for (CustomField customField : fields.getCustomFields()) {
            String fieldId = customField.getFieldDefinition().getJiraCustomFieldId();
            if (CustomField.SingleFieldType.RADIO_BUTTON.equals(customField.getFieldType())) {
                jsonGenerator.writeFieldName(fieldId);
                jsonGenerator.writeStartObject();
                jsonGenerator.writeObjectField("value", customField.getValue());
                jsonGenerator.writeEndObject();
            } else {
                jsonGenerator.writeObjectField(fieldId, customField.getValue());
            }
        }
        jsonGenerator.writeEndObject();
    }
}
