package org.broadinstitute.gpinformatics.mercury.test;

//import com.jprofiler.api.agent.Controller;

import org.apache.commons.collections4.Factory;
import org.apache.commons.collections4.map.LazySortedMap;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
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
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferEntityGrapher;
import org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferVisualizer;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.control.zims.ZimsIlluminaRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkDetail;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkLevel;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkReason;
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
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.IceEntityBuilder;
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
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
@Test(groups = TestGroups.DATABASE_FREE)
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
     * Used in test verification, accumulates the events in a chain of transfers
     */
    public static class ListTransfersFromStart implements TransferTraverserCriteria {
        private int hopCount = -1;
        private final List<String> labEventNames = new ArrayList<>();

        private final SortedMap<Integer, SortedSet<LabEvent>> labEventNamesByHopCount =
                LazySortedMap.lazySortedMap(new TreeMap<Integer, SortedSet<LabEvent>>(),
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

        expectedRouting = SystemRouter.System.SQUID;
        // todo jmt receipt batch?
        ProductOrder productOrder = ProductOrderTestFactory.buildHybridSelectionProductOrder(NUM_POSITIONS_IN_RACK - 2,
                                                                                             "A");
        productOrder.getResearchProject().setJiraTicketKey("RP-123");

        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("Hybrid Selection Batch",
                                              new HashSet<LabVessel>(mapBarcodeToTube.values()),
                                              LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.HYBRID_SELECTION);
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        TubeFormation daughterTubeFormation = daughterPlateTransfer(mapBarcodeToTube);

        Map<String, BarcodedTube> mapBarcodeToDaughterTube = new HashMap<>();
        for (BarcodedTube barcodedTube : daughterTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(barcodedTube.getLabel(), barcodedTube);
        }

        PreFlightEntityBuilder preFlightEntityBuilder = runPreflightProcess(mapBarcodeToDaughterTube,
                                                                            "1");
        ShearingEntityBuilder shearingEntityBuilder = runShearingProcess(mapBarcodeToDaughterTube,
                                                                         preFlightEntityBuilder.getTubeFormation(),
                                                                         preFlightEntityBuilder.getRackBarcode(), "1");
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = runLibraryConstructionProcess(
                shearingEntityBuilder.getShearingCleanupPlate(), shearingEntityBuilder.getShearCleanPlateBarcode(),
                shearingEntityBuilder.getShearingPlate(), "1");
        HybridSelectionEntityBuilder hybridSelectionEntityBuilder = runHybridSelectionProcess(
                libraryConstructionEntityBuilder.getPondRegRack(),
                libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), "1");
        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                                                          hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                                                          hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                                                          "1");

        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1", null,
                                            ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL, "Squid Designation",
                                            Workflow.HYBRID_SELECTION);

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
        Assert.assertEquals(zimsIlluminaRun.getSystemOfRecord(), SystemRouter.System.MERCURY);
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
        int positiveControlCount = 0;
        int negativeControlCount = 0;
        for (LibraryBean bean : zimsIlluminaChamber.getLibraries()) {
            // Every library should have an LCSET, even controls.
            Assert.assertEquals(bean.getLcSet(), workflowBatch.getBatchName());
            if (bean.isPositiveControl() != null && bean.isPositiveControl()) {
                positiveControlCount++;
            }
            if (bean.isNegativeControl() != null && bean.isNegativeControl()) {
                negativeControlCount++;
            }
        }
        Assert.assertEquals(positiveControlCount, 1);
        Assert.assertEquals(negativeControlCount, 1);

        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        BarcodedTube startingTube = mapBarcodeToTube.entrySet().iterator().next().getValue();
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
        expectedRouting = SystemRouter.System.MERCURY;

        // Use Standard Exome product, to verify that workflow is taken from LCSet, not Product
        ProductOrder productOrder = ProductOrderTestFactory.buildHybridSelectionProductOrder(NUM_POSITIONS_IN_RACK - 2,
                                                                                             "A");
        Date runDate = new Date();
        // todo jmt create bucket, then batch, rather than rack then batch then bucket
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                                              new HashSet<LabVessel>(mapBarcodeToTube.values()),
                                              LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);

        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        TubeFormation daughterTubeFormation = daughterPlateTransfer(mapBarcodeToTube);

        Map<String, BarcodedTube> mapBarcodeToDaughterTube = new HashMap<>();
        for (BarcodedTube barcodedTube : daughterTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(barcodedTube.getLabel(), barcodedTube);
        }

        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToDaughterTube,
                                                                                  String.valueOf(runDate.getTime()),
                                                                                  "1", true);
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
                                                          hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                                                          "1");

        final LabVessel denatureSource =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        LabBatch fctBatch =
                new LabBatch(FCT_TICKET,
                             Collections.singleton(denatureSource),
                             LabBatch.LabBatchType.FCT);

        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", FCT_TICKET,
                                            ProductionFlowcellPath.DILUTION_TO_FLOWCELL, null,
                                            Workflow.AGILENT_EXOME_EXPRESS);

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
        Assert.assertEquals(zimsIlluminaRun.getSystemOfRecord(), SystemRouter.System.MERCURY);

        Map.Entry<String, BarcodedTube> stringBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        stringBarcodedTubeEntry.getValue().evaluateCriteria(transferTraverserCriteria,
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
    public void testExomeExpressAlternative() {
        expectedRouting = SystemRouter.System.MERCURY;

        ProductOrder productOrder = ProductOrderTestFactory.buildHybridSelectionProductOrder(NUM_POSITIONS_IN_RACK,
                                                                                             "A");
        Date runDate = new Date();
        // todo jmt create bucket, then batch, rather than rack then batch then bucket
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                                              new HashSet<LabVessel>(mapBarcodeToTube.values()),
                                              LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);

        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                                                                                  String.valueOf(runDate.getTime()),
                                                                                  "1", true);
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
                                                          hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                                                          "1");
        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", "squidDesignationName",
                                            ProductionFlowcellPath.DENATURE_TO_FLOWCELL, null,
                                            Workflow.AGILENT_EXOME_EXPRESS);

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
        Assert.assertEquals(zimsIlluminaRun.getSystemOfRecord(), SystemRouter.System.MERCURY);

        Map.Entry<String, BarcodedTube> stringBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        stringBarcodedTubeEntry.getValue().evaluateCriteria(transferTraverserCriteria,
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
            expectedRouting = SystemRouter.System.MERCURY;

            // Use Standard Exome product, to verify that workflow is taken from LCSet, not Product
            ProductOrder productOrder1 = ProductOrderTestFactory.buildHybridSelectionProductOrder(NUM_POSITIONS_IN_RACK,
                                                                                                  "A");
            Date runDate = new Date();

            Map<String, BarcodedTube> mapBarcodeToTube1 = createInitialRack(productOrder1, "R1_");
            LabBatch workflowBatch1 = new LabBatch("Exome Express Batch 1",
                                                   new HashSet<LabVessel>(mapBarcodeToTube1.values()),
                                                   LabBatch.LabBatchType.WORKFLOW);
            workflowBatch1.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
            workflowBatch1.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);

            bucketBatchAndDrain(mapBarcodeToTube1, productOrder1, workflowBatch1, "1");
            PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube1,
                                                                                      String.valueOf(runDate.getTime()),
                                                                                      "1", true);
            ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                    runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                                                   picoPlatingEntityBuilder.getNormTubeFormation(),
                                                   picoPlatingEntityBuilder.getNormalizationBarcode(), "1");

            ProductOrder productOrder2 = ProductOrderTestFactory.buildHybridSelectionProductOrder(
                    NUM_POSITIONS_IN_RACK - 1, "B");

            Map<String, BarcodedTube> mapBarcodeToTube2 = createInitialRack(productOrder2, "R2_");
            BarcodedTube reworkTube = mapBarcodeToTube1.values().iterator().next();
            reworkTube.clearCaches();
            mapBarcodeToTube2.put(reworkTube.getLabel(), reworkTube);
            LabBatch workflowBatch2 = new LabBatch("Exome Express Batch 2",
                                                   new HashSet<LabVessel>(mapBarcodeToTube2.values()),
                                                   LabBatch.LabBatchType.WORKFLOW);
            workflowBatch2.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
            workflowBatch2.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);

            Bucket bucket = bucketBatchAndDrain(mapBarcodeToTube2, productOrder2, workflowBatch2, "2");
            PicoPlatingEntityBuilder picoPlatingEntityBuilder2 = runPicoPlatingProcess(mapBarcodeToTube2,
                                                                                       String.valueOf(
                                                                                               runDate.getTime()), "2",
                                                                                       true);
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
                                                               hybridSelectionEntityBuilder2
                                                                       .getMapBarcodeToNormCatchTubes(), "2");
            HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder2 = runHiSeq2500FlowcellProcess(
                    qtpEntityBuilder2.getDenatureRack(), "2" + "ADXX", "squidDesignationName",
                    ProductionFlowcellPath.DENATURE_TO_FLOWCELL, null, Workflow.AGILENT_EXOME_EXPRESS);

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
                    hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), "1");
            // Rework in pooling bucket should not change LCSET in pipeline
            BarcodedTube catchTube = hybridSelectionEntityBuilder.getNormCatchRack().getContainerRole().
                    getVesselAtPosition(VesselPosition.A01);
            catchTube.clearCaches();
            BucketEntry bucketEntry = new BucketEntry(catchTube, productOrder1, bucket,
                    BucketEntry.BucketEntryType.PDO_ENTRY);
            LabBatch poolReworkBatch = new LabBatch("LCSET-pool", Collections.<LabVessel>singleton(catchTube),
                    LabBatch.LabBatchType.WORKFLOW);
            bucketEntry.setLabBatch(poolReworkBatch);
            catchTube.addBucketEntry(bucketEntry);
            HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder = runHiSeq2500FlowcellProcess(
                    qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", "squidDesignationName",
                    ProductionFlowcellPath.DENATURE_TO_FLOWCELL, null, Workflow.AGILENT_EXOME_EXPRESS);

            runTransferVisualizer(mapBarcodeToTube1.values().iterator().next());

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
            Assert.assertEquals(zimsIlluminaRun2.getSystemOfRecord(), SystemRouter.System.MERCURY);

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
            expectedRouting = SystemRouter.System.MERCURY;

            // Use Standard Exome product, to verify that workflow is taken from LCSet, not Product
            ProductOrder productOrder1 = ProductOrderTestFactory.buildHybridSelectionProductOrder(NUM_POSITIONS_IN_RACK,
                                                                                                  "A");
            Date runDate = new Date();

            Map<String, BarcodedTube> mapBarcodeToTube1 = createInitialRack(productOrder1, "R1_");
            LabBatch workflowBatch1 = new LabBatch("Exome Express Pico Batch 1",
                                                   new HashSet<LabVessel>(mapBarcodeToTube1.values()),
                                                   LabBatch.LabBatchType.WORKFLOW);
            workflowBatch1.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
            workflowBatch1.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);

            bucketBatchAndDrain(mapBarcodeToTube1, productOrder1, workflowBatch1, "1");
            PicoPlatingEntityBuilder picoPlatingEntityBuilder1 = runPicoPlatingProcess(mapBarcodeToTube1,
                                                                                       String.valueOf(
                                                                                               runDate.getTime()), "1",
                                                                                       true);
            ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                    runExomeExpressShearingProcess(picoPlatingEntityBuilder1.getNormBarcodeToTubeMap(),
                                                   picoPlatingEntityBuilder1.getNormTubeFormation(),
                                                   picoPlatingEntityBuilder1.getNormalizationBarcode(), "1");

            LabBatch importBatch1 = new LabBatch("EX-123", new HashSet<LabVessel>(mapBarcodeToTube1.values()),
                                                 LabBatch.LabBatchType.SAMPLES_IMPORT);

            ProductOrder productOrder2 = ProductOrderTestFactory.buildHybridSelectionProductOrder(95, "B");

            Map<String, BarcodedTube> mapBarcodeToTube2 = createInitialRack(productOrder2, "R2_");
            LabBatch workflowBatch2 = new LabBatch("Exome Express Pico Batch 2",
                                                   new HashSet<LabVessel>(mapBarcodeToTube2.values()),
                                                   LabBatch.LabBatchType.WORKFLOW);
            workflowBatch2.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
            workflowBatch2.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);

            bucketBatchAndDrain(mapBarcodeToTube2, productOrder2, workflowBatch2, "2");
            PicoPlatingEntityBuilder picoPlatingEntityBuilder2 = runPicoPlatingProcess(mapBarcodeToTube2,
                                                                                       String.valueOf(
                                                                                               runDate.getTime()), "2",
                                                                                       true);

            LabBatch importBatch2 = new LabBatch("EX-123", new HashSet<LabVessel>(mapBarcodeToTube2.values()),
                                                 LabBatch.LabBatchType.SAMPLES_IMPORT);

            // Add sample from LCSET 1 TO LCSET 2 at shearing bucket
            final BarcodedTube reworkTube =
                    picoPlatingEntityBuilder1.getNormBarcodeToTubeMap().values().iterator().next();
            reworkTube.clearCaches();
