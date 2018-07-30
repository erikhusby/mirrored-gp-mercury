package org.broadinstitute.gpinformatics.mercury.test;

//import com.jprofiler.api.agent.Controller;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.Factory;
import org.apache.commons.collections4.map.LazySortedMap;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceTestProducer;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ChildVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
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
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UMIReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UniqueMolecularIdentifier;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ArrayPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.CrspPicoEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.CrspRiboPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.FPEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq4000FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.IceEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.InfiniumEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.InfiniumJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionCellFreeUMIEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PreFlightEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.SageEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.SelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.SingleCell10XEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.SingleCellSmartSeqEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.StoolTNAEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.TruSeqStrandSpecificEntityBuilder;
import org.easymock.EasyMock;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
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
    public static class ListTransfersFromStart extends TransferTraverserCriteria {
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
            if( context.getHopCount() > 0 ) {
                // Traversal starting vessel has no event
                LabEvent contextEvent = context.getVesselEvent().getLabEvent();
                labEventNamesByHopCount.get(context.getHopCount()).add(contextEvent);

                if (!getVisitedLabEvents().add(contextEvent)) {
                    return TraversalControl.StopTraversing;
                }
                if (context.getHopCount() > hopCount) {
                    hopCount = context.getHopCount();
                    labEventNames.add(makeLabEventName(contextEvent));
                }
            }
            return TraversalControl.ContinueTraversing;
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
        new BettaLimsMessageResource(new WorkflowLoader().load());
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

        TubeFormation daughterTubeFormation = daughterPlateTransfer(mapBarcodeToTube, workflowBatch);

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

        LabVessel denatureSource =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        LabBatch fctBatch = new LabBatch(FCT_TICKET, Collections.singleton(denatureSource), LabBatch.LabBatchType.FCT);
        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1", FCT_TICKET,
                                            ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL, "Squid Designation",
                                            Workflow.HYBRID_SELECTION);

        IlluminaFlowcell illuminaFlowcell = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell();
        Set<SampleInstanceV2> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPositionV2(
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

        ZimsIlluminaRunFactory zimsIlluminaRunFactory = constructZimsIlluminaRunFactory(productOrder,
                Collections.<FlowcellDesignation>emptyList());
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

        TubeFormation daughterTubeFormation = daughterPlateTransfer(mapBarcodeToTube, workflowBatch);

        Map<String, BarcodedTube> mapBarcodeToDaughterTube = new HashMap<>();
        for (BarcodedTube barcodedTube : daughterTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(barcodedTube.getLabel(), barcodedTube);
        }

        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToDaughterTube,
                String.valueOf(runDate.getTime()), "1", true);
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder = runExomeExpressShearingProcess(
                picoPlatingEntityBuilder.getNormBarcodeToTubeMap(), picoPlatingEntityBuilder.getNormTubeFormation(),
                picoPlatingEntityBuilder.getNormalizationBarcode(), "1");
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = runLibraryConstructionProcess(
                exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                exomeExpressShearingEntityBuilder.getShearingPlate(), "1");
        HybridSelectionEntityBuilder hybridSelectionEntityBuilder = runHybridSelectionProcess(
                libraryConstructionEntityBuilder.getPondRegRack(),
                libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), "1");
        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                "1");

        final LabVessel denatureSource =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        LabBatch fctBatch = new LabBatch(FCT_TICKET, Collections.singleton(denatureSource), LabBatch.LabBatchType.FCT);

        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", FCT_TICKET,
                                            ProductionFlowcellPath.DILUTION_TO_FLOWCELL, null,
                                            Workflow.AGILENT_EXOME_EXPRESS);

        runTransferVisualizer(mapBarcodeToTube.values().iterator().next());

        IlluminaFlowcell illuminaFlowcell = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell();
        Set<SampleInstanceV2> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPositionV2(
                VesselPosition.LANE1);
        Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(), 2,
                "Wrong number of reagents");
        Set<SampleInstanceV2> lane2SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPositionV2(
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

        ZimsIlluminaRunFactory zimsIlluminaRunFactory = constructZimsIlluminaRunFactory(productOrder,
                Collections.<FlowcellDesignation>emptyList());
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
                String.valueOf(runDate.getTime()), "1", true);
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder = runExomeExpressShearingProcess(
                picoPlatingEntityBuilder.getNormBarcodeToTubeMap(), picoPlatingEntityBuilder.getNormTubeFormation(),
                picoPlatingEntityBuilder.getNormalizationBarcode(), "1");
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = runLibraryConstructionProcess(
                exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                exomeExpressShearingEntityBuilder.getShearingPlate(), "1");
        HybridSelectionEntityBuilder hybridSelectionEntityBuilder = runHybridSelectionProcess(
                libraryConstructionEntityBuilder.getPondRegRack(),
                libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), "1");
        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                "1");
        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder = runHiSeq2500FlowcellProcess(
                qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", "squidDesignationName",
                ProductionFlowcellPath.DENATURE_TO_FLOWCELL, null, Workflow.AGILENT_EXOME_EXPRESS);

        IlluminaFlowcell illuminaFlowcell = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell();
        Set<SampleInstanceV2> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPositionV2(
                VesselPosition.LANE1);
        Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(), 2,
                            "Wrong number of reagents");
        Set<SampleInstanceV2> lane2SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPositionV2(
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

        ZimsIlluminaRunFactory zimsIlluminaRunFactory = constructZimsIlluminaRunFactory(productOrder,
                Collections.<FlowcellDesignation>emptyList());
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
            ZimsIlluminaRunFactory zimsIlluminaRunFactory = constructZimsIlluminaRunFactory(productOrder1,
                    Collections.<FlowcellDesignation>emptyList());

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
            LabVessel denatureSource =
                    qtpEntityBuilder2.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
            LabBatch fctBatch = new LabBatch(FCT_TICKET, Collections.singleton(denatureSource), LabBatch.LabBatchType.FCT);
            HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder2 =
                    runHiSeq2500FlowcellProcess(qtpEntityBuilder2.getDenatureRack(), "2" + "ANXX", FCT_TICKET,
                            ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL, "squidDesignationName",
                            Workflow.ICE_EXOME_EXPRESS);

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
            denatureSource =
                    qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
            fctBatch = new LabBatch(FCT_TICKET, Collections.singleton(denatureSource), LabBatch.LabBatchType.FCT);
            HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                    runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ANXX", FCT_TICKET,
                                                ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL, "squidDesignationName",
                                                Workflow.ICE_EXOME_EXPRESS);

            SimpleDateFormat dateFormat = new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN);
            File runPath = File.createTempFile("tempRun" + dateFormat.format(runDate), ".txt");
            String flowcellBarcode = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell().getCartridgeBarcode();
            String machineName = "Superman";
            IlluminaSequencingRunFactory runFactory =
                    new IlluminaSequencingRunFactory(EasyMock.createMock(JiraCommentUtil.class));
            ZimsIlluminaRunFactory zimsIlluminaRunFactory = constructZimsIlluminaRunFactory(productOrder1,
                    Collections.<FlowcellDesignation>emptyList());

            SolexaRunBean runBean1 = new SolexaRunBean(flowcellBarcode, flowcellBarcode + dateFormat.format(runDate),
                                                       runDate, machineName, runPath.getAbsolutePath(), null);

            IlluminaSequencingRun run1 =
                    runFactory.buildDbFree(runBean1, hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell());

            ZimsIlluminaRun zimsIlluminaRun1 = zimsIlluminaRunFactory.makeZimsIlluminaRun(run1);
            Assert.assertEquals(zimsIlluminaRun1.getLanes().size(), 8, "Wrong number of lanes");
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
            Assert.assertEquals(zimsIlluminaRun2.getLanes().size(), 8, "Wrong number of lanes");

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
     * Make a rack composed of a mix of tubes from two lcsets, and add in a tube from a third lcset.
     * Attempt shearing aliquot on the combined lcsets and verify the ambiguous lcset was detected.
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testAmbiguousLcset() {
        expectedRouting = SystemRouter.System.MERCURY;
        Date runDate = new Date();
        int counter = 1;
        int tubesPerPlate = 2;

        // Makes the first workflow batch.
        ProductOrder productOrder1 = ProductOrderTestFactory.buildHybridSelectionProductOrder(tubesPerPlate, "A");
        Map<String, BarcodedTube> mapBarcodeToTube1 = createInitialRack(productOrder1, "R1_");
        LabBatch workflowBatch1 = new LabBatch("ignored", new HashSet<LabVessel>(mapBarcodeToTube1.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch1.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
        workflowBatch1.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        bucketBatchAndDrain(mapBarcodeToTube1, productOrder1, workflowBatch1, String.valueOf(counter++));
        Assert.assertEquals(mapBarcodeToTube1.values().iterator().next().getBucketEntries().size(), 1);
        PicoPlatingEntityBuilder picoPlatingBuilder1 = runPicoPlatingProcess(mapBarcodeToTube1,
                String.valueOf(runDate.getTime()), String.valueOf(counter++), true);

        // Makes the second workflow batch.
        ProductOrder productOrder2 = ProductOrderTestFactory.buildHybridSelectionProductOrder(tubesPerPlate, "B");
        Map<String, BarcodedTube> mapBarcodeToTube2 = createInitialRack(productOrder2, "R2_");
        LabBatch workflowBatch2 = new LabBatch("ignored", new HashSet<LabVessel>(mapBarcodeToTube2.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch2.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
        workflowBatch2.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        bucketBatchAndDrain(mapBarcodeToTube2, productOrder2, workflowBatch2, String.valueOf(counter++));
        Assert.assertEquals(mapBarcodeToTube2.values().iterator().next().getBucketEntries().size(), 1);
        PicoPlatingEntityBuilder picoPlatingBuilder2 = runPicoPlatingProcess(mapBarcodeToTube2,
                String.valueOf(runDate.getTime()), String.valueOf(counter++), true);

        // Makes the third workflow batch.
        ProductOrder productOrder3 = ProductOrderTestFactory.buildHybridSelectionProductOrder(tubesPerPlate, "C");
        Map<String, BarcodedTube> mapBarcodeToTube3 = createInitialRack(productOrder3, "R3_");
        LabBatch workflowBatch3 = new LabBatch("ignored", new HashSet<LabVessel>(mapBarcodeToTube3.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch3.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
        workflowBatch3.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        bucketBatchAndDrain(mapBarcodeToTube3, productOrder3, workflowBatch3, String.valueOf(counter++));
        final BarcodedTube tube3 = mapBarcodeToTube3.values().iterator().next();
        Assert.assertEquals(tube3.getBucketEntries().size(), 1);
        PicoPlatingEntityBuilder picoPlatingBuilder3 = runPicoPlatingProcess(mapBarcodeToTube3,
                String.valueOf(runDate.getTime()), String.valueOf(counter++), true);
        // Runs the third batch through shearing process.
        ExomeExpressShearingEntityBuilder shearingBuilder3 = runExomeExpressShearingProcess(
                picoPlatingBuilder3.getNormBarcodeToTubeMap(),
                picoPlatingBuilder3.getNormTubeFormation(),
                picoPlatingBuilder3.getNormalizationBarcode(), String.valueOf(counter++));
        for (LabEvent event : shearingBuilder3.getShearingPlate().getEvents()) {
            Assert.assertFalse(event.hasAmbiguousLcsetProblem());
        }

        // Chooses a tube from the third batch, puts it into shearing bucket as rework, and takes it out of
        // the shearing bucket so it can be part of the second batch.
        Bucket shearingBucket = createAndPopulateBucket(
                new HashMap<String, BarcodedTube>() {{
                    put(tube3.getLabel(), tube3);
                }}, productOrder3, "Shearing");
        workflowBatch2.addLabVessel(tube3);
        drainBucket(shearingBucket, workflowBatch2);
        Assert.assertEquals(tube3.getBucketEntries().size(), 2);

        // These are key to make workflow validation work.
        tube3.addReworkLabBatch(workflowBatch2);
        workflowBatch2.addReworks(Collections.singletonList((LabVessel) tube3));
        Assert.assertTrue(tube3.getReworkLabBatches().contains(workflowBatch2));
        Assert.assertTrue(workflowBatch2.getReworks().contains(tube3));

        Assert.assertEquals(tube3.getBucketEntries().size(), 2);
        for (BucketEntry bucketEntry : tube3.getBucketEntries()) {
            if (bucketEntry.getLabBatch().getBatchName().equals(workflowBatch2.getBatchName())) {
                bucketEntry.setReworkDetail(
                        new ReworkDetail(new ReworkReason(ReworkEntry.ReworkReasonEnum.MACHINE_ERROR.getValue()),
                                ReworkLevel.ONE_SAMPLE_RELEASE_REST_BATCH,
                                LabEventType.SHEARING_TRANSFER, "test", null));
            }
        }

        // The first and second batches and the one rework tube are rearrayed into one rack.
        // It should result in the rework tube having an ambiguous LCSET, because the first event
        // after bucketing involves vessels having a mix of LCSETs without a single common one.
        Map<String, BarcodedTube> mapBarcodeToCombined = new LinkedHashMap<>();
        mapBarcodeToCombined.put(tube3.getLabel(), tube3);
        mapBarcodeToCombined.putAll(picoPlatingBuilder1.getNormBarcodeToTubeMap());
        mapBarcodeToCombined.putAll(picoPlatingBuilder2.getNormBarcodeToTubeMap());

        ExomeExpressShearingEntityBuilder builder = runExomeExpressShearingProcess(mapBarcodeToCombined, null,
                picoPlatingBuilder2.getNormalizationBarcode(), String.valueOf(counter++));
        StaticPlate plate = builder.getShearingPlate();

        for (LabVessel tube : mapBarcodeToCombined.values()) {
            tube.clearCaches();
        }
        // The shearing event should have the ambiguous lcset problem.
        boolean foundShearingTransfer = false;
        boolean foundCovarisLoaded = false;
        for (LabEvent event : plate.getEvents()) {
            switch (event.getLabEventType()) {
            case SHEARING_TRANSFER:
                for (LabVessel labVessel : event.getAllLabVessels()) {
                    labVessel.clearCaches();
                }
                Assert.assertTrue(event.hasAmbiguousLcsetProblem());
                foundShearingTransfer = true;
                break;
            case COVARIS_LOADED:
                for (LabVessel labVessel : event.getAllLabVessels()) {
                    labVessel.clearCaches();
                }
                // The lcset is still ambiguous, but this event cannot follow a bucket so no problem with it.
                Assert.assertFalse(event.hasAmbiguousLcsetProblem());
                foundCovarisLoaded = true;
            }
        }
        Assert.assertTrue(foundShearingTransfer, "Failed to find shearing transfer event");
        Assert.assertTrue(foundCovarisLoaded, "Failed to process beyond shearing transfer event");
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

        String lcsetSuffix = "1";
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, lcsetSuffix);

        TubeFormation daughterTubeFormation = daughterPlateTransfer(mapBarcodeToTube, workflowBatch);

        Map<String, BarcodedTube> mapBarcodeToDaughterTube = new HashMap<>();
        for (BarcodedTube barcodedTube : daughterTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(barcodedTube.getLabel(), barcodedTube);
        }

        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToDaughterTube,
                String.valueOf(runDate.getTime()), lcsetSuffix, true);
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                        picoPlatingEntityBuilder.getNormTubeFormation(),
                        picoPlatingEntityBuilder.getNormalizationBarcode(), lcsetSuffix);
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                        exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                        exomeExpressShearingEntityBuilder.getShearingPlate(), lcsetSuffix);

        IceEntityBuilder iceEntityBuilder = runIceProcess(
                Collections.singletonList(libraryConstructionEntityBuilder.getPondRegRack()),
                lcsetSuffix);

        // Need a version of QTP that jumps over pooling to normalization
        QtpEntityBuilder qtpEntityBuilder = new QtpEntityBuilder(
                getBettaLimsMessageTestFactory(), getLabEventFactory(), getLabEventHandler(),
                Collections.singletonList(iceEntityBuilder.getCatchEnrichRack()),
                Collections.singletonList(iceEntityBuilder.getCatchEnrichRack().getRacksOfTubes().iterator().next().getLabel()),
                Collections.singletonList(iceEntityBuilder.getCatchEnrichBarcodes()),
                iceEntityBuilder.getMapBarcodeToCatchEnrichTubes(), lcsetSuffix).
                invoke(true, QtpJaxbBuilder.PcrType.ECO_DUPLICATE);

        final LabVessel denatureSource = qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(
                VesselPosition.A01);
        LabBatch fctBatch = new LabBatch(FCT_TICKET, Collections.singleton(denatureSource), LabBatch.LabBatchType.FCT);

        // todo jmt denature rack has 8 source tubes, but 2500 builder is expecting only 1
        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), lcsetSuffix + "ADXX", FCT_TICKET,
                        ProductionFlowcellPath.DILUTION_TO_FLOWCELL, null,
                        Workflow.ICE_EXOME_EXPRESS);

        IlluminaFlowcell illuminaFlowcell = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell();
        runTransferVisualizer(denatureSource);
        Set<SampleInstanceV2> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPositionV2(
                VesselPosition.LANE1);
        Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(), 3,
                "Wrong number of reagents");
        Set<SampleInstanceV2> lane2SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPositionV2(
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

        ZimsIlluminaRunFactory zimsIlluminaRunFactory = constructZimsIlluminaRunFactory(productOrder,
                Collections.<FlowcellDesignation>emptyList());
        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(run);
        Assert.assertEquals(zimsIlluminaRun.getLanes().size(), 2, "Wrong number of lanes");
        Assert.assertEquals(zimsIlluminaRun.getActualReadStructure(), readStructureRequest.getActualReadStructure());
        Assert.assertEquals(zimsIlluminaRun.getSetupReadStructure(), readStructureRequest.getSetupReadStructure());
        Assert.assertEquals(zimsIlluminaRun.getImagedAreaPerLaneMM2(), readStructureRequest.getImagedArea());
        Assert.assertEquals(zimsIlluminaRun.getLanesSequenced(), "3,6");
        Assert.assertEquals(zimsIlluminaRun.getSystemOfRecord(), SystemRouter.System.MERCURY);
        for (ZimsIlluminaChamber zimsIlluminaChamber : zimsIlluminaRun.getLanes()) {
            Assert.assertEquals(zimsIlluminaChamber.getLibraries().size(), NUM_POSITIONS_IN_RACK);
            for (LibraryBean libraryBean : zimsIlluminaChamber.getLibraries()) {
                Assert.assertTrue(libraryBean.getLibrary().startsWith(
                        POND_REGISTRATION_TUBE_PREFIX + lcsetSuffix));
            }
        }


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
                "Ice96PlexSpriConcentration",
                "IcePoolTest",
                "Ice1stHybridization",
                "Ice1stCapture",
                "Ice2ndCapture",
                "IceCatchCleanup",
                "IceCatchEnrichmentCleanup",
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

    @Test
    public void test2LcsetIce() {
        TubeFormation pondRegRack = getTubeFormation("1");
        TubeFormation pondRegRack2 = getTubeFormation("2");

        runIceProcess(Arrays.asList(pondRegRack, pondRegRack2), "1");
        runTransferVisualizer(pondRegRack.getContainerRole().getContainedVessels().iterator().next());
    }

    @Nonnull
    private TubeFormation getTubeFormation(String unique) {
        HashMap<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        BarcodedTube p1 = new BarcodedTube("P1" + unique);
        p1.getMercurySamples().add(new MercurySample("SM-P1" + unique, MercurySample.MetadataSource.MERCURY));
        mapPositionToTube.put(VesselPosition.A01, p1);
        BarcodedTube p2 = new BarcodedTube("P2" + unique);
        p2.getMercurySamples().add(new MercurySample("SM-P2" + unique, MercurySample.MetadataSource.MERCURY));
        mapPositionToTube.put(VesselPosition.A02, p2);
        LabBatch labBatch = new LabBatch("LCSET-" + unique, new HashSet<LabVessel>(mapPositionToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        labBatch.setWorkflowName("ICE CRSP");

        TubeFormation pondRegRack = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        pondRegRack.getRacksOfTubes().add(new RackOfTubes("PondRack" + unique, RackOfTubes.RackType.Matrix96));
        LabEvent pondReg = new LabEvent(LabEventType.POND_REGISTRATION, new Date(), "BATMAN", 1L, 101L, "Bravo");
        pondRegRack.addInPlaceEvent(pondReg);
        return pondRegRack;
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

        Assert.assertEquals(sageEntityBuilder.getSageCleanupRack().getSampleInstancesV2().size(), NUM_POSITIONS_IN_RACK,
                            "Wrong number of sage cleanup samples");

        QtpEntityBuilder qtpEntityBuilder =
                runQtpProcess(sageEntityBuilder.getSageCleanupRack(), sageEntityBuilder.getSageCleanupTubeBarcodes(),
                              sageEntityBuilder.getMapBarcodeToSageUnloadTubes(), "1");

        LabVessel denatureSource =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        LabBatch fctBatch = new LabBatch(FCT_TICKET, Collections.singleton(denatureSource), LabBatch.LabBatchType.FCT);
        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1", FCT_TICKET,
                                            ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL, "Squid Designation",
                                            Workflow.WHOLE_GENOME);

        IlluminaFlowcell illuminaFlowcell = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell();
        Set<SampleInstanceV2> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPositionV2(
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
                targetRack.getContainerRole().getVesselAtPosition(VesselPosition.A01).getSampleInstancesV2().size(),
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
            nameToSampleData.put(poSample.getName(), new BspSampleData(dataMap));

            rackPosition++;
        }

        // Swap wells from rack 1 with wells from rack 2
        List<Integer> wellsToReplace = new ArrayList<>();
        wellsToReplace.add(5);
        wellsToReplace.add(35);
        wellsToReplace.add(88);

        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        LabBatch workflowBatch2 = new LabBatch("Exome Express Batch 2",
                new HashSet<LabVessel>(mapBarcodeToTube2.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch2.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
        workflowBatch2.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);

        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");
        bucketBatchAndDrain(mapBarcodeToTube2, productOrder, workflowBatch2, "1");
        TubeFormation daughterTubeFormation =
                mismatchedDaughterPlateTransfer(mapBarcodeToTube, mapBarcodeToTube2, wellsToReplace, workflowBatch);


        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(mapBarcodeToTube.keySet());
        Set<Map.Entry<String, BarcodedTube>> entries = mapBarcodeToTube2.entrySet();
        for (Integer well : wellsToReplace) {
            String key = keys.toArray(new String[keys.size()])[well];
            Map.Entry entry = entries.toArray(new Map.Entry[entries.size()])[well];

            mapBarcodeToTube.remove(key);
            mapBarcodeToTube.put((String) entry.getKey(), (BarcodedTube) entry.getValue());
        }

        Map<String, BarcodedTube> mapBarcodeToDaughterTube = new HashMap<>();
        for (BarcodedTube barcodedTube : daughterTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(barcodedTube.getLabel(), barcodedTube);
        }

        // Now run the pico process to make sure that routing works.
        runPicoPlatingProcess(mapBarcodeToDaughterTube,
                              String.valueOf(runDate.getTime()), "1", true);
//        Controller.stopCPURecording();
    }


    @Test(groups = TestGroups.DATABASE_FREE)
    public void testCrspPico() {
        expectedRouting = SystemRouter.System.MERCURY;

        // Use Standard Exome product, to verify that workflow is taken from LCSet, not Product
        int numSamples = NUM_POSITIONS_IN_RACK - 2;
        ProductOrder productOrder = ProductOrderTestFactory.buildIceProductOrder(numSamples);
        productOrder.getResearchProject().setRegulatoryDesignation(
                ResearchProject.RegulatoryDesignation.CLINICAL_DIAGNOSTICS);
        Date runDate = new Date();
        // todo jmt create bucket, then batch, rather than rack then batch then bucket
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R",
                MercurySample.MetadataSource.MERCURY);
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setCreatedOn(new Date());
        workflowBatch.setWorkflow(Workflow.ICE_CRSP);

        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        CrspPicoEntityBuilder crspPicoEntityBuilder = new CrspPicoEntityBuilder(getBettaLimsMessageTestFactory(),
                getLabEventFactory(), getLabEventHandler(), "", "CRSP", mapBarcodeToTube).invoke();

        TubeFormation shearingTf = (TubeFormation) crspPicoEntityBuilder.getShearingAliquotEntity().
                getTargetLabVessels().iterator().next();
        Assert.assertEquals(shearingTf.getContainerRole().getSampleInstancesV2().size(), numSamples);

        runTransferVisualizer(mapBarcodeToTube.values().iterator().next());
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testCrspRibo() {
        expectedRouting = SystemRouter.System.MERCURY;

        // Use Standard Exome product, to verify that workflow is taken from LCSet, not Product
        int numSamples = NUM_POSITIONS_IN_RACK - 2;
        ProductOrder productOrder = ProductOrderTestFactory.buildTruSeqStrandSpecificProductOrder(numSamples);
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        LabBatch workflowBatch = new LabBatch("Ribogreen Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setCreatedOn(new Date());
        workflowBatch.setWorkflow(Workflow.TRU_SEQ_STRAND_SPECIFIC_CRSP);

        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        CrspRiboPlatingEntityBuilder crspRiboPlatingBuilder = new CrspRiboPlatingEntityBuilder(getBettaLimsMessageTestFactory(),
                getLabEventFactory(), getLabEventHandler(), mapBarcodeToTube, "Ribo", "").invoke();

        StaticPlate initialRibo = (StaticPlate) crspRiboPlatingBuilder.getInitialRiboTransfer1().
                getTargetLabVessels().iterator().next();
        Assert.assertEquals(initialRibo.getContainerRole().getSampleInstancesV2().size(), numSamples);

        TubeFormation polyAAliquotTF = (TubeFormation) crspRiboPlatingBuilder.getPolyATSAliquot().
                getTargetLabVessels().iterator().next();
        Assert.assertEquals(polyAAliquotTF.getContainerRole().getSampleInstancesV2().size(), numSamples);
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
        labBatchEJB.setJiraService(JiraServiceTestProducer.stubInstance());

        LabVesselDao tubeDao = EasyMock.createNiceMock(LabVesselDao.class);
        labBatchEJB.setTubeDao(tubeDao);

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

        EasyMock.replay(mockBucketDao, tubeDao, labBatchDao);

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

    /**
     * Build object graph for FP messages
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testFP() {
        expectedRouting = SystemRouter.System.MERCURY;
        int numSamples = NUM_POSITIONS_IN_RACK * 4;
        ProductOrder productOrder = ProductOrderTestFactory.buildFPProductOrder(numSamples);
        List<StaticPlate> sourcePlates = buildSamplePlates(productOrder, "FP_PCR1Plate");
        int numSampleInstances = 0;
        for (StaticPlate sourceplate: sourcePlates) {
            int sampleInstances = sourceplate.getSampleInstancesV2().size();
            numSampleInstances += sampleInstances;
        }
        FPEntityBuilder fpEntityBuilder = runFPProcess(sourcePlates, numSampleInstances, "FP");
        LabVessel finalTube =
                fpEntityBuilder.getFinalPoolingRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        Set<SampleInstanceV2> sampleInstancesV2 = finalTube.getSampleInstancesV2();
        Assert.assertEquals(sampleInstancesV2.size(), numSampleInstances, "Wrong number of sample instances");
    }

    /**
     * Build object graph for Single Cell messages
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testSingleCellSmartSeq() {
        expectedRouting = SystemRouter.System.MERCURY;
        int numSamples = NUM_POSITIONS_IN_RACK * 4;
        ProductOrder productOrder = ProductOrderTestFactory.buildSingleCellProductOrder(numSamples);
        List<StaticPlate> sourcePlates = buildSamplePlates(productOrder, "SC_cDNAPlate");
        int numSampleInstances = 0;
        Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();
        List<LabVessel> labVesselList = new ArrayList<>();
        Map<String, PlateWell> mapWellToPlateWell = new HashMap<>();
        for (StaticPlate sourceplate: sourcePlates) {
            int sampleInstances = sourceplate.getSampleInstancesV2().size();
            numSampleInstances += sampleInstances;
            mapBarcodeToPlate.put(sourceplate.getLabel(), sourceplate);
            for (PlateWell plateWell: sourceplate.getContainerRole().getContainedVessels()) {
                labVesselList.add(plateWell);
                mapWellToPlateWell.put(plateWell.getLabel(), plateWell);
            }
        }

        LabBatch workflowBatch = new LabBatch("Single Cell SmartSeq Batch",
                new HashSet<>(labVesselList),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.SINGLE_CELL_SMART_SEQ);
        bucketPlateBatchAndDrain(mapWellToPlateWell, productOrder, workflowBatch, "1");

        SingleCellSmartSeqEntityBuilder scEntityBuilder = runSingleCellSmartSeqProcess(sourcePlates, numSampleInstances, "SC");
        LabVessel finalTube =
                scEntityBuilder.getBulkSpriRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        Set<SampleInstanceV2> sampleInstancesV2 = finalTube.getSampleInstancesV2();
        Assert.assertEquals(sampleInstancesV2.size(), numSampleInstances, "Wrong number of sample instances");
    }

    /**
     * Build object graph for Single Cell messages
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testSingleCell10X() {
        expectedRouting = SystemRouter.System.MERCURY;
        int numSamples = NUM_POSITIONS_IN_RACK;
        ProductOrder productOrder = ProductOrderTestFactory.buildSingleCellProductOrder(numSamples);
        List<StaticPlate> sourcePlates = buildSamplePlates(productOrder, "SC_cDNAPlate");
        int numSampleInstances = 0;
        Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();
        List<LabVessel> labVesselList = new ArrayList<>();
        Map<String, PlateWell> mapWellToPlateWell = new HashMap<>();
        for (StaticPlate sourceplate: sourcePlates) {
            int sampleInstances = sourceplate.getSampleInstancesV2().size();
            numSampleInstances += sampleInstances;
            mapBarcodeToPlate.put(sourceplate.getLabel(), sourceplate);
            for (PlateWell plateWell: sourceplate.getContainerRole().getContainedVessels()) {
                labVesselList.add(plateWell);
                mapWellToPlateWell.put(plateWell.getLabel(), plateWell);
            }
        }

        StaticPlate sourcePlate = sourcePlates.get(0);

        LabBatch workflowBatch = new LabBatch("Single Cell 10X Batch",
                new HashSet<>(labVesselList),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.SINGLE_CELL_10X);
        bucketPlateBatchAndDrain(mapWellToPlateWell, productOrder, workflowBatch, "1");

        SingleCell10XEntityBuilder entityBuilder = runSingleCell10XProcess(sourcePlate, numSamples, "SC");
        StaticPlate finalPlate = entityBuilder.getDoubleSidedSpriPlate();
        Set<SampleInstanceV2> sampleInstancesV2 = finalPlate.getSampleInstancesV2();
        Assert.assertEquals(sampleInstancesV2.size(), numSamples, "Wrong number of sample instances");
    }


    /**
     * Build object graph for infinium messages
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testInfinium() {
        expectedRouting = SystemRouter.System.MERCURY;
        int numSamples = NUM_POSITIONS_IN_RACK - 2;
        ProductOrder productOrder = ProductOrderTestFactory.buildInfiniumProductOrder(numSamples);
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("Infinium Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.INFINIUM);
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        TubeFormation daughterTubeFormation = daughterPlateTransfer(mapBarcodeToTube, workflowBatch);

        Map<String, BarcodedTube> mapBarcodeToDaughterTube = new HashMap<>();
        for (BarcodedTube barcodedTube : daughterTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(barcodedTube.getLabel(), barcodedTube);
        }

        ArrayPlatingEntityBuilder arrayPlatingEntityBuilder =
                runArrayPlatingProcess(mapBarcodeToDaughterTube, "Infinium");

        InfiniumEntityBuilder infiniumEntityBuilder = runInfiniumProcess(
                arrayPlatingEntityBuilder.getArrayPlatingPlate(), "Infinium");
        Set<SampleInstanceV2> samples = infiniumEntityBuilder.getHybChips().get(0).getSampleInstancesV2();
        Assert.assertEquals(samples.size(), 24, "Wrong number of sample instances");
    }

    /**
     * Build object graph for infinium messages with metylation.
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testInfiniumMethylation() {
        expectedRouting = SystemRouter.System.MERCURY;
        int numSamples = NUM_POSITIONS_IN_RACK - 2;
        ProductOrder productOrder = ProductOrderTestFactory.buildInfiniumMethylationProductOrder(numSamples);
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("Infinium Methylation Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.INFINIUM_METHYLATION);
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        TubeFormation daughterTubeFormation = daughterPlateTransfer(mapBarcodeToTube, workflowBatch);

        Map<String, BarcodedTube> mapBarcodeToDaughterTube = new HashMap<>();
        for (BarcodedTube barcodedTube : daughterTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(barcodedTube.getLabel(), barcodedTube);
        }

        ArrayPlatingEntityBuilder arrayPlatingEntityBuilder =
                runArrayPlatingProcess(mapBarcodeToDaughterTube, "Infinium");

        InfiniumEntityBuilder infiniumEntityBuilder = runInfiniumProcessWithMethylation(
                arrayPlatingEntityBuilder.getArrayPlatingPlate(), "Infinium",
                InfiniumJaxbBuilder.IncludeMethylation.TRUE);
        Set<SampleInstanceV2> samples = infiniumEntityBuilder.getHybChips().get(0).getSampleInstancesV2();
        Assert.assertEquals(samples.size(), 24, "Wrong number of sample instances");
    }

    /**
     * Build object graph for stool extraction to TNA messages
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testStoolExtractionToTNA() {
        expectedRouting = SystemRouter.System.MERCURY;
        SimpleDateFormat sdf = new SimpleDateFormat("MMddHHmmss");
        String suffix = sdf.format(new Date());

        ArrayList<ChildVesselBean> childVesselBeans = new ArrayList<>();
        childVesselBeans.add(new ChildVesselBean(null, "SM-1234" + suffix, "Well [200uL]", "A01"));
        childVesselBeans.add(new ChildVesselBean(null, "SM-2345" + suffix, "Well [200uL]", "A02"));
        ParentVesselBean parentVesselBean =
                new ParentVesselBean("P1234" + suffix, null, "Plate96Well200PCR", childVesselBeans);

        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        LabVesselFactory labVesselFactory = new LabVesselFactory();
        labVesselFactory.setBspUserList(testUserList);

        String batchId = "BP-" + suffix;
        List<LabVessel> startingVessels = labVesselFactory.buildLabVesselDaoFree(
                new HashMap<String, LabVessel>(), new HashMap<String, MercurySample>(),
                new HashMap<String, Set<ProductOrderSample>>(), "jowalsh",
                new Date(), Arrays.asList(parentVesselBean),
                null, MercurySample.MetadataSource.BSP).getLeft();
        Set<LabVessel> labVesselSet = new HashSet<>(startingVessels);
        LabBatch labBatch = new LabBatch(batchId, labVesselSet,
                        LabBatch.LabBatchType.BSP);

        StaticPlate sourcePlate = (StaticPlate) labBatch.getStartingBatchLabVessels().iterator().next();

        int numSamples = childVesselBeans.size();
        StoolTNAEntityBuilder stoolTNAEntityBuilder = runStoolExtractionToTNAProcess(
                sourcePlate, numSamples, "StoolXTR");
        Set<SampleInstanceV2> dnaSamples = stoolTNAEntityBuilder.getStoolDNATubeRack().getSampleInstancesV2();
        Set<SampleInstanceV2> rnaSamples = stoolTNAEntityBuilder.getStoolRNATubeRack().getSampleInstancesV2();
        Assert.assertEquals(dnaSamples.size(), numSamples, "Wrong number of sample instances");
        Assert.assertEquals(rnaSamples.size(), numSamples, "Wrong number of sample instances");
    }

    /**
     * Build object graph for Array Plating messages
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testArrayPlating() {
        expectedRouting = SystemRouter.System.MERCURY;
        int numSamples = NUM_POSITIONS_IN_RACK - 2;
        ProductOrder productOrder = ProductOrderTestFactory.buildArrayPlatingProductOrder(numSamples);
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("Array Plating Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.NONE);
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        TubeFormation daughterTubeFormation = daughterPlateTransfer(mapBarcodeToTube, workflowBatch);

        Map<String, BarcodedTube> mapBarcodeToDaughterTube = new HashMap<>();
        for (BarcodedTube barcodedTube : daughterTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(barcodedTube.getLabel(), barcodedTube);
        }

        runArrayPlatingProcess(mapBarcodeToDaughterTube, "Infinium");
    }

    /**
     * Build object graph for 10X messages
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testTenX() {
        expectedRouting = SystemRouter.System.MERCURY;
        int numSamples = NUM_POSITIONS_IN_RACK - 2;
        ProductOrder productOrder = ProductOrderTestFactory.buildArrayPlatingProductOrder(numSamples);
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("10X Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.TEN_X);
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        TubeFormation daughterTubeFormation = daughterPlateTransfer(mapBarcodeToTube, workflowBatch);

        Map<String, BarcodedTube> mapBarcodeToDaughterTube = new HashMap<>();
        for (BarcodedTube barcodedTube : daughterTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(barcodedTube.getLabel(), barcodedTube);
        }
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                String.valueOf(
                        LabEventTest.NUM_POSITIONS_IN_RACK),
                "1", true);

        runTenXProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(), "10X");
    }

    /**
     * Build object graph for TruSeq SS messages, verify chain of events.
     */
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testTruSeqStrandSpecific() {
        expectedRouting = SystemRouter.System.MERCURY;
        int numSamples = NUM_POSITIONS_IN_RACK - 2;
        ProductOrder productOrder = ProductOrderTestFactory.buildTruSeqStrandSpecificProductOrder(numSamples);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("TruSeq Strand Specific Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.TRU_SEQ_STRAND_SPECIFIC_CRSP);
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        CrspRiboPlatingEntityBuilder crspRiboPlatingBuilder = runRiboPlatingProcess(getBettaLimsMessageTestFactory(),
                getLabEventFactory(), getLabEventHandler(), mapBarcodeToTube, "Ribo", "");

        TruSeqStrandSpecificEntityBuilder truSeqStrandSpecificEntityBuilder =
                runTruSeqStrandSpecificProcess(crspRiboPlatingBuilder.getPolyAAliquotBarcodedTubeMap(),
                        crspRiboPlatingBuilder.getPolyAAliquotTubeFormation(),
                        crspRiboPlatingBuilder.getPolyAAliquotRackBarcode(),
                        "TruSeqStrandSpecific");

        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(truSeqStrandSpecificEntityBuilder.getEnrichmentCleanupRack(),
                truSeqStrandSpecificEntityBuilder.getEnrichmentCleanupBarcodes(),
                truSeqStrandSpecificEntityBuilder.getMapBarcodeToEnrichmentCleanupTubes(),
                "1",
                QtpJaxbBuilder.PcrType.ECO_TRIPLICATE);

        LabVessel denatureSource =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        LabBatch fctBatch = new LabBatch(FCT_TICKET, Collections.singleton(denatureSource), LabBatch.LabBatchType.FCT);
        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1", FCT_TICKET,
                        ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL, "Squid Designation",
                        Workflow.TRU_SEQ_STRAND_SPECIFIC_CRSP);

        IlluminaFlowcell illuminaFlowcell = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell();
        Set<SampleInstanceV2> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPositionV2(
                VesselPosition.LANE1);
        Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(), 1,
                "Wrong number of reagents");


        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        BarcodedTube startingTube = crspRiboPlatingBuilder.getPolyAAliquotBarcodedTubeMap().entrySet().iterator().next().getValue();

        startingTube.evaluateCriteria(transferTraverserCriteria,
                TransferTraverserCriteria.TraversalDirection.Descendants);
        List<String> labEventNames = transferTraverserCriteria.getAllEventNamesPerHop();
        String[] expectedEventNames = {
                "PolyATransfer",
                "PolyASelectionTS",
                "SecondStrandCleanupTS",
                "AdapterLigationCleanupTS",
                "EnrichmentCleanupTS",
                "PoolingTransfer",
                "EcoTransfer",
                "NormalizationTransfer",
                "DenatureTransfer",
                "StripTubeBTransfer",
                "FlowcellTransfer",
        };
        verifyEventSequence(labEventNames, expectedEventNames);
    }

    public void testCellFreeHyperPrep() {
//        Controller.startCPURecording(true);
        expectedRouting = SystemRouter.System.MERCURY;

        ProductOrder productOrder = ProductOrderTestFactory.buildCellFreeHyperPrepProductOrder(NUM_POSITIONS_IN_RACK);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");

        String[] expectedEventNames = {
                "SamplesDaughterPlateCreation",
                "SamplesNormalizationTransfer",
                "PicoPlatingPostNorm",
                "ShearingTransfer",
                "PostShearingTransferCleanup",
                "ShearingQC",
                "AdapterLigationCleanup",
                "CFDnaPondRegistration",
                "PoolingTransfer",
                "EcoTransfer",
                "NormalizationTransfer",
                "DenatureTransfer",
                "StripTubeBTransfer",
                "FlowcellTransfer",
        };
        testGenomeWorkflow(productOrder, LibraryConstructionJaxbBuilder.PondType.CELL_FREE, expectedEventNames,
                Workflow.CELL_FREE_HYPER_PREP);
    }

    public void testPcrPlusHyperPrep() {
        expectedRouting = SystemRouter.System.MERCURY;

        ProductOrder productOrder = ProductOrderTestFactory.buildPcrPlusHyperPrepProductOrder(NUM_POSITIONS_IN_RACK);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");
        String[] expectedEventNames = {
                "SamplesDaughterPlateCreation",
                "SamplesNormalizationTransfer",
                "PicoPlatingPostNorm",
                "ShearingTransfer",
                "PostShearingTransferCleanup",
                "ShearingQC",
                "AdapterLigationCleanup",
                "PCRPlusPondRegistration",
                "PoolingTransfer",
                "EcoTransfer",
                "NormalizationTransfer",
                "DenatureTransfer",
                "StripTubeBTransfer",
                "FlowcellTransfer",
        };
        testGenomeWorkflow(productOrder, LibraryConstructionJaxbBuilder.PondType.PCR_PLUS_HYPER_PREP, expectedEventNames,
                Workflow.PCR_PLUS_HYPER_PREP);
    }

    public void testPcrFreeHyperPrep() {
//        Controller.startCPURecording(true);
        expectedRouting = SystemRouter.System.MERCURY;

        ProductOrder productOrder = ProductOrderTestFactory.buildPcrFreeHyperPrepProductOrder(NUM_POSITIONS_IN_RACK);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");

        String[] expectedEventNames = {
                "SamplesDaughterPlateCreation",
                "SamplesNormalizationTransfer",
                "PicoPlatingPostNorm",
                "ShearingTransfer",
                "PostShearingTransferCleanup",
                "ShearingQC",
                "AdapterLigationCleanup",
                "PCRFreePondRegistration",
                "PoolingTransfer",
                "EcoTransfer",
                "NormalizationTransfer",
                "DenatureTransfer",
                "StripTubeBTransfer",
                "FlowcellTransfer",
        };
        testGenomeWorkflow(productOrder, LibraryConstructionJaxbBuilder.PondType.PCR_FREE_HYPER_PREP, expectedEventNames,
                Workflow.PCR_FREE_HYPER_PREP);
    }

    public void testICEHyperPrep() {
//        Controller.startCPURecording(true);
        expectedRouting = SystemRouter.System.MERCURY;

        ProductOrder productOrder = ProductOrderTestFactory.buildICEHyperPrepProductOrder(NUM_POSITIONS_IN_RACK);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");

        String[] expectedEventNames = {
                "PicoTransfer",
                "PicoTransfer",
                "FingerprintingAliquot",
                "PicoTransfer",
                "PicoTransfer",
                "FingerprintingPlateSetup",
                "ShearingAliquot",
                "PicoTransfer",
                "PicoTransfer",
                "ShearingTransfer",
                "PostShearingTransferCleanup",
                "ShearingQC",
                "AdapterLigationCleanup",
                "PCRPlusPondRegistration",
                "IcePoolingTransfer",
                "Ice96PlexSpriConcentration",
                "IcePoolTest",
                "IceHyperPrep1stHybridization",
                "Ice1stCapture",
                "Ice2ndCapture",
                "IceCatchCleanup",
                "IceCatchEnrichmentCleanup",
                "PoolingTransfer",
                "EcoTransfer",
                "NormalizationTransfer",
                "DenatureTransfer",
                "StripTubeBTransfer",
                "FlowcellTransfer",
        };
        testGenomeWorkflow(productOrder, LibraryConstructionJaxbBuilder.PondType.PCR_PLUS_HYPER_PREP, expectedEventNames,
                Workflow.ICE_EXOME_EXPRESS_HYPER_PREP);
    }

    public void testPcrFree() {
//        Controller.startCPURecording(true);
        expectedRouting = SystemRouter.System.MERCURY;

        ProductOrder productOrder = ProductOrderTestFactory.buildPcrFreeProductOrder(NUM_POSITIONS_IN_RACK);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");

        String[] expectedEventNames = {
                "SamplesDaughterPlateCreation",
                "SamplesNormalizationTransfer",
                "PicoPlatingPostNorm",
                "ShearingTransfer",
                "PostShearingTransferCleanup",
                "ShearingQC",
                "AdapterLigationCleanup",
                "HybSelPondEnrichmentCleanup",
                "PCRFreePondRegistration",
                "PoolingTransfer",
                "EcoTransfer",
                "NormalizationTransfer",
                "DenatureTransfer",
                "StripTubeBTransfer",
                "FlowcellTransfer",
        };
        testGenomeWorkflow(productOrder, LibraryConstructionJaxbBuilder.PondType.PCR_FREE, expectedEventNames,
                Workflow.PCR_FREE);
//        Controller.stopCPURecording();
    }

    public void testPcrPlus() {
//        Controller.startCPURecording(true);
        expectedRouting = SystemRouter.System.MERCURY;

        ProductOrder productOrder = ProductOrderTestFactory.buildPcrPlusProductOrder(NUM_POSITIONS_IN_RACK);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");

        String[] expectedEventNames = {
                "SamplesDaughterPlateCreation",
                "SamplesNormalizationTransfer",
                "PicoPlatingPostNorm",
                "ShearingTransfer",
                "PostShearingTransferCleanup",
                "ShearingQC",
                "AdapterLigationCleanup",
                "HybSelPondEnrichmentCleanup",
                "PCRPlusPondRegistration",
                "PCRPlusPondNormalization",
                "PoolingTransfer",
                "EcoTransfer",
                "NormalizationTransfer",
                "DenatureTransfer",
                "StripTubeBTransfer",
                "FlowcellTransfer",
        };
        testGenomeWorkflow(productOrder, LibraryConstructionJaxbBuilder.PondType.PCR_PLUS, expectedEventNames,
                Workflow.PCR_PLUS);
//        Controller.stopCPURecording();
    }

    public void testPcrPlusUMI() {
        expectedRouting = SystemRouter.System.MERCURY;

        ProductOrder productOrder = ProductOrderTestFactory.buildPcrPlusProductOrder(NUM_POSITIONS_IN_RACK);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");
        productOrder.setAnalyzeUmiOverride(true);

        // Grab denature tube from one UMI LC set and build another without
        Pair<Map<String, BarcodedTube>, QtpEntityBuilder> pairUmi =
                testUpToBooking(productOrder, LibraryConstructionJaxbBuilder.PondType.PCR_PLUS,
                        Workflow.CELL_FREE_HYPER_PREP_UMIS, "1_UMI", LibraryConstructionEntityBuilder.Indexing.DUAL,
                        LibraryConstructionEntityBuilder.Umi.SINGLE);

        Pair<Map<String, BarcodedTube>, QtpEntityBuilder> pair =
                testUpToBooking(productOrder, LibraryConstructionJaxbBuilder.PondType.PCR_PLUS,
                        Workflow.PCR_PLUS, "1", LibraryConstructionEntityBuilder.Indexing.DUAL,
                        LibraryConstructionEntityBuilder.Umi.NONE);

        QtpEntityBuilder qtpEntityBuilderUMI = pairUmi.getRight();
        QtpEntityBuilder qtpEntityBuilder = pair.getRight();

        BarcodedTube denatureTubeUMI =
                qtpEntityBuilderUMI.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);

        BarcodedTube denatureTube =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);

        Assert.assertEquals(denatureTubeUMI.getSampleInstancesV2().iterator().next().getReagents().size(), 2,
                "Wrong number of reagents");

        Assert.assertEquals(denatureTube.getSampleInstancesV2().iterator().next().getReagents().size(), 1,
                "Wrong number of reagents");

        // Put both together on a 4000 flowcell, 4 lanes each
        VesselPosition[] hiseq4000VesselPositions =
                IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell.getVesselGeometry().getVesselPositions();
        List<VesselPosition> vesselPositionList = Arrays.asList(hiseq4000VesselPositions);
        List<List<VesselPosition>> partition = Lists.partition(vesselPositionList, hiseq4000VesselPositions.length / 2);
        List<VesselPosition> vesselPositions1 = partition.get(0);
        List<VesselPosition> vesselPositions2 = partition.get(1);

        LabBatch.VesselToLanesInfo vesselToLanesInfoUMI = new LabBatch.VesselToLanesInfo(
                vesselPositions1, new BigDecimal("16.22"), denatureTubeUMI, null,
                productOrder.getProduct().getProductName(), Collections.<FlowcellDesignation>emptyList());

        LabBatch.VesselToLanesInfo vesselToLanesInfo = new LabBatch.VesselToLanesInfo(
                vesselPositions2, new BigDecimal("12.33"), denatureTube, null,
                productOrder.getProduct().getProductName(), Collections.<FlowcellDesignation>emptyList());
        List<LabBatch.VesselToLanesInfo> vesselToLanesInfos = Arrays.asList(vesselToLanesInfoUMI, vesselToLanesInfo);

        LabBatch fctBatchHiSeq4000 = new LabBatch("FCT-543", vesselToLanesInfos,
                LabBatch.LabBatchType.FCT, IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);

        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        mapPositionToTube.put(VesselPosition.A01, denatureTubeUMI);
        mapPositionToTube.put(VesselPosition.A02, denatureTube);
        TubeFormation rearrayedDenatureRack = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        rearrayedDenatureRack.addRackOfTubes(new RackOfTubes("denatureRearray", RackOfTubes.RackType.Matrix96));
        HiSeq4000FlowcellEntityBuilder flowcell4000EntityBuilder =
                runHiSeq4000FlowcellProcess(rearrayedDenatureRack, null, "UMIMix" + "ADXX",
                        fctBatchHiSeq4000, "UMI Mix Desig", HiSeq4000FlowcellEntityBuilder.FCTCreationPoint.DENATURE);

        IlluminaFlowcell illuminaFlowcell = flowcell4000EntityBuilder.getIlluminaFlowcell();

        for (VesselPosition vesselPosition: illuminaFlowcell.getVesselGeometry().getVesselPositions()) {
            Set<SampleInstanceV2> sampleInstancesAtPositionV2 =
                    illuminaFlowcell.getContainerRole().getSampleInstancesAtPositionV2(vesselPosition);
            SampleInstanceV2 sampleInstance = sampleInstancesAtPositionV2.iterator().next();
            System.out.println(sampleInstance);
        }

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
        readStructureRequest.setLanesSequenced("1,2,3,4,5,6,7,8");

        illuminaSequencingRunFactory.storeReadsStructureDBFree(readStructureRequest, illuminaSequencingRun);

        ZimsIlluminaRunFactory zimsIlluminaRunFactory = constructZimsIlluminaRunFactory(productOrder,
                Collections.<FlowcellDesignation>emptyList());

        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(illuminaSequencingRun);
        for (ZimsIlluminaChamber zimsIlluminaChamber : zimsIlluminaRun.getLanes()) {
            if (zimsIlluminaChamber.getSequencedLibrary().equals(denatureTube.getLabel())) {
                Assert.assertEquals(zimsIlluminaChamber.getSetupReadStructure(), "76T8B8B76T");
            } else if (zimsIlluminaChamber.getSequencedLibrary().equals(denatureTubeUMI.getLabel())) {
                Assert.assertEquals(zimsIlluminaChamber.getSetupReadStructure(), "6M3S67T8B8B76T");
            } else {
                Assert.fail("Wrong sequencing library found " + zimsIlluminaChamber.getSequencedLibrary());
            }
        }
        for (LibraryBean libraryBean: zimsIlluminaRun.getLanes().iterator().next().getLibraries()) {
            if (libraryBean != null) {
                if (libraryBean.isNegativeControl() == null  && libraryBean.isPositiveControl() == null) {
                    Assert.assertEquals(libraryBean.isAnalyzeUmis(), true);
                }
            }
        }

        productOrder.setAnalyzeUmiOverride(false);
        zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(illuminaSequencingRun);
        for (LibraryBean libraryBean: zimsIlluminaRun.getLanes().iterator().next().getLibraries()) {
            if (libraryBean != null) {
                if (libraryBean.isNegativeControl() == null  && libraryBean.isPositiveControl() == null) {
                    Assert.assertEquals(libraryBean.isAnalyzeUmis(), false);
                }
            }
        }

    }

    public void testCustomSelectionHyperPrep() {
//        Controller.startCPURecording(true);
        expectedRouting = SystemRouter.System.MERCURY;

        ProductOrder productOrder = ProductOrderTestFactory.buildPcrFreeHyperPrepProductOrder(NUM_POSITIONS_IN_RACK);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");

        String[] expectedEventNames = {
                "PicoTransfer",
                "PicoTransfer",
                "FingerprintingAliquot",
                "PicoTransfer",
                "PicoTransfer",
                "FingerprintingPlateSetup",
                "ShearingAliquot",
                "PicoTransfer",
                "PicoTransfer",
                "ShearingTransfer",
                "PostShearingTransferCleanup",
                "ShearingQC",
                "AdapterLigationCleanup",
                "PCRPlusPondRegistration",
                "SelectionPoolingTransfer",
                "SelectionConcentrationTransfer",
                "SelectionHybSetup",
                "SelectionCatchRegistration",
                "PoolingTransfer",
                "EcoTransfer",
                "NormalizationTransfer",
                "DenatureTransfer",
                "StripTubeBTransfer",
                "FlowcellTransfer",
        };
        testGenomeWorkflow(productOrder, LibraryConstructionJaxbBuilder.PondType.PCR_PLUS_HYPER_PREP, expectedEventNames,
                Workflow.CUSTOM_SELECTION);
    }

    private void testGenomeWorkflow(ProductOrder productOrder, LibraryConstructionJaxbBuilder.PondType pondType,
                                    String[] expectedEventNames, Workflow workflow) {
        testGenomeWorkflow(productOrder, pondType, expectedEventNames, workflow, "1", false);
    }

    private QtpEntityBuilder testGenomeWorkflow(ProductOrder productOrder, LibraryConstructionJaxbBuilder.PondType pondType,
                                                String[] expectedEventNames, Workflow workflow, String barcodeSuffix,
                                                boolean includeUMI) {
        Pair<Map<String, BarcodedTube>, QtpEntityBuilder> pair =
                testUpToBooking(productOrder, pondType, workflow, barcodeSuffix,
                        LibraryConstructionEntityBuilder.Indexing.DUAL, LibraryConstructionEntityBuilder.Umi.NONE);
        QtpEntityBuilder qtpEntityBuilder = pair.getRight();
        Map<String, BarcodedTube> mapBarcodeToTube = pair.getLeft();
        int numSeqReagents = 1;
        if (workflow == Workflow.CUSTOM_SELECTION)
            numSeqReagents = 2;
        if (workflow == Workflow.ICE_EXOME_EXPRESS_HYPER_PREP)
            numSeqReagents = 3;
        if (includeUMI)
            numSeqReagents++;

        LabVessel denatureSource =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        VesselPosition[] hiseq4000VesselPositions =
                IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell.getVesselGeometry().getVesselPositions();
        List<VesselPosition> vesselPositionList = Arrays.asList(hiseq4000VesselPositions);
        LabBatch.VesselToLanesInfo vesselToLanesInfo = new LabBatch.VesselToLanesInfo(
                vesselPositionList, new BigDecimal("16.22"), denatureSource, null,
                productOrder.getProduct().getProductName(), Collections.<FlowcellDesignation>emptyList());
        LabBatch fctBatch = new LabBatch(FCT_TICKET, Collections.singletonList(vesselToLanesInfo),
                LabBatch.LabBatchType.FCT, IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
        HiSeq4000FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder = runHiSeq4000FlowcellProcess(
                qtpEntityBuilder.getDenatureRack(), qtpEntityBuilder.getNormalizationRack(), "1", fctBatch,
                "Squid Designation", HiSeq4000FlowcellEntityBuilder.FCTCreationPoint.DENATURE);

        IlluminaFlowcell illuminaFlowcell = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell();
        Set<SampleInstanceV2> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPositionV2(
                VesselPosition.LANE1);
        Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(), numSeqReagents,
                "Wrong number of reagents");

        Map.Entry<String, BarcodedTube> stringBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        stringBarcodedTubeEntry.getValue().evaluateCriteria(transferTraverserCriteria,
                TransferTraverserCriteria.TraversalDirection.Descendants);
        List<String> labEventNames = transferTraverserCriteria.getAllEventNamesPerHop();
        verifyEventSequence(labEventNames, expectedEventNames);
        return qtpEntityBuilder;
    }

    private Pair<Map<String, BarcodedTube>, QtpEntityBuilder> testUpToBooking(ProductOrder productOrder, LibraryConstructionJaxbBuilder.PondType pondType,
                                                                              Workflow workflow, String barcodeSuffix,
                                                                              LibraryConstructionEntityBuilder.Indexing indexing,
                                                                              LibraryConstructionEntityBuilder.Umi umi) {
        expectedRouting = SystemRouter.System.MERCURY;

        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("whole Genome Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(workflow);

        String lcsetSuffix = "1";
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, lcsetSuffix);

        TubeFormation platingTubeFormation = null;
        String platingBarcode = null;
        Map<String, BarcodedTube> mapBarcodeToDaughterTube = null;
        Map<String, BarcodedTube> mapBarcodeToPlatingVessel = null;
        if (workflow == Workflow.ICE_EXOME_EXPRESS_HYPER_PREP || workflow == Workflow.CUSTOM_SELECTION) {
            CrspPicoEntityBuilder crspPicoEntityBuilder = new CrspPicoEntityBuilder(getBettaLimsMessageTestFactory(),
                    getLabEventFactory(), getLabEventHandler(), "", "CRSP", mapBarcodeToTube).invoke();
            platingTubeFormation = (TubeFormation) crspPicoEntityBuilder.getShearingAliquotEntity().
                    getTargetLabVessels().iterator().next();
            platingBarcode = platingTubeFormation.getLabCentricName();
            mapBarcodeToPlatingVessel = new HashMap<>();
            for (BarcodedTube barcodedTube : platingTubeFormation.getContainerRole().getContainedVessels()) {
                mapBarcodeToPlatingVessel.put(barcodedTube.getLabel(), barcodedTube);
            }
        } else {
            TubeFormation daughterTubeFormation = daughterPlateTransfer(mapBarcodeToTube, workflowBatch);
            mapBarcodeToDaughterTube = new HashMap<>();
            for (BarcodedTube barcodedTube : daughterTubeFormation.getContainerRole().getContainedVessels()) {
                mapBarcodeToDaughterTube.put(barcodedTube.getLabel(), barcodedTube);
            }
            PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToDaughterTube,
                    "P", lcsetSuffix, true);
            platingTubeFormation = picoPlatingEntityBuilder.getNormTubeFormation();
            platingBarcode = picoPlatingEntityBuilder.getNormalizationBarcode();
            mapBarcodeToPlatingVessel = picoPlatingEntityBuilder.getNormBarcodeToTubeMap();
        }

        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder;
        if (workflow == Workflow.CELL_FREE_HYPER_PREP_UMIS && umi != LibraryConstructionEntityBuilder.Umi.NONE) {
            LibraryConstructionCellFreeUMIEntityBuilder libraryConstructionProcessWithUMI =
                    runLibraryConstructionProcessWithUMI(mapBarcodeToPlatingVessel,
                            platingTubeFormation, umi);
            QtpEntityBuilder qtpEntityBuilder = runQtpProcess(libraryConstructionProcessWithUMI.getPondRegRack(),
                    libraryConstructionProcessWithUMI.getPondRegTubeBarcodes(),
                    libraryConstructionProcessWithUMI.getMapBarcodeToPondRegTubes(), barcodeSuffix);

            return Pair.of(mapBarcodeToDaughterTube, qtpEntityBuilder);
        } else if (umi == LibraryConstructionEntityBuilder.Umi.DUAL) {
            ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                    runExomeExpressShearingProcess(mapBarcodeToPlatingVessel,
                            platingTubeFormation,
                            platingBarcode, lcsetSuffix);

            StaticPlate shearingCleanupPlate = exomeExpressShearingEntityBuilder.getShearingCleanupPlate();
            UniqueMolecularIdentifier umiReagent = new UniqueMolecularIdentifier(
                    UniqueMolecularIdentifier.UMILocation.BEFORE_FIRST_READ, 6L, 3L);
            UniqueMolecularIdentifier umiReagent2 = new UniqueMolecularIdentifier(
                    UniqueMolecularIdentifier.UMILocation.BEFORE_SECOND_READ, 6L, 3L);
            StaticPlate umiPlate = LabEventTest.buildUmiPlate("UMITestPlate0101", umiReagent);
            LabEventTest.attachUMIToPlate(umiReagent2, umiPlate);
            LabEventTestFactory.doSectionTransfer(LabEventType.UMI_ADDITION, umiPlate, shearingCleanupPlate);

            libraryConstructionEntityBuilder = runWgsLibraryConstructionProcessWithUMI(
                    exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                    exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                    exomeExpressShearingEntityBuilder.getShearingPlate(),
                    lcsetSuffix,
                    pondType,
                    indexing, LibraryConstructionEntityBuilder.Umi.DUAL);
        } else {
            ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                    runExomeExpressShearingProcess(mapBarcodeToPlatingVessel, platingTubeFormation, platingBarcode,
                            lcsetSuffix);

            libraryConstructionEntityBuilder = new LibraryConstructionEntityBuilder(
                    getBettaLimsMessageTestFactory(), getLabEventFactory(), getLabEventHandler(),
                    exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                    exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                    exomeExpressShearingEntityBuilder.getShearingPlate(), NUM_POSITIONS_IN_RACK, barcodeSuffix,
                    indexing, pondType).invoke();
        }

        QtpEntityBuilder qtpEntityBuilder = null;

        if (workflow == Workflow.ICE_EXOME_EXPRESS_HYPER_PREP) {
            IceEntityBuilder iceEntityBuilder = runHyperPrepIceProcess(
                    Collections.singletonList(libraryConstructionEntityBuilder.getPondRegRack()),
                    "1");
            qtpEntityBuilder = runQtpProcess(iceEntityBuilder.getCatchEnrichRack(),
                    iceEntityBuilder.getCatchEnrichBarcodes(),
                    iceEntityBuilder.getMapBarcodeToCatchEnrichTubes(), barcodeSuffix);
        } else if (workflow == Workflow.CUSTOM_SELECTION) {
            SelectionEntityBuilder selectionEntityBuilder = runSelectionProcess(
                    Collections.singletonList(libraryConstructionEntityBuilder.getPondRegRack()),
                    "1");
            qtpEntityBuilder = runQtpProcess(selectionEntityBuilder.getCatchRack(),
                    selectionEntityBuilder.getCatchTubeBarcodes(),
                    selectionEntityBuilder.getMapBarcodeToCatchTubes(), barcodeSuffix);
        } else {
            qtpEntityBuilder = runQtpProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                    libraryConstructionEntityBuilder.getPondRegTubeBarcodes(),
                    libraryConstructionEntityBuilder.getMapBarcodeToPondRegTubes(), barcodeSuffix);
        }

        return Pair.of(mapBarcodeToTube, qtpEntityBuilder);
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
            Assert.assertEquals(chip.getSampleInstancesV2().size(), mapBarcodeToTube.size(),
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
            Assert.assertEquals(harvestRack.getSampleInstancesV2().size(), mapBarcodeToTube.size(),
                                "Wrong number of sample instances");
        }
    }

    /**
     * Builds plates of molecular indexes for the given index positions.  If there are multiple plates, e.g. P5 and P7,
     * a merged P5/P7 scheme is also created, so {@link SampleInstanceV2#addReagent(Reagent)} can find it.
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
                while (stringBuilder.length() < 8) {
                    stringBuilder.append(bases[0]);
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
                            getReagentContents().iterator().next()).getMolecularIndexingScheme();
                    mapPositionToIndex.putAll(molecularIndexingScheme.getIndexes());
                }
                new MolecularIndexingScheme(mapPositionToIndex);
            }
        }
        return indexPlates;
    }

    public static StaticPlate buildDualIndexPlate(
            @Nullable MolecularIndexingSchemeDao molecularIndexingSchemeDao,
            @Nullable MolecularIndexDao molecularIndexDao,
            List<MolecularIndexingScheme.IndexPosition> indexPositions,
            String indexPlateBarcode) {

        char[] bases = {'A', 'C', 'T', 'G'};

        StaticPlate indexPlate = new StaticPlate(indexPlateBarcode, StaticPlate.PlateType.IndexedAdapterPlate96);
        for (VesselPosition vesselPosition : SBSSection.ALL96.getWells()) {
            for (MolecularIndexingScheme.IndexPosition indexPosition : indexPositions) {
                PlateWell plateWell = indexPlate.getContainerRole().getVesselAtPosition(vesselPosition);
                if (plateWell == null) {
                    plateWell = new PlateWell(indexPlate, vesselPosition);
                    indexPlate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
                }
                StringBuilder stringBuilder = new StringBuilder();

                // Build a deterministic sequence from the vesselPosition, by indexing into the bases array
                int base4Ordinal = Integer.parseInt(Integer.toString(vesselPosition.ordinal() + 1, 4), 4);
                while (base4Ordinal > 0) {
                    stringBuilder.append(bases[base4Ordinal % 4]);
                    base4Ordinal = base4Ordinal / 4;
                }
                while (stringBuilder.length() < 8) {
                    stringBuilder.append(bases[0]);
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
            }
        }

        for (VesselPosition vesselPosition : indexPlate.getVesselGeometry().getVesselPositions()) {
            Map<MolecularIndexingScheme.IndexPosition, MolecularIndex> mapPositionToIndex =
                    new EnumMap<>(MolecularIndexingScheme.IndexPosition.class);
            PlateWell well = indexPlate.getContainerRole().getVesselAtPosition(vesselPosition);
            MolecularIndexingScheme molecularIndexingScheme = ((MolecularIndexReagent) well.
                    getReagentContents().iterator().next()).getMolecularIndexingScheme();
            mapPositionToIndex.putAll(molecularIndexingScheme.getIndexes());
            new MolecularIndexingScheme(mapPositionToIndex);
        }

        return indexPlate;
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

    public static UniqueMolecularIdentifier createUmi(long length, long spacerLength, UniqueMolecularIdentifier.UMILocation umiLocation) {
        return new UniqueMolecularIdentifier(umiLocation, length, spacerLength);
    }

    public static StaticPlate buildUmiPlate(String plateBarcode, UniqueMolecularIdentifier umiReagent) {
        StaticPlate umiPlate = new StaticPlate(plateBarcode, StaticPlate.PlateType.UniqueMolecularIdentifierPlate96);
        attachUMIToPlate(umiReagent, umiPlate);
        return umiPlate;
    }

    public static BarcodedTube buildUmiTube(String tubeBarcode, UniqueMolecularIdentifier ... umiReagent) {
        BarcodedTube umiTube = new BarcodedTube(tubeBarcode, BarcodedTube.BarcodedTubeType.MatrixTube);
        for (UniqueMolecularIdentifier umi: umiReagent) {
            umiTube.addReagent(new UMIReagent(umi));
        }
        return umiTube;
    }

    /**
     * Treated as a test by default and thusly fails: <br/>
     * org.testng.TestNGException:  <br/>
     * Cannot inject @Test annotated Method [attachUMIToPlate] with [class org.broadinstitute.gpinformatics.mercury.entity.reagent.UniqueMolecularIdentifier, class org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate]. <br/>
     * For more information on native dependency injection please refer to http://testng.org/doc/documentation-main.html#native-dependency-injection
     */
    @Test( enabled = false )
    public static void attachUMIToPlate(UniqueMolecularIdentifier umi, StaticPlate staticPlate) {
        UMIReagent umiReagent = new UMIReagent(umi);
        for (VesselPosition vesselPosition: staticPlate.getVesselGeometry().getVesselPositions()) {
            PlateWell plateWell = null;
            if (staticPlate.getContainerRole().getMapPositionToVessel().get(vesselPosition) == null) {
                plateWell = new PlateWell(staticPlate, vesselPosition);
            } else {
                plateWell = staticPlate.getContainerRole().getMapPositionToVessel().get(vesselPosition);
            }
            plateWell.addReagent(umiReagent);
            staticPlate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
        }
        staticPlate.addReagent(umiReagent);
    }

    public static List<StaticPlate> buildSamplePlates(ProductOrder productOrder, String platePrefix) {
        List<StaticPlate> samplePlates = new ArrayList<>();
        List<ProductOrderSample> samples = productOrder.getSamples();
        for(int i = 0; i < productOrder.getSamples().size(); i++) {
            StaticPlate samplePlate = new StaticPlate(platePrefix + (i / SBSSection.ALL96.getWells().size()),
                    StaticPlate.PlateType.Eppendorf96);
            for(VesselPosition vesselPosition: SBSSection.ALL96.getWells()) {
                if(i >= samples.size())
                    break;
                PlateWell plateWell = new PlateWell(samplePlate, vesselPosition);
                MercurySample mercurySample =
                        new MercurySample(samples.get(i).getSampleKey(), MercurySample.MetadataSource.MERCURY);
                plateWell.addSample(mercurySample);
                samplePlate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
                i++;
            }
            samplePlates.add(samplePlate);
        }
        return samplePlates;
    }
}
