package org.broadinstitute.sequel.boundary.run;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.boundary.ResourceException;
import org.broadinstitute.sequel.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.sequel.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.sequel.entity.run.IlluminaSequencingRun;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * A JAX-RS resource for Solexa sequencing runs
 */
@Path("/solexarun")
@Stateless // todo jmt should this be stateful?  It has stateful DAOs injected.
public class SolexaRunResource {

    private static final Log LOG = LogFactory.getLog(SolexaRunResource.class);

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    @Inject
    private IlluminaSequencingRunFactory illuminaSequencingRunFactory;

    // todo jmt GET method

    @POST
    @Consumes({"application/xml", "application/json"})
    @Produces({"application/xml", "application/json"})
    public Response createRun(SolexaRunBean solexaRunBean, @Context UriInfo uriInfo) {
        IlluminaSequencingRun illuminaSequencingRun;
        try {
            illuminaSequencingRun = registerRun(solexaRunBean);
        } catch (Exception e) {
            LOG.error("Failed to process run", e);
            throw new ResourceException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
        URI createdUri = uriInfo.getAbsolutePathBuilder().path(illuminaSequencingRun.getRunName()).build();
        return Response.created(createdUri).entity(new SolexaRunBean(illuminaSequencingRun)).build();
    }

    public IlluminaSequencingRun registerRun(SolexaRunBean solexaRunBean) {
        IlluminaSequencingRun illuminaSequencingRun;
        if(solexaRunBean.getReagentBlockBarcode() != null && !solexaRunBean.getReagentBlockBarcode().trim().isEmpty()) {
            // todo jmt how does SequeL do Designations?
            throw new RuntimeException("MiSeq registration not implemented yet");
//                solexaRun = solexaRunRegistrationService.createFlowcellAndRegisterRun(solexaRunBean.flowcellBarcode,
//                        solexaRunBean.reagentBlockBarcode, solexaRunBean.machineName, solexaRunBean.runBarcode,
//                        solexaRunBean.runDate, solexaRunBean.runDirectory);
        } else {
            illuminaSequencingRun = illuminaSequencingRunFactory.build(solexaRunBean);
        }
        illuminaSequencingRunDao.persist(illuminaSequencingRun);
        illuminaSequencingRunDao.flush();
        return illuminaSequencingRun;
    }
}
