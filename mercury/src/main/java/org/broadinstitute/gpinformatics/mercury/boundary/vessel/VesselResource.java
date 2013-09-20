package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Path("/vessel")
@Stateful
@RequestScoped
public class VesselResource {

    @Inject
    private BSPSampleDataFetcher bspSampleDataFetcher;

    @Inject
    private VesselEjb vesselEjb;

    /**
     * Register a collection of tubes by Matrix barcodes, associated with their sample IDs as recorded in BSP.
     * This will query BSP for the Matrix barcodes to retrieve sample IDs and register LabVessels only if all
     * Matrix barcodes are known to BSP.  MercurySamples will be associated with these LabVessels and created
     * only if there is not an already existing MercurySample with the same sample barcode.
     */
    @Path("/registerTubes")
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public Response registerTubes(@Nonnull @FormParam("barcodes") List<String> matrixBarcodes) {

        // The call to BSP happens for all barcodes since there is well information returned to the caller
        // of this webservice that is not available in Mercury.  However it's not clear this well information
        // is being used, perhaps this could be optimized to not call BSP unless the Matrix barcodes are not
        // registered with Mercury.
        Map<String, GetSampleDetails.SampleInfo> sampleInfoMap =
                bspSampleDataFetcher.fetchSampleDetailsByMatrixBarcodes(matrixBarcodes);

        RegisterTubesBean responseBean = new RegisterTubesBean();

        for (String matrixBarcode : matrixBarcodes) {
            String well = null;
            String sampleBarcode = null;

            if (sampleInfoMap.containsKey(matrixBarcode)) {
                GetSampleDetails.SampleInfo sampleInfo = sampleInfoMap.get(matrixBarcode);
                well = sampleInfo.getWellPosition();
                sampleBarcode = sampleInfo.getSampleId();
            }

            RegisterTubeBean registerTubeBean = new RegisterTubeBean(matrixBarcode, well, sampleBarcode);
            responseBean.getRegisterTubeBeans().add(registerTubeBean);
        }

        // Flag for whether to persist the tube and sample registrations and what status code to return.
        boolean allBarcodesInBsp = sampleInfoMap.keySet().containsAll(matrixBarcodes);

        Response.Status status;
        // Only write out these MercurySamples if all Matrix barcodes were recognized by BSP.
        if (allBarcodesInBsp) {
            status = Response.Status.OK;
            vesselEjb.registerSamplesAndTubes(matrixBarcodes, sampleInfoMap);
        } else {
            // No tube or sample registration.
            status = Response.Status.PRECONDITION_FAILED;
        }

        return Response.status(status).entity(responseBean).type(MediaType.APPLICATION_XML_TYPE).build();
    }
}
