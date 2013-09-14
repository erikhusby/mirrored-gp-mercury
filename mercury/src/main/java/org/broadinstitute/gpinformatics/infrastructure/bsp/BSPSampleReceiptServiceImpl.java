package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.WebResource;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.response.SampleKitReceiptResponse;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.BSPJerseyClient;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the Bsp sample receipt service.
 */
@Impl
public class BSPSampleReceiptServiceImpl extends BSPJerseyClient implements BSPSampleReceiptService {

    private static final String WEB_SERVICE_URL = "sample/receivesample";

    /**
     * Required for @Impl class.
     */
    @SuppressWarnings("unused")
    public BSPSampleReceiptServiceImpl() {
    }

    /**
     * Container free constructor, need to initialize all dependencies explicitly.
     *
     * @param bspConfig The config object
     */
    public BSPSampleReceiptServiceImpl(BSPConfig bspConfig) {
        super(bspConfig);
    }

    @Override
    public SampleKitReceiptResponse receiveSamples(List<String> barcodes, String username) {

        // Prepare the parameters.
        List<String> parameters = new ArrayList<>();
        parameters.add("username=" + username);
        parameters.add("barcodes=" + StringUtils.join(barcodes, ","));

        String parameterString = StringUtils.join(parameters, "&");

        // Change to the URL string and fire off the web service.
        String urlString = getUrl(WEB_SERVICE_URL + "?" + parameterString);

        WebResource webResource = getJerseyClient().resource(urlString);

        return webResource.type(MediaType.APPLICATION_XML_TYPE).post(SampleKitReceiptResponse.class, urlString);
    }
}
