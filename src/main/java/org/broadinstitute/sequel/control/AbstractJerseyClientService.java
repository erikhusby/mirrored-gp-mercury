package org.broadinstitute.sequel.control;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;

public class AbstractJerseyClientService {

    private Client jerseyClient;

    private boolean supportJson = false;
    
    public AbstractJerseyClientService() {}
    
    public AbstractJerseyClientService(boolean supportJson) {
        this.supportJson = supportJson;
    }


    protected Client getClient(UsernameAndPassword usernameAndPassword) {

        if (jerseyClient == null) {

            DefaultClientConfig clientConfig = new DefaultClientConfig();

            if (this.supportJson)
                clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

            jerseyClient = Client.create(clientConfig);

            jerseyClient.addFilter(new HTTPBasicAuthFilter(usernameAndPassword.getUsername(), usernameAndPassword.getPassword()));
        }
        return jerseyClient;
    }
}
