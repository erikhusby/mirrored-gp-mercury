package org.broadinstitute.gpinformatics.mercury.test;

//import com.jprofiler.api.agent.Controller;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.BaitReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;

/**
 * Test messaging
 */
@SuppressWarnings({"FeatureEnvy", "OverlyCoupledClass", "OverlyCoupledMethod", "OverlyLongMethod"})
public class LabEventTest {
    public static final int NUM_POSITIONS_IN_RACK = 96;

    public static final String POND_REGISTRATION_TUBE_PREFIX = "PondReg";
    private static Map<String, ProductOrder> mapKeyToProductOrder = new HashMap<String, ProductOrder>();

    private final LabEventFactory.LabEventRefDataFetcher labEventRefDataFetcher = new LabEventFactory.LabEventRefDataFetcher() {
        @Override
        public Person getOperator(String userId) {
            return new Person(userId);
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

        @Override
        public TraversalControl evaluateVesselPreOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
            if (labEvent != null) {
                if (hopCount > this.hopCount) {
                    this.hopCount = hopCount;
                    labEventNames.add(labEvent.getLabEventType().getName() + " into " +
                            labEvent.getTargetLabVessels().iterator().next().getLabel());
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselInOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
        }

        @Override
        public void evaluateVesselPostOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
        }

        public List<String> getLabEventNames() {
            return labEventNames;
        }
    }

    /**
     * Build object graph for Hybrid Selection messages, verify chain of events.
     */
    @Test(groups = {DATABASE_FREE})
    public void testHybridSelection() {
//        Controller.startCPURecording(true);

        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>();
        ProductOrder productOrder = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123",
                new Product("Test product", new ProductFamily("Test product family"), "test", "1234", null, null, 10000,
                        20000, 100, 40, null, null, true, "Hybrid Selection", false),
                new ResearchProject(101L, "Test RP", "Test synopsis", false));
        String jiraTicketKey = "PD0-1";
        productOrder.setJiraTicketKey(jiraTicketKey);
        mapKeyToProductOrder.put(jiraTicketKey, productOrder);

        // starting rack
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for (int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK; rackPosition++) {
            String barcode = "R" + rackPosition;
            String bspStock = "SM-" + rackPosition;
            productOrderSamples.add(new ProductOrderSample(bspStock));
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(jiraTicketKey, bspStock));
            mapBarcodeToTube.put(barcode, bspAliquot);
        }

