package org.broadinstitute.gpinformatics.infrastructure.salesforce;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Impl
public class SalesforceServiceImpl extends AbstractJerseyClientService implements SalesforceService {

    @Inject
    private SalesforceConfig salesforceConfig;

    @Inject
    private ProductDao productDao;

    @Inject
    private PriceListCache priceListCache;

    private static final Log log = LogFactory.getLog(SalesforceServiceImpl.class);
    public final HttpClient salesforceClient;

    public SalesforceServiceImpl() {
        salesforceClient = HttpClientBuilder.create()
                        .setRedirectStrategy(new LaxRedirectStrategy())
                        .build();
    }

    public SalesforceServiceImpl(SalesforceConfig salesforceConfig) {
        this.salesforceConfig = salesforceConfig;
        salesforceClient = HttpClientBuilder.create()
                        .setRedirectStrategy(new LaxRedirectStrategy())
                        .build();
    }

    @Override
    protected void customizeClient(Client client) {
        client.setFollowRedirects(true);
        specifyHttpAuthCredentials(client, salesforceConfig);
        forceResponseMimeTypes(client, MediaType.APPLICATION_JSON_TYPE);
    }

    private static class SalesforceAccessCredentials {
        private String accessToken;
        private String instanceUrl;


        public SalesforceAccessCredentials(String accessToken, String instanceUrl) {
            this.accessToken = accessToken;
            this.instanceUrl = instanceUrl;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getInstanceUrl() {
            return instanceUrl;
        }
    }

    @Override
    public void pushProducts() throws URISyntaxException, IOException {
        Collection<Product> testProducts = productDao.findTopLevelProductsForProductOrder();
        for(Product currentProduct: testProducts) {

            log.info("About to update " + currentProduct.getPartNumber() + ": " + currentProduct.getDisplayName());

            SalesforceAccessCredentials credentials = loginToSalesforce();

            JSONObject foundProductBase = getProductObject(currentProduct, credentials);

            String recordId = null;
            String recordIdPath = null;
            if(foundProductBase != null) {
                try {
                    log.info("Product Object found for " + foundProductBase.getString("Name"));
                    recordIdPath = foundProductBase.getJSONObject("attributes").getString("url");
                    recordId = foundProductBase.getString("Id");
                } catch (JSONException e) {
                    throw new InformaticsServiceException("Unable to create JSON Object for a URL attribute of an product"
                                                          + " JSON object", e);
                }
            }

            log.info("About to update the product Record");
            updateProductRecord(currentProduct, credentials, foundProductBase, recordIdPath);
            log.info("Product record updated in salesforce");

        /*
         * Find and create/update related PriceBookEntries
         */
            log.info("About to update pricebook entries in salesforce");
            updatePricebookEntries(currentProduct, credentials, recordId);
        }
    }

    @Override
    public void pushProduct(String exomeExpressV2PartNumber) throws URISyntaxException, IOException {

        HttpClient salesforceClient =
                HttpClientBuilder.create()
                        .setRedirectStrategy(new LaxRedirectStrategy())
                        .build();
        Product testProduct = productDao.findByBusinessKey(exomeExpressV2PartNumber);

        SalesforceAccessCredentials credentials = loginToSalesforce();


        JSONObject foundProductBase = getProductObject(testProduct, credentials);

        String recordId = null;
        String recordIdPath = null;
        if(foundProductBase != null) {
            try {
                recordIdPath = foundProductBase.getJSONObject("attributes").getString("url");
                recordId = foundProductBase.getString("Id");
            } catch (JSONException e) {
                throw new InformaticsServiceException("Unable to create JSON Object for a URL attribute of an product"
                                                      + " JSON object", e);
            }
        }


        updateProductRecord(testProduct, credentials, foundProductBase, recordIdPath);


        /*
         * Find and create/update related PriceBookEntries
         */
        updatePricebookEntries(testProduct, credentials, recordId);
    }

    public void updatePricebookEntries(Product testProduct,
                                       SalesforceAccessCredentials credentials, String recordId)
            throws URISyntaxException, IOException {

        WebResource priceListQuery = getJerseyClient().resource(salesforceConfig.getApiUrl(credentials.getInstanceUrl())+"/query")
                .queryParam("q","select id, Product2.id, product2.name, ProductCode, Pricebook2.name, pricebook2.id "
                                + "from PricebookEntry where "
                                + "Product2.ProductCode = '"+testProduct.getPartNumber()+"' "
                                + "and Pricebook2.isStandard = true"
                );
        log.info(priceListQuery.toString());
        /*
         * 4
         */
        ClientResponse priceListQueryResponse = priceListQuery
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + credentials.getAccessToken())
                .header("X-PrettyPrint", "1").get(ClientResponse.class);

