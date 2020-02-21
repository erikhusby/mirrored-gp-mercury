package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.FiniteStateMachineFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
        Set<VesselPosition> genomicLanes = verifyNewRun(solexaRunBean, likeRuns);
        if (genomicLanes == null || genomicLanes.isEmpty()) {
            return Response.status(Response.Status.OK).entity("Ignored").type(MediaType.TEXT_PLAIN_TYPE).build();
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_hh-mm").format(new Date());
        String stateMachineName = run.getRunName() + "-Test-" + timestamp;
        MessageCollection messageCollection = new MessageCollection();

        finiteStateMachineFactory.createFiniteStateMachineForRun(run, genomicLanes, stateMachineName,
                Collections.emptySet(), messageCollection);
        if (messageCollection.hasErrors()) {
            String errMsg = StringUtils.join(messageCollection.getErrors(), "\n");
            log.error(errMsg);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errMsg).type(MediaType.TEXT_PLAIN_TYPE).build();
        } else {
            return Response.status(Response.Status.CREATED).type(MediaType.TEXT_PLAIN_TYPE).build();
        }
    }

    /**
     * @return true if run exists, on SL-NVD, and all machines are inactive.
     */
    private Set<VesselPosition> verifyNewRun(SolexaRunBean run, List<FiniteStateMachine> stateMachines) {
        if (!run.getMachineName().equals("SL-NVD") && !run.getMachineName().equals("SL-NVE")) {
            return null;
        }

        IlluminaFlowcell flowcell = illuminaFlowcellDao.findByBarcode(run.getFlowcellBarcode());
        if (flowcell == null) {
            return null;
        }

        if (stateMachines.stream().anyMatch(FiniteStateMachine::isAlive)) {
            log.info("Found an already existing state machine for this run " + run.getRunDirectory());
            return null;
        }

        // ONLY care about lanes that are genomes
        Set<VesselPosition> genomeLanes = new HashSet<>();
        Iterator<String> positionNames = flowcell.getVesselGeometry().getPositionNames();
        short laneNum = 0;
        while (positionNames.hasNext()) {
            ++laneNum;
            String positionName = positionNames.next();
            VesselPosition vesselPosition = VesselPosition.getByName(positionName);

            boolean mixedLaneOk = false;
            for (SampleInstanceV2 sampleInstance : flowcell.getContainerRole().getSampleInstancesAtPositionV2(
                    vesselPosition)) {
                BucketEntry singleBucketEntry = sampleInstance.getSingleBucketEntry();
                if (singleBucketEntry != null) {
                    if (Objects.equals(singleBucketEntry.getProductOrder().getProduct().getPipelineDataTypeString(),
                            Aggregation.DATA_TYPE_WGS) ||
                        Aggregation.DATA_TYPE_WGS.equals(sampleInstance.getPipelineDataTypeString())) {
                        genomeLanes.add(vesselPosition);
                    }
                }
            }
        }

        return genomeLanes;
    }
}
