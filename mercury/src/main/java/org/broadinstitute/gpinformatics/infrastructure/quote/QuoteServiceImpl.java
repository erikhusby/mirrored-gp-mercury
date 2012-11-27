package org.broadinstitute.gpinformatics.infrastructure.quote;

import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Date;

@Impl
public class QuoteServiceImpl extends AbstractJerseyClientService implements QuoteService {
    @Inject
    private QuoteConfig quoteConfig;

    static final String WORK_ITEM_ID = "workItemId\t";

    public QuoteServiceImpl() {
    }

    /**
     * Non CDI constructor, all dependencies must be explicitly initialized!
     *
     * @param quoteConfig The configuration
     */
    public QuoteServiceImpl(QuoteConfig quoteConfig) {
        this.quoteConfig = quoteConfig;
    }

    enum Endpoint {
        SINGLE_QUOTE("/quotes/ws/portals/private/getquotes?with_funding=true&quote_alpha_ids="),
        ALL_SEQUENCING_QUOTES("/quotes/ws/portals/private/getquotes?platform_name=DNA+Sequencing&with_funding=true"),
        ALL_PRICE_ITEMS("/quotes/ws/portals/private/get_price_list"),
        REGISTER_WORK("/quotes/ws/portals/private/createworkitem"),
        //TODO this next enum value will be removed soon.
        SINGLE_NUMERIC_QUOTE("/quotes/ws/portals/private/getquotes?with_funding=true&quote_ids=");

        String suffixUrl;

        Endpoint(String suffixUrl) {
            this.suffixUrl = suffixUrl;
        }

        public String getSuffixUrl() {
            return suffixUrl;
        }
    }

    private String url(Endpoint endpoint) {
        return quoteConfig.getUrl() + endpoint.getSuffixUrl();
    }

    @Override
    public String registerNewWork(Quote quote, PriceItem priceItem, Date reportedCompletionDate, double numWorkUnits,
                                  String callbackUrl, String callbackParameterName, String callbackParameterValue) {

        // see https://iwww.broadinstitute.org/blogs/quote/?page_id=272 for details
        String url = url(Endpoint.REGISTER_WORK);
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();

        params.add("quote_alpha_id", quote.getAlphanumericId());
        params.add("platform_name", priceItem.getPlatformName());
        params.add("category_name", priceItem.getCategoryName());
        params.add("price_item_name", priceItem.getName());
        params.add("quantity", Double.toString(numWorkUnits));
        params.add("complete", Boolean.TRUE.toString());
        params.add("completion_date", DateUtil.formatDate(reportedCompletionDate));
        params.add("url", callbackUrl);
        params.add("object_type", callbackParameterName);
        params.add("object_value", callbackParameterValue);

        WebResource resource = getJerseyClient().resource(url);
        resource.accept(MediaType.TEXT_PLAIN);
        resource.queryParams(params);
        ClientResponse response = resource.queryParams(params).get(ClientResponse.class);

        return registerNewWork(response, quote, priceItem, numWorkUnits, callbackUrl, callbackParameterName, callbackParameterValue);
    }

    /**
     * Package visibility for negative testing
     *
     * @return The work item id returned (as a string)
     */
    String registerNewWork(@Nonnull ClientResponse response, Quote quote, PriceItem priceItem,
                           double numWorkUnits, String callbackUrl, String callbackParameterName,
                           String callbackParameterValue) {

        if (response == null) {
            throwQuoteServerFailureException(quote, priceItem, numWorkUnits);
        } else {
            if (response.getClientResponseStatus() != ClientResponse.Status.OK) {
                throw new RuntimeException(
                    "Quote server returned " + response.getClientResponseStatus() +
                    ".  registering work for " + numWorkUnits + " of " + priceItem.getName() + "" +
                    " against quote " + quote.getAlphanumericId() + " appears to have failed.");
            }
        }

        String output = response.getEntity(String.class);
        String workItemId = null;

        if (output == null) {
            throwQuoteServerFailureException(quote, priceItem, numWorkUnits);
        }

        if (!output.contains(WORK_ITEM_ID)) {
            StringBuilder builder = new StringBuilder();
            builder.append("Quote server returned:\n").append(output).append("\n")
                   .append(" for ").append(numWorkUnits).append(" of ").append(priceItem.getName())
                    .append(" against quote ").append(quote.getAlphanumericId());
            throw new RuntimeException(builder.toString());
        } else {
            String[] split = output.split(WORK_ITEM_ID);
            if (split.length != 2) {
                throwQuoteServerFailureException(quote, priceItem, numWorkUnits);
            } else {
                workItemId = split[1].trim();
                if (workItemId.isEmpty() || workItemId == null) {
                    throwQuoteServerFailureException(quote, priceItem, numWorkUnits);
                }
            }
        }
        return workItemId;
    }

