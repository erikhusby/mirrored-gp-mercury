package org.broadinstitute.gpinformatics.mercury.test;

//import com.jprofiler.api.agent.Controller;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.monitoring.HipChatMessageSender;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnectorProducer;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.*;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.boundary.graph.Graph;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettalimsMessageResource;
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
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.LabVesselComment;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkLevel;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkReason;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.presentation.transfervis.TransferVisualizerFrame;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PreFlightEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ShearingEntityBuilder;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
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

    private final TemplateEngine templateEngine = new TemplateEngine();

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

    @BeforeClass(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        templateEngine.postConstruct();
    }

    /**
     * Build object graph for Hybrid Selection messages, verify chain of events.
     */
    @Test(groups = {DATABASE_FREE})
    public void testHybridSelection() {
//        Controller.startCPURecording(true);

        AthenaClientService athenaClientService = AthenaClientProducer.stubInstance();

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
                ProductOrderTestFactory.buildHybridSelectionProductOrder(NUM_POSITIONS_IN_RACK);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");
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

        // Messaging
        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);


        BucketDao mockBucketDao = EasyMock.createNiceMock(BucketDao.class);
        BucketBean bucketBeanEJB = new BucketBean(labEventFactory, JiraServiceProducer.stubInstance(), labBatchEJB);
        EasyMock.replay(mockBucketDao, labBatchDAO, tubeDao, mockJira);

        LabEventHandler labEventHandler =
                new LabEventHandler(new WorkflowLoader(), AthenaClientProducer.stubInstance(), bucketBeanEJB,
                        mockBucketDao, new BSPUserList(BSPManagerFactoryProducer.stubInstance()));

        PreFlightEntityBuilder preFlightEntityBuilder = new PreFlightEntityBuilder(bettaLimsMessageTestFactory,
                labEventFactory, labEventHandler,
                mapBarcodeToTube).invoke();

        ShearingEntityBuilder shearingEntityBuilder = new ShearingEntityBuilder(mapBarcodeToTube, preFlightEntityBuilder.getTubeFormation(),
                bettaLimsMessageTestFactory, labEventFactory, labEventHandler, preFlightEntityBuilder.getRackBarcode()).invoke();

        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = new LibraryConstructionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, labEventHandler,
                shearingEntityBuilder.getShearingCleanupPlate(), shearingEntityBuilder.getShearCleanPlateBarcode(),
                shearingEntityBuilder.getShearingPlate(), NUM_POSITIONS_IN_RACK).invoke();

        HybridSelectionEntityBuilder hybridSelectionEntityBuilder = new HybridSelectionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, labEventHandler,
                libraryConstructionEntityBuilder.getPondRegRack(),
                libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                libraryConstructionEntityBuilder.getPondRegTubeBarcodes()).invoke();

        QtpEntityBuilder qtpEntityBuilder = new QtpEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, labEventHandler,
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
                new BSPSampleDataFetcher() {
                    @Override
                    public Map<String, BSPSampleDTO> fetchSamplesFromBSP(@Nonnull Collection<String> sampleNames) {
                        Map<String, BSPSampleDTO> mapSampleIdToDto = new HashMap<String, BSPSampleDTO>();
                        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
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
                    public ProductOrder retrieveProductOrderDetails(String poBusinessKey) {
                        return productOrder;
                    }

                    @Override
                    public Map<String, List<ProductOrderSample>> findMapSampleNameToPoSample(List<String> sampleNames) {
                        return null;
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


        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        String jiraTicketKey = productOrder.getBusinessKey();

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
        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory();
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

        PicoPlatingEntityBuilder pplatingEntityBuilder = new PicoPlatingEntityBuilder(bettaLimsMessageTestFactory,
                labEventFactory, labEventHandler,
                mapBarcodeToTube, rackBarcode).invoke();

        ExomeExpressShearingEntityBuilder shearingEntityBuilder =
                new ExomeExpressShearingEntityBuilder(pplatingEntityBuilder.getNormBarcodeToTubeMap(),
                        pplatingEntityBuilder.getNormTubeFormation(), bettaLimsMessageTestFactory, labEventFactory,
                        labEventHandler, pplatingEntityBuilder.getNormalizationBarcode()).invoke();

        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = new LibraryConstructionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, labEventHandler,
                shearingEntityBuilder.getShearingCleanupPlate(), shearingEntityBuilder.getShearCleanPlateBarcode(),
                shearingEntityBuilder.getShearingPlate(), NUM_POSITIONS_IN_RACK).invoke();

        HybridSelectionEntityBuilder hybridSelectionEntityBuilder = new HybridSelectionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, labEventHandler,
                libraryConstructionEntityBuilder.getPondRegRack(),
                libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                libraryConstructionEntityBuilder.getPondRegTubeBarcodes()).invoke();

        QtpEntityBuilder qtpEntityBuilder = new QtpEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, labEventHandler,
                Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchRack()),
                Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchRackBarcode()),
                Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchBarcodes()),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), WorkflowName.EXOME_EXPRESS);
        qtpEntityBuilder.invoke();

        String flowcellBarcode = "flowcell" + runDate.getTime();

        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
            new HiSeq2500FlowcellEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, labEventHandler,
                    qtpEntityBuilder.getDenatureRack(), flowcellBarcode).invoke();

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
        SolexaRunBean runBean = new SolexaRunBean(flowcellBarcode,
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

        //todo: these need to be made to assert something useful, and pass.
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


        ProductOrder productOrder = ProductOrderTestFactory.buildWholeGenomeProductOrder(NUM_POSITIONS_IN_RACK);
        String jiraTicketKey = productOrder.getBusinessKey();

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

        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);


        BucketDao mockBucketDao = EasyMock.createNiceMock(BucketDao.class);
        BucketBean bucketBeanEJB = new BucketBean(labEventFactory, JiraServiceProducer.stubInstance(), labBatchEJB);
        EasyMock.replay(mockBucketDao, tubeDao, mockJira, labBatchDAO);

        LabEventHandler labEventHandler = new LabEventHandler(new WorkflowLoader(), AthenaClientProducer.stubInstance(),
                bucketBeanEJB, mockBucketDao, new BSPUserList(BSPManagerFactoryProducer.stubInstance()));

        PreFlightEntityBuilder preFlightEntityBuilder = new PreFlightEntityBuilder(bettaLimsMessageTestFactory,
                labEventFactory, labEventHandler,
                mapBarcodeToTube).invoke();

        ShearingEntityBuilder shearingEntityBuilder = new ShearingEntityBuilder(mapBarcodeToTube, preFlightEntityBuilder.getTubeFormation(),
                bettaLimsMessageTestFactory, labEventFactory, labEventHandler, preFlightEntityBuilder.getRackBarcode()).invoke();

        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = new LibraryConstructionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, labEventHandler,
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
            PlateTransferEventType sageLoadingJaxb = bettaLimsMessageTestFactory.buildRackToPlate("SageLoading",
                    libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                    libraryConstructionEntityBuilder.getPondRegTubeBarcodes().subList(i * 4, i * 4 + 4),
                    sageCassetteBarcode);
            // todo jmt SAGE section
            LabEvent sageLoadingEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(sageLoadingJaxb,
                    libraryConstructionEntityBuilder.getPondRegRack(), null);
            labEventHandler.processEvent(sageLoadingEntity);
            StaticPlate sageCassette = (StaticPlate) sageLoadingEntity.getTargetLabVessels().iterator().next();

            // SageLoaded
            PlateEventType sageLoadedJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                    LabEventType.SAGE_LOADED.getName(), sageCassetteBarcode);
            LabEvent sageLoadedEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(sageLoadedJaxb, sageCassette);
            labEventHandler.processEvent(sageLoadedEntity);

            // SageUnloading
            PlateTransferEventType sageUnloadingJaxb = bettaLimsMessageTestFactory.buildPlateToRack("SageUnloading",
                    sageCassetteBarcode, sageUnloadBarcode, sageUnloadTubeBarcodes.subList(i * 4, i * 4 + 4));
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
        PlateTransferEventType sageCleanupJaxb = bettaLimsMessageTestFactory.buildRackToRack("SageCleanup", sageUnloadBarcode,
                sageUnloadTubeBarcodes, sageCleanupBarcode, sageCleanupTubeBarcodes);
        Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new HashMap<VesselPosition, TwoDBarcodedTube>();
        List<TwoDBarcodedTube> sageUnloadTubes = new ArrayList<TwoDBarcodedTube>(mapBarcodeToSageUnloadTubes.values());
        for (int i = 0; i < NUM_POSITIONS_IN_RACK; i++) {
            mapPositionToTube.put(VesselPosition.getByName(bettaLimsMessageTestFactory.buildWellName(i + 1)),
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
                new QtpEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, labEventHandler,
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

        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory();
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

        LabEventHandler labEventHandler = new LabEventHandler(new WorkflowLoader(), AthenaClientProducer.stubInstance(),
                bucketBeanEJB, mockBucketDao, new BSPUserList(BSPManagerFactoryProducer.stubInstance()));
        BuildIndexPlate buildIndexPlate = new BuildIndexPlate("IndexPlate").invoke(null);
        FluidigmMessagesBuilder fluidigmMessagesBuilder = new FluidigmMessagesBuilder("", bettaLimsMessageTestFactory,
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
        private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
        private String harvestRackBarcode;
        private final Map<String, TwoDBarcodedTube> mapBarcodeToHarvestTube = new HashMap<String, TwoDBarcodedTube>();

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
            ArrayList<String> tubeBarcodes = new ArrayList<String>(mapBarcodeToTube.keySet());
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
            addMessage(messageList, bettaLimsMessageTestFactory, fluidigmSampleInputJaxb);

            // FluidigmIndexedAdapterInput plate P96COLS1-6BYROW to chip P384COLS4-6BYROW
            fluidigmIndexedAdapterInputJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                    "FluidigmIndexedAdapterInput", indexPlate.getLabel(), chipBarcode);
            fluidigmIndexedAdapterInputJaxb.getSourcePlate().setPhysType(
                    StaticPlate.PlateType.IndexedAdapterPlate96.getDisplayName());
            fluidigmIndexedAdapterInputJaxb.getSourcePlate().setSection(SBSSection.P96COLS1_6BYROW.getSectionName());
            fluidigmIndexedAdapterInputJaxb.getPlate().setPhysType(
                    StaticPlate.PlateType.Fluidigm48_48AccessArrayIFC.getDisplayName());
            fluidigmIndexedAdapterInputJaxb.getPlate().setSection(SBSSection.P384COLS4_6BYROW.getSectionName());
            addMessage(messageList, bettaLimsMessageTestFactory, fluidigmIndexedAdapterInputJaxb);

            // FluidigmHarvestingToRack chip P384COLS4-6BYROW to rack P96COLS1-6BYROW
            harvestRackBarcode = "Harvest" + testPrefix;
            List<String> harvestTubeBarcodes = new ArrayList<String>();
            for (int rackPosition = 1; rackPosition <= mapBarcodeToTube.size(); rackPosition++) {
                harvestTubeBarcodes.add("Harvest" + testPrefix + rackPosition);
            }
            fluidigmHarvestingToRackJaxb = bettaLimsMessageTestFactory.buildPlateToRack("FluidigmHarvestingToRack",
                    chipBarcode,
                    harvestRackBarcode,
                    harvestTubeBarcodes);
            fluidigmHarvestingToRackJaxb.getSourcePlate().setPhysType(
                    StaticPlate.PlateType.Fluidigm48_48AccessArrayIFC.getDisplayName());
            fluidigmHarvestingToRackJaxb.getSourcePlate().setSection(SBSSection.P384COLS4_6BYROW.getSectionName());
            fluidigmHarvestingToRackJaxb.setPositionMap(buildFluidigmPositionMap(tubeBarcodes, fluidigmSampleInputJaxb
                    .getSourcePlate().getBarcode()));
            fluidigmHarvestingToRackJaxb.getPlate().setSection(SBSSection.P96COLS1_6BYROW.getSectionName());
            addMessage(messageList, bettaLimsMessageTestFactory, fluidigmHarvestingToRackJaxb);
        }

        private PositionMapType buildFluidigmPositionMap(ArrayList<String> tubeBarcodes, String rackBarcode) {
            PositionMapType sourcePositionMap = new PositionMapType();
            sourcePositionMap.setBarcode(rackBarcode);
            int barcodeIndex = 0;
            for (int row = 0; row < 8; row++) {
                for (int column = 1; column <= 6; column++) {
                    ReceptacleType receptacleType = new ReceptacleType();
                    receptacleType.setBarcode(tubeBarcodes.get(barcodeIndex));
                    receptacleType.setPosition(bettaLimsMessageTestFactory.buildWellName(row * 12 + column));
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

    public static void addMessage(List<BettaLIMSMessage> messageList, BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
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
        bettaLimsMessageTestFactory.advanceTime();
    }

    public static void validateWorkflow(String nextEventTypeName, Collection<? extends LabVessel> tubes) {
        List<LabVessel> labVessels = new ArrayList<LabVessel>(tubes);
        validateWorkflow(nextEventTypeName, labVessels);
    }

    public static void validateWorkflow(String nextEventTypeName, LabVessel labVessel) {
        List<LabVessel> labVessels = new ArrayList<LabVessel>();
        labVessels.add(labVessel);
        validateWorkflow(nextEventTypeName, labVessels);
    }

    public static void validateWorkflow(String nextEventTypeName, List<LabVessel> labVessels) {
        BettalimsMessageResource bettalimsMessageResource = new BettalimsMessageResource();
        final AthenaClientService athenaClientService = AthenaClientProducer.stubInstance();
        bettalimsMessageResource.setAthenaClientService(athenaClientService);
        bettalimsMessageResource.validateWorkflow(labVessels, nextEventTypeName);
        WorkflowLoader workflowLoader = new WorkflowLoader();
        WorkflowConfig workflowConfig = workflowLoader.load();
        for (LabVessel labVessel : labVessels) {
            for (SampleInstance sampleInstance : labVessel.getSampleInstances()) {
                ProductOrder productOrder = athenaClientService.retrieveProductOrderDetails(
                        sampleInstance.getStartingSample().getProductOrderKey());
                // get workflow name from product order
                ProductWorkflowDef productWorkflowDef = workflowConfig.getWorkflowByName(
                        productOrder.getProduct().getWorkflowName());
                List<ProductWorkflowDefVersion.ValidationError> errors =
                        productWorkflowDef.getEffectiveVersion().validate(labVessel, nextEventTypeName);
                if (!errors.isEmpty()) {
                    ProductWorkflowDefVersion.ValidationError validationError = errors.get(0);
                    Assert.fail(validationError.getMessage() + " expected " + validationError.getExpectedEventNames() +
                            " actual " + validationError.getActualEventNames());
                }
            }
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

    /*
    public static class HiSeqEntityBuilder {
        private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;
        private final StripTube stripTube;

        private IlluminaFlowcell illuminaFlowcell;

        public HiSeqJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                StripTube stripTube) {
            this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
            this.stripTube = stripTube;
        }
    }
*/

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
