package org.broadinstitute.gpinformatics.mercury.test;

//import com.jprofiler.api.agent.Controller;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.map.LazySortedMap;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.boundary.graph.Graph;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferEntityGrapher;
import org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferVisualizer;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowValidator;
import org.broadinstitute.gpinformatics.mercury.control.zims.ZimsIlluminaRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkDetail;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PreFlightEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.SageEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ShearingEntityBuilder;
import org.easymock.EasyMock;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * Test messaging
 */
@SuppressWarnings({"FeatureEnvy", "OverlyCoupledClass", "OverlyCoupledMethod", "OverlyLongMethod"})
public class LabEventTest extends BaseEventTest {
    /**
     * Physical type for a 2-lane flowcell
     */
    public static final String PHYS_TYPE_FLOWCELL_2_LANE = "Flowcell2Lane";

    /**
     * Section for both lanes of a 2-lane flowcell
     */
    public static final String SECTION_ALL_2 = "ALL2";

    public static final String POND_REGISTRATION_TUBE_PREFIX = "PondReg";
    public static final String FCT_TICKET = "FCT-1";

    private final TemplateEngine templateEngine = new TemplateEngine();

    /**
     * Controls are referenced in the routing logic
     */
    private static final List<Control> controlList = new ArrayList<>();
    private static final List<String> controlCollaboratorIdList = new ArrayList<>();

    static {
        controlList.add(new Control("NA12878", Control.ControlType.POSITIVE));
        controlList.add(new Control("WATER_CONTROL", Control.ControlType.NEGATIVE));

        for (Control control : controlList) {
            controlCollaboratorIdList.add(control.getCollaboratorSampleId());
        }
    }

    /**
     * Used in test verification, accumulates the events in a chain of transfers
     */
    public static class ListTransfersFromStart implements TransferTraverserCriteria {
        private int hopCount = -1;
        private final List<String> labEventNames = new ArrayList<>();

        private final SortedMap<Integer, SortedSet<LabEvent>> labEventNamesByHopCount =
                LazySortedMap.decorate(new TreeMap<Integer, SortedSet<LabEvent>>(),
                        new Factory<SortedSet<LabEvent>>() {
                            @Override
                            public SortedSet<LabEvent> create() {
                                return new TreeSet<>(LabEvent.BY_EVENT_DATE);
                            }
                        });

