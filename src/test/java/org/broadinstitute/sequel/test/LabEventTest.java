package org.broadinstitute.sequel.test;

//import com.jprofiler.api.agent.Controller;

import org.broadinstitute.sequel.bettalims.jaxb.BettaLIMSMessage;
import org.broadinstitute.sequel.bettalims.jaxb.PlateCherryPickEvent;
import org.broadinstitute.sequel.bettalims.jaxb.PlateEventType;
import org.broadinstitute.sequel.bettalims.jaxb.PlateTransferEventType;
import org.broadinstitute.sequel.bettalims.jaxb.PositionMapType;
import org.broadinstitute.sequel.bettalims.jaxb.ReceptaclePlateTransferEvent;
import org.broadinstitute.sequel.bettalims.jaxb.ReceptacleType;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.control.dao.workflow.WorkQueueDAO;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.entity.OrmUtil;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.entity.labevent.GenericLabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.BasicProject;
import org.broadinstitute.sequel.entity.project.BasicProjectPlan;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.queue.LabWorkQueue;
import org.broadinstitute.sequel.entity.reagent.GenericReagent;
import org.broadinstitute.sequel.entity.reagent.MolecularIndex;
import org.broadinstitute.sequel.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.sequel.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.sequel.entity.run.IlluminaFlowcell;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.PlateWell;
import org.broadinstitute.sequel.entity.vessel.RackOfTubes;
import org.broadinstitute.sequel.entity.vessel.SBSSection;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.StripTube;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.entity.vessel.VesselContainer;
import org.broadinstitute.sequel.entity.vessel.VesselPosition;
import org.broadinstitute.sequel.infrastructure.jira.JiraServiceStub;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

/**
 * Test messaging
 */
@SuppressWarnings({"FeatureEnvy", "OverlyCoupledClass", "OverlyCoupledMethod", "OverlyLongMethod"})
public class LabEventTest {
    public static final int NUM_POSITIONS_IN_RACK = 96;

    public static final String POND_REGISTRATION_TUBE_PREFIX = "PondReg";

    /**
     * Used in test verification, accumulates the events in a chain of transfers
     */
    public static class ListTransfersFromStart implements VesselContainer.TransferTraverserCriteria {
        private int hopCount = -1;
        private final List<String> labEventNames = new ArrayList<String>();

