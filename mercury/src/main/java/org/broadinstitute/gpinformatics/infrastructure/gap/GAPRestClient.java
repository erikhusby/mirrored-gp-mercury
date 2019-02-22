package org.broadinstitute.gpinformatics.infrastructure.gap;

import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

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

    public WebTarget getWebResource(String urlString) {
        return getJerseyClient().target(urlString);
    }
}
