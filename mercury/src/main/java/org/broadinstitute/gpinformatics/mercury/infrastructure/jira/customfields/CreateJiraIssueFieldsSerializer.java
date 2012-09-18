package org.broadinstitute.gpinformatics.mercury.infrastructure.jira.customfields;

import org.broadinstitute.gpinformatics.mercury.infrastructure.jira.issue.CreateIssueRequest.Fields;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import java.io.IOException;

/**
 * Used to serialize custom fields.  We need this because jira custom fields
 * are not portable across instances, so we may have a different
 * {@link org.broadinstitute.gpinformatics.mercury.infrastructure.jira.customfields.CustomFieldDefinition#getJiraCustomFieldId()}
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
        for (CustomField customField : fields.getCustomFields()) {
            String jiraFieldName = customField.getFieldDefinition().getJiraCustomFieldId();
            jsonGenerator.writeObjectField(jiraFieldName,customField.getValue());
        }
    }

}