        String productId = null;
        JSONArray priceBookEntryUpdates = new JSONArray();
        Map<String, String> priceBookEntryPaths = new HashMap<>();
        try {

            String entity = priceListQueryResponse.getEntity(String.class);
            JSONObject queryJson = new JSONObject(entity);
            JSONArray records = queryJson.getJSONArray("records");
            if(records != null && records.length() >0) {
                log.info("Found some records for price book entries");
                for(int entryIndex = 0; entryIndex < records.length(); entryIndex++) {
                    JSONObject priceBookEntry = records.getJSONObject(entryIndex);
                    productId = recordId;

                    String priceBookEntryId = priceBookEntry.getString("Id");
                    log.info("Capturing info for price book entry " + priceBookEntryId + " and product " +
                             priceBookEntry.getJSONObject("Product2").getString("Name"));
                    priceBookEntryPaths.put(priceBookEntryId, priceBookEntry.getJSONObject("attributes").getString("url"));

                    JSONObject entryToUpdate = new JSONObject();
                    entryToUpdate.put("Id", priceBookEntryId);
                    priceBookEntryUpdates.put(entryToUpdate);
                }
            } else {
                log.info("Did not find any priceBookEntries for the product");

                log.info("Looking for the existing Pricebooks");
                JSONArray bookRecords = getPricebookRecords(credentials);

                for(int bookRecordIndex = 0;bookRecordIndex<bookRecords.length();bookRecordIndex++) {

                    log.info("Found " + bookRecords.getJSONObject(bookRecordIndex).getString("Id") + " for pricebook "
                             + bookRecords.getJSONObject(bookRecordIndex).getString("Name"));

                    JSONObject entryToUpdate = new JSONObject();
                    entryToUpdate.put("Pricebook2Id", bookRecords.getJSONObject(bookRecordIndex).getString("Id"));
                    priceBookEntryUpdates.put(entryToUpdate);
                }

                if(recordId != null) {
                    productId = recordId;
                } else {
                    log.info("A product record was created in this session, Find the one that we just created");

                    JSONObject productFoundJSON = getProductObject(testProduct, credentials);
                    if(productFoundJSON != null) {
//                        JSONArray productRecords = productFoundJSON.getJSONArray("records");
//                        if (productRecords == null || productRecords.length() == 0) {
//                            throw new InformaticsServiceException(
//                                    "Unable to find product " + testProduct.getPartNumber() +
//                                    " in SalesForce");
//                        }
                        productId = productFoundJSON.getString("Id");
                    } else {
                        throw new InformaticsServiceException("Unable to find the required product info.");
                    }
                }
            }

            for(int updateEntryIndex = 0; updateEntryIndex<priceBookEntryUpdates.length();updateEntryIndex++) {
//                if (updateEntryIndex == 0) {

                priceBookEntryUpdates.getJSONObject(updateEntryIndex)
                        .put("UnitPrice", getProductPrice(testProduct));
//                }
                log.info("Price book entry being updated/Created with price of " +
                         priceBookEntryUpdates.getJSONObject(updateEntryIndex).getString("UnitPrice"));
                if(!priceBookEntryUpdates.getJSONObject(updateEntryIndex).has("Id")) {
                    log.info("This is a new PricebookEntry record being created");
                    priceBookEntryUpdates.getJSONObject(updateEntryIndex).put("isActive", true);

                    if (!priceBookEntryUpdates.getJSONObject(updateEntryIndex).has("Product2Id")) {
                        priceBookEntryUpdates.getJSONObject(updateEntryIndex).put("Product2Id", productId);
                    }
                    if (updateEntryIndex != 0) {
                        priceBookEntryUpdates.getJSONObject(updateEntryIndex).put("UseStandardPrice", true);
                    }
                } else {
                    log.info("existing price book entry being updated");
                }

                JSONObject currentPricebookEntryUpdate = priceBookEntryUpdates.getJSONObject(updateEntryIndex);

                updatePricebookEntry(credentials, priceBookEntryPaths, currentPricebookEntryUpdate);
            }
        } catch (JSONException e) {
            throw new InformaticsServiceException("Unable to create JSON Object for the records related to "
                                                  + "priceListEntry", e);
        }
    }

