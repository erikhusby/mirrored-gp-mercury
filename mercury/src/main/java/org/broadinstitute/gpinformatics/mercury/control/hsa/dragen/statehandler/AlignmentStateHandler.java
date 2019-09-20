package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.statehandler;

import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.AggregationStateDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.FastQListBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AggregationTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Dependent
public class AlignmentStateHandler extends StateHandler {

    @Inject
    private FastQListBuilder fastQListBuilder;

    @Inject
    private AggregationStateDao aggregationStateDao;

    @Override
    public boolean onEnter(State state) throws IOException {

        if (!OrmUtil.proxySafeIsInstance(state, AlignmentState.class)) {
            throw new RuntimeException("Expect only alignment states");
        }

        AlignmentState alignmentState = OrmUtil.proxySafeCast(state, AlignmentState.class);

        for (Task t: alignmentState.getTasks()) {
            if (OrmUtil.proxySafeIsInstance(t, AlignmentTask.class)) {
                AlignmentTask alignmentTask = OrmUtil.proxySafeCast(t, AlignmentTask.class);
                if (!alignmentTask.getOutputDir().exists()) {
                    alignmentTask.getOutputDir().mkdir();
                }

                if (!alignmentTask.getFastQList().exists()) {
                    MercurySample mercurySample = alignmentState.getMercurySamples().iterator().next();
                    IlluminaSequencingRunChamber runChamber = alignmentState.getSequencingRunChambers().iterator().next();

                    Set<SampleInstanceV2> sampleInstancesAtPositionV2 =
                            runChamber.getIlluminaSequencingRun().getSampleCartridge().getContainerRole()
                                    .getSampleInstancesAtPositionV2(runChamber.getLanePosition());
                    Optional<SampleInstanceV2> optionalSampleInstanceV2 = sampleInstancesAtPositionV2.stream()
                            .filter(si -> si.getRootOrEarliestMercurySampleName().equals(mercurySample.getSampleKey()))
                            .findFirst();
                    if (!optionalSampleInstanceV2.isPresent()) {
                        throw new RuntimeException("Failed to find sample " + mercurySample.getSampleKey() + " in lane "
                                                   + runChamber.getIlluminaSequencingRun().getSampleCartridge().getLabel()
                                                   + " " + runChamber.getLaneNumber());
                    }
                    SampleInstanceV2 sampleInstance = optionalSampleInstanceV2.get();
                    String library = sampleInstance.getFirstPcrVessel().getLabel();

                    library = String.format("%s_%s", library, sampleInstance.getMolecularIndexingScheme().getName());
                    boolean foundSample = fastQListBuilder.buildSingle(runChamber, sampleInstance,
                            mercurySample, library, alignmentTask.getFastQList());
                    if (!foundSample) {
                        throw new RuntimeException(
                                "Failed to find sample " + mercurySample.getSampleKey() + " in alignment task " + alignmentTask.getTaskId() );
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean onExit(State state) {
        if (!OrmUtil.proxySafeIsInstance(state, AlignmentState.class)) {
            throw new RuntimeException("Expect only alignment states");
        }

        AlignmentState alignmentState = OrmUtil.proxySafeCast(state, AlignmentState.class);
        for (Task t: alignmentState.getTasksWithStatus(Status.COMPLETE)) {
            AlignmentTask alignmentTask = OrmUtil.proxySafeCast(t, AlignmentTask.class);
            String sampleId = alignmentTask.getFastQSampleId();
            Optional<MercurySample> optSample = alignmentState.getMercurySamples().stream()
                    .filter(ms -> ms.getSampleKey().equals(sampleId)).findFirst();
            if (!optSample.isPresent()) {
                throw new RuntimeException("Failed to find Mercury Sample: " + sampleId + " in state " + alignmentState);
            }

            MercurySample mercurySample = optSample.get();
            List<AggregationState> aggregations = aggregationStateDao.findBySample(mercurySample);
            AggregationState aggregationState = null;
            if (!aggregations.isEmpty()) {
                aggregationState = aggregations.get(aggregations.size() - 1);
            } else {
                String machineName = "AlignAggregation" + mercurySample.getSampleKey() + "_" +
                                     DateUtils.getFileDateTime(new Date());
                FiniteStateMachine finiteStateMachine = new FiniteStateMachine();
                finiteStateMachine.setStateMachineName(machineName);
                finiteStateMachine.setStatus(Status.RUNNING);
                String stateName = "Aggregation_" + mercurySample.getSampleKey();
                Set<MercurySample> samples = Collections.singleton(mercurySample);
                aggregationState = new AggregationState(stateName, finiteStateMachine,
                        samples, alignmentState.getSequencingRunChambers());
                mercurySample.addState(aggregationState);
                aggregationState.setStartState(true);
                aggregationState.setAlive(true);

                List<State> states = new ArrayList<>();
                states.add(aggregationState);

                AggregationTask aggregationTask = new AggregationTask();
                aggregationTask.setTaskName("AggregationTask_" + mercurySample.getSampleKey());
                aggregationState.addTask(aggregationTask);

                finiteStateMachine.setStateMachineName("Aggregation_" + mercurySample.getSampleKey());
                finiteStateMachine.setStates(states);
                finiteStateMachine.setTransitions(Collections.emptyList());
            }
            aggregationState.getTasks().add(alignmentTask);

            // TODO in a dao free
            aggregationStateDao.persist(aggregationState);
            aggregationStateDao.flush();
        }
        return true;
    }
}
