package org.broadinstitute.pmbridge.control;

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


/**
 * Abstract subclass of {@link AbstractJerseyClientService} with helpful methods for JSON-based RESTful web services.
 *
 */
public abstract class AbstractJsonJerseyClientService extends AbstractJerseyClientService {

    private ObjectMapper objectMapper = new ObjectMapper();
    
    private Log logger = LogFactory.getLog(AbstractJsonJerseyClientService.class);

    /**
     * Write a JSON representation of the bean parameter to a {@link ByteArrayOutputStream} and return.
     *
     * @param bean
     *
     * @return
     * @throws IOException
     */
    protected ByteArrayOutputStream writeValue(Object bean) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        objectMapper.writeValue(baos, bean);

        return baos;
    }


    /**
     * Set the JSON MIME types for both request and response on the {@link WebResource}
     *
     * @param webResource
     *
     * @return
     */
    protected WebResource.Builder setJsonMimeTypes(WebResource webResource) {
        return webResource.
                type(MediaType.APPLICATION_JSON_TYPE).
                accept(MediaType.APPLICATION_JSON_TYPE);

    }


    /**
     * POST a JSON representation of the requestPojo to the specified {@link WebResource} and return a POJO
     * representation of the response.
     *
     * @param webResource
     * @param requestPojo
     * @param responseGenericType
     * @param <T>
     * @return
     * @throws IOException
     */
    protected <T> T post(WebResource webResource, Object requestPojo, GenericType<T> responseGenericType) throws IOException {

        final ByteArrayOutputStream baos = writeValue(requestPojo);

        logger.warn("POST request: " + baos.toString());

        T ret = setJsonMimeTypes(webResource).post(responseGenericType, baos.toString());

        logger.debug("POST response: " + ret);

        return ret;

    }


    /**
     * Return a JSON representation of the response to a GET issued to the specified {@link WebResource}
     * 
     * @param webResource
     * @param genericType
     * @param <T>
     * @return
     */
    protected <T> T get(WebResource webResource, GenericType<T> genericType) {

        return setJsonMimeTypes(webResource).get(genericType);
    }

}
