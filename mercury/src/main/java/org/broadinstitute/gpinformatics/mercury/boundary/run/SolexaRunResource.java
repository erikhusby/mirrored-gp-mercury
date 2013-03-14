package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnector;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
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
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.net.URI;
import java.text.MessageFormat;

/**
 * A JAX-RS resource for Solexa sequencing runs
 */
@Path("/solexarun")
@Stateful
@RequestScoped
public class SolexaRunResource {

    private static final Log LOG = LogFactory.getLog(SolexaRunResource.class);

    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    private IlluminaSequencingRunFactory illuminaSequencingRunFactory;

    private IlluminaFlowcellDao illuminaFlowcellDao;

    private MercuryOrSquidRouter router;

    private SquidConnector connector;

    @Inject
    public SolexaRunResource(IlluminaSequencingRunDao illuminaSequencingRunDao,
                             IlluminaSequencingRunFactory illuminaSequencingRunFactory,
                             IlluminaFlowcellDao illuminaFlowcellDao, MercuryOrSquidRouter router,
                             SquidConnector connector) {
        this.illuminaSequencingRunDao = illuminaSequencingRunDao;
        this.illuminaSequencingRunFactory = illuminaSequencingRunFactory;
        this.illuminaFlowcellDao = illuminaFlowcellDao;
        this.router = router;
        this.connector = connector;
    }

    public SolexaRunResource() {

    }

    // todo jmt GET method

    @POST
    @Consumes({"application/xml", "application/json"})
    @Produces({"application/xml", "application/json"})
    public Response createRun(SolexaRunBean solexaRunBean, @Context UriInfo uriInfo) {

        File runDirectory = new File(solexaRunBean.getRunDirectory());
        IlluminaSequencingRun run = illuminaSequencingRunDao.findByRunName(runDirectory.getName());

        if (run != null) {
            throw new ResourceException("Attempting to create a run that is already registered in the system",
                                               Response.Status.INTERNAL_SERVER_ERROR);
        }

        IlluminaFlowcell flowcell =
                illuminaFlowcellDao.findByBarcode(solexaRunBean.getFlowcellBarcode());

        Response callerResponse;

        SquidConnector.SquidResponse connectorRun = connector.createRun(solexaRunBean);
        if (connectorRun.getCode() != Response.Status.CREATED.getStatusCode()) {
            throw new ResourceException(connectorRun.getMessage(),
                                               Response.Status.fromStatusCode(connectorRun.getCode()));
        }
        callerResponse = Response.created(uriInfo.getAbsolutePathBuilder().path(runDirectory.getName()).build())
                                 .entity(connectorRun).build();

        if (router.routeForVessel(flowcell) == MercuryOrSquidRouter.MercuryOrSquid.MERCURY) {
            try {
                run = registerRun(solexaRunBean, flowcell);
            } catch (Exception e) {
                LOG.error("Failed to process run" + Response.Status.INTERNAL_SERVER_ERROR, e);
                /*
                 * TODO SGM  Until ExExV2 is totally live, errors thrown from the Mercury side with Registration should
                 * not be thrown (except if registering a run multiple times
                 *
                 * throw new ResourceException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR, e);
                 */
            }
            URI createdUri = uriInfo.getAbsolutePathBuilder().path(run.getRunName()).build();
            callerResponse = Response.created(createdUri).entity(new SolexaRunBean(run)).build();
        }
        return callerResponse;
    }

    public IlluminaSequencingRun registerRun(SolexaRunBean solexaRunBean, IlluminaFlowcell illuminaFlowcell) {
        IlluminaSequencingRun illuminaSequencingRun;

        /*
         * Need logic to register MiSeq run based off of the ReagentBlockBarcode in SolexaRunBean.
         * Will be another story.
         */

        illuminaSequencingRun = illuminaSequencingRunFactory.build(solexaRunBean, illuminaFlowcell);

        illuminaSequencingRunDao.persist(illuminaSequencingRun);
        illuminaSequencingRunDao.flush();
        return illuminaSequencingRun;
    }
}
