package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.broadinstitute.gpinformatics.mercury.presentation.transfervis.TransferVisualizerFrame;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.easymock.EasyMock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import java.util.*;

import static org.testng.Assert.*;

/**
 * Test that samples can go partway through a workflow, be marked for rework, go to a previous
 * step, and then move to completion.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ReworkDbFreeTest {
    public static final int NUM_POSITIONS_IN_RACK = 2;
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


    @BeforeClass(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        templateEngine.postConstruct();
    }

    // Advance to Pond Pico, rework a sample from the start
    @Test(enabled = true, groups = TestGroups.DATABASE_FREE)
    public void testRework() throws Exception {

        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(NUM_POSITIONS_IN_RACK);

        // Creates the mocks.
        JiraTicketDao mockJira = EasyMock.createNiceMock(JiraTicketDao.class);
        LabVesselDao tubeDao = EasyMock.createNiceMock(LabVesselDao.class);
        LabBatchDAO labBatchDAO = EasyMock.createNiceMock(LabBatchDAO.class);
        BucketDao mockBucketDao = EasyMock.createMock(BucketDao.class);

        EasyMock.expect(mockBucketDao.findByName(EasyMock.eq("Shearing Bucket"))).andReturn(
                new MockBucket(new WorkflowStepDef("Shearing Bucket"), productOrder.getBusinessKey())).times(4);
        EasyMock.expect(mockBucketDao.findByName(EasyMock.eq("Pico/Plating Bucket"))).andReturn(
                new MockBucket(new WorkflowStepDef("Pico/Plating Bucket"), productOrder.getBusinessKey())).times(2);

        EasyMock.replay(mockBucketDao, tubeDao, mockJira, labBatchDAO);


        LabBatchEjb labBatchEJB = new LabBatchEjb();
        labBatchEJB.setAthenaClientService(AthenaClientProducer.stubInstance());
        labBatchEJB.setJiraService(JiraServiceProducer.stubInstance());
        labBatchEJB.setTubeDAO(tubeDao);
        labBatchEJB.setJiraTicketDao(mockJira);
        labBatchEJB.setLabBatchDao(labBatchDAO);

        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);

        BucketBean bucketBeanEJB = new BucketBean(labEventFactory, JiraServiceProducer.stubInstance(), labBatchEJB);

        LabEventHandler labEventHandler =
                new LabEventHandler(new WorkflowLoader(), AthenaClientProducer.stubInstance(), bucketBeanEJB,
                        mockBucketDao, new BSPUserList(BSPManagerFactoryProducer.stubInstance()));

        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory();

        String origLcsetSuffix = "-111";
        String reworkLcsetSuffix = "-222";

        long now = System.currentTimeMillis();
        String origRackBarcode   = "REXEX" + (new Date(now)).toString();
        String reworkRackBarcode = "REXEX" + (new Date(now + 137L)).toString();

        String origTubePrefix = "999999";
        String reworkTubePrefix = "888888";
        int reworkIdx = NUM_POSITIONS_IN_RACK - 1; // arbitrary choice

        Map<String, TwoDBarcodedTube> origRackMap = createRack(productOrder, origTubePrefix, reworkIdx, null);
        LabBatch origBatch = createBatch(origRackMap, labBatchEJB, origLcsetSuffix);
        VesselContainer origContainer = sendMsgs(origRackMap, labEventFactory, labEventHandler,
                bettaLimsMessageTestFactory, origRackBarcode, "1", false);

        // Selects the rework tube and verifies its lcset.
        TwoDBarcodedTube reworkTube = origRackMap.get(origTubePrefix + reworkIdx);
        assertNotNull(reworkTube);
        assertEquals(reworkTube.getLabBatchesList().size(), 1);
        assertEquals(reworkTube.getLabBatchesList().get(0).getBatchName(), "LCSET" + origLcsetSuffix);
        assertEquals(reworkTube.getMercurySamplesList().size(), 1);
        String reworkSampleKey = reworkTube.getMercurySamplesList().get(0).getSampleKey();

        // Starts the rework with a new rack of tubes and includes the rework tube.
        Map<String, TwoDBarcodedTube> reworkRackMap = createRack(productOrder, reworkTubePrefix, reworkIdx, reworkTube);
        LabBatch reworkBatch = createBatch(reworkRackMap, labBatchEJB, reworkLcsetSuffix);
        VesselContainer reworkContainer = sendMsgs(reworkRackMap, labEventFactory, labEventHandler,
                bettaLimsMessageTestFactory, reworkRackBarcode, "2", true);

        // Checks the lab batch composition of the non-rework container.
        assertEquals(origContainer.getAllLabBatches().size(), 2);
        assertEquals(origContainer.getLabBatchCompositions().size(), 2);
        LabBatchComposition origComposition = (LabBatchComposition) origContainer.getLabBatchCompositions().get(0);
        assertEquals(origComposition.getDenominator(), NUM_POSITIONS_IN_RACK);
        assertEquals(origComposition.getCount(), NUM_POSITIONS_IN_RACK);
        assert(origComposition.getLabBatch().getBatchName().endsWith(origLcsetSuffix));
        origComposition = (LabBatchComposition) origContainer.getLabBatchCompositions().get(1);
        assertEquals(origComposition.getDenominator(), NUM_POSITIONS_IN_RACK);
        assertEquals(origComposition.getCount() , 1);
        assertFalse(origComposition.getLabBatch().getBatchName().endsWith(origLcsetSuffix));

        // Checks the lab batch composition of the rework container.
        assertEquals(reworkContainer.getAllLabBatches().size(), 2);
        assertEquals(reworkContainer.getLabBatchCompositions().size(), 2);

        LabBatchComposition reworkComposition;
        reworkComposition = (LabBatchComposition) reworkContainer.getLabBatchCompositions().get(0);
        assertEquals(reworkComposition.getDenominator(), NUM_POSITIONS_IN_RACK);
        assertEquals(reworkComposition.getCount() , NUM_POSITIONS_IN_RACK);
        assert(reworkComposition.getLabBatch().getBatchName().endsWith(reworkLcsetSuffix));
        reworkComposition = (LabBatchComposition) reworkContainer.getLabBatchCompositions().get(1);
        assertEquals(reworkComposition.getDenominator(), NUM_POSITIONS_IN_RACK);
        assertEquals(reworkComposition.getCount() , 1);
        assertFalse(reworkComposition.getLabBatch().getBatchName().endsWith(reworkLcsetSuffix));

        // Rework tube should be in two lcsets.
        assertEquals(reworkTube.getAllLabBatches().size(), 2);

        // Checks the correct batch identifier for the rework tube, which depends on the end container.
        assert(reworkTube.getPluralityLabBatch(origContainer).getBatchName().endsWith(origLcsetSuffix));
        assert(reworkTube.getPluralityLabBatch(reworkContainer).getBatchName().endsWith(reworkLcsetSuffix));
    }



    private Map<String, TwoDBarcodedTube> createRack(ProductOrder productOrder, String tubePrefix, int reworkIdx, TwoDBarcodedTube reworkTube) {
        Map<String, TwoDBarcodedTube> rackMap = new LinkedHashMap<String, TwoDBarcodedTube>();
        int rackPosition = 1;
        for (ProductOrderSample poSample : productOrder.getSamples()) {
            TwoDBarcodedTube bspAliquot;
            if (rackPosition == reworkIdx && reworkTube != null) {
                bspAliquot = reworkTube;
            } else {
                String barcode = tubePrefix + rackPosition;
                bspAliquot = new TwoDBarcodedTube(barcode);
                bspAliquot.addSample(new MercurySample(poSample.getSampleName()));
                bspAliquot.addBucketEntry(new BucketEntry(bspAliquot, productOrder.getBusinessKey()));
            }
            rackMap.put(bspAliquot.getLabel(), bspAliquot);
            rackPosition++;
        }
        return rackMap;
    }

    private LabBatch createBatch(Map<String, TwoDBarcodedTube> rackMap, LabBatchEjb labBatchEJB, String lcsetSuffix) {
        String defaultLcsetSuffix = JiraServiceStub.getCreatedIssueSuffix();
        JiraServiceStub.setCreatedIssueSuffix(lcsetSuffix);
        LabBatch batch = new LabBatch("x", new HashSet<LabVessel>(rackMap.values()), LabBatch.LabBatchType.WORKFLOW);
        labBatchEJB.createLabBatch(batch, "scottmat");
        JiraServiceStub.setCreatedIssueSuffix(defaultLcsetSuffix);
        return batch;
    }

    private VesselContainer sendMsgs(Map<String, TwoDBarcodedTube> rackMap, LabEventFactory labEventFactory,
                          LabEventHandler labEventHandler, BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                          String rackBarcode, String testPrefix, boolean sendLc) {

        VesselContainer lastContainer = null;

        PicoPlatingEntityBuilder pplatingEntityBuilder = new PicoPlatingEntityBuilder(bettaLimsMessageTestFactory,
                labEventFactory, labEventHandler, rackMap, rackBarcode, testPrefix).invoke();

        ExomeExpressShearingEntityBuilder shearingEntityBuilder =
                new ExomeExpressShearingEntityBuilder(pplatingEntityBuilder.getNormBarcodeToTubeMap(),
                        pplatingEntityBuilder.getNormTubeFormation(), bettaLimsMessageTestFactory, labEventFactory,
                        labEventHandler, pplatingEntityBuilder.getNormalizationBarcode(), testPrefix).invoke();

        lastContainer = (VesselContainer)pplatingEntityBuilder.getNormTubeFormation().getContainerRole();

        if (sendLc) {
            LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = new LibraryConstructionEntityBuilder(
                    bettaLimsMessageTestFactory, labEventFactory, labEventHandler,
                    shearingEntityBuilder.getShearingCleanupPlate(), shearingEntityBuilder.getShearCleanPlateBarcode(),
                    shearingEntityBuilder.getShearingPlate(), NUM_POSITIONS_IN_RACK, testPrefix).invoke();

            lastContainer = (VesselContainer)shearingEntityBuilder.getShearingPlate().getContainerRole();

            if (false) {
                TransferVisualizerFrame transferVisualizerFrame = new TransferVisualizerFrame();
                transferVisualizerFrame.renderVessel(rackMap.values().iterator().next());
                try {
                    Thread.sleep(500000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }

        return lastContainer;
    }

}
