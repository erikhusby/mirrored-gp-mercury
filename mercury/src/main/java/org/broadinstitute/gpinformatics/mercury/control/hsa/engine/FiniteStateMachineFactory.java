package org.broadinstitute.gpinformatics.mercury.control.hsa.engine;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.SampleSheetBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AggregationTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenFolderUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.FingerprintTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.FingerprintUploadTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForFileTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FingerprintState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.GenericState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Transition;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.hsa.AggregationActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.hsa.FingerprintWorkflowActionBean;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encapsulates the business logic related to {@link FiniteStateMachine}s.  This includes the creation
 * of a new state machine entity.
 */
@Dependent
public class FiniteStateMachineFactory {

    private static final Log log = LogFactory.getLog(FiniteStateMachineFactory.class);

    @Inject
    private DragenConfig dragenConfig;

    @Inject
    private SampleSheetBuilder sampleSheetBuilder;

    @Inject
    private StateMachineDao stateMachineDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    public FiniteStateMachine createFiniteStateMachineForRun(IlluminaSequencingRun run, String runName,
                                                             MessageCollection messageCollection) {
        RunCartridge flowcell = run.getSampleCartridge();
        Set<VesselPosition> lanes = new HashSet<>(Arrays.asList(flowcell.getVesselGeometry().getVesselPositions()));
        return createFiniteStateMachineForRun(run, lanes, runName, Collections.emptySet(), messageCollection);
    }

    public FiniteStateMachine createFiniteStateMachineForRun(IlluminaSequencingRun run, String runName,
                                                             Set<String> filterSampleIds,
                                                             MessageCollection messageCollection) {
        RunCartridge flowcell = run.getSampleCartridge();
        Set<VesselPosition> lanes = new HashSet<>(Arrays.asList(flowcell.getVesselGeometry().getVesselPositions()));
        return createFiniteStateMachineForRun(run, lanes, runName, filterSampleIds, messageCollection);
    }

    public FiniteStateMachine createFiniteStateMachineForRun(IlluminaSequencingRun run,
                                                             Set<VesselPosition> lanes,
                                                             String runName,
                                                             Set<String> filterSampleIds,
                                                             MessageCollection messageCollection) {
        List<VesselPosition> runChamberPositions = run.getSequencingRunChambers().stream()
                .map(IlluminaSequencingRunChamber::getLanePosition)
                .collect(Collectors.toList());

        for (VesselPosition vesselPosition: lanes) {
            int laneNumber = Integer.parseInt(vesselPosition.name().replaceAll("LANE", ""));
            if (!runChamberPositions.contains(vesselPosition)) {
                IlluminaSequencingRunChamber sequencingRunChamber = new IlluminaSequencingRunChamber(run, laneNumber);
                run.addSequencingRunChamber(sequencingRunChamber);
            }
        }
        FiniteStateMachine finiteStateMachine = createFiniteStateMachineForRunDaoFree(run, lanes, runName,
                filterSampleIds, messageCollection);
        if (!messageCollection.hasErrors()) {
            stateMachineDao.persist(finiteStateMachine);
        }

        return finiteStateMachine;
    }

