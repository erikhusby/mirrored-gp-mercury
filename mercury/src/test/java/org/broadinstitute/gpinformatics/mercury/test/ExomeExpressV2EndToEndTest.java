package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSetVolumeConcentration;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSetVolumeConcentrationStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceTestProducer;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemOfRecord;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpJaxbBuilder;
import org.easymock.EasyMock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;

/**
 * Test Exome Express in Mercury
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ExomeExpressV2EndToEndTest extends BaseEventTest {

    public static final String FCT_TICKET = "FCT-1";
    public static List<String> RACK_COLUMNS = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11",
                                                            "12");
    public static List<String> RACK_ROWS = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H");

    @Test(groups = DATABASE_FREE)
    public void test() {
        expectedRouting = SystemOfRecord.System.MERCURY;

        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);
        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        BSPSetVolumeConcentration bspSetVolumeConcentration =  new BSPSetVolumeConcentrationStub();
        LabEventFactory labEventFactory = new LabEventFactory(testUserList, bspSetVolumeConcentration);

        labEventFactory.setEventHandlerSelector(getLabEventFactory().getEventHandlerSelector());

        List<ProductOrderSample> productOrderSamples = new ArrayList<>();
        final ProductOrder productOrder1 = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123",
                                                            new Product(
                                                                    "Test product", new ProductFamily(
                                                                    "Test product family"), "test", "1234", null, null,
                                                                    10000, 20000, 100,
                                                                    40, null, null, true,
                                                                    Workflow.AGILENT_EXOME_EXPRESS, false, "agg type"),
                                                            new ResearchProject(101L, "Test RP", "Test synopsis",
                                                                                false,
                                                                                ResearchProject.RegulatoryDesignation.RESEARCH_ONLY));
        final String productOrder1Key = "PD0-1";
        String jiraTicketKey = productOrder1Key;
        productOrder1.setJiraTicketKey(jiraTicketKey);
        productOrder1.setOrderStatus(ProductOrder.OrderStatus.Submitted);


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

                ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder((String) arguments[0]);
                if ((arguments[0]).equals(productOrder1Key)) {
                    dummyProductOrder = productOrder1;
                }
                return dummyProductOrder;
            }
        });
        labBatchEJB.setProductOrderDao(mockProductOrderDao);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.postConstruct();

        // Bucket for Shearing - enters from workflow?

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


        List<String> shearingTubeBarcodes = new ArrayList<>()/*Arrays.asList("SH1", "SH2", "SH3")*/;
        Map<String, String> barcodesByRackPositions = new HashMap<>();

        // starting rack
        Map<String, BarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();
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
            BarcodedTube bspAliquot = new BarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(bspStock, MercurySample.MetadataSource.BSP));
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

        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                                              new HashSet<LabVessel>(mapBarcodeToTube.values()),
                                              LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());

        bucketBatchAndDrain(mapBarcodeToTube, productOrder1, workflowBatch, "1");
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                                                                                  String.valueOf(
                                                                                          LabEventTest.NUM_POSITIONS_IN_RACK),
                                                                                  "1", true);

        LabEventHandler leHandler = getLabEventHandler();

        Bucket workingBucket =
                createAndPopulateBucket(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(), productOrder1,
                                        "Shearing Bucket");
        // Lab Event Factory should have put tubes into the Bucket after normalization
        Assert.assertEquals(LabEventTest.NUM_POSITIONS_IN_RACK, workingBucket.getBucketEntries().size());
        for (BucketEntry bucketEntry : workingBucket.getBucketEntries()) {
            bucketEntry.setLabBatch(workflowBatch);
        }

        leHandler = getLabEventHandler();
        // Bucket for Shearing - enters from workflow?

        ExomeExpressShearingJaxbBuilder exexJaxbBuilder =
                new ExomeExpressShearingJaxbBuilder(bettaLimsMessageTestFactory,
                                                    new ArrayList<>(picoPlatingEntityBuilder.getNormBarcodeToTubeMap()
                                                                                            .keySet()), "",
                                                    picoPlatingEntityBuilder.getNormalizationBarcode());
        exexJaxbBuilder.invoke();

        //TODO Should this validate be on the tube formation?
//        LabEventTest.validateWorkflow(LabEventType.SHEARING_TRANSFER.getName(),
//                                      picoPlatingEntityBuilder.getNormBarcodeToTubeMap().values());
        PlateTransferEventType plateToShearXfer = exexJaxbBuilder.getShearTransferEventJaxb();
//        LabEvent shearEvent = labEventFactory.buildFromBettaLimsRackToPlateDbFree(plateToShearXfer,
//                picoPlatingEntityBuilder.getNormTubeFormation(),
//                new StaticPlate("NormPlate", StaticPlate.PlateType.Eppendorf96));
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        TubeFormation normTubeFormation = picoPlatingEntityBuilder.getNormTubeFormation();
        mapBarcodeToVessel.put(normTubeFormation.getLabel(), normTubeFormation);
        for (BarcodedTube barcodedTube : normTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }

        LabEvent shearEvent = labEventFactory.buildFromBettaLims(plateToShearXfer, mapBarcodeToVessel);
        StaticPlate shearPlate = (StaticPlate) shearEvent.getTargetLabVessels().iterator().next();
        leHandler.processEvent(shearEvent);

//        LabEventTest.validateWorkflow(LabEventType.COVARIS_LOADED.getName(), shearEvent.getTargetLabVessels());
//        // When the above event is executed, the items should be removed from the bucket.
        PlateEventType covarisxfer = exexJaxbBuilder.getCovarisLoadEventJaxb();
        LabEvent covarisEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(covarisxfer, shearPlate);
        leHandler.processEvent(covarisEvent);
