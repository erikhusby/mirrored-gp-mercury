package org.broadinstitute.gpinformatics.mercury.boundary.hsa.dragen;

import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.hsa.SampleSheetBuilder;
import org.broadinstitute.gpinformatics.mercury.boundary.hsa.engine.FiniteStateMachineEngine;
import org.broadinstitute.gpinformatics.mercury.boundary.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.boundary.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.boundary.hsa.state.StateMachine;
import org.broadinstitute.gpinformatics.mercury.boundary.hsa.state.StateNode;
import org.broadinstitute.gpinformatics.mercury.boundary.hsa.state.Transition;
import org.broadinstitute.gpinformatics.mercury.boundary.hsa.state.TransitionBean;
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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.fail;

@Test(groups = TestGroups.DATABASE_FREE)
public class DragenSimulatorTest extends BaseEventTest {

    private static final String BARCODE_SUFFIX = "1";
    private Dragen dragen;
    private String runBarcode;
    private IlluminaSequencingRun run;
    private String baseDirectory;
    private File sampleSheetFile;
    private SampleSheetBuilder.SampleSheet sampleSheet;
    private HybridSelectionEntityBuilder hybridSelectionEntityBuilder;

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
        File runFolder = new File(runFileDirectory);
        runFolder.mkdirs();
        SolexaRunBean testRunBean =
                new SolexaRunBean(flowcellBarcode, runBarcode, runDate, "SL-NVA", runFileDirectory, null);

        IlluminaSequencingRunFactory runFactory = new IlluminaSequencingRunFactory(EasyMock.createNiceMock(JiraCommentUtil.class));
        run = runFactory.build(testRunBean, hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell());

        SampleSheetBuilder builder = new SampleSheetBuilder();
        sampleSheet = builder.makeSampleSheet(run);
        String sampleSheetCsv = sampleSheet.toCsv();

        sampleSheetFile = new File(runFolder, "SampleSheet.csv");
        try {
            FileUtils.writeStringToFile(sampleSheetFile, sampleSheetCsv);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(sampleSheetFile.getPath());
    }

    @Test
    public void testValidateSampleSheetFromFile() throws IOException {
        List<SampleSheetBuilder.SampleData> sampleData = SampleSheetBuilder.grabDataFromFile(sampleSheetFile);
        Assert.assertEquals(sampleData.size(), 192);

        SampleSheetBuilder.SampleData first = sampleData.iterator().next();
        Assert.assertEquals(first.getSampleName(), "SM-10111312A");
        Assert.assertEquals(first.getIndex(), "TTAAAAAA");
        Assert.assertEquals(first.getIndex2(), "TTAAAAAA");

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

    public void runDemultiplexinginAndAlignment() throws IOException {
        File runDir = new File(run.getRunDirectory());
        File outputDir = new File(baseDirectory, run.getRunName());
        if (outputDir.exists()) {
            outputDir.delete();
        }

        List<State> states = new ArrayList<>();
        List<Transition> transitions = new ArrayList<>();

        File referenceFile = new File("hg19");
        File intermediateResults = new File(outputDir, "inters");

        // Consider a sequencing run complete when an RTAComplete.txt file is created in its root run folder
        File rtaComplete = new File(runDir, "RTAComplete.txt");

        StateMachine finiteStateMachine = new FiniteStateMachine();

        State sequencingRunComplete = new StateNode("SequencingRunComplete");
        sequencingRunComplete.setStart(true);
        states.add(sequencingRunComplete);

        State demultiplex = new StateNode("Demultiplex");
        states.add(demultiplex);

        Transition seqToDemux = new TransitionBean("Sequencing Complete To Demultiplexing");
        seqToDemux.setTask(new WaitForFileTask(rtaComplete));
        seqToDemux.setFromState(sequencingRunComplete);
        seqToDemux.setToState(demultiplex);
        transitions.add(seqToDemux);

        demultiplex.setOnEnter(new DemultiplexTask(runDir, outputDir, sampleSheetFile));

        File reportsDir = new File(outputDir, "Reports");
        File fastQList = new File(reportsDir, "fastq_list.csv");

        // For each sample run Alignment
        List<SampleSheetBuilder.SampleData> sampleData = SampleSheetBuilder.grabDataFromFile(sampleSheetFile);
        for (SampleSheetBuilder.SampleData sample: sampleData) {
            String fastQSampleId = sample.getSampleName();

            State alignment = new StateNode("Alignment");
            states.add(alignment);
            alignment.setOnEnter(new AlignmentTask(referenceFile, fastQList, fastQSampleId, outputDir,
                    intermediateResults, fastQSampleId, fastQSampleId));

            Transition demuxToAlignment = new TransitionBean("Demultiplexing To Alignment: " + fastQSampleId);
            demuxToAlignment.setFromState(demultiplex);
            demuxToAlignment.setToState(alignment);
            transitions.add(demuxToAlignment);
        }

        finiteStateMachine.setStates(states);
        finiteStateMachine.setTransitions(transitions);

        DragenAppContext appContext = new DragenAppContext(dragen);
        FiniteStateMachineEngine engine = new FiniteStateMachineEngine(appContext);

        Thread executionThread = new Thread() {
            public void run() {
                engine.executeProcess(finiteStateMachine);
            }
        };
        executionThread.start();

        // Wait a few seconds to create the RTAComplete.txt file
        Thread createRTAComplete = new Thread() {
            public void run() {
                try {
                    Thread.sleep(3000L);
                    System.out.println("Creating RTA Complete File, should kick off demux");
                    File rtaFile = new File(runDir, "RTAComplete.txt");
                    rtaFile.createNewFile();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        createRTAComplete.start();

        try {
            executionThread.join(100000);
        }
        catch (InterruptedException ie) {
        }
        if (executionThread.isAlive()) {
            fail("Thread should've completed by now.");
        }

    }
}