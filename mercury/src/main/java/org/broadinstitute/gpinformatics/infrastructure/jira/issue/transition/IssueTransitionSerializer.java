package org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.UpdateJiraIssueUpdateSerializer;

import java.io.IOException;

public class IssueTransitionSerializer extends JsonSerializer<IssueTransitionRequest> {

    @Override
    public void serialize(IssueTransitionRequest value, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {

        jsonGenerator.writeStartObject();

        if (!value.getFields().getCustomFields().isEmpty()) {
            jsonGenerator.writeFieldName("fields");
            jsonGenerator.writeStartObject();
            UpdateJiraIssueUpdateSerializer.writeCustomFields(value.getFields().getCustomFields(), jsonGenerator);
            jsonGenerator.writeEndObject();
        }

        jsonGenerator.writeFieldName("transition");
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("id", value.getTransition().getId());
        jsonGenerator.writeEndObject();

        writeComment(jsonGenerator, value.getComment());

        jsonGenerator.writeEndObject();
    }

    private static void writeComment(JsonGenerator jsonGenerator, String comment) throws IOException {
        if (comment != null) {
            jsonGenerator.writeFieldName("update");

            jsonGenerator.writeStartObject();
            jsonGenerator.writeArrayFieldStart("comment");

            jsonGenerator.writeStartObject();
            jsonGenerator.writeFieldName("add");

            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("body", comment);

            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
    }
}
