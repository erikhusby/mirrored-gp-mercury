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
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferEntityGrapher;
import org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferVisualizer;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowValidator;
import org.broadinstitute.gpinformatics.mercury.control.zims.ZimsIlluminaRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
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
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.broadinstitute.gpinformatics.mercury.presentation.transfervis.TransferVisualizerFrame;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PreFlightEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.SageEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ShearingEntityBuilder;
import org.easymock.EasyMock;
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

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign.ReagentType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;

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

    private final TemplateEngine templateEngine = new TemplateEngine();


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
                                return new TreeSet<>(LabEvent.byEventDate);
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
    @Test(groups = {DATABASE_FREE})
    public void testHybridSelection() {
        // Controller.startCPURecording(true);

        final ProductOrder productOrder =
                ProductOrderTestFactory.buildHybridSelectionProductOrder(NUM_POSITIONS_IN_RACK);
        AthenaClientServiceStub.addProductOrder(productOrder);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("Hybrid Selection Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflowName("Hybrid Selection");

        PreFlightEntityBuilder preFlightEntityBuilder =
                runPreflightProcess(mapBarcodeToTube, productOrder, workflowBatch, "1");
        ShearingEntityBuilder shearingEntityBuilder =
                runShearingProcess(mapBarcodeToTube, preFlightEntityBuilder.getTubeFormation(),
                        preFlightEntityBuilder.getRackBarcode(), "1");
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(shearingEntityBuilder.getShearingCleanupPlate(),
                        shearingEntityBuilder.getShearCleanPlateBarcode(), shearingEntityBuilder.getShearingPlate(),
                        "1");
        HybridSelectionEntityBuilder hybridSelectionEntityBuilder =
                runHybridSelectionProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                        libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                        libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), "1");
        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), "Hybrid Selection", "1");

        IlluminaFlowcell illuminaFlowcell = qtpEntityBuilder.getIlluminaFlowcell();
        Set<SampleInstance> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                VesselPosition.LANE1);
        Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(), 2,
                "Wrong number of reagents");

        IlluminaSequencingRunFactory illuminaSequencingRunFactory =
                new IlluminaSequencingRunFactory(EasyMock.createNiceMock(JiraCommentUtil.class));
        IlluminaSequencingRun illuminaSequencingRun;
        try {
            illuminaSequencingRun = illuminaSequencingRunFactory.buildDbFree(new SolexaRunBean(
                    qtpEntityBuilder.getIlluminaFlowcell().getCartridgeBarcode(), "Run1", new Date(), "SL-HAL",
                    File.createTempFile("RunDir", ".txt").getAbsolutePath(), null),
                    qtpEntityBuilder.getIlluminaFlowcell());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ReadStructureRequest readStructureRequest = new ReadStructureRequest();
        readStructureRequest.setRunBarcode(illuminaSequencingRun.getRunBarcode());
        readStructureRequest.setSetupReadStructure("71T8B8B71T");
        readStructureRequest.setActualReadStructure("101T8B8B101T");

        illuminaSequencingRunFactory.storeReadsStructureDBFree(readStructureRequest, illuminaSequencingRun);

        ZimsIlluminaRunFactory zimsIlluminaRunFactory = new ZimsIlluminaRunFactory(
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
                            mapSampleIdToDto.put(sampleName, new BSPSampleDTO(dataMap));
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
                        List<Control> controlList = new ArrayList<>();
                        controlList.add(new Control("NA12878", Control.ControlType.POSITIVE));
                        controlList.add(new Control("WATER_CONTROL", Control.ControlType.NEGATIVE));
                        return controlList;
                    }
                }
        );
        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(illuminaSequencingRun);
        Assert.assertEquals(zimsIlluminaRun.getLanes().size(), 8, "Wrong number of lanes");
        Assert.assertEquals(zimsIlluminaRun.getActualReadStructure(), readStructureRequest.getActualReadStructure());
        Assert.assertEquals(zimsIlluminaRun.getSetupReadStructure(), readStructureRequest.getSetupReadStructure());
        ZimsIlluminaChamber zimsIlluminaChamber = zimsIlluminaRun.getLanes().iterator().next();
        Assert.assertEquals(zimsIlluminaChamber.getLibraries().size(), NUM_POSITIONS_IN_RACK,
                "Wrong number of libraries");
        LibraryBean libraryBean = zimsIlluminaChamber.getLibraries().iterator().next();
        // todo jmt need to investigate the ordering of libraries in ZIMS API results, and do more asserts here
        Assert.assertNotNull(libraryBean.getMolecularIndexingScheme().getName(), "No molecular index");
        Assert.assertEquals(libraryBean.getBaitSetName(), HybridSelectionEntityBuilder.BAIT_DESIGN_NAME, "Wrong bait");

        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        TwoDBarcodedTube startingTube = mapBarcodeToTube.entrySet().iterator().next().getValue();
        startingTube.evaluateCriteria(transferTraverserCriteria,
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

        Assert.assertEquals(illuminaSequencingRun.getSampleCartridge(),
                qtpEntityBuilder.getIlluminaFlowcell(), "Wrong flowcell");

        // pick a sample and mark it for rework
        Map.Entry<String, TwoDBarcodedTube> twoDBarcodedTubeForRework = mapBarcodeToTube.entrySet().iterator().next();
        int lastEventIndex = transferTraverserCriteria.getVisitedLabEvents().size();
        LabEvent catchEvent =
                transferTraverserCriteria.getVisitedLabEvents().toArray(new LabEvent[lastEventIndex])[lastEventIndex
                                                                                                      - 1];

        if (false) {
            TransferVisualizerFrame transferVisualizerFrame = new TransferVisualizerFrame();
            transferVisualizerFrame.renderVessel(startingTube);
            try {
                Thread.sleep(500000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        TransferEntityGrapher transferEntityGrapher = new TransferEntityGrapher();
        transferEntityGrapher.setMaxNumVesselsPerRequest(1000);
        Graph graph = new Graph();
        transferEntityGrapher.startWithTube(startingTube, graph, new ArrayList<TransferVisualizer.AlternativeId>());
        Assert.assertEquals(graph.getMapIdToVertex().size(), 1307, "Wrong number of vertices");
//        Controller.stopCPURecording();
    }


    /**
     * Build object graph for Hybrid Selection messages, verify chain of events.
     */
    @Test(groups = {DATABASE_FREE})
    public void testExomeExpress() throws Exception {
//        Controller.startCPURecording(true);
        // Use Standard Exome product, to verify that workflow is taken from LCSet, not Product
        final ProductOrder productOrder = ProductOrderTestFactory.buildHybridSelectionProductOrder(96);
        AthenaClientServiceStub.addProductOrder(productOrder);
        Date runDate = new Date();
        // todo jmt create bucket, then batch, rather than rack than batch then bucket
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflowName("Exome Express");

        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube, productOrder,
                workflowBatch, null, String.valueOf(runDate.getTime()), "1", true);
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(productOrder, picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
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
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1", "squidDesignationName");

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

        File runPath = File.createTempFile("tempRun" + dateFormat.format(runDate), ".txt");
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

        runFactory.storeReadsStructureDBFree(readStructureRequest, run);

        ZimsIlluminaRunFactory zimsIlluminaRunFactory = new ZimsIlluminaRunFactory(
                new BSPSampleDataFetcher() {
                    @Override
                    public Map<String, BSPSampleDTO> fetchSamplesFromBSP(@Nonnull Collection<String> sampleNames) {
                        Map<String, BSPSampleDTO> mapSampleIdToDto = new HashMap<String, BSPSampleDTO>();
                        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>() {{
                            put(BSPSampleSearchColumn.PRIMARY_DISEASE, "Cancer");
                            put(BSPSampleSearchColumn.LSID, "org.broad:SM-1234");
                            put(BSPSampleSearchColumn.MATERIAL_TYPE, "DNA:DNA Genomic");
                            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "4321");
                            put(BSPSampleSearchColumn.SPECIES, "Homo Sapiens");
                            put(BSPSampleSearchColumn.PARTICIPANT_ID, "PT-1234");
                        }};
                        for (String sampleName : sampleNames) {
                            mapSampleIdToDto.put(sampleName, new BSPSampleDTO(dataMap));
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
                        List<Control> controlList = new ArrayList<>();
                        controlList.add(new Control("NA12878", Control.ControlType.POSITIVE));
                        controlList.add(new Control("WATER_CONTROL", Control.ControlType.NEGATIVE));
                        return controlList;
                    }
                }
        );
        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(run);
        Assert.assertEquals(zimsIlluminaRun.getLanes().size(), 2, "Wrong number of lanes");
        Assert.assertEquals(zimsIlluminaRun.getActualReadStructure(), readStructureRequest.getActualReadStructure());
        Assert.assertEquals(zimsIlluminaRun.getSetupReadStructure(), readStructureRequest.getSetupReadStructure());

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

        /*
         *
         * Temporarily disabling this check until after demo.
         */

        IlluminaSequencingRun illuminaSequencingRun
                = (IlluminaSequencingRun) hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell().getSequencingRuns()
                .iterator().next();

        Assert.assertEquals(illuminaSequencingRun.getSampleCartridge(),
                hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell(), "Wrong flowcell");

//        Controller.stopCPURecording();
    }

    /**
     * Build object graph for Whole Genome Shotgun messages, verify chain of events.
     */
    @Test(groups = {DATABASE_FREE})
    public void testWholeGenomeShotgun() {
//        Controller.startCPURecording(true);

        ProductOrder productOrder =
                ProductOrderTestFactory.buildWholeGenomeProductOrder(NUM_POSITIONS_IN_RACK);
        AthenaClientServiceStub.addProductOrder(productOrder);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("whole Genome Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflowName("Whole Genome");

        PreFlightEntityBuilder preFlightEntityBuilder =
                runPreflightProcess(mapBarcodeToTube, productOrder, workflowBatch, "1");
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

        IlluminaFlowcell illuminaFlowcell = qtpEntityBuilder.getIlluminaFlowcell();
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

    @Test(groups = DATABASE_FREE)
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
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.putAll(mapBarcodeToTube);
        PlateCherryPickEvent rackToPlateCherryPickEvent = bettaLimsMessageTestFactory.buildCherryPick(
                "PoolingTransfer", Collections.singletonList("SourceRack"),
                Collections.<List<String>>singletonList(new ArrayList<>(mapBarcodeToTube.keySet())),
                Collections.singletonList("SourcePlate"), Collections.singletonList(Arrays.asList("tube1", "tube2")),
                Arrays.asList(new BettaLimsMessageTestFactory.CherryPick("SourceRack", "A01", "SourcePlate", "A01")));
        rackToPlateCherryPickEvent.getPlate().get(0).setPhysType(LabEventFactory.PHYS_TYPE_EPPENDORF_96);
        LabEvent rackToPlateEntity = labEventFactory.buildFromBettaLims(rackToPlateCherryPickEvent, mapBarcodeToVessel);
        StaticPlate plate = (StaticPlate) rackToPlateEntity.getTargetLabVessels().iterator().next();

        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(plate.getLabel(), plate);
        PlateCherryPickEvent plateToRackCherryPickEvent = bettaLimsMessageTestFactory.buildCherryPick(
                "PoolingTransfer", Collections.singletonList("SourcePlate"), Collections.<List<String>>emptyList(),
                Collections.singletonList("TargetRack"), Collections.singletonList(Arrays.asList("tube1", "tube2")),
                Arrays.asList(new BettaLimsMessageTestFactory.CherryPick("SourcePlate", "A01", "TargetRack", "A01")));
        plateToRackCherryPickEvent.getSourcePlate().get(0).setPhysType(LabEventFactory.PHYS_TYPE_EPPENDORF_96);
        LabEvent plateToRackEntity = labEventFactory.buildFromBettaLims(plateToRackCherryPickEvent, mapBarcodeToVessel);
        LabVessel targetRack = plateToRackEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(targetRack.getContainerRole().getVesselAtPosition(VesselPosition.A01).getSampleInstances().size(),
                1, "Wrong number of sample instances");
    }

    /**
     * Build object graph for Fluidigm messages
     */
    @Test(groups = {DATABASE_FREE})
    public void testFluidigm() {
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
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);

        LabBatchEjb labBatchEJB = new LabBatchEjb();
        labBatchEJB.setAthenaClientService(AthenaClientProducer.stubInstance());
        labBatchEJB.setJiraService(JiraServiceProducer.stubInstance());

        LabVesselDao tubeDao = EasyMock.createNiceMock(LabVesselDao.class);
        labBatchEJB.setTubeDAO(tubeDao);

        JiraTicketDao mockJira = EasyMock.createNiceMock(JiraTicketDao.class);
        labBatchEJB.setJiraTicketDao(mockJira);

        LabBatchDAO labBatchDAO = EasyMock.createNiceMock(LabBatchDAO.class);
        labBatchEJB.setLabBatchDao(labBatchDAO);

        BucketDao mockBucketDao = EasyMock.createNiceMock(BucketDao.class);

        EasyMock.replay(mockBucketDao, tubeDao, mockJira, labBatchDAO);

        LabEventHandler labEventHandler = getLabEventHandler();
        BuildIndexPlate buildIndexPlate = new BuildIndexPlate("IndexPlate").invoke(null);
        FluidigmMessagesBuilder fluidigmMessagesBuilder = new FluidigmMessagesBuilder("", bettaLimsMessageTestFactory,
                labEventFactory, labEventHandler,
                mapBarcodeToTube,
                buildIndexPlate.getIndexPlate());
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
            assertThat(labEventNames, Matchers.<String>hasItem(startsWith(expectedEventName + " ")));
        }

        Assert.assertEquals(labEventNames.size(), expectedEventNames.length, "Wrong number of transfers");

        for (int i = 0; i < expectedEventNames.length; i++) {
            assertThat("Unexpected event at position " + i, labEventNames.get(i), startsWith(expectedEventNames[i]));
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
        List<LabVessel> labVessels = new ArrayList<>();
        labVessels.add(labVessel);
        validateWorkflow(nextEventTypeName, labVessels);
    }

    public static void validateWorkflow(String nextEventTypeName, List<LabVessel> labVessels) {
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
     * Builds a plate of molecular indexes
     */
    public static class BuildIndexPlate {
        private final String indexPlateBarcode;
        private StaticPlate indexPlate;
//        private MolecularIndexReagent index301;

        public BuildIndexPlate(String indexPlateBarcode) {
            this.indexPlateBarcode = indexPlateBarcode;
        }

        public StaticPlate getIndexPlate() {
            return indexPlate;
        }

        public BuildIndexPlate invoke(@Nullable MolecularIndexingSchemeDao molecularIndexingSchemeDao) {
            char[] bases = {'A', 'C', 'T', 'G'};

            indexPlate = new StaticPlate(indexPlateBarcode, StaticPlate.PlateType.IndexedAdapterPlate96);
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
                            .findSingleIndexScheme(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7, sequence);
                }
                if (testScheme == null) {
                    testScheme = new MolecularIndexingScheme(
                            new EnumMap<MolecularIndexingScheme.IndexPosition, MolecularIndex>(
                                    MolecularIndexingScheme.IndexPosition.class) {{
                                put(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7, new MolecularIndex(sequence));
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
            return this;
        }
    }

    public static TwoDBarcodedTube buildBaitTube(String tubeBarcode, ReagentDesign reagent) {
        TwoDBarcodedTube baitTube = new TwoDBarcodedTube(tubeBarcode);
        if (reagent == null) {
            reagent = new ReagentDesign("cancer_2000gene_shift170_undercovered",
                    ReagentType.BAIT);
            reagent.setTargetSetName("Cancer_2K");
            reagent.setManufacturersName("1234abc");
        }
        baitTube.addReagent(new DesignedReagent(reagent));
        return baitTube;
    }

    public static class MockBucket extends Bucket {

        private final String testProductOrder;

        public MockBucket(@Nonnull String bucketDefinitionIn, String testProductOrder) {
            super(bucketDefinitionIn);
            this.testProductOrder = testProductOrder;
        }

        public MockBucket(@Nonnull WorkflowStepDef bucketDef, String testProductOrder) {
            super(bucketDef);
            this.testProductOrder = testProductOrder;
        }

        @Override
        public BucketEntry findEntry(@Nonnull LabVessel entryVessel) {
            addEntry(new BucketEntry(entryVessel, testProductOrder, this, BucketEntry.BucketEntryType.PDO_ENTRY));
            return super.findEntry(entryVessel);
        }
    }
}
