package org.broadinstitute.sequel.control.jira;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import java.io.IOException;


/**
 * When applied to an enum with {@link org.codehaus.jackson.map.annotate.JsonSerialize} will convert any underscores
 * in the {@link Enum} instance to whitespace for JSON serialization.   Note this is not currently concerned with
 * deserialization.
 *
 */
public class JsonUnderscoreToWhitespaceEnumSerializer extends JsonSerializer<Enum<?>> {


    @Override
    /**
     * Replace all underscores in the value with whitespace
     */
    public void serialize(Enum<?> value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeFieldName("name");
        jgen.writeString(value.name().replace('_', ' '));
        jgen.writeEndObject();
    }
}
