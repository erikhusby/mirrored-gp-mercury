package org.broadinstitute.gpinformatics.mercury.boundary.run;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Parse JSON dates.
 */
public class CustomJsonDateSerializer extends JsonSerializer<Date> {
    @Override
    public void serialize(Date date, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException, JsonProcessingException {
        SimpleDateFormat format = new SimpleDateFormat(CustomJsonDateDeserializer.DATE_FORMAT);
        jsonGenerator.writeString(format.format(date));
    }
}