        @Override
        public TraversalControl evaluateVessel(LabVessel labVessel, LabEvent labEvent, int hopCount) {
            if (labEvent != null) {
                if(hopCount > this.hopCount) {
                    this.hopCount = hopCount;
                    GenericLabEvent genericLabEvent = OrmUtil.proxySafeCast(labEvent, GenericLabEvent.class);
                    labEventNames.add(genericLabEvent.getLabEventType().getName() + " into " +
                            genericLabEvent.getTargetLabVessels().iterator().next().getLabel());
                }
            }
            return TraversalControl.ContinueTraversing;
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

        // Project and workflow
        Project project = new BasicProject("LabEventTesting", new JiraTicket(new JiraServiceStub(),"TP-0","0"));
        WorkflowDescription workflowDescription = new WorkflowDescription("HS", null,
                CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);
        workflowDescription.initFromFile("HybridSelectionV2.bpmn");
        BasicProjectPlan projectPlan = new BasicProjectPlan(project,"To test hybrid selection", workflowDescription);

        // starting rack
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for(int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK; rackPosition++) {
            String barcode = "R" + rackPosition;
            String bspStock = "SM-" + rackPosition;
            BSPSampleAuthorityTwoDTube bspAliquot = new BSPSampleAuthorityTwoDTube(new BSPStartingSample(bspStock + ".aliquot", projectPlan, null));
            mapBarcodeToTube.put(barcode,bspAliquot);

        }

        // Messaging
        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setPersonDAO(new PersonDAO());
        LabEventHandler labEventHandler = new LabEventHandler(createMockWorkQueueDAO());

        PreFlightEntityBuilder preFlightEntityBuilder = new PreFlightEntityBuilder(workflowDescription,
                bettaLimsMessageFactory, labEventFactory, labEventHandler, mapBarcodeToTube);//.invoke();

        ShearingEntityBuilder shearingEntityBuilder = new ShearingEntityBuilder(workflowDescription, mapBarcodeToTube,
                bettaLimsMessageFactory, labEventFactory, labEventHandler, preFlightEntityBuilder.getRackBarcode()).invoke();

        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = new LibraryConstructionEntityBuilder(
                workflowDescription, bettaLimsMessageFactory, labEventFactory, labEventHandler,
                shearingEntityBuilder.getShearingCleanupPlate(), shearingEntityBuilder.getShearCleanPlateBarcode(),
                shearingEntityBuilder.getShearingPlate(), NUM_POSITIONS_IN_RACK).invoke();

        HybridSelectionEntityBuilder hybridSelectionEntityBuilder = new HybridSelectionEntityBuilder(
                workflowDescription, bettaLimsMessageFactory, labEventFactory, labEventHandler,
                libraryConstructionEntityBuilder.getPondRegRack(), libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                libraryConstructionEntityBuilder.getPondRegTubeBarcodes()).invoke();

        new QtpEntityBuilder(workflowDescription, bettaLimsMessageFactory, labEventFactory, labEventHandler,
                hybridSelectionEntityBuilder.getNormCatchRack(), hybridSelectionEntityBuilder.getNormCatchRackBarcode(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes()).invoke();

        Map.Entry<String, TwoDBarcodedTube> stringTwoDBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        VesselContainer<TwoDBarcodedTube> startingContainer = (VesselContainer<TwoDBarcodedTube>)
                stringTwoDBarcodedTubeEntry.getValue().getContainers().iterator().next();
        startingContainer.evaluateCriteria(
                startingContainer.getPositionOfVessel(stringTwoDBarcodedTubeEntry.getValue()),
                transferTraverserCriteria, VesselContainer.TraversalDirection.Descendants, null, 0);
        List<String> labEventNames = transferTraverserCriteria.getLabEventNames();
        Assert.assertEquals(labEventNames.size(), 13, "Wrong number of transfers");

//        Controller.stopCPURecording();
    }

    /**
     * Build object graph for Whole Genome Shotgun messages, verify chain of events.
     */
    @Test(groups = {DATABASE_FREE})
    public void testWholeGenomeShotgun() {
//        Controller.startCPURecording(true);

        Map<LabEventName,PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();
        Project project = new BasicProject("LabEventTesting", new JiraTicket(new JiraServiceStub(),"TP-0","0"));
        WorkflowDescription workflowDescription = new WorkflowDescription("WGS", null, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);
        workflowDescription.initFromFile("WholeGenomeShotgun.bpmn");
        BasicProjectPlan projectPlan = new BasicProjectPlan(project, "To test whole genome shotgun", workflowDescription);

        // starting rack
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for(int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK; rackPosition++) {
            String barcode = "R" + rackPosition;

            String bspStock = "SM-" + rackPosition;
            BSPSampleAuthorityTwoDTube bspAliquot = new BSPSampleAuthorityTwoDTube(new BSPStartingSample(bspStock + ".aliquot", projectPlan, null));
            mapBarcodeToTube.put(barcode,bspAliquot);


        }

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setPersonDAO(new PersonDAO());
        LabEventHandler labEventHandler = new LabEventHandler(createMockWorkQueueDAO());

        // todo jmt fix preflight
        PreFlightEntityBuilder preFlightEntityBuilder = new PreFlightEntityBuilder(workflowDescription,
                bettaLimsMessageFactory, labEventFactory, labEventHandler, mapBarcodeToTube);//.invoke();

        ShearingEntityBuilder shearingEntityBuilder = new ShearingEntityBuilder(workflowDescription, mapBarcodeToTube,
                bettaLimsMessageFactory, labEventFactory, labEventHandler, preFlightEntityBuilder.getRackBarcode()).invoke();

        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = new LibraryConstructionEntityBuilder(
                workflowDescription, bettaLimsMessageFactory, labEventFactory, labEventHandler,
                shearingEntityBuilder.getShearingCleanupPlate(), shearingEntityBuilder.getShearCleanPlateBarcode(),
                shearingEntityBuilder.getShearingPlate(), NUM_POSITIONS_IN_RACK).invoke();

        List<String> sageUnloadTubeBarcodes = new ArrayList<String>();
        for(int i = 1; i <= NUM_POSITIONS_IN_RACK; i++) {
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
            labEventHandler.processEvent(sageLoadingEntity, null);
            StaticPlate sageCassette = (StaticPlate) sageLoadingEntity.getTargetLabVessels().iterator().next();

            // SageLoaded

            // SageUnloading
            PlateTransferEventType sageUnloadingJaxb = bettaLimsMessageFactory.buildPlateToRack("SageUnloading",
                    sageCassetteBarcode, sageUnloadBarcode, sageUnloadTubeBarcodes.subList(i * 4, i * 4  + 4));
            LabEvent sageUnloadEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(sageUnloadingJaxb,
                    sageCassette, mapBarcodeToSageUnloadTubes);
            labEventHandler.processEvent(sageUnloadEntity, null);
            sageUnloadEntity.getTargetLabVessels().iterator().next();
        }

        // SageCleanup
        List<String> sageCleanupTubeBarcodes = new ArrayList<String>();
        for(int i = 1; i <= NUM_POSITIONS_IN_RACK; i++) {
            sageCleanupTubeBarcodes.add("SageCleanup" + i);
        }
        String sageCleanupBarcode = "SageCleanup";
        PlateTransferEventType sageCleanupJaxb = bettaLimsMessageFactory.buildRackToRack("SageCleanup", sageUnloadBarcode,
                sageUnloadTubeBarcodes, sageCleanupBarcode, sageCleanupTubeBarcodes);
        RackOfTubes sageUnloadRackRearrayed = new RackOfTubes("sageUnloadRearray");
        List<TwoDBarcodedTube> sageUnloadTubes = new ArrayList<TwoDBarcodedTube>(mapBarcodeToSageUnloadTubes.values());
        for(int i = 0; i < NUM_POSITIONS_IN_RACK; i++) {
            sageUnloadRackRearrayed.getVesselContainer().addContainedVessel(sageUnloadTubes.get(i),
                    VesselPosition.getByName(bettaLimsMessageFactory.buildWellName(i + 1)));
        }
        sageUnloadRackRearrayed.makeDigest();
        LabEvent sageCleanupEntity = labEventFactory.buildFromBettaLimsRackToRackDbFree(sageCleanupJaxb,
                sageUnloadRackRearrayed, new HashMap<String, TwoDBarcodedTube>());
        labEventHandler.processEvent(sageCleanupEntity, null);
        RackOfTubes sageCleanupRack = (RackOfTubes) sageCleanupEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(sageCleanupRack.getSampleInstances().size(), NUM_POSITIONS_IN_RACK, "Wrong number of sage cleanup samples");

        new QtpEntityBuilder(workflowDescription, bettaLimsMessageFactory, labEventFactory, labEventHandler, sageCleanupRack,
                sageCleanupBarcode, sageCleanupTubeBarcodes, mapBarcodeToSageUnloadTubes).invoke();

        Map.Entry<String, TwoDBarcodedTube> stringTwoDBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        ListTransfersFromStart transferTraverserCriteria = new ListTransfersFromStart();
        VesselContainer<TwoDBarcodedTube> startingContainer = (VesselContainer<TwoDBarcodedTube>)
                stringTwoDBarcodedTubeEntry.getValue().getContainers().iterator().next();
        startingContainer.evaluateCriteria(
                startingContainer.getPositionOfVessel(stringTwoDBarcodedTubeEntry.getValue()),
                transferTraverserCriteria, VesselContainer.TraversalDirection.Descendants, null, 0);
        List<String> labEventNames = transferTraverserCriteria.getLabEventNames();
        Assert.assertEquals(labEventNames.size(), 12, "Wrong number of transfers");

//        Controller.stopCPURecording();
    }

    /**
     * Build object graph for Fluidigm messages
     */
    @Test(groups = {DATABASE_FREE})
    public void testFluidigm() {
        Project project = new BasicProject("LabEventTesting", new JiraTicket(new JiraServiceStub(),"TP-0","0"));
        WorkflowDescription workflowDescription = new WorkflowDescription("WGS", null, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);
        BasicProjectPlan projectPlan = new BasicProjectPlan(project, "To test whole genome shotgun", workflowDescription);

        // starting rack
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for(int rackPosition = 1; rackPosition <= SBSSection.P96COLS1_6BYROW.getWells().size(); rackPosition++) {
            String barcode = "R" + rackPosition;

            String bspStock = "SM-" + rackPosition;
            BSPSampleAuthorityTwoDTube bspAliquot = new BSPSampleAuthorityTwoDTube(new BSPStartingSample(bspStock + ".aliquot", projectPlan, null));
            mapBarcodeToTube.put(barcode,bspAliquot);
        }

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setPersonDAO(new PersonDAO());
        LabEventHandler labEventHandler = new LabEventHandler(createMockWorkQueueDAO());
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

            // FluidigmHarvestingToRack chip P384COLS4-6BYROW to rack P96COLS1-6BYROW
            harvestRackBarcode = "Harvest" + testPrefix;
            List<String> harvestTubeBarcodes = new ArrayList<String>();
            for(int rackPosition = 1; rackPosition <= mapBarcodeToTube.size(); rackPosition++) {
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
        }

        private PositionMapType buildFluidigmPositionMap(ArrayList<String> tubeBarcodes, String rackBarcode) {
            PositionMapType sourcePositionMap = new PositionMapType();
            sourcePositionMap.setBarcode(rackBarcode);
            int barcodeIndex = 0;
            for(int row = 0; row < 8; row++) {
                for(int column = 1; column <= 6; column++) {
                    ReceptacleType receptacleType = new ReceptacleType();
                    receptacleType.setBarcode(tubeBarcodes.get(barcodeIndex));
                    receptacleType.setPosition(bettaLimsMessageFactory.buildWellName(row * 12 + column));
                    sourcePositionMap.getReceptacle().add(receptacleType);
                    barcodeIndex ++;
                }
            }
            return sourcePositionMap;
        }

        private void buildObjectGraph() {
            LabEvent fluidigmSampleInputEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(fluidigmSampleInputJaxb,
                    mapBarcodeToTube, null);
            labEventHandler.processEvent(fluidigmSampleInputEntity, null);
            // asserts
            StaticPlate chip = (StaticPlate) fluidigmSampleInputEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(chip.getSampleInstances().size(), mapBarcodeToTube.size(), "Wrong number of sample instances");

            LabEvent fluidigmIndexedAdapterInputEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    fluidigmIndexedAdapterInputJaxb, indexPlate, chip);
            labEventHandler.processEvent(fluidigmIndexedAdapterInputEntity, null);

            LabEvent fluidigmHarvestingToRackEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(
                    fluidigmHarvestingToRackJaxb, chip, mapBarcodeToHarvestTube);
            labEventHandler.processEvent(fluidigmHarvestingToRackEntity, null);
            // asserts
            RackOfTubes harvestRack = (RackOfTubes) fluidigmHarvestingToRackEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(harvestRack.getSampleInstances().size(), mapBarcodeToTube.size(), "Wrong number of sample instances");
        }
    }

