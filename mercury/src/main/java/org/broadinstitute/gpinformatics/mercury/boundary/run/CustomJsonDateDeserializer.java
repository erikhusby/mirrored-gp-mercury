package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Parse JSON dates.
 */
public class CustomJsonDateDeserializer extends JsonDeserializer<Date>
{
    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String date = jsonParser.getText();
        try {
            return format.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}