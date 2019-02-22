package org.broadinstitute.gpinformatics.mercury.integration;

import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.BeforeMethod;

import javax.enterprise.context.Dependent;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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
@Dependent
public abstract class RestServiceContainerTest extends Arquillian {

    public static final int DEFAULT_FORWARD_PORT = 8443;
    private static final String SERVLET_MAPPING_PREFIX = "rest";
    public static final String JBOSS_HTTPS_PORT_SYSTEM_PROPERTY = "jbossHttpsPort";

//    private ClientConfig clientConfig;

    /**
     * Returns the base path of the resource under test with no leading or
     * trailing slashes. Effectively, the value of the resource's @Path
     * annotation (with leading/trailing slashes removed).
     *
     * @return the resource's base path
     */
    protected abstract String getResourcePath();

    @BeforeMethod(groups = {EXTERNAL_INTEGRATION, ALTERNATIVES, STUBBY, STANDARD})
    public void setUp() throws Exception {
//        clientConfig = new ClientConfig();
//        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
//        clientConfig.property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE);
//        clientConfig.getClasses().add(JacksonJsonProvider.class);
    }

    /**
     * Returns a WebResource for a particular service call based on the base
     * application URL (usually injected by Arquillian) and the path for the
     * service method with no leading or trailing slashes. The serviceUrl is
     * effectively the value of the resource method's @Path annotation (with
     * leading/trailing slashes removed).
     *
     * @param baseUrl    the base URL of the deployed application
     * @param serviceUrl the relative URL of the service method
     *
     * @return a configured WebResource for the service method
     */
    protected WebTarget makeWebResource(URL baseUrl, String serviceUrl) throws MalformedURLException {
        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        JerseyUtils.acceptAllServerCertificates(clientBuilder);

        Client client = clientBuilder.build();
        client.property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE);
        String newUrl = convertUrlToSecure(baseUrl);
        return client.target(
                newUrl + SERVLET_MAPPING_PREFIX + "/" + getResourcePath() + "/" + serviceUrl);
    }

    /**
     * Helper method to convert the given URL (typically generated as an ArquillianResource) to point to the Secure
     * port of the machine that is running this application
     *
     * @throws MalformedURLException
     */
    public static String convertUrlToSecure(URL baseUrl) throws MalformedURLException {
        String port = System.getProperty(JBOSS_HTTPS_PORT_SYSTEM_PROPERTY, String.valueOf(DEFAULT_FORWARD_PORT));
        String returnValue;
            returnValue = new URL("https", baseUrl.getHost(), Integer.valueOf(port),
                    baseUrl.getFile()).toExternalForm();
        return returnValue;
    }

    /**
     * Performs a GET on the given WebResource. Automatically throws an
     * assertion failure if the call results in a UniformInterfaceException.
     *
     * @param resource the web resource to GET
     *
     * @return the response content
     */
    protected String get(WebTarget resource) {
        try {
            return resource.request(APPLICATION_JSON_TYPE).get(String.class);
        } catch (WebApplicationException e) {
            fail("Error with GET: " + e.getResponse().readEntity(String.class), e);
        }
        // Can't technically get here, but javac doesn't understand that fail() is guaranteed to throw a runtime exception
        return null;
    }

    /**
     * Performs a GET on the given WebResource, expecting an error response and
     * returning the UniformInterfaceException. Throws an assertion failure if
     * the call does NOT result in a UniformInterfaceException.
     *
     * @param resource the web resource to GET
     *
     * @return the caught UniformInterfaceException
     */
    protected WebApplicationException getWithError(WebTarget resource) {
        WebApplicationException caught = null;
        try {
            resource.request(APPLICATION_JSON_TYPE).get(String.class);
            fail("Expected UniformInterfaceException not thrown");
        } catch (WebApplicationException e) {
            caught = e;
        }
        return caught;
    }

    /**
     * Convenience method to perform assertions of the status and content of a
     * UniformInterfaceException. Throws and assertion failure if there is a
     * mismatch.
     *
     * @param caught  the caught UniformInterfaceException
     * @param status  the expected status code
     * @param content the expected response content
     */
    protected void assertErrorResponse(WebApplicationException caught, int status, String content) {
        assertThat(caught.getResponse().getStatus(), equalTo(status));
        assertThat(getResponseContent(caught), equalTo(content));
    }

    /**
     * Performs a POST on the given WebResource with the given request payload.
     * Automatically throws an assertion failure if the call results in a
     * UniformInterfaceException.
     *
     * @param resource the web resource to POST to
     * @param request  the request to post
     *
     * @return the response content
     */
    protected String post(WebTarget resource, String request) {
        try {
            return resource.request(APPLICATION_JSON_TYPE).accept(APPLICATION_JSON_TYPE)
                    .post(Entity.entity(request, APPLICATION_JSON_TYPE), String.class);
        } catch (WebApplicationException e) {
            fail("Error with POST: " + e.getResponse().readEntity(String.class), e);
        }
        // Can't technically get here, but javac doesn't understand that fail() is guaranteed to throw a runtime exception
        return null;
    }

    protected String getResponseContent(WebApplicationException caught) {
        return caught.getResponse().readEntity(String.class);
    }

    protected WebTarget addQueryParam(WebTarget resource, String name, List<String> values) {
        for (String value : values) {
            resource = resource.queryParam(name, value);
        }
        return resource;
    }

    protected String toJson(List<String> strings) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String string : strings) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(string).append("\"");
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }
}
