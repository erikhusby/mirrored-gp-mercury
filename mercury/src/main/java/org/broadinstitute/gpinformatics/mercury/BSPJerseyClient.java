package org.broadinstitute.gpinformatics.mercury;

import com.sun.jersey.api.client.Client;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.inject.Inject;

/**
 * This contains common code used by all clients of BSP web services.
 */
public abstract class BSPJerseyClient extends AbstractJerseyClientService {

    private static final long serialVersionUID = 5472586820069306030L;

    @Inject
    private BSPConfig bspConfig;

    public BSPJerseyClient() {
    }

    public BSPJerseyClient(BSPConfig bspConfig) {
        this.bspConfig = bspConfig;
    }

    protected String getUrl(String urlSuffix) {
        return bspConfig.getWSUrl(urlSuffix);
    }

    public BSPConfig getBspConfig() {
        return bspConfig;
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, bspConfig);
    }
}
