package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.monitoring.HipChatMessageSender;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnector;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;

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

/**
 * A JAX-RS resource for Solexa sequencing runs
 *
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

   // private IlluminaSequencingRunDao illuminaSequencingRunDao;

    private IlluminaSequencingRunFactory illuminaSequencingRunFactory;

   // private IlluminaFlowcellDao illuminaFlowcellDao;

    private MercuryOrSquidRouter router;

    private SquidConnector connector;

    private HipChatMessageSender messageSender;

    @Inject
    public SolexaRunResource(IlluminaSequencingRunFactory illuminaSequencingRunFactory,
                             MercuryOrSquidRouter router,
                             SquidConnector connector, HipChatMessageSender messageSender) {
        //this.illuminaSequencingRunDao = illuminaSequencingRunDao;
        this.illuminaSequencingRunFactory = illuminaSequencingRunFactory;
      //  this.illuminaFlowcellDao = illuminaFlowcellDao;
        this.router = router;
        this.connector = connector;
        this.messageSender = messageSender;
    }

    public SolexaRunResource() {

    }

    // todo jmt GET method

    @POST
    @Consumes({"application/xml", "application/json"})
    @Produces({"application/xml", "application/json"})
    public Response createRun(SolexaRunBean solexaRunBean, @Context UriInfo uriInfo,
                              IlluminaFlowcell flowcell) {

        String runname = new File(solexaRunBean.getRunDirectory()).getName();

        Response callerResponse;

        SquidConnector.SquidResponse connectorRun = connector.createRun(solexaRunBean);

        /**
         * TODO SGM  To get past the demo and pre ExExV2 release, we will not forcibly return if there is an error with
         * the Squid call.  For now, we will run, call mercury and return the Squid response if there was an error.
         *
         * In the future, this will be encompassed by MercuryOrSquidRouter tests.
         */
        UriBuilder absolutePathBuilder = uriInfo.getAbsolutePathBuilder();
        if (connectorRun.getCode() != Response.Status.CREATED.getStatusCode()) {
            callerResponse = Response.status(connectorRun.getCode()).entity(solexaRunBean).build();
        } else {

            callerResponse = Response.created(absolutePathBuilder.path(runname).build())
                                     .entity(solexaRunBean).build();
        }

        if (router.routeForVessel(flowcell) == MercuryOrSquidRouter.MercuryOrSquid.MERCURY) {
            try {
                IlluminaSequencingRun run = registerRun(solexaRunBean, flowcell);
                URI createdUri = absolutePathBuilder.path(run.getRunName()).build();
                if (callerResponse.getStatus() == Response.Status.CREATED.getStatusCode()) {
                    callerResponse = Response.created(createdUri).entity(run).build();
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
        IlluminaSequencingRun illuminaSequencingRun;

        /*
         * Need logic to register MiSeq run based off of the ReagentBlockBarcode in SolexaRunBean.
         * Will be another story.
         */

        illuminaSequencingRun = illuminaSequencingRunFactory.buildDbFree(solexaRunBean, illuminaFlowcell);

      //  illuminaSequencingRunDao.persist(illuminaSequencingRun);
        return illuminaSequencingRun;
    }
}
