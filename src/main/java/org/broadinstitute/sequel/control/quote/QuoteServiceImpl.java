package org.broadinstitute.sequel.control.quote;

import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

import org.apache.commons.lang.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.net.ssl.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class QuoteServiceImpl implements QuoteService {


    @Inject
    private QuoteConnectionParameters connectionParameters;


    private Client client;


    public QuoteServiceImpl(QuoteConnectionParameters params) {
        connectionParameters = params;
    }

    private void ensureAcceptanceOfServerCertificate(ClientConfig config) {

        // code pulled from http://stackoverflow.com/questions/6047996/ignore-self-signed-ssl-cert-using-jersey-client

        // This code is trusting ALL certificates.  This could be made more specific and secure,
        // but we are only applying it to the Jersey ClientConfig pointed at the Quote server so
        // this is probably okay.

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
     * What fresh hell is this? The quote sends us incorrect mime types. So we must create a
     * client response filter that hacks the mime type to be XML so jersey does the right thing.
     */
    synchronized private void initializeClient() {
        if(client == null)
        {
            ClientConfig config = new DefaultClientConfig();

            ensureAcceptanceOfServerCertificate(config);

            client = Client.create(config);

            client.addFilter(new HTTPBasicAuthFilter(connectionParameters.getUsername(), connectionParameters.getPassword()));
            client.addFilter(new ClientFilter() {
                @Override
                public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
                    ClientResponse resp = getNext().handle(cr);
                    MultivaluedMap<String, String> map = resp.getHeaders();
                    List<String> mimeTypes = new ArrayList<String>();
                    mimeTypes.add(MediaType.APPLICATION_XML);
                    map.put("Content-Type", mimeTypes);
                    return resp;
                }
            });
        }
    }



    /**
     * Asks the GAP quote server for basic information about a quote.
     *
     * @param id Alphanumeric ID for the quote
     * @return If the quote exists the return value will be a quote object. Otherwise null.
     */
    @Override
    public Quote getQuoteFromQuoteServer(String id) throws QuoteServerException, QuoteNotFoundException {

        initializeClient();

        Quote quote;
        if(StringUtils.isEmpty(id))
        {
           return(null);
        }


        WebResource resource = client.resource(connectionParameters.getUrl() + id);
        try
        {
            Quotes quotes = resource.accept(MediaType.APPLICATION_XML).get(Quotes.class);

            if(quotes.getQuotes() != null && quotes.getQuotes().size()>0)
            {
                quote = quotes.getQuotes().get(0);
            }
            else
            {
                throw new QuoteNotFoundException("Could not find quote " + id);
            }
        }
        catch(UniformInterfaceException e)
        {
           throw new QuoteNotFoundException("Could not find quote " + id);
        }
        catch(ClientHandlerException e)
        {
            throw new QuoteServerException("Could not communicate with quote server", e);
        }

        return quote;
    }

    public Quotes getAllSequencingPlatformQuotes() throws QuoteServerException, QuoteNotFoundException {

        initializeClient();
        WebResource resource = client.resource(connectionParameters.getUrl());
        Quotes quotes = null;
        try
        {
           quotes = resource.accept(MediaType.APPLICATION_XML).get(Quotes.class);
        }
        catch(UniformInterfaceException e)
        {
            throw new QuoteNotFoundException("Could not find quotes for sequencing at " + connectionParameters.getUrl());
        }
        catch(ClientHandlerException e)
        {
            throw new QuoteServerException("Could not communicate with quote server", e);
        }

        return quotes;
    }

}
