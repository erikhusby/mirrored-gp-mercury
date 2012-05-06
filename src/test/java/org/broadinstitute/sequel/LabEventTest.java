package org.broadinstitute.sequel;

//import com.jprofiler.api.agent.Controller;
import org.broadinstitute.sequel.bettalims.jaxb.BettaLIMSMessage;
import org.broadinstitute.sequel.bettalims.jaxb.PlateCherryPickEvent;
import org.broadinstitute.sequel.bettalims.jaxb.PlateEventType;
import org.broadinstitute.sequel.bettalims.jaxb.PlateTransferEventType;
import org.broadinstitute.sequel.bettalims.jaxb.ReceptaclePlateTransferEvent;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.control.dao.workflow.WorkQueueDAO;
import org.broadinstitute.sequel.control.workflow.WorkflowParser;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.BasicProject;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.queue.LabWorkQueue;
import org.broadinstitute.sequel.entity.reagent.GenericReagent;
import org.broadinstitute.sequel.entity.run.IlluminaFlowcell;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.StripTube;
import org.broadinstitute.sequel.entity.vessel.VesselPosition;
import org.broadinstitute.sequel.infrastructure.jira.DummyJiraService;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.reagent.IndexEnvelope;
import org.broadinstitute.sequel.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.vessel.PlateWell;
import org.broadinstitute.sequel.entity.vessel.RackOfTubes;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

/**
 * Test messaging
 */
@SuppressWarnings({"FeatureEnvy", "OverlyCoupledClass"})
public class LabEventTest {
    public static final int NUM_POSITIONS_IN_RACK = 96;

    @Test(groups = {DATABASE_FREE})
    public void testHybridSelection() {
//        Controller.startCPURecording(true);

        Map<LabEventName,PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();
        Project project = new BasicProject("LabEventTesting", new JiraTicket(new DummyJiraService(),"TP-0","0"));
        WorkflowDescription workflowDescription = new WorkflowDescription("HS", billableEvents, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);
        ProjectPlan projectPlan = new ProjectPlan(project,"To test hybrid selection", workflowDescription);

        WorkflowParser workflowParser = new WorkflowParser(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("HybridSelectionV2.bpmn"));
        workflowDescription.setStartState(workflowParser.getStartState());
        workflowDescription.setMapNameToTransitionList(workflowParser.getMapNameToTransitionList());

        // starting rack
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for(int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK; rackPosition++) {
            SampleSheet sampleSheet = new SampleSheet();
            sampleSheet.addStartingSample(new BSPSample("SM-" + rackPosition, projectPlan, null));
            String barcode = "R" + rackPosition;
            mapBarcodeToTube.put(barcode, new TwoDBarcodedTube(barcode, sampleSheet));
        }

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setPersonDAO(new PersonDAO());
        LabEventHandler labEventHandler = new LabEventHandler(createMockWorkQueueDAO());

        Shearing shearing = new Shearing(workflowDescription, mapBarcodeToTube, bettaLimsMessageFactory, labEventFactory,
                labEventHandler).invoke();
        StaticPlate shearingCleanupPlate = shearing.getShearingCleanupPlate();
        String shearPlateBarcode = shearing.getShearPlateBarcode();
        String shearCleanPlateBarcode = shearing.getShearCleanPlateBarcode();
        StaticPlate shearingPlate = shearing.getShearingPlate();

        LibraryConstruction libraryConstruction = new LibraryConstruction(workflowDescription, bettaLimsMessageFactory,
                labEventFactory, labEventHandler, shearingCleanupPlate, shearCleanPlateBarcode, shearingPlate).invoke();
        RackOfTubes pondRegRack = libraryConstruction.getPondRegRack();
        String pondRegRackBarcode = libraryConstruction.getPondRegRackBarcode();
        List<String> pondRegTubeBarcodes = libraryConstruction.getPondRegTubeBarcodes();

        HybridSelection hybridSelection = new HybridSelection(workflowDescription, bettaLimsMessageFactory, labEventFactory,
                labEventHandler, pondRegRack, pondRegRackBarcode, pondRegTubeBarcodes).invoke();
        RackOfTubes normCatchRack = hybridSelection.getNormCatchRack();
        String normCatchRackBarcode = hybridSelection.getNormCatchRackBarcode();
        List<String> normCatchBarcodes = hybridSelection.getNormCatchBarcodes();
        Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes = hybridSelection.getMapBarcodeToNormCatchTubes();

        new Qtp(workflowDescription, bettaLimsMessageFactory, labEventFactory, labEventHandler, normCatchRack,
                normCatchRackBarcode, normCatchBarcodes, mapBarcodeToNormCatchTubes).invoke();

        // Register run
//        IlluminaSequencingRun illuminaSequencingRun = new IlluminaSequencingRun();
        // Zims query

//        Controller.stopCPURecording();
        // tube has two sample instances
        // indexes

        /*
        Queries:
            project -> samples
            sample -> vessels
            vessel -> sample instances / molecular state / molecular envelope
            show only vessels that are farthest from root?
            need a concept of depleting a tube / well?
         */
    }

