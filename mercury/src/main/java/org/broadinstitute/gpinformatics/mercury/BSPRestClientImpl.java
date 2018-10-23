package org.broadinstitute.gpinformatics.mercury;

import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.bsp.client.response.ExomeExpressCheckResponse;

import javax.ws.rs.core.MediaType;
import java.util.List;

public class BSPRestClientImpl extends BSPRestClient {

    private static final String EXOMEEXPRESS_CHECK_IS_EXEX = "/exomeexpress/check_is_exex";

    public ExomeExpressCheckResponse callExomeExpressCheck(List<String> barcodes) {
        WebResource webResource = getWebResource(EXOMEEXPRESS_CHECK_IS_EXEX);
        return webResource.type(MediaType.APPLICATION_JSON).post(ExomeExpressCheckResponse.class, barcodes);
    }
}
