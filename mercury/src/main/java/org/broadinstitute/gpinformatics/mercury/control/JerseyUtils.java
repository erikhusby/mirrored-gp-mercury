package org.broadinstitute.gpinformatics.mercury.control;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to define common rest helper functions that can assist in most Jersey calls.
 */
// todo jmt rename
public class JerseyUtils {
    private static final int DEFAULT_TIMEOUT_MILLISECONDS = 300000;

    public static class LoggingFilter implements ClientRequestFilter {
        private static final Logger LOG = Logger.getLogger(LoggingFilter.class.getName());

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            LOG.log(Level.INFO, requestContext.getEntity().toString());
        }
    }

    public static Invocation.Builder getWebResource(String squidWSUrl, MediaType mediaType) {
        WebTarget resource = getWebResourceBase(squidWSUrl, mediaType);
        return resource.request(mediaType);
    }

    public static Invocation.Builder getWebResource(String wSUrl, MediaType mediaType, Map<String, List<String>> parameters) {
        WebTarget resource = getWebResourceBase(wSUrl, mediaType);
//        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
//        params.putAll(parameters);
        for (Map.Entry<String, List<String>> stringListEntry : parameters.entrySet()) {
            resource = resource.queryParam(stringListEntry.getKey(), stringListEntry.getValue());
        }
        return resource.request(mediaType);
    }

    public static WebTarget getWebResourceBase(String wsUrl, MediaType mediaType) {
//        if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
//            clientConfig.getClasses().add(JacksonJsonProvider.class);
//        }

        Client client = new ResteasyClientBuilder()
                .establishConnectionTimeout(DEFAULT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
                .socketTimeout(DEFAULT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS).build();

        return client.target(wsUrl);
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
    public static void acceptAllServerCertificates(ClientBuilder clientBuilder) {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = {
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

            clientBuilder.sslContext(sc).hostnameVerifier((s, sslSession) -> true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a new client config which has been configured to ignore warnings that a certificate has not been
     * signed.  Mainly useful when testing which is where the existence of an unsigned certificate is most likely
     */
    public static ClientBuilder getClientBuilderAcceptCertificate() {
        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        acceptAllServerCertificates(clientBuilder);
        return clientBuilder;
    }

}