    @DaoFree
    public FiniteStateMachine createFiniteStateMachineForRunDaoFree(IlluminaSequencingRun run,
                                                                    Set<VesselPosition> lanes,
                                                                    String runName,
                                                                    Set<String> filterSampleIds,
                                                                    MessageCollection messageCollection) {
        List<State> states = new ArrayList<>();
        List<Transition> transitions = new ArrayList<>();

        FiniteStateMachine finiteStateMachine = new FiniteStateMachine();
        finiteStateMachine.setStateMachineName(runName);

        File runDir = new File(run.getRunDirectory());

        // Create wait for RTA Complete Task
        State sequencingRunComplete = new GenericState("SequencingRunComplete", finiteStateMachine);
        sequencingRunComplete.setStartState(true);
        sequencingRunComplete.setAlive(true);
        File rtaCompleteFile = new File(runDir, "RTAComplete.txt");
        WaitForFileTask waitForFileTask = new WaitForFileTask(rtaCompleteFile);
        waitForFileTask.setTaskName("Waiting for RTAComplete.txt " + rtaCompleteFile.getPath());
        sequencingRunComplete.addTask(waitForFileTask);
        states.add(sequencingRunComplete);

        Date dateQueued = new Date();
        finiteStateMachine.setDateQueued(dateQueued);

        List<MercurySample> mercurySamples = mercurySampleDao.findBySampleKeys(filterSampleIds);

        State demultiplex = new DemultiplexState(finiteStateMachine.getDateQueuedString(), finiteStateMachine,
                new HashSet<>(mercurySamples), run.getSequencingRunChambers());
        states.add(demultiplex);

        DragenFolderUtil dragenFolderUtil = new DragenFolderUtil(dragenConfig, run, finiteStateMachine.getDateQueuedString());

        // Default is to create 1 Sample Sheet for all of the lanes
        File SampleSheetFile = new File(dragenFolderUtil.getAnalysisFolder(), "SampleSheet_hsa.csv");
        SampleSheetBuilder.SampleSheet sampleSheet = sampleSheetBuilder.makeSampleSheet(run, lanes, filterSampleIds);
        writeFile(SampleSheetFile, sampleSheet.toCsv(), messageCollection);

        DemultiplexTask demultiplexTask = new DemultiplexTask(runDir, dragenFolderUtil.getFastQFolder(), SampleSheetFile);
        demultiplexTask.setTaskName("Demultiplex_" + run.getRunName());
        demultiplex.addTask(demultiplexTask);

        DemultiplexMetricsTask demultiplexMetricsTask = new DemultiplexMetricsTask();
        demultiplexMetricsTask.setTaskName("Demultiplex_Metrics_" + run.getRunName());
        demultiplex.addExitTask(demultiplexMetricsTask);

        Transition seqToDemux = new Transition("Sequencing Complete To Demultiplexing", finiteStateMachine);
        seqToDemux.setFromState(sequencingRunComplete);
        seqToDemux.setToState(demultiplex);
        transitions.add(seqToDemux);

        // Alignment
        String fcBarcode = run.getSampleCartridge().getLabel();
        for (SampleSheetBuilder.SampleData sampleData: sampleSheet.getData().getMapSampleNameToData().values()) {
            MercurySample mercurySample =
                    sampleSheet.getData().getMapSampleToMercurySample().get(sampleData.getSampleName());

            IlluminaSequencingRunChamber runChamber = run.getSequencingRunChambers().stream()
                    .filter(seq -> seq.getLaneNumber() == sampleData.getLane())
                    .findFirst().get();
            int laneNumber = runChamber.getLaneNumber();

            String alignName = String.format("Align_%s_%d_%s", fcBarcode, laneNumber, sampleData.getSampleName());
            AlignmentState alignmentState = new AlignmentState(alignName, finiteStateMachine,
                    Collections.singleton(mercurySample), Collections.singleton(runChamber));
            states.add(alignmentState);

            File referenceFile = new File(AggregationActionBean.ReferenceGenome.HG38.getPath());
            File fastQList = dragenFolderUtil.getFastQReadGroupFile(sampleData.getSampleId());
            File outputDir = new File(dragenFolderUtil.getFastQFolder(), sampleData.getSampleName());
            File intermediateResults = new File(dragenConfig.getIntermediateResults());

            AlignmentTask alignmentTask = new AlignmentTask(referenceFile, fastQList, sampleData.getSampleName(),
                    outputDir, intermediateResults, sampleData.getSampleName(), sampleData.getSampleName(),
                    new File(AggregationActionBean.ReferenceGenome.HG38.getContamFile()),
                    new File(AggregationActionBean.ReferenceGenome.HG38.getCoverageBedFile()));
            alignmentTask.setTaskName("Alignment_" + sampleData.getSampleName() + "_" + runName);
            alignmentState.addTask(alignmentTask);

            AlignmentMetricsTask alignmentMetricsTask = new AlignmentMetricsTask();
            alignmentMetricsTask.setTaskName("Alignment_Metric_" + runName);
            alignmentState.addExitTask(alignmentMetricsTask);

            Transition demuxToAlignment = new Transition("DemuxToAlign", finiteStateMachine);
            demuxToAlignment.setFromState(demultiplex);
            demuxToAlignment.setToState(alignmentState);
            transitions.add(demuxToAlignment);
        }

        finiteStateMachine.setStatus(Status.RUNNING);
        finiteStateMachine.setStates(states);
        finiteStateMachine.setTransitions(transitions);
        return finiteStateMachine;
    }

