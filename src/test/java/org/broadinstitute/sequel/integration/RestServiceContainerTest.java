package org.broadinstitute.sequel.integration;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.testng.annotations.BeforeMethod;

import java.net.URL;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;
import static org.testng.Assert.fail;

/**
 * An abstract container test for testing restful web services. Currently only
 * pre-configures support for JSON requests and responses. All methods that deal
 * with service paths automatically add slashes to separate separately specified
 * pieces of the path. Therefore, the universal rule when supplying path pieces
 * as method arguments or returning them from abstract method implementations is
 * to omit leading and trailing slashes.
 *
 * @author breilly
 */
public abstract class RestServiceContainerTest extends ContainerTest {

    private static final String SERVLET_MAPPING_PREFIX = "rest";

    // TODO: BEFORE COMMIT! revert to private
    protected ClientConfig clientConfig;

    /**
     * Returns the base path of the resource under test with no leading or
     * trailing slashes. Effectively, the value of the resource's @Path
     * annotation (with leading/trailing slashes removed).
     *
     * @return the resource's base path
     */
    protected abstract String getResourcePath();

    @BeforeMethod(groups = EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    }

    /**
     * Returns a WebResource for a particular service call based on the base
     * application URL (usually injected by Arquillian) and the path for the
     * service method with no leading or trailing slashes. The serviceUrl is
     * effectively the value of the resource method's @Path annotation (with
     * leading/trailing slashes removed).
     *
     * @param baseUrl       the base URL of the deployed application
     * @param serviceUrl    the relative URL of the service method
     * @return a configured WebResource for the service method
     */
    protected WebResource makeWebResource(URL baseUrl, String serviceUrl) {
        return Client.create(clientConfig).resource(baseUrl + SERVLET_MAPPING_PREFIX + "/" + getResourcePath() + "/" + serviceUrl);
    }

    /**
     * Performs a GET on the given WebResource. Automatically throws an
     * assertion failure if the call results in a UniformInterfaceException.
     *
     * @param resource    the web resource to GET
     * @return the response content
     */
    protected String get(WebResource resource) {
        try {
            return resource.accept(APPLICATION_JSON_TYPE).get(String.class);
        } catch (UniformInterfaceException e) {
            fail(e.getResponse().getEntity(String.class), e);
        }
        // Can't technically get here, but javac doesn't understand that fail() is guaranteed to throw a runtime exception
        return null;
    }

    /**
     * Performs a POST on the given WebResource with the given request payload.
     * Automatically throws an assertion failure if the call results in a
     * UniformInterfaceException.
     *
     * @param resource    the web resource to POST to
     * @param request     the request to post
     * @return the response content
     */
    protected String post(WebResource resource, String request) {
        try {
            return resource.type(APPLICATION_JSON_TYPE).accept(APPLICATION_JSON_TYPE).post(String.class, request);
        } catch (UniformInterfaceException e) {
            fail(e.getResponse().getEntity(String.class), e);
        }
        // Can't technically get here, but javac doesn't understand that fail() is guaranteed to throw a runtime exception
        return null;
    }
}
