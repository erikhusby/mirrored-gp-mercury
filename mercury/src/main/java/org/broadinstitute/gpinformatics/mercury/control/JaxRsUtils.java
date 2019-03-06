package org.broadinstitute.gpinformatics.mercury.control;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to define common rest helper functions that can assist in most Jersey calls.
 */
public class JaxRsUtils {
    private static final int DEFAULT_TIMEOUT_MILLISECONDS = 300000;

    public static Invocation.Builder getWebResource(String squidWSUrl, MediaType mediaType) {
        WebTarget resource = getWebResourceBase(squidWSUrl);
        return resource.request(mediaType);
    }

    public static Invocation.Builder getWebResource(String wSUrl, MediaType mediaType, Map<String, List<String>> parameters) {
        WebTarget resource = getWebResourceBase(wSUrl);
        for (Map.Entry<String, List<String>> stringListEntry : parameters.entrySet()) {
            for (String s : stringListEntry.getValue()) {
                resource = resource.queryParam(stringListEntry.getKey(), s);
            }
        }
        return resource.request(mediaType);
    }

    public static WebTarget getWebResourceBase(String wsUrl) {
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

    public static <T> T getAndCheck(Invocation.Builder builder, GenericType<T> result) {
        Response response = builder.get();
        throwIfError(response);
        T t = response.readEntity(result);
        response.close();
        return t;
    }

    public static void throwIfError(Response response) {
        if (response.getStatus() >= 300) {
            response.close();
            ClientInvocation.handleErrorStatus(response);
        }
    }

    public static <T> T getAndCheck(Invocation.Builder builder, Class<T> result) {
        Response response = builder.get();
        throwIfError(response);
        T t = response.readEntity(result);
        response.close();
        return t;
    }

    public static <T, U> T postAndCheck(Invocation.Builder builder, Entity<U> entity, GenericType<T> result) {
        Response response = builder.post(entity);
        throwIfError(response);
        T t = response.readEntity(result);
        response.close();
        return t;
    }

    public static <T, U> T postAndCheck(Invocation.Builder builder, Entity<U> entity, Class<T> result) {
        Response response = builder.post(entity);
        throwIfError(response);
        T t = response.readEntity(result);
        response.close();
        return t;
    }

}
