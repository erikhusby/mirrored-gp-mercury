package org.broadinstitute.gpinformatics.mercury.control;

import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Nonnull;
import javax.net.ssl.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;

public abstract class AbstractJerseyClientService implements Serializable {

    private transient Client jerseyClient;

    private static final Log logger = LogFactory.getLog(AbstractJerseyClientService.class);

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
     * The default for this is to do nothing, but it can be overriden for custom set up
     *
     * @param clientConfig The config object
     */
    protected void customizeConfig(ClientConfig clientConfig) {
        // noop
    }

    /**
     * Template pattern method for subclasses to modify the {@link Client} after it has been created
     *
     * @param client
     */
    protected abstract void customizeClient(Client client);

    /**
     * Callback for the #post method
     */
    public interface PostCallback {
        /**
         * BSP data with newlines removed, accounting for trailing tab if necessary
         *
         * @param bspData
         */
        public void callback(String [] bspData);
    }

    /**
     * Strongly typed extra tab flag
     */
    public enum ExtraTab {
        TRUE,
        FALSE
    }


    /**
     * Post method.
     *
     * @param urlString Base URL.
     * @param paramString Parameter string with embedded ampersands, <b>without</b> initial question mark.
     * @param extraTab Extra tab flag, strip a trailing tab if this is present.
     * @param callback Callback method to feed data.
     */
    public void post(@Nonnull String urlString, @Nonnull String paramString, @Nonnull ExtraTab extraTab, @Nonnull PostCallback callback) {

        logger.debug(String.format("URL string is '%s'", urlString));
        WebResource webResource = getJerseyClient().resource(urlString);

        try {

            ClientResponse clientResponse =
                    webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, paramString);

            InputStream is = clientResponse.getEntityInputStream();
            BufferedReader rdr = new BufferedReader(new InputStreamReader(is));

            Response.Status clientResponseStatus = Response.Status.fromStatusCode(clientResponse.getStatus());

            // Per http://developer.yahoo.com/social/rest_api_guide/http-response-codes.html, BSP should properly be
            // returning a 202 ACCEPTED response for this POST, but in actuality it returns a 200 OK.  I'm allowing
            // for both in the event that the BSP server's behavior is ever corrected.
            if (!EnumSet.of(ACCEPTED, OK).contains(clientResponseStatus)) {
                logger.error("response code " + clientResponse.getStatus() + ": " + rdr.readLine());
                return;
            }

            // skip header line
            rdr.readLine();

            // what should be the first real data line
            String readLine = rdr.readLine();

            while (readLine != null) {

                String[] bspOutput = readLine.split("\t", -1);

                String[] truncatedData;

                // BSP WS sometimes puts a superfluous tab at the end, if this is such a WS set extraTab = true
                if (extraTab == ExtraTab.TRUE) {
                    truncatedData = new String[bspOutput.length - 1];
                    System.arraycopy(bspOutput, 0, truncatedData, 0, truncatedData.length);
                } else {
                    truncatedData = bspOutput;
                }

                callback.callback(truncatedData);

                readLine = rdr.readLine();
            }

            is.close();
        } catch (IOException e) {

            logger.error(e);
            return;
        }
    }
}