    private static void validateWorkflow(WorkflowDescription workflowDescription, String nextEventTypeName, Collection<? extends LabVessel> tubes) {
        List<LabVessel> labVessels = new ArrayList<LabVessel>(tubes);
        validateWorkflow(workflowDescription, nextEventTypeName, labVessels);
    }

    private static void validateWorkflow(WorkflowDescription workflowDescription, String nextEventTypeName, LabVessel labVessel) {
        List<LabVessel> labVessels = new ArrayList<LabVessel>();
        labVessels.add(labVessel);
        validateWorkflow(workflowDescription, nextEventTypeName, labVessels);
    }

    private static void validateWorkflow(WorkflowDescription workflowDescription, String nextEventTypeName, List<LabVessel> labVessels) {
        List<String> errors = workflowDescription.validate(labVessels, nextEventTypeName);
        if(!errors.isEmpty()) {
            Assert.fail(errors.get(0));
        }
    }

    /**
     * Builds entity graph for Pre-flight events
     */
    public static class PreFlightEntityBuilder {
        private final WorkflowDescription workflowDescription;
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;

        private final Map<String, TwoDBarcodedTube> mapBarcodeToTube;
        private RackOfTubes rackOfTubes;
        private String rackBarcode;

        public PreFlightEntityBuilder(WorkflowDescription workflowDescription, BettaLimsMessageFactory bettaLimsMessageFactory,
                LabEventFactory labEventFactory, LabEventHandler labEventHandler, Map<String, TwoDBarcodedTube> mapBarcodeToTube) {
            this.workflowDescription = workflowDescription;
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
            this.mapBarcodeToTube = mapBarcodeToTube;
        }

        public String getRackBarcode() {
            return rackBarcode;
        }

        public PreFlightEntityBuilder invoke() {
            PreFlightJaxbBuilder preFlightJaxbBuilder = new PreFlightJaxbBuilder(bettaLimsMessageFactory, "",
                    new ArrayList<String>(mapBarcodeToTube.keySet()));
            preFlightJaxbBuilder.invoke();
            rackBarcode = preFlightJaxbBuilder.getRackBarcode();

            // PreflightPicoSetup 1
            LabEvent preflightPicoSetup1Entity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    preFlightJaxbBuilder.getPreflightPicoSetup1(), mapBarcodeToTube, null);
            labEventHandler.processEvent(preflightPicoSetup1Entity, workflowDescription);
            // asserts
            StaticPlate preflightPicoSetup1Plate = (StaticPlate) preflightPicoSetup1Entity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(preflightPicoSetup1Plate.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

            // PreflightPicoSetup 2
            LabEvent preflightPicoSetup2Entity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    preFlightJaxbBuilder.getPreflightPicoSetup2(), mapBarcodeToTube, null);
            labEventHandler.processEvent(preflightPicoSetup2Entity, workflowDescription);
            // asserts
            StaticPlate preflightPicoSetup2Plate = (StaticPlate) preflightPicoSetup2Entity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(preflightPicoSetup2Plate.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

            // PreflightNormalization
            LabEvent preflightNormalization = labEventFactory.buildFromBettaLimsRackEventDbFree(
                    preFlightJaxbBuilder.getPreflightNormalization(), null, mapBarcodeToTube);
            labEventHandler.processEvent(preflightNormalization, workflowDescription);
            // asserts
            rackOfTubes = (RackOfTubes) preflightNormalization.getTargetLabVessels().iterator().next();
            Assert.assertEquals(rackOfTubes.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

            // PreflightPostNormPicoSetup 1
            LabEvent preflightPostNormPicoSetup1Entity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    preFlightJaxbBuilder.getPreflightPostNormPicoSetup1(), mapBarcodeToTube, null);
            labEventHandler.processEvent(preflightPostNormPicoSetup1Entity, workflowDescription);
            // asserts
            StaticPlate preflightPostNormPicoSetup1Plate = (StaticPlate) preflightPostNormPicoSetup1Entity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(preflightPostNormPicoSetup1Plate.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

            // PreflightPicoSetup 2
            LabEvent preflightPostNormPicoSetup2Entity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    preFlightJaxbBuilder.getPreflightPostNormPicoSetup2(), mapBarcodeToTube, null);
            labEventHandler.processEvent(preflightPostNormPicoSetup2Entity, workflowDescription);
            // asserts
            StaticPlate preflightPostNormPicoSetup2Plate = (StaticPlate) preflightPostNormPicoSetup2Entity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(preflightPostNormPicoSetup2Plate.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

            return this;
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

            preflightPicoSetup2 = this.bettaLimsMessageFactory.buildRackToPlate("PreflightPicoSetup", rackBarcode,
                    tubeBarcodes, "PreflightPicoPlate2" + testPrefix);
            BettaLIMSMessage bettaLIMSMessage1 = new BettaLIMSMessage();
            bettaLIMSMessage1.getPlateTransferEvent().add(preflightPicoSetup2);
            messageList.add(bettaLIMSMessage1);

            preflightNormalization = this.bettaLimsMessageFactory.buildRackEvent("PreflightNormalization", rackBarcode, tubeBarcodes);
            BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
            bettaLIMSMessage2.getPlateEvent().add(preflightNormalization);
            messageList.add(bettaLIMSMessage2);

            preflightPostNormPicoSetup1 = this.bettaLimsMessageFactory.buildRackToPlate("PreflightPostNormPicoSetup",
                    rackBarcode, tubeBarcodes, "PreflightPostNormPicoPlate1" + testPrefix);
            BettaLIMSMessage bettaLIMSMessage3 = new BettaLIMSMessage();
            bettaLIMSMessage3.getPlateEvent().add(preflightPostNormPicoSetup1);
            messageList.add(bettaLIMSMessage3);

            preflightPostNormPicoSetup2 = this.bettaLimsMessageFactory.buildRackToPlate("PreflightPostNormPicoSetup",
                    rackBarcode, tubeBarcodes, "PreflightPostNormPicoPlate2" + testPrefix);
            BettaLIMSMessage bettaLIMSMessage4 = new BettaLIMSMessage();
            bettaLIMSMessage4.getPlateEvent().add(preflightPostNormPicoSetup2);
            messageList.add(bettaLIMSMessage4);

            return this;
        }
    }

