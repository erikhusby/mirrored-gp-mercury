package org.broadinstitute.pmbridge.infrastructure.quote;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.pmbridge.control.AbstractJerseyClientService;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Alternative
public class QuoteServiceImpl extends AbstractJerseyClientService implements QuoteService {

    private org.apache.commons.logging.Log logger = LogFactory.getLog(QuoteServiceImpl.class);

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

    @Override
    public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {

        return this.getSingleQuoteById(alphaId, QuoteConnectionParameters.GET_SINGLE_ALPHA_QUOTE_URL);
    }

    @Override
    public Quote getQuoteByNumericId(final String numericId) throws QuoteServerException, QuoteNotFoundException {
        return this.getSingleQuoteById(numericId, QuoteConnectionParameters.GET_SINGLE_NUMERIC_QUOTE_URL);
    }

    /*
     * protected method to get a single quote.
     * Can be overridden by mocks.
     * @param id
     * @param queryUrl
     * @return
     * @throws QuoteNotFoundException
     * @throws QuoteServerException
     */
    protected Quote getSingleQuoteById(final String id, String queryUrl) throws QuoteNotFoundException, QuoteServerException {
        Quote quote;
        if(StringUtils.isEmpty(id))
        {
           return(null);
        }

        String url = connectionParameters.getUrl(queryUrl);
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
                throw new QuoteNotFoundException("Could not find quote " + id + " at " + url );
            }
        }
        catch(UniformInterfaceException e)
        {
            throw new QuoteNotFoundException("Could not find quote " + id + " at " + url );
        }
        catch(ClientHandlerException e)
        {
            throw new QuoteServerException("Could not communicate with quote server at " + url, e);
        }
        return quote;
    }


    /*
     * protected method to get all quote.
     * Can be overridden by mocks.
     * @return
     * @throws QuoteServerException
     * @throws QuoteNotFoundException
     */
    protected Quotes getAllQuotes() throws QuoteServerException, QuoteNotFoundException {

        //TODO probably best to Cache this data some how and refresh frequently async on separate thread.
        String url = connectionParameters.getUrl(QuoteConnectionParameters.GET_QUOTES_BASE_URL);
        WebResource resource = getJerseyClient().resource(url);

        Quotes quotes;
        try
        {
           quotes = resource.accept(MediaType.APPLICATION_XML).get(Quotes.class);
        }
        catch(UniformInterfaceException e)
        {
            throw new QuoteNotFoundException("Could not find any quotes at " + url);
        }
        catch(ClientHandlerException e)
        {
            throw new QuoteServerException("Could not communicate with quote server at " + url, e);
        }

        return quotes;
    }

    @Override
    public Set<Funding> getAllFundingSources() throws QuoteServerException, QuoteNotFoundException {

        Set<Funding> fundingSources = new HashSet<Funding>();
        for (Quote quote : this.getAllQuotes().getQuotes()) {
            if (quote.getQuoteFunding() != null) {
                if (quote.getQuoteFunding().getFundingLevel() != null) {
                    if (quote.getQuoteFunding().getFundingLevel().getFunding() != null) {
                        final Funding funding = quote.getQuoteFunding().getFundingLevel().getFunding();
                        // Add it , if it has a type and a number associated with it
                        if ( (funding.getFundingType() != null) &&
                            ((funding.getGrantNumber() != null)  || (funding.getPurchaseOrderNumber() != null) ) ) {
                            fundingSources.add(funding);
                        } else {
                            String expiredState = ( quote.getExpired() ? "Expired. " : "Non-Expired. ");
                            logger.info("Ignoring quote  " + quote.getAlphanumericId() + " because it has non-identifiable funding. " + expiredState + " Funds Remaining: " + quote.getQuoteFunding().getFundsRemaining() );
                        }
                    }
                }
            }
        }
        return fundingSources;
    }

    @Override
    public Set<Quote> getQuotesInFundingSource(final Funding fundingSource) throws QuoteServerException, QuoteNotFoundException {

        HashSet<Quote> quotesByFundingSource = new HashSet<Quote>();

        for (Quote quote : this.getAllQuotes().getQuotes()) {
            if (quote.getQuoteFunding() != null) {
                if (quote.getQuoteFunding().getFundingLevel() != null) {
                    if (quote.getQuoteFunding().getFundingLevel().getFunding() != null) {
                        Funding funding = quote.getQuoteFunding().getFundingLevel().getFunding();
                        if ((funding.getGrantDescription() != null) && (funding.getGrantNumber() != null)) {
                            if ( funding.equals(fundingSource)) {
                                quotesByFundingSource.add(quote);
                            }
                        }
                    }
                }
            }
        }
        return quotesByFundingSource;
    }


    @Override
    public PriceList getPlatformPriceItems(final QuotePlatformType quotePlatformType) throws QuoteServerException, QuoteNotFoundException {
        PriceList platformPrices = new PriceList();

        // get all the priceItems and then filter by platform name.
        PriceList allPrices = this.getAllPriceItems();
        for ( PriceItem priceItem : allPrices.getPriceList() ) {
            if (priceItem.getPlatform().equalsIgnoreCase( quotePlatformType.getPlatformName() )) {
                platformPrices.add(priceItem);
            }
        }
        return platformPrices;
    }


    protected PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException {

        String url = connectionParameters.getUrl(QuoteConnectionParameters.GET_ALL_PRICE_ITEMS);
        WebResource resource = getJerseyClient().resource(url);
        PriceList prices = null;
        try
        {
            prices = resource.accept(MediaType.APPLICATION_XML).get(PriceList.class);
        }
        catch(UniformInterfaceException e)
        {
            throw new QuoteNotFoundException("Could not find price list at " + url);
        }
        catch(ClientHandlerException e)
        {
            throw new QuoteServerException("Could not communicate with quote server at " + url, e);
        }
        return prices;
    }


}
