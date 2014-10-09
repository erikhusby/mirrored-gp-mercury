package org.broadinstitute.gpinformatics.mercury.integration;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.BeforeMethod;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.ALTERNATIVES;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STANDARD;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STUBBY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
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
public abstract class RestServiceContainerTest extends Arquillian {

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

    @BeforeMethod(groups = {EXTERNAL_INTEGRATION, ALTERNATIVES, STUBBY, STANDARD})
    public void setUp() throws Exception {
        clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        acceptAllServerCertificates(clientConfig);
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
    protected WebResource makeWebResource(URL baseUrl, String serviceUrl) {
        return Client.create(clientConfig).resource(
                baseUrl + SERVLET_MAPPING_PREFIX + "/" + getResourcePath() + "/" + serviceUrl);
    }

    /**
     * Subclasses can call this to trust all server certificates (Quote service).
     * <p/>
     * Code pulled from http://stackoverflow.com/questions/6047996/ignore-self-signed-ssl-cert-using-jersey-client
     * <p/>
     * This code is trusting ALL certificates.  This might be made more specific and secure,
     * but we are currently only applying it to the Jersey ClientConfig pointed at the Quote server so
     * this is probably okay.
     */
    public static void acceptAllServerCertificates(ClientConfig config) {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }};
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());

            config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(
                    new HostnameVerifier() {
                        @Override
                        public boolean verify(String s, SSLSession sslSession) {
                            return true;
                        }
                    }, sc
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Performs a GET on the given WebResource. Automatically throws an
     * assertion failure if the call results in a UniformInterfaceException.
     *
     * @param resource the web resource to GET
     *
     * @return the response content
     */
    protected String get(WebResource resource) {
        try {
            return resource.accept(APPLICATION_JSON_TYPE).get(String.class);
        } catch (UniformInterfaceException e) {
            fail("Error with GET: " + e.getResponse().getEntity(String.class), e);
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
    protected UniformInterfaceException getWithError(WebResource resource) {
        UniformInterfaceException caught = null;
        try {
            resource.accept(APPLICATION_JSON_TYPE).get(String.class);
            fail("Expected UniformInterfaceException not thrown");
        } catch (UniformInterfaceException e) {
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
    protected void assertErrorResponse(UniformInterfaceException caught, int status, String content) {
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
    protected String post(WebResource resource, String request) {
        try {
            return resource.type(APPLICATION_JSON_TYPE).accept(APPLICATION_JSON_TYPE).post(String.class, request);
        } catch (UniformInterfaceException e) {
            fail("Error with POST: " + e.getResponse().getEntity(String.class), e);
        }
        // Can't technically get here, but javac doesn't understand that fail() is guaranteed to throw a runtime exception
        return null;
    }

    protected String getResponseContent(UniformInterfaceException caught) {
        return caught.getResponse().getEntity(String.class);
    }

    protected WebResource addQueryParam(WebResource resource, String name, List<String> values) {
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
