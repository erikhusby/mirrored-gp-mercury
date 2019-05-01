package org.broadinstitute.gpinformatics.infrastructure.jira;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility;

import java.io.IOException;

public class JsonLabopsJiraVisibilitySerializer extends JsonSerializer<Visibility> {

    @Override
    public void serialize(Visibility visibility, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeObjectField("value",visibility.getValue().getName());
        jsonGenerator.writeObjectField("type",visibility.getType());
        jsonGenerator.writeEndObject();
    }
}
