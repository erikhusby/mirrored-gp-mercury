package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.statehandler;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.AggregationStateDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.FastQListBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.SampleSheetBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AggregationTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenFolderUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.ReadGroupUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.File;
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

    private static final Log log = LogFactory.getLog(AlignmentStateHandler.class);

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

                File dragenFastQFile = new File(alignmentTask.getFastQList().getParentFile(), DragenFolderUtil.FASTQ_LIST_CSV);
                if (dragenFastQFile.exists()) {
                    Pair<MercurySample, IlluminaSequencingRunChamber> pair = findRunPair(alignmentTask, alignmentState.getSequencingRunChambers(),
                            alignmentState.getMercurySamples());
                    MercurySample mercurySample = pair.getLeft();
                    IlluminaSequencingRunChamber runChamber = pair.getRight();
                    RunCartridge flowcell = runChamber.getIlluminaSequencingRun().getSampleCartridge();

                    Pair<MercurySample, SampleInstanceV2> instancePair = SampleSheetBuilder.
                            findSampleInFlowcellLane(flowcell, runChamber.getLanePosition(), mercurySample);

                    if (instancePair != null) {
                        SampleInstanceV2 sampleInstance = instancePair.getRight();
                        MercurySample sampleSheetSample = instancePair.getLeft();
                        String library = sampleInstance.getSequencingLibraryName();
                        String rgPu = ReadGroupUtil.createRgPu(flowcell.getLabel(), runChamber.getLaneNumber(), sampleInstance.getIndexingSchemeString());
                        String rgId = String.format("%s.%d.%s", flowcell.getLabel(), runChamber.getLaneNumber(),
                                sampleInstance.getIndexingSchemeString());
                        fastQListBuilder.buildSingle(runChamber.getLaneNumber(), rgId, rgPu, mercurySample,
                                sampleSheetSample, library, alignmentTask.getFastQList(), dragenFastQFile);
                    } else {
                        throw new RuntimeException(
                                "Failed to find sample " + mercurySample.getSampleKey() + " in alignment task "
                                + alignmentTask.getTaskId());
                    }
                } else {
                    log.error("No fastq file exists: " + alignmentTask.getFastQList().getPath());
                    return false;
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
                boolean disjoint = Collections.disjoint(aggregationState.getSequencingRunChambers(),
                        alignmentState.getSequencingRunChambers());
                if (disjoint) {
                    for (IlluminaSequencingRunChamber runChamber: alignmentState.getSequencingRunChambers()) {
                        runChamber.addState(aggregationState);
                    }
                }
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

    private Pair<MercurySample, IlluminaSequencingRunChamber> findRunPair(AlignmentTask alignmentTask,
                                                                          Set<IlluminaSequencingRunChamber> runChambers,
                                                                          Set<MercurySample> mercurySamples) {
        File outputDir = alignmentTask.getOutputDir();
        File laneDir = outputDir.getParentFile();
        File dragenAnalysisDir = laneDir.getParentFile();
        File dragenDir = dragenAnalysisDir.getParentFile();
        File runFolder = dragenDir.getParentFile();
        File fastQList = alignmentTask.getFastQList();

        String[] fastqSplit = fastQList.getName().split("_");
        String expectedSample = fastqSplit[0];
        int lane = Integer.parseInt(fastqSplit[1]);

        IlluminaSequencingRunChamber runChamber = null;
        for (IlluminaSequencingRunChamber currChamber: runChambers) {
            if (currChamber.getIlluminaSequencingRun().getRunDirectory().equals(runFolder.getPath())) {
                if (currChamber.getLaneNumber() == lane) {
                    runChamber = currChamber;
                    break;
                }
            }
        }

        MercurySample mercurySample = null;
        for (MercurySample currSample: mercurySamples) {
            if (expectedSample.equals(currSample.getSampleKey())) {
                mercurySample = currSample;
                break;
            }
        }

        return Pair.of(mercurySample, runChamber);
    }
}
