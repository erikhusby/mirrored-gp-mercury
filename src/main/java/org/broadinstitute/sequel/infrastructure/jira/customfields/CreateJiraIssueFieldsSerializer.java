package org.broadinstitute.sequel.infrastructure.jira.customfields;

import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest.Fields;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import java.io.IOException;
import java.util.List;

/**
 * Used to serialize custom fields.  We need this because jira custom fields
 * are not portable across instances, so we may have a different
 * {@link org.broadinstitute.sequel.infrastructure.jira.customfields.CustomFieldDefinition#getJiraCustomFieldId()}
 * between dev/test jira and production jira
 */
public class CreateJiraIssueFieldsSerializer extends JsonSerializer<Fields> {

    @Override
    public void serialize(Fields fields, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("project",fields.getProject());
        jsonGenerator.writeObjectField("issuetype",fields.getIssuetype());
        jsonGenerator.writeObjectField("summary",fields.getSummary());
        jsonGenerator.writeObjectField("description",fields.getDescription());

        writeCustomFields(fields,jsonGenerator);

        jsonGenerator.writeEndObject();
    }

    private void writeCustomFields(Fields fields,JsonGenerator jsonGenerator) throws IOException {
        CustomField protocol = fields.getProtocol();
        if (protocol != null) {
            String protocolFieldName = protocol.getFieldDefinition().getJiraCustomFieldId();
            jsonGenerator.writeObjectField(protocolFieldName,protocol.getValue());
        }
        CustomField workRequestId = fields.getWorkRequestId();
        if (workRequestId != null) {
            String workRequestFieldName = workRequestId.getFieldDefinition().getJiraCustomFieldId();
            jsonGenerator.writeObjectField(workRequestFieldName,workRequestId.getValue());
        }
    }

}
