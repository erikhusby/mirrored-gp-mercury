package org.broadinstitute.gpinformatics.athena.infrastructure.quote;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteConfig;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.util.HashSet;
import java.util.Set;


public class PMBQuoteServiceImpl extends AbstractJerseyClientService implements PMBQuoteService {

    private org.apache.commons.logging.Log logger = LogFactory.getLog(PMBQuoteServiceImpl.class);

    enum Endpoint {

        SINGLE_QUOTE("/portal/Quote/ws/portals/private/getquotes?with_funding=true&quote_alpha_ids="),
        ALL_SEQUENCING_QUOTES("/quotes/ws/portals/private/getquotes?platform_name=DNA+Sequencing&with_funding=true"),
        ALL_QUOTES("/quotes/ws/portals/private/getquotes?with_funding=true"),
        ALL_PRICE_ITEMS("/quotes/ws/portals/private/get_price_list"),
        REGISTER_WORK("/quotes/ws/portals/private/createworkitem"),
        //TODO this next enum value will be removed soon.
        SINGLE_NUMERIC_QUOTE("/portal/Quote/ws/portals/private/getquotes?with_funding=true&quote_ids=")
        ;

        String suffixUrl;

        Endpoint(String suffixUrl) {
            this.suffixUrl = suffixUrl;
        }

        public String getSuffixUrl() {
            return suffixUrl;
        }

    }


    @Inject
    private QuoteConfig quoteConfig;

    public PMBQuoteServiceImpl() {}

    @Override
    protected void customizeConfig(ClientConfig clientConfig) {
        acceptAllServerCertificates(clientConfig);
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, quoteConfig);
        forceResponseMimeTypes(client, MediaType.APPLICATION_XML_TYPE);
    }


    @Override
    public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {

        return getSingleQuoteById(alphaId, url( Endpoint.SINGLE_QUOTE) );

    }



    @Override
    public Quote getQuoteByNumericId(final String numericId) throws QuoteServerException, QuoteNotFoundException {
        return this.getSingleQuoteById(numericId, url( Endpoint.SINGLE_NUMERIC_QUOTE ));
    }



    private String url( Endpoint endpoint ) {

        return quoteConfig.getUrl() + endpoint.getSuffixUrl();
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
    protected Quote getSingleQuoteById(final String id, String url) throws QuoteNotFoundException, QuoteServerException {
        Quote quote;
        if(StringUtils.isEmpty(id))
        {
           return(null);
        }

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
    @Override
    public Quotes getAllQuotes() throws QuoteServerException, QuoteNotFoundException {

        //TODO probably best to Cache this data some how and refresh frequently async on separate thread.

        String url = url( Endpoint.ALL_QUOTES );
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

        String url = url( Endpoint.ALL_PRICE_ITEMS );
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
