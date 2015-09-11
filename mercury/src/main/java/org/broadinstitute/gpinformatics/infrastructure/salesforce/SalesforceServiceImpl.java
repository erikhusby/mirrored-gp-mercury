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

    public SalesforceServiceImpl() {
    }

    public SalesforceServiceImpl(SalesforceConfig salesforceConfig) {
        this.salesforceConfig = salesforceConfig;
    }

    @Override
    protected void customizeClient(Client client) {
        client.setFollowRedirects(true);
        specifyHttpAuthCredentials(client, salesforceConfig);
        forceResponseMimeTypes(client, MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public void pushProduct(String exomeExpressV2PartNumber) throws URISyntaxException, IOException {

        HttpClient salesforceClient =
                HttpClientBuilder.create()
                        .setRedirectStrategy(new LaxRedirectStrategy())
//                        .setDefaultHeaders(new BasicHeader())
                        .build();

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();

        params.add("grant_type", "password");
        params.add("client_id", salesforceConfig.getClientId());
        params.add("client_secret", salesforceConfig.getSecret());
        params.add("username", salesforceConfig.getLogin());
        params.add("password", salesforceConfig.getPassword());

        Product testProduct = productDao.findByBusinessKey(exomeExpressV2PartNumber);

        WebResource loginResource = getJerseyClient().resource(salesforceConfig.getLoginUrl()).queryParams(params);
        log.info(loginResource.toString());
        /*
         * 1
         */
        ClientResponse loginResponse = loginResource.type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class);

        String accessToken = null;
        String instanceUrl = null;
        try {
            JSONObject jsonObject = new JSONObject(loginResponse.getEntity(String.class));
            jsonObject.toString();
            accessToken = jsonObject.getString("access_token");

            instanceUrl = jsonObject.getString("instance_url");
        } catch (JSONException e) {
            throw new InformaticsServiceException("Unable to create JSON object from Login response", e);
        }

        WebResource queryResource = getJerseyClient().resource(salesforceConfig.getApiUrl(instanceUrl)+"/query")
                .queryParam("q","SELECT id, name, productCode from product2");
        log.info(queryResource.toString());
        /*
         * 2
         */
        ClientResponse queryResponse = queryResource
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization","Bearer "+accessToken)
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

        URIBuilder productUpdateUriBuilder = new URIBuilder()
                .setScheme("https")
                //TODO: Correct this to separate the host from the scheme in the config
                .setHost(StringUtils.substringAfter(instanceUrl, "https://"));
        HttpUriRequest httpRequest = null;

        if(foundProductBase != null) {

            productUpdateUriBuilder.setPath(recordIdPath);
            httpRequest = new HttpPatch(productUpdateUriBuilder.build());
        } else {

            productUpdateUriBuilder.setPath(salesforceConfig.getApiUri()+"/sobjects/Product2");
            httpRequest = new HttpPost(productUpdateUriBuilder.build());
        }
        ((HttpEntityEnclosingRequest)httpRequest).setEntity(
                new StringEntity(productInfo.toString(), ContentType.APPLICATION_JSON));

        httpRequest.setHeader("Authorization", "Bearer " + accessToken);
        httpRequest.setHeader("X-PrettyPrint", "1");
        log.info(httpRequest.getURI().toString());
        /*
         * 3
         */
        HttpResponse productUpdateHttpResponse = salesforceClient.execute(httpRequest);

        if (Response.Status.fromStatusCode(productUpdateHttpResponse.getStatusLine().getStatusCode()).getFamily()
            != Response.Status.Family.SUCCESSFUL) {
            throw new ResourceException(
                    "Product update did not succeed: " + productUpdateHttpResponse.getStatusLine().getReasonPhrase(),
                    Response.Status.fromStatusCode(productUpdateHttpResponse.getStatusLine().getStatusCode()));
        }

        /*
         * Find and create/update related PriceBookEntries
         */
        WebResource priceListQuery = getJerseyClient().resource(salesforceConfig.getApiUrl(instanceUrl)+"/query")
                .queryParam("q","select id, Product2.id, product2.name, ProductCode, Pricebook2.name, pricebook2.id "
                                + "from PricebookEntry where "
                                + "Product2.ProductCode = '"+testProduct.getPartNumber()+"' "
//                                + "and Pricebook2.isStandard = true"
                );
        log.info(priceListQuery.toString());
        /*
         * 4
         */
        ClientResponse priceListQueryResponse = priceListQuery
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-PrettyPrint", "1").get(ClientResponse.class);

        String productId = null;
        JSONArray priceBookEntryUpdates = new JSONArray();
        Map<String, String> priceBookEntryPaths = new HashMap<>();
        try {

            String entity = priceListQueryResponse.getEntity(String.class);
            JSONObject queryJson = new JSONObject(entity);
            JSONArray records = queryJson.getJSONArray("records");
            if(records != null && records.length() >0) {
                for(int entryIndex = 0; entryIndex < records.length(); entryIndex++) {
                    JSONObject priceBookEntry = records.getJSONObject(entryIndex);
                    String priceBookId  = priceBookEntry.getJSONObject("Pricebook2").getString("Id");
                    productId = recordId;

                    String priceBookEntryId = priceBookEntry.getString("Id");
                    priceBookEntryPaths.put(priceBookEntryId, priceBookEntry.getJSONObject("attributes").getString("url"));

                    JSONObject entryToUpdate = new JSONObject();
                    entryToUpdate.put("Id", priceBookEntryId);
                    entryToUpdate.put("Pricebook2Id", priceBookId);
                    priceBookEntryUpdates.put(entryToUpdate);
                }
            } else {
                WebResource standardPriceBookQuery = getJerseyClient().resource(salesforceConfig.getApiUrl(instanceUrl)+"/query")
                        .queryParam("q", "select id from pricebook2"
//                                         + " where isStandard = true"
                        );
                log.info(standardPriceBookQuery.toString());
                /*
                 * 5
                 */
                ClientResponse priceBookQueryResponse = standardPriceBookQuery.type(MediaType.APPLICATION_JSON_TYPE)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-PrettyPrint", "1").get(ClientResponse.class);

                String bookEntity = priceBookQueryResponse.getEntity(String.class);
                JSONObject bookQueryJson = new JSONObject(bookEntity);
                JSONArray bookRecords = bookQueryJson.getJSONArray("records");

                if (bookRecords == null ||  bookRecords.length() == 0) {
                    throw new InformaticsServiceException("Query for priceBook did not return a standard pricebook");
                }
                for(int bookRecordIndex = 0;bookRecordIndex<bookRecords.length();bookRecordIndex++) {

                    JSONObject entryToUpdate = new JSONObject();
                    entryToUpdate.put("Pricebook2Id", bookRecords.getJSONObject(0).getString("Id"));
                    priceBookEntryUpdates.put(entryToUpdate);
                }

                if(recordId != null) {
                    productId = recordId;
                } else {
                    WebResource queryProductResource = getJerseyClient().resource(salesforceConfig.getApiUrl(instanceUrl)+"/query")
                            .queryParam("q","SELECT id from product2 where productCode = '"+testProduct.getPartNumber()+"'");
                    log.info(queryProductResource.toString());
        /*
         * 6
         */
                    ClientResponse queryProductResponse = queryProductResource
                            .type(MediaType.APPLICATION_JSON_TYPE)
                            .accept(MediaType.APPLICATION_JSON)
                            .header("Authorization","Bearer "+accessToken)
                            .header("X-PrettyPrint", "1").get(ClientResponse.class);

                    String productEntity = queryProductResponse.getEntity(String.class);
                    JSONObject productFoundJSON = new JSONObject(productEntity);
                    JSONArray productRecords = productFoundJSON.getJSONArray("records");
                    if(productRecords == null || productRecords.length() == 0) {
                        throw new InformaticsServiceException("Unable to find prodcut " + testProduct.getPartNumber() +
                                                              " in SalesForce");
                    }
                    productId = productRecords.getJSONObject(0).getString("Id");
                }
            }


            for(int updateEntryIndex = 0; updateEntryIndex<priceBookEntryUpdates.length();updateEntryIndex++) {
                priceBookEntryUpdates.getJSONObject(updateEntryIndex).put("UnitPrice", getProductPrice(testProduct));
                priceBookEntryUpdates.getJSONObject(updateEntryIndex).put("isActive", true);

                priceBookEntryUpdates.getJSONObject(updateEntryIndex).put("Product2Id", productId);

                URIBuilder priceBookEntryUpdateUriBuilder = new URIBuilder()
                        .setScheme("https")
                                //TODO: Correct this to separate the host from the scheme in the config
                        .setHost(StringUtils.substringAfter(instanceUrl, "https://"));
                HttpUriRequest httpPriceBookEntryRequest = null;

                if (priceBookEntryUpdates.getJSONObject(updateEntryIndex).has("Id")) {
                    priceBookEntryUpdateUriBuilder.setPath(priceBookEntryPaths.get(priceBookEntryUpdates.getJSONObject(updateEntryIndex).get("Id")));
                    httpPriceBookEntryRequest = new HttpPatch(priceBookEntryUpdateUriBuilder.build());
                    ((HttpPatch) httpPriceBookEntryRequest)
                            .setEntity(new StringEntity(priceBookEntryUpdates.getJSONObject(updateEntryIndex).toString(),
                                    ContentType.APPLICATION_JSON));
                } else {
                    priceBookEntryUpdateUriBuilder.setPath(salesforceConfig.getApiUri() + "/sobjects/PricebookEntry");
                    httpPriceBookEntryRequest = new HttpPost(priceBookEntryUpdateUriBuilder.build());
                    ((HttpPost) httpPriceBookEntryRequest)
                            .setEntity(new StringEntity(priceBookEntryUpdates.getJSONObject(updateEntryIndex).toString(),
                                    ContentType.APPLICATION_JSON));
                }

                httpPriceBookEntryRequest.setHeader("Authorization", "Bearer " + accessToken);
                httpPriceBookEntryRequest.setHeader("X-PrettyPrint", "1");
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
        } catch (JSONException e) {
            throw new InformaticsServiceException("Unable to create JSON Object for the records related to "
                                                  + "priceListEntry", e);
        }
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
