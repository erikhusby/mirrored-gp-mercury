package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
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

    private static final Log log = LogFactory.getLog(VesselResource.class);

    private static final String BARCODES_KEY = "barcodes";

    private Response buildResponse(@Nonnull Response.Status status, @Nonnull RegisterTubesBean registerTubesBean) {
        return Response.status(status).entity(registerTubesBean).type(MediaType.APPLICATION_XML_TYPE).build();
    }

    /**
     * Examine the UriInfo and MultivaluedMap for any unexpected GET or POST parameters.  The service calling this is
     * not currently expecting GET parameters, and the only supported POST parameter is 'barcodes'.
     */
    private void logParameters(UriInfo uriInfo, MultivaluedMap<String, String> formParameters) {
        // uriInfo can be null when called from test code.
        if (uriInfo != null) {
            MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
            if (queryParameters != null) {
                for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
                    log.error(String.format("registerTubes called with unexpected query parameter '%s' and values: %s",
                            entry.getKey(), StringUtils.join(entry.getValue(), " ")));
                }
            }
        }

        // formParameters should not be null, but the caller will check this as part of extracting the Matrix
        // barcodes and returning the appropriate status code.
        if (formParameters != null) {
            // Loop over the form parameters and log an error if any unexpected parameters are seen.
            for (Map.Entry<String, List<String>> entry : formParameters.entrySet()) {
                String key = entry.getKey();
                if (!key.equals(BARCODES_KEY)) {
                    log.error(String.format("registerTubes called with unexpected form parameter '%s' and values: %s",
                            key, StringUtils.join(entry.getValue(), " ")));
                }
            }
        }
    }

    /**
     * Register a collection of tubes by Matrix barcodes, associated with their sample IDs as recorded in BSP.
     * This will query BSP for the Matrix barcodes to retrieve sample IDs and register LabVessels only if all
     * Matrix barcodes are known to BSP.  MercurySamples will be associated with these LabVessels and created
     * only if there is not an already existing MercurySample with the same sample barcode.
     */
    @Path("/registerTubes")
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public Response registerTubes(MultivaluedMap<String, String> formParameters, @Context UriInfo uriInfo) {

        logParameters(uriInfo, formParameters);
        RegisterTubesBean responseBean = new RegisterTubesBean();

        // Get the List<String> barcodes value from the form parameters in a null-safe way.
        List<String> matrixBarcodes = MapUtils.getObject(formParameters, BARCODES_KEY);

        if (CollectionUtils.isEmpty(matrixBarcodes)) {
            log.error("No 'barcodes' form parameters passed to registerTubes.");
            return buildResponse(Response.Status.BAD_REQUEST, responseBean);
        } else {
            log.info("registerTubes invoked for Matrix barcodes: " + StringUtils.join(matrixBarcodes, " "));
        }

        // The call to BSP happens for all barcodes since there is well information returned to the caller
        // of this webservice that is not available in Mercury.  However it's not clear this well information
        // is being used, perhaps this could be optimized to not call BSP unless the Matrix barcodes are not
        // registered with Mercury.
        // IntelliJ does not realize that the CollectionUtils.isEmpty test precludes matrixBarcodes from being null.
        @SuppressWarnings("ConstantConditions")
        Map<String, GetSampleDetails.SampleInfo> sampleInfoMap =
                bspSampleDataFetcher.fetchSampleDetailsByMatrixBarcodes(matrixBarcodes);

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

        return buildResponse(status, responseBean);
    }
}