        /**
         * Avoid infinite loops
         */
        private Set<LabEvent> visitedLabEvents = new HashSet<>();

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            if (context.getEvent() != null) {
                labEventNamesByHopCount.get(context.getHopCount()).add(context.getEvent());

                if (!getVisitedLabEvents().add(context.getEvent())) {
                    return TraversalControl.StopTraversing;
                }
                if (context.getHopCount() > hopCount) {
                    hopCount = context.getHopCount();
                    labEventNames.add(makeLabEventName(context.getEvent()));
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselInOrder(Context context) {
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public List<String> getLabEventNames() {
            return labEventNames;
        }

        public Set<LabEvent> getVisitedLabEvents() {
            return visitedLabEvents;
        }

        public List<String> getLastEventNamesPerHop() {
            List<String> result = new ArrayList<>();
            for (SortedSet<LabEvent> labEvents : labEventNamesByHopCount.values()) {
                result.add(makeLabEventName(labEvents.last()));
            }
            return result;
        }

        public List<String> getAllEventNamesPerHop() {
            List<String> result = new ArrayList<>();
            for (SortedSet<LabEvent> labEvents : labEventNamesByHopCount.values()) {
                for (LabEvent event : labEvents) {
                    result.add(makeLabEventName(event));
                }
            }
            return result;
        }

        private String makeLabEventName(LabEvent event) {
            return event.getLabEventType().getName() + " into " +
                   event.getTargetLabVessels().iterator().next().getLabel();
        }
    }

    @Override
    @BeforeClass(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        templateEngine.postConstruct();
        super.setUp();
    }

    /**
     * Build object graph for Hybrid Selection messages, verify chain of events.
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testHybridSelection() {
//        Controller.startCPURecording(true);

        expectedRouting = MercuryOrSquidRouter.MercuryOrSquid.SQUID;
        // todo jmt receipt batch?
        ProductOrder productOrder = ProductOrderTestFactory.buildHybridSelectionProductOrder(NUM_POSITIONS_IN_RACK - 2,
                "A");
        AthenaClientServiceStub.addProductOrder(productOrder);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("Hybrid Selection Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflowName("Hybrid Selection");
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        TubeFormation daughterTubeFormation = daughterPlateTransfer(mapBarcodeToTube);

        Map<String, TwoDBarcodedTube> mapBarcodeToDaughterTube = new HashMap<>();
        for (TwoDBarcodedTube twoDBarcodedTube : daughterTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(twoDBarcodedTube.getLabel(), twoDBarcodedTube);
        }

        PreFlightEntityBuilder preFlightEntityBuilder = runPreflightProcess(mapBarcodeToDaughterTube,
                "1");
        ShearingEntityBuilder shearingEntityBuilder = runShearingProcess(mapBarcodeToDaughterTube,
                preFlightEntityBuilder.getTubeFormation(), preFlightEntityBuilder.getRackBarcode(), "1");
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = runLibraryConstructionProcess(
                shearingEntityBuilder.getShearingCleanupPlate(), shearingEntityBuilder.getShearCleanPlateBarcode(),
                shearingEntityBuilder.getShearingPlate(), "1");
        HybridSelectionEntityBuilder hybridSelectionEntityBuilder = runHybridSelectionProcess(
                libraryConstructionEntityBuilder.getPondRegRack(),
                libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), "1");
        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), "Hybrid Selection", "1");

        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1", null,
                        ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL, "Squid Designation", "Hybrid Selection");

        IlluminaFlowcell illuminaFlowcell = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell();
        Set<SampleInstance> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                VesselPosition.LANE1);
        Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(), 2,
                "Wrong number of reagents");

        IlluminaSequencingRunFactory illuminaSequencingRunFactory =
                new IlluminaSequencingRunFactory(EasyMock.createNiceMock(JiraCommentUtil.class));
        IlluminaSequencingRun illuminaSequencingRun;
        try {
            illuminaSequencingRun = illuminaSequencingRunFactory.buildDbFree(new SolexaRunBean(
                    illuminaFlowcell.getCartridgeBarcode(), "Run1", new Date(), "SL-HAL",
                    File.createTempFile("RunDir", ".txt").getAbsolutePath(), null),
                    illuminaFlowcell);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ReadStructureRequest readStructureRequest = new ReadStructureRequest();
        readStructureRequest.setRunBarcode(illuminaSequencingRun.getRunBarcode());
        readStructureRequest.setSetupReadStructure("71T8B8B71T");
        readStructureRequest.setActualReadStructure("101T8B8B101T");
        readStructureRequest.setLanesSequenced("1,4");

        illuminaSequencingRunFactory.storeReadsStructureDBFree(readStructureRequest, illuminaSequencingRun);

        ZimsIlluminaRunFactory zimsIlluminaRunFactory = constructZimsIlluminaRunFactory(productOrder);
        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(illuminaSequencingRun);
        Assert.assertEquals(zimsIlluminaRun.getLanes().size(), 8, "Wrong number of lanes");
        Assert.assertEquals(zimsIlluminaRun.getActualReadStructure(), readStructureRequest.getActualReadStructure());
        Assert.assertEquals(zimsIlluminaRun.getSetupReadStructure(), readStructureRequest.getSetupReadStructure());
        Assert.assertEquals(zimsIlluminaRun.getSystemOfRecord(), MercuryOrSquidRouter.MercuryOrSquid.MERCURY);
        ZimsIlluminaChamber zimsIlluminaChamber = zimsIlluminaRun.getLanes().iterator().next();
        Assert.assertEquals(zimsIlluminaChamber.getLibraries().size(), NUM_POSITIONS_IN_RACK,
                "Wrong number of libraries");
        LibraryBean libraryBean = zimsIlluminaChamber.getLibraries().iterator().next();
        // todo jmt need to investigate the ordering of libraries in ZIMS API results, and do more asserts here
        Assert.assertNotNull(libraryBean.getMolecularIndexingScheme().getName(), "No molecular index");
        Assert.assertEquals(libraryBean.getBaitSetName(), HybridSelectionEntityBuilder.BAIT_DESIGN_NAME, "Wrong bait");
        // want to check that null is represented properly
        Assert.assertNull(zimsIlluminaRun.getImagedAreaPerLaneMM2());
        Assert.assertEquals(zimsIlluminaRun.getLanesSequenced(), "1,4");
        LabVessel denatureTube = illuminaFlowcell.getNearestTubeAncestorsForLanes().values().iterator().next();
        Assert.assertEquals(zimsIlluminaChamber.getSequencedLibrary(), denatureTube.getLabel());
        for (LibraryBean bean : zimsIlluminaChamber.getLibraries()) {
            // Every library should have an LCSET, even controls.
            Assert.assertEquals(bean.getLcSet(), workflowBatch.getBatchName());
//            if (!((libraryBean.isPositiveControl() != null && libraryBean.isPositiveControl()) ||
//                  (libraryBean.isNegativeControl() != null && libraryBean.isNegativeControl()))) {
//                Assert.assertEquals(bean.getProductOrderKey(), productOrder.getBusinessKey());
//            }
        }

        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        TwoDBarcodedTube startingTube = mapBarcodeToTube.entrySet().iterator().next().getValue();
        startingTube.evaluateCriteria(transferTraverserCriteria,
                TransferTraverserCriteria.TraversalDirection.Descendants);
        List<String> labEventNames = transferTraverserCriteria.getAllEventNamesPerHop();
        String[] expectedEventNames = {
                "SamplesDaughterPlateCreation",
                "PreflightPicoSetup",
                "PreflightPicoSetup",
                "PreflightPostNormPicoSetup",
                "PreflightPostNormPicoSetup",
                "ShearingTransfer",
                "PostShearingTransferCleanup",
                "ShearingQC",
                "AdapterLigationCleanup",
                "HybSelPondEnrichmentCleanup",
                "PondRegistration",
                "PreSelectionPool",
                "Hybridization",
                "CatchEnrichmentCleanup",
                "NormalizedCatchRegistration",
                "PoolingTransfer",
                "EcoTransfer",
                "NormalizationTransfer",
                "DenatureTransfer",
                "StripTubeBTransfer",
                "FlowcellTransfer",
        };
        verifyEventSequence(labEventNames, expectedEventNames);

        Assert.assertEquals(illuminaSequencingRun.getSampleCartridge(), illuminaFlowcell, "Wrong flowcell");

        runTransferVisualizer(startingTube);

        TransferEntityGrapher transferEntityGrapher = new TransferEntityGrapher();
        transferEntityGrapher.setMaxNumVesselsPerRequest(1000);
        Graph graph = new Graph();
        transferEntityGrapher.startWithTube(startingTube, graph, new ArrayList<TransferVisualizer.AlternativeId>());
        Assert.assertEquals(graph.getMapIdToVertex().size(), 3276, "Wrong number of vertices");
//        Controller.stopCPURecording();
    }


    /**
     * Build object graph for Exome Express messages, verify chain of events.
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testExomeExpress() {
//        Controller.startCPURecording(true);
        expectedRouting = MercuryOrSquidRouter.MercuryOrSquid.MERCURY;

        // Use Standard Exome product, to verify that workflow is taken from LCSet, not Product
        ProductOrder productOrder = ProductOrderTestFactory.buildHybridSelectionProductOrder(NUM_POSITIONS_IN_RACK - 2,
                "A");
        AthenaClientServiceStub.addProductOrder(productOrder);
        Date runDate = new Date();
        // todo jmt create bucket, then batch, rather than rack then batch then bucket
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
        workflowBatch.setWorkflowName("Exome Express");

        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        TubeFormation daughterTubeFormation = daughterPlateTransfer(mapBarcodeToTube);

        Map<String, TwoDBarcodedTube> mapBarcodeToDaughterTube = new HashMap<>();
        for (TwoDBarcodedTube twoDBarcodedTube : daughterTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(twoDBarcodedTube.getLabel(), twoDBarcodedTube);
        }

        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToDaughterTube,
                String.valueOf(runDate.getTime()), "1", true);
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                        picoPlatingEntityBuilder.getNormTubeFormation(),
                        picoPlatingEntityBuilder.getNormalizationBarcode(), "1");
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                        exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                        exomeExpressShearingEntityBuilder.getShearingPlate(), "1");
        HybridSelectionEntityBuilder hybridSelectionEntityBuilder =
                runHybridSelectionProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                        libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                        libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), "1");
        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), "Exome Express", "1");

        final LabVessel denatureSource =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        LabBatch fctBatch =
                new LabBatch(FCT_TICKET,
                        Collections.singleton(denatureSource),
                        LabBatch.LabBatchType.FCT);

        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", FCT_TICKET,
                        ProductionFlowcellPath.DILUTION_TO_FLOWCELL, null, "Exome Express");

        runTransferVisualizer(mapBarcodeToTube.values().iterator().next());

        IlluminaFlowcell illuminaFlowcell = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell();
        Set<SampleInstance> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                VesselPosition.LANE1);
        Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(), 2,
                "Wrong number of reagents");
        Set<SampleInstance> lane2SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                VesselPosition.LANE2);
        Assert.assertEquals(lane2SampleInstances.iterator().next().getReagents().size(), 2,
                "Wrong number of reagents");

        String machineName = "Superman";

        SimpleDateFormat dateFormat = new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN);

        File runPath = null;
        try {
            runPath = File.createTempFile("tempRun" + dateFormat.format(runDate), ".txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String flowcellBarcode = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell().getCartridgeBarcode();

        SolexaRunBean runBean = new SolexaRunBean(flowcellBarcode,
                flowcellBarcode + dateFormat.format(runDate),
                runDate, machineName,
                runPath.getAbsolutePath(), null);

        IlluminaSequencingRunFactory runFactory =
                new IlluminaSequencingRunFactory(EasyMock.createMock(JiraCommentUtil.class));
        IlluminaSequencingRun run =
                runFactory.buildDbFree(runBean, hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell());

        ReadStructureRequest readStructureRequest = new ReadStructureRequest();
        readStructureRequest.setRunBarcode(run.getRunBarcode());
        readStructureRequest.setSetupReadStructure("71T8B8B71T");
        readStructureRequest.setActualReadStructure("101T8B8B101T");
        readStructureRequest.setImagedArea(new Double("185.2049407959"));
        readStructureRequest.setLanesSequenced("3,6");

        runFactory.storeReadsStructureDBFree(readStructureRequest, run);

        ZimsIlluminaRunFactory zimsIlluminaRunFactory = constructZimsIlluminaRunFactory(productOrder);
        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(run);
        Assert.assertEquals(zimsIlluminaRun.getLanes().size(), 2, "Wrong number of lanes");
        Assert.assertEquals(zimsIlluminaRun.getActualReadStructure(), readStructureRequest.getActualReadStructure());
        Assert.assertEquals(zimsIlluminaRun.getSetupReadStructure(), readStructureRequest.getSetupReadStructure());
        Assert.assertEquals(zimsIlluminaRun.getImagedAreaPerLaneMM2(), readStructureRequest.getImagedArea());
        Assert.assertEquals(zimsIlluminaRun.getLanesSequenced(), "3,6");
        Assert.assertEquals(zimsIlluminaRun.getSystemOfRecord(), MercuryOrSquidRouter.MercuryOrSquid.MERCURY);

        Map.Entry<String, TwoDBarcodedTube> stringTwoDBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        stringTwoDBarcodedTubeEntry.getValue().evaluateCriteria(transferTraverserCriteria,
                TransferTraverserCriteria.TraversalDirection.Descendants);
        List<String> labEventNames = transferTraverserCriteria.getAllEventNamesPerHop();

        /*
         * TODO: Get expected events from workflow. Currently complicated by:
         *      some events are required while some are not
         *      testExomeExpress does not record all required BSP events
         *      ListTransfersFromStart does not return (is not even told about) in-place events
         */
        String[] expectedEventNames = {
                "SamplesDaughterPlateCreation",
                "SamplesNormalizationTransfer",
                "PicoPlatingPostNorm",
                "ShearingTransfer",
                "PostShearingTransferCleanup",
                "ShearingQC",
                "AdapterLigationCleanup",
                "HybSelPondEnrichmentCleanup",
                "PondRegistration",
                "PreSelectionPool",
                "Hybridization",
                "CatchEnrichmentCleanup",
                "NormalizedCatchRegistration",
                "PoolingTransfer",
                "EcoTransfer",
                "NormalizationTransfer",
                "DenatureTransfer",
                "DenatureToDilutionTransfer",
                "DilutionToFlowcellTransfer",
        };
        verifyEventSequence(labEventNames, expectedEventNames);

        IlluminaSequencingRun illuminaSequencingRun
                = (IlluminaSequencingRun) hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell().getSequencingRuns()
                .iterator().next();

        Assert.assertEquals(illuminaSequencingRun.getSampleCartridge(),
                hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell(), "Wrong flowcell");

        Assert.assertEquals(illuminaSequencingRun.getSampleCartridge().getSequencerModel(), "Illumina HiSeq 2500");

//        Controller.stopCPURecording();
    }

    /**
     * Build object graph for Exome Express with different optional messages, verify chain of events.
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testExomeExpressAlternative() throws Exception {
        expectedRouting = MercuryOrSquidRouter.MercuryOrSquid.MERCURY;

        ProductOrder productOrder = ProductOrderTestFactory.buildHybridSelectionProductOrder(NUM_POSITIONS_IN_RACK,
                "A");
        AthenaClientServiceStub.addProductOrder(productOrder);
        Date runDate = new Date();
        // todo jmt create bucket, then batch, rather than rack then batch then bucket
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
        workflowBatch.setWorkflowName("Exome Express");

        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                String.valueOf(runDate.getTime()), "1", true);
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                        picoPlatingEntityBuilder.getNormTubeFormation(),
                        picoPlatingEntityBuilder.getNormalizationBarcode(), "1");
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                        exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                        exomeExpressShearingEntityBuilder.getShearingPlate(), "1");
        HybridSelectionEntityBuilder hybridSelectionEntityBuilder =
                runHybridSelectionProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                        libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                        libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), "1");
        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), "Exome Express", "1");
        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", "squidDesignationName",
                        ProductionFlowcellPath.DENATURE_TO_FLOWCELL, null, "Exome Express");

        IlluminaFlowcell illuminaFlowcell = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell();
        Set<SampleInstance> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                VesselPosition.LANE1);
        Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(), 2,
                "Wrong number of reagents");
        Set<SampleInstance> lane2SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                VesselPosition.LANE2);
        Assert.assertEquals(lane2SampleInstances.iterator().next().getReagents().size(), 2,
                "Wrong number of reagents");

        SimpleDateFormat dateFormat = new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN);

        File runPath;
        try {
            runPath = File.createTempFile("tempRun" + dateFormat.format(runDate), ".txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String flowcellBarcode = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell().getCartridgeBarcode();

        String machineName = "Superman";
        SolexaRunBean runBean = new SolexaRunBean(flowcellBarcode,
                flowcellBarcode + dateFormat.format(runDate),
                runDate, machineName,
                runPath.getAbsolutePath(), null);

        IlluminaSequencingRunFactory runFactory =
                new IlluminaSequencingRunFactory(EasyMock.createMock(JiraCommentUtil.class));
        IlluminaSequencingRun run =
                runFactory.buildDbFree(runBean, hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell());

        ReadStructureRequest readStructureRequest = new ReadStructureRequest();
        readStructureRequest.setRunBarcode(run.getRunBarcode());
        readStructureRequest.setSetupReadStructure("71T8B8B71T");
        readStructureRequest.setActualReadStructure("101T8B8B101T");
        readStructureRequest.setImagedArea(new Double("185.2049407959"));

        runFactory.storeReadsStructureDBFree(readStructureRequest, run);

        ZimsIlluminaRunFactory zimsIlluminaRunFactory = constructZimsIlluminaRunFactory(productOrder);
        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(run);
        Assert.assertEquals(zimsIlluminaRun.getLanes().size(), 2, "Wrong number of lanes");
        Assert.assertEquals(zimsIlluminaRun.getActualReadStructure(), readStructureRequest.getActualReadStructure());
        Assert.assertEquals(zimsIlluminaRun.getSetupReadStructure(), readStructureRequest.getSetupReadStructure());
        Assert.assertEquals(zimsIlluminaRun.getImagedAreaPerLaneMM2(), readStructureRequest.getImagedArea());
        Assert.assertNull(zimsIlluminaRun.getLanesSequenced());
        Assert.assertEquals(zimsIlluminaRun.getSystemOfRecord(), MercuryOrSquidRouter.MercuryOrSquid.MERCURY);

        Map.Entry<String, TwoDBarcodedTube> stringTwoDBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        stringTwoDBarcodedTubeEntry.getValue().evaluateCriteria(transferTraverserCriteria,
                TransferTraverserCriteria.TraversalDirection.Descendants);
        List<String> labEventNames = transferTraverserCriteria.getAllEventNamesPerHop();

        /*
         * TODO: Get expected events from workflow. Currently complicated by:
         *      some events are required while some are not
         *      testExomeExpress does not record all required BSP events
         *      ListTransfersFromStart does not return (is not even told about) in-place events
         */
        String[] expectedEventNames = {
                "SamplesNormalizationTransfer",
                "PicoPlatingPostNorm",
                "ShearingTransfer",
                "PostShearingTransferCleanup",
                "ShearingQC",
                "AdapterLigationCleanup",
                "HybSelPondEnrichmentCleanup",
                "PondRegistration",
                "PreSelectionPool",
                "Hybridization",
                "CatchEnrichmentCleanup",
                "NormalizedCatchRegistration",
                "PoolingTransfer",
                "EcoTransfer",
                "NormalizationTransfer",
                "DenatureTransfer",
                "DenatureToFlowcellTransfer",
        };
        verifyEventSequence(labEventNames, expectedEventNames);

        IlluminaSequencingRun illuminaSequencingRun
                = (IlluminaSequencingRun) hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell().getSequencingRuns()
                .iterator().next();

        Assert.assertEquals(illuminaSequencingRun.getSampleCartridge(),
                hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell(), "Wrong flowcell");

//        Controller.stopCPURecording();
    }

