package org.broadinstitute.gpinformatics.mercury.control;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.security.ApplicationInstance;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;

public abstract class AbstractJerseyClientService implements Serializable {
    private static final long serialVersionUID = 460875882310020779L;

    private transient Client jerseyClient;

    private static final Log logger = LogFactory.getLog(AbstractJerseyClientService.class);

    @Inject
    private Deployment deployment;

    public AbstractJerseyClientService() {}
    
    /**
     * Subclasses can call this to turn on JSON processing support for client calls.
     */
    protected void supportJson(ClientConfig clientConfig) {
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    }

    /**
     * Subclasses can call this to specify the username and password for HTTP Auth
     *
     */
    protected void specifyHttpAuthCredentials(Client client, LoginAndPassword loginAndPassword) {
        client.addFilter(new HTTPBasicAuthFilter(loginAndPassword.getLogin(), loginAndPassword.getPassword()));
    }

    /**
     * Subclasses can call this to force a MIME type on the response if needed (Quote service)
     */
    protected void forceResponseMimeTypes(Client client, final MediaType... mediaTypes) {
        client.addFilter(new ClientFilter() {
            @Override
            public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
                ClientResponse resp = getNext().handle(cr);
                MultivaluedMap<String, String> map = resp.getHeaders();
                List<String> mimeTypes = new ArrayList<>();

                for (MediaType mediaType : mediaTypes) {
                    mimeTypes.add(mediaType.toString());
                }

                map.put("Content-Type", mimeTypes);
                return resp;
            }
        });
    }

    /**
     * Method for subclasses to retrieve the {@link Client} for making webservice calls.
     */
    protected Client getJerseyClient() {
        if (jerseyClient == null) {
            DefaultClientConfig clientConfig = new DefaultClientConfig();
            if(deployment != Deployment.PROD) {
                JerseyUtils.acceptAllServerCertificates(clientConfig);
            }
            customizeConfig(clientConfig);

            jerseyClient = Client.create(clientConfig);
            customizeClient(jerseyClient);
        }
        return jerseyClient;
    }

    /**
     * The default for this is to do nothing, but it can be overridden for custom set up.
     *
     * @param clientConfig The config object
     */
    protected void customizeConfig(ClientConfig clientConfig) {
    }

    /**
     * Template pattern method for subclasses to modify the {@link Client} after it has been created
     */
    protected abstract void customizeClient(Client client);

    /**
     * Returns a query string of the form key=value1&key=value2 etc.
     */
    protected String makeQueryString(@Nonnull String parameterKey, Collection<String> parameters) {
        List<NameValuePair> pairs = new ArrayList<>();
        for (String parameter : parameters) {
            pairs.add(new BasicNameValuePair(parameterKey, parameter));
        }

        return URLEncodedUtils.format(pairs, CharEncoding.UTF_8);
    }

    /**
     * Callback for the #post method
     */
    public interface PostCallback {
        /**
         * BSP data with newlines removed, accounting for trailing tab if necessary
         */
        void callback(String[] bspData);
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

        BufferedReader reader = null;
        try {
            ClientResponse clientResponse =
                    webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, paramString);

            InputStream is = clientResponse.getEntityInputStream();
            reader = new BufferedReader(new InputStreamReader(is));

            Response.Status clientResponseStatus = Response.Status.fromStatusCode(clientResponse.getStatus());

            // Per http://developer.yahoo.com/social/rest_api_guide/http-response-codes.html, BSP should properly be
            // returning a 202 ACCEPTED response for this POST, but in actuality it returns a 200 OK.  Allow
            // for both in the event that the BSP server's behavior is ever corrected.
            if (!EnumSet.of(ACCEPTED, OK).contains(clientResponseStatus)) {
                logger.error("response code " + clientResponse.getStatus() + ": " + reader.readLine());
                return;
            }

            // skip header line
            reader.readLine();

            // what should be the first real data line
            String readLine = reader.readLine();

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

                readLine = reader.readLine();
            }

            is.close();
        } catch (IOException e) {
            logger.error(e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    protected void clearClient() {
        jerseyClient = null;
    }
}
