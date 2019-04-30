package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJaxRsClientService;
import org.broadinstitute.gpinformatics.mercury.control.JaxRsUtils;
import org.owasp.encoder.Encode;
import org.w3c.dom.Document;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.Format;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
@Default
public class QuoteServiceImpl extends AbstractJaxRsClientService implements QuoteService {

    public static final String COMMUNICATION_ERROR = "Could not communicate with quote server at %s: %s";

    public static final String EXTERNAL_PRICE_LIST_NAME = "GP External Price List";
    public static final String CRSP_PRICE_LIST_NAME = "CRSP";
    public static final String SSF_PRICE_LIST_NAME = "SSF Price List";

    public static final String EFFECTIVE_DATE_FORMAT = "dd/MMM/yyyy";

    private static final long serialVersionUID = 8458283723746937096L;

    @Inject
    private QuoteConfig quoteConfig;

    static final String WORK_ITEM_ID = "workItemId\t";

    private final static Log log = LogFactory.getLog(QuoteServiceImpl.class);


    @SuppressWarnings("unused")
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
        SINGLE_QUOTE_WITH_PRICE_ITEMS("/quotes/ws/portals/private/getquotes?with_funding=true&with_quote_items=true&quote_alpha_ids="),
        ALL_SEQUENCING_QUOTES("/quotes/ws/portals/private/getquotes?platform_name=DNA+Sequencing&with_funding=true"),
        ALL_SSF_PRICE_ITEMS("/quotes/rest/price_list/10/true"),
        ALL_CRSP_PRICE_ITEMS("/quotes/rest/price_list/50/true"),
        ALL_GP_EXTERNAL_PRICE_ITEMS("/quotes/rest/price_list/60/true"),
        ALL_FUNDINGS("/quotes/rest/sql_report/41"),
        REGISTER_WORK("/quotes/ws/portals/private/createworkitem"),
        ALL_QUOTES("/quotes/ws/portals/private/getquotes?with_funding=true"),
        //TODO this next enum value will be removed soon.
        SINGLE_NUMERIC_QUOTE("/quotes/ws/portals/private/getquotes?with_funding=true&quote_ids="),
        REGISTER_BLOCKED_WORK("/quotes/rest/create_blocked_work"),
        PRICE_ITEM_DETAILS("/quotes/rest/getPriceitem");

        String suffixUrl;

