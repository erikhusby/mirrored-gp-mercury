package org.broadinstitute.gpinformatics.infrastructure.salesforce;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Impl
public class SalesforceServiceImpl extends AbstractJerseyClientService implements SalesforceService {

    @Inject
    private SalesforceConfig salesforceConfig;

    @Inject
    private ProductDao productDao;

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
            e.printStackTrace();
        }

        WebResource queryResource = getJerseyClient().resource(salesforceConfig.getApiUrl(instanceUrl)+"/query")
                .queryParam("q","SELECT name, productCode from product2");

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
            e.printStackTrace();
        }
        if(foundProductBase != null) {
            try {
                String recordId = foundProductBase.getJSONObject("attributes").getString("url");
            } catch (JSONException e) {
                e.printStackTrace();
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
            e.printStackTrace();
        }

        URIBuilder productUpdateUriBuilder = new URIBuilder()
                .setScheme("https")
                //TODO: Correct this to separate the host from the scheme in the config
                .setHost(StringUtils.substringAfter(instanceUrl, "https://"));
        HttpUriRequest httpRequest = null;

        if(foundProductBase != null) {

            String recordId = null;
            try {
                recordId = foundProductBase.getJSONObject("attributes").getString("url");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            productUpdateUriBuilder.setPath(recordId);
            httpRequest = new HttpPatch(productUpdateUriBuilder.build());

            ((HttpPatch)httpRequest).setEntity(new StringEntity(productInfo.toString(), ContentType.APPLICATION_JSON));
        } else {

            productUpdateUriBuilder.setPath(salesforceConfig.getApiUri()+"/sobjects/Product2");
            httpRequest = new HttpPost(productUpdateUriBuilder.build());
            ((HttpPost)httpRequest).setEntity(new StringEntity(productInfo.toString(), ContentType.APPLICATION_JSON));
        }

        httpRequest.setHeader("Authorization", "Bearer " + accessToken);
        httpRequest.setHeader("X-PrettyPrint", "1");

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
                                + "and Pricebook2.isStandard = true");




    }
}