//        StaticPlate covarisPlate = (StaticPlate) covarisEvent.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow(LabEventType.POST_SHEARING_TRANSFER_CLEANUP.getName(), shearPlate);
//        LabEvent postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
//                exexJaxbBuilder.getPostShearingTransferCleanupEventJaxb(), shearPlate, null);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(shearPlate.getLabel(), shearPlate);
        LabEvent postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLims(
                exexJaxbBuilder.getPostShearingTransferCleanupEventJaxb(), mapBarcodeToVessel);
        leHandler.processEvent(postShearingTransferCleanupEntity);
        StaticPlate shearingCleanupPlate =
                (StaticPlate) postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow(LabEventType.SHEARING_QC.getName(), shearingCleanupPlate);
//        LabEvent shearingQcEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
//                exexJaxbBuilder.getShearingQcEventJaxb(), shearingCleanupPlate, null);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(shearingCleanupPlate.getLabel(), shearingCleanupPlate);
        LabEvent shearingQcEntity = labEventFactory.buildFromBettaLims(
                exexJaxbBuilder.getShearingQcEventJaxb(), mapBarcodeToVessel);
        leHandler.processEvent(shearingQcEntity);

        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = new LibraryConstructionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, leHandler, shearingCleanupPlate,
                postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next().getLabel(),
                shearPlate, LabEventTest.NUM_POSITIONS_IN_RACK, "testPrefix",
                LibraryConstructionEntityBuilder.Indexing.DUAL, LibraryConstructionJaxbBuilder.PondType.REGULAR).invoke();

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
        HybridSelectionEntityBuilder hybridSelectionEntityBuilder = new HybridSelectionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, leHandler,
                libraryConstructionEntityBuilder.getPondRegRack(),
                libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), "testPrefix").invoke(false);

        // Pooling calculator
        // Strip Tube B
        // Create Flowcell JIRA
        QtpEntityBuilder qtpEntityBuilder = new QtpEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, leHandler,
                Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchRack()),
                Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchRackBarcode()),
                Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchBarcodes()),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), "testPrefix");
        qtpEntityBuilder.invoke(true, QtpJaxbBuilder.PcrType.VIIA_7);

        String flowcellBarcode = "flowcell" + new Date().getTime();

        final LabVessel denatureSource =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        LabBatch fctBatch = new LabBatch(FCT_TICKET, LabBatch.LabBatchType.FCT,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, denatureSource, BigDecimal.TEN);

        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                new HiSeq2500FlowcellEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, leHandler,
                                                   qtpEntityBuilder.getDenatureRack(), flowcellBarcode, "testPrefix",
                                                   FCT_TICKET,
                                                   ProductionFlowcellPath.DILUTION_TO_FLOWCELL, null, 2).invoke();
        // MiSeq reagent block transfer message
        String miSeqReagentKitBarcode = "MiSeqReagentKit" + new Date().getTime();

        Map<String, BarcodedTube> tubeMap = new HashMap<>();
        final String denatureBarcode = qtpEntityBuilder.getDenatureRack().getLabel();
        tubeMap.put(denatureBarcode, new BarcodedTube(denatureBarcode));
        Map<String, String> tubePositionMap = new HashMap<>();
        tubePositionMap.put(denatureBarcode, "A01");
        TubeFormation denatureRack = buildTubeFormation(tubeMap, denatureBarcode, tubePositionMap);

        Map<String, VesselPosition> denatureRackMap = new HashMap<>();
        for (VesselPosition vesselPosition : denatureRack.getVesselGeometry().getVesselPositions()) {
            BarcodedTube tube = denatureRack.getContainerRole().getVesselAtPosition(vesselPosition);
            if (tube != null) {
                denatureRackMap.put(denatureBarcode, vesselPosition);
            }
        }
        Assert.assertNotNull(denatureRackMap.get(denatureBarcode));
        Assert.assertEquals(denatureRackMap.values().size(), 1);

        // Register run
        IlluminaSequencingRunFactory illuminaSequencingRunFactory =
                new IlluminaSequencingRunFactory(EasyMock.createNiceMock(JiraCommentUtil.class));
        try {
            IlluminaSequencingRun illuminaSequencingRun = illuminaSequencingRunFactory.buildDbFree(new SolexaRunBean(
                    hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell().getCartridgeBarcode(), "Run1", new Date(),
                    "SL-HAL", File.createTempFile("RunDir", ".txt").getAbsolutePath(), miSeqReagentKitBarcode),
                                                                                                   hiSeq2500FlowcellEntityBuilder
                                                                                                           .getIlluminaFlowcell());
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

    }

    /**
     * Build a rack entity
     *
     * @param mapBarcodeToTubes    source tubes
     * @param barcode              barcode Barcode
     * @param barcodeByPositionMap JAXB list of tube barcodes
     *
     * @return entity
     */
    private TubeFormation buildTubeFormation(Map<String, BarcodedTube> mapBarcodeToTubes, String barcode,
                                             Map<String, String> barcodeByPositionMap) {

        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();

        for (Map.Entry<String, String> tubeEntry : barcodeByPositionMap.entrySet()) {
            BarcodedTube barcodedTube = mapBarcodeToTubes.get(tubeEntry.getValue());
            if (barcodedTube == null) {
                barcodedTube = new BarcodedTube(tubeEntry.getValue());
                mapBarcodeToTubes.put(tubeEntry.getValue(), barcodedTube);
            }
            mapPositionToTube.put(VesselPosition.getByName(tubeEntry.getValue()), barcodedTube);
        }
        TubeFormation formation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);

        return formation;
    }
}
