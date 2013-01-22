package org.broadinstitute.gpinformatics.infrastructure.quote;

import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;
import org.w3c.dom.Document;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Set;

@Impl
public class PMBQuoteServiceImpl extends AbstractJerseyClientService implements PMBQuoteService {

    private Log logger = LogFactory.getLog(PMBQuoteServiceImpl.class);

    enum Endpoint {

        SINGLE_QUOTE("/portal/Quote/ws/portals/private/getquotes?with_funding=true&quote_alpha_ids="),
        ALL_SEQUENCING_QUOTES("/quotes/ws/portals/private/getquotes?platform_name=DNA+Sequencing&with_funding=true"),
        ALL_QUOTES("/quotes/ws/portals/private/getquotes?with_funding=true"),
        ALL_FUNDINGS("/quotes/rest/sql_report/41"),
        ALL_PRICE_ITEMS("/quotes/rest/price_list/10"),
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

    public PMBQuoteServiceImpl() {
    }

    public PMBQuoteServiceImpl(QuoteConfig quoteConfig) {
        this.quoteConfig = quoteConfig;
    }

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
    public Quote getQuoteByNumericId(String numericId) throws QuoteServerException, QuoteNotFoundException {
        return getSingleQuoteById(numericId, url(Endpoint.SINGLE_NUMERIC_QUOTE));
    }

    private String url(Endpoint endpoint) {
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
    protected Quote getSingleQuoteById(String id, String url) throws QuoteNotFoundException, QuoteServerException {
        Quote quote;
        if (StringUtils.isEmpty(id)) {
            return (null);
        }

        WebResource resource = getJerseyClient().resource(url + id);

        try {
            Quotes quotes = resource.accept(MediaType.APPLICATION_XML).get(Quotes.class);
            if (quotes.getQuotes() != null && !quotes.getQuotes().isEmpty()) {
                quote = quotes.getQuotes().get(0);
            } else {
                throw new QuoteNotFoundException("Could not find quote " + id + " at " + url);
            }
        } catch (UniformInterfaceException e) {
            throw new QuoteNotFoundException("Could not find quote " + id + " at " + url);
        } catch (ClientHandlerException e) {
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
        String url = url( Endpoint.ALL_QUOTES );
        WebResource resource = getJerseyClient().resource(url);

        Quotes quotes;
        try {
            quotes = resource.accept(MediaType.APPLICATION_XML).get(Quotes.class);
        } catch (UniformInterfaceException e) {
            throw new QuoteNotFoundException("Could not find any quotes at " + url);
        } catch (ClientHandlerException e) {
            throw new QuoteServerException("Could not communicate with quote server at " + url, e);
        }

        return quotes;
    }

    @Override
    public Set<Funding> getAllFundingSources() throws QuoteServerException, QuoteNotFoundException, ParserConfigurationException {
        String url = url( Endpoint.ALL_FUNDINGS);
        WebResource resource = getJerseyClient().resource(url);

        try {
            GenericType<Document> document  = new GenericType<Document>() {};
            Document doc = resource.accept(MediaType.APPLICATION_XML).get(document);
            return Funding.getFundingSet(doc);
        } catch (UniformInterfaceException e) {
            throw new QuoteNotFoundException("Could not find any quotes at " + url);
        } catch (ClientHandlerException e) {
            throw new QuoteServerException("Could not communicate with quote server at " + url, e);
        }

    }

    @Override
    public PriceList getPlatformPriceItems(QuotePlatformType quotePlatformType) throws QuoteServerException, QuoteNotFoundException {
        PriceList platformPrices = new PriceList();

        // get all the priceItems and then filter by platform name.
        PriceList allPrices = getAllPriceItems();
        for ( PriceItem priceItem : allPrices.getPriceItems() ) {
            if (priceItem.getPlatformName().equalsIgnoreCase( quotePlatformType.getPlatformName() )) {
                platformPrices.add(priceItem);
            }
        }
        return platformPrices;
    }

    @Override
    public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException {

        String url = url(Endpoint.ALL_PRICE_ITEMS);
        WebResource resource = getJerseyClient().resource(url);
        PriceList prices;
        try {
            prices = resource.accept(MediaType.APPLICATION_XML).get(PriceList.class);
        } catch (UniformInterfaceException e) {
            throw new QuoteNotFoundException("Could not find price list at " + url);
        } catch (ClientHandlerException e) {
            throw new QuoteServerException("Could not communicate with quote server at " + url, e);
        }
        return prices;
    }
}
