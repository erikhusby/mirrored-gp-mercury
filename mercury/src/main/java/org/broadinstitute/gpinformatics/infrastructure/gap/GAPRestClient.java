package org.broadinstitute.gpinformatics.infrastructure.gap;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

/**
 * This contains common code used by all clients of GAP rest (ie: non-broadcore) services.
 */
@Dependent
public class GAPRestClient extends AbstractJerseyClientService {

    private static final long serialVersionUID = 20150720L;

    @Inject
    private GAPConfig gapConfig;

    public GAPRestClient() {
    }

    public GAPRestClient(GAPConfig gapConfig) {
        this.gapConfig = gapConfig;
    }

    public String getUrl(String urlSuffix) {
        return gapConfig.getUrl("esp/rest/" + urlSuffix);
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, gapConfig);
    }

    public WebResource getWebResource(String urlString) {
        return getJerseyClient().resource(urlString);
    }
}
