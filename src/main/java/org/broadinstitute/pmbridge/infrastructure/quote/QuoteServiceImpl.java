package org.broadinstitute.pmbridge.infrastructure.quote;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.lang.StringUtils;
import org.broadinstitute.pmbridge.control.AbstractJerseyClientService;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

@Default
public class QuoteServiceImpl extends AbstractJerseyClientService implements QuoteService {

    @Inject
    private QuoteConnectionParameters connectionParameters;

    public QuoteServiceImpl() {}

    public QuoteServiceImpl(QuoteConnectionParameters quoteConnectionParameters) {
        connectionParameters = quoteConnectionParameters;
    }


    @Override
    protected void customizeConfig(ClientConfig clientConfig) {
        acceptAllServerCertificates(clientConfig);
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, connectionParameters);
        forceResponseMimeTypes(client, MediaType.APPLICATION_XML_TYPE);
    }



    /**
     * Asks the GAP quote server for basic information about a quote.
     *
     * @param id Alphanumeric ID for the quote
     *
     * @return If the quote exists the return value will be a quote object. Otherwise null.
     *
     */
    @Override
    public Quote getQuoteFromQuoteServer(String id) throws QuoteServerException, QuoteNotFoundException {

        Quote quote;
        if(StringUtils.isEmpty(id))
        {
           return(null);
        }

        String url = connectionParameters.getUrl(QuoteConnectionParameters.GET_SINGLE_QUOTE_URL);
        WebResource resource = getJerseyClient().resource(url + id);

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


    /**
     * Gets all quotes
     * This is a bit slow.
     * @return
     * @throws QuoteServerException
     * @throws QuoteNotFoundException
     */
    public Quotes getAllQuotes() throws QuoteServerException, QuoteNotFoundException {

        String url = connectionParameters.getUrl(QuoteConnectionParameters.GET_ALL_QUOTES_URL);
        WebResource resource = getJerseyClient().resource(url);

        Quotes quotes;
        try
        {
           quotes = resource.accept(MediaType.APPLICATION_XML).get(Quotes.class);
        }
        catch(UniformInterfaceException e)
        {
            throw new QuoteNotFoundException("Could not find quotes for sequencing at " + url);
        }
        catch(ClientHandlerException e)
        {
            throw new QuoteServerException("Could not communicate with quote server", e);
        }

        return quotes;
    }

}
