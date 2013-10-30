package org.broadinstitute.gpinformatics.infrastructure.jira;

import org.broadinstitute.gpinformatics.infrastructure.jpa.Nameable;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import java.io.IOException;


/**
 * Note this class does not perform deserialization, nor is there currently a need for that.
 */
public class NameableTypeJsonSerializer extends JsonSerializer<Nameable> {

    @Override
    /**
     * Replace all underscores in the value with a blank
     */
    public void serialize(Nameable nameable, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        jgen.writeFieldName("name");
        jgen.writeString(nameable.getName());
        jgen.writeEndObject();
    }
}