    public void updatePricebookEntry(SalesforceAccessCredentials credentials,
                                     Map<String, String> priceBookEntryPaths, JSONObject currentPricebookEntryUpdate)
            throws JSONException, URISyntaxException, IOException {

        URIBuilder priceBookEntryUpdateUriBuilder = new URIBuilder()
                .setScheme("https")
                        //TODO: Correct this to separate the host from the scheme in the config
                .setHost(StringUtils.substringAfter(credentials.getInstanceUrl(), "https://"));
        HttpUriRequest httpPriceBookEntryRequest = null;

        if (currentPricebookEntryUpdate.has("Id")) {
            log.info("Creating a Patch request for the existing price book entry");
            priceBookEntryUpdateUriBuilder.setPath(priceBookEntryPaths.get(currentPricebookEntryUpdate.get("Id")));
            httpPriceBookEntryRequest = new HttpPatch(priceBookEntryUpdateUriBuilder.build());
            ((HttpPatch) httpPriceBookEntryRequest)
                    .setEntity(new StringEntity(currentPricebookEntryUpdate.toString(),
                            ContentType.APPLICATION_JSON));

//            currentPricebookEntryUpdate.remove("Id");

        } else {
            log.info("Creating a Post request for the new price book entry");
            priceBookEntryUpdateUriBuilder.setPath(salesforceConfig.getApiUri() + "/sobjects/PricebookEntry");
            httpPriceBookEntryRequest = new HttpPost(priceBookEntryUpdateUriBuilder.build());
            ((HttpPost) httpPriceBookEntryRequest)
                    .setEntity(new StringEntity(currentPricebookEntryUpdate.toString(),
                            ContentType.APPLICATION_JSON));
        }

        httpPriceBookEntryRequest.setHeader("Authorization", "Bearer " + credentials.getAccessToken());
        httpPriceBookEntryRequest.setHeader("X-PrettyPrint", "1");

        log.info("Price Entry request of: " + currentPricebookEntryUpdate.toString());
        log.info(httpPriceBookEntryRequest.getURI().toString());

        HttpResponse priceBookEntryUpdateHttpResponse = salesforceClient.execute(httpPriceBookEntryRequest);

        if (Response.Status.fromStatusCode(priceBookEntryUpdateHttpResponse.getStatusLine().getStatusCode())
                    .getFamily()
            != Response.Status.Family.SUCCESSFUL) {
            throw new ResourceException(
                    "Price Book Entry update did not succeed: " + priceBookEntryUpdateHttpResponse
                            .getStatusLine().getReasonPhrase(),
                    Response.Status
                            .fromStatusCode(priceBookEntryUpdateHttpResponse.getStatusLine().getStatusCode()));
        }
    }

    public JSONArray getPricebookRecords(SalesforceAccessCredentials credentials)
            throws JSONException {
        WebResource standardPriceBookQuery = getJerseyClient().resource(salesforceConfig.getApiUrl(credentials.getInstanceUrl())+"/query")
                .queryParam("q", "select id, name, isStandard from pricebook2 where isStandard=true"
//                .queryParam("q", "select id, name, isStandard from pricebook2 order by isStandard desc"
                );
        log.info(standardPriceBookQuery.toString());
                /*
                 * 5
                 */
        ClientResponse priceBookQueryResponse = standardPriceBookQuery.type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + credentials.getAccessToken())
                .header("X-PrettyPrint", "1").get(ClientResponse.class);

        String bookEntity = priceBookQueryResponse.getEntity(String.class);
        JSONObject bookQueryJson = new JSONObject(bookEntity);
        JSONArray bookRecords = bookQueryJson.getJSONArray("records");

