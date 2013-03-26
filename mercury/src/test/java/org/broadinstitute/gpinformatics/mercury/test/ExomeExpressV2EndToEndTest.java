package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.testng.Assert;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.*;
import org.easymock.EasyMock;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;

/**
 * Test Exome Express in Mercury
 */
public class ExomeExpressV2EndToEndTest {

    public static List<String> RACK_COLUMNS = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11",
            "12");
    public static List<String> RACK_ROWS = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H");

    @Test(groups = DATABASE_FREE)
    public void test() {


        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory();
        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());

        LabEventFactory labEventFactory = new LabEventFactory(testUserList);

        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>();
        ProductOrder productOrder1 = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123", new Product(
                "Test product", new ProductFamily("Test product family"), "test", "1234", null, null, 10000, 20000, 100,
                40, null, null, true, WorkflowName.EXOME_EXPRESS.getWorkflowName(), false), new ResearchProject(101L, "Test RP", "Test synopsis",
                false));
        String jiraTicketKey = "PD0-1";
        productOrder1.setJiraTicketKey(jiraTicketKey);
        productOrder1.setOrderStatus(ProductOrder.OrderStatus.Submitted);


        LabBatchEjb labBatchEJB = new LabBatchEjb();
        labBatchEJB.setAthenaClientService(AthenaClientProducer.stubInstance());
        labBatchEJB.setJiraService(JiraServiceProducer.stubInstance());

        LabVesselDao tubeDao = EasyMock.createNiceMock(LabVesselDao.class);
        labBatchEJB.setTubeDAO(tubeDao);

        JiraTicketDao mockJira = EasyMock.createNiceMock(JiraTicketDao.class);
        labBatchEJB.setJiraTicketDao(mockJira);

        LabBatchDAO labBatchDAO = EasyMock.createNiceMock(LabBatchDAO.class);
        labBatchEJB.setLabBatchDao(labBatchDAO);


//        LabEventTest.MockBucket workingShearingBucket= new LabEventTest.MockBucket("Shearing Bucket",jiraTicketKey);
        LabEventTest.MockBucket workingPicoBucket= new LabEventTest.MockBucket("Pico/Plating Bucket",jiraTicketKey);
        Bucket workingShearingBucket= new Bucket(new WorkflowStepDef("Shearing Bucket"));

        BucketDao mockBucketDao = EasyMock.createMock(BucketDao.class);
        EasyMock.expect(mockBucketDao.findByName(EasyMock.eq("Shearing Bucket")))
                .andReturn(workingShearingBucket);
        EasyMock.expect(mockBucketDao.findByName(EasyMock.eq("Shearing Bucket")))
                .andReturn(workingShearingBucket);
        EasyMock.expect(mockBucketDao.findByName(EasyMock.eq("Pico/Plating Bucket")))
                .andReturn(workingPicoBucket);
        BucketBean bucketBean = new BucketBean(labEventFactory, JiraServiceProducer.stubInstance(), labBatchEJB);
        EasyMock.replay(mockBucketDao, tubeDao, mockJira, labBatchDAO);

        final String testActor = "hrafal";

        LabEventHandler leHandler =
                new LabEventHandler(new WorkflowLoader(), AthenaClientProducer
                        .stubInstance(), bucketBean, mockBucketDao, new BSPUserList(BSPManagerFactoryProducer
                        .stubInstance()));


        // Bucket for Shearing - enters from workflow?
        WorkflowLoader workflowLoader = new WorkflowLoader();
        WorkflowConfig workflowConfig = workflowLoader.load();

        String rackBarcode = "exexendtoend" + LabEventTest.NUM_POSITIONS_IN_RACK;

        // Define baits (CATs later)
        // Associate baits with vessels
        // Define molecular indexes
        // Associate molecular indexes with vessels
        // Define Product
        // Define Research Project
        // Define IRB
        // Define Personnel
        // Define Cohort/Collection
        // {Create Quote/Setup Funding}
        // Create Product Order (key lab personnel and PDMs are notified)
        //        ProductOrder productOrder1 = AthenaClientServiceStub.createDummyProductOrder();


        List<String> shearingTubeBarcodes = new ArrayList<String>()/*Arrays.asList("SH1", "SH2", "SH3")*/;
        Map<String, String> barcodesByRackPositions = new HashMap<String, String>();

        // starting rack
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for (int rackPosition = 1; rackPosition <= LabEventTest.NUM_POSITIONS_IN_RACK; rackPosition++) {
            String barcode = "R" + rackPosition;
            shearingTubeBarcodes.add(barcode);

            String column = RACK_COLUMNS.get((rackPosition - 1) / RACK_ROWS.size());

            //            float rows = RACK_ROWS.size();

            //            float positionVal = rackPosition;

            //            String row = RACK_ROWS.get(Math.round((int)positionVal%rows));
            String row = RACK_ROWS.get((rackPosition - 1) % RACK_ROWS.size());

            String bspStock = "SM-" + rackPosition;

            barcodesByRackPositions.put(row + column, barcode);

            productOrderSamples.add(new ProductOrderSample(bspStock));
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(jiraTicketKey, bspStock));
            mapBarcodeToTube.put(barcode, bspAliquot);
        }

        // Define travel group (can be independent of product order)
        // {Create kit and Ship kit}
        // - Upload metadata through BSP portal
        // {Receive samples} (There are some hooks into product order so the receipt team can direct samples)
        // 3 stars align notification
        // BSP notifies Mercury of existence of samples in plastic
        // Buckets for Extraction and BSP Pico?
        // Add samples to bucket
        // Drain bucket into batch
        // Create JIRA ticket configured in bucket (There is a ticket for an LCSet but none for the extraction/pico work)
        // BSP registers batch in Mercury
        // BSP Manual messaging for extractions, various batches

