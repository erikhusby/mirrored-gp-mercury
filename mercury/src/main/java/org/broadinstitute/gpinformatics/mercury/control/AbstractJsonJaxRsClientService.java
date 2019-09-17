package org.broadinstitute.gpinformatics.mercury.control;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * Abstract subclass of {@link AbstractJaxRsClientService} with helpful methods for JSON-based RESTful web services.
 *
 */
// FIXME: This code is not OOP, and would be better abstracted as a helper class instead of a base class.
public abstract class AbstractJsonJaxRsClientService extends AbstractJaxRsClientService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final Log log = LogFactory.getLog(AbstractJsonJaxRsClientService.class);

    /**
     * Write a JSON representation of the bean parameter to a {@link String} and return.
     */
    protected String writeValue(Object bean) throws IOException {
        OutputStream outputStream = new ByteArrayOutputStream();
        objectMapper.writeValue(outputStream, bean);
        return outputStream.toString();
    }

    /**
     * Set the JSON MIME types for both request and response on the {@link WebTarget}
     */
    protected Invocation.Builder setJsonMimeTypes(WebTarget webResource) {
        return webResource.request(MediaType.APPLICATION_JSON_TYPE);
    }

    /**
     * POST a JSON representation of the requestPojo to the specified {@link WebTarget} and return a POJO
     * representation of the response.
     */
    protected <T> T post(WebTarget webResource, Object requestPojo, GenericType<T> responseGenericType) throws IOException {
        String request = writeValue(requestPojo);

        log.trace("POST request: " + request);

        Response response = setJsonMimeTypes(webResource).post(Entity.json(request));
        if (response.getStatus() >= 300) {
            String message = response.readEntity(String.class);
            log.error("POST request: " + request);
            response.close();
            throw new RuntimeException(message);
        }
        T ret = response.readEntity(responseGenericType);
        log.trace("POST response: " + ret);
        response.close();
        return ret;
    }

    /**
     * POST a JSON representation of the requestPojo to the specified {@link WebTarget} This method is used when a
     * a post does not expect a response (HTTP Status code in the 200 range)
     */
    protected void post(WebTarget webResource, Object requestPojo) throws IOException {
        String request = writeValue(requestPojo);

        log.trace("POST request: " + request);

        Response response = setJsonMimeTypes(webResource).post(Entity.json(request));
        if (response.getStatus() >= 300) {
            log.error("POST request: " + request);
            String message = response.readEntity(String.class);
            response.close();
            throw new RuntimeException(message);
        }
        response.close();
    }

    /**
     * PUT a JSON representation of the requestPojo to the specified {@link WebTarget} and return a POJO
     * representation of the response.
     */
    protected void put(WebTarget webResource, Object requestPojo) throws IOException {
        String request = writeValue(requestPojo);
        log.trace("PUT request: " + request);
        Response response = setJsonMimeTypes(webResource).put(Entity.json(request));
        if (response.getStatus() >= 300) {
            log.error("PUT request: " + request);
            String message = response.readEntity(String.class);
            response.close();
            throw new RuntimeException(message);
        }
        response.close();
    }

    /**
     * Return a JSON representation of the response to a GET issued to the specified {@link WebTarget}
     */
    protected <T> T get(WebTarget webResource, GenericType<T> genericType) {
        Response response = setJsonMimeTypes(webResource).get();
        if (response.getStatus() >= 300) {
            log.error("GET request" + webResource.getUri());
            String message = response.readEntity(String.class);
            response.close();
            throw new RuntimeException(message);
        }
        T t = response.readEntity(genericType);
        response.close();
        return t;
    }

    /**
     * Return a JSON representation of the response to a GET issued to the specified {@link WebTarget}
     */
    protected void delete(WebTarget webResource) {
        Response response = setJsonMimeTypes(webResource).delete();
        if (response.getStatus() >= 300) {
            log.error("DELETE request" + webResource.getUri());
            String message = response.readEntity(String.class);
            response.close();
            throw new RuntimeException(message);
        }
        response.close();
    }
}
