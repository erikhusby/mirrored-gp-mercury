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

//import org.broadinstitute.bsp.client.queue.CompletedSamples;
import org.broadinstitute.bsp.client.response.ExomeExpressCheckResponse;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJaxRsClientService;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.List;

/**
 * This contains common code used by all clients of BSP rest, ie: non-broadcore) services.
 */
@Dependent
public class BSPRestClient extends AbstractJaxRsClientService {

    private static final String EXOMEEXPRESS_CHECK_IS_EXEX = "exomeexpress/check_is_exex_with_wrapper";

    private static final String SEND_PICO_MESSAGE = "afterPico/notifyOfCompletedPico";

    private static final long serialVersionUID = 5472586820069306030L;

    @Inject
    private BSPConfig bspConfig;

    @SuppressWarnings("unused")
    public BSPRestClient() {
    }

    @SuppressWarnings("unused")
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
        return getJaxRsClient().target(urlString);
    }

    public ExomeExpressCheckResponse callExomeExpressCheck(List<String> barcodes) {
        WebTarget webResource = getJaxRsClient().target(getUrl(EXOMEEXPRESS_CHECK_IS_EXEX));
        Response post = webResource.request().post(Entity.entity(new ListWrapper(barcodes), MediaType.APPLICATION_JSON_TYPE));
        return post.readEntity(ExomeExpressCheckResponse.class);
    }

    public void informUsersOfPicoCompletion(List<String> sampleIds) {
        WebTarget webResource = getJaxRsClient().target(getUrl(SEND_PICO_MESSAGE));
//        webResource.request(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(new CompletedSamples(sampleIds), MediaType.APPLICATION_JSON_TYPE));
    }

    @XmlRootElement
    public static class ListWrapper {
        @SuppressWarnings("unused")
        public ListWrapper() {
            this(Collections.emptyList());
        }

        ListWrapper(List<String> list) {
            this.list = list.toArray(new String[0]);
        }

        public String[] list;
    }
}