//        PlateEventType
        /*
            BSP Pico Work.
            Rack of tubes "Taken out of" bucket.  Run through all pico steps.  Awaits final
          */

        Map<String, ProductOrder> keyToPoMap = new HashMap<String, ProductOrder>();
        keyToPoMap.put(productOrder1.getBusinessKey(), productOrder1);

        LabEventTest.PicoPlatingEntityBuilder pplatingEntityBuilder =
                new LabEventTest.PicoPlatingEntityBuilder(bettaLimsMessageTestFactory,
                labEventFactory, leHandler, mapBarcodeToTube, rackBarcode, keyToPoMap).invoke();

        // Lab Event Factory should have put tubes into the Bucket after normalization
        Assert.assertEquals(LabEventTest.NUM_POSITIONS_IN_RACK,workingShearingBucket.getBucketEntries().size());

        // Bucket for Shearing - enters from workflow?

        LabEventTest.ExomeExpressShearingJaxbBuilder exexJaxbBuilder =
                new LabEventTest.ExomeExpressShearingJaxbBuilder(bettaLimsMessageTestFactory,
                        new ArrayList<String>(pplatingEntityBuilder.getNormBarcodeToTubeMap().keySet()), "",
                        pplatingEntityBuilder.getNormalizationBarcode());
        exexJaxbBuilder.invoke();


        //TODO SGM   SHould this validate be on the tube formation?
        LabEventTest.validateWorkflow(LabEventType.SHEARING_TRANSFER.getName(),
                pplatingEntityBuilder.getNormBarcodeToTubeMap().values());
        PlateTransferEventType plateToShearXfer = exexJaxbBuilder.getShearTransferEventJaxb();
        LabEvent sheerEvent = labEventFactory.buildFromBettaLimsRackToPlateDbFree(plateToShearXfer,
                pplatingEntityBuilder.getNormTubeFormation(),
                new StaticPlate("NormPlate", StaticPlate.PlateType.Eppendorf96));
        StaticPlate sheerPlate = (StaticPlate) sheerEvent.getTargetLabVessels().iterator().next();
        leHandler.processEvent(sheerEvent);


        // Bucket should have been drained after Plating to Shearing Tubes
        Assert.assertEquals(0,workingShearingBucket.getBucketEntries().size());


        LabEventTest.validateWorkflow(LabEventType.COVARIS_LOADED.getName(),
                                             sheerEvent.getTargetLabVessels());
//        // When the above event is executed, the items should be removed from the bucket.
        PlateEventType covarisxfer = exexJaxbBuilder.getCovarisLoadEventJaxb();
        LabEvent covarisEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(covarisxfer,
                sheerPlate);
        leHandler.processEvent(covarisEvent);