//            workflowBatch2.addLabVessel(reworkTube);
            Map<String, BarcodedTube> mapBarcodeToTubesPlusRework =
                    new LinkedHashMap<>(picoPlatingEntityBuilder2.getNormBarcodeToTubeMap());
            mapBarcodeToTubesPlusRework.put(reworkTube.getLabel(), reworkTube);

            Bucket shearingBucket = createAndPopulateBucket(
                    new HashMap<String, BarcodedTube>() {{
                        put(reworkTube.getLabel(), reworkTube);
                    }}, productOrder2, "Shearing");
            workflowBatch2.addLabVessel(reworkTube);
            drainBucket(shearingBucket);
            reworkTube.getBucketEntries().iterator().next().setReworkDetail(new ReworkDetail(
                    new ReworkReason(ReworkEntry.ReworkReasonEnum.MACHINE_ERROR.getValue()),
                    ReworkLevel.ONE_SAMPLE_RELEASE_REST_BATCH,
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
                                                               hybridSelectionEntityBuilder2
                                                                       .getMapBarcodeToNormCatchTubes(), "2");
            HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder2 =
                    runHiSeq2500FlowcellProcess(qtpEntityBuilder2.getDenatureRack(), "2" + "ADXX", null,
                                                ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL, "squidDesignationName",
                                                Workflow.AGILENT_EXOME_EXPRESS);

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
                                                              hybridSelectionEntityBuilder
                                                                      .getMapBarcodeToNormCatchTubes(), "1");
            HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                    runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", null,
                                                ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL, "squidDesignationName",
                                                Workflow.AGILENT_EXOME_EXPRESS);

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
            Assert.assertEquals(zimsIlluminaRun1.getSystemOfRecord(), SystemRouter.System.MERCURY);
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

    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testExomeExpressIce() {
        // e.g. 0157473471
//        Controller.startCPURecording(true);
        expectedRouting = SystemRouter.System.MERCURY;

        // Use Standard Exome product, to verify that workflow is taken from LCSet, not Product
        ProductOrder productOrder = ProductOrderTestFactory.buildHybridSelectionProductOrder(NUM_POSITIONS_IN_RACK - 2,
                                                                                             "A");
        Date runDate = new Date();
        // todo jmt create bucket, then batch, rather than rack then batch then bucket
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                                              new HashSet<LabVessel>(mapBarcodeToTube.values()),
                                              LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setCreatedOn(new Date());
        workflowBatch.setWorkflow(Workflow.ICE_EXOME_EXPRESS);

        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        TubeFormation daughterTubeFormation = daughterPlateTransfer(mapBarcodeToTube);

        Map<String, BarcodedTube> mapBarcodeToDaughterTube = new HashMap<>();
        for (BarcodedTube barcodedTube : daughterTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(barcodedTube.getLabel(), barcodedTube);
        }

        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToDaughterTube,
                                                                                  String.valueOf(runDate.getTime()),
                                                                                  "1", true);
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                                               picoPlatingEntityBuilder.getNormTubeFormation(),
                                               picoPlatingEntityBuilder.getNormalizationBarcode(), "1");
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                                              exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                                              exomeExpressShearingEntityBuilder.getShearingPlate(), "1");

        IceEntityBuilder iceEntityBuilder = runIceProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                                                          libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                                                          libraryConstructionEntityBuilder.getPondRegTubeBarcodes(),
                                                          "1");

        // Need a version of QTP that jumps over pooling to normalization
        QtpEntityBuilder qtpEntityBuilder = new QtpEntityBuilder(
                getBettaLimsMessageTestFactory(), getLabEventFactory(), getLabEventHandler(),
                Collections.singletonList(iceEntityBuilder.getCatchEnrichRack()),
                Collections.singletonList(iceEntityBuilder.getCatchEnrichRack().getLabel()),
                Collections.singletonList(iceEntityBuilder.getCatchEnrichBarcodes()),
                iceEntityBuilder.getMapBarcodeToCatchEnrichTubes(), "1").invoke(false, true);

        final LabVessel denatureSource = qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(
                VesselPosition.A01);
        LabBatch fctBatch = new LabBatch(FCT_TICKET, Collections.singleton(denatureSource), LabBatch.LabBatchType.FCT);

        // todo jmt denature rack has 8 source tubes, but 2500 builder is expecting only 1
        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", FCT_TICKET,
                                            ProductionFlowcellPath.DILUTION_TO_FLOWCELL, null,
                                            Workflow.AGILENT_EXOME_EXPRESS);

        runTransferVisualizer(mapBarcodeToTube.values().iterator().next());

        IlluminaFlowcell illuminaFlowcell = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell();
        Set<SampleInstance> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                VesselPosition.LANE1);
        Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(), 3,
                            "Wrong number of reagents");
        Set<SampleInstance> lane2SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                VesselPosition.LANE2);
        Assert.assertEquals(lane2SampleInstances.iterator().next().getReagents().size(), 3,
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
        Assert.assertEquals(zimsIlluminaRun.getSystemOfRecord(), SystemRouter.System.MERCURY);

        Map.Entry<String, BarcodedTube> stringBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        stringBarcodedTubeEntry.getValue().evaluateCriteria(transferTraverserCriteria,
                                                                TransferTraverserCriteria.TraversalDirection.Descendants);
        List<String> labEventNames = transferTraverserCriteria.getAllEventNamesPerHop();

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
                "IcePoolingTransfer",
                "IceSPRIConcentration",
                "IcePoolTest",
                "Ice1stHybridization",
                "Ice1stCapture",
                "Ice2ndCapture",
                "IceCatchCleanup",
                "IceCatchEnrichmentCleanup",
//                "EcoTransfer",
                "NormalizationTransfer",
                "DenatureTransfer",
                // todo jmt why aren't these events found?
//                "DenatureToDilutionTransfer",
//                "DilutionToFlowcellTransfer",
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
     * Build object graph for Whole Genome Shotgun messages, verify chain of events.
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testWholeGenomeShotgun() {
//        Controller.startCPURecording(true);
        expectedRouting = SystemRouter.System.SQUID;

        ProductOrder productOrder =
                ProductOrderTestFactory.buildWholeGenomeProductOrder(NUM_POSITIONS_IN_RACK);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");

        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("whole Genome Batch",
                                              new HashSet<LabVessel>(mapBarcodeToTube.values()),
                                              LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.WHOLE_GENOME);

        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        PreFlightEntityBuilder preFlightEntityBuilder =
                runPreflightProcess(mapBarcodeToTube, "1");
        ShearingEntityBuilder shearingEntityBuilder =
                runShearingProcess(mapBarcodeToTube, preFlightEntityBuilder.getTubeFormation(),
                                   preFlightEntityBuilder.getRackBarcode(), "1");
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(shearingEntityBuilder.getShearingCleanupPlate(),
                                              shearingEntityBuilder.getShearCleanPlateBarcode(),
                                              shearingEntityBuilder.getShearingPlate(),
                                              "1");
        SageEntityBuilder sageEntityBuilder = runSageProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                                                             libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                                                             libraryConstructionEntityBuilder.getPondRegTubeBarcodes());

        Assert.assertEquals(sageEntityBuilder.getSageCleanupRack().getSampleInstances().size(), NUM_POSITIONS_IN_RACK,
                            "Wrong number of sage cleanup samples");

        QtpEntityBuilder qtpEntityBuilder =
                runQtpProcess(sageEntityBuilder.getSageCleanupRack(), sageEntityBuilder.getSageCleanupTubeBarcodes(),
                              sageEntityBuilder.getMapBarcodeToSageUnloadTubes(), "1");

        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1", null,
                                            ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL, "Squid Designation",
                                            Workflow.WHOLE_GENOME);

        IlluminaFlowcell illuminaFlowcell = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell();
        Set<SampleInstance> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                VesselPosition.LANE1);
        Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(), 1,
                            "Wrong number of reagents");

        Map.Entry<String, BarcodedTube> stringBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        stringBarcodedTubeEntry.getValue().evaluateCriteria(transferTraverserCriteria,
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
        Map<String, BarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();
        for (int rackPosition = 1; rackPosition <= SBSSection.P96COLS1_6BYROW.getWells().size(); rackPosition++) {
            String barcode = "R" + rackPosition;

            String bspStock = "SM-" + rackPosition;
            BarcodedTube bspAliquot = new BarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(bspStock, MercurySample.MetadataSource.BSP));
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

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testDaughterPlateTransferFromMultipleSources() {
        //        Controller.startCPURecording(true);
        expectedRouting = SystemRouter.System.MERCURY;

        // Use Standard Exome product, to verify that workflow is taken from LCSet, not Product
        ProductOrder productOrder =
                ProductOrderTestFactory.buildHybridSelectionProductOrder((NUM_POSITIONS_IN_RACK - 2) * 2,
                                                                         "A");
        Date runDate = new Date();

        // Create 2 racks of tubes
        Map<String, BarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();
        Map<String, BarcodedTube> mapBarcodeToTube2 = new LinkedHashMap<>();
        int rackPosition = 0;
        for (ProductOrderSample poSample : productOrder.getSamples()) {
            String barcode = "R" + rackPosition;
            BarcodedTube bspAliquot = new BarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(poSample.getName(), MercurySample.MetadataSource.BSP));

            if (rackPosition >= 94) {
                mapBarcodeToTube2.put(barcode, bspAliquot);
            } else {
                mapBarcodeToTube.put(barcode, bspAliquot);
            }
            Map<BSPSampleSearchColumn, String> dataMap = new HashMap<>();
            dataMap.put(BSPSampleSearchColumn.SAMPLE_ID, poSample.getName());
            mapSampleNameToDto.put(poSample.getName(), new BSPSampleDTO(dataMap));

            rackPosition++;
        }

        // Swap wells from rack 1 with wells from rack 2
        List<Integer> wellsToReplace = new ArrayList<>();
        wellsToReplace.add(5);
        wellsToReplace.add(35);
        wellsToReplace.add(88);

        TubeFormation daughterTubeFormation =
                mismatchedDaughterPlateTransfer(mapBarcodeToTube, mapBarcodeToTube2, wellsToReplace);


        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(mapBarcodeToTube.keySet());
        Set<Map.Entry<String, BarcodedTube>> entries = mapBarcodeToTube2.entrySet();
        for (Integer well : wellsToReplace) {
            String key = keys.toArray(new String[keys.size()])[well];
            Map.Entry entry = entries.toArray(new Map.Entry[entries.size()])[well];

            mapBarcodeToTube.remove(key);
            mapBarcodeToTube.put((String) entry.getKey(), (BarcodedTube) entry.getValue());
        }

        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                                              new HashSet<LabVessel>(mapBarcodeToTube.values()),
                                              LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);

        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        Map<String, BarcodedTube> mapBarcodeToDaughterTube = new HashMap<>();
        for (BarcodedTube barcodedTube : daughterTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(barcodedTube.getLabel(), barcodedTube);
        }

        // Now run the pico process to make sure that routing works.
        runPicoPlatingProcess(mapBarcodeToDaughterTube,
                              String.valueOf(runDate.getTime()), "1", true);
