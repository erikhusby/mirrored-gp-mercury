package org.broadinstitute.gpinformatics.mercury.boundary.run;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.monitoring.HipChatMessageSender;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConfig;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnector;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.VesselTransferEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.broadinstitute.gpinformatics.mercury.squid.generated.SolexaRunSynopsisBean;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;

/**
 * A JAX-RS resource for Solexa sequencing runs
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

    private MercuryOrSquidRouter router;

    private SquidConnector connector;

    private HipChatMessageSender messageSender;

    private SquidConfig squidConfig;

    @Inject
    public SolexaRunResource(IlluminaSequencingRunDao illuminaSequencingRunDao,
                             IlluminaSequencingRunFactory illuminaSequencingRunFactory,
                             IlluminaFlowcellDao illuminaFlowcellDao, VesselTransferEjb vesselTransferEjb,
                             MercuryOrSquidRouter router, SquidConnector connector,
                             HipChatMessageSender messageSender,
                             SquidConfig squidConfig) {
        this.illuminaSequencingRunDao = illuminaSequencingRunDao;
        this.illuminaSequencingRunFactory = illuminaSequencingRunFactory;
        this.illuminaFlowcellDao = illuminaFlowcellDao;
        this.vesselTransferEjb = vesselTransferEjb;
        this.router = router;
        this.connector = connector;
        this.messageSender = messageSender;
        this.squidConfig = squidConfig;
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
        MercuryOrSquidRouter.MercuryOrSquid route = router.routeForVessels(
                Collections.<LabVessel>singletonList(flowcell));

        Response callerResponse = null;
        UriBuilder absolutePathBuilder = uriInfo.getAbsolutePathBuilder();
        if (EnumSet.of(MercuryOrSquidRouter.MercuryOrSquid.SQUID,
                MercuryOrSquidRouter.MercuryOrSquid.BOTH).contains(route)) {
            SquidConnector.SquidResponse connectorRun = connector.createRun(solexaRunBean);

            /**
             * TODO SGM  To get past the demo and pre ExExV2 release, we will not forcibly return if there is an error with
             * the Squid call.  For now, we will run, call mercury and return the Squid response if there was an error.
             *
             * In the future, this will be encompassed by MercuryOrSquidRouter tests.
             */
            if (connectorRun.getCode() == Response.Status.CREATED.getStatusCode()) {
                callerResponse = Response.created(absolutePathBuilder.path(runName).build())
                        .entity(solexaRunBean).build();
            } else {
                callerResponse = Response.status(connectorRun.getCode()).entity(solexaRunBean).build();
            }
        }

        /*
            updated which routing should determine if a run should be registered in Mercury.  If BOTH is returned, we
             must cover Mercury as well as Squid
         */
        if (EnumSet.of(MercuryOrSquidRouter.MercuryOrSquid.MERCURY,
                MercuryOrSquidRouter.MercuryOrSquid.BOTH).contains(route)) {
            try {
                run = registerRun(solexaRunBean, flowcell);
                URI createdUri = absolutePathBuilder.path(run.getRunName()).build();
                if (callerResponse != null && callerResponse.getStatus() == Response.Status.CREATED.getStatusCode()) {
                    callerResponse = Response.created(createdUri).entity(solexaRunBean).build();
                }
            } catch (Exception e) {
                LOG.error("Failed to process run" + Response.Status.INTERNAL_SERVER_ERROR, e);
                messageSender.postMessageToGpLims("Failed to process run" + Response.Status.INTERNAL_SERVER_ERROR);
                /*
                * TODO SGM  Until ExExV2 is totally live, errors thrown from the Mercury side with Registration should
                * not be thrown (except if registering a run multiple times
                *
                * throw new ResourceException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR, e);
                */
            }
        }

        return callerResponse;
    }


    public IlluminaSequencingRun registerRun(SolexaRunBean solexaRunBean, IlluminaFlowcell illuminaFlowcell) {
         IlluminaSequencingRun illuminaSequencingRun =
                illuminaSequencingRunFactory.build(solexaRunBean, illuminaFlowcell);

        illuminaSequencingRunDao.persist(illuminaSequencingRun);

        // Link the reagentKit to flowcell if you have a reagentBlockBarcode. Only MiSeq uses reagentKits.;
        if (!StringUtils.isEmpty(solexaRunBean.getReagentBlockBarcode())) {
            vesselTransferEjb
                    .reagentKitToFlowcell(solexaRunBean.getReagentBlockBarcode(), solexaRunBean.getFlowcellBarcode(),
                            "pdunlea", solexaRunBean.getMachineName());
        }

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
    public ReadStructureRequest storeRunReadStructure(ReadStructureRequest readStructureRequest) {

        ReadStructureRequest requestToReturn = null;
        String runBarcode = readStructureRequest.getRunBarcode();
        if (runBarcode == null) {
            throw new ResourceException("No run barcode given.", Response.Status.NOT_FOUND);
        }
        // in the absence of information, route to squid
        MercuryOrSquidRouter.MercuryOrSquid route = MercuryOrSquidRouter.MercuryOrSquid.SQUID;

        IlluminaSequencingRun run = illuminaSequencingRunDao.findByBarcode(runBarcode);

        if (run != null) {
            // can only do routing if mercury actually knows the run
            route = router.routeForVessels(Collections.<LabVessel>singletonList(
                run.getSampleCartridge()
            ));
        }

        Throwable squidError = null;
        if (EnumSet.of(MercuryOrSquidRouter.MercuryOrSquid.SQUID,MercuryOrSquidRouter.MercuryOrSquid.BOTH).contains(route)) {
            if (squidConfig.getMercuryDeployment() != Deployment.STUBBY) {
                String squidUrl = squidConfig.getUrl() + "/resources/solexarunsynopsis";
                try {
                    sendToSquid(readStructureRequest,squidUrl);
                }
                catch(Throwable t) {
                    squidError = t;
                }
            }
            requestToReturn = readStructureRequest;
        }

        if (EnumSet.of(MercuryOrSquidRouter.MercuryOrSquid.MERCURY,MercuryOrSquidRouter.MercuryOrSquid.BOTH).contains(route)) {
            requestToReturn = illuminaSequencingRunFactory.storeReadsStructureDBFree(readStructureRequest, run);
        }

        if (squidError != null) {
            requestToReturn.setError("Failed while sending solexa_run_synopsis data to squid for " + runBarcode +":" + squidError.getMessage());
        }
        return requestToReturn;
    }

    /**
     * Sends the given read structure changes to squid's
     * solexa_run_synopsis table
     * @param readStructureData
     * @param squidWSUrl
     */
    private void sendToSquid(@Nonnull ReadStructureRequest readStructureData,
                             @Nonnull String squidWSUrl) {

        SolexaRunSynopsisBean solexaRunSynopsis = new SolexaRunSynopsisBean();
        solexaRunSynopsis.setRunBarcode(readStructureData.getRunBarcode());
        solexaRunSynopsis.setLanesSequenced(readStructureData.getLanesSequenced());
        solexaRunSynopsis.setActualReadStructure(readStructureData.getActualReadStructure());
        solexaRunSynopsis.setSetupReadStructure(readStructureData.getSetupReadStructure());
        solexaRunSynopsis.setImagedAreaPerLaneMM2(readStructureData.getImagedArea());

        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getClasses().add(JacksonJsonProvider.class);

        try {
            Client.create(clientConfig).resource(squidWSUrl)
                    .type(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON)
                    .post(solexaRunSynopsis);
        }
        catch(UniformInterfaceException e) {
            if (e.getResponse().getClientResponseStatus().getStatusCode() == 412) {
                throw new RuntimeException("Run " + readStructureData.getRunBarcode() + " is not registered in squid.");
            }
            else {
                throw e;
            }
        }
    }


}
