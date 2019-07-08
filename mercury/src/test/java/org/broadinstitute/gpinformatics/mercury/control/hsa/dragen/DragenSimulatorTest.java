package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.SampleSheetBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.FiniteStateMachineEngine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerControllerStub;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.GenericState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Transition;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SequencingTemplateFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.testng.Assert.fail;

@Test(groups = TestGroups.DATABASE_FREE)
public class DragenSimulatorTest extends BaseEventTest {

    private static final String BARCODE_SUFFIX = "1";
    private Dragen dragen;
    private String runBarcode;
    private IlluminaSequencingRun run;
    private String baseDirectory;
    private HybridSelectionEntityBuilder hybridSelectionEntityBuilder;
    private File runFolder;

    @BeforeMethod
    public void setUp() {
        dragen = new DragenSimulator();
        super.setUp();

        expectedRouting = SystemRouter.System.MERCURY;

        super.setUp();
        SequencingTemplateFactory factory = new SequencingTemplateFactory();
        factory.setWorkflowConfig(new WorkflowLoader().load());

        // Method calls on factory will always use our list of flowcell designations.
        List<FlowcellDesignation> flowcellDesignations = new ArrayList<>();
        factory.setFlowcellDesignationEjb(new FlowcellDesignationEjb(){
            @Override
            public List<FlowcellDesignation> getFlowcellDesignations(LabBatch fct) {
                return flowcellDesignations;
            }
        });
        flowcellDesignations.clear();

        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        Date runDate = new Date();
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());

