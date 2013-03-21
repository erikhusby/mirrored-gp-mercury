package org.broadinstitute.gpinformatics.mercury.test;

//import com.jprofiler.api.agent.Controller;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.monitoring.HipChatMessageSender;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnectorProducer;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.boundary.graph.Graph;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunResource;
import org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferEntityGrapher;
import org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferVisualizer;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.control.zims.ZimsIlluminaRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.rework.LabVesselComment;
import org.broadinstitute.gpinformatics.mercury.entity.rework.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.rework.ReworkLevel;
import org.broadinstitute.gpinformatics.mercury.entity.rework.ReworkReason;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StripTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.presentation.transfervis.TransferVisualizerFrame;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign.ReagentType;

/**
 * Test messaging
 */
@SuppressWarnings({"FeatureEnvy", "OverlyCoupledClass", "OverlyCoupledMethod", "OverlyLongMethod"})
public class LabEventTest {
    public static final int NUM_POSITIONS_IN_RACK = 96;

    /**
     * Physical type for a 2-lane flowcell
     */
    public static final String PHYS_TYPE_FLOWCELL_2_LANE = "Flowcell2Lane";

    /**
     * Section for both lanes of a 2-lane flowcell
     */
    public static final String SECTION_ALL_2 = "FLOWCELL2";

    public static final String POND_REGISTRATION_TUBE_PREFIX = "PondReg";
    private static Map<String, ProductOrder> mapKeyToProductOrder = new HashMap<String, ProductOrder>();

    private final LabEventFactory.LabEventRefDataFetcher labEventRefDataFetcher =
            new LabEventFactory.LabEventRefDataFetcher() {

                @Override
                public BspUser getOperator(String userId) {
                    return new BSPUserList.QADudeUser("Test", BSPManagerFactoryStub.QA_DUDE_USER_ID);
                }

                @Override
                public BspUser getOperator(Long bspUserId) {
                    return new BSPUserList.QADudeUser("Test", BSPManagerFactoryStub.QA_DUDE_USER_ID);
                }

                @Override
                public LabBatch getLabBatch(String labBatchName) {
                    return null;
                }
            };

    /**
     * Used in test verification, accumulates the events in a chain of transfers
     */
    public static class ListTransfersFromStart implements TransferTraverserCriteria {
        private int hopCount = -1;
        private final List<String> labEventNames = new ArrayList<String>();

        /**
         * Avoid infinite loops
         */
        private Set<LabEvent> visitedLabEvents = new HashSet<LabEvent>();

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            if (context.getEvent() != null) {
                if (!getVisitedLabEvents().add(context.getEvent())) {
                    return TraversalControl.StopTraversing;
                }
                if (context.getHopCount() > hopCount) {
                    hopCount = context.getHopCount();
                    labEventNames.add(context.getEvent().getLabEventType().getName() + " into " +
                            context.getEvent().getTargetLabVessels().iterator().next().getLabel());
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
    }

    /**
     * Build object graph for Hybrid Selection messages, verify chain of events.
     */
    @Test(groups = {DATABASE_FREE})
    public void testHybridSelection() {
//        Controller.startCPURecording(true);

        LabBatchEjb labBatchEJB = new LabBatchEjb();
        labBatchEJB.setAthenaClientService(AthenaClientProducer.stubInstance());
        labBatchEJB.setJiraService(JiraServiceProducer.stubInstance());

        LabVesselDao tubeDao = EasyMock.createNiceMock(LabVesselDao.class);
        labBatchEJB.setTubeDAO(tubeDao);

        JiraTicketDao mockJira = EasyMock.createNiceMock(JiraTicketDao.class);
        labBatchEJB.setJiraTicketDao(mockJira);

        LabBatchDAO labBatchDAO = EasyMock.createNiceMock(LabBatchDAO.class);
        labBatchEJB.setLabBatchDao(labBatchDAO);

        // starting rack
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();

        final ProductOrder productOrder =
                AthenaClientServiceStub.buildHybridSelectionProductOrder(NUM_POSITIONS_IN_RACK);
        int rackPosition=1;

        for(ProductOrderSample poSample:productOrder.getSamples()) {
            String barcode = "R" +rackPosition;

            TwoDBarcodedTube aliquot = new TwoDBarcodedTube(barcode);
            aliquot.addSample(new MercurySample(productOrder.getBusinessKey(), poSample.getSampleName()));
            mapBarcodeToTube.put(barcode, aliquot);
            rackPosition++;
        }

        LabBatch workflowBatch = new LabBatch("Hybrid Selection Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        labBatchEJB.createLabBatch(workflowBatch, "scottmat");

        mapKeyToProductOrder.put(productOrder.getBusinessKey(), productOrder);

        // Messaging
        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);


        BucketDao mockBucketDao = EasyMock.createNiceMock(BucketDao.class);
        BucketBean bucketBeanEJB = new BucketBean(labEventFactory, JiraServiceProducer.stubInstance(), labBatchEJB);
        EasyMock.replay(mockBucketDao, labBatchDAO, tubeDao, mockJira);

        LabEventHandler labEventHandler =
                new LabEventHandler(new WorkflowLoader(), AthenaClientProducer
                        .stubInstance(), bucketBeanEJB, mockBucketDao, new BSPUserList(BSPManagerFactoryProducer
                        .stubInstance()));

        PreFlightEntityBuilder preFlightEntityBuilder = new PreFlightEntityBuilder(bettaLimsMessageFactory,
                labEventFactory, labEventHandler,
                mapBarcodeToTube).invoke();

        ShearingEntityBuilder shearingEntityBuilder = new ShearingEntityBuilder(mapBarcodeToTube, preFlightEntityBuilder.getTubeFormation(),
                bettaLimsMessageFactory, labEventFactory, labEventHandler, preFlightEntityBuilder.getRackBarcode()).invoke();

        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = new LibraryConstructionEntityBuilder(
                bettaLimsMessageFactory, labEventFactory, labEventHandler,
                shearingEntityBuilder.getShearingCleanupPlate(), shearingEntityBuilder.getShearCleanPlateBarcode(),
                shearingEntityBuilder.getShearingPlate(), NUM_POSITIONS_IN_RACK).invoke();

        HybridSelectionEntityBuilder hybridSelectionEntityBuilder = new HybridSelectionEntityBuilder(
                bettaLimsMessageFactory, labEventFactory, labEventHandler,
                libraryConstructionEntityBuilder.getPondRegRack(),
                libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                libraryConstructionEntityBuilder.getPondRegTubeBarcodes()).invoke();

        QtpEntityBuilder qtpEntityBuilder = new QtpEntityBuilder(
                bettaLimsMessageFactory, labEventFactory, labEventHandler,
                Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchRack()),
                Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchRackBarcode()),
                Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchBarcodes()),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), WorkflowName.HYBRID_SELECTION);
        qtpEntityBuilder.invoke();

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
        ZimsIlluminaRunFactory zimsIlluminaRunFactory = new ZimsIlluminaRunFactory(
                new ProductOrderDao() {
                    @Override
                    public ProductOrder findByBusinessKey(String key) {
                        return productOrder;
                    }
                },
                new BSPSampleDataFetcher() {
                    @Override
                    public BSPSampleDTO fetchSingleSampleFromBSP(String sampleName) {
                        return new BSPSampleDTO("HIV", "lsid", "DNA", "samplealias", "Homo Sapiens", "PT-123");
                    }
                }
        );
        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(illuminaSequencingRun);
        Assert.assertEquals(zimsIlluminaRun.getLanes().size(), 8, "Wrong number of lanes");
        ZimsIlluminaChamber zimsIlluminaChamber = zimsIlluminaRun.getLanes().iterator().next();
        Assert.assertEquals(zimsIlluminaChamber.getLibraries().size(), NUM_POSITIONS_IN_RACK, "Wrong number of libraries");
        LibraryBean libraryBean = zimsIlluminaChamber.getLibraries().iterator().next();
        // todo jmt need to investigate the ordering of libraries in ZIMS API results, and do more asserts here
        Assert.assertNotNull(libraryBean.getMolecularIndexingScheme().getName(), "No molecular index");
        Assert.assertEquals(libraryBean.getBaitSetName(), HybridSelectionEntityBuilder.BAIT_DESIGN_NAME, "Wrong bait");

        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        TwoDBarcodedTube startingTube = mapBarcodeToTube.entrySet().iterator().next().getValue();
        startingTube.evaluateCriteria(transferTraverserCriteria,
                TransferTraverserCriteria.TraversalDirection.Descendants);
        List<String> labEventNames = transferTraverserCriteria.getLabEventNames();
        Assert.assertEquals(labEventNames.size(), 13, "Wrong number of transfers");

        Assert.assertEquals(illuminaSequencingRun.getSampleCartridge(),
                qtpEntityBuilder.getIlluminaFlowcell(), "Wrong flowcell");

        // pick a sample and mark it for rework
        Map.Entry<String, TwoDBarcodedTube> twoDBarcodedTubeForRework = mapBarcodeToTube.entrySet().iterator().next();
        int lastEventIndex = transferTraverserCriteria.getVisitedLabEvents().size();
        LabEvent catchEvent =
                transferTraverserCriteria.getVisitedLabEvents().toArray(new LabEvent[lastEventIndex])[lastEventIndex - 1];
        MercurySample startingSample =
                twoDBarcodedTubeForRework.getValue().getAllSamples().iterator().next().getStartingSample();

        ReworkEntry reworkEntry = startingSample.getRapSheet().addRework(ReworkReason.MACHINE_ERROR, ReworkLevel.ONE_SAMPLE_HOLD_REST_BATCH,
                                catchEvent.getLabEventType(),VesselPosition.TUBE1,startingSample);

        LabVesselComment reworkComment =
                new LabVesselComment<ReworkEntry>(catchEvent, twoDBarcodedTubeForRework.getValue(), "rework comment",
                        Arrays.asList(reworkEntry));
        Assert.assertNotNull(reworkComment.getLabEvent(),"Lab event is required.");
        Assert.assertNotNull(reworkComment.getLabVessel(),"Lab Vessel is required.");
        Assert.assertNotNull(reworkComment.getRapSheetEntries(),"Rap Sheet Entries should not be null.");
        Assert.assertFalse(reworkComment.getRapSheetEntries().isEmpty(), "Should have some Rap Sheet Entries.");
        Assert.assertTrue(reworkComment.getRapSheetEntries().get(0) instanceof ReworkEntry, "Entry should be ReworkEntry.");
        ReworkEntry rework = (ReworkEntry)reworkComment.getRapSheetEntries().get(0);
        Assert.assertNotNull(rework.getReworkLevel(), "ReworkLevel cannot be null.");
        Assert.assertNotNull(rework.getReworkReason(), "ReworkReason cannot be null.");
        Assert.assertNotNull(rework.getReworkStep(), "getReworkStep cannot be null.");
        Assert.assertNotNull(rework.getRapSheet(), "rework.getRapSheet cannot be null.");
        Assert.assertNotNull(rework.getRapSheet().getSample(), "RapSheet.sample cannot be null.");

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
        Assert.assertEquals(graph.getMapIdToVertex().size(), 1208, "Wrong number of vertices");
//        Controller.stopCPURecording();
    }


    /**
     * Build object graph for Hybrid Selection messages, verify chain of events.
     */
    @Test(groups = {DATABASE_FREE})
    public void testExomeExpress() throws Exception {
//        Controller.startCPURecording(true);

        LabBatchEjb labBatchEJB = new LabBatchEjb();
        labBatchEJB.setAthenaClientService(AthenaClientProducer.stubInstance());
        labBatchEJB.setJiraService(JiraServiceProducer.stubInstance());

        LabVesselDao tubeDao = EasyMock.createNiceMock(LabVesselDao.class);
        labBatchEJB.setTubeDAO(tubeDao);

        JiraTicketDao mockJira = EasyMock.createNiceMock(JiraTicketDao.class);
        labBatchEJB.setJiraTicketDao(mockJira);

        LabBatchDAO labBatchDAO = EasyMock.createNiceMock(LabBatchDAO.class);
        labBatchEJB.setLabBatchDao(labBatchDAO);


        ProductOrder productOrder = AthenaClientServiceStub.buildExExProductOrder(96);
        String jiraTicketKey = productOrder.getBusinessKey();

        mapKeyToProductOrder.put(productOrder.getBusinessKey(), productOrder);

        // starting rack
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        int rackPosition = 1;
        for(ProductOrderSample poSample:productOrder.getSamples()) {
            String barcode = "R" + rackPosition;
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(productOrder.getBusinessKey(), poSample.getSampleName()));
            mapBarcodeToTube.put(barcode, bspAliquot);
            rackPosition++;
        }

        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        labBatchEJB.createLabBatch(workflowBatch, "scottmat");

        final Date runDate = new Date();
        String rackBarcode = "REXEX" + runDate.toString();

        // Messaging
        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);


        BucketDao mockBucketDao = EasyMock.createMock(BucketDao.class);
        EasyMock.expect(mockBucketDao.findByName(EasyMock.eq("Shearing Bucket")))
                .andReturn(new MockBucket(new WorkflowStepDef("Shearing Bucket"), jiraTicketKey));
        EasyMock.expect(mockBucketDao.findByName(EasyMock.eq("Shearing Bucket")))
                .andReturn(new MockBucket(new WorkflowStepDef("Shearing Bucket"), jiraTicketKey));
        EasyMock.expect(mockBucketDao.findByName(EasyMock.eq("Pico/Plating Bucket")))
                .andReturn(new MockBucket(new WorkflowStepDef("Pico/Plating Bucket"), jiraTicketKey));
        BucketBean bucketBeanEJB = new BucketBean(labEventFactory, JiraServiceProducer.stubInstance(), labBatchEJB);
        EasyMock.replay(mockBucketDao, tubeDao, mockJira, labBatchDAO);
        LabEventHandler labEventHandler =
                new LabEventHandler(new WorkflowLoader(), AthenaClientProducer.stubInstance(), bucketBeanEJB,
                        mockBucketDao, new BSPUserList(BSPManagerFactoryProducer.stubInstance()));

        PicoPlatingEntityBuilder pplatingEntityBuilder = new PicoPlatingEntityBuilder(bettaLimsMessageFactory,
                labEventFactory, labEventHandler,
                mapBarcodeToTube, rackBarcode).invoke();

        ExomeExpressShearingEntityBuilder shearingEntityBuilder =
                new ExomeExpressShearingEntityBuilder(pplatingEntityBuilder.getNormBarcodeToTubeMap(),
                        pplatingEntityBuilder.getNormTubeFormation(), bettaLimsMessageFactory, labEventFactory,
                        labEventHandler, pplatingEntityBuilder.getNormalizationBarcode()).invoke();

        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = new LibraryConstructionEntityBuilder(
                bettaLimsMessageFactory, labEventFactory, labEventHandler,
                shearingEntityBuilder.getShearingCleanupPlate(), shearingEntityBuilder.getShearCleanPlateBarcode(),
                shearingEntityBuilder.getShearingPlate(), NUM_POSITIONS_IN_RACK).invoke();

        HybridSelectionEntityBuilder hybridSelectionEntityBuilder = new HybridSelectionEntityBuilder(
                bettaLimsMessageFactory, labEventFactory, labEventHandler,
                libraryConstructionEntityBuilder.getPondRegRack(),
                libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                libraryConstructionEntityBuilder.getPondRegTubeBarcodes()).invoke();

        QtpEntityBuilder qtpEntityBuilder = new QtpEntityBuilder(
                bettaLimsMessageFactory, labEventFactory, labEventHandler,
                Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchRack()),
                Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchRackBarcode()),
                Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchBarcodes()),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), WorkflowName.EXOME_EXPRESS);
        qtpEntityBuilder.invoke();

        String flowcellBarcode = "flowcell" + runDate.getTime();

        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
            new HiSeq2500FlowcellEntityBuilder(bettaLimsMessageFactory, labEventFactory,
                    labEventHandler,
                    qtpEntityBuilder.getDenatureRack(),
                            flowcellBarcode).invoke();

        IlluminaFlowcell illuminaFlowcell = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell();
        Set<SampleInstance> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                VesselPosition.LANE1);
        Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(), 2,
                "Wrong number of reagents");
        Set<SampleInstance> lane2SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                VesselPosition.LANE2);
        Assert.assertEquals(lane2SampleInstances.iterator().next().getReagents().size(), 2,
                "Wrong number of reagents");




        final String machineName = "Superman";

        SimpleDateFormat dateFormat = new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN);

        final File runPath = File.createTempFile("tempRun" +dateFormat.format(runDate), ".txt");
        SolexaRunBean runBean =
                new SolexaRunBean(flowcellBarcode,
                                         flowcellBarcode + dateFormat.format(runDate),
                                         runDate, machineName,
                                         runPath.getAbsolutePath(), null);

        IlluminaSequencingRunDao runDao = EasyMock.createNiceMock(IlluminaSequencingRunDao.class);

        EasyMock.expect(runDao.findByRunName(EasyMock.anyObject(String.class))).andReturn(null);

        IlluminaSequencingRunFactory runFactory = EasyMock.createMock(IlluminaSequencingRunFactory.class);
        EasyMock.expect(runFactory.build(EasyMock.anyObject(SolexaRunBean.class),
                                                EasyMock.anyObject(IlluminaFlowcell.class)))
                .andReturn(new IlluminaSequencingRun(hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell(),
                                                            runPath.getName(), runBean.getRunBarcode(), machineName,
                                                            null, false, runDate,
                                                            null,
                                                            runPath.getAbsolutePath()));

        IlluminaFlowcellDao flowcellDao = EasyMock.createMock(IlluminaFlowcellDao.class);
        EasyMock.expect(flowcellDao.findByBarcode(EasyMock.anyObject(String.class)))
                .andReturn(hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell());


        LabVesselDao vesselDao = EasyMock.createNiceMock(LabVesselDao.class);

        HipChatMessageSender hipChatMsgSender = EasyMock.createNiceMock(HipChatMessageSender.class);

        MercuryOrSquidRouter router = new MercuryOrSquidRouter(vesselDao, AthenaClientProducer.stubInstance());

        SolexaRunResource runResource = new SolexaRunResource(runDao, runFactory, flowcellDao, router,
                                                                     SquidConnectorProducer.stubInstance(),
                                                                     hipChatMsgSender);

        UriInfo uriInfoMock = EasyMock.createNiceMock(UriInfo.class);
        EasyMock.expect(uriInfoMock.getAbsolutePathBuilder()).andReturn(UriBuilder.fromPath(""));
        EasyMock.replay(runDao, runFactory, flowcellDao, vesselDao, uriInfoMock,hipChatMsgSender);

        Map.Entry<String, TwoDBarcodedTube> stringTwoDBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        stringTwoDBarcodedTubeEntry.getValue().evaluateCriteria(transferTraverserCriteria,
                TransferTraverserCriteria.TraversalDirection.Descendants);
        List<String> labEventNames = transferTraverserCriteria.getLabEventNames();

        //todo: these need to be made to assert something useful, andn pass.
        Assert.assertEquals(labEventNames.size(), 11, "Wrong number of transfers");

        /*
         *
         * Temporarily disabling this check until after demo.
         */

        IlluminaSequencingRun illuminaSequencingRun
                = (IlluminaSequencingRun) hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell().getSequencingRuns().iterator().next();

        Assert.assertEquals(illuminaSequencingRun.getSampleCartridge(),
                hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell(), "Wrong flowcell");

        EasyMock.verify(mockBucketDao);