    @Test(groups = {DATABASE_FREE})
    public void testWholeGenomeShotgun() {
//        Controller.startCPURecording(true);

        Map<LabEventName,PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();
        Project project = new BasicProject("LabEventTesting", new JiraTicket(new DummyJiraService(),"TP-0","0"));
        WorkflowDescription workflowDescription = new WorkflowDescription("WGS", billableEvents, CreateIssueRequest.Fields.Issuetype.Whole_Genome_Shotgun);
        ProjectPlan projectPlan = new ProjectPlan(project, "To test whole genome shotgun", workflowDescription);

        WorkflowParser workflowParser = new WorkflowParser(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("WholeGenomeShotgun.bpmn"));
        workflowDescription.setStartState(workflowParser.getStartState());
        workflowDescription.setMapNameToTransitionList(workflowParser.getMapNameToTransitionList());

        // starting rack
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for(int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK; rackPosition++) {
            SampleSheet sampleSheet = new SampleSheet();
            sampleSheet.addStartingSample(new BSPSample("SM-" + rackPosition, projectPlan, null));
            String barcode = "R" + rackPosition;
            mapBarcodeToTube.put(barcode, new TwoDBarcodedTube(barcode, sampleSheet));
        }

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setPersonDAO(new PersonDAO());
        LabEventHandler labEventHandler = new LabEventHandler(createMockWorkQueueDAO());

        Shearing shearing = new Shearing(workflowDescription, mapBarcodeToTube, bettaLimsMessageFactory, labEventFactory,
                labEventHandler).invoke();
        StaticPlate shearingCleanupPlate = shearing.getShearingCleanupPlate();
        String shearPlateBarcode = shearing.getShearPlateBarcode();
        String shearCleanPlateBarcode = shearing.getShearCleanPlateBarcode();
        StaticPlate shearingPlate = shearing.getShearingPlate();

        LibraryConstruction libraryConstruction = new LibraryConstruction(workflowDescription, bettaLimsMessageFactory,
                labEventFactory, labEventHandler, shearingCleanupPlate, shearCleanPlateBarcode, shearingPlate).invoke();
        RackOfTubes pondRegRack = libraryConstruction.getPondRegRack();
        String pondRegRackBarcode = libraryConstruction.getPondRegRackBarcode();
        List<String> pondRegTubeBarcodes = libraryConstruction.getPondRegTubeBarcodes();

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
                    pondRegRackBarcode, pondRegTubeBarcodes.subList(i * 4, i * 4 + 4), sageCassetteBarcode);
            // todo jmt SAGE section
            LabEvent sageLoadingEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(sageLoadingJaxb, pondRegRack, null);
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
                    bettaLimsMessageFactory.buildWellName(i + 1));
        }
        LabEvent sageCleanupEntity = labEventFactory.buildFromBettaLimsRackToRackDbFree(sageCleanupJaxb,
                sageUnloadRackRearrayed, new HashMap<String, TwoDBarcodedTube>());
        labEventHandler.processEvent(sageCleanupEntity, null);
        RackOfTubes sageCleanupRack = (RackOfTubes) sageCleanupEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(sageCleanupRack.getSampleInstances().size(), NUM_POSITIONS_IN_RACK, "Wrong number of sage cleanup samples");

        new Qtp(workflowDescription, bettaLimsMessageFactory, labEventFactory, labEventHandler, sageCleanupRack,
                sageCleanupBarcode, sageCleanupTubeBarcodes, mapBarcodeToSageUnloadTubes).invoke();

        // Register run