    /**
     * Message part of the workflow for a batch, then rework a sample in a new batch.  Message both batches to
     * completion, and check that the pipeline API reports the correct LCSETs.
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testExomeExpressRework() {
        try {
            expectedRouting = MercuryOrSquidRouter.MercuryOrSquid.MERCURY;

            // Use Standard Exome product, to verify that workflow is taken from LCSet, not Product
            ProductOrder productOrder1 = ProductOrderTestFactory.buildHybridSelectionProductOrder(NUM_POSITIONS_IN_RACK,
                    "A");
            AthenaClientServiceStub.addProductOrder(productOrder1);
            Date runDate = new Date();

            Map<String, TwoDBarcodedTube> mapBarcodeToTube1 = createInitialRack(productOrder1, "R1_");
            LabBatch workflowBatch1 = new LabBatch("Exome Express Batch 1",
                    new HashSet<LabVessel>(mapBarcodeToTube1.values()), LabBatch.LabBatchType.WORKFLOW);
            workflowBatch1.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
            workflowBatch1.setWorkflowName("Exome Express");

            bucketBatchAndDrain(mapBarcodeToTube1, productOrder1, workflowBatch1, "1");
            PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube1,
                    String.valueOf(runDate.getTime()), "1", true);
            ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                    runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                            picoPlatingEntityBuilder.getNormTubeFormation(),
                            picoPlatingEntityBuilder.getNormalizationBarcode(), "1");

            ProductOrder productOrder2 = ProductOrderTestFactory.buildHybridSelectionProductOrder(
                    NUM_POSITIONS_IN_RACK - 1, "B");
            AthenaClientServiceStub.addProductOrder(productOrder2);

            Map<String, TwoDBarcodedTube> mapBarcodeToTube2 = createInitialRack(productOrder2, "R2_");
            TwoDBarcodedTube reworkTube = mapBarcodeToTube1.values().iterator().next();
            mapBarcodeToTube2.put(reworkTube.getLabel(), reworkTube);
            LabBatch workflowBatch2 = new LabBatch("Exome Express Batch 2",
                    new HashSet<LabVessel>(mapBarcodeToTube2.values()), LabBatch.LabBatchType.WORKFLOW);
            workflowBatch2.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
            workflowBatch2.setWorkflowName("Exome Express");

            bucketBatchAndDrain(mapBarcodeToTube2, productOrder2, workflowBatch2, "2");
            PicoPlatingEntityBuilder picoPlatingEntityBuilder2 = runPicoPlatingProcess(mapBarcodeToTube2,
                    String.valueOf(runDate.getTime()), "2", true);
            ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder2 =
                    runExomeExpressShearingProcess(picoPlatingEntityBuilder2.getNormBarcodeToTubeMap(),
                            picoPlatingEntityBuilder2.getNormTubeFormation(),
                            picoPlatingEntityBuilder2.getNormalizationBarcode(), "2");
            LibraryConstructionEntityBuilder libraryConstructionEntityBuilder2 =
                    runLibraryConstructionProcess(exomeExpressShearingEntityBuilder2.getShearingCleanupPlate(),
                            exomeExpressShearingEntityBuilder2.getShearCleanPlateBarcode(),
                            exomeExpressShearingEntityBuilder2.getShearingPlate(), "2");
            HybridSelectionEntityBuilder hybridSelectionEntityBuilder2 =
                    runHybridSelectionProcess(libraryConstructionEntityBuilder2.getPondRegRack(),
                            libraryConstructionEntityBuilder2.getPondRegRackBarcode(),
                            libraryConstructionEntityBuilder2.getPondRegTubeBarcodes(), "2");
            QtpEntityBuilder qtpEntityBuilder2 = runQtpProcess(hybridSelectionEntityBuilder2.getNormCatchRack(),
                    hybridSelectionEntityBuilder2.getNormCatchBarcodes(),
                    hybridSelectionEntityBuilder2.getMapBarcodeToNormCatchTubes(), "Exome Express", "2");
            HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder2 = runHiSeq2500FlowcellProcess(
                    qtpEntityBuilder2.getDenatureRack(), "2" + "ADXX", "squidDesignationName",
                    ProductionFlowcellPath.DENATURE_TO_FLOWCELL, null, "Exome Express");

            LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                    runLibraryConstructionProcess(exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                            exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                            exomeExpressShearingEntityBuilder.getShearingPlate(), "1");
            HybridSelectionEntityBuilder hybridSelectionEntityBuilder =
                    runHybridSelectionProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                            libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                            libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), "1");
            QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                    hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                    hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), "Exome Express", "1");
            HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder = runHiSeq2500FlowcellProcess(
                    qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", "squidDesignationName",
                    ProductionFlowcellPath.DENATURE_TO_FLOWCELL, null, "Exome Express");

            SimpleDateFormat dateFormat = new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN);
            File runPath = File.createTempFile("tempRun" + dateFormat.format(runDate), ".txt");
            String flowcellBarcode = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell().getCartridgeBarcode();
            String machineName = "Superman";
            IlluminaSequencingRunFactory runFactory =
                    new IlluminaSequencingRunFactory(EasyMock.createMock(JiraCommentUtil.class));
            ZimsIlluminaRunFactory zimsIlluminaRunFactory = constructZimsIlluminaRunFactory(productOrder1);

            SolexaRunBean runBean1 = new SolexaRunBean(flowcellBarcode, flowcellBarcode + dateFormat.format(runDate),
                    runDate, machineName, runPath.getAbsolutePath(), null);

            IlluminaSequencingRun run1 =
                    runFactory.buildDbFree(runBean1, hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell());

            ZimsIlluminaRun zimsIlluminaRun1 = zimsIlluminaRunFactory.makeZimsIlluminaRun(run1);
            Assert.assertEquals(zimsIlluminaRun1.getLanes().size(), 2, "Wrong number of lanes");

            ZimsIlluminaChamber zimsIlluminaChamber1 = zimsIlluminaRun1.getLanes().iterator().next();
            for (LibraryBean libraryBean : zimsIlluminaChamber1.getLibraries()) {
                Assert.assertEquals(libraryBean.getLcSet(), workflowBatch1.getBatchName());
                Assert.assertEquals(libraryBean.getProductOrderKey(), productOrder1.getBusinessKey());
            }

            SolexaRunBean runBean2 = new SolexaRunBean(flowcellBarcode, flowcellBarcode + dateFormat.format(runDate),
                    runDate, machineName, runPath.getAbsolutePath(), null);

            IlluminaSequencingRun run2 =
                    runFactory.buildDbFree(runBean2, hiSeq2500FlowcellEntityBuilder2.getIlluminaFlowcell());

            ZimsIlluminaRun zimsIlluminaRun2 = zimsIlluminaRunFactory.makeZimsIlluminaRun(run2);
            Assert.assertEquals(zimsIlluminaRun2.getLanes().size(), 2, "Wrong number of lanes");
            Assert.assertEquals(zimsIlluminaRun2.getSystemOfRecord(), MercuryOrSquidRouter.MercuryOrSquid.MERCURY);

            ZimsIlluminaChamber zimsIlluminaChamber2 = zimsIlluminaRun2.getLanes().iterator().next();
            for (LibraryBean libraryBean : zimsIlluminaChamber2.getLibraries()) {
                Assert.assertEquals(libraryBean.getLcSet(), workflowBatch2.getBatchName());
                Assert.assertEquals(libraryBean.getProductOrderKey(), productOrder2.getBusinessKey());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Message LCSET 1 through Pico and Shearing to ShearingQC; message LCSET 2 through Pico;
     * add one sample from LCSET 1 to shearing bucket for LCSET 2; message both sets to completion;
     * verify that pipeline API correctly reflects LCSETS.
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testExomeExpressShearingRework() {
        try {
            expectedRouting = MercuryOrSquidRouter.MercuryOrSquid.MERCURY;

            // Use Standard Exome product, to verify that workflow is taken from LCSet, not Product
            ProductOrder productOrder1 = ProductOrderTestFactory.buildHybridSelectionProductOrder(NUM_POSITIONS_IN_RACK,
                    "A");
            AthenaClientServiceStub.addProductOrder(productOrder1);
            Date runDate = new Date();

            Map<String, TwoDBarcodedTube> mapBarcodeToTube1 = createInitialRack(productOrder1, "R1_");
            LabBatch workflowBatch1 = new LabBatch("Exome Express Pico Batch 1",
                    new HashSet<LabVessel>(mapBarcodeToTube1.values()), LabBatch.LabBatchType.WORKFLOW);
            workflowBatch1.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
            workflowBatch1.setWorkflowName("Exome Express");

            bucketBatchAndDrain(mapBarcodeToTube1, productOrder1, workflowBatch1, "1");
            PicoPlatingEntityBuilder picoPlatingEntityBuilder1 = runPicoPlatingProcess(mapBarcodeToTube1,
                    String.valueOf(runDate.getTime()), "1", true);
            ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                    runExomeExpressShearingProcess(picoPlatingEntityBuilder1.getNormBarcodeToTubeMap(),
                            picoPlatingEntityBuilder1.getNormTubeFormation(),
                            picoPlatingEntityBuilder1.getNormalizationBarcode(), "1");

            LabBatch importBatch1 = new LabBatch("EX-123", new HashSet<LabVessel>(mapBarcodeToTube1.values()),
                    LabBatch.LabBatchType.SAMPLES_IMPORT);

            ProductOrder productOrder2 = ProductOrderTestFactory.buildHybridSelectionProductOrder(95, "B");
            AthenaClientServiceStub.addProductOrder(productOrder2);

            Map<String, TwoDBarcodedTube> mapBarcodeToTube2 = createInitialRack(productOrder2, "R2_");
            LabBatch workflowBatch2 = new LabBatch("Exome Express Pico Batch 2",
                    new HashSet<LabVessel>(mapBarcodeToTube2.values()), LabBatch.LabBatchType.WORKFLOW);
            workflowBatch2.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
            workflowBatch2.setWorkflowName("Exome Express");

            bucketBatchAndDrain(mapBarcodeToTube2, productOrder2, workflowBatch2, "2");
            PicoPlatingEntityBuilder picoPlatingEntityBuilder2 = runPicoPlatingProcess(mapBarcodeToTube2,
                    String.valueOf(runDate.getTime()), "2", true);

            LabBatch importBatch2 = new LabBatch("EX-123", new HashSet<LabVessel>(mapBarcodeToTube2.values()),
                    LabBatch.LabBatchType.SAMPLES_IMPORT);

            // Add sample from LCSET 1 TO LCSET 2 at shearing bucket
            final TwoDBarcodedTube reworkTube =
                    picoPlatingEntityBuilder1.getNormBarcodeToTubeMap().values().iterator().next();
//            workflowBatch2.addLabVessel(reworkTube);
            Map<String, TwoDBarcodedTube> mapBarcodeToTubesPlusRework =
                    new LinkedHashMap<>(picoPlatingEntityBuilder2.getNormBarcodeToTubeMap());
            mapBarcodeToTubesPlusRework.put(reworkTube.getLabel(), reworkTube);

            Bucket shearingBucket = createAndPopulateBucket(
                    new HashMap<String, TwoDBarcodedTube>() {{
                        put(reworkTube.getLabel(), reworkTube);
                    }}, productOrder2, "Shearing");
            workflowBatch2.addLabVessel(reworkTube);
            drainBucket(shearingBucket);
            reworkTube.getBucketEntries().iterator().next().setReworkDetail(new ReworkDetail(
                    ReworkEntry.ReworkReason.MACHINE_ERROR, ReworkEntry.ReworkLevel.ONE_SAMPLE_RELEASE_REST_BATCH,
                    LabEventType.SHEARING_TRANSFER, "test", null));

            ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder2 =
                    runExomeExpressShearingProcess(mapBarcodeToTubesPlusRework, null,
                            picoPlatingEntityBuilder2.getNormalizationBarcode(), "2");

            LibraryConstructionEntityBuilder libraryConstructionEntityBuilder2 =
                    runLibraryConstructionProcess(exomeExpressShearingEntityBuilder2.getShearingCleanupPlate(),
                            exomeExpressShearingEntityBuilder2.getShearCleanPlateBarcode(),
                            exomeExpressShearingEntityBuilder2.getShearingPlate(), "2");
            HybridSelectionEntityBuilder hybridSelectionEntityBuilder2 =
                    runHybridSelectionProcess(libraryConstructionEntityBuilder2.getPondRegRack(),
                            libraryConstructionEntityBuilder2.getPondRegRackBarcode(),
                            libraryConstructionEntityBuilder2.getPondRegTubeBarcodes(), "2");
            QtpEntityBuilder qtpEntityBuilder2 = runQtpProcess(hybridSelectionEntityBuilder2.getNormCatchRack(),
                    hybridSelectionEntityBuilder2.getNormCatchBarcodes(),
                    hybridSelectionEntityBuilder2.getMapBarcodeToNormCatchTubes(), "Exome Express", "2");
            HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder2 =
                    runHiSeq2500FlowcellProcess(qtpEntityBuilder2.getDenatureRack(), "2" + "ADXX", null,
                            ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL, "squidDesignationName", "Exome Express");

            LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                    runLibraryConstructionProcess(exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                            exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                            exomeExpressShearingEntityBuilder.getShearingPlate(), "1");
            HybridSelectionEntityBuilder hybridSelectionEntityBuilder =
                    runHybridSelectionProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                            libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                            libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), "1");
            QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                    hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                    hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), "Exome Express", "1");
            HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                    runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", null,
                            ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL, "squidDesignationName", "Exome Express");

            SimpleDateFormat dateFormat = new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN);
            File runPath = File.createTempFile("tempRun" + dateFormat.format(runDate), ".txt");
            String flowcellBarcode = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell().getCartridgeBarcode();
            String machineName = "Superman";
            IlluminaSequencingRunFactory runFactory =
                    new IlluminaSequencingRunFactory(EasyMock.createMock(JiraCommentUtil.class));
            ZimsIlluminaRunFactory zimsIlluminaRunFactory = constructZimsIlluminaRunFactory(productOrder1);

            SolexaRunBean runBean1 = new SolexaRunBean(flowcellBarcode, flowcellBarcode + dateFormat.format(runDate),
                    runDate, machineName, runPath.getAbsolutePath(), null);

            IlluminaSequencingRun run1 =
                    runFactory.buildDbFree(runBean1, hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell());

            ZimsIlluminaRun zimsIlluminaRun1 = zimsIlluminaRunFactory.makeZimsIlluminaRun(run1);
            Assert.assertEquals(zimsIlluminaRun1.getLanes().size(), 2, "Wrong number of lanes");
            Assert.assertEquals(zimsIlluminaRun1.getSystemOfRecord(), MercuryOrSquidRouter.MercuryOrSquid.MERCURY);
            ZimsIlluminaChamber zimsIlluminaChamber1 = zimsIlluminaRun1.getLanes().iterator().next();
            for (LibraryBean libraryBean : zimsIlluminaChamber1.getLibraries()) {
                Assert.assertEquals(libraryBean.getLcSet(), workflowBatch1.getBatchName());
                Assert.assertEquals(libraryBean.getProductOrderKey(), productOrder1.getBusinessKey());
            }

            runTransferVisualizer(reworkTube);

            SolexaRunBean runBean2 = new SolexaRunBean(flowcellBarcode, flowcellBarcode + dateFormat.format(runDate),
                    runDate, machineName, runPath.getAbsolutePath(), null);

            IlluminaSequencingRun run2 =
                    runFactory.buildDbFree(runBean2, hiSeq2500FlowcellEntityBuilder2.getIlluminaFlowcell());

            ZimsIlluminaRun zimsIlluminaRun2 = zimsIlluminaRunFactory.makeZimsIlluminaRun(run2);
            Assert.assertEquals(zimsIlluminaRun2.getLanes().size(), 2, "Wrong number of lanes");

            ZimsIlluminaChamber zimsIlluminaChamber2 = zimsIlluminaRun2.getLanes().iterator().next();
            for (LibraryBean libraryBean : zimsIlluminaChamber2.getLibraries()) {
                Assert.assertEquals(libraryBean.getLcSet(), workflowBatch2.getBatchName());
                Assert.assertEquals(libraryBean.getProductOrderKey(), productOrder2.getBusinessKey());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ZimsIlluminaRunFactory constructZimsIlluminaRunFactory(final ProductOrder productOrder) {
        return new ZimsIlluminaRunFactory(
                new BSPSampleDataFetcher() {
                    @Override
                    public Map<String, BSPSampleDTO> fetchSamplesFromBSP(@Nonnull Collection<String> sampleNames) {
                        Map<String, BSPSampleDTO> mapSampleIdToDto = new HashMap<>();
                        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>() {{
                            put(BSPSampleSearchColumn.PRIMARY_DISEASE, "Cancer");
                            put(BSPSampleSearchColumn.LSID, "org.broad:SM-1234");
                            put(BSPSampleSearchColumn.MATERIAL_TYPE, "DNA:DNA Genomic");
                            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "4321");
                            put(BSPSampleSearchColumn.SPECIES, "Homo Sapiens");
                            put(BSPSampleSearchColumn.PARTICIPANT_ID, "PT-1234");
                        }};
                        for (String sampleName : sampleNames) {
                            Map<BSPSampleSearchColumn, String> dataMapCopy = new HashMap<>(dataMap);
                            dataMapCopy.put(BSPSampleSearchColumn.SAMPLE_ID, sampleName);
                            mapSampleIdToDto.put(sampleName, new BSPSampleDTO(dataMapCopy));
                        }
                        return mapSampleIdToDto;
                    }
                },
                new AthenaClientService() {
                    @Override
                    public ProductOrder retrieveProductOrderDetails(@Nonnull String poBusinessKey) {
                        return productOrder;
                    }

                    @Override
                    public Map<String, List<ProductOrderSample>> findMapSampleNameToPoSample(List<String> sampleNames) {
                        return null;
                    }

                    @Override
                    public Collection<ProductOrder> retrieveMultipleProductOrderDetails(
                            @Nonnull Collection<String> poBusinessKeys) {
                        return null;
                    }
                },
                new ControlDao() {
                    @Override
                    public List<Control> findAllActive() {
                        return controlList;
                    }
                }
        );
    }

    /**
     * Build object graph for Whole Genome Shotgun messages, verify chain of events.
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testWholeGenomeShotgun() {
//        Controller.startCPURecording(true);
        expectedRouting = MercuryOrSquidRouter.MercuryOrSquid.SQUID;

        ProductOrder productOrder =
                ProductOrderTestFactory.buildWholeGenomeProductOrder(NUM_POSITIONS_IN_RACK);
        AthenaClientServiceStub.addProductOrder(productOrder);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("whole Genome Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflowName("Whole Genome");

        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        PreFlightEntityBuilder preFlightEntityBuilder =
                runPreflightProcess(mapBarcodeToTube, "1");
        ShearingEntityBuilder shearingEntityBuilder =
                runShearingProcess(mapBarcodeToTube, preFlightEntityBuilder.getTubeFormation(),
                        preFlightEntityBuilder.getRackBarcode(), "1");
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(shearingEntityBuilder.getShearingCleanupPlate(),
                        shearingEntityBuilder.getShearCleanPlateBarcode(), shearingEntityBuilder.getShearingPlate(),
                        "1");
        SageEntityBuilder sageEntityBuilder = runSageProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                libraryConstructionEntityBuilder.getPondRegTubeBarcodes());

        Assert.assertEquals(sageEntityBuilder.getSageCleanupRack().getSampleInstances().size(), NUM_POSITIONS_IN_RACK,
                "Wrong number of sage cleanup samples");

        QtpEntityBuilder qtpEntityBuilder =
                runQtpProcess(sageEntityBuilder.getSageCleanupRack(), sageEntityBuilder.getSageCleanupTubeBarcodes(),
                        sageEntityBuilder.getMapBarcodeToSageUnloadTubes(), "Whole Genome", "1");

        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1", null,
                        ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL, "Squid Designation", "Whole Genome");

        IlluminaFlowcell illuminaFlowcell = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell();
        Set<SampleInstance> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                VesselPosition.LANE1);
        Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(), 1,
                "Wrong number of reagents");

        Map.Entry<String, TwoDBarcodedTube> stringTwoDBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        stringTwoDBarcodedTubeEntry.getValue().evaluateCriteria(transferTraverserCriteria,
                TransferTraverserCriteria.TraversalDirection.Descendants);
        List<String> labEventNames = transferTraverserCriteria.getAllEventNamesPerHop();
        String[] expectedEventNames = {
                "PreflightPicoSetup",
                "PreflightPicoSetup",
                "PreflightPostNormPicoSetup",
                "PreflightPostNormPicoSetup",
                "ShearingTransfer",
                "PostShearingTransferCleanup",
                "ShearingQC",
                "AdapterLigationCleanup",
                "HybSelPondEnrichmentCleanup",
                "PondRegistration",
                "SageLoading",
                "SageUnloading",
                "SageCleanup",
                "PoolingTransfer",
                "EcoTransfer",
                "NormalizationTransfer",
                "DenatureTransfer",
                "StripTubeBTransfer",
                "FlowcellTransfer",
        };
        verifyEventSequence(labEventNames, expectedEventNames);

//        Controller.stopCPURecording();
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testPlateCherryPick() {
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();
        for (int rackPosition = 1; rackPosition <= SBSSection.P96COLS1_6BYROW.getWells().size(); rackPosition++) {
            String barcode = "R" + rackPosition;

            String bspStock = "SM-" + rackPosition;
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(bspStock));
            mapBarcodeToTube.put(barcode, bspAliquot);
        }

        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.putAll(mapBarcodeToTube);
        PlateCherryPickEvent rackToPlateCherryPickEvent = bettaLimsMessageTestFactory.buildCherryPick(
                "PoolingTransfer", Collections.singletonList("SourceRack"),
                Collections.<List<String>>singletonList(new ArrayList<>(mapBarcodeToTube.keySet())),
                Collections.singletonList("SourcePlate"), Collections.<List<String>>emptyList(),
                Arrays.asList(new BettaLimsMessageTestFactory.CherryPick("SourceRack", "A01", "SourcePlate", "A01")));
        rackToPlateCherryPickEvent.getPlate().get(0).setPhysType(LabEventFactory.PHYS_TYPE_EPPENDORF_96);
        LabEvent rackToPlateEntity =
                getLabEventFactory().buildFromBettaLims(rackToPlateCherryPickEvent, mapBarcodeToVessel);
        StaticPlate plate = (StaticPlate) rackToPlateEntity.getTargetLabVessels().iterator().next();

        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(plate.getLabel(), plate);
        PlateCherryPickEvent plateToRackCherryPickEvent = bettaLimsMessageTestFactory.buildCherryPick(
                "PoolingTransfer", Collections.singletonList("SourcePlate"), Collections.<List<String>>emptyList(),
                Collections.singletonList("TargetRack"), Collections.singletonList(Arrays.asList("tube1", "tube2")),
                Arrays.asList(new BettaLimsMessageTestFactory.CherryPick("SourcePlate", "A01", "TargetRack", "A01")));
        plateToRackCherryPickEvent.getSourcePlate().get(0).setPhysType(LabEventFactory.PHYS_TYPE_EPPENDORF_96);
        LabEvent plateToRackEntity =
                getLabEventFactory().buildFromBettaLims(plateToRackCherryPickEvent, mapBarcodeToVessel);
        LabVessel targetRack = plateToRackEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(
                targetRack.getContainerRole().getVesselAtPosition(VesselPosition.A01).getSampleInstances().size(),
                1, "Wrong number of sample instances");
    }

    /**
     * Build object graph for Fluidigm messages
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testFluidigm() {
        expectedRouting = MercuryOrSquidRouter.MercuryOrSquid.SQUID;

        // starting rack
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();
        for (int rackPosition = 1; rackPosition <= SBSSection.P96COLS1_6BYROW.getWells().size(); rackPosition++) {
            String barcode = "R" + rackPosition;

            String bspStock = "SM-" + rackPosition;
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(bspStock));
            mapBarcodeToTube.put(barcode, bspAliquot);
        }

        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);
        LabEventFactory labEventFactory = new LabEventFactory(null, null);
        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);

        LabBatchEjb labBatchEJB = new LabBatchEjb();
        labBatchEJB.setAthenaClientService(AthenaClientProducer.stubInstance());
        labBatchEJB.setJiraService(JiraServiceProducer.stubInstance());

        LabVesselDao tubeDao = EasyMock.createNiceMock(LabVesselDao.class);
        labBatchEJB.setTubeDao(tubeDao);

        JiraTicketDao mockJira = EasyMock.createNiceMock(JiraTicketDao.class);
        labBatchEJB.setJiraTicketDao(mockJira);

        LabBatchDao labBatchDao = EasyMock.createNiceMock(LabBatchDao.class);
        labBatchEJB.setLabBatchDao(labBatchDao);

        BucketDao mockBucketDao = EasyMock.createNiceMock(BucketDao.class);

        EasyMock.replay(mockBucketDao, tubeDao, mockJira, labBatchDao);

        LabEventHandler labEventHandler = getLabEventHandler();
        StaticPlate indexPlate = buildIndexPlate(null,
                Collections.singletonList(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7),
                Collections.singletonList("IndexPlate")).get(0);
        FluidigmMessagesBuilder fluidigmMessagesBuilder = new FluidigmMessagesBuilder("", bettaLimsMessageTestFactory,
                labEventFactory, labEventHandler, mapBarcodeToTube, indexPlate);
        fluidigmMessagesBuilder.buildJaxb();
        fluidigmMessagesBuilder.buildObjectGraph();
    }

    private void verifyEventSequence(List<String> labEventNames, String[] expectedEventNames) {
        /*
        * First, make sure that all expected event names are present. Then, check for extra events. Finally, make sure
        * that they're in the right order. The idea is that if all of the event positions are thrown off because of a
        * missing early event, that would be more useful feedback than the n+1th event being in the nth slot.
        */
        for (String expectedEventName : expectedEventNames) {
            /*
             * Concatenate a " " to the expected event name to match, for example, "Hybridization into ..." without
             * falsely matching on "HybridizationCleanup into ...".
             */
            // TODO: try removing ugly Matchers.<String> syntax after moving to Java 7
            MatcherAssert.assertThat(labEventNames, Matchers.<String>hasItem(Matchers.startsWith(expectedEventName + " ")));
        }

