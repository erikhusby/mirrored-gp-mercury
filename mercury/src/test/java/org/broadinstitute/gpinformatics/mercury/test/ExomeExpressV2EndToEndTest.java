package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Test Exome Express in Mercury
 */
public class ExomeExpressV2EndToEndTest {

    public static List<String> RACK_COLUMNS = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
    public static List<String> RACK_ROWS = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H");


    @Test
    public void test() {

        LabEventHandler leHandler = new LabEventHandler(new WorkflowLoader(), AthenaClientProducer.stubInstance());

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());

        LabEventFactory labEventFactory = new LabEventFactory(testUserList);
        final String testActor = "hrafal";

        BucketBean bucketBean = new BucketBean(labEventFactory, JiraServiceProducer.stubInstance());

        // Bucket for Shearing - enters from workflow?
        WorkflowLoader workflowLoader = new WorkflowLoader();
        WorkflowConfig workflowConfig = workflowLoader.load();

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

        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>();
        ProductOrder productOrder1 = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123", new Product(
                "Test product", new ProductFamily("Test product family"), "test", "1234", null, null, 10000, 20000, 100,
                40, null, null, true, "Exome Express", false), new ResearchProject(101L, "Test RP", "Test synopsis",
                                                                                      false));
        String jiraTicketKey = "PD0-1";
        productOrder1.setJiraTicketKey(jiraTicketKey);

        List<String> shearingTubeBarcodes = new ArrayList<String>()/*Arrays.asList("SH1", "SH2", "SH3")*/;
        Map<String, String> barcodesByRackPositions = new HashMap<String, String>();

        // starting rack
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for (int rackPosition = 1; rackPosition <= LabEventTest.NUM_POSITIONS_IN_RACK; rackPosition++) {
            String barcode = "R" + rackPosition;
            shearingTubeBarcodes.add(barcode);

            String column =    RACK_COLUMNS.get((rackPosition-1)/RACK_ROWS.size());

//            float rows = RACK_ROWS.size();

//            float positionVal = rackPosition;

//            String row = RACK_ROWS.get(Math.round((int)positionVal%rows));
            String row = RACK_ROWS.get((rackPosition-1)%RACK_ROWS.size());

            String bspStock = "SM-" + rackPosition;

            barcodesByRackPositions.put(row + column,barcode);

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

        ProductWorkflowDef productWorkflowDef = workflowConfig.getWorkflowByName(
                productOrder1.getProduct().getWorkflowName());
        ProductWorkflowDefVersion productWorkflowDefVersion = productWorkflowDef.getEffectiveVersion();
        // todo get from message event name to bucket def
        Bucket shearingBucket = new Bucket(new WorkflowBucketDef("Shearing"));

        Collection<LabVessel> vessels = new LinkedList<LabVessel>(mapBarcodeToTube.values());

        bucketBean.add(productOrder1.getBusinessKey(), vessels, shearingBucket, testActor, "");

        String rackBarcode = "exexendtoend" + LabEventTest.NUM_POSITIONS_IN_RACK;

        LabEventTest.ExomeExpressShearingEntityBuilder exExShearingEntityBuilder =
                new LabEventTest.ExomeExpressShearingEntityBuilder(mapBarcodeToTube,
                                                                   buildRack(mapBarcodeToTube,rackBarcode ,barcodesByRackPositions),
                                                                   bettaLimsMessageFactory, labEventFactory, leHandler,
                                                                   rackBarcode );
        exExShearingEntityBuilder.addProductOrderToMap(productOrder1);
        exExShearingEntityBuilder.invoke();

        //May Happen in BSP Before bucket
//        PlateTransferEventType plateTransferEventType = bettaLimsMessageFactory.buildPlateToRack(
//                "PlatingToShearingTubes", "NormPlate", "CovarisRack", shearingTubeBarcodes);
//        //        Map<String, TwoDBarcodedTube> mapBarcodeToTargetTubes = new HashMap<String, TwoDBarcodedTube>();
//
//        LabEvent labEvent = labEventFactory.buildFromBettaLimsPlateToRackDbFree(plateTransferEventType, new StaticPlate(
//                "NormPlate", StaticPlate.PlateType.Eppendorf96), mapBarcodeToTube);
//
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
    }

    //    private void dataSetup() {
    //
    //        Map<String, ProductOrder> mapKeyToProductOrder = new HashMap<String, ProductOrder>();
    //        LabEventFactory.LabEventRefDataFetcher labEventRefDataFetcher =
    //                    new LabEventFactory.LabEventRefDataFetcher() {
    //
    //                        @Override
    //                        public BspUser getOperator(String userId) {
    //
    //                            return new BSPUserList.QADudeUser("Test", BSPManagerFactoryStub.QA_DUDE_USER_ID);
    //                        }
    //
    //                        @Override
    //                        public BspUser getOperator(Long bspUserId) {
    //                            BspUser testUser = new BSPUserList.QADudeUser("Test", BSPManagerFactoryStub.QA_DUDE_USER_ID);
    //                            return testUser;
    //                        }
    //
    //                        @Override
    //                        public LabBatch getLabBatch(String labBatchName) {
    //                            return null;
    //                        }
    //                    };
    //        //        Controller.startCPURecording(true);
    //
    //        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>();
    //        ProductOrder productOrder = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123", new Product(
    //                "Test product", new ProductFamily("Test product family"), "test", "1234", null, null, 10000, 20000, 100,
    //                40, null, null, true, "Hybrid Selection", false), new ResearchProject(101L, "Test RP", "Test synopsis",
    //                                                                                      false));
    //        String jiraTicketKey = "PD0-1";
    //        productOrder.setJiraTicketKey(jiraTicketKey);
    //        mapKeyToProductOrder.put(jiraTicketKey, productOrder);
    //
    //        // starting rack
    //        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
    //        for (int rackPosition = 1; rackPosition <= LabEventTest.NUM_POSITIONS_IN_RACK; rackPosition++) {
    //            String barcode = "R" + rackPosition;
    //            String bspStock = "SM-" + rackPosition;
    //            productOrderSamples.add(new ProductOrderSample(bspStock));
    //            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
    //            bspAliquot.addSample(new MercurySample(jiraTicketKey, bspStock));
    //            mapBarcodeToTube.put(barcode, bspAliquot);
    //        }
    //
    //        // Messaging
    //        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
    //        LabEventFactory labEventFactory = new LabEventFactory();
    //        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);
    //        LabEventHandler labEventHandler = new LabEventHandler(new WorkflowLoader(), AthenaClientProducer.stubInstance(),
    //                                                              new BSPUserList(
    //                                                                      BSPManagerFactoryProducer.stubInstance()));
    //
    ////        LabEventTest.PreFlightEntityBuilder preFlightEntityBuilder = new LabEventTest.PreFlightEntityBuilder(bettaLimsMessageFactory,
    ////                                                                                   labEventFactory, labEventHandler,
    ////                                                                                   mapBarcodeToTube).invoke();
    //
    //
    //                     //Modified
    //        LabEventTest.ShearingEntityBuilder shearingEntityBuilder = new LabEventTest.ShearingEntityBuilder(mapBarcodeToTube,
    //                                                                                preFlightEntityBuilder.getRackOfTubes(),
    //                                                                                bettaLimsMessageFactory,
    //                                                                                labEventFactory, labEventHandler,
    //                                                                                preFlightEntityBuilder.getRackBarcode())
    //                .invoke();
    //
    //                     //Same
    //        LabEventTest.LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = new LabEventTest.LibraryConstructionEntityBuilder(
    //                bettaLimsMessageFactory, labEventFactory, labEventHandler,
    //                shearingEntityBuilder.getShearingCleanupPlate(), shearingEntityBuilder.getShearCleanPlateBarcode(),
    //                shearingEntityBuilder.getShearingPlate(), LabEventTest.NUM_POSITIONS_IN_RACK).invoke();
    //
    //
    //                     //Same with Fewer GSWashes
    //        LabEventTest.HybridSelectionEntityBuilder hybridSelectionEntityBuilder = new LabEventTest.HybridSelectionEntityBuilder(
    //                bettaLimsMessageFactory, labEventFactory, labEventHandler,
    //                libraryConstructionEntityBuilder.getPondRegRack(),
    //                libraryConstructionEntityBuilder.getPondRegRackBarcode(),
    //                libraryConstructionEntityBuilder.getPondRegTubeBarcodes()).invoke();
    //
    //
    //                     //Same
    //        LabEventTest.QtpEntityBuilder qtpEntityBuilder = new LabEventTest.QtpEntityBuilder(bettaLimsMessageFactory, labEventFactory,
    //                                                                 labEventHandler,
    //                                                                 hybridSelectionEntityBuilder.getNormCatchRack(),
    //                                                                 hybridSelectionEntityBuilder.getNormCatchRackBarcode(),
    //                                                                 hybridSelectionEntityBuilder.getNormCatchBarcodes(),
    //                                                                 hybridSelectionEntityBuilder
    //                                                                         .getMapBarcodeToNormCatchTubes());
    //        qtpEntityBuilder.invoke();
    //
    //        IlluminaSequencingRunFactory illuminaSequencingRunFactory = new IlluminaSequencingRunFactory();
    //        IlluminaSequencingRun illuminaSequencingRun;
    //        try {
    //            illuminaSequencingRun = illuminaSequencingRunFactory.buildDbFree(new SolexaRunBean(
    //                    qtpEntityBuilder.getIlluminaFlowcell().getCartridgeBarcode(), "Run1", new Date(), "SL-HAL",
    //                    File.createTempFile("RunDir", ".txt").getAbsolutePath(), null),
    //                                                                             qtpEntityBuilder.getIlluminaFlowcell());
    //        } catch (IOException e) {
    //            throw new RuntimeException(e);
    //        }
    //
    //        Map.Entry<String, TwoDBarcodedTube> stringTwoDBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
    //        LabEventTest.ListTransfersFromStart transferTraverserCriteria = new LabEventTest.ListTransfersFromStart();
    //        stringTwoDBarcodedTubeEntry.getValue().evaluateCriteria(transferTraverserCriteria,
    //                                                                TransferTraverserCriteria.TraversalDirection.Descendants);
    //        List<String> labEventNames = transferTraverserCriteria.getLabEventNames();
    //        Assert.assertEquals(labEventNames.size(), 13, "Wrong number of transfers");
    //
    //        Assert.assertEquals(illuminaSequencingRun.getSampleCartridge().iterator().next(),
    //                            qtpEntityBuilder.getIlluminaFlowcell(), "Wrong flowcell");
    //
    //    }


    /**
     * Build a rack entity
     *
     * @param mapBarcodeToTubes source tubes
     * @param barcode             barcode Barcode
     * @param barcodeByPositionMap       JAXB list of tube barcodes
     *
     * @return entity
     */
    private RackOfTubes buildRack ( Map<String, TwoDBarcodedTube> mapBarcodeToTubes, String barcode,
                                    Map<String, String> barcodeByPositionMap ) {
        // todo jmt fix label
        RackOfTubes rackOfTubes = new RackOfTubes ( barcode + "_" + Long.toString (
                System.currentTimeMillis () ), RackOfTubes.RackType.Matrix96 );
        for ( Map.Entry<String, String> tubeEntry : barcodeByPositionMap.entrySet() ) {
            TwoDBarcodedTube twoDBarcodedTube = mapBarcodeToTubes.get(tubeEntry.getValue());
            if ( twoDBarcodedTube == null ) {
                twoDBarcodedTube = new TwoDBarcodedTube ( tubeEntry.getValue() );
                mapBarcodeToTubes.put ( tubeEntry.getValue(), twoDBarcodedTube );
            }
            rackOfTubes.getContainerRole().addContainedVessel(twoDBarcodedTube, VesselPosition.getByName(
                    tubeEntry.getKey()));
        }
        rackOfTubes.makeDigest ();
        return rackOfTubes;
    }
}
