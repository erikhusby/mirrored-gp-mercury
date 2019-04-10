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
import com.sun.jersey.api.client.filter.LoggingFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.queue.CompletedSamples;
import org.broadinstitute.bsp.client.response.ExomeExpressCheckResponse;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This contains common code used by all clients of BSP rest, ie: non-broadcore) services.
 */
@Dependent
public class BSPRestClient extends AbstractJerseyClientService {

    private static final String EXOMEEXPRESS_CHECK_IS_EXEX = "exomeexpress/check_is_exex_with_wrapper";

    private static final String SEND_PICO_MESSAGE = "afterPico/notifyOfCompletedPico";

    private static final long serialVersionUID = 5472586820069306030L;

    private static final Log logger = LogFactory.getLog(BSPRestClient.class);

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
        WebResource webResource = getWebResource(getUrl(EXOMEEXPRESS_CHECK_IS_EXEX));
        webResource.addFilter(new LoggingFilter(System.out));
        return webResource.type(MediaType.APPLICATION_JSON).post(ExomeExpressCheckResponse.class, new ListWrapper(barcodes));
    }

    public void informUsersOfPicoCompletion(List<String> sampleIds) {
        WebResource webResource = getWebResource(getUrl(SEND_PICO_MESSAGE));
        webResource.type(MediaType.APPLICATION_JSON_TYPE).post(String.class, new CompletedSamples(sampleIds));
    }

    @XmlRootElement
    public static class ListWrapper {
        public ListWrapper() {
            this(Collections.emptyList());
        }

        public ListWrapper(List<String> list) {
            this.list = list.toArray(new String[0]);
        }

        public String[] list;
    }
}
