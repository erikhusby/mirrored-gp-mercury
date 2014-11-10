package org.broadinstitute.gpinformatics.infrastructure.quote;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;
import org.w3c.dom.Document;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.util.Set;

@Impl
public class PMBQuoteServiceImpl extends AbstractJerseyClientService implements PMBQuoteService {

    private static final long serialVersionUID = 1824099036573894112L;

    enum Endpoint {

        SINGLE_QUOTE("/portal/Quote/ws/portals/private/getquotes?with_funding=true&quote_alpha_ids="),
        ALL_SEQUENCING_QUOTES("/quotes/ws/portals/private/getquotes?platform_name=DNA+Sequencing&with_funding=true"),
        ALL_QUOTES("/quotes/ws/portals/private/getquotes?with_funding=true"),
        ALL_FUNDINGS("/quotes/rest/sql_report/41"),
        ALL_PRICE_ITEMS("/quotes/rest/price_list/10/true"),
        REGISTER_WORK("/quotes/ws/portals/private/createworkitem");

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
        JerseyUtils.acceptAllServerCertificates(clientConfig);
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, quoteConfig);
        forceResponseMimeTypes(client, MediaType.APPLICATION_XML_TYPE);
    }

    @Override
    public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {
        return getSingleQuoteById(alphaId, url(Endpoint.SINGLE_QUOTE));
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
            throw new QuoteServerException(String.format("Could not communicate with quote server at %s: %s",
                    url, e.getLocalizedMessage()));
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
            throw new QuoteServerException(String.format("Could not communicate with quote server at %s: %s", url,
                                e.getLocalizedMessage()));

        }

        return quotes;
    }

    @Override
    public Set<Funding> getAllFundingSources() throws QuoteServerException, QuoteNotFoundException {
        String url = url( Endpoint.ALL_FUNDINGS);
        WebResource resource = getJerseyClient().resource(url);

        try {
            GenericType<Document> document  = new GenericType<Document>() {};
            Document doc = resource.accept(MediaType.APPLICATION_XML).get(document);
            return Funding.getFundingSet(doc);
        } catch (UniformInterfaceException e) {
            throw new QuoteNotFoundException("Could not find any quotes at " + url);
        } catch (ClientHandlerException e) {
            throw new QuoteServerException(String.format("Could not communicate with quote server at %s: %s", url,
                                e.getLocalizedMessage()));
        }

    }

    @Override
    public PriceList getPlatformPriceItems(QuotePlatformType quotePlatformType) throws QuoteServerException, QuoteNotFoundException {
        PriceList platformPrices = new PriceList();

        // get all the priceItems and then filter by platform name.
        PriceList allPrices = getAllPriceItems();
        for ( QuotePriceItem quotePriceItem : allPrices.getQuotePriceItems() ) {
            if (quotePriceItem.getPlatformName().equalsIgnoreCase( quotePlatformType.getPlatformName() )) {
                platformPrices.add(quotePriceItem);
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
            throw new QuoteServerException(String.format("Could not communicate with quote server at %s: %s", url,
                                e.getLocalizedMessage()));

        }
        return prices;
    }
}
