/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJaxRsClientService;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

/**
 * This contains common code used by all clients of BSP rest, ie: non-broadcore) services.
 */
@Dependent
public class BSPRestClient extends AbstractJaxRsClientService {

    private static final long serialVersionUID = 5472586820069306030L;

    @Inject
    private BSPConfig bspConfig;

    public BSPRestClient() {
    }

    public BSPRestClient(BSPConfig bspConfig) {
        this.bspConfig = bspConfig;
    }

    public String getUrl(String urlSuffix) {
        return bspConfig.getUrl("rest/" + urlSuffix);
    }

    public BSPConfig getBspConfig() {
        return bspConfig;
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, bspConfig);
    }

    public WebTarget getWebResource(String urlString) {
        return getJerseyClient().target(urlString);
    }
}
