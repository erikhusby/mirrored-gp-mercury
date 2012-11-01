package org.broadinstitute.gpinformatics.infrastructure.jira.customfields;

import org.broadinstitute.gpinformatics.infrastructure.jira.issue.UpdateIssueRequest;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import java.io.IOException;

import static org.broadinstitute.gpinformatics.infrastructure.jira.issue.UpdateIssueRequest.Update;

/**
 * @author breilly
 */
public class UpdateJiraIssueUpdateSerializer extends JsonSerializer<Update> {

    @Override
    public void serialize(Update update, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {

        jsonGenerator.writeStartObject();
        for (CustomField customField : update.getCustomFields()) {
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
