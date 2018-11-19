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

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.bsp.client.response.ExomeExpressCheckResponse;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * This contains common code used by all clients of BSP rest, ie: non-broadcore) services.
 */
@Dependent
public class BSPRestClient extends AbstractJerseyClientService {

    private static final String EXOMEEXPRESS_CHECK_IS_EXEX = "/exomeexpress/check_is_exex";

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

    public WebResource getWebResource(String urlString) {
        return getJerseyClient().resource(urlString);
    }


    public ExomeExpressCheckResponse callExomeExpressCheck(List<String> barcodes) {
        WebResource webResource = getWebResource(EXOMEEXPRESS_CHECK_IS_EXEX);
        return webResource.type(MediaType.APPLICATION_JSON).post(ExomeExpressCheckResponse.class, barcodes);
    }
}
