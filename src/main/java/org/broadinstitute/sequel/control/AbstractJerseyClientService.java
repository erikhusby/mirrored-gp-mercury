package org.broadinstitute.sequel.control;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

import javax.net.ssl.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractJerseyClientService {

    private Client jerseyClient;

    public AbstractJerseyClientService() {}
    
    /**
     * Subclasses can call this to turn on JSON processing support for client calls.
     *
     * @param clientConfig
     */
    protected void supportJson(ClientConfig clientConfig) {
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    }

    /**
     * Subclasses can call this to specify the username and password for HTTP Auth
     *
     * @param client
     *
     * @param loginAndPassword
     */
    protected void specifyHttpAuthCredentials(Client client, LoginAndPassword loginAndPassword) {
        client.addFilter(new HTTPBasicAuthFilter(loginAndPassword.getLogin(), loginAndPassword.getPassword()));
    }


    /**
     * Subclasses can call this to force a MIME type on the response if needed (Quote service)
     *
     * @param client
     *
     * @param mediaTypes
     */
    protected void forceResponseMimeTypes(final Client client, final MediaType... mediaTypes) {

        client.addFilter(new ClientFilter() {
            @Override
            public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
                ClientResponse resp = getNext().handle(cr);
                MultivaluedMap<String, String> map = resp.getHeaders();
                List<String> mimeTypes = new ArrayList<String>();

                for (MediaType mediaType : mediaTypes)
                    mimeTypes.add(mediaType.toString());

                map.put("Content-Type", mimeTypes);
                return resp;
            }
        });
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
     * @param config
     */
    protected void acceptAllServerCertificates(ClientConfig config) {


        try {

            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

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
     * Method for subclasses to retrieve the {@link Client} for making webservice calls.
     *
     * @return
     */
    protected Client getJerseyClient() {

        if (jerseyClient == null) {

            DefaultClientConfig clientConfig = new DefaultClientConfig();
            customizeConfig(clientConfig);


            jerseyClient = Client.create(clientConfig);
            customizeClient(jerseyClient);

        }
        return jerseyClient;
    }

    /**
     * Template pattern method for subclasses to customize the {@link ClientConfig} before the {@link Client} is created.
     *
     * @param clientConfig
     */
    protected abstract void customizeConfig(ClientConfig clientConfig);


    /**
     * Template pattern method for subclasses to modify the {@link Client} after it has been created
     *
     * @param client
     */
    protected abstract void customizeClient(Client client);
    
}