        //Build Event History
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, BARCODE_SUFFIX);
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                String.valueOf(runDate.getTime()),
                BARCODE_SUFFIX, true);
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                        picoPlatingEntityBuilder.getNormTubeFormation(),
                        picoPlatingEntityBuilder.getNormalizationBarcode(), BARCODE_SUFFIX);
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                        exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                        exomeExpressShearingEntityBuilder.getShearingPlate(), BARCODE_SUFFIX);
        hybridSelectionEntityBuilder =
                runHybridSelectionProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                        libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                        libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), BARCODE_SUFFIX);
        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                "1");

        generateSequencingRun(qtpEntityBuilder, new Date());
    }

    /**
     * From a rack ready for QTP create a new sequencing run and sample sheet for demultiplexing
     * @param qtpEntityBuilder
     * @param runDate
     */
    private void generateSequencingRun(QtpEntityBuilder qtpEntityBuilder, Date runDate) {
        BarcodedTube denatureTube2500 =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        TubeFormation denatureTubeContainer =
                qtpEntityBuilder.getDenatureRack();

        LabBatch fctBatch = new LabBatch("FCT-233", LabBatch.LabBatchType.FCT,
                IlluminaFlowcell.FlowcellType.NovaSeqFlowcell, denatureTube2500, new BigDecimal("12.33"));

        String flowcellBarcode = "flowcell" + new Date().getTime() + "DMXX";
        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                new HiSeq2500FlowcellEntityBuilder(getBettaLimsMessageTestFactory(), getLabEventFactory(),
                        getLabEventHandler(),
                        denatureTubeContainer, flowcellBarcode, BARCODE_SUFFIX + "DMXX",
                        "FCT-233",
                        ProductionFlowcellPath.DILUTION_TO_FLOWCELL,
                        null, 2).invoke();

        SimpleDateFormat format = new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN);
        runBarcode = flowcellBarcode + format.format(runDate);
        baseDirectory = System.getProperty("java.io.tmpdir");
        String runFileDirectory =
                baseDirectory + File.separator + "SL-NVA" + File.separator
                + runBarcode;
        runFolder = new File(runFileDirectory);
        runFolder.mkdirs();
        SolexaRunBean testRunBean =
                new SolexaRunBean(flowcellBarcode, runBarcode, runDate, "SL-NVA", runFileDirectory, null);

        IlluminaSequencingRunFactory runFactory = new IlluminaSequencingRunFactory(EasyMock.createNiceMock(JiraCommentUtil.class));
        run = runFactory.build(testRunBean, hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell());
    }

    @Test
    public void testDemultiplexAlignCoverage() throws IOException {
        runDemultiplexinginAndAlignment();

        // Re run to meet 'coverage'
        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), "2");

        generateSequencingRun(qtpEntityBuilder, new Date());
        runDemultiplexinginAndAlignment();
    }

    private void runDemultiplexinginAndAlignment() throws IOException {
        File runDir = new File(run.getRunDirectory());
        File outputDir = new File(baseDirectory, run.getRunName());
        if (outputDir.exists()) {
            outputDir.delete();
        }

        FiniteStateMachine finiteStateMachine = createStateMachine(run, outputDir);

        DragenAppContext appContext = new DragenAppContext(dragen);
        SchedulerContext schedulerContext = new SchedulerContext(new SchedulerControllerStub(), appContext);
        FiniteStateMachineEngine engine = new FiniteStateMachineEngine(schedulerContext);
        engine.setTaskManager(new TaskManager());

        // File not created, state will still be in WaitForFile
        engine.executeProcessDaoFree(finiteStateMachine);
        Assert.assertEquals(1, finiteStateMachine.getActiveStates().size());
        for (State state: finiteStateMachine.getActiveStates()) {
            assertThat(state, instanceOf(GenericState.class));
            assertThat(state.getTask(), instanceOf(WaitForFileTask.class));
        }

        File rtaFile = new File(runDir, "RTAComplete.txt");
        rtaFile.createNewFile();

        // Second execution will now see the file and transition to demultiplexing
        engine.executeProcessDaoFree(finiteStateMachine);
        Assert.assertEquals(2, finiteStateMachine.getActiveStates().size());
        for (State state: finiteStateMachine.getActiveStates()) {
            assertThat(state, instanceOf(DemultiplexState.class));
        }

        // Third execution will 'check' the pid's of the running demultiplexing tasks which won't exist. It'll assume
        // they are complete and move on to alignment.
        engine.executeProcessDaoFree(finiteStateMachine);
        Assert.assertEquals(96, finiteStateMachine.getActiveStates().size());
        for (State state: finiteStateMachine.getActiveStates()) {
            assertThat(state, instanceOf(AlignmentState.class));
        }

        // Alignment ran so state machine should now be complete
        engine.executeProcessDaoFree(finiteStateMachine);
        Assert.assertEquals(true, finiteStateMachine.isComplete());
    }

    public static FiniteStateMachine createStateMachine(IlluminaSequencingRun run, File outputDir) {
        File runDir = new File(run.getRunDirectory());
        if (outputDir.exists()) {
            outputDir.delete();
        }

        List<State> states = new ArrayList<>();
        List<Transition> transitions = new ArrayList<>();

        File referenceFile = new File("hg19");
        File intermediateResults = new File(outputDir, "inters");

        // Consider a sequencing run complete when an RTAComplete.txt file is created in its root run folder
        File rtaComplete = new File(runDir, "RTAComplete.txt");

        FiniteStateMachine finiteStateMachine = new FiniteStateMachine();

        State sequencingRunComplete = new GenericState("SequencingRunComplete", finiteStateMachine);
        sequencingRunComplete.setStartState(true);
        sequencingRunComplete.setAlive(true);
        sequencingRunComplete.setTask(new WaitForFileTask(rtaComplete));
        states.add(sequencingRunComplete);

        List<State> demultiplexStates = new ArrayList<>();
        List<State> alignmentStates = new ArrayList<>();

        RunCartridge flowcell = run.getSampleCartridge();
        VesselPosition[] positionNames = flowcell.getVesselGeometry().getVesselPositions();
        short laneNum = 0;
        Map<String, MercurySample> mapSmToAlignment = new HashMap<>();
        for (VesselPosition vesselPosition: positionNames) {
            ++laneNum;

            SampleSheetBuilder builder = new SampleSheetBuilder();
            SampleSheetBuilder.SampleSheet sampleSheet = builder.makeSampleSheet(run, vesselPosition, laneNum);
            String sampleSheetCsv = sampleSheet.toCsv();

            File sampleSheetFile = new File(run.getRunDirectory(), String.format("SampleSheet_%d.csv", laneNum));
            try {
                FileUtils.writeStringToFile(sampleSheetFile, sampleSheetCsv);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println(sampleSheetFile.getPath());

            IlluminaSequencingRunChamber sequencingRunChamber = run.getSequencingRunChamber(laneNum);
            State demultiplex = new DemultiplexState("Demultiplex", finiteStateMachine, sequencingRunChamber);
            demultiplex.setTask(new DemultiplexTask(runDir, outputDir, sampleSheetFile));
            states.add(demultiplex);

            demultiplexStates.add(demultiplex);

            Transition seqToDemux = new Transition("Sequencing Complete To Demultiplexing", finiteStateMachine);
            seqToDemux.setFromState(sequencingRunComplete);
            seqToDemux.setToState(demultiplex);
            transitions.add(seqToDemux);

            File reportsDir = new File(outputDir, "Reports");
            File fastQList = new File(reportsDir, "fastq_list.csv");

            // For each sample run Alignment
            for (SampleInstanceV2 laneSampleInstance : flowcell.getContainerRole().getSampleInstancesAtPositionV2(vesselPosition)) {
                MercurySample mercurySample = laneSampleInstance.getRootOrEarliestMercurySample();
                String fastQSampleId = mercurySample.getSampleKey();

                // One sample across two lanes, ignore 2nd alignment.
                if (!mapSmToAlignment.containsKey(fastQSampleId)) {
                    State alignment = new AlignmentState("Alignment", finiteStateMachine, mercurySample);
                    alignment.setTask(new AlignmentTask(referenceFile, fastQList, fastQSampleId, outputDir,
                            intermediateResults, fastQSampleId, fastQSampleId));
                    states.add(alignment);

                    Transition demuxToAlignment = new Transition("Demultiplexing To Alignment: " + fastQSampleId, finiteStateMachine);
                    demuxToAlignment.setFromState(demultiplex);
                    demuxToAlignment.setToState(alignment);
                    transitions.add(demuxToAlignment);
                    alignmentStates.add(alignment);
                    mapSmToAlignment.put(mercurySample.getSampleKey(), mercurySample);
                }
            }
        }

        finiteStateMachine.setStatus(Status.RUNNING);
        finiteStateMachine.setStates(states);
        finiteStateMachine.setTransitions(transitions);

        return finiteStateMachine;
    }
}