package org.broadinstitute.sequel.infrastructure.quote;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.lang.StringUtils;
import org.broadinstitute.sequel.control.AbstractJerseyClientService;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

public class QuoteServiceImpl extends AbstractJerseyClientService implements QuoteService {

    @Inject
    private QuoteConnectionParameters connectionParameters;


    public QuoteServiceImpl(QuoteConnectionParameters quoteConnectionParameters) {
        connectionParameters = quoteConnectionParameters;
    }

    @Override
    public String registerNewWork(Quote quote, PriceItem priceItem, double numWorkUnits, String callbackUrl, String callbackParameterName, String callbackParameterValue) {
        throw new RuntimeException("I haven't been written yet.");
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

        WebResource resource = getJerseyClient().resource(connectionParameters.getUrl() + id);

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

    public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException {
        WebResource resource = getJerseyClient().resource(connectionParameters.getUrl());
        PriceList prices = null;
        try
        {
            prices = resource.accept(MediaType.APPLICATION_XML).get(PriceList.class);
        }
        catch(UniformInterfaceException e)
        {
            throw new QuoteNotFoundException("Could not find price list at " + connectionParameters.getUrl());
        }
        catch(ClientHandlerException e)
        {
            throw new QuoteServerException("Could not communicate with quote server", e);
        }

        return prices;
    }

    /**
     * Gets all quotes for the sequencing platform.
     * This is a bit slow.
     * @return
     * @throws QuoteServerException
     * @throws QuoteNotFoundException
     */
    public Quotes getAllSequencingPlatformQuotes() throws QuoteServerException, QuoteNotFoundException {

        WebResource resource = getJerseyClient().resource(connectionParameters.getUrl());

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
