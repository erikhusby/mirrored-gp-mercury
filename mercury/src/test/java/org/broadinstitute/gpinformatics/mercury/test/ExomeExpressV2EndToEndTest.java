package org.broadinstitute.gpinformatics.mercury.test;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.*;
import org.easymock.EasyMock;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test Exome Express in Mercury
 */
public class ExomeExpressV2EndToEndTest {

    public static List<String> RACK_COLUMNS = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11",
            "12");
    public static List<String> RACK_ROWS = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H");

    @Test
    public void test() {


        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());

        LabEventFactory labEventFactory = new LabEventFactory(testUserList);

        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>();
        ProductOrder productOrder1 = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123", new Product(
                "Test product", new ProductFamily("Test product family"), "test", "1234", null, null, 10000, 20000, 100,
                40, null, null, true, "Exome Express", false), new ResearchProject(101L, "Test RP", "Test synopsis",
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


        BucketDao mockBucketDao = EasyMock.createMock(BucketDao.class);
        EasyMock.expect(mockBucketDao.findByName(EasyMock.eq(LabEventType.SHEARING_BUCKET.getName())))
                .andReturn(new LabEventTest.MockBucket(new WorkflowStepDef(LabEventType.SHEARING_BUCKET.getName()), jiraTicketKey));
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

        //TODO SGM:  DO not use Entity Builder.  Specifically use Jaxb.... OR Make this entity builder add to mapKeyToProductOrder.  Yeah, do that!!!!
        LabEventTest.PicoPlatingEntityBuider pplatingEntityBuilder =
                new LabEventTest.PicoPlatingEntityBuider(bettaLimsMessageFactory,
                labEventFactory, leHandler, mapBarcodeToTube, rackBarcode, keyToPoMap).invoke();


        // Bucket for Shearing - enters from workflow?
        /*
            TODO SGM:  Make labEventHandler put this in the bucket automatically

        Bucket shearingBucket = new Bucket(new WorkflowBucketDef("Shearing Bucket"));
        Collection<LabVessel> vessels = new ArrayList<LabVessel>(pplatingEntityBuilder.getNormBarcodeToTubeMap().values());
        //                new LinkedList<LabVessel>(mapBarcodeToTube.values());

        bucketBean.add(productOrder1
                .getBusinessKey(), vessels, shearingBucket, testActor, "", LabEventType.SHEARING_BUCKET);

        */


        LabEventTest.ExomeExpressShearingJaxbBuilder exexJaxbBuilder =
                new LabEventTest.ExomeExpressShearingJaxbBuilder(bettaLimsMessageFactory, shearingTubeBarcodes, "",
                        rackBarcode);
        exexJaxbBuilder.invoke();



        LabEventTest.validateWorkflow(LabEventType.PLATING_TO_SHEARING_TUBES.getName(),
                pplatingEntityBuilder.getNormTubeFormation());
        PlateTransferEventType plateToShearXfer = exexJaxbBuilder.getPlateToShearTubeTransferEventJaxb();
        LabEvent pToShearEvent = labEventFactory.buildFromBettaLimsRackToPlateDbFree(plateToShearXfer,
                pplatingEntityBuilder.getNormTubeFormation(),
                new StaticPlate("CovarisPlate", StaticPlate.PlateType.Eppendorf96));
        leHandler.processEvent(pToShearEvent);
        LabEventTest.validateWorkflow(LabEventType.COVARIS_LOADED.getName(),
                pToShearEvent.getTargetLabVessels());


        // When the above event is executed, the items should be removed from the bucket.
        PlateTransferEventType covarisxfer = exexJaxbBuilder.getCovarisLoadEventJaxb();
        LabEvent covarisEvent = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(covarisxfer,
                (StaticPlate) pToShearEvent.getTargetLabVessels().iterator().next(),
                new StaticPlate("NormPlate", StaticPlate.PlateType.Eppendorf96));



        ProductWorkflowDef productWorkflowDef = workflowConfig.getWorkflowByName(
                productOrder1.getProduct().getWorkflowName());
        ProductWorkflowDefVersion productWorkflowDefVersion = productWorkflowDef.getEffectiveVersion();
        Map<WorkflowStepDef, Collection<LabVessel>> bucketToVessels = leHandler.itemizeBucketItems(covarisEvent);
        Assert.assertTrue(bucketToVessels.keySet().size() == 1);
//        Assert.assertEquals(shearingBucket.getBucketDefinitionName(),
//                bucketToVessels.keySet().iterator().next().getName());


//        bucketBean.startDBFree(testActor, labEvent.getTargetLabVessels(), shearingBucket, labEvent.getEventLocation());


        //        // todo plates vs tubes?
        //        // - Deck calls web service to verify source barcodes?
        //        // - Deck calls web service to validate next action against workflow and batch
        //        // Decks (BSP and Sequencing) send messages to Mercury, first message auto-drains bucket
        //
        //        leHandler.processEvent(labEvent);

        //        bucketBean.startDBFree(testActor, vessels, shearingBucket, labEvent.getEventLocation());

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
        // Pooling calculator
        // Strip Tube B
        // Create Flowcell JIRA
        // MiSeq reagent block transfer message
        // Register run
        // Analysis calls ZIMS
        // BASS?
        // View PDO details screen, including analysis progress, aggregations and sequencing metrics
        // PDM marks sample in PDO complete, PM notified
        // Billing
        // Submissions
        // Reporting

        EasyMock.verify(mapBarcodeToTube);

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
