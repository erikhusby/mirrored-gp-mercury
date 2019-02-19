package org.broadinstitute.gpinformatics.mercury.control;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

/**
 * Utility class to define common rest helper functions that can assist in most Jersey calls.
 */
public class JerseyUtils {
    private static final int DEFAULT_TIMEOUT_MILLISECONDS = 300000;

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

    public static WebResource getWebResourceBase(String wsUrl, MediaType mediaType) {
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, DEFAULT_TIMEOUT_MILLISECONDS);
        clientConfig.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, DEFAULT_TIMEOUT_MILLISECONDS);
        if (mediaType == MediaType.APPLICATION_JSON_TYPE) {
            clientConfig.getClasses().add(JacksonJsonProvider.class);
        }
        Client client = Client.create(clientConfig);

        return client.resource(wsUrl);
    }

    /**
     * Subclasses can call this to trust all server certificates (Quote service).
     *
     * Code pulled from http://stackoverflow.com/questions/6047996/ignore-self-signed-ssl-cert-using-jersey-client
     *
     * This code is trusting ALL certificates.  This might be made more specific and secure,
     * but we are currently only applying it to the Jersey ClientConfig pointed at the Quote server so
     * this is probably okay.
     *
     */
    public static void acceptAllServerCertificates(ClientConfig config) {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {
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
     * Generates a new client config which has been configured to ignore warnings that a certificate has not been
     * signed.  Mainly useful when testing which is where the existence of an unsigned certificate is most likely
     */
    public static ClientConfig getClientConfigAcceptCertificate() {
        ClientConfig clientConfig = new DefaultClientConfig();
        acceptAllServerCertificates(clientConfig);
        return clientConfig;
    }
}