//        Controller.stopCPURecording();
    }

    /**
     * Build object graph for Whole Genome Shotgun messages, verify chain of events.
     */
    @Test(groups = {DATABASE_FREE})
    public void testWholeGenomeShotgun() {
//        Controller.startCPURecording(true);

        LabBatchEjb labBatchEJB = new LabBatchEjb();
        labBatchEJB.setAthenaClientService(AthenaClientProducer.stubInstance());
        labBatchEJB.setJiraService(JiraServiceProducer.stubInstance());

        LabVesselDao tubeDao = EasyMock.createNiceMock(LabVesselDao.class);
        labBatchEJB.setTubeDAO(tubeDao);

        JiraTicketDao mockJira = EasyMock.createNiceMock(JiraTicketDao.class);
        labBatchEJB.setJiraTicketDao(mockJira);

        LabBatchDAO labBatchDAO = EasyMock.createNiceMock(LabBatchDAO.class);
        labBatchEJB.setLabBatchDao(labBatchDAO);


        ProductOrder productOrder = AthenaClientServiceStub.buildWholeGenomeProductOrder(NUM_POSITIONS_IN_RACK);
        String jiraTicketKey = productOrder.getBusinessKey();

        mapKeyToProductOrder.put(jiraTicketKey, productOrder);

        // starting rack
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        int rackPosition = 1;
        for(ProductOrderSample poSample: productOrder.getSamples()) {
            String barcode = "R" + rackPosition;
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(jiraTicketKey, poSample.getSampleName()));
            mapBarcodeToTube.put(barcode, bspAliquot);
            rackPosition++;
        }
        LabBatch workflowBatch = new LabBatch("whole Genome Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        labBatchEJB.createLabBatch(workflowBatch, "scottmat");

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);


        BucketDao mockBucketDao = EasyMock.createNiceMock(BucketDao.class);
        BucketBean bucketBeanEJB = new BucketBean(labEventFactory, JiraServiceProducer.stubInstance(), labBatchEJB);
        EasyMock.replay(mockBucketDao, tubeDao, mockJira, labBatchDAO);

        LabEventHandler labEventHandler =
                new LabEventHandler(new WorkflowLoader(), AthenaClientProducer
                        .stubInstance(), bucketBeanEJB, mockBucketDao, new BSPUserList(BSPManagerFactoryProducer
                        .stubInstance()));

        PreFlightEntityBuilder preFlightEntityBuilder = new PreFlightEntityBuilder(bettaLimsMessageFactory,
                labEventFactory, labEventHandler,
                mapBarcodeToTube).invoke();

        ShearingEntityBuilder shearingEntityBuilder = new ShearingEntityBuilder(mapBarcodeToTube, preFlightEntityBuilder.getTubeFormation(),
                bettaLimsMessageFactory, labEventFactory, labEventHandler, preFlightEntityBuilder.getRackBarcode()).invoke();

        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = new LibraryConstructionEntityBuilder(
                bettaLimsMessageFactory, labEventFactory, labEventHandler,
                shearingEntityBuilder.getShearingCleanupPlate(), shearingEntityBuilder.getShearCleanPlateBarcode(),
                shearingEntityBuilder.getShearingPlate(), NUM_POSITIONS_IN_RACK).invoke();

        List<String> sageUnloadTubeBarcodes = new ArrayList<String>();
        for (int i = 1; i <= NUM_POSITIONS_IN_RACK; i++) {
            sageUnloadTubeBarcodes.add("SageUnload" + i);
        }
        String sageUnloadBarcode = "SageUnload";
        Map<String, TwoDBarcodedTube> mapBarcodeToSageUnloadTubes = new HashMap<String, TwoDBarcodedTube>();
        RackOfTubes targetRackOfTubes = null;
        for (int i = 0; i < NUM_POSITIONS_IN_RACK / 4; i++) {
            // SageLoading
            String sageCassetteBarcode = "SageCassette" + i;
            PlateTransferEventType sageLoadingJaxb = bettaLimsMessageFactory.buildRackToPlate("SageLoading",
                    libraryConstructionEntityBuilder
                            .getPondRegRackBarcode(),
                    libraryConstructionEntityBuilder
                            .getPondRegTubeBarcodes()
                            .subList(i * 4,
                                    i * 4 + 4),
                    sageCassetteBarcode);
            // todo jmt SAGE section
            LabEvent sageLoadingEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(sageLoadingJaxb,
                    libraryConstructionEntityBuilder
                            .getPondRegRack(),
                    null);
            labEventHandler.processEvent(sageLoadingEntity);
            StaticPlate sageCassette = (StaticPlate) sageLoadingEntity.getTargetLabVessels().iterator().next();

            // SageLoaded

            // SageUnloading
            PlateTransferEventType sageUnloadingJaxb = bettaLimsMessageFactory.buildPlateToRack("SageUnloading",
                    sageCassetteBarcode,
                    sageUnloadBarcode,
                    sageUnloadTubeBarcodes
                            .subList(i * 4,
                                    i * 4 + 4));
            LabEvent sageUnloadEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(sageUnloadingJaxb,
                    sageCassette, mapBarcodeToSageUnloadTubes, targetRackOfTubes);
            labEventHandler.processEvent(sageUnloadEntity);
            sageUnloadEntity.getTargetLabVessels().iterator().next();
        }

        // SageCleanup
        List<String> sageCleanupTubeBarcodes = new ArrayList<String>();
        for (int i = 1; i <= NUM_POSITIONS_IN_RACK; i++) {
            sageCleanupTubeBarcodes.add("SageCleanup" + i);
        }
        String sageCleanupBarcode = "SageCleanup";
        PlateTransferEventType sageCleanupJaxb = bettaLimsMessageFactory.buildRackToRack("SageCleanup", sageUnloadBarcode,
                sageUnloadTubeBarcodes, sageCleanupBarcode, sageCleanupTubeBarcodes);
        Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new HashMap<VesselPosition, TwoDBarcodedTube>();
        List<TwoDBarcodedTube> sageUnloadTubes = new ArrayList<TwoDBarcodedTube>(mapBarcodeToSageUnloadTubes.values());
        for (int i = 0; i < NUM_POSITIONS_IN_RACK; i++) {
            mapPositionToTube.put(VesselPosition.getByName(bettaLimsMessageFactory.buildWellName(i + 1)),
                    sageUnloadTubes.get(i));
        }
        TubeFormation sageUnloadRackRearrayed = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        sageUnloadRackRearrayed.addRackOfTubes(new RackOfTubes("sageUnloadRearray", RackOfTubes.RackType.Matrix96));
        LabEvent sageCleanupEntity = labEventFactory.buildFromBettaLimsRackToRackDbFree(sageCleanupJaxb,
                sageUnloadRackRearrayed, new HashMap<String, TwoDBarcodedTube>(), targetRackOfTubes);
        labEventHandler.processEvent(sageCleanupEntity);
        TubeFormation sageCleanupRack = (TubeFormation) sageCleanupEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(sageCleanupRack.getSampleInstances().size(), NUM_POSITIONS_IN_RACK, "Wrong number of sage cleanup samples");

        QtpEntityBuilder qtpEntityBuilder =
                new QtpEntityBuilder(bettaLimsMessageFactory, labEventFactory, labEventHandler,
                        Collections.singletonList(sageCleanupRack), Collections.singletonList(sageCleanupBarcode),
                        Collections.singletonList(sageCleanupTubeBarcodes), mapBarcodeToSageUnloadTubes,
                        WorkflowName.WHOLE_GENOME);
        qtpEntityBuilder.invoke();

        IlluminaFlowcell illuminaFlowcell = qtpEntityBuilder.getIlluminaFlowcell();
        Set<SampleInstance> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                VesselPosition.LANE1);
        Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(), 1,
                "Wrong number of reagents");

        Map.Entry<String, TwoDBarcodedTube> stringTwoDBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        stringTwoDBarcodedTubeEntry.getValue().evaluateCriteria(transferTraverserCriteria,
                TransferTraverserCriteria.TraversalDirection.Descendants);
        List<String> labEventNames = transferTraverserCriteria.getLabEventNames();
        Assert.assertEquals(labEventNames.size(), 12, "Wrong number of transfers");