    /**
     * Builds entity graph for Shearing events
     */
    public static class ShearingEntityBuilder {
        private final WorkflowDescription workflowDescription;
        private final Map<String, TwoDBarcodedTube> mapBarcodeToTube;
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;
        private final String rackBarcode;
        private String shearPlateBarcode;
        private StaticPlate shearingPlate;
        private String shearCleanPlateBarcode;
        private StaticPlate shearingCleanupPlate;

        public ShearingEntityBuilder(WorkflowDescription workflowDescription, Map<String, TwoDBarcodedTube> mapBarcodeToTube,
                BettaLimsMessageFactory bettaLimsMessageFactory, LabEventFactory labEventFactory,
                LabEventHandler labEventHandler, String rackBarcode) {
            this.workflowDescription = workflowDescription;
            this.mapBarcodeToTube = mapBarcodeToTube;
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
            validateWorkflow(workflowDescription, "ShearingTransfer", mapBarcodeToTube.values());
            LabEvent shearingTransferEventEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    shearingJaxbBuilder.getShearingTransferEventJaxb(), mapBarcodeToTube, null);
            labEventHandler.processEvent(shearingTransferEventEntity, null);
            // asserts
            shearingPlate = (StaticPlate) shearingTransferEventEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(shearingPlate.getSampleInstances().size(),
                    mapBarcodeToTube.size(), "Wrong number of sample instances");

            // PostShearingTransferCleanup
            validateWorkflow(workflowDescription, "PostShearingTransferCleanup", shearingPlate);
            LabEvent postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    shearingJaxbBuilder.getPostShearingTransferCleanupEventJaxb(), shearingPlate, null);
            labEventHandler.processEvent(postShearingTransferCleanupEntity, null);
            // asserts
            shearingCleanupPlate = (StaticPlate) postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(shearingCleanupPlate.getSampleInstances().size(),
                    mapBarcodeToTube.size(), "Wrong number of sample instances");
            Set<SampleInstance> sampleInstancesInWell = shearingCleanupPlate.getVesselContainer().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(sampleInstancesInWell.size(), 1, "Wrong number of sample instances in well");
            Assert.assertEquals(sampleInstancesInWell.iterator().next().getStartingSample().getSampleName(),
                    mapBarcodeToTube.values().iterator().next().getSampleInstances().iterator().next().getStartingSample().getSampleName(), "Wrong sample");

            // ShearingQC
            validateWorkflow(workflowDescription, "ShearingQC", shearingCleanupPlate);
            LabEvent shearingQcEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    shearingJaxbBuilder.getShearingQcEventJaxb(), shearingCleanupPlate, null);
            labEventHandler.processEvent(shearingQcEntity, null);
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

            shearCleanPlateBarcode = "ShearCleanPlate" + testPrefix;
            postShearingTransferCleanupEventJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PostShearingTransferCleanup", shearPlateBarcode, shearCleanPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage1 = new BettaLIMSMessage();
            bettaLIMSMessage1.getPlateTransferEvent().add(postShearingTransferCleanupEventJaxb);
            messageList.add(bettaLIMSMessage1);

            String shearQcPlateBarcode = "ShearQcPlate" + testPrefix;
            shearingQcEventJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "ShearingQC", shearCleanPlateBarcode, shearQcPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
            bettaLIMSMessage2.getPlateTransferEvent().add(shearingQcEventJaxb);
            messageList.add(bettaLIMSMessage2);

            return this;
        }
    }

    /**
     * Builds entity graph for Library Construction events
     */
    public static class LibraryConstructionEntityBuilder {
        private final WorkflowDescription workflowDescription;
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

        public LibraryConstructionEntityBuilder(WorkflowDescription workflowDescription, BettaLimsMessageFactory bettaLimsMessageFactory,
                LabEventFactory labEventFactory, LabEventHandler labEventHandler, StaticPlate shearingCleanupPlate,
                String shearCleanPlateBarcode, StaticPlate shearingPlate, int numSamples) {
            this.workflowDescription = workflowDescription;
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
            validateWorkflow(workflowDescription, "EndRepair", shearingCleanupPlate);
            LabEvent endRepairEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(libraryConstructionJaxbBuilder.getEndRepairJaxb(), shearingCleanupPlate);
            labEventHandler.processEvent(endRepairEntity, null);

            // EndRepairCleanup
            validateWorkflow(workflowDescription, "EndRepairCleanup", shearingCleanupPlate);
            LabEvent endRepairCleanupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    libraryConstructionJaxbBuilder.getEndRepairCleanupJaxb(), shearingCleanupPlate);
            labEventHandler.processEvent(endRepairCleanupEntity, null);

            // ABase
            validateWorkflow(workflowDescription, "ABase", shearingCleanupPlate);
            LabEvent aBaseEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    libraryConstructionJaxbBuilder.getaBaseJaxb(), shearingCleanupPlate);
            labEventHandler.processEvent(aBaseEntity, null);

            // ABaseCleanup
            validateWorkflow(workflowDescription, "ABaseCleanup", shearingCleanupPlate);
            LabEvent aBaseCleanupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    libraryConstructionJaxbBuilder.getaBaseCleanupJaxb(), shearingCleanupPlate);
            labEventHandler.processEvent(aBaseCleanupEntity, null);

            // IndexedAdapterLigation
            validateWorkflow(workflowDescription, "IndexedAdapterLigation", shearingCleanupPlate);
            BuildIndexPlate buildIndexPlate = new BuildIndexPlate(libraryConstructionJaxbBuilder.getIndexPlateBarcode()).invoke();
            StaticPlate indexPlate = buildIndexPlate.getIndexPlate();
            LabEvent indexedAdapterLigationEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    libraryConstructionJaxbBuilder.getIndexedAdapterLigationJaxb(), indexPlate, shearingCleanupPlate);
            labEventHandler.processEvent(indexedAdapterLigationEntity, null);
            // asserts
            Set<SampleInstance> postIndexingSampleInstances = shearingCleanupPlate.getVesselContainer().getSampleInstancesAtPosition(VesselPosition.A01);
