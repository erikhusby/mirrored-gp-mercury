package org.broadinstitute.sequel.infrastructure.jira;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import java.io.IOException;


/**
 * When applied to an {@link Enum} with
 * {@link org.codehaus.jackson.map.annotate.JsonSerialize}, will convert any underscores
 * in the {@link Enum} instance name to a blank character during JSON serialization.
 * Note this class does not perform deserialization, nor is there currently a need for that.
 *
 */
public class JsonUnderscoreToBlankEnumSerializer extends JsonSerializer<Enum<?>> {


    @Override
    /**
     * Replace all underscores in the value with a blank
     */
    public void serialize(Enum<?> value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeFieldName("name");
        jgen.writeString(value.name().replace('_', ' '));
        jgen.writeEndObject();
    }



}
