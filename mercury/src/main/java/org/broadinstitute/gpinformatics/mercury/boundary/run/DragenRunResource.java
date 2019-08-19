package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.FiniteStateMachineFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Stateful
@RequestScoped
@Path("/dragen")
public class DragenRunResource {

    private static final Log log = LogFactory.getLog(DragenRunResource.class);

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    @Inject
    private IlluminaFlowcellDao illuminaFlowcellDao;

    @Inject
    private SystemRouter router;

    @Inject
    private FiniteStateMachineFactory finiteStateMachineFactory;

    @Inject
    private StateMachineDao stateMachineDao;

    public DragenRunResource() {
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Path("/create")
    public Response createDragenRun(SolexaRunBean solexaRunBean) {
        String runName = new File(solexaRunBean.getRunDirectory()).getName();

        IlluminaSequencingRun run = illuminaSequencingRunDao.findByRunName(runName);
        if (run == null) {
            throw new ResourceException("Attempting to create a dragen workflow for a run that doesn't exist",
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        List<FiniteStateMachine> likeRuns = stateMachineDao.findLikeByRunName(run.getRunName());
        boolean createNewRun = verifyNewRun(solexaRunBean, likeRuns);
        if (createNewRun) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_hh-mm").format(new Date());
            String stateMachineName = run.getRunName() + "-Test-" + timestamp;
            MessageCollection messageCollection = new MessageCollection();
            finiteStateMachineFactory.createFiniteStateMachineForRun(run, stateMachineName,
                    messageCollection);
            if (messageCollection.hasErrors()) {
                String errMsg = StringUtils.join(messageCollection.getErrors(), "\n");
                log.error(errMsg);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errMsg).type(MediaType.TEXT_PLAIN_TYPE).build();
            } else {
                return Response.status(Response.Status.CREATED).type(MediaType.TEXT_PLAIN_TYPE).build();
            }
        }

        return Response.status(Response.Status.OK).entity("Ignored").type(MediaType.TEXT_PLAIN_TYPE).build();
    }

    /**
     * @return true if run exists, on SL-NVD, and all machines are inactive.
     */
    private boolean verifyNewRun(SolexaRunBean run, List<FiniteStateMachine> stateMachines) {
        if (!run.getMachineName().equals("SL-NVD")) {
            return false;
        }

        IlluminaFlowcell flowcell = illuminaFlowcellDao.findByBarcode(run.getFlowcellBarcode());
        if (flowcell == null) {
            return false;
        }

        SystemRouter.System route = router.routeForVessels(Collections.<LabVessel>singletonList(flowcell));
        if (route != SystemRouter.System.MERCURY) {
            return false;
        }

        return stateMachines.stream().anyMatch(FiniteStateMachine::isAlive);
    }
}