        Assert.assertEquals(labEventNames.size(), expectedEventNames.length, "Wrong number of transfers");

        for (int i = 0; i < expectedEventNames.length; i++) {
            MatcherAssert.assertThat("Unexpected event at position " + i, labEventNames.get(i), Matchers.startsWith(expectedEventNames[i]));
        }
    }

    /**
     * Builds entity graph for Fluidigm events
     */
    private static class FluidigmMessagesBuilder {
        private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;
        private final String testPrefix;
        private final Map<String, TwoDBarcodedTube> mapBarcodeToTube;
        private final StaticPlate indexPlate;

        private String rackBarcode;
        private String chipBarcode;
        private PlateTransferEventType fluidigmSampleInputJaxb;
        private PlateTransferEventType fluidigmIndexedAdapterInputJaxb;
        private PlateTransferEventType fluidigmHarvestingToRackJaxb;
        private final List<BettaLIMSMessage> messageList = new ArrayList<>();
        private String harvestRackBarcode;

        private FluidigmMessagesBuilder(String testPrefix, BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                        LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                        Map<String, TwoDBarcodedTube> mapBarcodeToTube, StaticPlate indexPlate) {
            this.testPrefix = testPrefix;
            this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
            this.mapBarcodeToTube = mapBarcodeToTube;
            this.indexPlate = indexPlate;
        }

