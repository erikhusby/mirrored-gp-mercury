package org.broadinstitute.gpinformatics.infrastructure.jira;

import org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

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