//            PlateWell plateWellA1PostIndex = shearingCleanupPlate.getVesselContainer().getVesselAtPosition("A01");
//            Assert.assertEquals(plateWellA1PostIndex.getAppliedReagents().iterator().next(), index301, "Wrong reagent");
            SampleInstance sampleInstance = postIndexingSampleInstances.iterator().next();
            MolecularIndexReagent molecularIndexReagent = (MolecularIndexReagent) sampleInstance.getReagents().iterator().next();
            Assert.assertEquals(molecularIndexReagent.getMolecularIndexingScheme().getName(), "Illumina_P7-Fenok", "Wrong index");

            // AdapterLigationCleanup
            validateWorkflow(workflowDescription, "AdapterLigationCleanup", shearingCleanupPlate);
            LabEvent ligationCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    libraryConstructionJaxbBuilder.getLigationCleanupJaxb(), shearingCleanupPlate, null);
            labEventHandler.processEvent(ligationCleanupEntity, null);
            StaticPlate ligationCleanupPlate = (StaticPlate) ligationCleanupEntity.getTargetLabVessels().iterator().next();

            // PondEnrichment
            validateWorkflow(workflowDescription, "PondEnrichment", ligationCleanupPlate);
            LabEvent pondEnrichmentEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    libraryConstructionJaxbBuilder.getPondEnrichmentJaxb(), ligationCleanupPlate);
            labEventHandler.processEvent(pondEnrichmentEntity, null);

            // HybSelPondEnrichmentCleanup
            validateWorkflow(workflowDescription, "HybSelPondEnrichmentCleanup", ligationCleanupPlate);
            LabEvent pondCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    libraryConstructionJaxbBuilder.getPondCleanupJaxb(), ligationCleanupPlate, null);
            labEventHandler.processEvent(pondCleanupEntity, null);
            StaticPlate pondCleanupPlate = (StaticPlate) pondCleanupEntity.getTargetLabVessels().iterator().next();

            // PondRegistration
            validateWorkflow(workflowDescription, "PondRegistration", pondCleanupPlate);
            Map<String, TwoDBarcodedTube> mapBarcodeToPondRegTube = new HashMap<String, TwoDBarcodedTube>();
            LabEvent pondRegistrationEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(
                    libraryConstructionJaxbBuilder.getPondRegistrationJaxb(), pondCleanupPlate, mapBarcodeToPondRegTube);
            labEventHandler.processEvent(pondRegistrationEntity, null);
            // asserts
            pondRegRack = (RackOfTubes) pondRegistrationEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(pondRegRack.getSampleInstances().size(),
                    shearingPlate.getSampleInstances().size(), "Wrong number of sample instances");
            Set<SampleInstance> sampleInstancesInPondRegWell = pondRegRack.getVesselContainer().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(sampleInstancesInPondRegWell.size(), 1, "Wrong number of sample instances in position");
            Assert.assertEquals(sampleInstancesInPondRegWell.iterator().next().getStartingSample().getSampleName(),
                    shearingPlate.getSampleInstances().iterator().next().getStartingSample().getSampleName(), "Wrong sample");
            return this;
        }
    }

    /**
     * Builds a plate of molecular indexes
     */
    public static class BuildIndexPlate {
        private final String indexPlateBarcode;
        private StaticPlate indexPlate;
        private MolecularIndexReagent index301;

        public BuildIndexPlate(String indexPlateBarcode) {
            this.indexPlateBarcode = indexPlateBarcode;
        }

        public StaticPlate getIndexPlate() {
            return indexPlate;
        }

        public BuildIndexPlate invoke() {
            indexPlate = new StaticPlate(indexPlateBarcode, StaticPlate.PlateType.IndexedAdapterPlate96);
            PlateWell plateWellA01 = new PlateWell(indexPlate, VesselPosition.A01);
            index301 = new MolecularIndexReagent(new MolecularIndexingScheme(
                    new HashMap<MolecularIndexingScheme.PositionHint, MolecularIndex>(){{
                        put(MolecularIndexingScheme.IlluminaPositionHint.P7, new MolecularIndex("ATCGATCG"));}}));
            plateWellA01.addReagent(index301);
            indexPlate.getVesselContainer().addContainedVessel(plateWellA01, VesselPosition.A01);
            PlateWell plateWellA02 = new PlateWell(indexPlate, VesselPosition.A02);
            plateWellA02.addReagent(new MolecularIndexReagent(new MolecularIndexingScheme(
                    new HashMap<MolecularIndexingScheme.PositionHint, MolecularIndex>(){{
                        put(MolecularIndexingScheme.IlluminaPositionHint.P7, new MolecularIndex("TCGATCGA"));}})));
            indexPlate.getVesselContainer().addContainedVessel(plateWellA02, VesselPosition.A02);
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

            endRepairCleanupJaxb = bettaLimsMessageFactory.buildPlateEvent("EndRepairCleanup", shearCleanPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage1 = new BettaLIMSMessage();
            bettaLIMSMessage1.getPlateEvent().add(endRepairCleanupJaxb);
            messageList.add(bettaLIMSMessage1);

            aBaseJaxb = bettaLimsMessageFactory.buildPlateEvent("ABase", shearCleanPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
            bettaLIMSMessage2.getPlateEvent().add(aBaseJaxb);
            messageList.add(bettaLIMSMessage2);

            aBaseCleanupJaxb = bettaLimsMessageFactory.buildPlateEvent("ABaseCleanup", shearCleanPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage3 = new BettaLIMSMessage();
            bettaLIMSMessage3.getPlateEvent().add(aBaseCleanupJaxb);
            messageList.add(bettaLIMSMessage3);

//            indexPlateBarcode = "IndexPlate" + testPrefix;
            indexedAdapterLigationJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "IndexedAdapterLigation", indexPlateBarcode, shearCleanPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage4 = new BettaLIMSMessage();
            bettaLIMSMessage4.getPlateTransferEvent().add(indexedAdapterLigationJaxb);
            messageList.add(bettaLIMSMessage4);

            String ligationCleanupBarcode = "ligationCleanupPlate" + testPrefix;
            ligationCleanupJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "AdapterLigationCleanup", shearCleanPlateBarcode, ligationCleanupBarcode);
            BettaLIMSMessage bettaLIMSMessage5 = new BettaLIMSMessage();
            bettaLIMSMessage5.getPlateTransferEvent().add(ligationCleanupJaxb);
            messageList.add(bettaLIMSMessage5);

            pondEnrichmentJaxb = bettaLimsMessageFactory.buildPlateEvent("PondEnrichment", ligationCleanupBarcode);
            BettaLIMSMessage bettaLIMSMessage6 = new BettaLIMSMessage();
            bettaLIMSMessage6.getPlateEvent().add(pondEnrichmentJaxb);
            messageList.add(bettaLIMSMessage6);

            String pondCleanupBarcode = "pondCleanupPlate" + testPrefix;
            pondCleanupJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "HybSelPondEnrichmentCleanup", ligationCleanupBarcode, pondCleanupBarcode);
            BettaLIMSMessage bettaLIMSMessage7 = new BettaLIMSMessage();
            bettaLIMSMessage7.getPlateTransferEvent().add(pondCleanupJaxb);
            messageList.add(bettaLIMSMessage7);

            pondRegRackBarcode = "PondReg" + testPrefix;
            pondRegTubeBarcodes = new ArrayList<String>();
            for(int rackPosition = 1; rackPosition <= numSamples; rackPosition++) {
                pondRegTubeBarcodes.add(POND_REGISTRATION_TUBE_PREFIX + testPrefix + rackPosition);
            }
            pondRegistrationJaxb = bettaLimsMessageFactory.buildPlateToRack(
                    "PondRegistration", pondCleanupBarcode, pondRegRackBarcode, pondRegTubeBarcodes);
            BettaLIMSMessage bettaLIMSMessage8 = new BettaLIMSMessage();
            bettaLIMSMessage8.getPlateTransferEvent().add(pondRegistrationJaxb);
            messageList.add(bettaLIMSMessage8);

            return this;
        }
    }

    /**
     * Builds entity graph for Hybrid Selection events
     */
    public static class HybridSelectionEntityBuilder {
        private final WorkflowDescription workflowDescription;
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

        public HybridSelectionEntityBuilder(WorkflowDescription workflowDescription, BettaLimsMessageFactory bettaLimsMessageFactory,
                LabEventFactory labEventFactory, LabEventHandler labEventHandler, RackOfTubes pondRegRack,
                String pondRegRackBarcode, List<String> pondRegTubeBarcodes) {
            this.workflowDescription = workflowDescription;
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
            validateWorkflow(workflowDescription, "PreSelectionPool", pondRegRack); //todo jmt should be mapBarcodeToPondRegTube.values());
            Map<String, TwoDBarcodedTube> mapBarcodeToPreSelPoolTube = new HashMap<String, TwoDBarcodedTube>();
            Map<String, TwoDBarcodedTube> mapBarcodeToPondTube = new HashMap<String, TwoDBarcodedTube>();
            for (TwoDBarcodedTube twoDBarcodedTube : pondRegRack.getVesselContainer().getContainedVessels()) {
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
            labEventHandler.processEvent(preSelPoolEntity, null);
            RackOfTubes preSelPoolRack = (RackOfTubes) preSelPoolEntity.getTargetLabVessels().iterator().next();
            LabEvent preSelPoolEntity2 = labEventFactory.buildFromBettaLimsRackToRackDbFree(hybridSelectionJaxbBuilder.getPreSelPoolJaxb2(),
                    mapBarcodeToPreSelSource2Tube, preSelPoolRack);
            labEventHandler.processEvent(preSelPoolEntity2, null);
            //asserts
            Set<SampleInstance> preSelPoolSampleInstances = preSelPoolRack.getSampleInstances();
            Assert.assertEquals(preSelPoolSampleInstances.size(),
                    pondRegRack.getSampleInstances().size(), "Wrong number of sample instances");
            Set<String> sampleNames = new HashSet<String>();
            for (SampleInstance preSelPoolSampleInstance : preSelPoolSampleInstances) {
                if(!sampleNames.add(preSelPoolSampleInstance.getStartingSample().getSampleName())) {
                    Assert.fail("Duplicate sample " + preSelPoolSampleInstance.getStartingSample().getSampleName());
                }
            }
            Set<SampleInstance> sampleInstancesInPreSelPoolWell = preSelPoolRack.getVesselContainer().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(sampleInstancesInPreSelPoolWell.size(), 2, "Wrong number of sample instances in position");

            // Hybridization
            validateWorkflow(workflowDescription, "Hybridization", preSelPoolRack);
            LabEvent hybridizationEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(hybridSelectionJaxbBuilder.getHybridizationJaxb(), preSelPoolRack, null);
            labEventHandler.processEvent(hybridizationEntity, null);
            StaticPlate hybridizationPlate = (StaticPlate) hybridizationEntity.getTargetLabVessels().iterator().next();

            // BaitSetup
            TwoDBarcodedTube baitTube = buildBaitTube(hybridSelectionJaxbBuilder.getBaitTubeBarcode());
            LabEvent baitSetupEntity = labEventFactory.buildVesselToSectionDbFree(hybridSelectionJaxbBuilder.getBaitSetupJaxb(),
                    baitTube, null, SBSSection.ALL96.getSectionName());
            labEventHandler.processEvent(baitSetupEntity, null);
            StaticPlate baitSetupPlate = (StaticPlate) baitSetupEntity.getTargetLabVessels().iterator().next();

            // BaitAddition
            validateWorkflow(workflowDescription, "BaitAddition", hybridizationPlate);
            LabEvent baitAdditionEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(hybridSelectionJaxbBuilder.getBaitAdditionJaxb(), baitSetupPlate, hybridizationPlate);
            labEventHandler.processEvent(baitAdditionEntity, null);

            // BeadAddition
            validateWorkflow(workflowDescription, "BeadAddition", hybridizationPlate);
            LabEvent beadAdditionEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(hybridSelectionJaxbBuilder.getBeadAdditionJaxb(), hybridizationPlate);
            labEventHandler.processEvent(beadAdditionEntity, null);

            // APWash
            validateWorkflow(workflowDescription, "APWash", hybridizationPlate);
            LabEvent apWashEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(hybridSelectionJaxbBuilder.getApWashJaxb(), hybridizationPlate);
            labEventHandler.processEvent(apWashEntity, null);

            // GSWash1
            validateWorkflow(workflowDescription, "GSWash1", hybridizationPlate);
            LabEvent gsWash1Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(hybridSelectionJaxbBuilder.getGsWash1Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash1Entity, null);

            // GSWash2
            validateWorkflow(workflowDescription, "GSWash2", hybridizationPlate);
            LabEvent gsWash2Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(hybridSelectionJaxbBuilder.getGsWash2Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash2Entity, null);

            // CatchEnrichmentSetup
            validateWorkflow(workflowDescription, "CatchEnrichmentSetup", hybridizationPlate);
            LabEvent catchEnrichmentSetupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    hybridSelectionJaxbBuilder.getCatchEnrichmentSetupJaxb(), hybridizationPlate);
            labEventHandler.processEvent(catchEnrichmentSetupEntity, null);

            // CatchEnrichmentCleanup
            validateWorkflow(workflowDescription, "CatchEnrichmentCleanup", hybridizationPlate);
            LabEvent catchEnrichmentCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    hybridSelectionJaxbBuilder.getCatchEnrichmentCleanupJaxb(), hybridizationPlate, null);
            labEventHandler.processEvent(catchEnrichmentCleanupEntity, null);
            StaticPlate catchCleanPlate = (StaticPlate) catchEnrichmentCleanupEntity.getTargetLabVessels().iterator().next();

            // NormalizedCatchRegistration
            validateWorkflow(workflowDescription, "NormalizedCatchRegistration", catchCleanPlate);
            mapBarcodeToNormCatchTubes = new HashMap<String, TwoDBarcodedTube>();
            LabEvent normCatchEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(hybridSelectionJaxbBuilder.getNormCatchJaxb(),
                    catchCleanPlate, mapBarcodeToNormCatchTubes);
            labEventHandler.processEvent(normCatchEntity, null);
            normCatchRack = (RackOfTubes) normCatchEntity.getTargetLabVessels().iterator().next();
            return this;
        }
    }

    public static TwoDBarcodedTube buildBaitTube(String tubeBarcode) {
        TwoDBarcodedTube baitTube = new TwoDBarcodedTube(tubeBarcode);
        baitTube.addReagent(new GenericReagent("BaitSet", "xyz", null));
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
            for(int rackPosition = 1; rackPosition <= pondRegTubeBarcodes.size() / 2; rackPosition++) {
                preSelPoolBarcodes.add("PreSelPool" + testPrefix + rackPosition);
            }
            String preSelPoolRackBarcode = "PreSelPool" + testPrefix;
            preSelPoolJaxb = bettaLimsMessageFactory.buildRackToRack("PreSelectionPool", pondRegRackBarcode,
                    pondRegTubeBarcodes.subList(0, pondRegTubeBarcodes.size() / 2), preSelPoolRackBarcode, preSelPoolBarcodes);
            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            bettaLIMSMessage.getPlateTransferEvent().add(preSelPoolJaxb);
            messageList.add(bettaLIMSMessage);

            preSelPoolJaxb2 = bettaLimsMessageFactory.buildRackToRack("PreSelectionPool", pondRegRackBarcode,
                    pondRegTubeBarcodes.subList(pondRegTubeBarcodes.size() / 2, pondRegTubeBarcodes.size()),
                    preSelPoolRackBarcode, preSelPoolBarcodes);
            BettaLIMSMessage bettaLIMSMessage1 = new BettaLIMSMessage();
            bettaLIMSMessage1.getPlateTransferEvent().add(preSelPoolJaxb2);
            messageList.add(bettaLIMSMessage1);

            String hybridizationPlateBarcode = "Hybrid" + testPrefix;
            hybridizationJaxb = bettaLimsMessageFactory.buildRackToPlate(
                    "Hybridization", preSelPoolRackBarcode, preSelPoolBarcodes, hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
            bettaLIMSMessage2.getPlateTransferEvent().add(hybridizationJaxb);
            messageList.add(bettaLIMSMessage2);

            baitTubeBarcode = "Bait" + testPrefix;
            String baitSetupBarcode = "BaitSetup" + testPrefix;
            baitSetupJaxb = bettaLimsMessageFactory.buildTubeToPlate("BaitSetup", baitTubeBarcode,
                    baitSetupBarcode);
            BettaLIMSMessage bettaLIMSMessage3 = new BettaLIMSMessage();
            bettaLIMSMessage3.setReceptaclePlateTransferEvent(baitSetupJaxb);
            messageList.add(bettaLIMSMessage3);

            baitAdditionJaxb = bettaLimsMessageFactory.buildPlateToPlate("BaitAddition", baitSetupBarcode,
                    hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage4 = new BettaLIMSMessage();
            bettaLIMSMessage4.getPlateTransferEvent().add(baitAdditionJaxb);
            messageList.add(bettaLIMSMessage4);

            beadAdditionJaxb = bettaLimsMessageFactory.buildPlateEvent("BeadAddition", hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage5 = new BettaLIMSMessage();
            bettaLIMSMessage5.getPlateEvent().add(beadAdditionJaxb);
            messageList.add(bettaLIMSMessage5);

            apWashJaxb = bettaLimsMessageFactory.buildPlateEvent("APWash", hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage6 = new BettaLIMSMessage();
            bettaLIMSMessage6.getPlateEvent().add(apWashJaxb);
            messageList.add(bettaLIMSMessage6);

            gsWash1Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash1", hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage7 = new BettaLIMSMessage();
            bettaLIMSMessage7.getPlateEvent().add(gsWash1Jaxb);
            messageList.add(bettaLIMSMessage7);

            gsWash2Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash2", hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage8 = new BettaLIMSMessage();
            bettaLIMSMessage8.getPlateEvent().add(gsWash2Jaxb);
            messageList.add(bettaLIMSMessage8);

            catchEnrichmentSetupJaxb = bettaLimsMessageFactory.buildPlateEvent("CatchEnrichmentSetup", hybridizationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage9 = new BettaLIMSMessage();
            bettaLIMSMessage9.getPlateEvent().add(catchEnrichmentSetupJaxb);
            messageList.add(bettaLIMSMessage9);

            String catchCleanupBarcode = "catchCleanPlate" + testPrefix;
            catchEnrichmentCleanupJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "CatchEnrichmentCleanup", hybridizationPlateBarcode, catchCleanupBarcode);
            BettaLIMSMessage bettaLIMSMessage10 = new BettaLIMSMessage();
            bettaLIMSMessage10.getPlateTransferEvent().add(catchEnrichmentCleanupJaxb);
            messageList.add(bettaLIMSMessage10);

            normCatchBarcodes = new ArrayList<String>();
            for(int rackPosition = 1; rackPosition <= pondRegTubeBarcodes.size() / 2; rackPosition++) {
                normCatchBarcodes.add("NormCatch" + testPrefix + rackPosition);
            }
            normCatchRackBarcode = "NormCatchRack";
            normCatchJaxb = bettaLimsMessageFactory.buildPlateToRack(
                    "NormalizedCatchRegistration", hybridizationPlateBarcode, normCatchRackBarcode, normCatchBarcodes);
            BettaLIMSMessage bettaLIMSMessage11 = new BettaLIMSMessage();
            bettaLIMSMessage11.getPlateTransferEvent().add(normCatchJaxb);
            messageList.add(bettaLIMSMessage11);

            return this;
        }
    }

    /**
     * Builds entity graph for Qtp events
     */
    public static class QtpEntityBuilder {
        private final WorkflowDescription workflowDescription;
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;
        private final RackOfTubes normCatchRack;
        private final String normCatchRackBarcode;
        private final List<String> normCatchBarcodes;
        private final Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes;

        private RackOfTubes denatureRack;

        public QtpEntityBuilder(WorkflowDescription workflowDescription, BettaLimsMessageFactory bettaLimsMessageFactory,
                LabEventFactory labEventFactory, LabEventHandler labEventHandler, RackOfTubes normCatchRack,
                String normCatchRackBarcode, List<String> normCatchBarcodes, Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes) {
            this.workflowDescription = workflowDescription;
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
            PlateCherryPickEvent cherryPickJaxb = qtpJaxbBuilder.getCherryPickJaxb();
            final String poolRackBarcode = qtpJaxbBuilder.getPoolRackBarcode();
            PlateCherryPickEvent denatureJaxb = qtpJaxbBuilder.getDenatureJaxb();
            final String denatureRackBarcode = qtpJaxbBuilder.getDenatureRackBarcode();
            PlateCherryPickEvent stripTubeTransferJaxb = qtpJaxbBuilder.getStripTubeTransferJaxb();
            final String stripTubeHolderBarcode = qtpJaxbBuilder.getStripTubeHolderBarcode();
            PlateTransferEventType flowcellTransferJaxb = qtpJaxbBuilder.getFlowcellTransferJaxb();

            // PoolingTransfer
            validateWorkflow(workflowDescription, "PoolingTransfer", normCatchRack);
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
            labEventHandler.processEvent(poolingEntity, null);
            // asserts
            final RackOfTubes poolingRack = (RackOfTubes) poolingEntity.getTargetLabVessels().iterator().next();
            Set<SampleInstance> pooledSampleInstances = poolingRack.getVesselContainer().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(pooledSampleInstances.size(), normCatchRack.getSampleInstances().size(), "Wrong number of pooled samples");

            // DenatureTransfer
            validateWorkflow(workflowDescription, "DenatureTransfer", poolingRack);
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
            labEventHandler.processEvent(denatureEntity, null);
            // asserts
            denatureRack = (RackOfTubes) denatureEntity.getTargetLabVessels().iterator().next();
            Set<SampleInstance> denaturedSampleInstances = denatureRack.getVesselContainer().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(denaturedSampleInstances.size(), normCatchRack.getSampleInstances().size(), "Wrong number of denatured samples");

            // StripTubeBTransfer
            validateWorkflow(workflowDescription, "StripTubeBTransfer", denatureRack);
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
            labEventHandler.processEvent(stripTubeTransferEntity, null);
            // asserts
            StripTube stripTube = (StripTube) stripTubeTransferEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(stripTube.getVesselContainer().getSampleInstancesAtPosition(VesselPosition.TUBE1).size(),
                    normCatchRack.getSampleInstances().size(), "Wrong number of samples in strip tube well");

            // FlowcellTransfer
            validateWorkflow(workflowDescription, "FlowcellTransfer", stripTube);
            LabEvent flowcellTransferEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(flowcellTransferJaxb, stripTube, null);
            labEventHandler.processEvent(flowcellTransferEntity, null);
            //asserts
            IlluminaFlowcell illuminaFlowcell = (IlluminaFlowcell) flowcellTransferEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(illuminaFlowcell.getVesselContainer().getSampleInstancesAtPosition(VesselPosition.LANE1).size(),
                    normCatchRack.getSampleInstances().size(), "Wrong number of samples in flowcell lane");
        }

        public RackOfTubes getDenatureRack() {
            return denatureRack;
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
        private PlateCherryPickEvent cherryPickJaxb;
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

        public PlateCherryPickEvent getCherryPickJaxb() {
            return cherryPickJaxb;
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
            poolRackBarcode = "PoolRack" + testPrefix;
            List<BettaLimsMessageFactory.CherryPick> poolingCherryPicks = new ArrayList<BettaLimsMessageFactory.CherryPick>();
            List<String> poolTubeBarcodes = new ArrayList<String>();
            for(int rackPosition = 1; rackPosition <= normCatchBarcodes.size(); rackPosition++) {
                poolingCherryPicks.add(new BettaLimsMessageFactory.CherryPick(normCatchRackBarcode,
                        bettaLimsMessageFactory.buildWellName(rackPosition), poolRackBarcode, "A01"));
                poolTubeBarcodes.add("Pool" + testPrefix + rackPosition);
            }
            cherryPickJaxb = bettaLimsMessageFactory.buildCherryPick("PoolingTransfer",
                    Arrays.asList(normCatchRackBarcode), Arrays.asList(normCatchBarcodes), poolRackBarcode, poolTubeBarcodes,
                    poolingCherryPicks);
            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            bettaLIMSMessage.getPlateCherryPickEvent().add(cherryPickJaxb);
            messageList.add(bettaLIMSMessage);

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

            stripTubeHolderBarcode = "StripTubeHolder" + testPrefix;
            List<BettaLimsMessageFactory.CherryPick> stripTubeCherryPicks = new ArrayList<BettaLimsMessageFactory.CherryPick>();
            for(int rackPosition = 0; rackPosition < 8; rackPosition++) {
                stripTubeCherryPicks.add(new BettaLimsMessageFactory.CherryPick(denatureRackBarcode,
                        "A01", stripTubeHolderBarcode,
                        Character.toString((char)('A' + rackPosition)) + "01"));
            }
            String stripTubeBarcode = "StripTube" + testPrefix + "1";
            stripTubeTransferJaxb = bettaLimsMessageFactory.buildCherryPickToStripTube("StripTubeBTransfer",
                    Arrays.asList(denatureRackBarcode), Arrays.asList(denatureTubeBarcodes),
                    stripTubeHolderBarcode, Arrays.asList(stripTubeBarcode), stripTubeCherryPicks);
            BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
            bettaLIMSMessage2.getPlateCherryPickEvent().add(stripTubeTransferJaxb);
            messageList.add(bettaLIMSMessage2);

            flowcellBarcode = "Flowcell" + testPrefix;
            flowcellTransferJaxb = bettaLimsMessageFactory.buildStripTubeToFlowcell("FlowcellTransfer",
                    stripTubeBarcode, flowcellBarcode);
            BettaLIMSMessage bettaLIMSMessage3 = new BettaLIMSMessage();
            bettaLIMSMessage3.getPlateTransferEvent().add(flowcellTransferJaxb);
            messageList.add(bettaLIMSMessage3);

            return this;
        }
    }

    private WorkQueueDAO createMockWorkQueueDAO() {
        WorkQueueDAO workQueueDAO = EasyMock.createMock(WorkQueueDAO.class);
        EasyMock.expect(workQueueDAO.getPendingQueues(
                (LabVessel)EasyMock.anyObject(),
                (WorkflowDescription)EasyMock.anyObject()
        )).andReturn(Collections.<LabWorkQueue>emptySet()).anyTimes();

        EasyMock.replay(workQueueDAO);
        return workQueueDAO;
    }
}
