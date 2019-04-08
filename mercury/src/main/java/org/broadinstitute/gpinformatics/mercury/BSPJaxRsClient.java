package org.broadinstitute.gpinformatics.mercury;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJaxRsClientService;

import javax.inject.Inject;
import javax.ws.rs.client.Client;

/**
 * This contains common code used by all clients of BSP web services.
 */
public abstract class BSPJaxRsClient extends AbstractJaxRsClientService {

    private static final long serialVersionUID = 5472586820069306030L;

    @Inject
    private BSPConfig bspConfig;

    public BSPJaxRsClient() {
    }

    public BSPJaxRsClient(BSPConfig bspConfig) {
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