//        Controller.stopCPURecording();
    }

    /**
     * Build object graph for Fluidigm messages
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testFluidigm() {
        expectedRouting = SystemRouter.System.SQUID;

        // starting rack
        Map<String, BarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();
        for (int rackPosition = 1; rackPosition <= SBSSection.P96COLS1_6BYROW.getWells().size(); rackPosition++) {
            String barcode = "R" + rackPosition;

            String bspStock = "SM-" + rackPosition;
            BarcodedTube bspAliquot = new BarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(bspStock, MercurySample.MetadataSource.BSP));
            mapBarcodeToTube.put(barcode, bspAliquot);
        }

        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);
        LabEventFactory labEventFactory = new LabEventFactory(null, null);
        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);

        LabBatchEjb labBatchEJB = new LabBatchEjb();
        labBatchEJB.setJiraService(JiraServiceProducer.stubInstance());

        LabVesselDao tubeDao = EasyMock.createNiceMock(LabVesselDao.class);
        labBatchEJB.setTubeDao(tubeDao);

        JiraTicketDao mockJira = EasyMock.createNiceMock(JiraTicketDao.class);
        labBatchEJB.setJiraTicketDao(mockJira);

        LabBatchDao labBatchDao = EasyMock.createNiceMock(LabBatchDao.class);
        labBatchEJB.setLabBatchDao(labBatchDao);

        ProductOrderDao mockProductOrderDao = Mockito.mock(ProductOrderDao.class);
        Mockito.when(mockProductOrderDao.findByBusinessKey(Mockito.anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {

                Object[] arguments = invocationOnMock.getArguments();

                return ProductOrderTestFactory.createDummyProductOrder((String) arguments[0]);
            }
        });
        labBatchEJB.setProductOrderDao(mockProductOrderDao);

        BucketDao mockBucketDao = EasyMock.createNiceMock(BucketDao.class);

        EasyMock.replay(mockBucketDao, tubeDao, mockJira, labBatchDao);

        LabEventHandler labEventHandler = getLabEventHandler();
        StaticPlate indexPlate = buildIndexPlate(null, null,
                                                 Collections.singletonList(
                                                         MolecularIndexingScheme.IndexPosition.ILLUMINA_P7),
                                                 Collections.singletonList("IndexPlate")).get(0);
        FluidigmMessagesBuilder fluidigmMessagesBuilder = new FluidigmMessagesBuilder("", bettaLimsMessageTestFactory,
                                                                                      labEventFactory, labEventHandler,
                                                                                      mapBarcodeToTube, indexPlate);
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
            MatcherAssert.assertThat(labEventNames, Matchers.hasItem(Matchers.startsWith(expectedEventName + " ")));
        }

        Assert.assertEquals(labEventNames.size(), expectedEventNames.length, "Wrong number of transfers");

        for (int i = 0; i < expectedEventNames.length; i++) {
            MatcherAssert.assertThat("Unexpected event at position " + i, labEventNames.get(i),
                                     Matchers.startsWith(expectedEventNames[i]));
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
        private final Map<String, BarcodedTube> mapBarcodeToTube;
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
                                        Map<String, BarcodedTube> mapBarcodeToTube, StaticPlate indexPlate) {
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
                    StaticPlate.PlateType.Fluidigm48_48AccessArrayIFC.getAutomationName());
            fluidigmSampleInputJaxb.getPlate().setSection(SBSSection.P384COLS4_6BYROW.getSectionName());
            bettaLimsMessageTestFactory.addMessage(messageList, fluidigmSampleInputJaxb);

            // FluidigmIndexedAdapterInput plate P96COLS1-6BYROW to chip P384COLS4-6BYROW
            fluidigmIndexedAdapterInputJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                    "FluidigmIndexedAdapterInput", indexPlate.getLabel(), chipBarcode);
            fluidigmIndexedAdapterInputJaxb.getSourcePlate().setPhysType(
                    StaticPlate.PlateType.IndexedAdapterPlate96.getAutomationName());
            fluidigmIndexedAdapterInputJaxb.getSourcePlate().setSection(SBSSection.P96COLS1_6BYROW.getSectionName());
            fluidigmIndexedAdapterInputJaxb.getPlate().setPhysType(
                    StaticPlate.PlateType.Fluidigm48_48AccessArrayIFC.getAutomationName());
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
                                                                                        chipBarcode, harvestRackBarcode,
                                                                                        harvestTubeBarcodes);
            fluidigmHarvestingToRackJaxb.getSourcePlate().setPhysType(
                    StaticPlate.PlateType.Fluidigm48_48AccessArrayIFC.getAutomationName());
            fluidigmHarvestingToRackJaxb.getSourcePlate().setSection(SBSSection.P384COLS4_6BYROW.getSectionName());

            fluidigmHarvestingToRackJaxb.setPositionMap(buildFluidigmPositionMap(tubeBarcodes,
                                                                                 fluidigmHarvestingToRackJaxb.getPlate()
                                                                                                             .getBarcode()));
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

    /**
     * Builds plates of molecular indexes for the given index positions.  If there are multiple plates, e.g. P5 and P7,
     * a merged P5/P7 scheme is also created, so {@link SampleInstance#addReagent(Reagent)} can find it.
     *
     * @param molecularIndexingSchemeDao DAO, nullable if in database free test
     * @param molecularIndexDao          DAO, nullable if in database free test
     * @param indexPositions             list of positions, e.g. P5, P7
     * @param indexPlateBarcodes         list of barcodes for plates to create
     */
    public static List<StaticPlate> buildIndexPlate(
            @Nullable MolecularIndexingSchemeDao molecularIndexingSchemeDao,
            @Nullable MolecularIndexDao molecularIndexDao,
            List<MolecularIndexingScheme.IndexPosition> indexPositions,
            List<String> indexPlateBarcodes) {

        char[] bases = {'A', 'C', 'T', 'G'};

        List<StaticPlate> indexPlates = new ArrayList<>();
        int arrayIndex = 0;
        for (MolecularIndexingScheme.IndexPosition indexPosition : indexPositions) {
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

                // Re-use existing sequence, if any
                String sequence = stringBuilder.toString();
                MolecularIndex molecularIndex = null;
                if (molecularIndexDao != null) {
                    molecularIndex = molecularIndexDao.findBySequence(sequence);
                }
                if (molecularIndex == null) {
                    molecularIndex = new MolecularIndex(sequence);
                }

                // Re-use existing scheme, if any
                MolecularIndexingScheme testScheme = null;
                if (molecularIndexingSchemeDao != null) {
                    testScheme = molecularIndexingSchemeDao.findSingleIndexScheme(indexPosition, sequence);
                }
                if (testScheme == null) {
                    EnumMap<MolecularIndexingScheme.IndexPosition, MolecularIndex> mapPositionToIndex =
                            new EnumMap<>(MolecularIndexingScheme.IndexPosition.class);
                    mapPositionToIndex.put(indexPosition, molecularIndex);
                    testScheme = new MolecularIndexingScheme(
                            mapPositionToIndex);
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
                                                                                                           getReagentContents()
                                                                                                   .iterator().next())
                            .getMolecularIndexingScheme();
                    mapPositionToIndex.putAll(molecularIndexingScheme.getIndexes());
                }
                new MolecularIndexingScheme(mapPositionToIndex);
            }
        }
        return indexPlates;
    }

    public static BarcodedTube buildBaitTube(String tubeBarcode, ReagentDesign reagent) {
        BarcodedTube baitTube = new BarcodedTube(tubeBarcode);
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