//        Controller.stopCPURecording();
    }

    /**
     * Build object graph for Fluidigm messages
     */
    @Test(groups = {DATABASE_FREE})
    public void testFluidigm() {
        // starting rack
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for (int rackPosition = 1; rackPosition <= SBSSection.P96COLS1_6BYROW.getWells().size(); rackPosition++) {
            String barcode = "R" + rackPosition;

            String bspStock = "SM-" + rackPosition;
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(null, bspStock));
            mapBarcodeToTube.put(barcode, bspAliquot);
        }

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
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
        BucketBean bucketBeanEJB = new BucketBean(labEventFactory, JiraServiceProducer.stubInstance(), labBatchEJB);
        EasyMock.replay(mockBucketDao, tubeDao, mockJira, labBatchDAO);

        LabEventHandler labEventHandler = new LabEventHandler(new WorkflowLoader(), AthenaClientProducer.stubInstance(), bucketBeanEJB, mockBucketDao, new BSPUserList(BSPManagerFactoryProducer
                .stubInstance()));
        BuildIndexPlate buildIndexPlate = new BuildIndexPlate("IndexPlate").invoke(null);
        FluidigmMessagesBuilder fluidigmMessagesBuilder = new FluidigmMessagesBuilder("", bettaLimsMessageFactory,
                labEventFactory, labEventHandler,
                mapBarcodeToTube,
                buildIndexPlate.getIndexPlate());
        fluidigmMessagesBuilder.buildJaxb();
        fluidigmMessagesBuilder.buildObjectGraph();
    }

    /**
     * Builds entity graph for Fluidigm events
     */
    private static class FluidigmMessagesBuilder {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
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
        private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
        private String harvestRackBarcode;
        private final Map<String, TwoDBarcodedTube> mapBarcodeToHarvestTube = new HashMap<String, TwoDBarcodedTube>();

        private FluidigmMessagesBuilder(String testPrefix, BettaLimsMessageFactory bettaLimsMessageFactory,
                LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                Map<String, TwoDBarcodedTube> mapBarcodeToTube, StaticPlate indexPlate) {
            this.testPrefix = testPrefix;
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
            this.mapBarcodeToTube = mapBarcodeToTube;
            this.indexPlate = indexPlate;
        }

        private void buildJaxb() {
            // FluidigmSampleInput rack P96COLS1-6BYROW to chip P384COLS4-6BYROW
            ArrayList<String> tubeBarcodes = new ArrayList<String>(mapBarcodeToTube.keySet());
            chipBarcode = "Fluidigm" + testPrefix;
            rackBarcode = "InputRack" + testPrefix;
            fluidigmSampleInputJaxb = bettaLimsMessageFactory.buildRackToPlate("FluidigmSampleInput", rackBarcode,
                    tubeBarcodes, chipBarcode);
            fluidigmSampleInputJaxb.getSourcePlate().setSection(SBSSection.P96COLS1_6BYROW.getSectionName());
            PositionMapType sourcePositionMap = buildFluidigmPositionMap(tubeBarcodes,
                    fluidigmSampleInputJaxb.getSourcePlate()
                            .getBarcode());
            fluidigmSampleInputJaxb.setSourcePositionMap(sourcePositionMap);
            fluidigmSampleInputJaxb.getPlate().setPhysType(
                    StaticPlate.PlateType.Fluidigm48_48AccessArrayIFC.getDisplayName());
            fluidigmSampleInputJaxb.getPlate().setSection(SBSSection.P384COLS4_6BYROW.getSectionName());
            addMessage(messageList, bettaLimsMessageFactory, fluidigmSampleInputJaxb);

            // FluidigmIndexedAdapterInput plate P96COLS1-6BYROW to chip P384COLS4-6BYROW
            fluidigmIndexedAdapterInputJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "FluidigmIndexedAdapterInput", indexPlate.getLabel(), chipBarcode);
            fluidigmIndexedAdapterInputJaxb.getSourcePlate().setPhysType(
                    StaticPlate.PlateType.IndexedAdapterPlate96.getDisplayName());
            fluidigmIndexedAdapterInputJaxb.getSourcePlate().setSection(SBSSection.P96COLS1_6BYROW.getSectionName());
            fluidigmIndexedAdapterInputJaxb.getPlate().setPhysType(
                    StaticPlate.PlateType.Fluidigm48_48AccessArrayIFC.getDisplayName());
            fluidigmIndexedAdapterInputJaxb.getPlate().setSection(SBSSection.P384COLS4_6BYROW.getSectionName());
            addMessage(messageList, bettaLimsMessageFactory, fluidigmIndexedAdapterInputJaxb);

            // FluidigmHarvestingToRack chip P384COLS4-6BYROW to rack P96COLS1-6BYROW
            harvestRackBarcode = "Harvest" + testPrefix;
            List<String> harvestTubeBarcodes = new ArrayList<String>();
            for (int rackPosition = 1; rackPosition <= mapBarcodeToTube.size(); rackPosition++) {
                harvestTubeBarcodes.add("Harvest" + testPrefix + rackPosition);
            }
            fluidigmHarvestingToRackJaxb = bettaLimsMessageFactory.buildPlateToRack("FluidigmHarvestingToRack",
                    chipBarcode,
                    harvestRackBarcode,
                    harvestTubeBarcodes);
            fluidigmHarvestingToRackJaxb.getSourcePlate().setPhysType(
                    StaticPlate.PlateType.Fluidigm48_48AccessArrayIFC.getDisplayName());
            fluidigmHarvestingToRackJaxb.getSourcePlate().setSection(SBSSection.P384COLS4_6BYROW.getSectionName());
            fluidigmHarvestingToRackJaxb.setPositionMap(buildFluidigmPositionMap(tubeBarcodes, fluidigmSampleInputJaxb
                    .getSourcePlate().getBarcode()));
            fluidigmHarvestingToRackJaxb.getPlate().setSection(SBSSection.P96COLS1_6BYROW.getSectionName());
            addMessage(messageList, bettaLimsMessageFactory, fluidigmHarvestingToRackJaxb);
        }

        private PositionMapType buildFluidigmPositionMap(ArrayList<String> tubeBarcodes, String rackBarcode) {
            PositionMapType sourcePositionMap = new PositionMapType();
            sourcePositionMap.setBarcode(rackBarcode);
            int barcodeIndex = 0;
            for (int row = 0; row < 8; row++) {
                for (int column = 1; column <= 6; column++) {
                    ReceptacleType receptacleType = new ReceptacleType();
                    receptacleType.setBarcode(tubeBarcodes.get(barcodeIndex));
                    receptacleType.setPosition(bettaLimsMessageFactory.buildWellName(row * 12 + column));
                    sourcePositionMap.getReceptacle().add(receptacleType);
                    barcodeIndex++;
                }
            }
            return sourcePositionMap;
        }

        private void buildObjectGraph() {
            LabEvent fluidigmSampleInputEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(fluidigmSampleInputJaxb,
                    mapBarcodeToTube, null, null);
            labEventHandler.processEvent(fluidigmSampleInputEntity);
            // asserts
            StaticPlate chip = (StaticPlate) fluidigmSampleInputEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(chip.getSampleInstances().size(), mapBarcodeToTube.size(),
                    "Wrong number of sample instances");

            LabEvent fluidigmIndexedAdapterInputEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    fluidigmIndexedAdapterInputJaxb, indexPlate, chip);
            labEventHandler.processEvent(fluidigmIndexedAdapterInputEntity);

            LabEvent fluidigmHarvestingToRackEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(
                    fluidigmHarvestingToRackJaxb, chip, mapBarcodeToHarvestTube, null);
            labEventHandler.processEvent(fluidigmHarvestingToRackEntity);
            // asserts
            TubeFormation harvestRack = (TubeFormation) fluidigmHarvestingToRackEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(harvestRack.getSampleInstances().size(), mapBarcodeToTube.size(), "Wrong number of sample instances");
        }
    }

    public static void addMessage(List<BettaLIMSMessage> messageList, BettaLimsMessageFactory bettaLimsMessageFactory,
            StationEventType... stationEventTypes) {
        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.setMode(LabEventFactory.MODE_MERCURY);
        for (StationEventType stationEventType : stationEventTypes) {
            if (stationEventType instanceof PlateTransferEventType) {
                bettaLIMSMessage.getPlateTransferEvent().add((PlateTransferEventType) stationEventType);
            } else if (stationEventType instanceof PlateCherryPickEvent) {
                bettaLIMSMessage.getPlateCherryPickEvent().add((PlateCherryPickEvent) stationEventType);
            } else if (stationEventType instanceof PlateEventType) {
                bettaLIMSMessage.getPlateEvent().add((PlateEventType) stationEventType);
            } else if (stationEventType instanceof ReceptaclePlateTransferEvent) {
                bettaLIMSMessage.getReceptaclePlateTransferEvent().add((ReceptaclePlateTransferEvent) stationEventType);
            } else if (stationEventType instanceof ReceptacleEventType) {
                bettaLIMSMessage.getReceptacleEvent().add((ReceptacleEventType) stationEventType);
            } else {
                throw new RuntimeException("Unknown station event type " + stationEventType);
            }
        }
        messageList.add(bettaLIMSMessage);
        bettaLimsMessageFactory.advanceTime();
    }

    static void validateWorkflow(String nextEventTypeName, Collection<? extends LabVessel> tubes) {
        List<LabVessel> labVessels = new ArrayList<LabVessel>(tubes);
        validateWorkflow(nextEventTypeName, labVessels);
    }

    static void validateWorkflow(String nextEventTypeName, LabVessel labVessel) {
        List<LabVessel> labVessels = new ArrayList<LabVessel>();
        labVessels.add(labVessel);
        validateWorkflow(nextEventTypeName, labVessels);
    }

    static void validateWorkflow(String nextEventTypeName, List<LabVessel> labVessels) {
        WorkflowLoader workflowLoader = new WorkflowLoader();
        WorkflowConfig workflowConfig = workflowLoader.load();
        for (LabVessel labVessel : labVessels) {
            for (SampleInstance sampleInstance : labVessel.getSampleInstances()) {
                ProductOrder productOrder = mapKeyToProductOrder.get(
                        sampleInstance.getStartingSample().getProductOrderKey());
                // get workflow name from product order
                ProductWorkflowDef productWorkflowDef = workflowConfig.getWorkflowByName(
                        productOrder.getProduct().getWorkflowName());
                List<String> errors = productWorkflowDef.validate(labVessel, nextEventTypeName);
                if (!errors.isEmpty()) {
                    Assert.fail(errors.get(0));
                }
            }
        }
    }

    /**
     * Builds entity graph for Pre-flight events
     */
    public static class PreFlightEntityBuilder {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;

        private final Map<String, TwoDBarcodedTube> mapBarcodeToTube;
        private TubeFormation tubeFormation;
        private String rackBarcode;

        public PreFlightEntityBuilder(BettaLimsMessageFactory bettaLimsMessageFactory, LabEventFactory labEventFactory,
                LabEventHandler labEventHandler, Map<String, TwoDBarcodedTube> mapBarcodeToTube) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
            this.mapBarcodeToTube = mapBarcodeToTube;
        }
        public PreFlightEntityBuilder(BettaLimsMessageFactory bettaLimsMessageFactory, LabEventFactory labEventFactory,
                LabEventHandler labEventHandler, Map<String, TwoDBarcodedTube> mapBarcodeToTube,Map<String, ProductOrder> mapKeyToProductOrder) {

            this(bettaLimsMessageFactory,labEventFactory, labEventHandler, mapBarcodeToTube);

            LabEventTest.mapKeyToProductOrder = mapKeyToProductOrder;
        }

        public PreFlightEntityBuilder invoke() {
            PreFlightJaxbBuilder preFlightJaxbBuilder = new PreFlightJaxbBuilder(bettaLimsMessageFactory, "",
                    new ArrayList<String>(
                            mapBarcodeToTube.keySet()));
            preFlightJaxbBuilder.invoke();
            rackBarcode = preFlightJaxbBuilder.getRackBarcode();

            // PreflightPicoSetup 1
            validateWorkflow("PreflightPicoSetup", mapBarcodeToTube.values());
            LabEvent preflightPicoSetup1Entity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    preFlightJaxbBuilder.getPreflightPicoSetup1(), mapBarcodeToTube, null, null);
            labEventHandler.processEvent(preflightPicoSetup1Entity);
            // asserts
            tubeFormation = (TubeFormation) preflightPicoSetup1Entity.getSourceLabVessels().iterator().next();
            StaticPlate preflightPicoSetup1Plate = (StaticPlate) preflightPicoSetup1Entity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(preflightPicoSetup1Plate.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

            // PreflightPicoSetup 2
            validateWorkflow("PreflightPicoSetup", mapBarcodeToTube.values());
            LabEvent preflightPicoSetup2Entity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    preFlightJaxbBuilder.getPreflightPicoSetup2(), tubeFormation, null);
            labEventHandler.processEvent(preflightPicoSetup2Entity);
            // asserts
            StaticPlate preflightPicoSetup2Plate =
                    (StaticPlate) preflightPicoSetup2Entity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(preflightPicoSetup2Plate.getSampleInstances().size(), NUM_POSITIONS_IN_RACK,
                    "Wrong number of sample instances");

            // PreflightNormalization
            validateWorkflow("PreflightNormalization", mapBarcodeToTube.values());
            LabEvent preflightNormalization = labEventFactory.buildFromBettaLimsRackEventDbFree(
                    preFlightJaxbBuilder.getPreflightNormalization(), tubeFormation, mapBarcodeToTube, null);
            labEventHandler.processEvent(preflightNormalization);
            // asserts
            Assert.assertEquals(tubeFormation.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

            // PreflightPostNormPicoSetup 1
            validateWorkflow("PreflightPostNormPicoSetup", mapBarcodeToTube.values());
            LabEvent preflightPostNormPicoSetup1Entity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    preFlightJaxbBuilder.getPreflightPostNormPicoSetup1(), tubeFormation, null);
            labEventHandler.processEvent(preflightPostNormPicoSetup1Entity);
            // asserts
            StaticPlate preflightPostNormPicoSetup1Plate =
                    (StaticPlate) preflightPostNormPicoSetup1Entity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(preflightPostNormPicoSetup1Plate.getSampleInstances().size(), NUM_POSITIONS_IN_RACK,
                    "Wrong number of sample instances");

            // PreflightPostNormPicoSetup 2
            validateWorkflow("PreflightPostNormPicoSetup", mapBarcodeToTube.values());
            LabEvent preflightPostNormPicoSetup2Entity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    preFlightJaxbBuilder.getPreflightPostNormPicoSetup2(), tubeFormation, null);
            labEventHandler.processEvent(preflightPostNormPicoSetup2Entity);
            // asserts
            StaticPlate preflightPostNormPicoSetup2Plate =
                    (StaticPlate) preflightPostNormPicoSetup2Entity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(preflightPostNormPicoSetup2Plate.getSampleInstances().size(), NUM_POSITIONS_IN_RACK,
                    "Wrong number of sample instances");

            return this;
        }

        public String getRackBarcode() {
            return rackBarcode;
        }

        public TubeFormation getTubeFormation() {
            return tubeFormation;
        }
    }

    /**
     * Builds JAXB objects for Pre-flight messages
     */
    public static class PreFlightJaxbBuilder {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final String testPrefix;
        private final List<String> tubeBarcodes;

        private String rackBarcode;
        private PlateTransferEventType preflightPicoSetup1;
        private PlateTransferEventType preflightPicoSetup2;
        private PlateEventType preflightNormalization;
        private PlateTransferEventType preflightPostNormPicoSetup1;
        private PlateTransferEventType preflightPostNormPicoSetup2;

        private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();

        public PreFlightJaxbBuilder(BettaLimsMessageFactory bettaLimsMessageFactory, String testPrefix,
                List<String> tubeBarcodes) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.testPrefix = testPrefix;
            this.tubeBarcodes = tubeBarcodes;
        }

        public PlateTransferEventType getPreflightPicoSetup1() {
            return preflightPicoSetup1;
        }

        public PlateTransferEventType getPreflightPicoSetup2() {
            return preflightPicoSetup2;
        }

        public PlateEventType getPreflightNormalization() {
            return preflightNormalization;
        }

        public PlateTransferEventType getPreflightPostNormPicoSetup1() {
            return preflightPostNormPicoSetup1;
        }

        public PlateTransferEventType getPreflightPostNormPicoSetup2() {
            return preflightPostNormPicoSetup2;
        }

        public List<BettaLIMSMessage> getMessageList() {
            return messageList;
        }

        public String getRackBarcode() {
            return rackBarcode;
        }

        public PreFlightJaxbBuilder invoke() {
            rackBarcode = "PreflightRack" + testPrefix;
            preflightPicoSetup1 = bettaLimsMessageFactory.buildRackToPlate("PreflightPicoSetup", rackBarcode,
                    tubeBarcodes,
                    "PreflightPicoPlate1" + testPrefix);
            addMessage(messageList, bettaLimsMessageFactory, preflightPicoSetup1);

            preflightPicoSetup2 = bettaLimsMessageFactory.buildRackToPlate("PreflightPicoSetup", rackBarcode,
                    tubeBarcodes,
                    "PreflightPicoPlate2" + testPrefix);
            addMessage(messageList, bettaLimsMessageFactory, preflightPicoSetup2);

            preflightNormalization = bettaLimsMessageFactory.buildRackEvent("PreflightNormalization", rackBarcode,
                    tubeBarcodes);
            addMessage(messageList, bettaLimsMessageFactory, preflightNormalization);

            preflightPostNormPicoSetup1 = bettaLimsMessageFactory.buildRackToPlate("PreflightPostNormPicoSetup",
                    rackBarcode, tubeBarcodes,
                    "PreflightPostNormPicoPlate1" + testPrefix);
            addMessage(messageList, bettaLimsMessageFactory, preflightPostNormPicoSetup1);

            preflightPostNormPicoSetup2 = bettaLimsMessageFactory.buildRackToPlate("PreflightPostNormPicoSetup",
                    rackBarcode, tubeBarcodes,
                    "PreflightPostNormPicoPlate2" + testPrefix);
            addMessage(messageList, bettaLimsMessageFactory, preflightPostNormPicoSetup2);

            return this;
        }
    }


    public static class PicoPlatingEntityBuilder {

        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;

        private final Map<String, TwoDBarcodedTube> mapBarcodeToTube;
        private TubeFormation normTubeFormation;
        private String rackBarcode;
        private StaticPlate postNormPicoPlate;
        private String normalizationBarcode;
        private Map<String,TwoDBarcodedTube> normBarcodedTubeMap;

        public TubeFormation getNormTubeFormation() {
            return normTubeFormation;
        }

        public Map<String, TwoDBarcodedTube> getNormBarcodeToTubeMap() {

            return normBarcodedTubeMap;
        }

        public String getRackBarcode() {
            return rackBarcode;
        }

        public Map<String, TwoDBarcodedTube> getMapBarcodeToTube() {
            return mapBarcodeToTube;
        }

        public StaticPlate getPostNormPicoPlate() {
            return postNormPicoPlate;
        }

        public String getNormalizationBarcode() {
            return normalizationBarcode;
        }

        public void addKeyToProductOrder(String key, ProductOrder orderToMap) {
            mapKeyToProductOrder.put(key, orderToMap);
        }

        public PicoPlatingEntityBuilder(BettaLimsMessageFactory bettaLimsMessageFactory,
                                        LabEventFactory labEventFactory,
                                        LabEventHandler labEventHandler, Map<String, TwoDBarcodedTube> mapBarcodeToTube,
                                        String rackBarcode) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
            this.mapBarcodeToTube = mapBarcodeToTube;
            this.rackBarcode = rackBarcode;
        }

        public PicoPlatingEntityBuilder(BettaLimsMessageFactory bettaLimsMessageFactory,
                                        LabEventFactory labEventFactory,
                                        LabEventHandler labEventHandler, Map<String, TwoDBarcodedTube> mapBarcodeToTube,
                                        String rackBarcode, Map<String, ProductOrder> keyToProductOrderMap) {
            this(bettaLimsMessageFactory, labEventFactory,labEventHandler, mapBarcodeToTube, rackBarcode);

           mapKeyToProductOrder = keyToProductOrderMap;
        }

        public PicoPlatingEntityBuilder invoke() {

            PicoPlatingJaxbBuilder jaxbBuilder =
                    new PicoPlatingJaxbBuilder(rackBarcode, new ArrayList<String>(mapBarcodeToTube
                            .keySet()), "", bettaLimsMessageFactory);
            jaxbBuilder.invoke();


            validateWorkflow(LabEventType.PICO_PLATING_BUCKET.getName(), mapBarcodeToTube.values());
            LabEvent picoPlatingBucket =
                    labEventFactory.buildFromBettaLimsRackEventDbFree(jaxbBuilder.getPicoPlatingBucket(),
                            null, mapBarcodeToTube,null);
            labEventHandler.processEvent(picoPlatingBucket);
            TubeFormation initialTubeFormation = (TubeFormation) picoPlatingBucket.getInPlaceLabVessel();


            validateWorkflow(LabEventType.PICO_PLATING_QC.getName(), mapBarcodeToTube.values());
            LabEvent picoQcEntity =
                    labEventFactory.buildFromBettaLimsRackToPlateDbFree(jaxbBuilder
                            .getPicoPlatingQc(), mapBarcodeToTube, initialTubeFormation.getRacksOfTubes().iterator()
                            .next(), null);
            labEventHandler.processEvent(picoQcEntity);

            StaticPlate picoQcPlate = (StaticPlate) picoQcEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(picoQcPlate.getSampleInstances().size(), mapBarcodeToTube.values().size());


            validateWorkflow(LabEventType.PICO_DILUTION_TRANSFER.getName(), picoQcPlate);
            LabEvent picoPlatingSetup1Entity = labEventFactory
                    .buildFromBettaLimsPlateToPlateDbFree(jaxbBuilder.getPicoPlatingSetup1(), picoQcPlate, null);
            labEventHandler.processEvent(picoPlatingSetup1Entity);
            StaticPlate picoPlatingSetup1Plate = (StaticPlate) picoPlatingSetup1Entity.getTargetLabVessels().iterator()
                    .next();


            validateWorkflow(LabEventType.PICO_BUFFER_ADDITION.getName(), picoPlatingSetup1Plate);
            LabEvent picoPlatingSetup2Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(jaxbBuilder
                    .getPicoPlatingSetup2(), picoPlatingSetup1Plate, null);
            labEventHandler.processEvent(picoPlatingSetup2Entity);
            StaticPlate picoPlatingSetup2Plate = (StaticPlate) picoPlatingSetup2Entity.getTargetLabVessels().iterator()
                    .next();


            validateWorkflow(LabEventType.PICO_MICROFLUOR_TRANSFER.getName(), picoPlatingSetup2Plate);
            LabEvent picoPlatingSetup3Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(jaxbBuilder
                    .getPicoPlatingSetup3(), picoPlatingSetup2Plate, null);
            labEventHandler.processEvent(picoPlatingSetup3Entity);
            StaticPlate picoPlatingSetup3Plate = (StaticPlate) picoPlatingSetup3Entity.getTargetLabVessels().iterator()
                    .next();


            validateWorkflow(LabEventType.PICO_STANDARDS_TRANSFER.getName(), picoPlatingSetup3Plate);
            LabEvent picoPlatingSetup4Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(jaxbBuilder
                    .getPicoPlatingSetup4(), picoPlatingSetup3Plate, null);
            labEventHandler.processEvent(picoPlatingSetup4Entity);
            StaticPlate picoPlatingSetup4Plate = (StaticPlate) picoPlatingSetup4Entity.getTargetLabVessels().iterator()
                    .next();


            Map<VesselPosition, TwoDBarcodedTube> normPositionToTube =
                    new HashMap<VesselPosition, TwoDBarcodedTube>(initialTubeFormation.getContainerRole()
                            .getContainedVessels().size());

            SimpleDateFormat timestampFormat = new SimpleDateFormat("MMddHHmmss");
            String timestamp = timestampFormat.format(new Date());
            for (TwoDBarcodedTube currTube : initialTubeFormation.getContainerRole().getContainedVessels()) {
                VesselPosition currTubePositon = initialTubeFormation.getContainerRole().getPositionOfVessel(currTube);

                TwoDBarcodedTube currNormTube = new TwoDBarcodedTube("Norm" + currTubePositon.name() + timestamp);

                normPositionToTube.put(currTubePositon, currNormTube);
            }
            TubeFormation normTubeFormation = new TubeFormation(normPositionToTube, initialTubeFormation.getRackType());


            validateWorkflow(LabEventType.SAMPLES_NORMALIZATION_TRANSFER.getName(), picoPlatingSetup4Plate);
            LabEvent picoPlatingNormEntity =
                    labEventFactory.buildFromBettaLimsRackToRackDbFree(jaxbBuilder
                            .getPicoPlatingNormalizaion(), initialTubeFormation, normTubeFormation);

            labEventHandler.processEvent(picoPlatingNormEntity);
            this.normTubeFormation = (TubeFormation) picoPlatingNormEntity.getTargetLabVessels().iterator().next();
            normalizationBarcode = jaxbBuilder.getPicoPlatingNormalizaionBarcode();

            normBarcodedTubeMap =
                    new HashMap<String, TwoDBarcodedTube>(this.normTubeFormation.getContainerRole().getContainedVessels().size());

            for (TwoDBarcodedTube currTube : this.normTubeFormation.getContainerRole().getContainedVessels()) {
                normBarcodedTubeMap.put(currTube.getLabel(), currTube);
            }

            validateWorkflow(LabEventType.PICO_PLATING_POST_NORM_PICO.getName(), this.normTubeFormation);
            LabEvent picoPlatingPostNormEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(jaxbBuilder
                    .getPicoPlatingPostNormSetup(), this.normTubeFormation, null);
            labEventHandler.processEvent(picoPlatingPostNormEntity);

            postNormPicoPlate = (StaticPlate) picoPlatingPostNormEntity.getTargetLabVessels().iterator().next();


            return this;

        }
    }

    /**
     * Refer to {@link SamplesPicoEndToEndTest} for expanded version
     * <p/>
     * TODO SGM:  Merge to lesen code duplication
     */
    public static class PicoPlatingJaxbBuilder {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final String testPrefix;
        private final List<String> tubeBarcodes;
        private String rackBarcode;

        private PlateEventType picoPlatingBucket;
        private PlateTransferEventType picoPlatingQc;
        private PlateTransferEventType picoPlatingSetup1;
        private PlateTransferEventType picoPlatingSetup2;
        private PlateTransferEventType picoPlatingSetup3;
        private PlateTransferEventType picoPlatingSetup4;
        private PlateTransferEventType picoPlatingNormalizaion;
        private PlateTransferEventType picoPlatingPostNormSetup;


        private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
        private String picoPlatingQcBarcode;
        private String picoPlatingSetup1Barcode;
        private String picoPlatingSetup2Barcode;
        private String picoPlatingSetup3Barcode;
        private String picoPlatingSetup4Barcode;
        private String picoPlatingNormalizaionBarcode;
        private String picoPlatingPostNormSetupBarcode;
        private List<String> picoPlateNormBarcodes;

        public PicoPlatingJaxbBuilder(String rackBarcode, List<String> tubeBarcodes, String testPrefix,
                                      BettaLimsMessageFactory bettaLimsMessageFactory) {
            this.rackBarcode = rackBarcode;
            this.tubeBarcodes = tubeBarcodes;
            this.testPrefix = testPrefix;
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
        }

        public String getTestPrefix() {
            return testPrefix;
        }

        public List<String> getTubeBarcodes() {
            return tubeBarcodes;
        }

        public String getRackBarcode() {
            return rackBarcode;
        }

        public PlateTransferEventType getPicoPlatingQc() {
            return picoPlatingQc;
        }

        public PlateTransferEventType getPicoPlatingSetup1() {
            return picoPlatingSetup1;
        }

        public PlateTransferEventType getPicoPlatingSetup2() {
            return picoPlatingSetup2;
        }

        public PlateTransferEventType getPicoPlatingSetup3() {
            return picoPlatingSetup3;
        }

        public PlateTransferEventType getPicoPlatingSetup4() {
            return picoPlatingSetup4;
        }

        public PlateTransferEventType getPicoPlatingNormalizaion() {
            return picoPlatingNormalizaion;
        }

        public PlateTransferEventType getPicoPlatingPostNormSetup() {
            return picoPlatingPostNormSetup;
        }

        public List<BettaLIMSMessage> getMessageList() {
            return messageList;
        }

        public String getPicoPlatingNormalizaionBarcode() {
            return picoPlatingNormalizaionBarcode;
        }

        public List<String> getPicoPlateNormBarcodes() {
            return picoPlateNormBarcodes;
        }

        public PlateEventType getPicoPlatingBucket() {
            return picoPlatingBucket;
        }

        public PicoPlatingJaxbBuilder invoke() {

            picoPlatingBucket = bettaLimsMessageFactory
                    .buildRackEvent(LabEventType.PICO_PLATING_BUCKET.getName(), rackBarcode, tubeBarcodes);
            addMessage(messageList, bettaLimsMessageFactory, picoPlatingBucket);

            picoPlatingQcBarcode = LabEventType.PICO_PLATING_QC.getName() + testPrefix;
            picoPlatingQc = bettaLimsMessageFactory.buildRackToPlate(LabEventType.PICO_PLATING_QC.getName(),
                    rackBarcode, tubeBarcodes, picoPlatingQcBarcode);
            addMessage(messageList, bettaLimsMessageFactory, picoPlatingQc);


            picoPlatingSetup1Barcode = LabEventType.PICO_DILUTION_TRANSFER.getName() + testPrefix;
            picoPlatingSetup1 = bettaLimsMessageFactory.buildPlateToPlate(LabEventType.PICO_DILUTION_TRANSFER
                    .getName(), picoPlatingQcBarcode, picoPlatingSetup1Barcode);
            addMessage(messageList, bettaLimsMessageFactory, picoPlatingSetup1);

            picoPlatingSetup2Barcode = LabEventType.PICO_BUFFER_ADDITION.getName() + testPrefix;
            picoPlatingSetup2 = bettaLimsMessageFactory.buildPlateToPlate(LabEventType.PICO_BUFFER_ADDITION
                    .getName(), picoPlatingSetup1Barcode, picoPlatingSetup2Barcode);
            addMessage(messageList, bettaLimsMessageFactory, picoPlatingSetup2);

            picoPlatingSetup3Barcode = LabEventType.PICO_MICROFLUOR_TRANSFER.getName() + testPrefix;
            picoPlatingSetup3 = bettaLimsMessageFactory.buildPlateToPlate(LabEventType.PICO_MICROFLUOR_TRANSFER
                    .getName(), picoPlatingSetup2Barcode, picoPlatingSetup3Barcode);
            addMessage(messageList, bettaLimsMessageFactory, picoPlatingSetup3);

            picoPlatingSetup4Barcode = LabEventType.PICO_STANDARDS_TRANSFER.getName() + testPrefix;
            picoPlatingSetup4 = bettaLimsMessageFactory.buildPlateToPlate(LabEventType.PICO_STANDARDS_TRANSFER
                    .getName(), picoPlatingSetup3Barcode, picoPlatingSetup4Barcode);
            addMessage(messageList, bettaLimsMessageFactory, picoPlatingSetup4);

            picoPlateNormBarcodes = new ArrayList<String>();
            for (int rackPosition = 1; rackPosition <= tubeBarcodes.size() / 2; rackPosition++) {
                picoPlateNormBarcodes.add("PicoPlateNorm" + testPrefix + rackPosition);
            }
            picoPlatingNormalizaionBarcode = LabEventType.SAMPLES_NORMALIZATION_TRANSFER.getName() + testPrefix;
            picoPlatingNormalizaion = bettaLimsMessageFactory.buildRackToRack(LabEventType
                    .SAMPLES_NORMALIZATION_TRANSFER
                    .getName(), rackBarcode, tubeBarcodes, picoPlatingNormalizaionBarcode, picoPlateNormBarcodes);
            addMessage(messageList, bettaLimsMessageFactory, picoPlatingNormalizaion);

            picoPlatingPostNormSetupBarcode = LabEventType.PICO_PLATING_POST_NORM_PICO.getName() + testPrefix;
            picoPlatingPostNormSetup = bettaLimsMessageFactory
                    .buildRackToPlate(LabEventType.PICO_PLATING_POST_NORM_PICO
                            .getName(), picoPlatingNormalizaionBarcode, picoPlateNormBarcodes, picoPlatingPostNormSetupBarcode);
            addMessage(messageList, bettaLimsMessageFactory, picoPlatingPostNormSetup);

            return this;
        }
    }

    /**
     * Builds entity graph for Shearing events
     */
    public static class ShearingEntityBuilder {
        private final Map<String, TwoDBarcodedTube> mapBarcodeToTube;
        private TubeFormation preflightRack;
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;
        private final String rackBarcode;
        private String shearPlateBarcode;
        private StaticPlate shearingPlate;
        private String shearCleanPlateBarcode;
        private StaticPlate shearingCleanupPlate;

        public ShearingEntityBuilder(Map<String, TwoDBarcodedTube> mapBarcodeToTube, TubeFormation preflightRack,
                BettaLimsMessageFactory bettaLimsMessageFactory, LabEventFactory labEventFactory,
                LabEventHandler labEventHandler, String rackBarcode) {
            this.mapBarcodeToTube = mapBarcodeToTube;
            this.preflightRack = preflightRack;
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
            this.rackBarcode = rackBarcode;
        }

        public StaticPlate getShearingPlate() {
            return shearingPlate;
        }

        public String getShearCleanPlateBarcode() {
            return shearCleanPlateBarcode;
        }

        public StaticPlate getShearingCleanupPlate() {
            return shearingCleanupPlate;
        }

        public ShearingEntityBuilder invoke() {
            ShearingJaxbBuilder shearingJaxbBuilder = new ShearingJaxbBuilder(bettaLimsMessageFactory,
                    new ArrayList<String>(
                            mapBarcodeToTube.keySet()), "",
                    rackBarcode).invoke();
            shearPlateBarcode = shearingJaxbBuilder.getShearPlateBarcode();
            shearCleanPlateBarcode = shearingJaxbBuilder.getShearCleanPlateBarcode();

            // ShearingTransfer
            validateWorkflow("ShearingTransfer", mapBarcodeToTube.values());
            LabEvent shearingTransferEventEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    shearingJaxbBuilder.getShearingTransferEventJaxb(), preflightRack, null);
            labEventHandler.processEvent(shearingTransferEventEntity);
            // asserts
            shearingPlate = (StaticPlate) shearingTransferEventEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(shearingPlate.getSampleInstances().size(), mapBarcodeToTube.size(),
                    "Wrong number of sample instances");

            // PostShearingTransferCleanup
            validateWorkflow("PostShearingTransferCleanup", shearingPlate);
            LabEvent postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    shearingJaxbBuilder.getPostShearingTransferCleanupEventJaxb(), shearingPlate, null);
            labEventHandler.processEvent(postShearingTransferCleanupEntity);
            // asserts
            shearingCleanupPlate =
                    (StaticPlate) postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(shearingCleanupPlate.getSampleInstances().size(), mapBarcodeToTube.size(),
                    "Wrong number of sample instances");
            Set<SampleInstance> sampleInstancesInWell =
                    shearingCleanupPlate.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(sampleInstancesInWell.size(), 1, "Wrong number of sample instances in well");
            Assert.assertEquals(sampleInstancesInWell.iterator().next().getStartingSample().getSampleKey(),
                    mapBarcodeToTube.values().iterator().next().getSampleInstances().iterator().next()
                            .getStartingSample().getSampleKey(), "Wrong sample");

            // ShearingQC
            validateWorkflow("ShearingQC", shearingCleanupPlate);
            LabEvent shearingQcEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    shearingJaxbBuilder.getShearingQcEventJaxb(), shearingCleanupPlate, null);
            labEventHandler.processEvent(shearingQcEntity);
            return this;
        }

    }

    /**
     * Builds JAXB objects for Shearing messages
     */
    public static class ShearingJaxbBuilder {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final List<String> tubeBarcodeList;
        private final String testPrefix;
        private final String rackBarcode;
        private String shearPlateBarcode;
        private String shearCleanPlateBarcode;

        private PlateTransferEventType shearingTransferEventJaxb;
        private PlateTransferEventType postShearingTransferCleanupEventJaxb;
        private PlateTransferEventType shearingQcEventJaxb;
        private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();

        public ShearingJaxbBuilder(BettaLimsMessageFactory bettaLimsMessageFactory, List<String> tubeBarcodeList,
                String testPrefix, String rackBarcode) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.tubeBarcodeList = tubeBarcodeList;
            this.testPrefix = testPrefix;
            this.rackBarcode = rackBarcode;
        }

        public PlateTransferEventType getShearingTransferEventJaxb() {
            return shearingTransferEventJaxb;
        }

        public PlateTransferEventType getPostShearingTransferCleanupEventJaxb() {
            return postShearingTransferCleanupEventJaxb;
        }

        public PlateTransferEventType getShearingQcEventJaxb() {
            return shearingQcEventJaxb;
        }

        public String getShearPlateBarcode() {
            return shearPlateBarcode;
        }

        public String getShearCleanPlateBarcode() {
            return shearCleanPlateBarcode;
        }

        public List<BettaLIMSMessage> getMessageList() {
            return messageList;
        }

        public ShearingJaxbBuilder invoke() {
            shearPlateBarcode = "ShearPlate" + testPrefix;
            shearingTransferEventJaxb = bettaLimsMessageFactory.buildRackToPlate("ShearingTransfer", rackBarcode,
                    tubeBarcodeList, shearPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, shearingTransferEventJaxb);

            shearCleanPlateBarcode = "ShearCleanPlate" + testPrefix;
            postShearingTransferCleanupEventJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PostShearingTransferCleanup", shearPlateBarcode, shearCleanPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, postShearingTransferCleanupEventJaxb);

            String shearQcPlateBarcode = "ShearQcPlate" + testPrefix;
            shearingQcEventJaxb = bettaLimsMessageFactory.buildPlateToPlate("ShearingQC", shearCleanPlateBarcode,
                    shearQcPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, shearingQcEventJaxb);

            return this;
        }
    }

    /**
     * Builds entity graph for Shearing events
     */
    public static class ExomeExpressShearingEntityBuilder {
        private final Map<String, TwoDBarcodedTube> mapBarcodeToTube;
        private TubeFormation preflightRack;
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;
        private final String rackBarcode;
        private String shearPlateBarcode;
        private StaticPlate shearingPlate;
        private String shearCleanPlateBarcode;
        private StaticPlate shearingCleanupPlate;

        public ExomeExpressShearingEntityBuilder(Map<String, TwoDBarcodedTube> mapBarcodeToTube,
                                                 TubeFormation preflightRack,
                                                 BettaLimsMessageFactory bettaLimsMessageFactory,
                                                 LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                                 String rackBarcode) {
            this.mapBarcodeToTube = mapBarcodeToTube;
            this.preflightRack = preflightRack;
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
            this.rackBarcode = rackBarcode;
        }

        public void addProductOrderToMap(ProductOrder pdo) {
            mapKeyToProductOrder.put(pdo.getJiraTicketKey(), pdo);
        }

        public StaticPlate getShearingPlate() {
            return shearingPlate;
        }

        public String getShearCleanPlateBarcode() {
            return shearCleanPlateBarcode;
        }

        public StaticPlate getShearingCleanupPlate() {
            return shearingCleanupPlate;
        }

        public ExomeExpressShearingEntityBuilder invoke() {
            ExomeExpressShearingJaxbBuilder exomeExpressShearingJaxbBuilder = new ExomeExpressShearingJaxbBuilder(
                    bettaLimsMessageFactory, new ArrayList<String>(mapBarcodeToTube.keySet()), "", rackBarcode)
                    .invoke();


            shearPlateBarcode = exomeExpressShearingJaxbBuilder.getShearPlateBarcode();
            shearCleanPlateBarcode = exomeExpressShearingJaxbBuilder.getShearCleanPlateBarcode();

//            validateWorkflow(LabEventType.SHEARING_BUCKET.getName(), mapBarcodeToTube.values());
//            LabEvent shearingBucketEntity =
//                    labEventFactory.buildFromBettaLimsRackEventDbFree(
//                            exomeExpressShearingJaxbBuilder.getExExShearingBucket(), null, mapBarcodeToTube, null);
//            labEventHandler.processEvent(shearingBucketEntity);

            validateWorkflow("ShearingTransfer", mapBarcodeToTube.values());
            LabEvent shearingTransferEventEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                          exomeExpressShearingJaxbBuilder.getShearTransferEventJaxb(), preflightRack, null);
            labEventHandler.processEvent(shearingTransferEventEntity);
            // asserts
            shearingPlate = (StaticPlate) shearingTransferEventEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(shearingPlate.getSampleInstances().size(), mapBarcodeToTube.size(),
                    "Wrong number of sample instances");


            // Covaris Load
//            validateWorkflow("CovarisLoaded", shearingPlate);
            validateWorkflow("CovarisLoaded", mapBarcodeToTube.values());
            LabEvent covarisLoadedEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                                exomeExpressShearingJaxbBuilder.getCovarisLoadEventJaxb(), shearingPlate);
            labEventHandler.processEvent(covarisLoadedEntity);
            // asserts

            Set<SampleInstance> sampleInstancesInWell =
                    shearingPlate.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(sampleInstancesInWell.size(), 1, "Wrong number of sample instances in well");


            // PostShearingTransferCleanup
            validateWorkflow("PostShearingTransferCleanup", shearingPlate);
            LabEvent postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    exomeExpressShearingJaxbBuilder.getPostShearingTransferCleanupEventJaxb(), shearingPlate, null);
            labEventHandler.processEvent(postShearingTransferCleanupEntity);
            // asserts
            shearingCleanupPlate =
                    (StaticPlate) postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(shearingCleanupPlate.getSampleInstances().size(), mapBarcodeToTube.size(),
                    "Wrong number of sample instances");
            Set<SampleInstance> sampleInstancesInWell2 =
                    shearingCleanupPlate.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(sampleInstancesInWell2.size(), 1, "Wrong number of sample instances in well");

            // ShearingQC
            validateWorkflow("ShearingQC", shearingCleanupPlate);
            LabEvent shearingQcEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    exomeExpressShearingJaxbBuilder.getShearingQcEventJaxb(), shearingCleanupPlate, null);
            labEventHandler.processEvent(shearingQcEntity);
            return this;
        }

    }

    /**
     * Builds JAXB objects for Shearing messages
     */
    public static class ExomeExpressShearingJaxbBuilder {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final List<String> tubeBarcodeList;
        private final String testPrefix;
        private final String rackBarcode;
        private String shearPlateBarcode;
        private String shearCleanPlateBarcode;
        private String covarisRackBarCode;

        private PlateEventType exExShearingBucket;
        private PlateTransferEventType shearTransferEventJaxb;
        private PlateEventType covarisLoadEventJaxb;
        private PlateTransferEventType postShearingTransferCleanupEventJaxb;
        private PlateTransferEventType shearingQcEventJaxb;
        private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();

        public ExomeExpressShearingJaxbBuilder(BettaLimsMessageFactory bettaLimsMessageFactory,
                                               List<String> tubeBarcodeList, String testPrefix, String rackBarcode) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.tubeBarcodeList = tubeBarcodeList;
            this.testPrefix = testPrefix;
            this.rackBarcode = rackBarcode;
        }

        //
        public PlateTransferEventType getShearTransferEventJaxb() {
            return shearTransferEventJaxb;
        }

        public PlateEventType getCovarisLoadEventJaxb() {
            return covarisLoadEventJaxb;
        }

        public String getCovarisRackBarCode() {
            return covarisRackBarCode;
        }

        public PlateTransferEventType getPostShearingTransferCleanupEventJaxb() {
            return postShearingTransferCleanupEventJaxb;
        }

        public PlateTransferEventType getShearingQcEventJaxb() {
            return shearingQcEventJaxb;
        }

        public String getShearPlateBarcode() {
            return shearPlateBarcode;
        }

        public String getShearCleanPlateBarcode() {
            return shearCleanPlateBarcode;
        }

        public List<BettaLIMSMessage> getMessageList() {
            return messageList;
        }

        public PlateEventType getExExShearingBucket() {
            return exExShearingBucket;
        }

        public ExomeExpressShearingJaxbBuilder invoke() {

            exExShearingBucket =
                    bettaLimsMessageFactory
                            .buildRackEvent(LabEventType.SHEARING_BUCKET.getName(), rackBarcode, tubeBarcodeList);
            addMessage(messageList, bettaLimsMessageFactory, exExShearingBucket);

            shearPlateBarcode = "ShearPlate" + testPrefix;
            shearTransferEventJaxb = bettaLimsMessageFactory.buildRackToPlate(LabEventType.SHEARING_TRANSFER.getName(), rackBarcode,
                    tubeBarcodeList, shearPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, shearTransferEventJaxb);

            covarisLoadEventJaxb =
                    bettaLimsMessageFactory.buildPlateEvent(LabEventType.COVARIS_LOADED.getName(), shearPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, covarisLoadEventJaxb);

            shearCleanPlateBarcode = "ShearCleanPlate" + testPrefix;
            postShearingTransferCleanupEventJaxb = bettaLimsMessageFactory.buildPlateToPlate(LabEventType.POST_SHEARING_TRANSFER_CLEANUP.getName()
                    , shearPlateBarcode, shearCleanPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, postShearingTransferCleanupEventJaxb);

            String shearQcPlateBarcode = "ShearQcPlate" + testPrefix;
            shearingQcEventJaxb = bettaLimsMessageFactory.buildPlateToPlate(LabEventType.SHEARING_QC.getName(), shearCleanPlateBarcode,
                    shearQcPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, shearingQcEventJaxb);

            return this;
        }
    }

    /**
     * Builds entity graph for Library Construction events
     */
    public static class LibraryConstructionEntityBuilder {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;
        private final StaticPlate shearingPlate;
        private final String shearCleanPlateBarcode;
        private final StaticPlate shearingCleanupPlate;
        private String pondRegRackBarcode;
        private List<String> pondRegTubeBarcodes;
        private TubeFormation pondRegRack;
        private int numSamples;

        public LibraryConstructionEntityBuilder(BettaLimsMessageFactory bettaLimsMessageFactory,
                LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                StaticPlate shearingCleanupPlate, String shearCleanPlateBarcode,
                StaticPlate shearingPlate, int numSamples) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
            this.shearingCleanupPlate = shearingCleanupPlate;
            this.shearCleanPlateBarcode = shearCleanPlateBarcode;
            this.shearingPlate = shearingPlate;
            this.numSamples = numSamples;
        }

        public List<String> getPondRegTubeBarcodes() {
            return pondRegTubeBarcodes;
        }

        public String getPondRegRackBarcode() {
            return pondRegRackBarcode;
        }

        public TubeFormation getPondRegRack() {
            return pondRegRack;
        }

        public LibraryConstructionEntityBuilder invoke() {
            LibraryConstructionJaxbBuilder libraryConstructionJaxbBuilder = new LibraryConstructionJaxbBuilder(
                    bettaLimsMessageFactory, "", shearCleanPlateBarcode, "IndexPlate", numSamples).invoke();
            pondRegRackBarcode = libraryConstructionJaxbBuilder.getPondRegRackBarcode();
            pondRegTubeBarcodes = libraryConstructionJaxbBuilder.getPondRegTubeBarcodes();

            // EndRepair
            validateWorkflow("EndRepair", shearingCleanupPlate);
            LabEvent endRepairEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    libraryConstructionJaxbBuilder.getEndRepairJaxb(), shearingCleanupPlate);
            labEventHandler.processEvent(endRepairEntity);

            // EndRepairCleanup
            validateWorkflow("EndRepairCleanup", shearingCleanupPlate);
            LabEvent endRepairCleanupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    libraryConstructionJaxbBuilder.getEndRepairCleanupJaxb(), shearingCleanupPlate);
            labEventHandler.processEvent(endRepairCleanupEntity);

            // ABase
            validateWorkflow("ABase", shearingCleanupPlate);
            LabEvent aBaseEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    libraryConstructionJaxbBuilder.getaBaseJaxb(), shearingCleanupPlate);
            labEventHandler.processEvent(aBaseEntity);

            // ABaseCleanup
            validateWorkflow("ABaseCleanup", shearingCleanupPlate);
            LabEvent aBaseCleanupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    libraryConstructionJaxbBuilder.getaBaseCleanupJaxb(), shearingCleanupPlate);
            labEventHandler.processEvent(aBaseCleanupEntity);

            // IndexedAdapterLigation
            validateWorkflow("IndexedAdapterLigation", shearingCleanupPlate);
            BuildIndexPlate buildIndexPlate = new BuildIndexPlate(libraryConstructionJaxbBuilder.getIndexPlateBarcode())
                    .invoke(null);
            StaticPlate indexPlate = buildIndexPlate.getIndexPlate();
            LabEvent indexedAdapterLigationEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    libraryConstructionJaxbBuilder.getIndexedAdapterLigationJaxb(), indexPlate, shearingCleanupPlate);
            labEventHandler.processEvent(indexedAdapterLigationEntity);
            // asserts
            Set<SampleInstance> postIndexingSampleInstances =
                    shearingCleanupPlate.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
            SampleInstance sampleInstance = postIndexingSampleInstances.iterator().next();
            MolecularIndexReagent molecularIndexReagent =
                    (MolecularIndexReagent) sampleInstance.getReagents().iterator().next();
            Assert.assertEquals(molecularIndexReagent.getMolecularIndexingScheme().getName(), "Illumina_P7-M",
                    "Wrong index");

            // AdapterLigationCleanup
            validateWorkflow("AdapterLigationCleanup", shearingCleanupPlate);
            LabEvent ligationCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    libraryConstructionJaxbBuilder.getLigationCleanupJaxb(), shearingCleanupPlate, null);
            labEventHandler.processEvent(ligationCleanupEntity);
            StaticPlate ligationCleanupPlate =
                    (StaticPlate) ligationCleanupEntity.getTargetLabVessels().iterator().next();

            // PondEnrichment
            validateWorkflow("PondEnrichment", ligationCleanupPlate);
            LabEvent pondEnrichmentEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    libraryConstructionJaxbBuilder.getPondEnrichmentJaxb(), ligationCleanupPlate);
            labEventHandler.processEvent(pondEnrichmentEntity);

            // HybSelPondEnrichmentCleanup
            validateWorkflow("HybSelPondEnrichmentCleanup", ligationCleanupPlate);
            LabEvent pondCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    libraryConstructionJaxbBuilder.getPondCleanupJaxb(), ligationCleanupPlate, null);
            labEventHandler.processEvent(pondCleanupEntity);
            StaticPlate pondCleanupPlate = (StaticPlate) pondCleanupEntity.getTargetLabVessels().iterator().next();

            // PondRegistration
            validateWorkflow("PondRegistration", pondCleanupPlate);
            Map<String, TwoDBarcodedTube> mapBarcodeToPondRegTube = new HashMap<String, TwoDBarcodedTube>();
            LabEvent pondRegistrationEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(
                    libraryConstructionJaxbBuilder.getPondRegistrationJaxb(), pondCleanupPlate, mapBarcodeToPondRegTube, null);
            labEventHandler.processEvent(pondRegistrationEntity);
            // asserts
            pondRegRack = (TubeFormation) pondRegistrationEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(pondRegRack.getSampleInstances().size(),
                    shearingPlate.getSampleInstances().size(), "Wrong number of sample instances");
            Set<SampleInstance> sampleInstancesInPondRegWell = pondRegRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(sampleInstancesInPondRegWell.size(), 1, "Wrong number of sample instances in position");
            Assert.assertEquals(sampleInstancesInPondRegWell.iterator().next().getStartingSample().getSampleKey(),
                    shearingPlate.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01).iterator().next().getStartingSample().getSampleKey(),
                    "Wrong sample");
            return this;
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
                if(molecularIndexingSchemeDao != null) {
                    testScheme = molecularIndexingSchemeDao.findSingleIndexScheme(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7, sequence);
                }
                if(testScheme == null) {
                    testScheme = new MolecularIndexingScheme(
                                            new HashMap<MolecularIndexingScheme.IndexPosition, MolecularIndex>() {{
                                                put(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7, new MolecularIndex(sequence));
                                            }});
                    if(molecularIndexingSchemeDao != null) {
                        molecularIndexingSchemeDao.persist(testScheme);
                        System.out.println(testScheme.getName() + ", " + testScheme
                                                                                 .getIndex(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7)
                                                                                 .getSequence());
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

    /**
     * Builds JAXB objects for library construction messages
     */
    public static class LibraryConstructionJaxbBuilder {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final String testPrefix;
        private final String shearCleanPlateBarcode;

        private final String indexPlateBarcode;
        private String pondRegRackBarcode;
        private List<String> pondRegTubeBarcodes;

        private PlateEventType endRepairJaxb;
        private PlateEventType endRepairCleanupJaxb;
        private PlateEventType aBaseJaxb;
        private PlateEventType aBaseCleanupJaxb;
        private PlateTransferEventType indexedAdapterLigationJaxb;
        private PlateTransferEventType ligationCleanupJaxb;
        private PlateEventType pondEnrichmentJaxb;
        private PlateTransferEventType pondCleanupJaxb;
        private PlateTransferEventType pondRegistrationJaxb;
        private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
        private int numSamples;

        public LibraryConstructionJaxbBuilder(BettaLimsMessageFactory bettaLimsMessageFactory, String testPrefix,
                String shearCleanPlateBarcode, String indexPlateBarcode, int numSamples) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.testPrefix = testPrefix;
            this.shearCleanPlateBarcode = shearCleanPlateBarcode;
            this.indexPlateBarcode = indexPlateBarcode;
            this.numSamples = numSamples;
        }

        public PlateEventType getEndRepairJaxb() {
            return endRepairJaxb;
        }

        public PlateEventType getEndRepairCleanupJaxb() {
            return endRepairCleanupJaxb;
        }

        public PlateEventType getaBaseJaxb() {
            return aBaseJaxb;
        }

        public PlateEventType getaBaseCleanupJaxb() {
            return aBaseCleanupJaxb;
        }

        public String getIndexPlateBarcode() {
            return indexPlateBarcode;
        }

        public PlateTransferEventType getIndexedAdapterLigationJaxb() {
            return indexedAdapterLigationJaxb;
        }

        public PlateTransferEventType getLigationCleanupJaxb() {
            return ligationCleanupJaxb;
        }

        public PlateEventType getPondEnrichmentJaxb() {
            return pondEnrichmentJaxb;
        }

        public PlateTransferEventType getPondCleanupJaxb() {
            return pondCleanupJaxb;
        }

        public PlateTransferEventType getPondRegistrationJaxb() {
            return pondRegistrationJaxb;
        }

        public List<BettaLIMSMessage> getMessageList() {
            return messageList;
        }

        public String getPondRegRackBarcode() {
            return pondRegRackBarcode;
        }

        public List<String> getPondRegTubeBarcodes() {
            return pondRegTubeBarcodes;
        }

        public LibraryConstructionJaxbBuilder invoke() {
            endRepairJaxb = bettaLimsMessageFactory.buildPlateEvent("EndRepair", shearCleanPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, endRepairJaxb);

            endRepairCleanupJaxb = bettaLimsMessageFactory.buildPlateEvent("EndRepairCleanup", shearCleanPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, endRepairCleanupJaxb);

            aBaseJaxb = bettaLimsMessageFactory.buildPlateEvent("ABase", shearCleanPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, aBaseJaxb);

            aBaseCleanupJaxb = bettaLimsMessageFactory.buildPlateEvent("ABaseCleanup", shearCleanPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, aBaseCleanupJaxb);

//            indexPlateBarcode = "IndexPlate" + testPrefix;
            indexedAdapterLigationJaxb = bettaLimsMessageFactory.buildPlateToPlate("IndexedAdapterLigation",
                    indexPlateBarcode,
                    shearCleanPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, indexedAdapterLigationJaxb);

            String ligationCleanupBarcode = "ligationCleanupPlate" + testPrefix;
            ligationCleanupJaxb = bettaLimsMessageFactory.buildPlateToPlate("AdapterLigationCleanup",
                    shearCleanPlateBarcode,
                    ligationCleanupBarcode);
            addMessage(messageList, bettaLimsMessageFactory, ligationCleanupJaxb);

            pondEnrichmentJaxb = bettaLimsMessageFactory.buildPlateEvent("PondEnrichment", ligationCleanupBarcode);
            addMessage(messageList, bettaLimsMessageFactory, pondEnrichmentJaxb);

            String pondCleanupBarcode = "pondCleanupPlate" + testPrefix;
            pondCleanupJaxb = bettaLimsMessageFactory.buildPlateToPlate("HybSelPondEnrichmentCleanup",
                    ligationCleanupBarcode, pondCleanupBarcode);
            addMessage(messageList, bettaLimsMessageFactory, pondCleanupJaxb);

            pondRegRackBarcode = "PondReg" + testPrefix;
            pondRegTubeBarcodes = new ArrayList<String>();
            for (int rackPosition = 1; rackPosition <= numSamples; rackPosition++) {
                pondRegTubeBarcodes.add(POND_REGISTRATION_TUBE_PREFIX + testPrefix + rackPosition);
            }
            pondRegistrationJaxb = bettaLimsMessageFactory.buildPlateToRack("PondRegistration", pondCleanupBarcode,
                    pondRegRackBarcode, pondRegTubeBarcodes);
            addMessage(messageList, bettaLimsMessageFactory, pondRegistrationJaxb);

            return this;
        }
    }

    /**
     * Builds entity graph for Hybrid Selection events
     */
    public static class HybridSelectionEntityBuilder {
        public static final String BAIT_DESIGN_NAME = "cancer_2000gene_shift170_undercovered";
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;
        private final TubeFormation pondRegRack;
        private final String pondRegRackBarcode;
        private final List<String> pondRegTubeBarcodes;
        private String normCatchRackBarcode;
        private List<String> normCatchBarcodes;
        private Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes;
        private TubeFormation normCatchRack;


        public HybridSelectionEntityBuilder(BettaLimsMessageFactory bettaLimsMessageFactory,
                LabEventFactory labEventFactory, LabEventHandler labEventHandler, TubeFormation pondRegRack,
                String pondRegRackBarcode, List<String> pondRegTubeBarcodes) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
            this.pondRegRack = pondRegRack;
            this.pondRegRackBarcode = pondRegRackBarcode;
            this.pondRegTubeBarcodes = pondRegTubeBarcodes;
        }

        public List<String> getNormCatchBarcodes() {
            return normCatchBarcodes;
        }

        public String getNormCatchRackBarcode() {
            return normCatchRackBarcode;
        }

        public Map<String, TwoDBarcodedTube> getMapBarcodeToNormCatchTubes() {
            return mapBarcodeToNormCatchTubes;
        }

        public TubeFormation getNormCatchRack() {
            return normCatchRack;
        }

        public HybridSelectionEntityBuilder invoke() {
            HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = new HybridSelectionJaxbBuilder(
                    bettaLimsMessageFactory, "", pondRegRackBarcode, pondRegTubeBarcodes, "Bait").invoke();
            normCatchRackBarcode = hybridSelectionJaxbBuilder.getNormCatchRackBarcode();
            normCatchBarcodes = hybridSelectionJaxbBuilder.getNormCatchBarcodes();

            // PreSelectionPool - rearray left half of pond rack into left half of a new rack,
            // rearray right half of pond rack into left half of a new rack, then transfer these
            // two racks into a third rack, making a 2-plex pool.
            validateWorkflow("PreSelectionPool", pondRegRack); //todo jmt should be mapBarcodeToPondRegTube.values());
            Map<String, TwoDBarcodedTube> mapBarcodeToPreSelPoolTube = new HashMap<String, TwoDBarcodedTube>();
            Map<String, TwoDBarcodedTube> mapBarcodeToPondTube = new HashMap<String, TwoDBarcodedTube>();
            for (TwoDBarcodedTube twoDBarcodedTube : pondRegRack.getContainerRole().getContainedVessels()) {
                mapBarcodeToPondTube.put(twoDBarcodedTube.getLabel(), twoDBarcodedTube);
            }
            Map<String, TwoDBarcodedTube> mapBarcodeToPreSelSource1Tube = new HashMap<String, TwoDBarcodedTube>();
            for (ReceptacleType receptacleType : hybridSelectionJaxbBuilder.getPreSelPoolJaxb().getSourcePositionMap()
                    .getReceptacle()) {
                mapBarcodeToPreSelSource1Tube.put(receptacleType.getBarcode(), mapBarcodeToPondTube.get(
                        receptacleType.getBarcode()));
            }
            Map<String, TwoDBarcodedTube> mapBarcodeToPreSelSource2Tube = new HashMap<String, TwoDBarcodedTube>();
            for (ReceptacleType receptacleType : hybridSelectionJaxbBuilder.getPreSelPoolJaxb2().getSourcePositionMap()
                    .getReceptacle()) {
                mapBarcodeToPreSelSource2Tube.put(receptacleType.getBarcode(), mapBarcodeToPondTube.get(
                        receptacleType.getBarcode()));
            }
            LabEvent preSelPoolEntity = labEventFactory.buildFromBettaLimsRackToRackDbFree(hybridSelectionJaxbBuilder
                    .getPreSelPoolJaxb(),
                    mapBarcodeToPreSelSource1Tube, null, mapBarcodeToPreSelPoolTube, null);
            labEventHandler.processEvent(preSelPoolEntity);
            TubeFormation preSelPoolRack = (TubeFormation) preSelPoolEntity.getTargetLabVessels().iterator().next();
            LabEvent preSelPoolEntity2 = labEventFactory.buildFromBettaLimsRackToRackDbFree(
                    hybridSelectionJaxbBuilder.getPreSelPoolJaxb2(), mapBarcodeToPreSelSource2Tube, null, preSelPoolRack);
            labEventHandler.processEvent(preSelPoolEntity2);
            //asserts
            Set<SampleInstance> preSelPoolSampleInstances = preSelPoolRack.getSampleInstances();
            Assert.assertEquals(preSelPoolSampleInstances.size(), pondRegRack.getSampleInstances().size(),
                    "Wrong number of sample instances");
            Set<String> sampleNames = new HashSet<String>();
            for (SampleInstance preSelPoolSampleInstance : preSelPoolSampleInstances) {
                if (!sampleNames.add(preSelPoolSampleInstance.getStartingSample().getSampleKey())) {
                    Assert.fail("Duplicate sample " + preSelPoolSampleInstance.getStartingSample().getSampleKey());
                }
            }
            Set<SampleInstance> sampleInstancesInPreSelPoolWell =
                    preSelPoolRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(sampleInstancesInPreSelPoolWell.size(), 2,
                    "Wrong number of sample instances in position");

            // Hybridization
            validateWorkflow("Hybridization", preSelPoolRack);
            LabEvent hybridizationEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    hybridSelectionJaxbBuilder.getHybridizationJaxb(), preSelPoolRack, null);
            labEventHandler.processEvent(hybridizationEntity);
            StaticPlate hybridizationPlate = (StaticPlate) hybridizationEntity.getTargetLabVessels().iterator().next();

            // BaitSetup
            ReagentDesign baitDesign =
                    new ReagentDesign(BAIT_DESIGN_NAME, ReagentType.BAIT);

            TwoDBarcodedTube baitTube = buildBaitTube(hybridSelectionJaxbBuilder.getBaitTubeBarcode(), baitDesign);
            LabEvent baitSetupEntity = labEventFactory.buildVesselToSectionDbFree(
                    hybridSelectionJaxbBuilder.getBaitSetupJaxb(), baitTube, null, SBSSection.ALL96.getSectionName());
            labEventHandler.processEvent(baitSetupEntity);
            StaticPlate baitSetupPlate = (StaticPlate) baitSetupEntity.getTargetLabVessels().iterator().next();

            // BaitAddition
            validateWorkflow("BaitAddition", hybridizationPlate);
            LabEvent baitAdditionEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    hybridSelectionJaxbBuilder.getBaitAdditionJaxb(), baitSetupPlate, hybridizationPlate);
            labEventHandler.processEvent(baitAdditionEntity);

            // BeadAddition
            validateWorkflow("BeadAddition", hybridizationPlate);
            LabEvent beadAdditionEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    hybridSelectionJaxbBuilder.getBeadAdditionJaxb(), hybridizationPlate);
            labEventHandler.processEvent(beadAdditionEntity);

            // APWash
            validateWorkflow("APWash", hybridizationPlate);
            LabEvent apWashEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    hybridSelectionJaxbBuilder.getApWashJaxb(), hybridizationPlate);
            labEventHandler.processEvent(apWashEntity);

            // GSWash1
            validateWorkflow("GSWash1", hybridizationPlate);
            LabEvent gsWash1Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    hybridSelectionJaxbBuilder.getGsWash1Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash1Entity);

            // GSWash2
            validateWorkflow("GSWash2", hybridizationPlate);
            LabEvent gsWash2Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    hybridSelectionJaxbBuilder.getGsWash2Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash2Entity);

            // GSWash3
            validateWorkflow("GSWash3", hybridizationPlate);
            LabEvent gsWash3Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    hybridSelectionJaxbBuilder.getGsWash3Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash3Entity);

            // GSWash4
            validateWorkflow("GSWash4", hybridizationPlate);
            LabEvent gsWash4Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    hybridSelectionJaxbBuilder.getGsWash4Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash4Entity);

            // GSWash5
            validateWorkflow("GSWash5", hybridizationPlate);
            LabEvent gsWash5Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    hybridSelectionJaxbBuilder.getGsWash5Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash5Entity);

            // GSWash6
            validateWorkflow("GSWash6", hybridizationPlate);
            LabEvent gsWash6Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    hybridSelectionJaxbBuilder.getGsWash6Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash6Entity);

            // CatchEnrichmentSetup
            validateWorkflow("CatchEnrichmentSetup", hybridizationPlate);
            LabEvent catchEnrichmentSetupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    hybridSelectionJaxbBuilder.getCatchEnrichmentSetupJaxb(), hybridizationPlate);
            labEventHandler.processEvent(catchEnrichmentSetupEntity);

            // CatchEnrichmentCleanup
            validateWorkflow("CatchEnrichmentCleanup", hybridizationPlate);
            LabEvent catchEnrichmentCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    hybridSelectionJaxbBuilder.getCatchEnrichmentCleanupJaxb(), hybridizationPlate, null);
            labEventHandler.processEvent(catchEnrichmentCleanupEntity);
            StaticPlate catchCleanPlate =
                    (StaticPlate) catchEnrichmentCleanupEntity.getTargetLabVessels().iterator().next();

            // NormalizedCatchRegistration
            validateWorkflow("NormalizedCatchRegistration", catchCleanPlate);
            mapBarcodeToNormCatchTubes = new HashMap<String, TwoDBarcodedTube>();
            LabEvent normCatchEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(hybridSelectionJaxbBuilder
                    .getNormCatchJaxb(),
                    catchCleanPlate, mapBarcodeToNormCatchTubes, null);
            labEventHandler.processEvent(normCatchEntity);
            normCatchRack = (TubeFormation) normCatchEntity.getTargetLabVessels().iterator().next();
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

    /**
     * Builds JAXB objects for Hybrid Selection messages
     */
    public static class HybridSelectionJaxbBuilder {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final String testPrefix;
        private final String pondRegRackBarcode;
        private final List<String> pondRegTubeBarcodes;
        private String normCatchRackBarcode;
        private List<String> normCatchBarcodes;
        private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();

        private PlateTransferEventType preSelPoolJaxb;
        private PlateTransferEventType preSelPoolJaxb2;
        private PlateTransferEventType hybridizationJaxb;
        private String baitTubeBarcode;
        private ReceptaclePlateTransferEvent baitSetupJaxb;
        private PlateTransferEventType baitAdditionJaxb;
        private PlateEventType beadAdditionJaxb;
        private PlateEventType apWashJaxb;
        private PlateEventType gsWash1Jaxb;
        private PlateEventType gsWash2Jaxb;
        private PlateEventType gsWash3Jaxb;
        private PlateEventType gsWash4Jaxb;
        private PlateEventType gsWash5Jaxb;
        private PlateEventType gsWash6Jaxb;
        private PlateEventType catchEnrichmentSetupJaxb;
        private PlateTransferEventType catchEnrichmentCleanupJaxb;
        private PlateTransferEventType normCatchJaxb;

        public HybridSelectionJaxbBuilder(BettaLimsMessageFactory bettaLimsMessageFactory, String testPrefix,
                String pondRegRackBarcode, List<String> pondRegTubeBarcodes, String baitTubeBarcode) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.testPrefix = testPrefix;
            this.pondRegRackBarcode = pondRegRackBarcode;
            this.pondRegTubeBarcodes = pondRegTubeBarcodes;
            this.baitTubeBarcode = baitTubeBarcode;
        }

        public PlateTransferEventType getPreSelPoolJaxb() {
            return preSelPoolJaxb;
        }

        public PlateTransferEventType getPreSelPoolJaxb2() {
            return preSelPoolJaxb2;
        }

        public PlateTransferEventType getHybridizationJaxb() {
            return hybridizationJaxb;
        }

        public String getBaitTubeBarcode() {
            return baitTubeBarcode;
        }

        public ReceptaclePlateTransferEvent getBaitSetupJaxb() {
            return baitSetupJaxb;
        }

        public PlateTransferEventType getBaitAdditionJaxb() {
            return baitAdditionJaxb;
        }

        public PlateEventType getBeadAdditionJaxb() {
            return beadAdditionJaxb;
        }

        public PlateEventType getApWashJaxb() {
            return apWashJaxb;
        }

        public PlateEventType getGsWash1Jaxb() {
            return gsWash1Jaxb;
        }

        public PlateEventType getGsWash2Jaxb() {
            return gsWash2Jaxb;
        }

        public PlateEventType getGsWash3Jaxb() {
            return gsWash3Jaxb;
        }

        public PlateEventType getGsWash4Jaxb() {
            return gsWash4Jaxb;
        }

        public PlateEventType getGsWash5Jaxb() {
            return gsWash5Jaxb;
        }

        public PlateEventType getGsWash6Jaxb() {
            return gsWash6Jaxb;
        }

        public PlateEventType getCatchEnrichmentSetupJaxb() {
            return catchEnrichmentSetupJaxb;
        }

        public PlateTransferEventType getCatchEnrichmentCleanupJaxb() {
            return catchEnrichmentCleanupJaxb;
        }

        public PlateTransferEventType getNormCatchJaxb() {
            return normCatchJaxb;
        }

        public String getNormCatchRackBarcode() {
            return normCatchRackBarcode;
        }

        public List<String> getNormCatchBarcodes() {
            return normCatchBarcodes;
        }

        public List<BettaLIMSMessage> getMessageList() {
            return messageList;
        }

        public HybridSelectionJaxbBuilder invoke() {
            List<String> preSelPoolBarcodes = new ArrayList<String>();
            for (int rackPosition = 1; rackPosition <= pondRegTubeBarcodes.size() / 2; rackPosition++) {
                preSelPoolBarcodes.add("PreSelPool" + testPrefix + rackPosition);
            }
            String preSelPoolRackBarcode = "PreSelPool" + testPrefix;
            preSelPoolJaxb = bettaLimsMessageFactory.buildRackToRack("PreSelectionPool", pondRegRackBarcode,
                    pondRegTubeBarcodes.subList(0, pondRegTubeBarcodes
                            .size() / 2), preSelPoolRackBarcode,
                    preSelPoolBarcodes);
            addMessage(messageList, bettaLimsMessageFactory, preSelPoolJaxb);

            preSelPoolJaxb2 = bettaLimsMessageFactory.buildRackToRack("PreSelectionPool", pondRegRackBarcode,
                    pondRegTubeBarcodes.subList(
                            pondRegTubeBarcodes.size() / 2,
                            pondRegTubeBarcodes.size()),
                    preSelPoolRackBarcode, preSelPoolBarcodes);
            addMessage(messageList, bettaLimsMessageFactory, preSelPoolJaxb2);

            String hybridizationPlateBarcode = "Hybrid" + testPrefix;
            hybridizationJaxb = bettaLimsMessageFactory.buildRackToPlate("Hybridization", preSelPoolRackBarcode,
                    preSelPoolBarcodes, hybridizationPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, hybridizationJaxb);

            String baitSetupBarcode = "BaitSetup" + testPrefix;
            baitSetupJaxb = bettaLimsMessageFactory.buildTubeToPlate("BaitSetup", baitTubeBarcode, baitSetupBarcode,
                    LabEventFactory.PHYS_TYPE_EPPENDORF_96,
                    LabEventFactory.SECTION_ALL_96, "tube");
            addMessage(messageList, bettaLimsMessageFactory, baitSetupJaxb);

            baitAdditionJaxb = bettaLimsMessageFactory.buildPlateToPlate("BaitAddition", baitSetupBarcode,
                    hybridizationPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, baitAdditionJaxb);

            beadAdditionJaxb = bettaLimsMessageFactory.buildPlateEvent("BeadAddition", hybridizationPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, beadAdditionJaxb);

            apWashJaxb = bettaLimsMessageFactory.buildPlateEvent("APWash", hybridizationPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, apWashJaxb);

            gsWash1Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash1", hybridizationPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, gsWash1Jaxb);

            gsWash2Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash2", hybridizationPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, gsWash2Jaxb);

            gsWash3Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash3", hybridizationPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, gsWash3Jaxb);

            gsWash4Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash4", hybridizationPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, gsWash4Jaxb);

            gsWash5Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash5", hybridizationPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, gsWash5Jaxb);

            gsWash6Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash6", hybridizationPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, gsWash6Jaxb);

            catchEnrichmentSetupJaxb = bettaLimsMessageFactory.buildPlateEvent("CatchEnrichmentSetup",
                    hybridizationPlateBarcode);
            addMessage(messageList, bettaLimsMessageFactory, catchEnrichmentSetupJaxb);

            String catchCleanupBarcode = "catchCleanPlate" + testPrefix;
            catchEnrichmentCleanupJaxb = bettaLimsMessageFactory.buildPlateToPlate("CatchEnrichmentCleanup",
                    hybridizationPlateBarcode,
                    catchCleanupBarcode);
            addMessage(messageList, bettaLimsMessageFactory, catchEnrichmentCleanupJaxb);

            normCatchBarcodes = new ArrayList<String>();
            for (int rackPosition = 1; rackPosition <= pondRegTubeBarcodes.size() / 2; rackPosition++) {
                normCatchBarcodes.add("NormCatch" + testPrefix + rackPosition);
            }
            normCatchRackBarcode = "NormCatchRack";
            normCatchJaxb = bettaLimsMessageFactory.buildPlateToRack("NormalizedCatchRegistration",
                    hybridizationPlateBarcode, normCatchRackBarcode,
                    normCatchBarcodes);
            addMessage(messageList, bettaLimsMessageFactory, normCatchJaxb);

            return this;
        }
    }

    /**
     * Builds entity graph for Qtp events
     */
    public static class QtpEntityBuilder {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;
        private final List<TubeFormation> normCatchRacks;
        private final List<String> normCatchRackBarcodes;
        private final List<List<String>> listLcsetListNormCatchBarcodes;
        private final Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes;
        private final WorkflowName workflowName;

        private TubeFormation denatureRack;
        private IlluminaFlowcell illuminaFlowcell;
        private StripTube stripTube;

        public QtpEntityBuilder(BettaLimsMessageFactory bettaLimsMessageFactory,
                                LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                List<TubeFormation> normCatchRacks,
                                List<String >normCatchRackBarcodes, List<List<String>> listLcsetListNormCatchBarcodes,
                                Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes,
                                WorkflowName workflowName) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
            this.normCatchRacks = normCatchRacks;
            this.normCatchRackBarcodes = normCatchRackBarcodes;
            this.listLcsetListNormCatchBarcodes = listLcsetListNormCatchBarcodes;
            this.mapBarcodeToNormCatchTubes = mapBarcodeToNormCatchTubes;
            this.workflowName = workflowName;
        }

        public void invoke() {
            QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, "",
                    listLcsetListNormCatchBarcodes, normCatchRackBarcodes, workflowName).invoke();
            PlateCherryPickEvent cherryPickJaxb = qtpJaxbBuilder.getPoolingTransferJaxb();
            final String poolRackBarcode = qtpJaxbBuilder.getPoolRackBarcode();
            PlateCherryPickEvent denatureJaxb = qtpJaxbBuilder.getDenatureJaxb();
            final String denatureRackBarcode = qtpJaxbBuilder.getDenatureRackBarcode();
            PlateCherryPickEvent stripTubeTransferJaxb = qtpJaxbBuilder.getStripTubeTransferJaxb();
            final String stripTubeHolderBarcode = qtpJaxbBuilder.getStripTubeHolderBarcode();
            PlateTransferEventType flowcellTransferJaxb = qtpJaxbBuilder.getFlowcellTransferJaxb();

            ReceptacleEventType flowcellLoadJaxb = qtpJaxbBuilder.getFlowcellLoad();
            Map<String, TwoDBarcodedTube> mapBarcodeToPoolTube = null;

            int i = 0;
            List<TwoDBarcodedTube> poolTubes = new ArrayList<TwoDBarcodedTube>();
            List<TubeFormation> poolingRacks = new ArrayList<TubeFormation>();
            for (final TubeFormation normCatchRack : normCatchRacks) {
                // PoolingTransfer
                validateWorkflow("PoolingTransfer", normCatchRack);
                mapBarcodeToPoolTube = new HashMap<String, TwoDBarcodedTube>();
                final int finalI = i;
                LabEvent poolingEntity = labEventFactory.buildCherryPickRackToRackDbFree(cherryPickJaxb,
                        new HashMap<String, TubeFormation>() {{
                            put(normCatchRackBarcodes.get(finalI), normCatchRack);
                        }},
                        new HashMap<String, RackOfTubes>(),
                        mapBarcodeToNormCatchTubes,
                        new HashMap<String, TubeFormation>() {{
                            put(poolRackBarcode, null);
                        }}, mapBarcodeToPoolTube, null
                );
                labEventHandler.processEvent(poolingEntity);
                // asserts
                TubeFormation poolingRack = (TubeFormation) poolingEntity.getTargetLabVessels().iterator().next();
                poolingRacks.add(poolingRack);
                poolTubes.add(poolingRack.getContainerRole().getVesselAtPosition(VesselPosition.A01));
                Set<SampleInstance> pooledSampleInstances = poolingRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
                Assert.assertEquals(pooledSampleInstances.size(), normCatchRack.getSampleInstances().size(), "Wrong number of pooled samples");
                i++;
            }

            TubeFormation rearrayedPoolingRack;
            if (poolTubes.size() > 1) {
                Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new HashMap<VesselPosition, TwoDBarcodedTube>();
                for (int j = 0; j < poolTubes.size(); j++) {
                    mapPositionToTube.put(VesselPosition.getByName(bettaLimsMessageFactory.buildWellName(j + 1)),
                            poolTubes.get(j));
                }
                rearrayedPoolingRack = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
                rearrayedPoolingRack.addRackOfTubes(new RackOfTubes("poolRearray", RackOfTubes.RackType.Matrix96));
            } else {
                rearrayedPoolingRack = poolingRacks.get(0);
            }

            // DenatureTransfer
            validateWorkflow("DenatureTransfer", rearrayedPoolingRack);
            Map<String, TwoDBarcodedTube> mapBarcodeToDenatureTube = new HashMap<String, TwoDBarcodedTube>();
            Map<String, TubeFormation> mapBarcodeToPoolRack = new HashMap<String, TubeFormation>();
            mapBarcodeToPoolRack.put(poolRackBarcode, rearrayedPoolingRack);
            LabEvent denatureEntity = labEventFactory.buildCherryPickRackToRackDbFree(denatureJaxb,
                    mapBarcodeToPoolRack,
                    new HashMap<String, RackOfTubes>(),
                    mapBarcodeToPoolTube,
                    new HashMap<String, TubeFormation>() {{
                        put(denatureRackBarcode, null);
                    }}, mapBarcodeToDenatureTube, null
            );
            labEventHandler.processEvent(denatureEntity);
            // asserts
            denatureRack = (TubeFormation) denatureEntity.getTargetLabVessels().iterator().next();
            Set<SampleInstance> denaturedSampleInstances = denatureRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
            int catchSampleInstanceCount = 0;
            for (TubeFormation normCatchRack : normCatchRacks) {
                catchSampleInstanceCount += normCatchRack.getSampleInstances().size();
            }

            Assert.assertEquals(denaturedSampleInstances.size(), catchSampleInstanceCount, "Wrong number of denatured samples");
            Assert.assertEquals(denatureRack.getContainerRole().getVesselAtPosition(VesselPosition.A01).getSampleInstances().size(),
                    catchSampleInstanceCount, "Wrong number of denatured samples");
            LabEvent flowcellTransferEntity;
            // StripTubeBTransfer
            if (workflowName != WorkflowName.EXOME_EXPRESS) {
                validateWorkflow("StripTubeBTransfer", denatureRack);

                Map<String, StripTube> mapBarcodeToStripTube = new HashMap<String, StripTube>();
                LabEvent stripTubeTransferEntity =
                        labEventFactory.buildCherryPickRackToStripTubeDbFree(stripTubeTransferJaxb,
                                new HashMap<String, TubeFormation>() {{
                                    put(denatureRackBarcode, denatureRack);
                                }},
                                mapBarcodeToDenatureTube,
                                new HashMap<String, TubeFormation>() {{
                                    put(stripTubeHolderBarcode, null);
                                }},
                                mapBarcodeToStripTube, new HashMap<String, RackOfTubes>()
                        );
                labEventHandler.processEvent(stripTubeTransferEntity);
                // asserts
                stripTube = (StripTube) stripTubeTransferEntity.getTargetLabVessels().iterator().next();
                Assert.assertEquals(
                        stripTube.getContainerRole().getSampleInstancesAtPosition(VesselPosition.TUBE1).size(),
                        catchSampleInstanceCount,
                        "Wrong number of samples in strip tube well");

                // FlowcellTransfer
                validateWorkflow("FlowcellTransfer", stripTube);
                flowcellTransferEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(flowcellTransferJaxb,
                        stripTube, null);
                labEventHandler.processEvent(flowcellTransferEntity);
                //asserts
                illuminaFlowcell = (IlluminaFlowcell) flowcellTransferEntity.getTargetLabVessels().iterator().next();
                Set<SampleInstance> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                        VesselPosition.LANE1);
                Assert.assertEquals(lane1SampleInstances.size(), normCatchRacks.get(0).getSampleInstances().size(),
                        "Wrong number of samples in flowcell lane");
                //FlowcellLoaded
                validateWorkflow(LabEventType.FLOWCELL_LOADED.getName(), illuminaFlowcell);
                LabEvent flowcellLoadEntity = labEventFactory
                        .buildReceptacleEventDbFree(flowcellLoadJaxb, illuminaFlowcell);
                labEventHandler.processEvent(flowcellLoadEntity);
            }
        }

        public TubeFormation getDenatureRack() {
            return denatureRack;
        }

        public IlluminaFlowcell getIlluminaFlowcell() {
            return illuminaFlowcell;
        }

        public StripTube getStripTube() {
            return stripTube;
        }
    }

