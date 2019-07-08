package org.broadinstitute.gpinformatics.mercury.control.hsa.engine;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.datawh.ExtractTransform;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.SampleSheetBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForFileTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.GenericState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Transition;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the business logic related to {@link FiniteStateMachine}s.  This includes the creation
 * of a new state machine entity.
 */
@Dependent
public class FiniteStateMachineFactory {

    private static final Log log = LogFactory.getLog(ExtractTransform.class);

    @Inject
    private DragenConfig dragenConfig;

    @Inject
    private SampleSheetBuilder sampleSheetBuilder;

    @Inject
    private StateMachineDao stateMachineDao;

    public FiniteStateMachine createFiniteStateMachineForRun(IlluminaSequencingRun run,
                                                             MessageCollection messageCollection) {
        List<State> states = new ArrayList<>();
        List<Transition> transitions = new ArrayList<>();

        FiniteStateMachine finiteStateMachine = new FiniteStateMachine();

        File runDir = new File(run.getRunDirectory());

        // Create wait for RTA Complete Task
        State sequencingRunComplete = new GenericState("SequencingRunComplete", finiteStateMachine);
        sequencingRunComplete.setStartState(true);
        sequencingRunComplete.setAlive(true);
        File rtaCompleteFile = new File(runDir, "RTAComplete.txt");
        sequencingRunComplete.setTask(new WaitForFileTask(rtaCompleteFile));
        states.add(sequencingRunComplete);

        File demuxOutputRootDir = new File(dragenConfig.getDemultiplexOutputDirectory());
        File fastQDir = new File(demuxOutputRootDir, "fastq");

        // dragen will create the fastq dir but not the output dir
        if (!demuxOutputRootDir.exists()) {
            if (!demuxOutputRootDir.mkdir()) {
                messageCollection.addError("Failed to create demultiplexing output directory " + demuxOutputRootDir.getPath());
            }
        }

        short laneNum = 0;
        for (VesselPosition vesselPosition: run.getSampleCartridge().getVesselGeometry().getVesselPositions()) {
            ++laneNum;
            File SampleSheetFile = new File(demuxOutputRootDir, String.format("SampleSheet_hsa_{%d}.csv", laneNum));
            SampleSheetBuilder.SampleSheet sampleSheet = sampleSheetBuilder.makeSampleSheet(run, vesselPosition, laneNum);
            writeFile(SampleSheetFile, sampleSheet.toCsv(), messageCollection);

            DemultiplexTask demultiplexTask = new DemultiplexTask(runDir, fastQDir, SampleSheetFile);
            IlluminaSequencingRunChamber sequencingRunChamber = run.getSequencingRunChamber(laneNum);

            String demuxName = String.format("%s_%d_demux", run.getSampleCartridge().getLabel(), laneNum);
            State demultiplex = new DemultiplexState(demuxName, finiteStateMachine, sequencingRunChamber);
            demultiplex.setTask(demultiplexTask);
            states.add(demultiplex);

            Transition seqToDemux = new Transition("Sequencing Complete To Demultiplexing", finiteStateMachine);
            seqToDemux.setFromState(sequencingRunComplete);
            seqToDemux.setToState(demultiplex);
            transitions.add(seqToDemux);
        }

        finiteStateMachine.setStatus(Status.RUNNING);
        finiteStateMachine.setStates(states);
        finiteStateMachine.setTransitions(transitions);

        if (!messageCollection.hasErrors()) {
            stateMachineDao.persist(finiteStateMachine);
        }

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
}
