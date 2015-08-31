package org.broadinstitute.gpinformatics.infrastructure.salesforce;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

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
    public void pushProducts() {

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();

        params.add("grant_type", "password");
        params.add("client_id", salesforceConfig.getClientId());
        params.add("client_secret", salesforceConfig.getSecret());
        params.add("username", salesforceConfig.getLogin());
        params.add("password", salesforceConfig.getPassword());

        Product testProduct = productDao.findByBusinessKey(Product.EXOME_EXPRESS_V2_PART_NUMBER);

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
    }
}
