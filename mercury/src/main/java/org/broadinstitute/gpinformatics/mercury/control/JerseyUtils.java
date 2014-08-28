package org.broadinstitute.gpinformatics.mercury.control;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class JerseyUtils {
    public static WebResource.Builder getWebResource(String squidWSUrl, MediaType mediaType) {
        WebResource resource = getWebResourceBase(squidWSUrl, mediaType);
        return resource.type(mediaType);
    }

    public static WebResource.Builder getWebResource(String wSUrl, MediaType mediaType, Map<String, List<String>> parameters) {
        WebResource resource = getWebResourceBase(wSUrl, mediaType);
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.putAll(parameters);
        resource.queryParams(params);
        return resource.queryParams(params).type(mediaType);
    }

    public static WebResource getWebResourceBase(String squidWSUrl, MediaType mediaType) {
        Client client = Client.create();
        if (mediaType == MediaType.APPLICATION_JSON_TYPE) {
            ClientConfig clientConfig = new DefaultClientConfig();
            clientConfig.getClasses().add(JacksonJsonProvider.class);
            client = Client.create(clientConfig);
        }

        return client.resource(squidWSUrl);
    }
}