        private void buildJaxb() {
            // FluidigmSampleInput rack P96COLS1-6BYROW to chip P384COLS4-6BYROW
            ArrayList<String> tubeBarcodes = new ArrayList<>(mapBarcodeToTube.keySet());
            chipBarcode = "Fluidigm" + testPrefix;
            rackBarcode = "InputRack" + testPrefix;
            fluidigmSampleInputJaxb = bettaLimsMessageTestFactory.buildRackToPlate("FluidigmSampleInput", rackBarcode,
                    tubeBarcodes, chipBarcode);
            fluidigmSampleInputJaxb.getSourcePlate().setSection(SBSSection.P96COLS1_6BYROW.getSectionName());
            PositionMapType sourcePositionMap = buildFluidigmPositionMap(tubeBarcodes,
                    fluidigmSampleInputJaxb.getSourcePlate()
                            .getBarcode());
            fluidigmSampleInputJaxb.setSourcePositionMap(sourcePositionMap);
            fluidigmSampleInputJaxb.getPlate().setPhysType(
                    StaticPlate.PlateType.Fluidigm48_48AccessArrayIFC.getDisplayName());
            fluidigmSampleInputJaxb.getPlate().setSection(SBSSection.P384COLS4_6BYROW.getSectionName());
            bettaLimsMessageTestFactory.addMessage(messageList, fluidigmSampleInputJaxb);

            // FluidigmIndexedAdapterInput plate P96COLS1-6BYROW to chip P384COLS4-6BYROW
            fluidigmIndexedAdapterInputJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                    "FluidigmIndexedAdapterInput", indexPlate.getLabel(), chipBarcode);
            fluidigmIndexedAdapterInputJaxb.getSourcePlate().setPhysType(
                    StaticPlate.PlateType.IndexedAdapterPlate96.getDisplayName());
            fluidigmIndexedAdapterInputJaxb.getSourcePlate().setSection(SBSSection.P96COLS1_6BYROW.getSectionName());
            fluidigmIndexedAdapterInputJaxb.getPlate().setPhysType(
                    StaticPlate.PlateType.Fluidigm48_48AccessArrayIFC.getDisplayName());
            fluidigmIndexedAdapterInputJaxb.getPlate().setSection(SBSSection.P384COLS4_6BYROW.getSectionName());
            bettaLimsMessageTestFactory
                    .addMessage(messageList, fluidigmIndexedAdapterInputJaxb);