        // Messaging
        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);
        LabEventHandler labEventHandler = new LabEventHandler();

        PreFlightEntityBuilder preFlightEntityBuilder = new PreFlightEntityBuilder(
                bettaLimsMessageFactory, labEventFactory, labEventHandler, mapBarcodeToTube).invoke();

        ShearingEntityBuilder shearingEntityBuilder = new ShearingEntityBuilder(mapBarcodeToTube, preFlightEntityBuilder.getRackOfTubes(),
                bettaLimsMessageFactory, labEventFactory, labEventHandler, preFlightEntityBuilder.getRackBarcode()).invoke();

        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = new LibraryConstructionEntityBuilder(
                bettaLimsMessageFactory, labEventFactory, labEventHandler,
                shearingEntityBuilder.getShearingCleanupPlate(), shearingEntityBuilder.getShearCleanPlateBarcode(),
                shearingEntityBuilder.getShearingPlate(), NUM_POSITIONS_IN_RACK).invoke();

        HybridSelectionEntityBuilder hybridSelectionEntityBuilder = new HybridSelectionEntityBuilder(
                bettaLimsMessageFactory, labEventFactory, labEventHandler,
                libraryConstructionEntityBuilder.getPondRegRack(), libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                libraryConstructionEntityBuilder.getPondRegTubeBarcodes()).invoke();

        QtpEntityBuilder qtpEntityBuilder = new QtpEntityBuilder(bettaLimsMessageFactory, labEventFactory, labEventHandler,
                hybridSelectionEntityBuilder.getNormCatchRack(), hybridSelectionEntityBuilder.getNormCatchRackBarcode(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes());
        qtpEntityBuilder.invoke();

        IlluminaSequencingRunFactory illuminaSequencingRunFactory = new IlluminaSequencingRunFactory();
        IlluminaSequencingRun illuminaSequencingRun;
        try {
            illuminaSequencingRun = illuminaSequencingRunFactory.buildDbFree(
                    new SolexaRunBean(qtpEntityBuilder.getIlluminaFlowcell().getCartridgeBarcode(), "Run1", new Date(), "SL-HAL",
                            File.createTempFile("RunDir", ".txt").getAbsolutePath(), null), qtpEntityBuilder.getIlluminaFlowcell());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map.Entry<String, TwoDBarcodedTube> stringTwoDBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        stringTwoDBarcodedTubeEntry.getValue().evaluateCriteria(transferTraverserCriteria,
                TransferTraverserCriteria.TraversalDirection.Descendants);
        List<String> labEventNames = transferTraverserCriteria.getLabEventNames();
        Assert.assertEquals(labEventNames.size(), 13, "Wrong number of transfers");

        Assert.assertEquals(illuminaSequencingRun.getSampleCartridge().iterator().next(), qtpEntityBuilder.getIlluminaFlowcell(),
                "Wrong flowcell");

//        Controller.stopCPURecording();
    }

    /**
     * Build object graph for Whole Genome Shotgun messages, verify chain of events.
     */
    @Test(groups = {DATABASE_FREE})
    public void testWholeGenomeShotgun() {
//        Controller.startCPURecording(true);

        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>();
        ProductOrder productOrder = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123",
                new Product("Test product", new ProductFamily("Test product family"), "test", "1234", null, null, 10000,
                        20000, 100, 40, null, null, true, "Whole Genome Shotgun", false),
                new ResearchProject(101L, "Test RP", "Test synopsis", false));
        String jiraTicketKey = "PD0-2";
        productOrder.setJiraTicketKey(jiraTicketKey);
        mapKeyToProductOrder.put(jiraTicketKey, productOrder);

        // starting rack
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for (int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK; rackPosition++) {
            String barcode = "R" + rackPosition;
            String bspStock = "SM-" + rackPosition;
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(jiraTicketKey, bspStock));
            mapBarcodeToTube.put(barcode, bspAliquot);
        }

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);
        LabEventHandler labEventHandler = new LabEventHandler();

        PreFlightEntityBuilder preFlightEntityBuilder = new PreFlightEntityBuilder(
                bettaLimsMessageFactory, labEventFactory, labEventHandler, mapBarcodeToTube).invoke();

        ShearingEntityBuilder shearingEntityBuilder = new ShearingEntityBuilder(mapBarcodeToTube, preFlightEntityBuilder.getRackOfTubes(),
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
        for (int i = 0; i < NUM_POSITIONS_IN_RACK / 4; i++) {
            // SageLoading
            String sageCassetteBarcode = "SageCassette" + i;
            PlateTransferEventType sageLoadingJaxb = bettaLimsMessageFactory.buildRackToPlate("SageLoading",
                    libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                    libraryConstructionEntityBuilder.getPondRegTubeBarcodes().subList(i * 4, i * 4 + 4), sageCassetteBarcode);
            // todo jmt SAGE section
            LabEvent sageLoadingEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(sageLoadingJaxb,
                    libraryConstructionEntityBuilder.getPondRegRack(), null);
            labEventHandler.processEvent(sageLoadingEntity);
            StaticPlate sageCassette = (StaticPlate) sageLoadingEntity.getTargetLabVessels().iterator().next();

            // SageLoaded

            // SageUnloading
            PlateTransferEventType sageUnloadingJaxb = bettaLimsMessageFactory.buildPlateToRack("SageUnloading",
                    sageCassetteBarcode, sageUnloadBarcode, sageUnloadTubeBarcodes.subList(i * 4, i * 4 + 4));
            LabEvent sageUnloadEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(sageUnloadingJaxb,
                    sageCassette, mapBarcodeToSageUnloadTubes);
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
        RackOfTubes sageUnloadRackRearrayed = new RackOfTubes("sageUnloadRearray", RackOfTubes.RackType.Matrix96);
        List<TwoDBarcodedTube> sageUnloadTubes = new ArrayList<TwoDBarcodedTube>(mapBarcodeToSageUnloadTubes.values());
        for (int i = 0; i < NUM_POSITIONS_IN_RACK; i++) {
            sageUnloadRackRearrayed.getContainerRole().addContainedVessel(sageUnloadTubes.get(i),
                    VesselPosition.getByName(bettaLimsMessageFactory.buildWellName(i + 1)));
        }
        sageUnloadRackRearrayed.makeDigest();
        LabEvent sageCleanupEntity = labEventFactory.buildFromBettaLimsRackToRackDbFree(sageCleanupJaxb,
                sageUnloadRackRearrayed, new HashMap<String, TwoDBarcodedTube>());
        labEventHandler.processEvent(sageCleanupEntity);
        RackOfTubes sageCleanupRack = (RackOfTubes) sageCleanupEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(sageCleanupRack.getSampleInstances().size(), NUM_POSITIONS_IN_RACK, "Wrong number of sage cleanup samples");

        new QtpEntityBuilder(bettaLimsMessageFactory, labEventFactory, labEventHandler, sageCleanupRack,
                sageCleanupBarcode, sageCleanupTubeBarcodes, mapBarcodeToSageUnloadTubes).invoke();

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
//        Project project = new BasicProject("LabEventTesting", new JiraTicket(new JiraServiceStub(),"TP-0","0"));
//        WorkflowDescription workflowDescription = new WorkflowDescription("WGS", null, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);
//        BasicProjectPlan projectPlan = new BasicProjectPlan(project, "To test whole genome shotgun", workflowDescription);

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
        LabEventHandler labEventHandler = new LabEventHandler();
        BuildIndexPlate buildIndexPlate = new BuildIndexPlate("IndexPlate").invoke();
        FluidigmMessagesBuilder fluidigmMessagesBuilder = new FluidigmMessagesBuilder("", bettaLimsMessageFactory, labEventFactory,
                labEventHandler, mapBarcodeToTube, buildIndexPlate.getIndexPlate());
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

        private FluidigmMessagesBuilder(String testPrefix, BettaLimsMessageFactory bettaLimsMessageFactory, LabEventFactory labEventFactory,
                                        LabEventHandler labEventHandler, Map<String, TwoDBarcodedTube> mapBarcodeToTube, StaticPlate indexPlate) {
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
            fluidigmSampleInputJaxb = this.bettaLimsMessageFactory.buildRackToPlate(
                    "FluidigmSampleInput", rackBarcode, tubeBarcodes, chipBarcode);
            fluidigmSampleInputJaxb.getSourcePlate().setSection(SBSSection.P96COLS1_6BYROW.getSectionName());
            PositionMapType sourcePositionMap = buildFluidigmPositionMap(tubeBarcodes,
                    fluidigmSampleInputJaxb.getSourcePlate().getBarcode());
            fluidigmSampleInputJaxb.setSourcePositionMap(sourcePositionMap);
            fluidigmSampleInputJaxb.getPlate().setPhysType(StaticPlate.PlateType.Fluidigm48_48AccessArrayIFC.getDisplayName());
            fluidigmSampleInputJaxb.getPlate().setSection(SBSSection.P384COLS4_6BYROW.getSectionName());
            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            bettaLIMSMessage.getPlateTransferEvent().add(fluidigmSampleInputJaxb);
            messageList.add(bettaLIMSMessage);
            bettaLimsMessageFactory.advanceTime();

            // FluidigmIndexedAdapterInput plate P96COLS1-6BYROW to chip P384COLS4-6BYROW
            fluidigmIndexedAdapterInputJaxb = this.bettaLimsMessageFactory.buildPlateToPlate(
                    "FluidigmIndexedAdapterInput", indexPlate.getLabel(), chipBarcode);
            fluidigmIndexedAdapterInputJaxb.getSourcePlate().setPhysType(StaticPlate.PlateType.IndexedAdapterPlate96.getDisplayName());
            fluidigmIndexedAdapterInputJaxb.getSourcePlate().setSection(SBSSection.P96COLS1_6BYROW.getSectionName());
            fluidigmIndexedAdapterInputJaxb.getPlate().setPhysType(StaticPlate.PlateType.Fluidigm48_48AccessArrayIFC.getDisplayName());
            fluidigmIndexedAdapterInputJaxb.getPlate().setSection(SBSSection.P384COLS4_6BYROW.getSectionName());
            BettaLIMSMessage bettaLIMSMessage1 = new BettaLIMSMessage();
            bettaLIMSMessage1.getPlateTransferEvent().add(fluidigmIndexedAdapterInputJaxb);
            messageList.add(bettaLIMSMessage1);
            bettaLimsMessageFactory.advanceTime();

            // FluidigmHarvestingToRack chip P384COLS4-6BYROW to rack P96COLS1-6BYROW
            harvestRackBarcode = "Harvest" + testPrefix;
            List<String> harvestTubeBarcodes = new ArrayList<String>();
            for (int rackPosition = 1; rackPosition <= mapBarcodeToTube.size(); rackPosition++) {
                harvestTubeBarcodes.add("Harvest" + testPrefix + rackPosition);
            }
            fluidigmHarvestingToRackJaxb = this.bettaLimsMessageFactory.buildPlateToRack(
                    "FluidigmHarvestingToRack", chipBarcode, harvestRackBarcode, harvestTubeBarcodes);
            fluidigmHarvestingToRackJaxb.getSourcePlate().setPhysType(StaticPlate.PlateType.Fluidigm48_48AccessArrayIFC.getDisplayName());
            fluidigmHarvestingToRackJaxb.getSourcePlate().setSection(SBSSection.P384COLS4_6BYROW.getSectionName());
            fluidigmHarvestingToRackJaxb.setPositionMap(buildFluidigmPositionMap(tubeBarcodes,
                    fluidigmSampleInputJaxb.getSourcePlate().getBarcode()));
            fluidigmHarvestingToRackJaxb.getPlate().setSection(SBSSection.P96COLS1_6BYROW.getSectionName());
            BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
            bettaLIMSMessage2.getPlateTransferEvent().add(fluidigmIndexedAdapterInputJaxb);
            messageList.add(bettaLIMSMessage2);
            bettaLimsMessageFactory.advanceTime();
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
                    mapBarcodeToTube, null);
            labEventHandler.processEvent(fluidigmSampleInputEntity);
            // asserts
            StaticPlate chip = (StaticPlate) fluidigmSampleInputEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(chip.getSampleInstances().size(), mapBarcodeToTube.size(), "Wrong number of sample instances");

            LabEvent fluidigmIndexedAdapterInputEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    fluidigmIndexedAdapterInputJaxb, indexPlate, chip);
            labEventHandler.processEvent(fluidigmIndexedAdapterInputEntity);

            LabEvent fluidigmHarvestingToRackEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(
                    fluidigmHarvestingToRackJaxb, chip, mapBarcodeToHarvestTube);
            labEventHandler.processEvent(fluidigmHarvestingToRackEntity);
            // asserts
            RackOfTubes harvestRack = (RackOfTubes) fluidigmHarvestingToRackEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(harvestRack.getSampleInstances().size(), mapBarcodeToTube.size(), "Wrong number of sample instances");
        }
    }

    private static void validateWorkflow(String nextEventTypeName, Collection<? extends LabVessel> tubes) {
        List<LabVessel> labVessels = new ArrayList<LabVessel>(tubes);
        validateWorkflow(nextEventTypeName, labVessels);
    }

    private static void validateWorkflow(String nextEventTypeName, LabVessel labVessel) {
        List<LabVessel> labVessels = new ArrayList<LabVessel>();
        labVessels.add(labVessel);
        validateWorkflow(nextEventTypeName, labVessels);
    }

    private static void validateWorkflow(String nextEventTypeName, List<LabVessel> labVessels) {
        WorkflowLoader workflowLoader = new WorkflowLoader();
        WorkflowConfig workflowConfig = workflowLoader.load();
        for (LabVessel labVessel : labVessels) {
            for (SampleInstance sampleInstance : labVessel.getSampleInstances()) {
                ProductOrder productOrder = mapKeyToProductOrder.get(sampleInstance.getStartingSample().getProductOrderKey());
                // get workflow name from product order
                ProductWorkflowDef productWorkflowDef = workflowConfig.getWorkflowByName(productOrder.getProduct().getWorkflowName());
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
        private RackOfTubes rackOfTubes;
        private String rackBarcode;

        public PreFlightEntityBuilder(BettaLimsMessageFactory bettaLimsMessageFactory,
                                      LabEventFactory labEventFactory, LabEventHandler labEventHandler, Map<String, TwoDBarcodedTube> mapBarcodeToTube) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
            this.mapBarcodeToTube = mapBarcodeToTube;
        }

        public PreFlightEntityBuilder invoke() {
            PreFlightJaxbBuilder preFlightJaxbBuilder = new PreFlightJaxbBuilder(bettaLimsMessageFactory, "",
                    new ArrayList<String>(mapBarcodeToTube.keySet()));
            preFlightJaxbBuilder.invoke();
            rackBarcode = preFlightJaxbBuilder.getRackBarcode();

            // PreflightPicoSetup 1
            validateWorkflow("PreflightPicoSetup", mapBarcodeToTube.values());
            LabEvent preflightPicoSetup1Entity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    preFlightJaxbBuilder.getPreflightPicoSetup1(), mapBarcodeToTube, null);
            labEventHandler.processEvent(preflightPicoSetup1Entity);
            // asserts
            rackOfTubes = (RackOfTubes) preflightPicoSetup1Entity.getSourceLabVessels().iterator().next();
            StaticPlate preflightPicoSetup1Plate = (StaticPlate) preflightPicoSetup1Entity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(preflightPicoSetup1Plate.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

            // PreflightPicoSetup 2
            validateWorkflow("PreflightPicoSetup", mapBarcodeToTube.values());
            LabEvent preflightPicoSetup2Entity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    preFlightJaxbBuilder.getPreflightPicoSetup2(), rackOfTubes, null);
            labEventHandler.processEvent(preflightPicoSetup2Entity);
            // asserts
            StaticPlate preflightPicoSetup2Plate = (StaticPlate) preflightPicoSetup2Entity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(preflightPicoSetup2Plate.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

            // PreflightNormalization
            validateWorkflow("PreflightNormalization", mapBarcodeToTube.values());
            LabEvent preflightNormalization = labEventFactory.buildFromBettaLimsRackEventDbFree(
                    preFlightJaxbBuilder.getPreflightNormalization(), rackOfTubes, mapBarcodeToTube);
            labEventHandler.processEvent(preflightNormalization);
            // asserts
            Assert.assertEquals(rackOfTubes.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

            // PreflightPostNormPicoSetup 1
            validateWorkflow("PreflightPostNormPicoSetup", mapBarcodeToTube.values());
            LabEvent preflightPostNormPicoSetup1Entity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    preFlightJaxbBuilder.getPreflightPostNormPicoSetup1(), rackOfTubes, null);
            labEventHandler.processEvent(preflightPostNormPicoSetup1Entity);
            // asserts
            StaticPlate preflightPostNormPicoSetup1Plate = (StaticPlate) preflightPostNormPicoSetup1Entity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(preflightPostNormPicoSetup1Plate.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

            // PreflightPostNormPicoSetup 2
            validateWorkflow("PreflightPostNormPicoSetup", mapBarcodeToTube.values());
            LabEvent preflightPostNormPicoSetup2Entity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    preFlightJaxbBuilder.getPreflightPostNormPicoSetup2(), rackOfTubes, null);
            labEventHandler.processEvent(preflightPostNormPicoSetup2Entity);
            // asserts
            StaticPlate preflightPostNormPicoSetup2Plate = (StaticPlate) preflightPostNormPicoSetup2Entity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(preflightPostNormPicoSetup2Plate.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

            return this;
        }

        public String getRackBarcode() {
            return rackBarcode;
        }

        public RackOfTubes getRackOfTubes() {
            return rackOfTubes;
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

        public PreFlightJaxbBuilder(BettaLimsMessageFactory bettaLimsMessageFactory, String testPrefix, List<String> tubeBarcodes) {
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
            preflightPicoSetup1 = this.bettaLimsMessageFactory.buildRackToPlate("PreflightPicoSetup", rackBarcode,
                    tubeBarcodes, "PreflightPicoPlate1" + testPrefix);
            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            bettaLIMSMessage.getPlateTransferEvent().add(preflightPicoSetup1);
            messageList.add(bettaLIMSMessage);
            bettaLimsMessageFactory.advanceTime();

            preflightPicoSetup2 = this.bettaLimsMessageFactory.buildRackToPlate("PreflightPicoSetup", rackBarcode,
                    tubeBarcodes, "PreflightPicoPlate2" + testPrefix);
            BettaLIMSMessage bettaLIMSMessage1 = new BettaLIMSMessage();
            bettaLIMSMessage1.getPlateTransferEvent().add(preflightPicoSetup2);
            messageList.add(bettaLIMSMessage1);
            bettaLimsMessageFactory.advanceTime();

            preflightNormalization = this.bettaLimsMessageFactory.buildRackEvent("PreflightNormalization", rackBarcode, tubeBarcodes);
            BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
            bettaLIMSMessage2.getPlateEvent().add(preflightNormalization);
            messageList.add(bettaLIMSMessage2);
            bettaLimsMessageFactory.advanceTime();

            preflightPostNormPicoSetup1 = this.bettaLimsMessageFactory.buildRackToPlate("PreflightPostNormPicoSetup",
                    rackBarcode, tubeBarcodes, "PreflightPostNormPicoPlate1" + testPrefix);
            BettaLIMSMessage bettaLIMSMessage3 = new BettaLIMSMessage();
            bettaLIMSMessage3.getPlateEvent().add(preflightPostNormPicoSetup1);
            messageList.add(bettaLIMSMessage3);
            bettaLimsMessageFactory.advanceTime();

            preflightPostNormPicoSetup2 = this.bettaLimsMessageFactory.buildRackToPlate("PreflightPostNormPicoSetup",
                    rackBarcode, tubeBarcodes, "PreflightPostNormPicoPlate2" + testPrefix);
            BettaLIMSMessage bettaLIMSMessage4 = new BettaLIMSMessage();
            bettaLIMSMessage4.getPlateEvent().add(preflightPostNormPicoSetup2);
            messageList.add(bettaLIMSMessage4);
            bettaLimsMessageFactory.advanceTime();

            return this;
        }
    }

    /**
     * Builds entity graph for Shearing events
     */
    public static class ShearingEntityBuilder {
        private final Map<String, TwoDBarcodedTube> mapBarcodeToTube;
        private RackOfTubes preflightRack;
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;
        private final String rackBarcode;
        private String shearPlateBarcode;
        private StaticPlate shearingPlate;
        private String shearCleanPlateBarcode;
        private StaticPlate shearingCleanupPlate;

        public ShearingEntityBuilder(Map<String, TwoDBarcodedTube> mapBarcodeToTube, RackOfTubes preflightRack,
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
            ShearingJaxbBuilder shearingJaxbBuilder = new ShearingJaxbBuilder(bettaLimsMessageFactory, new ArrayList<String>(mapBarcodeToTube.keySet()),
                    "", rackBarcode).invoke();
            this.shearPlateBarcode = shearingJaxbBuilder.getShearPlateBarcode();
            this.shearCleanPlateBarcode = shearingJaxbBuilder.getShearCleanPlateBarcode();

            // ShearingTransfer
            validateWorkflow("ShearingTransfer", mapBarcodeToTube.values());
            LabEvent shearingTransferEventEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    shearingJaxbBuilder.getShearingTransferEventJaxb(), preflightRack, null);
            labEventHandler.processEvent(shearingTransferEventEntity);
            // asserts
            shearingPlate = (StaticPlate) shearingTransferEventEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(shearingPlate.getSampleInstances().size(),
                    mapBarcodeToTube.size(), "Wrong number of sample instances");

            // PostShearingTransferCleanup
            validateWorkflow("PostShearingTransferCleanup", shearingPlate);
            LabEvent postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    shearingJaxbBuilder.getPostShearingTransferCleanupEventJaxb(), shearingPlate, null);
            labEventHandler.processEvent(postShearingTransferCleanupEntity);
            // asserts
            shearingCleanupPlate = (StaticPlate) postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(shearingCleanupPlate.getSampleInstances().size(),
                    mapBarcodeToTube.size(), "Wrong number of sample instances");
            Set<SampleInstance> sampleInstancesInWell = shearingCleanupPlate.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(sampleInstancesInWell.size(), 1, "Wrong number of sample instances in well");
            Assert.assertEquals(sampleInstancesInWell.iterator().next().getStartingSample().getSampleKey(),
                    mapBarcodeToTube.values().iterator().next().getSampleInstances().iterator().next().getStartingSample().getSampleKey(), "Wrong sample");

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
            shearingTransferEventJaxb = bettaLimsMessageFactory.buildRackToPlate(
                    "ShearingTransfer", rackBarcode, tubeBarcodeList, shearPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            bettaLIMSMessage.getPlateTransferEvent().add(shearingTransferEventJaxb);
            messageList.add(bettaLIMSMessage);
            bettaLimsMessageFactory.advanceTime();

            shearCleanPlateBarcode = "ShearCleanPlate" + testPrefix;
            postShearingTransferCleanupEventJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PostShearingTransferCleanup", shearPlateBarcode, shearCleanPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage1 = new BettaLIMSMessage();
            bettaLIMSMessage1.getPlateTransferEvent().add(postShearingTransferCleanupEventJaxb);
            messageList.add(bettaLIMSMessage1);
            bettaLimsMessageFactory.advanceTime();

            String shearQcPlateBarcode = "ShearQcPlate" + testPrefix;
            shearingQcEventJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "ShearingQC", shearCleanPlateBarcode, shearQcPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
            bettaLIMSMessage2.getPlateTransferEvent().add(shearingQcEventJaxb);
            messageList.add(bettaLIMSMessage2);
            bettaLimsMessageFactory.advanceTime();

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
        private RackOfTubes pondRegRack;
        private int numSamples;

        public LibraryConstructionEntityBuilder(BettaLimsMessageFactory bettaLimsMessageFactory,
                                                LabEventFactory labEventFactory, LabEventHandler labEventHandler, StaticPlate shearingCleanupPlate,
                                                String shearCleanPlateBarcode, StaticPlate shearingPlate, int numSamples) {
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

        public RackOfTubes getPondRegRack() {
            return pondRegRack;
        }

        public LibraryConstructionEntityBuilder invoke() {
            LibraryConstructionJaxbBuilder libraryConstructionJaxbBuilder = new LibraryConstructionJaxbBuilder(bettaLimsMessageFactory, "",
                    shearCleanPlateBarcode, "IndexPlate", numSamples).invoke();
            pondRegRackBarcode = libraryConstructionJaxbBuilder.getPondRegRackBarcode();
            pondRegTubeBarcodes = libraryConstructionJaxbBuilder.getPondRegTubeBarcodes();

            // EndRepair
            validateWorkflow("EndRepair", shearingCleanupPlate);
            LabEvent endRepairEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(libraryConstructionJaxbBuilder.getEndRepairJaxb(), shearingCleanupPlate);
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
            BuildIndexPlate buildIndexPlate = new BuildIndexPlate(libraryConstructionJaxbBuilder.getIndexPlateBarcode()).invoke();
            StaticPlate indexPlate = buildIndexPlate.getIndexPlate();
            LabEvent indexedAdapterLigationEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    libraryConstructionJaxbBuilder.getIndexedAdapterLigationJaxb(), indexPlate, shearingCleanupPlate);
            labEventHandler.processEvent(indexedAdapterLigationEntity);
            // asserts
            Set<SampleInstance> postIndexingSampleInstances = shearingCleanupPlate.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
            SampleInstance sampleInstance = postIndexingSampleInstances.iterator().next();
            MolecularIndexReagent molecularIndexReagent = (MolecularIndexReagent) sampleInstance.getReagents().iterator().next();
            Assert.assertEquals(molecularIndexReagent.getMolecularIndexingScheme().getName(), "Illumina_P7-M", "Wrong index");

            // AdapterLigationCleanup
            validateWorkflow("AdapterLigationCleanup", shearingCleanupPlate);
            LabEvent ligationCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    libraryConstructionJaxbBuilder.getLigationCleanupJaxb(), shearingCleanupPlate, null);
            labEventHandler.processEvent(ligationCleanupEntity);
            StaticPlate ligationCleanupPlate = (StaticPlate) ligationCleanupEntity.getTargetLabVessels().iterator().next();

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
                    libraryConstructionJaxbBuilder.getPondRegistrationJaxb(), pondCleanupPlate, mapBarcodeToPondRegTube);
            labEventHandler.processEvent(pondRegistrationEntity);
            // asserts
            pondRegRack = (RackOfTubes) pondRegistrationEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(pondRegRack.getSampleInstances().size(),
                    shearingPlate.getSampleInstances().size(), "Wrong number of sample instances");
            Set<SampleInstance> sampleInstancesInPondRegWell = pondRegRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(sampleInstancesInPondRegWell.size(), 1, "Wrong number of sample instances in position");
            Assert.assertEquals(sampleInstancesInPondRegWell.iterator().next().getStartingSample().getSampleKey(),
                    shearingPlate.getSampleInstances().iterator().next().getStartingSample().getSampleKey(), "Wrong sample");
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

        public BuildIndexPlate invoke() {
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
                MolecularIndexReagent molecularIndexReagent = new MolecularIndexReagent(new MolecularIndexingScheme(
                        new HashMap<MolecularIndexingScheme.PositionHint, MolecularIndex>() {{
                            put(MolecularIndexingScheme.IlluminaPositionHint.P7, new MolecularIndex(sequence));
                        }}));
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
            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            bettaLIMSMessage.getPlateEvent().add(endRepairJaxb);
            messageList.add(bettaLIMSMessage);
            bettaLimsMessageFactory.advanceTime();

            endRepairCleanupJaxb = bettaLimsMessageFactory.buildPlateEvent("EndRepairCleanup", shearCleanPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage1 = new BettaLIMSMessage();
            bettaLIMSMessage1.getPlateEvent().add(endRepairCleanupJaxb);
            messageList.add(bettaLIMSMessage1);
            bettaLimsMessageFactory.advanceTime();

            aBaseJaxb = bettaLimsMessageFactory.buildPlateEvent("ABase", shearCleanPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
            bettaLIMSMessage2.getPlateEvent().add(aBaseJaxb);
            messageList.add(bettaLIMSMessage2);
            bettaLimsMessageFactory.advanceTime();

            aBaseCleanupJaxb = bettaLimsMessageFactory.buildPlateEvent("ABaseCleanup", shearCleanPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage3 = new BettaLIMSMessage();
            bettaLIMSMessage3.getPlateEvent().add(aBaseCleanupJaxb);
            messageList.add(bettaLIMSMessage3);
            bettaLimsMessageFactory.advanceTime();

//            indexPlateBarcode = "IndexPlate" + testPrefix;
            indexedAdapterLigationJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "IndexedAdapterLigation", indexPlateBarcode, shearCleanPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage4 = new BettaLIMSMessage();
            bettaLIMSMessage4.getPlateTransferEvent().add(indexedAdapterLigationJaxb);
            messageList.add(bettaLIMSMessage4);
            bettaLimsMessageFactory.advanceTime();

            String ligationCleanupBarcode = "ligationCleanupPlate" + testPrefix;
            ligationCleanupJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "AdapterLigationCleanup", shearCleanPlateBarcode, ligationCleanupBarcode);
            BettaLIMSMessage bettaLIMSMessage5 = new BettaLIMSMessage();
            bettaLIMSMessage5.getPlateTransferEvent().add(ligationCleanupJaxb);
            messageList.add(bettaLIMSMessage5);
            bettaLimsMessageFactory.advanceTime();

            pondEnrichmentJaxb = bettaLimsMessageFactory.buildPlateEvent("PondEnrichment", ligationCleanupBarcode);
            BettaLIMSMessage bettaLIMSMessage6 = new BettaLIMSMessage();
            bettaLIMSMessage6.getPlateEvent().add(pondEnrichmentJaxb);
            messageList.add(bettaLIMSMessage6);
            bettaLimsMessageFactory.advanceTime();

            String pondCleanupBarcode = "pondCleanupPlate" + testPrefix;
            pondCleanupJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "HybSelPondEnrichmentCleanup", ligationCleanupBarcode, pondCleanupBarcode);
            BettaLIMSMessage bettaLIMSMessage7 = new BettaLIMSMessage();
            bettaLIMSMessage7.getPlateTransferEvent().add(pondCleanupJaxb);
            messageList.add(bettaLIMSMessage7);
            bettaLimsMessageFactory.advanceTime();

            pondRegRackBarcode = "PondReg" + testPrefix;
            pondRegTubeBarcodes = new ArrayList<String>();
            for (int rackPosition = 1; rackPosition <= numSamples; rackPosition++) {
                pondRegTubeBarcodes.add(POND_REGISTRATION_TUBE_PREFIX + testPrefix + rackPosition);
            }
            pondRegistrationJaxb = bettaLimsMessageFactory.buildPlateToRack(
                    "PondRegistration", pondCleanupBarcode, pondRegRackBarcode, pondRegTubeBarcodes);
            BettaLIMSMessage bettaLIMSMessage8 = new BettaLIMSMessage();
            bettaLIMSMessage8.getPlateTransferEvent().add(pondRegistrationJaxb);
            messageList.add(bettaLIMSMessage8);
            bettaLimsMessageFactory.advanceTime();

            return this;
        }
    }

    /**
     * Builds entity graph for Hybrid Selection events
     */
    public static class HybridSelectionEntityBuilder {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;
        private final RackOfTubes pondRegRack;
        private final String pondRegRackBarcode;
        private final List<String> pondRegTubeBarcodes;
        private String normCatchRackBarcode;
        private List<String> normCatchBarcodes;
        private Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes;
        private RackOfTubes normCatchRack;

        public HybridSelectionEntityBuilder(BettaLimsMessageFactory bettaLimsMessageFactory,
                                            LabEventFactory labEventFactory, LabEventHandler labEventHandler, RackOfTubes pondRegRack,
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

        public RackOfTubes getNormCatchRack() {
            return normCatchRack;
        }

        public HybridSelectionEntityBuilder invoke() {
            HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = new HybridSelectionJaxbBuilder(bettaLimsMessageFactory, "", pondRegRackBarcode,
                    pondRegTubeBarcodes).invoke();
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
            for (ReceptacleType receptacleType : hybridSelectionJaxbBuilder.getPreSelPoolJaxb().getSourcePositionMap().getReceptacle()) {
                mapBarcodeToPreSelSource1Tube.put(receptacleType.getBarcode(), mapBarcodeToPondTube.get(receptacleType.getBarcode()));
            }
            Map<String, TwoDBarcodedTube> mapBarcodeToPreSelSource2Tube = new HashMap<String, TwoDBarcodedTube>();
            for (ReceptacleType receptacleType : hybridSelectionJaxbBuilder.getPreSelPoolJaxb2().getSourcePositionMap().getReceptacle()) {
                mapBarcodeToPreSelSource2Tube.put(receptacleType.getBarcode(), mapBarcodeToPondTube.get(receptacleType.getBarcode()));
            }
            LabEvent preSelPoolEntity = labEventFactory.buildFromBettaLimsRackToRackDbFree(hybridSelectionJaxbBuilder.getPreSelPoolJaxb(),
                    mapBarcodeToPreSelSource1Tube, mapBarcodeToPreSelPoolTube);
            labEventHandler.processEvent(preSelPoolEntity);
            RackOfTubes preSelPoolRack = (RackOfTubes) preSelPoolEntity.getTargetLabVessels().iterator().next();
            LabEvent preSelPoolEntity2 = labEventFactory.buildFromBettaLimsRackToRackDbFree(hybridSelectionJaxbBuilder.getPreSelPoolJaxb2(),
                    mapBarcodeToPreSelSource2Tube, preSelPoolRack);
            labEventHandler.processEvent(preSelPoolEntity2);
            //asserts
            Set<SampleInstance> preSelPoolSampleInstances = preSelPoolRack.getSampleInstances();
            Assert.assertEquals(preSelPoolSampleInstances.size(),
                    pondRegRack.getSampleInstances().size(), "Wrong number of sample instances");
            Set<String> sampleNames = new HashSet<String>();
            for (SampleInstance preSelPoolSampleInstance : preSelPoolSampleInstances) {
                if (!sampleNames.add(preSelPoolSampleInstance.getStartingSample().getSampleKey())) {
                    Assert.fail("Duplicate sample " + preSelPoolSampleInstance.getStartingSample().getSampleKey());
                }
            }
            Set<SampleInstance> sampleInstancesInPreSelPoolWell = preSelPoolRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(sampleInstancesInPreSelPoolWell.size(), 2, "Wrong number of sample instances in position");

            // Hybridization
            validateWorkflow("Hybridization", preSelPoolRack);
            LabEvent hybridizationEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(hybridSelectionJaxbBuilder.getHybridizationJaxb(), preSelPoolRack, null);
            labEventHandler.processEvent(hybridizationEntity);
            StaticPlate hybridizationPlate = (StaticPlate) hybridizationEntity.getTargetLabVessels().iterator().next();

            // BaitSetup
            TwoDBarcodedTube baitTube = buildBaitTube(hybridSelectionJaxbBuilder.getBaitTubeBarcode());
            LabEvent baitSetupEntity = labEventFactory.buildVesselToSectionDbFree(hybridSelectionJaxbBuilder.getBaitSetupJaxb(),
                    baitTube, null, SBSSection.ALL96.getSectionName());
            labEventHandler.processEvent(baitSetupEntity);
            StaticPlate baitSetupPlate = (StaticPlate) baitSetupEntity.getTargetLabVessels().iterator().next();

            // BaitAddition
            validateWorkflow("BaitAddition", hybridizationPlate);
            LabEvent baitAdditionEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(hybridSelectionJaxbBuilder.getBaitAdditionJaxb(), baitSetupPlate, hybridizationPlate);
            labEventHandler.processEvent(baitAdditionEntity);

            // BeadAddition
            validateWorkflow("BeadAddition", hybridizationPlate);
            LabEvent beadAdditionEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(hybridSelectionJaxbBuilder.getBeadAdditionJaxb(), hybridizationPlate);
            labEventHandler.processEvent(beadAdditionEntity);

            // APWash
            validateWorkflow("APWash", hybridizationPlate);
            LabEvent apWashEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(hybridSelectionJaxbBuilder.getApWashJaxb(), hybridizationPlate);
            labEventHandler.processEvent(apWashEntity);

            // GSWash1
            validateWorkflow("GSWash1", hybridizationPlate);
            LabEvent gsWash1Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(hybridSelectionJaxbBuilder.getGsWash1Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash1Entity);

            // GSWash2
            validateWorkflow("GSWash2", hybridizationPlate);
            LabEvent gsWash2Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(hybridSelectionJaxbBuilder.getGsWash2Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash2Entity);

            // GSWash3
            validateWorkflow("GSWash3", hybridizationPlate);
            LabEvent gsWash3Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(hybridSelectionJaxbBuilder.getGsWash3Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash3Entity);

            // GSWash4
            validateWorkflow("GSWash4", hybridizationPlate);
            LabEvent gsWash4Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(hybridSelectionJaxbBuilder.getGsWash4Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash4Entity);

            // GSWash5
            validateWorkflow("GSWash5", hybridizationPlate);
            LabEvent gsWash5Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(hybridSelectionJaxbBuilder.getGsWash5Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash5Entity);

            // GSWash6
            validateWorkflow("GSWash6", hybridizationPlate);
            LabEvent gsWash6Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(hybridSelectionJaxbBuilder.getGsWash6Jaxb(), hybridizationPlate);
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
            StaticPlate catchCleanPlate = (StaticPlate) catchEnrichmentCleanupEntity.getTargetLabVessels().iterator().next();

            // NormalizedCatchRegistration
            validateWorkflow("NormalizedCatchRegistration", catchCleanPlate);
            mapBarcodeToNormCatchTubes = new HashMap<String, TwoDBarcodedTube>();
            LabEvent normCatchEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(hybridSelectionJaxbBuilder.getNormCatchJaxb(),
                    catchCleanPlate, mapBarcodeToNormCatchTubes);
            labEventHandler.processEvent(normCatchEntity);
            normCatchRack = (RackOfTubes) normCatchEntity.getTargetLabVessels().iterator().next();
            return this;
        }
    }

    public static TwoDBarcodedTube buildBaitTube(String tubeBarcode) {
        TwoDBarcodedTube baitTube = new TwoDBarcodedTube(tubeBarcode);
        baitTube.addReagent(new BaitReagent("cancer_2000gene_shift170_undercovered", "Cancer_2K", "1234abc"));
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

        public HybridSelectionJaxbBuilder(BettaLimsMessageFactory bettaLimsMessageFactory, String testPrefix, String pondRegRackBarcode,
                                          List<String> pondRegTubeBarcodes) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.testPrefix = testPrefix;
            this.pondRegRackBarcode = pondRegRackBarcode;
            this.pondRegTubeBarcodes = pondRegTubeBarcodes;
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
                    pondRegTubeBarcodes.subList(0, pondRegTubeBarcodes.size() / 2), preSelPoolRackBarcode, preSelPoolBarcodes);
            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            bettaLIMSMessage.getPlateTransferEvent().add(preSelPoolJaxb);
            messageList.add(bettaLIMSMessage);
            bettaLimsMessageFactory.advanceTime();

            preSelPoolJaxb2 = bettaLimsMessageFactory.buildRackToRack("PreSelectionPool", pondRegRackBarcode,
                    pondRegTubeBarcodes.subList(pondRegTubeBarcodes.size() / 2, pondRegTubeBarcodes.size()),
                    preSelPoolRackBarcode, preSelPoolBarcodes);
            BettaLIMSMessage bettaLIMSMessage1 = new BettaLIMSMessage();
            bettaLIMSMessage1.getPlateTransferEvent().add(preSelPoolJaxb2);
            messageList.add(bettaLIMSMessage1);
            bettaLimsMessageFactory.advanceTime();

            String hybridizationPlateBarcode = "Hybrid" + testPrefix;
            hybridizationJaxb = bettaLimsMessageFactory.buildRackToPlate(
                    "Hybridization", preSelPoolRackBarcode, preSelPoolBarcodes, hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
            bettaLIMSMessage2.getPlateTransferEvent().add(hybridizationJaxb);
            messageList.add(bettaLIMSMessage2);
            bettaLimsMessageFactory.advanceTime();

            baitTubeBarcode = "Bait" + testPrefix;
            String baitSetupBarcode = "BaitSetup" + testPrefix;
            baitSetupJaxb = bettaLimsMessageFactory.buildTubeToPlate("BaitSetup", baitTubeBarcode,
                    baitSetupBarcode, LabEventFactory.PHYS_TYPE_EPPENDORF_96, LabEventFactory.SECTION_ALL_96, "tube");
            BettaLIMSMessage bettaLIMSMessage3 = new BettaLIMSMessage();
            bettaLIMSMessage3.getReceptaclePlateTransferEvent().add(baitSetupJaxb);
            messageList.add(bettaLIMSMessage3);
            bettaLimsMessageFactory.advanceTime();

            baitAdditionJaxb = bettaLimsMessageFactory.buildPlateToPlate("BaitAddition", baitSetupBarcode,
                    hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage4 = new BettaLIMSMessage();
            bettaLIMSMessage4.getPlateTransferEvent().add(baitAdditionJaxb);
            messageList.add(bettaLIMSMessage4);
            bettaLimsMessageFactory.advanceTime();

            beadAdditionJaxb = bettaLimsMessageFactory.buildPlateEvent("BeadAddition", hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage5 = new BettaLIMSMessage();
            bettaLIMSMessage5.getPlateEvent().add(beadAdditionJaxb);
            messageList.add(bettaLIMSMessage5);
            bettaLimsMessageFactory.advanceTime();

            apWashJaxb = bettaLimsMessageFactory.buildPlateEvent("APWash", hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage6 = new BettaLIMSMessage();
            bettaLIMSMessage6.getPlateEvent().add(apWashJaxb);
            messageList.add(bettaLIMSMessage6);
            bettaLimsMessageFactory.advanceTime();

            gsWash1Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash1", hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage7 = new BettaLIMSMessage();
            bettaLIMSMessage7.getPlateEvent().add(gsWash1Jaxb);
            messageList.add(bettaLIMSMessage7);
            bettaLimsMessageFactory.advanceTime();

            gsWash2Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash2", hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage8 = new BettaLIMSMessage();
            bettaLIMSMessage8.getPlateEvent().add(gsWash2Jaxb);
            messageList.add(bettaLIMSMessage8);
            bettaLimsMessageFactory.advanceTime();

            gsWash3Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash3", hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage15 = new BettaLIMSMessage();
            bettaLIMSMessage15.getPlateEvent().add(gsWash3Jaxb);
            messageList.add(bettaLIMSMessage15);
            bettaLimsMessageFactory.advanceTime();

            gsWash4Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash4", hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage12 = new BettaLIMSMessage();
            bettaLIMSMessage12.getPlateEvent().add(gsWash4Jaxb);
            messageList.add(bettaLIMSMessage12);
            bettaLimsMessageFactory.advanceTime();

            gsWash5Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash5", hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage13 = new BettaLIMSMessage();
            bettaLIMSMessage13.getPlateEvent().add(gsWash5Jaxb);
            messageList.add(bettaLIMSMessage13);
            bettaLimsMessageFactory.advanceTime();

            gsWash6Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash6", hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage14 = new BettaLIMSMessage();
            bettaLIMSMessage14.getPlateEvent().add(gsWash6Jaxb);
            messageList.add(bettaLIMSMessage14);
            bettaLimsMessageFactory.advanceTime();

            catchEnrichmentSetupJaxb = bettaLimsMessageFactory.buildPlateEvent("CatchEnrichmentSetup", hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage9 = new BettaLIMSMessage();
            bettaLIMSMessage9.getPlateEvent().add(catchEnrichmentSetupJaxb);
            messageList.add(bettaLIMSMessage9);
            bettaLimsMessageFactory.advanceTime();

            String catchCleanupBarcode = "catchCleanPlate" + testPrefix;
            catchEnrichmentCleanupJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "CatchEnrichmentCleanup", hybridizationPlateBarcode, catchCleanupBarcode);
            BettaLIMSMessage bettaLIMSMessage10 = new BettaLIMSMessage();
            bettaLIMSMessage10.getPlateTransferEvent().add(catchEnrichmentCleanupJaxb);
            messageList.add(bettaLIMSMessage10);
            bettaLimsMessageFactory.advanceTime();

            normCatchBarcodes = new ArrayList<String>();
            for (int rackPosition = 1; rackPosition <= pondRegTubeBarcodes.size() / 2; rackPosition++) {
                normCatchBarcodes.add("NormCatch" + testPrefix + rackPosition);
            }
            normCatchRackBarcode = "NormCatchRack";
            normCatchJaxb = bettaLimsMessageFactory.buildPlateToRack(
                    "NormalizedCatchRegistration", hybridizationPlateBarcode, normCatchRackBarcode, normCatchBarcodes);
            BettaLIMSMessage bettaLIMSMessage11 = new BettaLIMSMessage();
            bettaLIMSMessage11.getPlateTransferEvent().add(normCatchJaxb);
            messageList.add(bettaLIMSMessage11);
            bettaLimsMessageFactory.advanceTime();

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
        private final RackOfTubes normCatchRack;
        private final String normCatchRackBarcode;
        private final List<String> normCatchBarcodes;
        private final Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes;

        private RackOfTubes denatureRack;
        private IlluminaFlowcell illuminaFlowcell;

        public QtpEntityBuilder(BettaLimsMessageFactory bettaLimsMessageFactory,
                                LabEventFactory labEventFactory, LabEventHandler labEventHandler, RackOfTubes normCatchRack,
                                String normCatchRackBarcode, List<String> normCatchBarcodes, Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
            this.normCatchRack = normCatchRack;
            this.normCatchRackBarcode = normCatchRackBarcode;
            this.normCatchBarcodes = normCatchBarcodes;
            this.mapBarcodeToNormCatchTubes = mapBarcodeToNormCatchTubes;
        }

        public void invoke() {
            QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, "", normCatchBarcodes, normCatchRackBarcode).invoke();
            PlateCherryPickEvent cherryPickJaxb = qtpJaxbBuilder.getPoolingTransferJaxb();
            final String poolRackBarcode = qtpJaxbBuilder.getPoolRackBarcode();
            PlateCherryPickEvent denatureJaxb = qtpJaxbBuilder.getDenatureJaxb();
            final String denatureRackBarcode = qtpJaxbBuilder.getDenatureRackBarcode();
            PlateCherryPickEvent stripTubeTransferJaxb = qtpJaxbBuilder.getStripTubeTransferJaxb();
            final String stripTubeHolderBarcode = qtpJaxbBuilder.getStripTubeHolderBarcode();
            PlateTransferEventType flowcellTransferJaxb = qtpJaxbBuilder.getFlowcellTransferJaxb();

            // PoolingTransfer
            validateWorkflow("PoolingTransfer", normCatchRack);
            Map<String, TwoDBarcodedTube> mapBarcodeToPoolTube = new HashMap<String, TwoDBarcodedTube>();
            LabEvent poolingEntity = labEventFactory.buildCherryPickRackToRackDbFree(cherryPickJaxb,
                    new HashMap<String, RackOfTubes>() {{
                        put(normCatchRackBarcode, normCatchRack);
                    }},
                    mapBarcodeToNormCatchTubes,
                    new HashMap<String, RackOfTubes>() {{
                        put(poolRackBarcode, null);
                    }}, mapBarcodeToPoolTube
            );
            labEventHandler.processEvent(poolingEntity);
            // asserts
            final RackOfTubes poolingRack = (RackOfTubes) poolingEntity.getTargetLabVessels().iterator().next();
            Set<SampleInstance> pooledSampleInstances = poolingRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(pooledSampleInstances.size(), normCatchRack.getSampleInstances().size(), "Wrong number of pooled samples");

            // DenatureTransfer
            validateWorkflow("DenatureTransfer", poolingRack);
            Map<String, TwoDBarcodedTube> mapBarcodeToDenatureTube = new HashMap<String, TwoDBarcodedTube>();
            LabEvent denatureEntity = labEventFactory.buildCherryPickRackToRackDbFree(denatureJaxb,
                    new HashMap<String, RackOfTubes>() {{
                        put(poolRackBarcode, poolingRack);
                    }},
                    mapBarcodeToPoolTube,
                    new HashMap<String, RackOfTubes>() {{
                        put(denatureRackBarcode, null);
                    }}, mapBarcodeToDenatureTube
            );
            labEventHandler.processEvent(denatureEntity);
            // asserts
            denatureRack = (RackOfTubes) denatureEntity.getTargetLabVessels().iterator().next();
            Set<SampleInstance> denaturedSampleInstances = denatureRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(denaturedSampleInstances.size(), normCatchRack.getSampleInstances().size(), "Wrong number of denatured samples");

            // StripTubeBTransfer
            validateWorkflow("StripTubeBTransfer", denatureRack);
            Map<String, StripTube> mapBarcodeToStripTube = new HashMap<String, StripTube>();
            LabEvent stripTubeTransferEntity = labEventFactory.buildCherryPickRackToStripTubeDbFree(stripTubeTransferJaxb,
                    new HashMap<String, RackOfTubes>() {{
                        put(denatureRackBarcode, denatureRack);
                    }},
                    mapBarcodeToDenatureTube,
                    new HashMap<String, RackOfTubes>() {{
                        put(stripTubeHolderBarcode, null);
                    }},
                    mapBarcodeToStripTube
            );
            labEventHandler.processEvent(stripTubeTransferEntity);
            // asserts
            StripTube stripTube = (StripTube) stripTubeTransferEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(stripTube.getContainerRole().getSampleInstancesAtPosition(VesselPosition.TUBE1).size(),
                    normCatchRack.getSampleInstances().size(), "Wrong number of samples in strip tube well");

            // FlowcellTransfer
            validateWorkflow("FlowcellTransfer", stripTube);
            LabEvent flowcellTransferEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(flowcellTransferJaxb, stripTube, null);
            labEventHandler.processEvent(flowcellTransferEntity);
            //asserts
            illuminaFlowcell = (IlluminaFlowcell) flowcellTransferEntity.getTargetLabVessels().iterator().next();
            Set<SampleInstance> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(VesselPosition.LANE1);
            Assert.assertEquals(lane1SampleInstances.size(), normCatchRack.getSampleInstances().size(),
                    "Wrong number of samples in flowcell lane");
            Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(), 1, "Wrong number of reagents");
        }

        public RackOfTubes getDenatureRack() {
            return denatureRack;
        }

        public IlluminaFlowcell getIlluminaFlowcell() {
            return illuminaFlowcell;
        }
    }

    /**
     * Builds JAXB objects for QTP messages
     */
    public static class QtpJaxbBuilder {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final String testPrefix;
        private final List<String> normCatchBarcodes;
        private final String normCatchRackBarcode;

        private String poolRackBarcode;
        private String poolTubeBarcode;
        private PlateCherryPickEvent poolingTransferJaxb;
        private String denatureRackBarcode;
        private PlateCherryPickEvent denatureJaxb;
        private String stripTubeHolderBarcode;
        private PlateCherryPickEvent stripTubeTransferJaxb;
        private PlateTransferEventType flowcellTransferJaxb;
        private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
        private String flowcellBarcode;

        public QtpJaxbBuilder(BettaLimsMessageFactory bettaLimsMessageFactory, String testPrefix, List<String> normCatchBarcodes,
                              String normCatchRackBarcode) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.testPrefix = testPrefix;
            this.normCatchBarcodes = normCatchBarcodes;
            this.normCatchRackBarcode = normCatchRackBarcode;
        }

        public String getPoolRackBarcode() {
            return poolRackBarcode;
        }

        public String getPoolTubeBarcode() {
            return poolTubeBarcode;
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

        public String getFlowcellBarcode() {
            return flowcellBarcode;
        }

        public List<BettaLIMSMessage> getMessageList() {
            return messageList;
        }

        public QtpJaxbBuilder invoke() {
            // PoolingTransfer
            poolRackBarcode = "PoolRack" + testPrefix;
            List<BettaLimsMessageFactory.CherryPick> poolingCherryPicks = new ArrayList<BettaLimsMessageFactory.CherryPick>();
            List<String> poolTubeBarcodes = new ArrayList<String>();
            for (int rackPosition = 1; rackPosition <= normCatchBarcodes.size(); rackPosition++) {
                poolingCherryPicks.add(new BettaLimsMessageFactory.CherryPick(normCatchRackBarcode,
                        bettaLimsMessageFactory.buildWellName(rackPosition), poolRackBarcode, "A01"));
                poolTubeBarcodes.add("Pool" + testPrefix + rackPosition);
            }
            poolTubeBarcode = "Pool" + testPrefix + "1";
            poolingTransferJaxb = bettaLimsMessageFactory.buildCherryPick("PoolingTransfer",
                    Arrays.asList(normCatchRackBarcode), Arrays.asList(normCatchBarcodes), poolRackBarcode, poolTubeBarcodes,
                    poolingCherryPicks);
            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            bettaLIMSMessage.getPlateCherryPickEvent().add(poolingTransferJaxb);
            messageList.add(bettaLIMSMessage);
            bettaLimsMessageFactory.advanceTime();

            // DenatureTransfer
            denatureRackBarcode = "DenatureRack" + testPrefix;
            List<BettaLimsMessageFactory.CherryPick> denatureCherryPicks = new ArrayList<BettaLimsMessageFactory.CherryPick>();
            List<String> denatureTubeBarcodes = new ArrayList<String>();
            denatureCherryPicks.add(new BettaLimsMessageFactory.CherryPick(poolRackBarcode,
                    "A01", denatureRackBarcode, "A01"));
            denatureTubeBarcodes.add("DenatureTube" + testPrefix + "1");
            denatureJaxb = bettaLimsMessageFactory.buildCherryPick("DenatureTransfer",
                    Arrays.asList(poolRackBarcode), Arrays.asList(poolTubeBarcodes), denatureRackBarcode, denatureTubeBarcodes,
                    denatureCherryPicks);
            BettaLIMSMessage bettaLIMSMessage1 = new BettaLIMSMessage();
            bettaLIMSMessage1.getPlateCherryPickEvent().add(denatureJaxb);
            messageList.add(bettaLIMSMessage1);
            bettaLimsMessageFactory.advanceTime();

            // StripTubeBTransfer
            stripTubeHolderBarcode = "StripTubeHolder" + testPrefix;
            List<BettaLimsMessageFactory.CherryPick> stripTubeCherryPicks = new ArrayList<BettaLimsMessageFactory.CherryPick>();
            for (int rackPosition = 0; rackPosition < 8; rackPosition++) {
                stripTubeCherryPicks.add(new BettaLimsMessageFactory.CherryPick(denatureRackBarcode,
                        "A01", stripTubeHolderBarcode,
                        Character.toString((char) ('A' + rackPosition)) + "01"));
            }
            String stripTubeBarcode = "StripTube" + testPrefix + "1";
            stripTubeTransferJaxb = bettaLimsMessageFactory.buildCherryPickToStripTube("StripTubeBTransfer",
                    Arrays.asList(denatureRackBarcode), Arrays.asList(denatureTubeBarcodes),
                    stripTubeHolderBarcode, Arrays.asList(stripTubeBarcode), stripTubeCherryPicks);
            BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
            bettaLIMSMessage2.getPlateCherryPickEvent().add(stripTubeTransferJaxb);
            messageList.add(bettaLIMSMessage2);
            bettaLimsMessageFactory.advanceTime();

            // FlowcellTransfer
            flowcellBarcode = "Flowcell" + testPrefix;
            flowcellTransferJaxb = bettaLimsMessageFactory.buildStripTubeToFlowcell("FlowcellTransfer",
                    stripTubeBarcode, flowcellBarcode);
            BettaLIMSMessage bettaLIMSMessage3 = new BettaLIMSMessage();
            bettaLIMSMessage3.getPlateTransferEvent().add(flowcellTransferJaxb);
            messageList.add(bettaLIMSMessage3);
            bettaLimsMessageFactory.advanceTime();

            return this;
        }
    }
}
