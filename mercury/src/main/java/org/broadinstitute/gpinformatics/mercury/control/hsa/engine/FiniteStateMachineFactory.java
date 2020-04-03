package org.broadinstitute.gpinformatics.mercury.control.hsa.engine;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.AggregationStateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.SampleSheetBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AggregationTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.Bcl2FastqFolderUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.BclDemultiplexTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.CrosscheckFingerprintTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.CrosscheckFingerprintUploadTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenFolderUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.FingerprintTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.FingerprintUploadTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.GsUtilTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForFileTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForReviewTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.CrosscheckFingerprintState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FingerprintState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.GenericState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Transition;
import org.broadinstitute.gpinformatics.mercury.control.run.ConcordanceCalculator;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.hsa.AggregationActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.hsa.FingerprintWorkflowActionBean;
import org.jetbrains.annotations.NotNull;

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

    @Inject
    private AggregationStateDao aggregationStateDao;

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
        FiniteStateMachine finiteStateMachine =
                createDemultiplexMachine(run, run.getSequencingRunChambers(), messageCollection);
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

            String gender = mercurySample.getSampleData().getGender();
            if (!StringUtils.isBlank(gender)) {
                gender = gender.toUpperCase();
            }
            AlignmentTask alignmentTask = new AlignmentTask(referenceFile, fastQList, sampleData.getSampleName(),
                    outputDir, intermediateResults, sampleData.getSampleName(), sampleData.getSampleName(),
                    new File(AggregationActionBean.ReferenceGenome.HG38.getContamFile()),
                    new File(AggregationActionBean.ReferenceGenome.HG38.getCoverageBedFile()), gender);
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

    public List<FiniteStateMachine> createAggegations(
            Map<String, List<AggregationActionBean.SampleRunData>> sampleToLanes,
            boolean redoDemultiplex, boolean crossCheckFingerprint,
            AggregationActionBean.ReferenceGenome referenceGenome,
            boolean useConfig, String configFilePath,
            CramFileNameBuilder.CramFilenameFormat cramFilenameFormat,
            boolean allowMultipleAggregationTasks, MessageCollection messageCollection) {
        List<FiniteStateMachine> finiteStateMachines = new ArrayList<>();
        Map<String, IlluminaSequencingRun> mapNameToRun =
                createDemultiplexStateMachines(sampleToLanes, redoDemultiplex, messageCollection, finiteStateMachines);

        for (Map.Entry<String, List<AggregationActionBean.SampleRunData>> entry: sampleToLanes.entrySet()) {
            String sampleKey = entry.getKey();
            List<AggregationActionBean.SampleRunData> sampleRunData = sampleToLanes.get(sampleKey);
            Set<IlluminaSequencingRunChamber> aggregationChambers = new HashSet<>();
            for (AggregationActionBean.SampleRunData sampleRun: sampleRunData) {
                IlluminaSequencingRun run = mapNameToRun.get(sampleRun.getRunName());
                for (IlluminaSequencingRunChamber runChamber: run.getSequencingRunChambers()) {
                    if (sampleRun.getLanes().contains(runChamber.getLanePosition())) {
                        aggregationChambers.add(runChamber);
                    }
                }
            }
            MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleKey);
            FiniteStateMachine aggegation = createAggegation(mercurySample, aggregationChambers, crossCheckFingerprint,
                    referenceGenome, useConfig, configFilePath, cramFilenameFormat, allowMultipleAggregationTasks);
            finiteStateMachines.add(aggegation);
        }
        return finiteStateMachines;
    }

    @NotNull
    private Map<String, IlluminaSequencingRun> createDemultiplexStateMachines(
            Map<String, List<AggregationActionBean.SampleRunData>> sampleToLanes, boolean redoDemultiplex,
            MessageCollection messageCollection, List<FiniteStateMachine> finiteStateMachines) {
        Set<String> uniqueRuns = sampleToLanes.values().stream().flatMap(List::stream)
                .map(AggregationActionBean.SampleRunData::getRunName).collect(
                        Collectors.toSet());

        Map<String, IlluminaSequencingRun> mapNameToRun = new HashMap<>();
        // Demultiplex All Missing Run Chambers
        for (String runName: uniqueRuns) {
            IlluminaSequencingRun run = illuminaSequencingRunDao.findByRunName(runName);
            mapNameToRun.put(runName, run);
            IlluminaFlowcell flowcell = OrmUtil.proxySafeCast(run.getSampleCartridge(), IlluminaFlowcell.class);
            VesselPosition[] expectedLanes = flowcell.getFlowcellType().getVesselGeometry().getVesselPositions();
            List<VesselPosition> lanes = run.getSequencingRunChambers().stream()
                    .map(IlluminaSequencingRunChamber::getLanePosition)
                    .collect(Collectors.toList());

            // Lanes aren't automatically added at registration, check for existence and create if need be.
            if (lanes.isEmpty() || lanes.size() < expectedLanes.length) {
                for (VesselPosition vesselPosition: expectedLanes) {
                    if (!lanes.contains(vesselPosition)) {
                        IlluminaSequencingRunChamber sequencingRunChamber =
                                new IlluminaSequencingRunChamber(run, vesselPosition);
                        run.addSequencingRunChamber(sequencingRunChamber);
                    }
                }
            }

            Set<IlluminaSequencingRunChamber> chambersNotDemultiplexed = new HashSet<>();
            for (IlluminaSequencingRunChamber runChamber: run.getSequencingRunChambers()) {
                List<DemultiplexState> demultiplexStates =
                        runChamber.fetchAllStatesOfType(DemultiplexState.class);
                if (demultiplexStates.isEmpty() || redoDemultiplex) {
                    chambersNotDemultiplexed.add(runChamber);
                }
            }

            if (!chambersNotDemultiplexed.isEmpty()) {
                FiniteStateMachine finiteStateMachine = createDemultiplexMachine(run, chambersNotDemultiplexed,
                        messageCollection);
                finiteStateMachines.add(finiteStateMachine);
            }
        }
        return mapNameToRun;
    }

    public FiniteStateMachine createAggegation(MercurySample mercurySample,
                                               Set<IlluminaSequencingRunChamber> aggregationChambers,
                                               boolean crosscheckFingerprint,
                                               AggregationActionBean.ReferenceGenome referenceGenome,
                                               boolean useConfig, String configFilePath,
                                               CramFileNameBuilder.CramFilenameFormat cramFilenameFormat,
                                               boolean allowMultipleAggregationTasks) {
        String sampleKey = mercurySample.getSampleKey();

        AggregationState aggregationState = aggregationStateDao.findBySampleWithChambers(mercurySample, aggregationChambers);
        FiniteStateMachine aggregationStateMachine = null;
        List<State> states = new ArrayList<>();
        if (aggregationState == null || allowMultipleAggregationTasks) {
            aggregationStateMachine = new FiniteStateMachine();
            String aggStateName = "Agg" + sampleKey;
            aggregationState = new AggregationState(aggStateName, aggregationStateMachine,
                    Collections.singleton(mercurySample), aggregationChambers);
            aggregationStateMachine.setStateMachineName("Agg_" + sampleKey);
            states.add(aggregationState);

            AlignmentMetricsTask aggregationMetricsTask = new AlignmentMetricsTask();
            aggregationMetricsTask.setTaskName("AggMetric_" + sampleKey);
            aggregationState.addExitTask(aggregationMetricsTask);

            File referenceFile = new File(referenceGenome.getPath());
            File outputDir = DragenFolderUtil.newSampleAggregation(dragenConfig, sampleKey);
            File fastQList = new File(outputDir, DragenFolderUtil.FASTQ_LIST_CSV);
            File intermediateResults = new File(dragenConfig.getIntermediateResults());
            File contaminationFile = referenceGenome.getContamFile() == null ? null : new File(referenceGenome.getContamFile());
            File bedFile = new File(referenceGenome.getCoverageBedFile());
            File bed2File = referenceGenome.getCoverageBed2File() == null ? null : new File(referenceGenome.getCoverageBed2File());
            File bed3File = referenceGenome.getCoverageBed3File() == null ? null : new File(referenceGenome.getCoverageBed3File());

            AggregationTask aggregationTask = null;
            String taskname = "Agg_" + sampleKey + "_" + System.currentTimeMillis();
            String outputFilePrefix = CramFileNameBuilder.process(mercurySample, cramFilenameFormat);
            File cramFile = new File(outputDir, outputFilePrefix + ".cram");
            if (useConfig) {
                aggregationTask = new AggregationTask(referenceFile, fastQList, sampleKey,
                        outputDir, intermediateResults, outputFilePrefix, sampleKey, contaminationFile, bedFile, bed2File,
                        bed3File, new File(configFilePath));
            }else  {
                aggregationTask = new AggregationTask(referenceFile, fastQList, sampleKey,
                        outputDir, intermediateResults, outputFilePrefix, sampleKey, contaminationFile, bedFile, bed2File,
                        bed3File);
            }
            aggregationTask.setTaskName(taskname);

            aggregationState.addTask(aggregationTask);
            aggregationState.setAlive(true);
            aggregationState.setStartState(true);

            if (crosscheckFingerprint) {
                CrosscheckFingerprintState crosscheckFingerprintState = new CrosscheckFingerprintState(
                        "Crosscheck_" + mercurySample.getSampleKey(), mercurySample, aggregationStateMachine, aggregationChambers);

                File haplotypeDatabase = new File(referenceGenome.getHaplotypeDatabase());
                File refSeq = new File(referenceGenome.getFasta());
                File outputFilePrefixFp = new File(outputDir, sampleKey + ".crosscheck.txt");
                CrosscheckFingerprintTask task = new CrosscheckFingerprintTask(cramFile, haplotypeDatabase,
                        outputFilePrefixFp.getPath(), -5, refSeq); //TODO JW Lod Threshold
                task.setTaskName("CrossCheckFingerprint" + sampleKey);
                crosscheckFingerprintState.addTask(task);

                CrosscheckFingerprintUploadTask uploadTask = new CrosscheckFingerprintUploadTask();
                uploadTask.setTaskName("CrosscheckUpload" + sampleKey);
                crosscheckFingerprintState.addExitTask(uploadTask);
                states.add(crosscheckFingerprintState);

                Transition transition = new Transition("Aggregation To Crosscheck FP", aggregationStateMachine);
                transition.setFromState(aggregationState);
                transition.setToState(crosscheckFingerprintState);
                aggregationStateMachine.setTransitions(Collections.singletonList(transition));
            }
            List<Transition> transitions = new ArrayList<>();

            FingerprintState fingerprintState = new FingerprintState("FP_" + sampleKey, mercurySample,
                    aggregationStateMachine, aggregationChambers);

            File vcfFile = new File(outputDir, outputFilePrefix + ".vcf.gz");
            FingerprintTask fpTask = new FingerprintTask(
                    cramFile, vcfFile,
                    new File(referenceGenome.getHaplotypeDatabase()), outputFilePrefix,
                    referenceFile);
            taskname = "FP_" + sampleKey + "_" + System.currentTimeMillis();
            fpTask.setTaskName(taskname);
            fingerprintState.addTask(fpTask);


            Transition transition = new Transition("AggToFp", aggregationStateMachine);
            transition.setFromState(aggregationState);
            transition.setToState(fingerprintState);
            transitions.add(transition);

            states.add(fingerprintState);

            String waitForReview = "DataReview";
            GenericState dataReviewState = new GenericState(waitForReview, aggregationStateMachine);
            dataReviewState.addSample(mercurySample);
            WaitForReviewTask waitForReviewTask = new WaitForReviewTask(waitForReview);
            dataReviewState.addTask(waitForReviewTask);
            states.add(dataReviewState);

            Transition fpToReview = new Transition("FPToReview_" + sampleKey, aggregationStateMachine);
            fpToReview.setFromState(fingerprintState);
            fpToReview.setToState(dataReviewState);
            transitions.add(fpToReview);

            State uploadState = new GenericState("Upload_" + sampleKey, aggregationStateMachine, Collections.emptySet());
            String gsBucket = dragenConfig.getGsBucket();
            GsUtilTask gsUtilTask = GsUtilTask.cp(cramFile, gsBucket);
            uploadState.addTask(gsUtilTask);
            states.add(uploadState);

            Transition reviewToUpload = new Transition("ReviewToUpload_" + sampleKey, aggregationStateMachine);
            reviewToUpload.setFromState(dataReviewState);
            reviewToUpload.setToState(uploadState);
            transitions.add(reviewToUpload);

            aggregationStateMachine.setStates(states);
            aggregationStateMachine.setTransitions(transitions);
            aggregationStateMachine.setStatus(Status.RUNNING);
        } else {
            log.info("Agg already exists - restarting machine: " + aggregationState);
            aggregationStateMachine = aggregationState.getFiniteStateMachine();
            aggregationStateMachine.setStatus(Status.RUNNING);
            aggregationState.setAlive(true);
            aggregationState.setStartState(true);
        }

        return aggregationStateMachine;
    }

    public FiniteStateMachine createDemultiplexMachine(IlluminaSequencingRun run, Set<IlluminaSequencingRunChamber> runChambers,
                                                       MessageCollection messageCollection) {

        FiniteStateMachine finiteStateMachine = new FiniteStateMachine();
        finiteStateMachine.setStateMachineName("Demultiplex_" + run.getRunName());
        DragenFolderUtil dragenFolderUtil = new DragenFolderUtil(dragenConfig, run,
                finiteStateMachine.getDateQueuedString());
        File runDir = new File(ConcordanceCalculator.convertFilePaths(run.getRunDirectory()));

        File ssFile = new File(dragenFolderUtil.getAnalysisFolder(), "SampleSheet_hsa.csv");
        Set<VesselPosition> lanes =
                runChambers.stream().map(IlluminaSequencingRunChamber::getLanePosition).collect(Collectors.toSet());
        SampleSheetBuilder.SampleSheet sampleSheet = sampleSheetBuilder.makeSampleSheet(
                run, lanes, Collections.emptySet());
        FiniteStateMachineFactory.writeFile(ssFile, sampleSheet.toCsv(), messageCollection);
        Set<MercurySample> samples = new HashSet<>(sampleSheet.getData().getMapSampleToMercurySample().values());

        DemultiplexTask task = new DemultiplexTask(runDir, dragenFolderUtil.getFastQFolder(), ssFile);
        task.setTaskName("Demux_" + run.getRunName());

        DemultiplexState demultiplexState =
                new DemultiplexState(finiteStateMachine.getDateQueuedString(), finiteStateMachine, samples, runChambers);
        demultiplexState.setAlive(true);
        demultiplexState.setStartState(true);
        demultiplexState.addTask(task);

        DemultiplexMetricsTask demultiplexMetricsTask = new DemultiplexMetricsTask();
        demultiplexMetricsTask.setTaskName("Demultiplex_Metrics_" + run.getRunName());
        demultiplexState.addExitTask(demultiplexMetricsTask);

        finiteStateMachine.setStatus(Status.RUNNING);
        finiteStateMachine.setStates(Collections.singletonList(demultiplexState));
        return finiteStateMachine;
    }

    public FiniteStateMachine createBcl2FastMachine(IlluminaSequencingRun run,
                                                       MessageCollection messageCollection) {

        Set<IlluminaSequencingRunChamber> runChambers = run.getSequencingRunChambers();
        Set<VesselPosition> lanes = runChambers.stream()
                .map(IlluminaSequencingRunChamber::getLanePosition)
                .collect(Collectors.toSet());

        IlluminaFlowcell flowcell = OrmUtil.proxySafeCast(run.getSampleCartridge(), IlluminaFlowcell.class);
        VesselPosition[] expectedLanes = flowcell.getFlowcellType().getVesselGeometry().getVesselPositions();
        if (runChambers.isEmpty() || runChambers.size() < expectedLanes.length) {
            for (VesselPosition vesselPosition: expectedLanes) {
                if (!lanes.contains(vesselPosition)) {
                    IlluminaSequencingRunChamber sequencingRunChamber =
                            new IlluminaSequencingRunChamber(run, vesselPosition);
                    run.addSequencingRunChamber(sequencingRunChamber);
                }
            }
        }

        lanes = runChambers.stream()
                .map(IlluminaSequencingRunChamber::getLanePosition)
                .collect(Collectors.toSet());

        FiniteStateMachine finiteStateMachine = new FiniteStateMachine();
        finiteStateMachine.setStateMachineName("Bcl2Fastq_" + run.getRunName());
        Bcl2FastqFolderUtil folderUtil = new Bcl2FastqFolderUtil(run);
        File runDir = new File(run.getRunDirectory());
        File analysisFolder = folderUtil.createNewAnalysisFolder();

        File ssFile = new File(analysisFolder, "SampleSheet_hsa.csv");

        SampleSheetBuilder.SampleSheet sampleSheet = sampleSheetBuilder.makeSampleSheet(
                run, lanes, Collections.emptySet());
        FiniteStateMachineFactory.writeFile(ssFile, sampleSheet.toCsv(), messageCollection);
        Set<MercurySample> samples = new HashSet<>(sampleSheet.getData().getMapSampleToMercurySample().values());

        BclDemultiplexTask task = new BclDemultiplexTask(runDir, analysisFolder, ssFile);
        task.setTaskName("Bcl2Fastq" + run.getRunName());

        DemultiplexState demultiplexState =
                new DemultiplexState(finiteStateMachine.getDateQueuedString(), finiteStateMachine, samples, runChambers);
        demultiplexState.setAlive(true);
        demultiplexState.setStartState(true);
        demultiplexState.addTask(task);

        // TODO Metrics
//        DemultiplexMetricsTask demultiplexMetricsTask = new DemultiplexMetricsTask();
//        demultiplexMetricsTask.setTaskName("Demultiplex_Metrics_" + run.getRunName());
//        demultiplexState.addExitTask(demultiplexMetricsTask);

        finiteStateMachine.setStatus(Status.RUNNING);
        finiteStateMachine.setStates(Collections.singletonList(demultiplexState));
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
            FingerprintState fingerprintState = new FingerprintState(stateName, mercurySample, finiteStateMachine, Collections.emptySet());
            fingerprintState.setStartState(true);
            fingerprintState.setAlive(true);
            states.add(fingerprintState);

            File outputDir = new File(dto.getOutputDirectory());
            File outputFilePrefix = new File(outputDir, "Fingerprint");
            FingerprintTask fpTask = new FingerprintTask(new File(dto.getBamFile()), new File(dto.getVcfFile()),
                    new File(dto.getHaplotypeDatabase()), outputFilePrefix.getPath(),
                    new File(dto.getFasta()));
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

    public FiniteStateMachine createUploadTasks(List<String> sampleKeys, MessageCollection messageCollection) {
        List<File> uploadFiles = new ArrayList<>();
        for (String sampleKey: sampleKeys) {
            try {
                Optional<File> mostRecentAggregation =
                        DragenFolderUtil.findMostRecentAggregation(dragenConfig, sampleKey);
                if (!mostRecentAggregation.isPresent()) {
                    messageCollection.addError("Failed to find aggregation for " + sampleKey);
                } else {
                    uploadFiles.add(mostRecentAggregation.get());
                }
            } catch (Exception e) {
                messageCollection.addError(e.getMessage());
            }
        }

        if (messageCollection.hasErrors()) {
            return null;
        }

        FiniteStateMachine fsm = new FiniteStateMachine();
        fsm.setStateMachineName("Upload");
        fsm.setStatus(Status.RUNNING);

        State state = new GenericState("Upload", fsm, Collections.emptySet());
        state.setAlive(true);
        state.setStartState(true);
        fsm.setStates(Collections.singletonList(state));
        for (File f: uploadFiles) {
            String gsBucket = dragenConfig.getGsBucket();
            GsUtilTask gsUtilTask = GsUtilTask.cp(f, gsBucket);
            state.addTask(gsUtilTask);
        }

        stateMachineDao.persist(fsm);
        return fsm;
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