    private void throwQuoteServerFailureException(Quote quote, PriceItem priceItem, double numWorkUnits) {
        throw new RuntimeException(
                "Quote server did not return the appropriate response.  Registering work for " + numWorkUnits +
                " of " + priceItem.getName() + " against quote " + quote.getAlphanumericId() +
                " appears to have failed.");
    }

    @Override
    protected void customizeConfig(ClientConfig clientConfig) {
        acceptAllServerCertificates(clientConfig);
    }

    @Override
    protected void customizeClient(Client client) {
        client.setFollowRedirects(true);
        specifyHttpAuthCredentials(client, quoteConfig);
        forceResponseMimeTypes(client, MediaType.APPLICATION_XML_TYPE);
    }

    /**
     * Asks the GAP quote server for basic information about a quote.
     *
     * @param id Alphanumeric ID for the quote
     * @return If the quote exists the return value will be a quote object. Otherwise null.
     */
    @Override
    public Quote getQuoteFromQuoteServer(String id) throws QuoteServerException, QuoteNotFoundException {
        return this.getSingleQuoteById(id, url(Endpoint.SINGLE_QUOTE));
    }

    @Override
    public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException {
        String url = url(Endpoint.ALL_PRICE_ITEMS);
        WebResource resource = getJerseyClient().resource(url);
        PriceList prices = null;

        try {
            prices = resource.accept(MediaType.APPLICATION_XML).get(PriceList.class);
        } catch (UniformInterfaceException e) {
            throw new QuoteNotFoundException("Could not find price list at " + url);
        } catch (ClientHandlerException e) {
            throw new QuoteServerException("Could not communicate with quote server", e);
        }

        return prices;
    }

    /**
     * Gets all quotes for the sequencing platform.
     * This is a bit slow.
     *
     * @return The quotes
     *
     * @throws QuoteServerException
     * @throws QuoteNotFoundException
     */
    @Override
    public Quotes getAllSequencingPlatformQuotes() throws QuoteServerException, QuoteNotFoundException {
        String url = url(Endpoint.ALL_SEQUENCING_QUOTES);

        WebResource resource = getJerseyClient().resource(url);

        Quotes quotes = null;
        try {
            quotes = resource.accept(MediaType.APPLICATION_XML).get(Quotes.class);
        } catch (UniformInterfaceException e) {
            throw new QuoteNotFoundException("Could not find quotes for sequencing at " + url);
        } catch (ClientHandlerException e) {
            throw new QuoteServerException("Could not communicate with quote server", e);
        }

        return quotes;
    }

    @Override
    public Quote getQuoteByNumericId(final String numericId) throws QuoteServerException, QuoteNotFoundException {
        return this.getSingleQuoteById(numericId, url(Endpoint.SINGLE_NUMERIC_QUOTE));
    }

    @Override
    public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {
        return this.getSingleQuoteById(alphaId, url(Endpoint.SINGLE_QUOTE));
    }

    /*
    * private method to get a single quote.
    * Can be overridden by mocks.
    * @param id
    * @param queryUrl
    * @return
    * @throws QuoteNotFoundException
    * @throws QuoteServerException
    */
    private Quote getSingleQuoteById(final String id, String url) throws QuoteNotFoundException, QuoteServerException {
        Quote quote;
        if (StringUtils.isEmpty(id)) {
            return (null);
        }

        WebResource resource = getJerseyClient().resource(url + id);

        try {
            Quotes quotes = resource.accept(MediaType.APPLICATION_XML).get(Quotes.class);
            if (quotes.getQuotes() != null && quotes.getQuotes().size() > 0) {
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
}