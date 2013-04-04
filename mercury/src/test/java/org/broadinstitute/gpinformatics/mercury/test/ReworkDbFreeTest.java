package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.easymock.EasyMock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Test that samples can go partway through a workflow, be marked for rework, go to a previous
 * step, and then move to completion.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ReworkDbFreeTest {
    public static final int NUM_POSITIONS_IN_RACK = 96;
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

    // Advance to Pond Pico, rework 2 samples from the start.
    @Test(enabled = true, groups = TestGroups.DATABASE_FREE)
    public void testRework() {

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

        ProductOrder reworkProductOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        String reworkJiraTicketKey = reworkProductOrder.getBusinessKey();

        TwoDBarcodedTube reworkTube = null;
        final int REWORK_POSITION = (int)Math.ceil((double)productOrder.getSamples().size() / 2);

        // starting rack
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        int rackPosition = 1;
        for(ProductOrderSample poSample:productOrder.getSamples()) {
            String barcode = "R" + rackPosition;
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(productOrder.getBusinessKey(), poSample.getSampleName()));
            mapBarcodeToTube.put(barcode, bspAliquot);
            if (rackPosition == REWORK_POSITION) {
                reworkTube = bspAliquot;
            }
            rackPosition++;
        }

        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        labBatchEJB.createLabBatch(workflowBatch, "scottmat");

        final long now = System.currentTimeMillis();
        String rackBarcode = "REXEX" + (new Date(now)).toString();

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


        // Prior to rework tube should be on first lcset.
        //xxx
        //MercurySample startingSample = reworkTube.getSampleInstancesList().get(0).getStartingSample();

        // rework rack
        Map<String, TwoDBarcodedTube> reworkMapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        rackPosition = 1;
        for(ProductOrderSample poSample : reworkProductOrder.getSamples()) {
            String barcode = "RW" + rackPosition;
            TwoDBarcodedTube bspAliquot = (rackPosition == REWORK_POSITION) ? reworkTube : new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(productOrder.getBusinessKey(), poSample.getSampleName()));
            mapBarcodeToTube.put(barcode, bspAliquot);
            rackPosition++;
        }

        LabBatch reworkBatch = new LabBatch("Exome Express Rework Batch",
                new HashSet<LabVessel>(reworkMapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        labBatchEJB.createLabBatch(reworkBatch, "scottmat");
        String reworkRackBarcode = "REXEX" + (new Date(now + 1000L)).toString();


        // Messaging
        LabBatchEjb reworkLabBatchEJB = new LabBatchEjb();
        reworkLabBatchEJB.setAthenaClientService(AthenaClientProducer.stubInstance());
        reworkLabBatchEJB.setJiraService(JiraServiceProducer.stubInstance());
        reworkLabBatchEJB.setTubeDAO(tubeDao);
        reworkLabBatchEJB.setJiraTicketDao(mockJira);
        reworkLabBatchEJB.setLabBatchDao(labBatchDAO);
        reworkLabBatchEJB.createLabBatch(workflowBatch, "scottmat");

        BucketBean reworkBucketBeanEJB = new BucketBean(labEventFactory, JiraServiceProducer.stubInstance(), reworkLabBatchEJB);
        LabEventHandler reworkLabEventHandler =
                new LabEventHandler(new WorkflowLoader(), AthenaClientProducer.stubInstance(), reworkBucketBeanEJB,
                        mockBucketDao, new BSPUserList(BSPManagerFactoryProducer.stubInstance()));

        PicoPlatingEntityBuilder reworkPplatingEntityBuilder = new PicoPlatingEntityBuilder(bettaLimsMessageTestFactory,
                labEventFactory, reworkLabEventHandler,
                reworkMapBarcodeToTube, reworkRackBarcode).invoke();

        ExomeExpressShearingEntityBuilder reworkShearingEntityBuilder =
                new ExomeExpressShearingEntityBuilder(reworkPplatingEntityBuilder.getNormBarcodeToTubeMap(),
                        reworkPplatingEntityBuilder.getNormTubeFormation(),
                        bettaLimsMessageTestFactory,
                        labEventFactory,
                        reworkLabEventHandler,
                        reworkPplatingEntityBuilder.getNormalizationBarcode()).invoke();


    }

}
