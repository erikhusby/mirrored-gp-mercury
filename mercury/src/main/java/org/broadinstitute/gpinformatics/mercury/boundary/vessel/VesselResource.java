package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
    private SampleDataFetcher sampleDataFetcher;

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
                    if (!TUBE_TYPE_QUERY_PARAM.equals(entry.getKey())) {
                        log.error(String.format(
                                "registerTubes called with unexpected query parameter '%s' and values: %s",
                                entry.getKey(), StringUtils.join(entry.getValue(), " ")));
                    }
                }
            }
        }

        // formParameters should not be null, but the caller will check this as part of extracting the tube
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
    private static final String TUBE_TYPE_QUERY_PARAM = "tubeType";
    /**
     * Register a collection of barcoded tubes and associate with their sample IDs as recorded in BSP.
     * This will query BSP using the tube barcode which can be manufacturer, sqm, or sample name barcode.
     * Registers LabVessels only if all tubes are known to BSP.  MercurySamples will be associated with these
     * LabVessels and created only if there is not an already existing MercurySample with the same sample name.
     * An optional tubeType is the BarcodedTube.BarcodedTubeType.name and defaults to Matrix tube.
     */
    @Path("/registerTubes")
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public Response registerTubes(MultivaluedMap<String, String> formParameters,
                                  @QueryParam(TUBE_TYPE_QUERY_PARAM) String tubeType,
                                  @Context UriInfo uriInfo) {

        logParameters(uriInfo, formParameters);
        RegisterTubesBean responseBean = new RegisterTubesBean();

        // Get the List<String> barcodes value from the form parameters in a null-safe way.
        List<String> tubeBarcodes = MapUtils.getObject(formParameters, BARCODES_KEY);

        if (CollectionUtils.isEmpty(tubeBarcodes)) {
            log.error("No 'barcodes' form parameters passed to registerTubes.");
            return buildResponse(Response.Status.BAD_REQUEST, responseBean);
        } else {
            log.info("registerTubes invoked for tube barcodes: " + StringUtils.join(tubeBarcodes, " "));
        }

        // The call to BSP happens for all tube barcodes since there is well information returned to the caller
        // of this webservice that is not available in Mercury.  However it's not clear this well information
        // is being used, perhaps this could be optimized to not call BSP unless the tube barcodes are not
        // registered with Mercury.
        // IntelliJ does not realize that the CollectionUtils.isEmpty test precludes tubeBarcodes from being null.
        @SuppressWarnings("ConstantConditions")
        Map<String, GetSampleDetails.SampleInfo> sampleInfoMap =
                sampleDataFetcher.fetchSampleDetailsByBarcode(tubeBarcodes);

        for (String tubeBarcode : tubeBarcodes) {
            String well = null;
            String sampleName = null;

            if (sampleInfoMap.containsKey(tubeBarcode)) {
                GetSampleDetails.SampleInfo sampleInfo = sampleInfoMap.get(tubeBarcode);
                well = sampleInfo.getWellPosition();
                sampleName = sampleInfo.getSampleId();
            }

            RegisterTubeBean registerTubeBean = new RegisterTubeBean(tubeBarcode, well, sampleName);
            responseBean.getRegisterTubeBeans().add(registerTubeBean);
        }

        // Flag for whether to persist the tube and sample registrations and what status code to return.
        boolean allBarcodesInBsp = sampleInfoMap.keySet().containsAll(tubeBarcodes);

        Response.Status status;
        // Only write out these MercurySamples if all tube barcodes were recognized by BSP.
        if (allBarcodesInBsp) {
            status = Response.Status.OK;
            vesselEjb.registerSamplesAndTubes(tubeBarcodes, tubeType, sampleInfoMap);
        } else {
            // No tube or sample registration.
            status = Response.Status.PRECONDITION_FAILED;
        }

        return buildResponse(status, responseBean);
    }
}