        Endpoint(String suffixUrl) {
            this.suffixUrl = suffixUrl;
        }
    }

    private String url(Endpoint endpoint) {
        final StringBuilder constructedUrl = new StringBuilder(quoteConfig.getUrl() + endpoint.suffixUrl);

        return constructedUrl.toString();
    }

    @Override
    public String registerNewWork(Quote quote, QuotePriceItem quotePriceItem, QuotePriceItem itemIsReplacing,
                                  Date reportedCompletionDate, double numWorkUnits,
                                  String callbackUrl, String callbackParameterName, String callbackParameterValue,
                                  BigDecimal priceAdjustment) {

        return registerWorkHelper(quote, quotePriceItem, itemIsReplacing, reportedCompletionDate, numWorkUnits,
                callbackUrl,
                callbackParameterName, callbackParameterValue,priceAdjustment, Endpoint.REGISTER_WORK);
    }

    @Override
    public String registerNewSAPWork(Quote quote, QuotePriceItem quotePriceItem, QuotePriceItem itemIsReplacing,
                                     Date reportedCompletionDate, double numWorkUnits,
                                     String callbackUrl, String callbackParameterName, String callbackParameterValue,
                                     BigDecimal priceAdjustment) {

        return registerWorkHelper(quote, quotePriceItem, itemIsReplacing, reportedCompletionDate, numWorkUnits,
                callbackUrl,
                callbackParameterName, callbackParameterValue, priceAdjustment, Endpoint.REGISTER_BLOCKED_WORK);
    }

    private String registerWorkHelper(Quote quote, QuotePriceItem quotePriceItem, QuotePriceItem itemIsReplacing,
                                      Date reportedCompletionDate, double numWorkUnits, String callbackUrl,
                                      String callbackParameterName, String callbackParameterValue,
                                      BigDecimal priceAdjustment, Endpoint endpoint) {
        Format dateFormat = FastDateFormat.getInstance("MM/dd/yyyy");

        // see https://iwww.broadinstitute.org/blogs/quote/?page_id=272 for details.
        String url = url(endpoint);
        log.info("Quote server endpoint is:  " + url);

        Map<String, String> params = new HashMap<>();

        params.put("quote_alpha_id", quote.getAlphanumericId());
        params.put("platform_name", quotePriceItem.getPlatformName());
        params.put("category_name", quotePriceItem.getCategoryName());

        // Handle replacement item logic.
        if (itemIsReplacing == null) {
            // This is not a replacement item, so just send the price item through.
            params.put("price_item_name", quotePriceItem.getName());
        } else {
            // This IS a replacement item, so send the original price through and the real price item as a replacement.
            params.put("price_item_name", itemIsReplacing.getName());
            params.put("replacement_item_name", quotePriceItem.getName());
        }

        params.put("quantity", String.valueOf(numWorkUnits));
        params.put("complete", Boolean.TRUE.toString());
        params.put("completion_date", dateFormat.format(reportedCompletionDate));
        params.put("url", callbackUrl);
        params.put("object_type", callbackParameterName);
        params.put("object_value", callbackParameterValue);
        if(priceAdjustment != null) {
            params.put("price_adjustment", String.valueOf(priceAdjustment));
        }

        WebTarget resource = getJaxRsClient().target(url);
        for (Map.Entry<String, String> stringStringEntry : params.entrySet()) {
            resource = resource.queryParam(stringStringEntry.getKey(), stringStringEntry.getValue());
        }

        Response response = resource.request(MediaType.TEXT_PLAIN).get();

        String registerNewWork = registerNewWork(response, quote, quotePriceItem, numWorkUnits);
        response.close();
        return registerNewWork;
    }

    /**
     * Package visibility for negative testing.
     *
     * @return The work item id returned (as a string).
     */
    String registerNewWork(@Nonnull Response response, Quote quote, QuotePriceItem quotePriceItem, double numWorkUnits) {

        if (response.getStatusInfo() != Response.Status.OK) {
            throw new RuntimeException(
                    "Quote server returned " + response.getStatusInfo().getStatusCode() + ".  registering work for "
                    + numWorkUnits + " of " + quotePriceItem.getName() + " against quote " + quote.getAlphanumericId()
                    + " appears to have failed.");
        }

        String output = response.readEntity(String.class);
        String workItemId;

        if (output == null) {
            throw newQuoteServerFailureException(quote, quotePriceItem, numWorkUnits);
        }

        if (!output.contains(WORK_ITEM_ID)) {
            String message =
                    "Quote server returned:\n" + output + "\n" + " for " + numWorkUnits + " of " +
                            quotePriceItem.getName() + " against quote " + quote.getAlphanumericId();
            throw new RuntimeException(message);
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

    private static RuntimeException newQuoteServerFailureException(
            Quote quote, QuotePriceItem quotePriceItem, double numWorkUnits) {
        String message = "Quote server did not return the appropriate response.  Registering work for " + numWorkUnits +
                " of " + quotePriceItem.getName() + " against quote " + quote.getAlphanumericId() +
                " appears to have failed.";
        return new RuntimeException(message);
    }

    @Override
    protected void customizeBuilder(ClientBuilder clientBuilder) {
        JaxRsUtils.acceptAllServerCertificates(clientBuilder);
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, quoteConfig);
        forceResponseMimeTypes(client, MediaType.APPLICATION_XML_TYPE);
    }

    @Override
    public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException {
        PriceList allPriceItems = getPriceItemsByList(Endpoint.ALL_SSF_PRICE_ITEMS);
        allPriceItems.getQuotePriceItems().addAll(getPriceItemsByList(Endpoint.ALL_CRSP_PRICE_ITEMS).getQuotePriceItems());
        allPriceItems.getQuotePriceItems().addAll(getPriceItemsByList(Endpoint.ALL_GP_EXTERNAL_PRICE_ITEMS).getQuotePriceItems());
        return allPriceItems;
    }

    private PriceList getPriceItemsByList(Endpoint targetPriceList) throws QuoteNotFoundException, QuoteServerException {
        String url = url(targetPriceList);
        WebTarget resource = getJaxRsClient().target(url);
        PriceList prices;
        try {
            prices = JaxRsUtils.getAndCheck(resource.request(MediaType.APPLICATION_XML), PriceList.class);
        } catch (WebApplicationException e) {
            throw new QuoteNotFoundException("Could not find price list at " + url);
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

        WebTarget resource = getJaxRsClient().target(url);

        Quotes quotes;
        try {
            quotes = JaxRsUtils.getAndCheck(resource.request(MediaType.APPLICATION_XML), Quotes.class);
        } catch (WebApplicationException e) {
            throw new QuoteNotFoundException("Could not find quotes for sequencing at " + url);
        }

        return quotes;
    }

    @Override
    public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {
        return getSingleQuoteById(alphaId, url(Endpoint.SINGLE_QUOTE));
    }

    @Override
    public Quote getQuoteWithPriceItems(String alphaId) throws QuoteServerException, QuoteNotFoundException {
        return getSingleQuoteById(alphaId, url(Endpoint.SINGLE_QUOTE_WITH_PRICE_ITEMS));
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
            WebTarget resource = getJaxRsClient().target(url + URLEncoder.encode(id, ENCODING));

            Quotes quotes = JaxRsUtils.getAndCheck(resource.request(MediaType.APPLICATION_XML), Quotes.class);
            if (! CollectionUtils.isEmpty(quotes.getQuotes())) {
                quote = quotes.getQuotes().get(0);
            } else {
                throw new QuoteNotFoundException("Could not find quote " + Encode.forHtml(id) + " at " + url);
            }
        } catch (WebApplicationException e) {
            throw new QuoteNotFoundException("Could not find quote " + Encode.forHtml(id) + " at " + url);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("URL encoding not supported: '" + ENCODING + "'", e);
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
        WebTarget resource = getJaxRsClient().target(url);

        Quotes quotes;
        try {
            quotes = JaxRsUtils.getAndCheck(resource.request(MediaType.APPLICATION_XML), Quotes.class);
        } catch (WebApplicationException e) {
            throw new QuoteNotFoundException("Could not find any quotes at " + url);
        }

        return quotes;
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
    public Set<Funding> getAllFundingSources() throws QuoteServerException, QuoteNotFoundException {
        String url = url( Endpoint.ALL_FUNDINGS);
        WebTarget resource = getJaxRsClient().target(url);

        try {
            GenericType<Document> document  = new GenericType<Document>() {};
            Document doc = JaxRsUtils.getAndCheck(resource.request(MediaType.APPLICATION_XML), document);
            return Funding.getFundingSet(doc);
        } catch (WebApplicationException e) {
            throw new QuoteNotFoundException("Could not find any quotes at " + url);
        }

    }

    @Override
    public PriceList getPriceItemsForDate(List<QuoteImportItem> targetedPriceItemCriteria)
            throws QuoteServerException, QuoteNotFoundException {

        List<String> orderedPriceItemNames = new ArrayList<>();
        List<String> orderedCategoryNames = new ArrayList<>();
        List<String> orderedPlatformNames = new ArrayList<>();
        List<String> orderedEffectiveDates = new ArrayList<>();

        for (QuoteImportItem targetedPriceItemCriterion : targetedPriceItemCriteria) {

            orderedPriceItemNames.add(targetedPriceItemCriterion.getPriceItem().getName());
            orderedCategoryNames.add(targetedPriceItemCriterion.getPriceItem().getCategory());
            orderedPlatformNames.add(targetedPriceItemCriterion.getPriceItem().getPlatform());
            orderedEffectiveDates.add(FastDateFormat.getInstance(EFFECTIVE_DATE_FORMAT).format(targetedPriceItemCriterion.getWorkCompleteDate()));

            final PriceItem primaryPriceItem =
                    targetedPriceItemCriterion.getProductOrder().determinePriceItemByCompanyCode(targetedPriceItemCriterion.getProductOrder().getProduct());
            orderedPriceItemNames.add(primaryPriceItem.getName());
            orderedCategoryNames.add(primaryPriceItem.getCategory());
            orderedPlatformNames.add(primaryPriceItem.getPlatform());
            orderedEffectiveDates.add(FastDateFormat.getInstance(EFFECTIVE_DATE_FORMAT).format(targetedPriceItemCriterion.getWorkCompleteDate()));

            for (ProductOrderAddOn productOrderAddOn : targetedPriceItemCriterion.getProductOrder().getAddOns()) {
                final PriceItem addonPriceItem =
                        targetedPriceItemCriterion.getProductOrder().determinePriceItemByCompanyCode(productOrderAddOn.getAddOn());
                orderedPriceItemNames.add(addonPriceItem.getName());
                orderedCategoryNames.add(addonPriceItem.getCategory());
                orderedPlatformNames.add(addonPriceItem.getPlatform());
                orderedEffectiveDates.add(FastDateFormat.getInstance(EFFECTIVE_DATE_FORMAT).format(targetedPriceItemCriterion.getWorkCompleteDate()));
            }
        }

        final PriceList priceList;

        Map<String, String> params = new HashMap<>();
        params.put("effective_date", StringUtils.join(orderedEffectiveDates, ";;"));
        params.put("priceitem_name", StringUtils.join(orderedPriceItemNames, ";;"));
        params.put("platform_name", StringUtils.join(orderedPlatformNames, ";;"));
        params.put("category_name", StringUtils.join(orderedCategoryNames, ";;"));

        final String urlString = url(Endpoint.PRICE_ITEM_DETAILS);

        WebTarget resource = getJaxRsClient().target( urlString);

        try {
            for (Map.Entry<String, String> stringStringEntry : params.entrySet()) {
                resource = resource.queryParam(stringStringEntry.getKey(), stringStringEntry.getValue());
            }

            final Response clientResponse = resource.request(MediaType.APPLICATION_XML).get();
            priceList = clientResponse.readEntity(PriceList.class);
            clientResponse.close();

            if(priceList == null) {
                throw new QuoteServerException("No results returned when looking for price items :" + orderedPriceItemNames);
            }
        } catch (WebApplicationException e) {
            final String priceFindErrorMessage = "Could not find specific billing prices for the given work complete dates::";
            log.error(priceFindErrorMessage+urlString, e);
            throw new QuoteServerException(priceFindErrorMessage);
        }

        return priceList;
    }
}
