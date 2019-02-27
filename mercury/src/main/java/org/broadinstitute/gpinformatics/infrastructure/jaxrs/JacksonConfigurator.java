package org.broadinstitute.gpinformatics.infrastructure.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.text.SimpleDateFormat;

/**
 * Derived from: http://stackoverflow.com/questions/4428109/jersey-jackson-json-date-serialization-format-problem-how-to-change-the-form
 *
 * @author breilly
 */
@Provider
@Produces("application/json")
public class JacksonConfigurator implements ContextResolver<ObjectMapper> {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private final ObjectMapper mapper = new ObjectMapper();

    public JacksonConfigurator() {
        mapper.setDateFormat(new SimpleDateFormat(DATE_FORMAT));
    }

    @Override
    public ObjectMapper getContext(Class<?> clazz) {
        return mapper;
    }
}
