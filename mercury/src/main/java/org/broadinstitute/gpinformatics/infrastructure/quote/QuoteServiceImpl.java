package org.broadinstitute.gpinformatics.infrastructure.quote;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.Format;
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
     * @param quoteConfig The configuration.
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

    }

    private String url(Endpoint endpoint) {
        return quoteConfig.getUrl() + endpoint.suffixUrl;
    }

    @Override
    public String registerNewWork(Quote quote, QuotePriceItem quotePriceItem, QuotePriceItem itemIsReplacing,
                                  Date reportedCompletionDate, double numWorkUnits,
                                  String callbackUrl, String callbackParameterName, String callbackParameterValue) {

        Format dateFormat = FastDateFormat.getInstance("MM/dd/yyyy");

        // see https://iwww.broadinstitute.org/blogs/quote/?page_id=272 for details.
        String url = url(Endpoint.REGISTER_WORK);
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();

        params.add("quote_alpha_id", quote.getAlphanumericId());
        params.add("platform_name", quotePriceItem.getPlatformName());
        params.add("category_name", quotePriceItem.getCategoryName());

        // Handle replacement item logic.
        if (itemIsReplacing == null) {
            // This is not a replacement item, so just send the price item through.
            params.add("price_item_name", quotePriceItem.getName());
        } else {
            // This IS a replacement item, so send the original price through and the real price item as a replacement.
            params.add("price_item_name", itemIsReplacing.getName());
            params.add("replacement_item_name", quotePriceItem.getName());
        }

        params.add("quantity", String.valueOf(numWorkUnits));
        params.add("complete", Boolean.TRUE.toString());
        params.add("completion_date", dateFormat.format(reportedCompletionDate));
        params.add("url", callbackUrl);
        params.add("object_type", callbackParameterName);
        params.add("object_value", callbackParameterValue);

        WebResource resource = getJerseyClient().resource(url);
        resource.accept(MediaType.TEXT_PLAIN);
        resource.queryParams(params);
        ClientResponse response = resource.queryParams(params).get(ClientResponse.class);
        if (response == null) {
            throw newQuoteServerFailureException(quote, quotePriceItem, numWorkUnits);
        }

        return registerNewWork(response, quote, quotePriceItem, numWorkUnits);
    }

    /**
     * Package visibility for negative testing.
     *
     * @return The work item id returned (as a string).
     */
    String registerNewWork(@Nonnull ClientResponse response, Quote quote, QuotePriceItem quotePriceItem, double numWorkUnits) {

        if (response.getClientResponseStatus() != ClientResponse.Status.OK) {
            throw new RuntimeException(
                    "Quote server returned " + response.getClientResponseStatus() + ".  registering work for "
                    + numWorkUnits + " of " + quotePriceItem.getName() + " against quote " + quote.getAlphanumericId()
                    + " appears to have failed.");
        }

        String output = response.getEntity(String.class);
        String workItemId;

        if (output == null) {
            throw newQuoteServerFailureException(quote, quotePriceItem, numWorkUnits);
        }

        if (!output.contains(WORK_ITEM_ID)) {
            StringBuilder builder = new StringBuilder();
            builder.append("Quote server returned:\n").append(output).append("\n")
                    .append(" for ").append(numWorkUnits).append(" of ").append(quotePriceItem.getName())
                    .append(" against quote ").append(quote.getAlphanumericId());
            throw new RuntimeException(builder.toString());
        }
        String[] split = output.split(WORK_ITEM_ID);
        if (split.length != 2) {
            throw newQuoteServerFailureException(quote, quotePriceItem, numWorkUnits);
        }
        workItemId = split[1].trim();
        if (workItemId.isEmpty()) {
            throw newQuoteServerFailureException(quote, quotePriceItem, numWorkUnits);
        }
        return workItemId;
    }

    private static RuntimeException newQuoteServerFailureException(Quote quote, QuotePriceItem quotePriceItem,
                                                                   double numWorkUnits) {
        return new RuntimeException(
                "Quote server did not return the appropriate response.  Registering work for " + numWorkUnits +
                " of " + quotePriceItem.getName() + " against quote " + quote.getAlphanumericId() +
                " appears to have failed.");
    }

    @Override
    protected void customizeConfig(ClientConfig clientConfig) {
        JerseyUtils.acceptAllServerCertificates(clientConfig);
    }

    @Override
    protected void customizeClient(Client client) {
        client.setFollowRedirects(true);
        specifyHttpAuthCredentials(client, quoteConfig);
        forceResponseMimeTypes(client, MediaType.APPLICATION_XML_TYPE);
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

        Quotes quotes;
        try {
            quotes = resource.accept(MediaType.APPLICATION_XML).get(Quotes.class);
        } catch (UniformInterfaceException e) {
            throw new QuoteNotFoundException("Could not find quotes for sequencing at " + url);
        } catch (ClientHandlerException e) {
            throw new QuoteServerException(String.format("Could not communicate with quote server at %s: %s", url,
                                e.getLocalizedMessage()));

        }

        return quotes;
    }

    @Override
    public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {
        return getSingleQuoteById(alphaId, url(Endpoint.SINGLE_QUOTE));
    }

    /**
    * private method to get a single quote. Can be overridden by mocks.
    *
    * @param id The quote identifier.
    * @param url The url to use for the quote server.
     *
    * @return The quote found.
    * @throws QuoteNotFoundException Error when quote is not found.
    * @throws QuoteServerException Any other error with the quote server.
    **/
    private Quote getSingleQuoteById(String id, String url) throws QuoteNotFoundException, QuoteServerException {
        Quote quote;
        if (StringUtils.isEmpty(id)) {
            return (null);
        }

        final String ENCODING = "UTF-8";

        try {
            WebResource resource = getJerseyClient().resource(url + URLEncoder.encode(id, ENCODING));

            Quotes quotes = resource.accept(MediaType.APPLICATION_XML).get(Quotes.class);
            if (! CollectionUtils.isEmpty(quotes.getQuotes())) {
                quote = quotes.getQuotes().get(0);
            } else {
                throw new QuoteNotFoundException("Could not find quote " + id + " at " + url);
            }
        } catch (UniformInterfaceException e) {
            throw new QuoteNotFoundException("Could not find quote " + id + " at " + url);
        } catch (ClientHandlerException e) {
            throw new QuoteServerException(String.format("Could not communicate with quote server at %s: %s", url,
                                e.getLocalizedMessage()));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("URL encoding not supported: '" + ENCODING + "'", e);
        }

        return quote;
    }
}
