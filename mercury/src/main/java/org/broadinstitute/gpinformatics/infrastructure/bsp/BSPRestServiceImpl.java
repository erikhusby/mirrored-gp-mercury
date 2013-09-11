package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.bsp.getsampledetails.Details;
import org.broadinstitute.gpinformatics.infrastructure.bsp.getsampledetails.SampleInfo;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.BSPJerseyClient;

import javax.annotation.Nonnull;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Impl
public class BSPRestServiceImpl extends BSPJerseyClient implements BSPRestService {

    // Used for mapping Matrix barcodes to Sample short barcodes, forces xml output format.
    private static final String WS_SAMPLE_DETAILS = "sample/getsampledetails?format=xml";

    public BSPRestServiceImpl() {
    }

    /**
     * Return a Map of manufacturer barcodes to the SampleDetails object for each input barcode.
     */
    public Map<String, SampleInfo> fetchSampleDetailsByMatrixBarcodes(@Nonnull Collection<String> matrixBarcodes) {
        String queryString = makeQueryString("barcodes", matrixBarcodes);
        String urlString = getUrl(WS_SAMPLE_DETAILS);

        Map<String, SampleInfo> map = new HashMap<>();

        WebResource resource = getJerseyClient().resource(urlString + "&" + queryString);
        Details details = resource.accept(MediaType.APPLICATION_XML).get(new GenericType<Details>() {});
        // Initialize all map values to null.
        for (String matrixBarcode : matrixBarcodes) {
            map.put(matrixBarcode, null);
        }

        // Overwrite the map values that were found in BSP with the SampleDetails objects.
        for (SampleInfo sampleInfo : details.getSampleDetails().getSampleInfo()) {
            map.put(sampleInfo.getManufacturerBarcode(), sampleInfo);
        }

        return map;
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, getBspConfig());
    }
}