    public FiniteStateMachine createAggegation(List<AggregationActionBean.SampleRunData> sampleRunData, MercurySample mercurySample,
                                               boolean createFingerprint, MessageCollection messageCollection) {
        List<State> states = new ArrayList<>();
        List<Transition> transitions = new ArrayList<>();

        FiniteStateMachine finiteStateMachine = new FiniteStateMachine();
        String sampleKey = mercurySample.getSampleKey();
        finiteStateMachine.setStateMachineName("Agg_" + sampleKey);

        Set<IlluminaSequencingRunChamber> missingChambers = new HashSet<>();
        Set<IlluminaSequencingRunChamber> aggChambers = new HashSet<>();
        Set<IlluminaSequencingRunChamber> alignmentChambers = new HashSet<>();
        Set<IlluminaSequencingRunChamber> fingerprintChambers = new HashSet<>();
        Set<Task> aggDemultiplex = new HashSet<>();

        Map<String, Set<VesselPosition>> lanesToDemux = new HashMap<>();
        Map<String, IlluminaSequencingRun> mapNameToRun = new HashMap<>();

        for (AggregationActionBean.SampleRunData sampleRunDatum: sampleRunData) {
            IlluminaSequencingRun run = illuminaSequencingRunDao.findByRunName(sampleRunDatum.getRunName());
            IlluminaFlowcell flowcell = OrmUtil.proxySafeCast(run.getSampleCartridge(), IlluminaFlowcell.class);
            VesselPosition[] expectedLanes = flowcell.getFlowcellType().getVesselGeometry().getVesselPositions();
            List<VesselPosition> lanes = run.getSequencingRunChambers().stream()
                    .map(IlluminaSequencingRunChamber::getLanePosition)
                    .collect(Collectors.toList());

            if (!mapNameToRun.containsKey(run.getRunName())) {
                mapNameToRun.put(run.getRunName(), run);
            }

            // Lanes aren't automatically added at registration, check for existence
            if (lanes.isEmpty() || lanes.size() < expectedLanes.length) {
                for (VesselPosition vesselPosition: expectedLanes) {
                    if (!lanes.contains(vesselPosition)) {
                        IlluminaSequencingRunChamber sequencingRunChamber =
                                new IlluminaSequencingRunChamber(run, vesselPosition);
                        run.addSequencingRunChamber(sequencingRunChamber);
                    }
                }
            }

            // TODO the export is the sample not the root
            for (VesselPosition vesselPosition: sampleRunDatum.getLanes()) {
                boolean foundFastqs = false;
                for (IlluminaSequencingRunChamber runChamber: run.getSequencingRunChambers()) {
                    if (runChamber.getLanePosition() == vesselPosition) {
                        Optional<DemultiplexState> recentDemultiplex =
                                runChamber.getRecentStateWithSample(DemultiplexState.class, mercurySample);
                        if (recentDemultiplex.isPresent()) {
                            foundFastqs = true;
                            aggChambers.add(runChamber);
                            aggDemultiplex.addAll(recentDemultiplex.get().getTasks());
                        }

                        if (createFingerprint) {
                            Optional<FingerprintState> fingerprintStateOptional =
                                    runChamber.getRecentStateWithSample(FingerprintState.class, mercurySample);
                            if (!fingerprintStateOptional.isPresent()) {
                                fingerprintChambers.add(runChamber);
                                Optional<AlignmentState> alignmentOptional =
                                        runChamber.getRecentStateWithSample(AlignmentState.class, mercurySample);
                                if (!alignmentOptional.isPresent()) {
                                    alignmentChambers.add(runChamber);
                                }
                            }
                        }
                    }
                }

                if (!foundFastqs) {
                    if (!lanesToDemux.containsKey(run.getRunName())) {
                        lanesToDemux.put(run.getRunName(), new HashSet<>());
                    }
                    lanesToDemux.get(run.getRunName()).add(vesselPosition);
                    IlluminaSequencingRunChamber sequencingRunChamber = run.getSequencingRunChamber(vesselPosition);
                    missingChambers.add(sequencingRunChamber);
                    aggChambers.add(sequencingRunChamber);
                }
            }
        }

        // Single Demux State made up of many demux tasks depending on how many flowcells
        String stateName = finiteStateMachine.getDateQueuedString();
        Set<MercurySample> allSampleSet = new HashSet<>();

        Set<Task> demultiplexTasks = new HashSet<>();
        for (Map.Entry<String, Set<VesselPosition>> entry: lanesToDemux.entrySet()) {
            IlluminaSequencingRun run = mapNameToRun.get(entry.getKey());
            Set<VesselPosition> lanes = entry.getValue();
            DragenFolderUtil dragenFolderUtil = new DragenFolderUtil(dragenConfig, run,
                    finiteStateMachine.getDateQueuedString());
            File runDir = new File(run.getRunDirectory());

            File ssFile = new File(dragenFolderUtil.getAnalysisFolder(), "SampleSheet_hsa.csv");
            SampleSheetBuilder.SampleSheet sampleSheet = sampleSheetBuilder.makeSampleSheet(
                    run, lanes, Collections.emptySet());
            FiniteStateMachineFactory.writeFile(ssFile, sampleSheet.toCsv(), messageCollection);
            allSampleSet.addAll(sampleSheet.getData().getMapSampleToMercurySample().values());

            DemultiplexTask task = new DemultiplexTask(runDir, dragenFolderUtil.getFastQFolder(), ssFile);
            task.setTaskName("Demux_" + run.getRunName() + "_" + sampleKey);
            demultiplexTasks.add(task);

            aggDemultiplex.add(task);
        }

        DemultiplexState demultiplexState =
                new DemultiplexState(stateName, finiteStateMachine, allSampleSet, missingChambers);
        demultiplexState.addTasks(demultiplexTasks);

        Set<MercurySample> aggSampleSet = Collections.singleton(mercurySample);
        String aggStateName = "Agg" + sampleKey;
        AggregationState aggregationState = new AggregationState(
                aggStateName, finiteStateMachine, aggSampleSet, aggChambers);

        File referenceFile = new File(AggregationActionBean.ReferenceGenome.HG38.getPath());
        File outputDir = DragenFolderUtil.newSampleAggregation(dragenConfig, sampleKey);
        File fastQList = new File(outputDir, DragenFolderUtil.FASTQ_LIST_CSV);
        File intermediateResults = new File(dragenConfig.getIntermediateResults());
        File contaminationFile = new File(AggregationActionBean.ReferenceGenome.HG38.getContamFile());
        File bedFile = new File(AggregationActionBean.ReferenceGenome.HG38.getCoverageBedFile());

        AggregationTask aggregationTask = new AggregationTask(referenceFile, fastQList, sampleKey,
                outputDir, intermediateResults, sampleKey, sampleKey, contaminationFile, bedFile);
        aggregationTask.setTaskName("Agg_" + sampleKey );

        aggregationState.addTask(aggregationTask);

        AlignmentMetricsTask aggregationMetricsTask = new AlignmentMetricsTask();
        aggregationMetricsTask.setTaskName("AggMetric_" + sampleKey);
        aggregationState.addExitTask(aggregationMetricsTask);

        boolean demuxRequired = !demultiplexState.getTasks().isEmpty();
        if (demuxRequired) {
            DemultiplexMetricsTask demultiplexMetricsTask = new DemultiplexMetricsTask();
            demultiplexMetricsTask.setTaskName("DemuxMetricsAgg");
            demultiplexState.addExitTask(demultiplexMetricsTask);

            demultiplexState.setStartState(true);
            demultiplexState.setAlive(true);
            states.add(demultiplexState);
        }

        AlignmentState alignmentState = new AlignmentState("Align_" + mercurySample.getSampleKey(), finiteStateMachine, aggSampleSet, alignmentChambers);
        boolean alignmentsRequired = !alignmentChambers.isEmpty();
        if (alignmentsRequired) {
            states.add(alignmentState);
            for (IlluminaSequencingRunChamber runChamber: alignmentChambers) {
                DragenFolderUtil dragenFolderUtil = new DragenFolderUtil(dragenConfig, runChamber.getIlluminaSequencingRun());
                File fastQFolder = dragenFolderUtil.getFastQFolder();
                int lane = runChamber.getLaneNumber();
                File laneOutputFolder = dragenFolderUtil.newlaneFolder(fastQFolder, lane);
                File fastQListFile = DragenFolderUtil.fastQFileSampleLane(mercurySample, lane, dragenFolderUtil.getReportsFolder());
                AlignmentTask alignmentTask = new AlignmentTask(referenceFile, fastQListFile, mercurySample.getSampleKey(),
                        laneOutputFolder, intermediateResults, mercurySample.getSampleKey(), mercurySample.getSampleKey(),
                        new File(AggregationActionBean.ReferenceGenome.HG38.getContamFile()),
                        new File(AggregationActionBean.ReferenceGenome.HG38.getCoverageBedFile()));
                String fc = runChamber.getIlluminaSequencingRun().getSampleCartridge().getLabel();
                String taskSuffix = "_" + fc + "_" + lane + "_" + DateUtils.getFileDateTime(new Date());
                alignmentTask.setTaskName("Alignment_" + taskSuffix);
                alignmentState.addTask(alignmentTask);

                String name = "FP_" + taskSuffix;

                FingerprintState fingerprintState = new FingerprintState(stateName, mercurySample, finiteStateMachine, runChamber);

                File outputFilePrefix = new File(laneOutputFolder, "Fingerprint");
                File bamFile = dragenFolderUtil.getBamFile(mercurySample.getSampleKey(), lane);
                File vcfFile = dragenFolderUtil.getVcfFile(mercurySample.getSampleKey(), lane);
                File haplotypeDatabse =
                        new File(AggregationActionBean.ReferenceGenome.HG38.getHaplotypeDatabase());
                File referenceSequence =
                        new File(AggregationActionBean.ReferenceGenome.HG38.getFasta());
                FingerprintTask fpTask = new FingerprintTask(
                        bamFile, vcfFile,
                        haplotypeDatabse, outputFilePrefix.getPath(),
                        referenceSequence);
                fpTask.setTaskName(name);
                fingerprintState.addTask(fpTask);

                name = "FPUp" + taskSuffix;
                FingerprintUploadTask fingerprintUploadTask = new FingerprintUploadTask();
                fingerprintUploadTask.setTaskName(name);
                fingerprintState.addExitTask(fingerprintUploadTask);

                Transition transition = new Transition("AlignToFp", finiteStateMachine);
                transition.setFromState(alignmentState);
                transition.setToState(fingerprintState);
                transitions.add(transition);

                states.add(fingerprintState);
            }

            AlignmentMetricsTask alignmentMetricsTask = new AlignmentMetricsTask();
            alignmentMetricsTask.setTaskName("AlignmentMetrics");
            alignmentState.addExitTask(alignmentMetricsTask);

            if (demuxRequired) {
                Transition transition = new Transition("DemuxToAlignment", finiteStateMachine);
                transition.setFromState(demultiplexState);
                transition.setToState(alignmentState);
                transitions.add(transition);
            } else {
                alignmentState.setAlive(true);
                alignmentState.setStartState(true);
                Transition transition = new Transition("AlignmentToAgg", finiteStateMachine);
                transition.setFromState(alignmentState);
                transition.setToState(aggregationState);
                transitions.add(transition);
            }
        }

        // TODO This does the aggregation without waiting for the fingerprint
        states.add(aggregationState);
        if (demuxRequired) {
            Transition transition = new Transition("AggregationTransition", finiteStateMachine);
            if (alignmentsRequired) {
                transition.setFromState(alignmentState);
            } else {
                transition.setFromState(demultiplexState);
            }
            transition.setToState(aggregationState);
            transitions.add(transition);
        }

        if (!alignmentsRequired && !demuxRequired) {
            aggregationState.setAlive(true);
            aggregationState.setStartState(true);
        }

        finiteStateMachine.setStatus(Status.RUNNING);
        finiteStateMachine.setStates(states);
        finiteStateMachine.setTransitions(transitions);
        return finiteStateMachine;
    }