//        IlluminaSequencingRun illuminaSequencingRun = new IlluminaSequencingRun();
        // Zims query

//        Controller.stopCPURecording();
        // tube has two sample instances
        // indexes
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

    private static class Shearing {
        private final WorkflowDescription workflowDescription;
        private final Map<String, TwoDBarcodedTube> mapBarcodeToTube;
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;
        private String shearPlateBarcode;
        private StaticPlate shearingPlate;
        private String shearCleanPlateBarcode;
        private StaticPlate shearingCleanupPlate;

        private Shearing(WorkflowDescription workflowDescription, Map<String, TwoDBarcodedTube> mapBarcodeToTube,
                BettaLimsMessageFactory bettaLimsMessageFactory, LabEventFactory labEventFactory, LabEventHandler labEventHandler) {
            this.workflowDescription = workflowDescription;
            this.mapBarcodeToTube = mapBarcodeToTube;
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
        }

        public String getShearPlateBarcode() {
            return shearPlateBarcode;
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

        public Shearing invoke() {
            ShearingJaxb shearingJaxb = new ShearingJaxb(bettaLimsMessageFactory, new ArrayList<String>(mapBarcodeToTube.keySet()),
                    "").invoke();
            this.shearPlateBarcode = shearingJaxb.getShearPlateBarcode();
            this.shearCleanPlateBarcode = shearingJaxb.getShearCleanPlateBarcode();

            // ShearingTransfer
            validateWorkflow(workflowDescription, "ShearingTransfer", mapBarcodeToTube.values());
            LabEvent shearingTransferEventEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    shearingJaxb.getShearingTransferEventJaxb(), mapBarcodeToTube, null);
            labEventHandler.processEvent(shearingTransferEventEntity, null);
            // asserts
            shearingPlate = (StaticPlate) shearingTransferEventEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(shearingPlate.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

            // PostShearingTransferCleanup
            validateWorkflow(workflowDescription, "PostShearingTransferCleanup", shearingPlate);
            LabEvent postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    shearingJaxb.getPostShearingTransferCleanupEventJaxb(), shearingPlate, null);
            labEventHandler.processEvent(postShearingTransferCleanupEntity, null);
            // asserts
            shearingCleanupPlate = (StaticPlate) postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(shearingCleanupPlate.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");
            Set<SampleInstance> sampleInstancesInWell = shearingCleanupPlate.getVesselContainer().getSampleInstancesAtPosition("A08");
            Assert.assertEquals(sampleInstancesInWell.size(), 1, "Wrong number of sample instances in well");
            Assert.assertEquals(sampleInstancesInWell.iterator().next().getStartingSample().getSampleName(), "SM-8", "Wrong sample");

            // ShearingQC
            validateWorkflow(workflowDescription, "ShearingQC", shearingCleanupPlate);
            LabEvent shearingQcEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    shearingJaxb.getShearingQcEventJaxb(), shearingCleanupPlate, null);
            labEventHandler.processEvent(shearingQcEntity, null);
            return this;
        }

    }

    public static class ShearingJaxb {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final List<String> tubeBarcodeList;
        private final String testPrefix;
        private String shearPlateBarcode;
        private String shearCleanPlateBarcode;

        private PlateTransferEventType shearingTransferEventJaxb;
        private PlateTransferEventType postShearingTransferCleanupEventJaxb;
        private PlateTransferEventType shearingQcEventJaxb;
        private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();

        public ShearingJaxb(BettaLimsMessageFactory bettaLimsMessageFactory, List<String> tubeBarcodeList,
                String testPrefix) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.tubeBarcodeList = tubeBarcodeList;
            this.testPrefix = testPrefix;
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

        public ShearingJaxb invoke() {
            shearPlateBarcode = "ShearPlate" + testPrefix;
            shearingTransferEventJaxb = bettaLimsMessageFactory.buildRackToPlate(
                    "ShearingTransfer", "KioskRack" + testPrefix, tubeBarcodeList, shearPlateBarcode);
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

    private static class LibraryConstruction {
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

        private LibraryConstruction(WorkflowDescription workflowDescription, BettaLimsMessageFactory bettaLimsMessageFactory,
                LabEventFactory labEventFactory, LabEventHandler labEventHandler, StaticPlate shearingCleanupPlate,
                String shearCleanPlateBarcode, StaticPlate shearingPlate) {
            this.workflowDescription = workflowDescription;
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
            this.shearingCleanupPlate = shearingCleanupPlate;
            this.shearCleanPlateBarcode = shearCleanPlateBarcode;
            this.shearingPlate = shearingPlate;
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

        public LibraryConstruction invoke() {
            LibraryConstructionJaxb libraryConstructionJaxb = new LibraryConstructionJaxb(bettaLimsMessageFactory, "",
                    shearCleanPlateBarcode).invoke();
            pondRegRackBarcode = libraryConstructionJaxb.getPondRegRackBarcode();
            pondRegTubeBarcodes = libraryConstructionJaxb.getPondRegTubeBarcodes();

            // EndRepair
            validateWorkflow(workflowDescription, "EndRepair", shearingCleanupPlate);
            LabEvent endRepairEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(libraryConstructionJaxb.getEndRepairJaxb(), shearingCleanupPlate);
            labEventHandler.processEvent(endRepairEntity, null);

            // EndRepairCleanup
            validateWorkflow(workflowDescription, "EndRepairCleanup", shearingCleanupPlate);
            LabEvent endRepairCleanupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(libraryConstructionJaxb.getEndRepairCleanupJaxb(), shearingCleanupPlate);
            labEventHandler.processEvent(endRepairCleanupEntity, null);

            // ABase
            validateWorkflow(workflowDescription, "ABase", shearingCleanupPlate);
            LabEvent aBaseEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(libraryConstructionJaxb.getaBaseJaxb(), shearingCleanupPlate);
            labEventHandler.processEvent(aBaseEntity, null);

            // ABaseCleanup
            validateWorkflow(workflowDescription, "ABaseCleanup", shearingCleanupPlate);
            LabEvent aBaseCleanupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(libraryConstructionJaxb.getaBaseCleanupJaxb(), shearingCleanupPlate);
            labEventHandler.processEvent(aBaseCleanupEntity, null);

            // IndexedAdapterLigation
            validateWorkflow(workflowDescription, "IndexedAdapterLigation", shearingCleanupPlate);
            BuildIndexPlate buildIndexPlate = new BuildIndexPlate(libraryConstructionJaxb.getIndexPlateBarcode()).invoke();
            StaticPlate indexPlate = buildIndexPlate.getIndexPlate();
            MolecularIndexReagent index301 = buildIndexPlate.getIndex301();
            LabEvent indexedAdapterLigationEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    libraryConstructionJaxb.getIndexedAdapterLigationJaxb(), indexPlate, shearingCleanupPlate);
            labEventHandler.processEvent(indexedAdapterLigationEntity, null);
            // asserts
            Set<SampleInstance> postIndexingSampleInstances = shearingCleanupPlate.getVesselContainer().getSampleInstancesAtPosition("A01");
            PlateWell plateWellA1PostIndex = shearingCleanupPlate.getVesselContainer().getVesselAtPosition("A01");
            Assert.assertEquals(plateWellA1PostIndex.getAppliedReagents().iterator().next(), index301, "Wrong reagent");
            SampleInstance sampleInstance = postIndexingSampleInstances.iterator().next();
            Assert.assertEquals(sampleInstance.getMolecularState().getMolecularEnvelope().get3PrimeAttachment().getAppendageName(),
                    "tagged_301", "Wrong index");

            // AdapterLigationCleanup
            validateWorkflow(workflowDescription, "AdapterLigationCleanup", shearingCleanupPlate);
            LabEvent ligationCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    libraryConstructionJaxb.getLigationCleanupJaxb(), shearingPlate, null);
            labEventHandler.processEvent(ligationCleanupEntity, null);
            StaticPlate ligationCleanupPlate = (StaticPlate) ligationCleanupEntity.getTargetLabVessels().iterator().next();

            // PondEnrichment
            validateWorkflow(workflowDescription, "PondEnrichment", ligationCleanupPlate);
            LabEvent pondEnrichmentEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(libraryConstructionJaxb.getPondEnrichmentJaxb(), shearingCleanupPlate);
            labEventHandler.processEvent(pondEnrichmentEntity, null);

            // HybSelPondEnrichmentCleanup
            validateWorkflow(workflowDescription, "HybSelPondEnrichmentCleanup", shearingCleanupPlate);
            LabEvent pondCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    libraryConstructionJaxb.getPondCleanupJaxb(), shearingPlate, null);
            labEventHandler.processEvent(pondCleanupEntity, null);
            StaticPlate pondCleanupPlate = (StaticPlate) pondCleanupEntity.getTargetLabVessels().iterator().next();

            // PondRegistration
            validateWorkflow(workflowDescription, "PondRegistration", pondCleanupPlate);
            Map<String, TwoDBarcodedTube> mapBarcodeToPondRegTube = new HashMap<String, TwoDBarcodedTube>();
            LabEvent pondRegistrationEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(
                    libraryConstructionJaxb.getPondRegistrationJaxb(), shearingCleanupPlate, mapBarcodeToPondRegTube);
            labEventHandler.processEvent(pondRegistrationEntity, null);
            // asserts
            pondRegRack = (RackOfTubes) pondRegistrationEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(pondRegRack.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");
            Set<SampleInstance> sampleInstancesInPondRegWell = pondRegRack.getVesselContainer().getSampleInstancesAtPosition("A08");
            Assert.assertEquals(sampleInstancesInPondRegWell.size(), 1, "Wrong number of sample instances in position");
            Assert.assertEquals(sampleInstancesInPondRegWell.iterator().next().getStartingSample().getSampleName(), "SM-8", "Wrong sample");
            return this;
        }
    }

    public static class BuildIndexPlate {
        private String indexPlateBarcode;
        private StaticPlate indexPlate;
        private MolecularIndexReagent index301;

        public BuildIndexPlate(String indexPlateBarcode) {
            this.indexPlateBarcode = indexPlateBarcode;
        }

        public StaticPlate getIndexPlate() {
            return indexPlate;
        }

        public MolecularIndexReagent getIndex301() {
            return index301;
        }

        public BuildIndexPlate invoke() {
            indexPlate = new StaticPlate(indexPlateBarcode);
            PlateWell plateWellA01 = new PlateWell(indexPlate, VesselPosition.A01);
            index301 = new MolecularIndexReagent(new IndexEnvelope("ATCGATCG", null, "tagged_301"));
            plateWellA01.addReagent(index301);
            indexPlate.getVesselContainer().addContainedVessel(plateWellA01, "A01");
            PlateWell plateWellA02 = new PlateWell(indexPlate, VesselPosition.A02);
            IndexEnvelope index502 = new IndexEnvelope("TCGATCGA", null, "tagged_502");
            plateWellA02.addReagent(new MolecularIndexReagent(index502));
            indexPlate.getVesselContainer().addContainedVessel(plateWellA02, "A02");
            return this;
        }
    }

    public static class LibraryConstructionJaxb {
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final String testPrefix;
        private final String shearCleanPlateBarcode;

        private String indexPlateBarcode;
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

        public LibraryConstructionJaxb(BettaLimsMessageFactory bettaLimsMessageFactory, String testPrefix, String shearCleanPlateBarcode) {
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.testPrefix = testPrefix;
            this.shearCleanPlateBarcode = shearCleanPlateBarcode;
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

        public LibraryConstructionJaxb invoke() {
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

            indexPlateBarcode = "IndexPlate" + testPrefix;
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
                    "HybSelPondEnrichmentCleanup", shearCleanPlateBarcode, pondCleanupBarcode);
            BettaLIMSMessage bettaLIMSMessage7 = new BettaLIMSMessage();
            bettaLIMSMessage7.getPlateTransferEvent().add(pondCleanupJaxb);
            messageList.add(bettaLIMSMessage7);

            pondRegRackBarcode = "PondReg" + testPrefix;
            pondRegTubeBarcodes = new ArrayList<String>();
            for(int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK; rackPosition++) {
                pondRegTubeBarcodes.add("PondReg" + testPrefix + rackPosition);
            }
            pondRegistrationJaxb = bettaLimsMessageFactory.buildPlateToRack(
                    "PondRegistration", pondCleanupBarcode, pondRegRackBarcode, pondRegTubeBarcodes);
            BettaLIMSMessage bettaLIMSMessage8 = new BettaLIMSMessage();
            bettaLIMSMessage8.getPlateTransferEvent().add(pondRegistrationJaxb);
            messageList.add(bettaLIMSMessage8);

            return this;
        }
    }

    private class HybridSelection {
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

        private HybridSelection(WorkflowDescription workflowDescription, BettaLimsMessageFactory bettaLimsMessageFactory,
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

        public HybridSelection invoke() {
            HybridSelectionJaxb hybridSelectionJaxb = new HybridSelectionJaxb(bettaLimsMessageFactory, "", pondRegRackBarcode,
                    pondRegTubeBarcodes).invoke();
            normCatchRackBarcode = hybridSelectionJaxb.getNormCatchRackBarcode();
            normCatchBarcodes = hybridSelectionJaxb.getNormCatchBarcodes();

            // PreSelectionPool
            validateWorkflow(workflowDescription, "PreSelectionPool", pondRegRack); //todo jmt should be mapBarcodeToPondRegTube.values());
            Map<String, TwoDBarcodedTube> mapBarcodeToPreSelPoolTube = new HashMap<String, TwoDBarcodedTube>();
            LabEvent preSelPoolEntity = labEventFactory.buildFromBettaLimsRackToRackDbFree(hybridSelectionJaxb.getPreSelPoolJaxb(),
                    pondRegRack, mapBarcodeToPreSelPoolTube);
            labEventHandler.processEvent(preSelPoolEntity, null);
            RackOfTubes preSelPoolRack = (RackOfTubes) preSelPoolEntity.getTargetLabVessels().iterator().next();
            LabEvent preSelPoolEntity2 = labEventFactory.buildFromBettaLimsRackToRackDbFree(hybridSelectionJaxb.getPreSelPoolJaxb2(),
                    pondRegRack, preSelPoolRack);
            labEventHandler.processEvent(preSelPoolEntity2, null);
            //asserts
            Assert.assertEquals(preSelPoolRack.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");
            Set<SampleInstance> sampleInstancesInPreSelPoolWell = preSelPoolRack.getVesselContainer().getSampleInstancesAtPosition("A08");
            Assert.assertEquals(sampleInstancesInPreSelPoolWell.size(), 2, "Wrong number of sample instances in position");

            // Hybridization
            validateWorkflow(workflowDescription, "Hybridization", preSelPoolRack);
            LabEvent hybridizationEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(hybridSelectionJaxb.getHybridizationJaxb(), preSelPoolRack, null);
            labEventHandler.processEvent(hybridizationEntity, null);
            StaticPlate hybridizationPlate = (StaticPlate) hybridizationEntity.getTargetLabVessels().iterator().next();

            // BaitSetup
            TwoDBarcodedTube baitTube = buildBaitTube(hybridSelectionJaxb.getBaitTubeBarcode());
            LabEvent baitSetupEntity = labEventFactory.buildVesselToSectionDbFree(hybridSelectionJaxb.getBaitSetupJaxb(), baitTube, null, "ALL96");
            labEventHandler.processEvent(baitSetupEntity, null);
            StaticPlate baitSetupPlate = (StaticPlate) baitSetupEntity.getTargetLabVessels().iterator().next();

            // BaitAddition
            validateWorkflow(workflowDescription, "BaitAddition", hybridizationPlate);
            LabEvent baitAdditionEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(hybridSelectionJaxb.getBaitAdditionJaxb(), baitSetupPlate, hybridizationPlate);
            labEventHandler.processEvent(baitAdditionEntity, null);

            // BeadAddition
            validateWorkflow(workflowDescription, "BeadAddition", hybridizationPlate);
            LabEvent beadAdditionEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(hybridSelectionJaxb.getBeadAdditionJaxb(), hybridizationPlate);
            labEventHandler.processEvent(beadAdditionEntity, null);

            // APWash
            validateWorkflow(workflowDescription, "APWash", hybridizationPlate);
            LabEvent apWashEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(hybridSelectionJaxb.getApWashJaxb(), hybridizationPlate);
            labEventHandler.processEvent(apWashEntity, null);

            // GSWash1
            validateWorkflow(workflowDescription, "GSWash1", hybridizationPlate);
            LabEvent gsWash1Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(hybridSelectionJaxb.getGsWash1Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash1Entity, null);

            // GSWash2
            validateWorkflow(workflowDescription, "GSWash2", hybridizationPlate);
            LabEvent gsWash2Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(hybridSelectionJaxb.getGsWash2Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash2Entity, null);

            // CatchEnrichmentSetup
            validateWorkflow(workflowDescription, "CatchEnrichmentSetup", hybridizationPlate);
            LabEvent catchEnrichmentSetupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(hybridSelectionJaxb.getCatchEnrichmentSetupJaxb(), hybridizationPlate);
            labEventHandler.processEvent(catchEnrichmentSetupEntity, null);

            // CatchEnrichmentCleanup
            validateWorkflow(workflowDescription, "CatchEnrichmentCleanup", hybridizationPlate);
            LabEvent catchEnrichmentCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    hybridSelectionJaxb.getCatchEnrichmentCleanupJaxb(), hybridizationPlate, null);
            labEventHandler.processEvent(catchEnrichmentCleanupEntity, null);

            // NormalizedCatchRegistration
            validateWorkflow(workflowDescription, "NormalizedCatchRegistration", hybridizationPlate);
            mapBarcodeToNormCatchTubes = new HashMap<String, TwoDBarcodedTube>();
            LabEvent normCatchEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(hybridSelectionJaxb.getNormCatchJaxb(), hybridizationPlate,
                    mapBarcodeToNormCatchTubes);
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

    public static class HybridSelectionJaxb {
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

        public HybridSelectionJaxb(BettaLimsMessageFactory bettaLimsMessageFactory, String testPrefix, String pondRegRackBarcode,
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

        public HybridSelectionJaxb invoke() {
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
            for(int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK / 2; rackPosition++) {
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

    private static class Qtp {
        private final WorkflowDescription workflowDescription;
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;
        private final RackOfTubes normCatchRack;
        private final String normCatchRackBarcode;
        private final List<String> normCatchBarcodes;
        private final Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes;

        private Qtp(WorkflowDescription workflowDescription, BettaLimsMessageFactory bettaLimsMessageFactory,
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
            QtpJaxb qtpJaxb = new QtpJaxb(bettaLimsMessageFactory, "", normCatchBarcodes, normCatchRackBarcode).invoke();
            PlateCherryPickEvent cherryPickJaxb = qtpJaxb.getCherryPickJaxb();
            final String poolRackBarcode = qtpJaxb.getPoolRackBarcode();
            PlateCherryPickEvent denatureJaxb = qtpJaxb.getDenatureJaxb();
            final String denatureRackBarcode = qtpJaxb.getDenatureRackBarcode();
            PlateCherryPickEvent stripTubeTransferJaxb = qtpJaxb.getStripTubeTransferJaxb();
            final String stripTubeHolderBarcode = qtpJaxb.getStripTubeHolderBarcode();
            PlateTransferEventType flowcellTransferJaxb = qtpJaxb.getFlowcellTransferJaxb();

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
            Set<SampleInstance> pooledSampleInstances = poolingRack.getVesselContainer().getSampleInstancesAtPosition("A01");
            Assert.assertEquals(pooledSampleInstances.size(), NUM_POSITIONS_IN_RACK, "Wrong number of pooled samples");

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
            final RackOfTubes denatureRack = (RackOfTubes) denatureEntity.getTargetLabVessels().iterator().next();
            Set<SampleInstance> denaturedSampleInstances = denatureRack.getVesselContainer().getSampleInstancesAtPosition("A01");
            Assert.assertEquals(denaturedSampleInstances.size(), NUM_POSITIONS_IN_RACK, "Wrong number of denatured samples");

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
            Assert.assertEquals(stripTube.getVesselContainer().getSampleInstancesAtPosition("1").size(), NUM_POSITIONS_IN_RACK,
                    "Wrong number of samples in strip tube well");

            // FlowcellTransfer
            validateWorkflow(workflowDescription, "FlowcellTransfer", stripTube);
            LabEvent flowcellTransferEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(flowcellTransferJaxb, stripTube, null);
            labEventHandler.processEvent(flowcellTransferEntity, null);
            //asserts
            IlluminaFlowcell illuminaFlowcell = (IlluminaFlowcell) flowcellTransferEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(illuminaFlowcell.getVesselContainer().getSampleInstancesAtPosition("1").size(), NUM_POSITIONS_IN_RACK,
                    "Wrong number of samples in flowcell lane");
        }
    }

    public static class QtpJaxb {
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

        public QtpJaxb(BettaLimsMessageFactory bettaLimsMessageFactory, String testPrefix, List<String> normCatchBarcodes,
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

        public QtpJaxb invoke() {
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