            // FluidigmHarvestingToRack chip P384COLS4-6BYROW to rack P96COLS1-6BYROW
            harvestRackBarcode = "Harvest" + testPrefix;
            List<String> harvestTubeBarcodes = new ArrayList<>();
            for (int rackPosition = 1; rackPosition <= mapBarcodeToTube.size(); rackPosition++) {
                harvestTubeBarcodes.add("Harvest" + testPrefix + rackPosition);
            }
            fluidigmHarvestingToRackJaxb = bettaLimsMessageTestFactory.buildPlateToRack("FluidigmHarvestingToRack",
                    chipBarcode, harvestRackBarcode, harvestTubeBarcodes);
            fluidigmHarvestingToRackJaxb.getSourcePlate().setPhysType(
                    StaticPlate.PlateType.Fluidigm48_48AccessArrayIFC.getDisplayName());
            fluidigmHarvestingToRackJaxb.getSourcePlate().setSection(SBSSection.P384COLS4_6BYROW.getSectionName());

            fluidigmHarvestingToRackJaxb.setPositionMap(buildFluidigmPositionMap(tubeBarcodes,
                    fluidigmHarvestingToRackJaxb.getPlate().getBarcode()));
            fluidigmHarvestingToRackJaxb.getPlate().setSection(SBSSection.P96COLS1_6BYROW.getSectionName());
            bettaLimsMessageTestFactory.addMessage(messageList, fluidigmHarvestingToRackJaxb);
        }

