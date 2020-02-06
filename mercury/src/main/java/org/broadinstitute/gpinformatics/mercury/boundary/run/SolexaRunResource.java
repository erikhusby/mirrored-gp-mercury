package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.VesselTransferEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A JAX-RS resource for creating, updating, or registering Solexa sequencing runs in Mercury.
 * <p/>
 * There exists another resource {@link org.broadinstitute.gpinformatics.mercury.boundary.zims.IlluminaRunResource}
 * that also deals with Run information, but it is geared toward finding Run info.  Currently the two resources are
 * separate paths and files but it may be prudent in the future to join them to eliminate the confusion of what is
 * found where.
 */
@Path("/solexarun")
@Stateful
@RequestScoped
public class SolexaRunResource {

    private static final Log LOG = LogFactory.getLog(SolexaRunResource.class);

    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    private IlluminaSequencingRunFactory illuminaSequencingRunFactory;

    private IlluminaFlowcellDao illuminaFlowcellDao;

    private VesselTransferEjb vesselTransferEjb;

    @Inject
    public SolexaRunResource(IlluminaSequencingRunDao illuminaSequencingRunDao,
                             IlluminaSequencingRunFactory illuminaSequencingRunFactory,
                             IlluminaFlowcellDao illuminaFlowcellDao, VesselTransferEjb vesselTransferEjb) {
        this.illuminaSequencingRunDao = illuminaSequencingRunDao;
        this.illuminaSequencingRunFactory = illuminaSequencingRunFactory;
        this.illuminaFlowcellDao = illuminaFlowcellDao;
        this.vesselTransferEjb = vesselTransferEjb;
    }

    public SolexaRunResource() {

    }

    // todo jmt GET method

    @POST
    @Consumes({"application/xml", "application/json"})
    @Produces({"application/xml", "application/json"})
    public Response createRun(SolexaRunBean solexaRunBean, @Context UriInfo uriInfo) {

        String runName = new File(solexaRunBean.getRunDirectory()).getName();

        IlluminaSequencingRun run = illuminaSequencingRunDao.findByRunName(runName);
        if (run != null) {
            throw new ResourceException("Attempting to create a run that is already registered in the system",
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        IlluminaFlowcell flowcell = illuminaFlowcellDao.findByBarcode(solexaRunBean.getFlowcellBarcode());
        Response callerResponse = null;
        UriBuilder absolutePathBuilder = uriInfo.getAbsolutePathBuilder();
        try {
            run = registerRun(solexaRunBean, flowcell);
            URI createdUri = absolutePathBuilder.path(run.getRunName()).build();
            if (callerResponse == null || callerResponse.getStatus() == Response.Status.CREATED.getStatusCode()) {
                callerResponse = Response.created(createdUri).entity(solexaRunBean).build();
            }
        } catch (Exception e) {
            LOG.error("Failed to process run" + Response.Status.INTERNAL_SERVER_ERROR, e);
            throw new ResourceException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR, e);
        }

        return callerResponse;
    }


    public IlluminaSequencingRun registerRun(SolexaRunBean solexaRunBean, IlluminaFlowcell illuminaFlowcell) {
        // Link the reagentKit to flowcell if you have a reagentBlockBarcode. Only MiSeq uses reagentKits.;
        if (!StringUtils.isEmpty(solexaRunBean.getReagentBlockBarcode())) {
            LabEvent labEvent = vesselTransferEjb.reagentKitToFlowcell(solexaRunBean.getReagentBlockBarcode(),
                    solexaRunBean.getFlowcellBarcode(), "pdunlea", solexaRunBean.getMachineName());
            illuminaFlowcell = (IlluminaFlowcell) labEvent.getTargetLabVessels().iterator().next();
        }

        IlluminaSequencingRun illuminaSequencingRun = illuminaSequencingRunFactory.build(solexaRunBean,
                illuminaFlowcell);
        illuminaSequencingRunDao.persist(illuminaSequencingRun);

        return illuminaSequencingRun;
    }

    /**
     * storeRunReadStructure is the implementation for a Rest service that allows the Pipeline to associate run read
     * structures (both planned and actual) with a sequencing run
     *
     * @param readStructureRequest contains all information necessary to searching for and update a Sequencing run
     *
     * @return a new instance of a readStructureRequest populated with the values as they are found on the run itself
     */
    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Path("/storeRunReadStructure")
    public Response storeRunReadStructure(ReadStructureRequest readStructureRequest) {
        ReadStructureRequest requestToReturn = null;
        Response callerResponse = null;
        boolean searchByBarcode = false;

        String requestIdentifier = readStructureRequest.getRunName();
        // We prefer to query by runName but if one is not supplied we will try to find the run by barcode.
        if (StringUtils.isBlank(requestIdentifier)) {
            requestIdentifier = readStructureRequest.getRunBarcode();
            searchByBarcode = true;
        }
        if (requestIdentifier == null) {
            throw new ResourceException("No run name or barcode given.", Response.Status.NOT_FOUND);
        }

        IlluminaSequencingRun run=null;
        if (searchByBarcode) {
            Collection<IlluminaSequencingRun> runs = illuminaSequencingRunDao.findByBarcode(requestIdentifier);
            if (runs.size() > 1) {
                List<String> runNames = new ArrayList<>(runs.size());
                for (IlluminaSequencingRun illuminaSequencingRun : runs) {
                    runNames.add(illuminaSequencingRun.getRunName());
                }
                throw new ResourceException(String.format(
                        "%s sequencing runs found for barcode %s: %s. Please try supplying the run name instead.",
                        runs.size(), requestIdentifier, runNames), Response.Status.INTERNAL_SERVER_ERROR);
            }
            if (!runs.isEmpty()) {
                run = runs.iterator().next();
            }
        } else {
            run = illuminaSequencingRunDao.findByRunName(requestIdentifier);
        }
        if (run == null) {
            throw new ResourceException("There is no run found in mercury for " + requestIdentifier,
                    Response.Status.NOT_FOUND);
        }

        // Saves the read structure data.
        try {
            requestToReturn = illuminaSequencingRunFactory.storeReadsStructureDBFree(readStructureRequest, run);
            callerResponse = Response.ok(requestToReturn).entity(requestToReturn).build();
        } catch (ResourceException e) {
            callerResponse = Response.status(e.getStatus()).entity(requestToReturn).build();
        }
        return callerResponse;
    }
}
