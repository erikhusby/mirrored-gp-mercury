package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.getsampledetails.SampleInfo;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Path("/vessel")
@Stateful
@RequestScoped
public class VesselResource {

    @Inject
    private BSPSampleDataFetcher bspSampleDataFetcher;

    @Inject
    private TwoDBarcodedTubeDao twoDBarcodedTubeDao;

    private static final String BARCODES_PARAMETER_KEY = "barcodes";

    /**
     * Register a collection of tubes by Matrix barcodes, associated with their samples IDs as recorded in BSP.
     * This will query BSP for the Matrix barcodes to retrieve sample IDs and register LabVessels only if all
     * Matrix barcodes are known to BSP.
     */
    @Path("/registerTubes")
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public Response registerTubes(@Nonnull MultivaluedMap<String, String> parameters) {
        Collection<String> matrixBarcodes = extractMatrixBarcodes(parameters);
        Map<String, SampleInfo> sampleInfoMap = bspSampleDataFetcher.fetchSampleDetailsByMatrixBarcodes(matrixBarcodes);

        // Determine which tubes are already known to Mercury.  This call creates map entries for all parameters
        // but leaves values null for unknown vessels.
        List<TwoDBarcodedTube> previouslyRegisteredTubes = twoDBarcodedTubeDao.findListByBarcodes(matrixBarcodes);
        Set<String> previouslyRegisteredBarcodes = new HashSet<>();
        for (TwoDBarcodedTube tube : previouslyRegisteredTubes) {
            previouslyRegisteredBarcodes.add(tube.getLabel());
        }
        List<TwoDBarcodedTube> newTubes = new ArrayList<>();

        boolean allBarcodesInBsp = true;
        RegisterTubesBean responseBean = new RegisterTubesBean();

        // Iterate the sample infos to determine which barcodes are known to BSP.
        for (Map.Entry<String, SampleInfo> entry : sampleInfoMap.entrySet()) {
            String matrixBarcode = entry.getKey();
            SampleInfo sampleInfo = entry.getValue();

            String well = null;
            String sampleBarcode = null;
            if (sampleInfo != null) {
                well = sampleInfo.getWellPosition();
                sampleBarcode = sampleInfo.getSampleId();
                // If the barcode is not previously known to Mercury, create a TwoDBarcoded tube in the newTubes
                // List to be registered.
                if (!previouslyRegisteredBarcodes.contains(matrixBarcode)) {
                    newTubes.add(new TwoDBarcodedTube(matrixBarcode, sampleBarcode));
                }
            } else {
                // Keep going even if error is true, we just won't do the registration.  We still want to return
                // results to the caller (a deck script) so it could show the user what's wrong.
                allBarcodesInBsp = false;
            }
            RegisterTubeBean tubeBean = new RegisterTubeBean(matrixBarcode, well, sampleBarcode);
            responseBean.getRegisterTubeBeans().add(tubeBean);
        }

        if (allBarcodesInBsp) {
            // Flush to make sure we encounter any errors with database constraints prior to returning a
            // response to the client.
            twoDBarcodedTubeDao.persistAll(newTubes);
            twoDBarcodedTubeDao.flush();
        }

        Response.Status status = allBarcodesInBsp ? Response.Status.OK : Response.Status.PRECONDITION_FAILED;
        return Response.status(status).entity(responseBean).type(MediaType.APPLICATION_XML_TYPE).build();
    }

    /**
     * Extract the unique Set of Matrix barcodes from the request parameters.
     */
    private Collection<String> extractMatrixBarcodes(@Nonnull MultivaluedMap<String, String> map) {
        Set<String> barcodes = new HashSet<>();

        if (map.containsKey(BARCODES_PARAMETER_KEY)) {
            barcodes.addAll(map.get(BARCODES_PARAMETER_KEY));
        }

        return barcodes;
    }
}
