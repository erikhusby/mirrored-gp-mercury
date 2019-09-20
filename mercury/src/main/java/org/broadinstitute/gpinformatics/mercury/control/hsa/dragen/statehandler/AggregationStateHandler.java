package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.statehandler;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.enterprise.context.Dependent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Dependent
public class AggregationStateHandler extends StateHandler {

    /**
     * Only want to enter if its in a state ready to aggregate
     * @param state
     * @return
     * @throws IOException
     */
    @Override
    public boolean onEnter(State state) throws IOException {
        if (!OrmUtil.proxySafeIsInstance(state, AggregationState.class)) {
            throw new RuntimeException("Expect only aggregation states");
        }

        /**
         * Make some determination if the state should be run or not
         */
        AggregationState aggregationState = OrmUtil.proxySafeCast(state, AggregationState.class);
        for (AlignmentTask alignmentTask : aggregationState.getAlignmentTasks()) {
            RunCartridge sampleCartridge = alignmentTask.getState().getRun().getSampleCartridge();
            Set<VesselPosition> lanes = alignmentTask.getState().getSequencingRunChambers().stream()
                    .map(IlluminaSequencingRunChamber::getLanePosition)
                    .collect(Collectors.toSet());

            List<LabBatch> labBatches = fetchFlowcellTicketForLabBatch(sampleCartridge);

            for (VesselPosition lane: lanes) {
                for (SampleInstanceV2 sampleInstanceV2: sampleCartridge.getContainerRole().getSampleInstancesAtPositionV2(lane)) {
                    if (sampleInstanceV2.getRootOrEarliestMercurySampleName().equals(alignmentTask.getFastQSampleId())) {

                    }
                }
            }
        }
        return true;
    }

    public List<LabBatch> fetchFlowcellTicketForLabBatch(LabVessel labVessel) {
        List<LabBatch> results = new ArrayList<>();

        LabVesselSearchDefinition.VesselBatchTraverserCriteria downstreamBatchFinder =
                new LabVesselSearchDefinition.VesselBatchTraverserCriteria();
        if( labVessel.getContainerRole() != null ) {
            labVessel.getContainerRole().applyCriteriaToAllPositions(
                    downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Ancestors);
        } else {
            labVessel.evaluateCriteria(
                    downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Ancestors);
        }

        for ( LabBatch labBatch : downstreamBatchFinder.getLabBatches() ) {
            if( labBatch.getLabBatchType() == LabBatch.LabBatchType.FCT
                || labBatch.getLabBatchType() == LabBatch.LabBatchType.MISEQ ) {
                results.add(labBatch);
            }
        }
        return results;
    }
}
