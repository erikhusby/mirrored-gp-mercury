package org.broadinstitute.gpinformatics.infrastructure.salesforce;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Impl
public class SalesforceServiceImpl extends AbstractJerseyClientService implements SalesforceService {

    @Inject
    private SalesforceConfig salesforceConfig;

    public SalesforceServiceImpl() {
    }

    @Override
    protected void customizeClient(Client client) {
        client.setFollowRedirects(true);
        specifyHttpAuthCredentials(client, salesforceConfig);
        forceResponseMimeTypes(client, MediaType.APPLICATION_XML_TYPE);
    }

    public void pushProducts() {

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();

        params.add("grant_type", "password");
        params.add("client_id", salesforceConfig.getClientId());
        params.add("client_secret", salesforceConfig.getSecret());
        params.add("username", salesforceConfig.getLogin());
        params.add("password", salesforceConfig.getPassword());



        WebResource resource = getJerseyClient().resource(salesforceConfig.getApiUrl()).queryParams(params);

    }
}
