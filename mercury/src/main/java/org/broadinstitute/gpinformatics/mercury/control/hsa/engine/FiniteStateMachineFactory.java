package org.broadinstitute.gpinformatics.mercury.control.hsa.engine;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.datawh.ExtractTransform;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.SampleSheetBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenFolderUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForFileTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.GenericState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Transition;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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

    public FiniteStateMachine createFiniteStateMachineForRun(IlluminaSequencingRun run, String runName,
                                                             MessageCollection messageCollection) {
        FiniteStateMachine finiteStateMachine = createFiniteStateMachineForRunDaoFree(run, runName, messageCollection);
        if (!messageCollection.hasErrors()) {
            stateMachineDao.persist(finiteStateMachine);
        }

        return finiteStateMachine;
    }

    @DaoFree
    public FiniteStateMachine createFiniteStateMachineForRunDaoFree(IlluminaSequencingRun run, String runName,
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

        State demultiplex = new DemultiplexState(finiteStateMachine.getDateQueuedString(), run.getSequencingRunChambers(), finiteStateMachine);
        states.add(demultiplex);

        DragenFolderUtil dragenFolderUtil = new DragenFolderUtil(dragenConfig, run, finiteStateMachine.getDateQueuedString());

        // Default is to create 1 Sample Sheet for all of the lanes
        File SampleSheetFile = new File(dragenFolderUtil.getAnalysisFolder(), "SampleSheet_hsa.csv");
        SampleSheetBuilder.SampleSheet sampleSheet = sampleSheetBuilder.makeSampleSheet(run);
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
        Set<MercurySample> mercurySamples = sampleSheet.getData().getMercurySamples();
        AlignmentState alignmentState = new AlignmentState("Alignment_" + runName, finiteStateMachine, mercurySamples);
        states.add(alignmentState);

        Set<String> samplesAligned = new HashSet<>();
        for (SampleSheetBuilder.SampleData sampleData: sampleSheet.getData().getMapSampleNameToData().values()) {
            if (!samplesAligned.add(sampleData.getSampleName())) {
                continue;
            }
            File referenceFile = new File("/staging/reference/hg38/v1");
            File fastQList = dragenFolderUtil.getFastQListFile();
            File outputDir = new File(dragenFolderUtil.getFastQFolder(), sampleData.getSampleName());
            File intermediateResults = new File("/staging/out");
            AlignmentTask alignmentTask = new AlignmentTask(referenceFile, fastQList, sampleData.getSampleName(),
                    outputDir, intermediateResults, sampleData.getSampleName(), sampleData.getSampleName());
            alignmentTask.setTaskName("Alignment_" + sampleData.getSampleName() + " " + runName);
            alignmentState.addTask(alignmentTask);
            samplesAligned.add(sampleData.getSampleName());
        }

        AlignmentMetricsTask alignmentMetricsTask = new AlignmentMetricsTask();
        alignmentMetricsTask.setTaskName("Alignment_Metrics " + runName);
        alignmentState.addExitTask(alignmentMetricsTask);

        Transition demuxToAlignment = new Transition("Demultiplexing To Alignment", finiteStateMachine);
        demuxToAlignment.setFromState(demultiplex);
        demuxToAlignment.setToState(alignmentState);
        transitions.add(demuxToAlignment);

        finiteStateMachine.setStatus(Status.RUNNING);
        finiteStateMachine.setStates(states);
        finiteStateMachine.setTransitions(transitions);
        return finiteStateMachine;
    }

    private boolean writeFile(File f, String content, MessageCollection messageCollection) {
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