//        StaticPlate covarisPlate = (StaticPlate) covarisEvent.getTargetLabVessels().iterator().next();


        LabEventTest.validateWorkflow(LabEventType.POST_SHEARING_TRANSFER_CLEANUP.getName(),sheerPlate);
        LabEvent postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                exexJaxbBuilder.getPostShearingTransferCleanupEventJaxb(), sheerPlate, null);
        leHandler.processEvent(postShearingTransferCleanupEntity);
        StaticPlate shearingCleanupPlate =
                (StaticPlate) postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next();


        LabEventTest.validateWorkflow(LabEventType.SHEARING_QC.getName(), shearingCleanupPlate);
        LabEvent shearingQcEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                exexJaxbBuilder.getShearingQcEventJaxb(), shearingCleanupPlate, null);
        leHandler.processEvent(shearingQcEntity);


        LabEventTest.LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = new LabEventTest.LibraryConstructionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, leHandler,
                shearingCleanupPlate,  postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next().getLabel(),
                                                                                                                                                  sheerPlate, LabEventTest.NUM_POSITIONS_IN_RACK).invoke();



        //        // todo plates vs tubes?
        //        // - Deck calls web service to verify source barcodes?
        //        // - Deck calls web service to validate next action against workflow and batch
        //        // Decks (BSP and Sequencing) send messages to Mercury, first message auto-drains bucket
        //

        // Various messages advance workflow (test JMS vs JAX-RS)
        // Non ExEx messages handled by BettaLIMS
        // Automation uploads QC
        // Operator views recently handled plasticware
        // Operator visits check point page, chooses re-entry point for rework (type 1)
        // Operator makes note and adds attachment
        // - View batch RAP sheet, add items
        // - View sample RAP sheet, including notes
        // - View Product Order Status (Detail?), including rework and notes
        // - Mark plastic dead
        // Lookup plastic
        // - Search (future)
        // - PdM abandons sample (PMs notified)
        // Add library to queue for sequencing
        // Register library to MiSeq
        // Create pool group
        LabEventTest.HybridSelectionEntityBuilder hybridSelectionEntityBuilder = new LabEventTest.HybridSelectionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, leHandler,
                libraryConstructionEntityBuilder.getPondRegRack(),
                libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                libraryConstructionEntityBuilder.getPondRegTubeBarcodes()).invoke();

        // Pooling calculator
        // Strip Tube B
        // Create Flowcell JIRA
        LabEventTest.QtpEntityBuilder qtpEntityBuilder = new LabEventTest.QtpEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, leHandler,
                Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchRack()),
                Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchRackBarcode()),
                Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchBarcodes()),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), WorkflowName.EXOME_EXPRESS);
        qtpEntityBuilder.invoke();

        String flowcellBarcode = "flowcell"+ new Date().getTime();

        LabEventTest.HiSeq2500FlowcellEntityBuilder  hiSeq2500FlowcellEntityBuilder =
            new LabEventTest.HiSeq2500FlowcellEntityBuilder(bettaLimsMessageTestFactory, labEventFactory,
                            leHandler,
                    qtpEntityBuilder.getDenatureRack(),
                            flowcellBarcode).invoke();
        // MiSeq reagent block transfer message
        // Register run
        IlluminaSequencingRunFactory illuminaSequencingRunFactory =
                new IlluminaSequencingRunFactory(EasyMock.createNiceMock(JiraCommentUtil.class));
        IlluminaSequencingRun illuminaSequencingRun;
        try {
            illuminaSequencingRun = illuminaSequencingRunFactory.buildDbFree(new SolexaRunBean(
                    hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell().getCartridgeBarcode(), "Run1", new Date(),
                    "SL-HAL", File.createTempFile("RunDir", ".txt").getAbsolutePath(), null),
                                                                              hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        // Analysis calls ZIMS
        // BASS?
        // View PDO details screen, including analysis progress, aggregations and sequencing metrics
        // PDM marks sample in PDO complete, PM notified
        // Billing
        // Submissions
        // Reporting

        EasyMock.verify(mockBucketDao);

    }

    /**
     * Build a rack entity
     *
     * @param mapBarcodeToTubes    source tubes
     * @param barcode              barcode Barcode
     * @param barcodeByPositionMap JAXB list of tube barcodes
     * @return entity
     */
    private TubeFormation buildTubeFormation(Map<String, TwoDBarcodedTube> mapBarcodeToTubes, String barcode,
                                             Map<String, String> barcodeByPositionMap) {

        Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new HashMap<VesselPosition, TwoDBarcodedTube>();

        for (Map.Entry<String, String> tubeEntry : barcodeByPositionMap.entrySet()) {
            TwoDBarcodedTube twoDBarcodedTube = mapBarcodeToTubes.get(tubeEntry.getValue());
            if (twoDBarcodedTube == null) {
                twoDBarcodedTube = new TwoDBarcodedTube(tubeEntry.getValue());
                mapBarcodeToTubes.put(tubeEntry.getValue(), twoDBarcodedTube);
            }
            mapPositionToTube.put(VesselPosition.getByName(tubeEntry.getKey()), twoDBarcodedTube);
        }
        TubeFormation formation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);

        return formation;
    }
}