/*
    public static class HiSeqEntityBuilder {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;
        private final StripTube stripTube;

        private IlluminaFlowcell illuminaFlowcell;

        public HiSeqJaxbBuilder(BettaLimsMessageFactory bettaLimsMessageFactory,
                                LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                StripTube stripTube) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
            this.stripTube = stripTube;
        }
    }
*/

    public static class HiSeq2500FlowcellEntityBuilder {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;
        private final String flowcellBarcode;
        private IlluminaFlowcell illuminaFlowcell;
        private LabEvent flowcellTransferEntity;
        private final TubeFormation denatureRack;

        public HiSeq2500FlowcellEntityBuilder(BettaLimsMessageFactory bettaLimsMessageFactory,
                                              LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                              TubeFormation denatureRack, String flowcellBarcode){

            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
            this.denatureRack = denatureRack;
            this.flowcellBarcode = flowcellBarcode;
        }

        public HiSeq2500FlowcellEntityBuilder invoke() {
            HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder =
                    new HiSeq2500JaxbBuilder(bettaLimsMessageFactory, "",
                            denatureRack.getContainerRole().getContainedVessels().iterator().next().getLabel())
                            .invoke();
            ReceptaclePlateTransferEvent flowcellTransferJaxb = hiSeq2500JaxbBuilder.getFlowcellTransferJaxb();

            // DenatureToFlowcellTransfer
            validateWorkflow("DenatureToFlowcellTransfer", denatureRack);
            LabEvent flowcellTransferEntity = labEventFactory.buildVesselToSectionDbFree(flowcellTransferJaxb,
                    denatureRack.getContainerRole().getContainedVessels().iterator().next(), null,
                    SBSSection.FLOWCELL2.getSectionName());
            labEventHandler.processEvent(flowcellTransferEntity);
            //asserts
            illuminaFlowcell = (IlluminaFlowcell) flowcellTransferEntity.getTargetLabVessels().iterator().next();
            Set<SampleInstance> lane1SampleInstances =
                    illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                            VesselPosition.LANE1);
            Assert.assertEquals(lane1SampleInstances.size(), denatureRack.getSampleInstances().size(),
                    "Wrong number of samples in flowcell lane");
            Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(), 2,
                    "Wrong number of reagents");
            Set<SampleInstance> lane2SampleInstances =
                    illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                            VesselPosition.LANE2);
            Assert.assertEquals(lane2SampleInstances.size(), denatureRack.getSampleInstances().size(),
                    "Wrong number of samples in flowcell lane");
            Assert.assertEquals(lane2SampleInstances.iterator().next().getReagents().size(), 2,
                    "Wrong number of reagents");

            validateWorkflow("FlowcellLoaded", illuminaFlowcell);

            ReceptacleEventType flowcellLoadJaxb =
                    bettaLimsMessageFactory.buildReceptacleEvent("FlowcellLoaded", flowcellBarcode, "Flowcell2Lane");

            LabEvent flowcellLoadEntity = labEventFactory
                    .buildReceptacleEventDbFree(flowcellLoadJaxb, illuminaFlowcell);
            labEventHandler.processEvent(flowcellLoadEntity);

            return this;
        }

        public IlluminaFlowcell getIlluminaFlowcell() {
            return illuminaFlowcell;
        }

        public LabEvent getFlowcellTransferEntity() {
            return flowcellTransferEntity;
        }

        public TubeFormation getDenatureRack() {
            return denatureRack;
        }
    }

    public static class HiSeq2500JaxbBuilder {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private String testPrefix;
        private final String denatureTubeBarcode;
        private String flowcellBarcode;

        private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
        private ReceptaclePlateTransferEvent flowcellTransferJaxb;

        public HiSeq2500JaxbBuilder(BettaLimsMessageFactory bettaLimsMessageFactory,
                                    String testPrefix, String denatureTubeBarcode) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.testPrefix = testPrefix;
            this.denatureTubeBarcode = denatureTubeBarcode;
        }

        public HiSeq2500JaxbBuilder invoke() {
            flowcellBarcode = "Flowcell" + testPrefix;
            flowcellTransferJaxb =
                    bettaLimsMessageFactory.buildTubeToPlate("DenatureToFlowcellTransfer",
                            denatureTubeBarcode, flowcellBarcode, PHYS_TYPE_FLOWCELL_2_LANE, SECTION_ALL_2, "tube");
            addMessage(messageList, bettaLimsMessageFactory, flowcellTransferJaxb);

            return this;
        }

        public ReceptaclePlateTransferEvent getFlowcellTransferJaxb() {
            return flowcellTransferJaxb;
        }

        public List<BettaLIMSMessage> getMessageList() {
            return messageList;
        }

        public String getFlowcellBarcode() {
            return flowcellBarcode;
        }
    }


    /**
     * Builds JAXB objects for QTP messages
     */
    public static class QtpJaxbBuilder {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final String testPrefix;
        private final List<List<String>> listLcsetListNormCatchBarcodes;
        private final List<String> normCatchRackBarcodes;

        private String poolRackBarcode;
        private List<String> poolTubeBarcodes = new ArrayList<String>();
        private PlateCherryPickEvent poolingTransferJaxb;
        private String denatureRackBarcode;
        private PlateCherryPickEvent denatureJaxb;
        private String stripTubeHolderBarcode;
        private PlateCherryPickEvent stripTubeTransferJaxb;
        private PlateTransferEventType flowcellTransferJaxb;
        private ReceptacleEventType flowcellLoad;
        private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
        private String flowcellBarcode;
        private String stripTubeBarcode;
        private final WorkflowName workflowName;
        private String denatureTubeBarcode;

        public QtpJaxbBuilder(BettaLimsMessageFactory bettaLimsMessageFactory, String testPrefix,
                List<List<String>> listLcsetListNormCatchBarcodes, List<String> normCatchRackBarcodes, WorkflowName workflowName) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.testPrefix = testPrefix;
            this.listLcsetListNormCatchBarcodes = listLcsetListNormCatchBarcodes;
            this.normCatchRackBarcodes = normCatchRackBarcodes;
            this.workflowName = workflowName;
        }

        public String getPoolRackBarcode() {
            return poolRackBarcode;
        }

        public List<String> getPoolTubeBarcodes() {
            return poolTubeBarcodes;
        }

        public PlateCherryPickEvent getPoolingTransferJaxb() {
            return poolingTransferJaxb;
        }

        public String getDenatureRackBarcode() {
            return denatureRackBarcode;
        }

        public PlateCherryPickEvent getDenatureJaxb() {
            return denatureJaxb;
        }

        public String getStripTubeHolderBarcode() {
            return stripTubeHolderBarcode;
        }

        public PlateCherryPickEvent getStripTubeTransferJaxb() {
            return stripTubeTransferJaxb;
        }

        public PlateTransferEventType getFlowcellTransferJaxb() {
            return flowcellTransferJaxb;
        }

        public ReceptacleEventType getFlowcellLoad() {
            return flowcellLoad;
        }

        public String getFlowcellBarcode() {
            return flowcellBarcode;
        }

        public List<BettaLIMSMessage> getMessageList() {
            return messageList;
        }

        public String getStripTubeBarcode() {
            return stripTubeBarcode;
        }

        public String getDenatureTubeBarcode() {
            return denatureTubeBarcode;
        }

        public QtpJaxbBuilder invoke() {
            int i = 0;
            for (List<String> normCatchBarcodes : listLcsetListNormCatchBarcodes) {
                // PoolingTransfer
                poolRackBarcode = "PoolRack" + testPrefix;
                List<BettaLimsMessageFactory.CherryPick> poolingCherryPicks =
                        new ArrayList<BettaLimsMessageFactory.CherryPick>();
                for (int rackPosition = 1; rackPosition <= normCatchBarcodes.size(); rackPosition++) {
                    poolingCherryPicks.add(new BettaLimsMessageFactory.CherryPick(normCatchRackBarcodes.get(i),
                            bettaLimsMessageFactory.buildWellName(rackPosition), poolRackBarcode,
                            "A01"));
                }
                poolTubeBarcodes.add("Pool" + testPrefix + i);
                poolingTransferJaxb = bettaLimsMessageFactory.buildCherryPick("PoolingTransfer",
                        Arrays.asList(normCatchRackBarcodes.get(i)), Collections.singletonList(normCatchBarcodes),
                        poolRackBarcode, Collections.singletonList(poolTubeBarcodes.get(i)), poolingCherryPicks);
                addMessage(messageList, bettaLimsMessageFactory, poolingTransferJaxb);
                i++;
            }

            // DenatureTransfer
            denatureRackBarcode = "DenatureRack" + testPrefix;
            List<BettaLimsMessageFactory.CherryPick> denatureCherryPicks = new ArrayList<BettaLimsMessageFactory.CherryPick>();
            List<String> denatureTubeBarcodes = new ArrayList<String>();
            for (int j = 0; j < poolTubeBarcodes.size(); j++) {
                denatureCherryPicks.add(new BettaLimsMessageFactory.CherryPick(poolRackBarcode,
                        bettaLimsMessageFactory.buildWellName(j + 1), denatureRackBarcode,
                        bettaLimsMessageFactory.buildWellName(j + 1)));
                denatureTubeBarcode = "DenatureTube" + testPrefix + "1";
                denatureTubeBarcodes.add(denatureTubeBarcode);
            }
            denatureJaxb = bettaLimsMessageFactory.buildCherryPick("DenatureTransfer",
                    Collections.singletonList(poolRackBarcode), Collections.singletonList(poolTubeBarcodes),
                    denatureRackBarcode, denatureTubeBarcodes, denatureCherryPicks);
            addMessage(messageList, bettaLimsMessageFactory, denatureJaxb);

            if (workflowName != WorkflowName.EXOME_EXPRESS) {
                // StripTubeBTransfer
                stripTubeHolderBarcode = "StripTubeHolder" + testPrefix;
                List<BettaLimsMessageFactory.CherryPick> stripTubeCherryPicks =
                        new ArrayList<BettaLimsMessageFactory.CherryPick>();
                for (int rackPosition = 0; rackPosition < 8; rackPosition++) {
                    stripTubeCherryPicks.add(new BettaLimsMessageFactory.CherryPick(denatureRackBarcode, "A01",
                            stripTubeHolderBarcode, Character.toString((char) ('A' + rackPosition)) + "01"));
                }
                stripTubeBarcode = "StripTube" + testPrefix + "1";

                stripTubeTransferJaxb = bettaLimsMessageFactory.buildCherryPickToStripTube("StripTubeBTransfer",
                        Arrays.asList(denatureRackBarcode),
                        Arrays.asList(denatureTubeBarcodes),
                        stripTubeHolderBarcode,
                        Arrays.asList(stripTubeBarcode),
                        stripTubeCherryPicks);
                addMessage(messageList, bettaLimsMessageFactory, stripTubeTransferJaxb);

                // FlowcellTransfer
                flowcellBarcode = "Flowcell" + testPrefix;
                flowcellTransferJaxb = bettaLimsMessageFactory.buildStripTubeToFlowcell("FlowcellTransfer",
                        stripTubeBarcode, flowcellBarcode);
                addMessage(messageList, bettaLimsMessageFactory, flowcellTransferJaxb);

                flowcellLoad = bettaLimsMessageFactory.buildReceptacleEvent(LabEventType.FLOWCELL_LOADED.getName(),
                        flowcellBarcode, LabEventFactory.PHYS_TYPE_FLOWCELL);
                addMessage(messageList, bettaLimsMessageFactory, flowcellLoad);
            }
            return this;
        }
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
            addEntry(new BucketEntry(entryVessel, testProductOrder, this));
            return super.findEntry(entryVessel);
        }
    }
}
