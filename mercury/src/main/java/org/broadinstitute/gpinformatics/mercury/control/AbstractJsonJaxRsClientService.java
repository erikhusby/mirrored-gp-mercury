package org.broadinstitute.gpinformatics.mercury.control;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.WebApplicationException;
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

        try {
            Response response = setJsonMimeTypes(webResource).post(Entity.json(request));
            T ret = response.readEntity(responseGenericType);
            log.trace("POST response: " + ret);
            response.close();
            JaxRsUtils.throwIfError(response);
            return ret;
        } catch (WebApplicationException e) {
            //TODO   Change to a more defined exception to give the option to set in throws or even catch
            log.error("POST request: " + request, e);
            throw new RuntimeException(e.getResponse().readEntity(String.class), e);
        }
    }

    /**
     * POST a JSON representation of the requestPojo to the specified {@link WebTarget} This method is used when a
     * a post does not expect a response (HTTP Status code in the 200 range)
     */
    protected void post(WebTarget webResource, Object requestPojo) throws IOException {
        String request = writeValue(requestPojo);

        log.trace("POST request: " + request);

        try {
            Response response = setJsonMimeTypes(webResource).post(Entity.json(request));
            response.close();
            JaxRsUtils.throwIfError(response);
        } catch (WebApplicationException e) {
            //TODO  Change to a more defined exception to give the option to set in throws or even catch
            log.error("POST request: " + request, e);
            throw new RuntimeException(e.getResponse().readEntity(String.class), e);
        }
    }

    /**
     * PUT a JSON representation of the requestPojo to the specified {@link WebTarget} and return a POJO
     * representation of the response.
     */
    protected void put(WebTarget webResource, Object requestPojo) throws IOException {
        String request = writeValue(requestPojo);
        log.trace("PUT request: " + request);
        try {
            Response response = setJsonMimeTypes(webResource).put(Entity.json(request));
            response.close();
        } catch (WebApplicationException e) {
            //TODO Change to a more defined exception to give the option to set in throws or even catch
            log.error("PUT request: " + request, e);
            throw new RuntimeException(e.getResponse().readEntity(String.class), e);
        }
    }

    /**
     * Return a JSON representation of the response to a GET issued to the specified {@link WebTarget}
     */
    protected <T> T get(WebTarget webResource, GenericType<T> genericType) {
        try {
            return JaxRsUtils.getAndCheck(setJsonMimeTypes(webResource), genericType);
        } catch (WebApplicationException e) {
            //TODO Change to a more defined exception to give the option to set in throws or even catch
            log.error("GET request" + webResource.getUri(), e);
            throw new RuntimeException(e.getResponse().readEntity(String.class), e);
        }
    }

    /**
     * Return a JSON representation of the response to a GET issued to the specified {@link WebTarget}
     */
    protected void delete(WebTarget webResource) {
        try {
            Response response = setJsonMimeTypes(webResource).delete();
            response.close();
        } catch (WebApplicationException e) {
            //TODO Change to a more defined exception to give the option to set in throws or even catch
            log.error("DELETE request" + webResource.getUri(), e);
            throw new RuntimeException(e.getResponse().readEntity(String.class), e);
        }
    }
}
