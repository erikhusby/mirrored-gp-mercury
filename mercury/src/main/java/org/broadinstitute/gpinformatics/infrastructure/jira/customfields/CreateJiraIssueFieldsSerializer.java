package org.broadinstitute.gpinformatics.infrastructure.jira.customfields;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;

import java.io.IOException;

/**
 * Used to serialize custom fields.  We need this because jira custom fields
 * are not portable across instances, so we may have a different
 * {@link org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition#getJiraCustomFieldId()}
 * between dev/test jira and production jira
 */
public class CreateJiraIssueFieldsSerializer extends JsonSerializer<CreateFields> {

    @Override
    public void serialize(CreateFields fields, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("project", fields.getProject());
        jsonGenerator.writeObjectField("issuetype", fields.getIssueType());
        jsonGenerator.writeObjectField("summary", fields.getSummary());

        if (fields.getReporter() != null) {
            jsonGenerator.writeObjectField("reporter", fields.getReporter());
        }

        UpdateJiraIssueUpdateSerializer.writeCustomFields(fields.getCustomFields(), jsonGenerator);

        jsonGenerator.writeEndObject();
    }
}