    public List<FiniteStateMachine> createFingerprintTasks(List<FingerprintWorkflowActionBean.AlignmentDirectoryDto> alignmentDtos) {
        List<String> sampleKeys = alignmentDtos.stream()
                .map(FingerprintWorkflowActionBean.AlignmentDirectoryDto::getSampleKey)
                .collect(Collectors.toList());
        Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(sampleKeys);
        List<FiniteStateMachine> stateMachines =
                createFingerprintTasksDaoFree(alignmentDtos, mapIdToMercurySample);
        stateMachineDao.persistAll(stateMachines);
        return stateMachines;
    }

    @DaoFree
    private List<FiniteStateMachine> createFingerprintTasksDaoFree(
            List<FingerprintWorkflowActionBean.AlignmentDirectoryDto> alignmentDtos,
            Map<String, MercurySample> mapIdToMercurySample) {
        List<FiniteStateMachine> finiteStateMachines = new ArrayList<>();
        for (FingerprintWorkflowActionBean.AlignmentDirectoryDto dto: alignmentDtos) {
            List<State> states = new ArrayList<>();

            String name = "FP_" + dto.getSampleKey() + "_" + DateUtils.getFileDateTime(new Date());
            String stateName = "FP_State" + dto.getSampleKey() + "_" + DateUtils.getFileDateTime(new Date());
            FiniteStateMachine finiteStateMachine = new FiniteStateMachine();
            finiteStateMachine.setStateMachineName(name);

            MercurySample mercurySample = mapIdToMercurySample.get(dto.getSampleKey());
            // TODO Fix this
            FingerprintState fingerprintState = new FingerprintState(stateName, mercurySample, finiteStateMachine, null);
            fingerprintState.setStartState(true);
            fingerprintState.setAlive(true);
            states.add(fingerprintState);

            File outputDir = new File(dto.getOutputDirectory());
            File outputFilePrefix = new File(outputDir, "Fingerprint");
            FingerprintTask fpTask = new FingerprintTask(new File(dto.getBamFile()), new File(dto.getVcfFile()),
                    new File(dto.getHaplotypeDatabase()), outputFilePrefix.getPath(),
                    new File(AggregationActionBean.ReferenceGenome.HG38.getFasta()));
            fpTask.setTaskName(name);
            fingerprintState.addTask(fpTask);

            name = "FP_Upload_" + dto.getSampleKey() + "_" + DateUtils.getFileDateTime(new Date());
            FingerprintUploadTask fingerprintUploadTask = new FingerprintUploadTask();
            fingerprintUploadTask.setTaskName(name);
            fingerprintState.addExitTask(fingerprintUploadTask);

            finiteStateMachine.setStatus(Status.RUNNING);
            finiteStateMachine.setStates(states);
            finiteStateMachine.setTransitions(Collections.emptyList());
            finiteStateMachines.add(finiteStateMachine);
        }
        return finiteStateMachines;
    }

    public static boolean writeFile(File f, String content, MessageCollection messageCollection) {
        Writer fw = null;
        try {
            fw = new FileWriter(f, false);
            IOUtils.write(content, fw);
            return true;
        } catch (IOException e) {
            String errMsg = "Error writing file" + f.getPath();
            log.error(errMsg, e);
            messageCollection.addError(errMsg);
            return false;
        } finally {
            IOUtils.closeQuietly(fw);
        }
    }

    public void setDragenConfig(DragenConfig dragenConfig) {
        this.dragenConfig = dragenConfig;
    }
}