        if (bookRecords == null ||  bookRecords.length() == 0) {
            throw new InformaticsServiceException("Query for priceBook did not return a standard pricebook");
        }
        return bookRecords;
    }

    public void updateProductRecord(Product testProduct, SalesforceAccessCredentials credentials,
                                    JSONObject foundProductBase, String recordIdPath)
            throws URISyntaxException, IOException {

    /*
     * Update/create the Product Info
     */
        JSONObject productInfo = new JSONObject();
        try {
            productInfo.put("Name", testProduct.getProductName());
            productInfo.put("ProductCode", testProduct.getPartNumber());
            productInfo.put("Description", testProduct.getDescription());
            productInfo.put("IsActive", testProduct.isAvailable());
        } catch (JSONException e) {
            throw new InformaticsServiceException("Unable to create JSON Object to create/update a product", e);
        }

        log.info("Initial product data updated: " + productInfo.toString());
        URIBuilder productUpdateUriBuilder = new URIBuilder()
                .setScheme("https")
                //TODO: Correct this to separate the host from the scheme in the config
                .setHost(StringUtils.substringAfter(credentials.getInstanceUrl(), "https://"));
        HttpUriRequest httpRequest = null;

        if(foundProductBase != null) {
                     log.info("Must do a Patch to update");
            productUpdateUriBuilder.setPath(recordIdPath);
            httpRequest = new HttpPatch(productUpdateUriBuilder.build());
        } else {
            log.info("Must do a post to create the object");
            productUpdateUriBuilder.setPath(salesforceConfig.getApiUri()+"/sobjects/Product2");
            httpRequest = new HttpPost(productUpdateUriBuilder.build());
        }
        ((HttpEntityEnclosingRequest)httpRequest).setEntity(
                new StringEntity(productInfo.toString(), ContentType.APPLICATION_JSON));

        httpRequest.setHeader("Authorization", "Bearer " + credentials.getAccessToken());
        httpRequest.setHeader("X-PrettyPrint", "1");
        log.info(httpRequest.getURI().toString());
        /*
         * 3
         */
        HttpResponse productUpdateHttpResponse = salesforceClient.execute(httpRequest);
        log.info("Update product returned with " + productUpdateHttpResponse.getStatusLine().getStatusCode());
        if (Response.Status.fromStatusCode(productUpdateHttpResponse.getStatusLine().getStatusCode()).getFamily()
            != Response.Status.Family.SUCCESSFUL) {
            throw new ResourceException(
                    "Product update did not succeed: " + productUpdateHttpResponse.getStatusLine().getReasonPhrase(),
                    Response.Status.fromStatusCode(productUpdateHttpResponse.getStatusLine().getStatusCode()));
        }
    }

    public JSONObject getProductObject(Product testProduct,
                                       SalesforceAccessCredentials credentials) {
        String productQuery = "SELECT id, name, productCode from product2 where productCode = '" + testProduct.getPartNumber() + "'";

        WebResource queryResource = getJerseyClient().resource(salesforceConfig.getApiUrl(credentials.getInstanceUrl())+"/query")
                .queryParam("q", productQuery);
        log.info(queryResource.toString());
        /*
         * 2
         */
        ClientResponse queryResponse = queryResource
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization","Bearer "+credentials.getAccessToken())
                .header("X-PrettyPrint", "1").get(ClientResponse.class);

        JSONObject foundProductBase =  null;
        try {
            String entity = queryResponse.getEntity(String.class);
            JSONObject queryJson = new JSONObject(entity);
            JSONArray records = queryJson.getJSONArray("records");
            for(int index = 0; index<records.length();index++) {
                JSONObject productRecord = records.getJSONObject(index);
                if(testProduct.getPartNumber().equals(productRecord.getString("ProductCode"))) {
                    foundProductBase = productRecord;
                    break;
                }
            }
        } catch (JSONException e) {
            throw new InformaticsServiceException("Unable to create JSON Object from product query", e);
        }
        return foundProductBase;
    }

    public SalesforceAccessCredentials loginToSalesforce() {
        SalesforceAccessCredentials credentials = null;

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();

        params.add("grant_type", "password");
        params.add("client_id", salesforceConfig.getClientId());
        params.add("client_secret", salesforceConfig.getSecret());
        params.add("username", salesforceConfig.getLogin());
        params.add("password", salesforceConfig.getPassword());


        WebResource loginResource = getJerseyClient().resource(salesforceConfig.getLoginUrl()).queryParams(params);
        log.info(loginResource.toString());
        /*
         * 1
         */
        ClientResponse loginResponse = loginResource.type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class);

        try {
            JSONObject jsonObject = new JSONObject(loginResponse.getEntity(String.class));

            credentials = new SalesforceAccessCredentials(jsonObject.getString("access_token"),
                    jsonObject.getString("instance_url"));
        } catch (JSONException e) {
            throw new InformaticsServiceException("Unable to create JSON object from Login response", e);
        }
        return credentials;
    }

    private String getProductPrice(Product product) {

        String price = "23.0";
        QuotePriceItem quotePriceItem = priceListCache.findByKeyFields(
                product.getPrimaryPriceItem().getPlatform(), product.getPrimaryPriceItem().getCategory(),
                product.getPrimaryPriceItem().getName());
        if (quotePriceItem != null) {
            price = //"$" +
                    String.format("%.1f", Float.valueOf(quotePriceItem.getPrice()));
        }
        return price;
    }



}