        private PositionMapType buildFluidigmPositionMap(ArrayList<String> tubeBarcodes, String rackBarcode) {
            PositionMapType sourcePositionMap = new PositionMapType();
            sourcePositionMap.setBarcode(rackBarcode);
            int barcodeIndex = 0;
            for (int row = 0; row < 8; row++) {
                for (int column = 1; column <= 6; column++) {
                    ReceptacleType receptacleType = new ReceptacleType();
                    receptacleType.setBarcode(tubeBarcodes.get(barcodeIndex));
                    receptacleType.setPosition(bettaLimsMessageTestFactory.buildWellName(row * 12 + column,
                            BettaLimsMessageTestFactory.WellNameType.SHORT));
                    sourcePositionMap.getReceptacle().add(receptacleType);
                    barcodeIndex++;
                }
            }
            return sourcePositionMap;
        }

        private void buildObjectGraph() {
            Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
            mapBarcodeToVessel.putAll(mapBarcodeToTube);
            LabEvent fluidigmSampleInputEntity = labEventFactory.buildFromBettaLims(fluidigmSampleInputJaxb,
                    mapBarcodeToVessel);
            labEventHandler.processEvent(fluidigmSampleInputEntity);
            // asserts
            StaticPlate chip = (StaticPlate) fluidigmSampleInputEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(chip.getSampleInstances().size(), mapBarcodeToTube.size(),
                    "Wrong number of sample instances");

            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(indexPlate.getLabel(), indexPlate);
            mapBarcodeToVessel.put(chip.getLabel(), chip);
            LabEvent fluidigmIndexedAdapterInputEntity = labEventFactory.buildFromBettaLims(
                    fluidigmIndexedAdapterInputJaxb, mapBarcodeToVessel);
            labEventHandler.processEvent(fluidigmIndexedAdapterInputEntity);

            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(chip.getLabel(), chip);
            LabEvent fluidigmHarvestingToRackEntity = labEventFactory.buildFromBettaLims(
                    fluidigmHarvestingToRackJaxb, mapBarcodeToVessel);
            labEventHandler.processEvent(fluidigmHarvestingToRackEntity);
            // asserts
            TubeFormation harvestRack =
                    (TubeFormation) fluidigmHarvestingToRackEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(harvestRack.getSampleInstances().size(), mapBarcodeToTube.size(),
                    "Wrong number of sample instances");
        }
    }

    public static void validateWorkflow(String nextEventTypeName, Collection<? extends LabVessel> tubes) {
        List<LabVessel> labVessels = new ArrayList<>(tubes);
        validateWorkflow(nextEventTypeName, labVessels);
    }

    public static void validateWorkflow(String nextEventTypeName, LabVessel labVessel) {
        validateWorkflow(nextEventTypeName, Collections.singletonList(labVessel));
    }

    public static void validateWorkflow(String nextEventTypeName, List<LabVessel> labVessels) {
        MercuryOrSquidRouter mercuryOrSquidRouter = new MercuryOrSquidRouter(null, null, new WorkflowLoader(), null);
        MercuryOrSquidRouter.MercuryOrSquid mercuryOrSquid = mercuryOrSquidRouter.routeForVessels(labVessels,
                controlCollaboratorIdList, mapSampleNameToDto, MercuryOrSquidRouter.Intent.ROUTE);
        Assert.assertEquals(mercuryOrSquid, expectedRouting);

        WorkflowValidator workflowValidator = new WorkflowValidator();
        AthenaClientService athenaClientService = AthenaClientProducer.stubInstance();
        workflowValidator.setAthenaClientService(athenaClientService);
        List<WorkflowValidator.WorkflowValidationError> workflowValidationErrors =
                workflowValidator.validateWorkflow(labVessels, nextEventTypeName);
        if (!workflowValidationErrors.isEmpty()) {
            WorkflowValidator.WorkflowValidationError workflowValidationError = workflowValidationErrors.get(0);
            ProductWorkflowDefVersion.ValidationError validationError = workflowValidationError.getErrors().get(0);
            Assert.fail(validationError.getMessage() + " expected " + validationError.getExpectedEventNames() +
                        " actual " + validationError.getActualEventNames());
        }
    }

    /**
     * Builds plates of molecular indexes for the given index positions.  If there are multiple plates, e.g. P5 and P7,
     * a merged P5/P7 scheme is also created, so {@link SampleInstance#addReagent(Reagent)} can find it.
     *
     * @param molecularIndexingSchemeDao DAO, nullable if in database free test
     * @param indexPositions             list of positions, e.g. P5, P7
     * @param indexPlateBarcodes         list of barcodes for plates to create
     */
    public static List<StaticPlate> buildIndexPlate(@Nullable MolecularIndexingSchemeDao molecularIndexingSchemeDao,
                                                    List<MolecularIndexingScheme.IndexPosition> indexPositions,
                                                    List<String> indexPlateBarcodes) {

        char[] bases = {'A', 'C', 'T', 'G'};

        List<StaticPlate> indexPlates = new ArrayList<>();
        int arrayIndex = 0;
        for (final MolecularIndexingScheme.IndexPosition indexPosition : indexPositions) {
            String indexPlateBarcode = indexPlateBarcodes.get(arrayIndex);
            StaticPlate indexPlate = new StaticPlate(indexPlateBarcode, StaticPlate.PlateType.IndexedAdapterPlate96);
            for (VesselPosition vesselPosition : SBSSection.ALL96.getWells()) {
                PlateWell plateWell = new PlateWell(indexPlate, vesselPosition);
                StringBuilder stringBuilder = new StringBuilder();

                // Build a deterministic sequence from the vesselPosition, by indexing into the bases array
                int base4Ordinal = Integer.parseInt(Integer.toString(vesselPosition.ordinal() + 1, 4), 4);
                while (base4Ordinal > 0) {
                    stringBuilder.append(bases[base4Ordinal % 4]);
                    base4Ordinal = base4Ordinal / 4;
                }

                final String sequence = stringBuilder.toString();
                MolecularIndexingScheme testScheme = null;
                if (molecularIndexingSchemeDao != null) {
                    testScheme = molecularIndexingSchemeDao
                            .findSingleIndexScheme(indexPosition, sequence);
                }
                if (testScheme == null) {
                    testScheme = new MolecularIndexingScheme(
                            new EnumMap<MolecularIndexingScheme.IndexPosition, MolecularIndex>(
                                    MolecularIndexingScheme.IndexPosition.class) {{
                                put(indexPosition, new MolecularIndex(sequence));
                            }});
                    if (molecularIndexingSchemeDao != null) {
                        molecularIndexingSchemeDao.persist(testScheme);
                        molecularIndexingSchemeDao.flush();
                        molecularIndexingSchemeDao.clear();
                    }

                }
                MolecularIndexReagent molecularIndexReagent = new MolecularIndexReagent(testScheme);
                plateWell.addReagent(molecularIndexReagent);
                indexPlate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
            }
            indexPlates.add(indexPlate);
            arrayIndex++;
        }

        if (indexPlates.size() > 1) {
            for (VesselPosition vesselPosition : indexPlates.get(0).getVesselGeometry().getVesselPositions()) {
                Map<MolecularIndexingScheme.IndexPosition, MolecularIndex> mapPositionToIndex =
                        new EnumMap<>(MolecularIndexingScheme.IndexPosition.class);
                for (StaticPlate indexPlate : indexPlates) {
                    PlateWell well = indexPlate.getContainerRole().getVesselAtPosition(vesselPosition);
                    MolecularIndexingScheme molecularIndexingScheme = ((MolecularIndexReagent) well.
                            getReagentContents().iterator().next()).getMolecularIndexingScheme();
                    mapPositionToIndex.putAll(molecularIndexingScheme.getIndexes());
                }
                new MolecularIndexingScheme(mapPositionToIndex);
            }
        }
        return indexPlates;
    }

    public static TwoDBarcodedTube buildBaitTube(String tubeBarcode, ReagentDesign reagent) {
        TwoDBarcodedTube baitTube = new TwoDBarcodedTube(tubeBarcode);
        if (reagent == null) {
            reagent = new ReagentDesign("cancer_2000gene_shift170_undercovered",
                    ReagentDesign.ReagentType.BAIT);
            reagent.setTargetSetName("Cancer_2K");
            reagent.setManufacturersName("1234abc");
        }
        baitTube.addReagent(new DesignedReagent(reagent));
        return baitTube;
    }
}
