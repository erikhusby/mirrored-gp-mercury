package org.broadinstitute.sequel;

//import com.jprofiler.api.agent.Controller;
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
import org.broadinstitute.sequel.entity.vessel.VesselContainer;
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
import org.broadinstitute.sequel.entity.vessel.WellName;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

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
                labEventFactory, labEventHandler, shearingCleanupPlate, shearPlateBarcode, shearCleanPlateBarcode, shearingPlate).invoke();
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
                labEventFactory, labEventHandler, shearingCleanupPlate, shearPlateBarcode, shearCleanPlateBarcode, shearingPlate).invoke();
        RackOfTubes pondRegRack = libraryConstruction.getPondRegRack();
        String pondRegRackBarcode = libraryConstruction.getPondRegRackBarcode();
        List<String> pondRegTubeBarcodes = libraryConstruction.getPondRegTubeBarcodes();

        List<String> sageUnloadTubeBarcodes = new ArrayList<String>();
        for(int i = 1; i <= NUM_POSITIONS_IN_RACK; i++) {
            sageUnloadTubeBarcodes.add("SageUnload" + i);
        }
        String sageUnloadBarcode = "SageUnload";
        Map<String, TwoDBarcodedTube> mapBarcodeToSageUnloadTubes = new HashMap<String, TwoDBarcodedTube>();
        RackOfTubes sageUnloadRack = null;
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
            sageUnloadRack = (RackOfTubes) sageUnloadEntity.getTargetLabVessels().iterator().next();
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

    private void validateWorkflow(WorkflowDescription workflowDescription, String nextEventTypeName, Collection<? extends LabVessel> tubes) {
        List<LabVessel> labVessels = new ArrayList<LabVessel>(tubes);
        validateWorkflow(workflowDescription, nextEventTypeName, labVessels);
    }

    private void validateWorkflow(WorkflowDescription workflowDescription, String nextEventTypeName, LabVessel labVessel) {
        List<LabVessel> labVessels = new ArrayList<LabVessel>();
        labVessels.add(labVessel);
        validateWorkflow(workflowDescription, nextEventTypeName, labVessels);
    }

    private void validateWorkflow(WorkflowDescription workflowDescription, String nextEventTypeName, List<LabVessel> labVessels) {
        List<String> errors = workflowDescription.validate(labVessels, nextEventTypeName);
        if(!errors.isEmpty()) {
            Assert.fail(errors.get(0));
        }
    }

    private class Shearing {
        private final WorkflowDescription workflowDescription;
        private final Map<String, TwoDBarcodedTube> mapBarcodeToTube;
        private final BettaLimsMessageFactory bettaLimsMessageFactory;
        private final LabEventFactory labEventFactory;
        private final LabEventHandler labEventHandler;
        private String shearPlateBarcode;
        private StaticPlate shearingPlate;
        private String shearCleanPlateBarcode;
        private StaticPlate shearingCleanupPlate;

        public Shearing(WorkflowDescription workflowDescription, Map<String, TwoDBarcodedTube> mapBarcodeToTube, BettaLimsMessageFactory bettaLimsMessageFactory, LabEventFactory labEventFactory, LabEventHandler labEventHandler) {
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
            PlateTransferEventType shearingTransferEventJaxb = shearingJaxb.getShearingTransferEventJaxb();
            PlateTransferEventType postShearingTransferCleanupEventJaxb = shearingJaxb.getPostShearingTransferCleanupEventJaxb();
            PlateTransferEventType shearingQcEventJaxb = shearingJaxb.getShearingQcEventJaxb();
            this.shearPlateBarcode = shearingJaxb.getShearPlateBarcode();
            this.shearCleanPlateBarcode = shearingJaxb.getShearCleanPlateBarcode();

            // ShearingTransfer
            validateWorkflow(workflowDescription, "ShearingTransfer", mapBarcodeToTube.values());
            LabEvent shearingTransferEventEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    shearingTransferEventJaxb, mapBarcodeToTube, null);
            labEventHandler.processEvent(shearingTransferEventEntity, null);
            // asserts
            shearingPlate = (StaticPlate) shearingTransferEventEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(shearingPlate.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

            // PostShearingTransferCleanup
            validateWorkflow(workflowDescription, "PostShearingTransferCleanup", shearingPlate);
            LabEvent postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    postShearingTransferCleanupEventJaxb, shearingPlate, null);
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
                    shearingQcEventJaxb, shearingCleanupPlate, null);
            labEventHandler.processEvent(shearingQcEntity, null);
            return this;
        }

    }

    public static class ShearingJaxb {
        private BettaLimsMessageFactory bettaLimsMessageFactory;
        private List<String> tubeBarcodeList;
        private String testPrefix;
        private String shearPlateBarcode;
        private String shearCleanPlateBarcode;

        private PlateTransferEventType shearingTransferEventJaxb;
        private PlateTransferEventType postShearingTransferCleanupEventJaxb;
        private PlateTransferEventType shearingQcEventJaxb;

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

        public ShearingJaxb invoke() {
            shearPlateBarcode = "ShearPlate" + testPrefix;
            shearingTransferEventJaxb = bettaLimsMessageFactory.buildRackToPlate(
                    "ShearingTransfer", "KioskRack" + testPrefix, tubeBarcodeList, shearPlateBarcode);
            shearCleanPlateBarcode = "ShearCleanPlate" + testPrefix;
            postShearingTransferCleanupEventJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PostShearingTransferCleanup", shearPlateBarcode, shearCleanPlateBarcode);
            String shearQcPlateBarcode = "ShearQcPlate" + testPrefix;
            shearingQcEventJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "ShearingQC", shearCleanPlateBarcode, shearQcPlateBarcode);
            return this;
        }
    }

    private class LibraryConstruction {
        private WorkflowDescription workflowDescription;
        private BettaLimsMessageFactory bettaLimsMessageFactory;
        private LabEventFactory labEventFactory;
        private LabEventHandler labEventHandler;
        private StaticPlate shearingCleanupPlate;
        private String shearPlateBarcode;
        private String shearCleanPlateBarcode;
        private StaticPlate shearingPlate;
        private List<String> pondRegTubeBarcodes;
        private String pondRegRackBarcode;
        private RackOfTubes pondRegRack;

        public LibraryConstruction(WorkflowDescription workflowDescription, BettaLimsMessageFactory bettaLimsMessageFactory, LabEventFactory labEventFactory, LabEventHandler labEventHandler, StaticPlate shearingCleanupPlate, String shearPlateBarcode, String shearCleanPlateBarcode, StaticPlate shearingPlate) {
            this.workflowDescription = workflowDescription;
            this.bettaLimsMessageFactory = bettaLimsMessageFactory;
            this.labEventFactory = labEventFactory;
            this.labEventHandler = labEventHandler;
            this.shearingCleanupPlate = shearingCleanupPlate;
            this.shearPlateBarcode = shearPlateBarcode;
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
            // EndRepair
            validateWorkflow(workflowDescription, "EndRepair", shearingCleanupPlate);
            PlateEventType endRepairJaxb = bettaLimsMessageFactory.buildPlateEvent("EndRepair", shearPlateBarcode);
            LabEvent endRepairEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(endRepairJaxb, shearingCleanupPlate);
            labEventHandler.processEvent(endRepairEntity, null);

            // EndRepairCleanup
            validateWorkflow(workflowDescription, "EndRepairCleanup", shearingCleanupPlate);
            PlateEventType endRepairCleanupJaxb = bettaLimsMessageFactory.buildPlateEvent("EndRepairCleanup", shearPlateBarcode);
            LabEvent endRepairCleanupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(endRepairCleanupJaxb, shearingCleanupPlate);
            labEventHandler.processEvent(endRepairCleanupEntity, null);

            // ABase
            validateWorkflow(workflowDescription, "ABase", shearingCleanupPlate);
            PlateEventType aBaseJaxb = bettaLimsMessageFactory.buildPlateEvent("ABase", shearPlateBarcode);
            LabEvent aBaseEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(aBaseJaxb, shearingCleanupPlate);
            labEventHandler.processEvent(aBaseEntity, null);

            // ABaseCleanup
            validateWorkflow(workflowDescription, "ABaseCleanup", shearingCleanupPlate);
            PlateEventType aBaseCleanupJaxb = bettaLimsMessageFactory.buildPlateEvent("ABaseCleanup", shearPlateBarcode);
            LabEvent aBaseCleanupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(aBaseCleanupJaxb, shearingCleanupPlate);
            labEventHandler.processEvent(aBaseCleanupEntity, null);

            // IndexedAdapterLigation
            validateWorkflow(workflowDescription, "IndexedAdapterLigation", shearingCleanupPlate);
            String indexPlateBarcode = "IndexPlate";
            PlateTransferEventType indexedAdapterLigationJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "IndexedAdapterLigation", indexPlateBarcode, shearCleanPlateBarcode);
            StaticPlate indexPlate = new StaticPlate(indexPlateBarcode);
            PlateWell plateWellA01 = new PlateWell(indexPlate, new WellName("A01"));
            MolecularIndexReagent index301 = new MolecularIndexReagent(new IndexEnvelope("ATCGATCG", null, "tagged_301"));
            plateWellA01.addReagent(index301);
            indexPlate.getVesselContainer().addContainedVessel(plateWellA01, "A01");
            PlateWell plateWellA02 = new PlateWell(indexPlate, new WellName("A02"));
            IndexEnvelope index502 = new IndexEnvelope("TCGATCGA", null, "tagged_502");
            plateWellA02.addReagent(new MolecularIndexReagent(index502));
            indexPlate.getVesselContainer().addContainedVessel(plateWellA02, "A02");
            LabEvent indexedAdapterLigationEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    indexedAdapterLigationJaxb, indexPlate, shearingCleanupPlate);
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
            String ligationCleanupBarcode = "ligationCleanupPlate";
            PlateTransferEventType ligationCleanupJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "AdapterLigationCleanup", shearPlateBarcode, ligationCleanupBarcode);
            LabEvent ligationCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    ligationCleanupJaxb, shearingPlate, null);
            labEventHandler.processEvent(ligationCleanupEntity, null);
            StaticPlate ligationCleanupPlate = (StaticPlate) ligationCleanupEntity.getTargetLabVessels().iterator().next();

            // PondEnrichment
            validateWorkflow(workflowDescription, "PondEnrichment", ligationCleanupPlate);
            PlateEventType pondEnrichmentJaxb = bettaLimsMessageFactory.buildPlateEvent("PondEnrichment", shearPlateBarcode);
            LabEvent pondEnrichmentEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(pondEnrichmentJaxb, shearingCleanupPlate);
            labEventHandler.processEvent(pondEnrichmentEntity, null);

            // HybSelPondEnrichmentCleanup
            validateWorkflow(workflowDescription, "HybSelPondEnrichmentCleanup", shearingCleanupPlate);
            String pondCleanupBarcode = "pondCleanupPlate";
            PlateTransferEventType pondCleanupJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "HybSelPondEnrichmentCleanup", shearPlateBarcode, pondCleanupBarcode);
            LabEvent pondCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    pondCleanupJaxb, shearingPlate, null);
            labEventHandler.processEvent(pondCleanupEntity, null);
            StaticPlate pondCleanupPlate = (StaticPlate) pondCleanupEntity.getTargetLabVessels().iterator().next();

            // PondRegistration
            validateWorkflow(workflowDescription, "PondRegistration", pondCleanupPlate);
            pondRegTubeBarcodes = new ArrayList<String>();
            for(int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK; rackPosition++) {
                pondRegTubeBarcodes.add("PondReg" + rackPosition);
            }
            pondRegRackBarcode = "PondReg";
            PlateTransferEventType pondRegistrationJaxb = bettaLimsMessageFactory.buildPlateToRack(
                    "PondRegistration", pondCleanupBarcode, pondRegRackBarcode, pondRegTubeBarcodes);
            Map<String, TwoDBarcodedTube> mapBarcodeToPondRegTube = new HashMap<String, TwoDBarcodedTube>();
            LabEvent pondRegistrationEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(
                    pondRegistrationJaxb, shearingCleanupPlate, mapBarcodeToPondRegTube);
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

    private class HybridSelection {
        private WorkflowDescription workflowDescription;
        private BettaLimsMessageFactory bettaLimsMessageFactory;
        private LabEventFactory labEventFactory;
        private LabEventHandler labEventHandler;
        private RackOfTubes pondRegRack;
        private String pondRegRackBarcode;
        private List<String> pondRegTubeBarcodes;
        private List<String> normCatchBarcodes;
        private String normCatchRackBarcode;
        private Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes;
        private RackOfTubes normCatchRack;

        public HybridSelection(WorkflowDescription workflowDescription, BettaLimsMessageFactory bettaLimsMessageFactory, LabEventFactory labEventFactory, LabEventHandler labEventHandler, RackOfTubes pondRegRack, String pondRegRackBarcode, List<String> pondRegTubeBarcodes) {
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
            // PreSelectionPool
            validateWorkflow(workflowDescription, "PreSelectionPool", pondRegRack); //todo jmt should be mapBarcodeToPondRegTube.values());
            List<String> preSelPoolBarcodes = new ArrayList<String>();
            for(int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK / 2; rackPosition++) {
                preSelPoolBarcodes.add("PreSelPool" + rackPosition);
            }
            Map<String, TwoDBarcodedTube> mapBarcodeToPreSelPoolTube = new HashMap<String, TwoDBarcodedTube>();
            String preSelPoolRackBarcode = "PreSelPool";
            PlateTransferEventType preSelPoolJaxb = bettaLimsMessageFactory.buildRackToRack("PreSelectionPool",
                    pondRegRackBarcode, pondRegTubeBarcodes.subList(0, NUM_POSITIONS_IN_RACK / 2), preSelPoolRackBarcode, preSelPoolBarcodes);
            LabEvent preSelPoolEntity = labEventFactory.buildFromBettaLimsRackToRackDbFree(preSelPoolJaxb,
                    pondRegRack, mapBarcodeToPreSelPoolTube);
            labEventHandler.processEvent(preSelPoolEntity, null);
            RackOfTubes preSelPoolRack = (RackOfTubes) preSelPoolEntity.getTargetLabVessels().iterator().next();
            PlateTransferEventType preSelPoolJaxb2 = bettaLimsMessageFactory.buildRackToRack("PreSelectionPool", pondRegRackBarcode,
                    pondRegTubeBarcodes.subList(NUM_POSITIONS_IN_RACK / 2, NUM_POSITIONS_IN_RACK), preSelPoolRackBarcode, preSelPoolBarcodes);
            LabEvent preSelPoolEntity2 = labEventFactory.buildFromBettaLimsRackToRackDbFree(preSelPoolJaxb2,
                    pondRegRack, preSelPoolRack);
            labEventHandler.processEvent(preSelPoolEntity2, null);
            //asserts
            Assert.assertEquals(preSelPoolRack.getSampleInstances().size(),
                    NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");
            Set<SampleInstance> sampleInstancesInPreSelPoolWell = preSelPoolRack.getVesselContainer().getSampleInstancesAtPosition("A08");
            Assert.assertEquals(sampleInstancesInPreSelPoolWell.size(), 2, "Wrong number of sample instances in position");

            // Hybridization
            validateWorkflow(workflowDescription, "Hybridization", preSelPoolRack);
            String hybridizationPlateBarcode = "Hybrid";
            PlateTransferEventType hybridizationJaxb = bettaLimsMessageFactory.buildRackToPlate(
                    "Hybridization", preSelPoolRackBarcode, preSelPoolBarcodes, hybridizationPlateBarcode);
            LabEvent hybridizationEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(hybridizationJaxb, preSelPoolRack, null);
            labEventHandler.processEvent(hybridizationEntity, null);
            StaticPlate hybridizationPlate = (StaticPlate) hybridizationEntity.getTargetLabVessels().iterator().next();

            // BaitSetup
            String baitTubeBarcode = "Bait";
            String baitSetupBarcode = "BaitSetup";
            ReceptaclePlateTransferEvent baitSetupJaxb = bettaLimsMessageFactory.buildTubeToPlate("BaitSetup", baitTubeBarcode,
                    baitSetupBarcode);
            TwoDBarcodedTube baitTube = new TwoDBarcodedTube(baitTubeBarcode);
            baitTube.addReagent(new GenericReagent("BaitSet", "xyz"));
            LabEvent baitSetupEntity = labEventFactory.buildVesselToSectionDbFree(baitSetupJaxb, baitTube, null, "ALL96");
            labEventHandler.processEvent(baitSetupEntity, null);
            StaticPlate baitSetupPlate = (StaticPlate) baitSetupEntity.getTargetLabVessels().iterator().next();

            // BaitAddition
            validateWorkflow(workflowDescription, "BaitAddition", hybridizationPlate);
            PlateTransferEventType baitAdditionJaxb = bettaLimsMessageFactory.buildPlateToPlate("BaitAddition", baitSetupBarcode,
                    hybridizationPlateBarcode);
            LabEvent baitAdditionEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(baitAdditionJaxb, baitSetupPlate, hybridizationPlate);
            labEventHandler.processEvent(baitAdditionEntity, null);

            // BeadAddition
            validateWorkflow(workflowDescription, "BeadAddition", hybridizationPlate);
            PlateEventType beadAdditionJaxb = bettaLimsMessageFactory.buildPlateEvent("BeadAddition", hybridizationPlateBarcode);
            LabEvent beadAdditionEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(beadAdditionJaxb, hybridizationPlate);
            labEventHandler.processEvent(beadAdditionEntity, null);

            // APWash
            validateWorkflow(workflowDescription, "APWash", hybridizationPlate);
            PlateEventType apWashJaxb = bettaLimsMessageFactory.buildPlateEvent("APWash", hybridizationPlateBarcode);
            LabEvent apWashEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(apWashJaxb, hybridizationPlate);
            labEventHandler.processEvent(apWashEntity, null);

            // GSWash1
            validateWorkflow(workflowDescription, "GSWash1", hybridizationPlate);
            PlateEventType gsWash1Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash1", hybridizationPlateBarcode);
            LabEvent gsWash1Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(gsWash1Jaxb, hybridizationPlate);
            labEventHandler.processEvent(gsWash1Entity, null);

            // GSWash2
            validateWorkflow(workflowDescription, "GSWash2", hybridizationPlate);
            PlateEventType gsWash2Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash2", hybridizationPlateBarcode);
            LabEvent gsWash2Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(gsWash2Jaxb, hybridizationPlate);
            labEventHandler.processEvent(gsWash2Entity, null);

            // CatchEnrichmentSetup
            validateWorkflow(workflowDescription, "CatchEnrichmentSetup", hybridizationPlate);
            PlateEventType catchEnrichmentSetupJaxb = bettaLimsMessageFactory.buildPlateEvent("CatchEnrichmentSetup", hybridizationPlateBarcode);
            LabEvent catchEnrichmentSetupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(catchEnrichmentSetupJaxb, hybridizationPlate);
            labEventHandler.processEvent(catchEnrichmentSetupEntity, null);

            // CatchEnrichmentCleanup
            validateWorkflow(workflowDescription, "CatchEnrichmentCleanup", hybridizationPlate);
            String catchCleanupBarcode = "catchCleanPlate";
            PlateTransferEventType catchEnrichmentCleanupJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "CatchEnrichmentCleanup", hybridizationPlateBarcode, catchCleanupBarcode);
            LabEvent catchEnrichmentCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    catchEnrichmentCleanupJaxb, hybridizationPlate, null);
            labEventHandler.processEvent(catchEnrichmentCleanupEntity, null);

            // NormalizedCatchRegistration
            validateWorkflow(workflowDescription, "NormalizedCatchRegistration", hybridizationPlate);
            normCatchBarcodes = new ArrayList<String>();
            for(int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK / 2; rackPosition++) {
                normCatchBarcodes.add("NormCatch" + rackPosition);
            }
            normCatchRackBarcode = "NormCatchRack";
            PlateTransferEventType normCatchJaxb = bettaLimsMessageFactory.buildPlateToRack(
                    "NormalizedCatchRegistration", hybridizationPlateBarcode, normCatchRackBarcode, normCatchBarcodes);
            mapBarcodeToNormCatchTubes = new HashMap<String, TwoDBarcodedTube>();
            LabEvent normCatchEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(normCatchJaxb, hybridizationPlate,
                    mapBarcodeToNormCatchTubes);
            labEventHandler.processEvent(normCatchEntity, null);
            normCatchRack = (RackOfTubes) normCatchEntity.getTargetLabVessels().iterator().next();
            return this;
        }
    }

    private class Qtp {
        private WorkflowDescription workflowDescription;
        private BettaLimsMessageFactory bettaLimsMessageFactory;
        private LabEventFactory labEventFactory;
        private LabEventHandler labEventHandler;
        private final RackOfTubes normCatchRack;
        private final String normCatchRackBarcode;
        private List<String> normCatchBarcodes;
        private Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes;

        public Qtp(WorkflowDescription workflowDescription, BettaLimsMessageFactory bettaLimsMessageFactory, LabEventFactory labEventFactory, LabEventHandler labEventHandler, RackOfTubes normCatchRack, String normCatchRackBarcode, List<String> normCatchBarcodes, Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes) {
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
            // PoolingTransfer
            validateWorkflow(workflowDescription, "PoolingTransfer", normCatchRack);
            final String poolRackBarcode = "PoolRack";
            List<BettaLimsMessageFactory.CherryPick> poolingCherryPicks = new ArrayList<BettaLimsMessageFactory.CherryPick>();
            List<String> poolTubeBarcodes = new ArrayList<String>();
            for(int rackPosition = 1; rackPosition <= normCatchBarcodes.size(); rackPosition++) {
                poolingCherryPicks.add(new BettaLimsMessageFactory.CherryPick(normCatchRackBarcode,
                        bettaLimsMessageFactory.buildWellName(rackPosition), poolRackBarcode, "A01"));
                poolTubeBarcodes.add("Pool" + rackPosition);
            }
            PlateCherryPickEvent cherryPickJaxb = bettaLimsMessageFactory.buildCherryPick("PoolingTransfer",
                    Arrays.asList(normCatchRackBarcode), Arrays.asList(normCatchBarcodes), poolRackBarcode, poolTubeBarcodes,
                    poolingCherryPicks);
            Map<String, TwoDBarcodedTube> mapBarcodeToPoolTube = new HashMap<String, TwoDBarcodedTube>();
            LabEvent poolingEntity = labEventFactory.buildCherryPickRackToRackDbFree(cherryPickJaxb,
                    new HashMap<String, VesselContainer<?>>() {{
                        put(normCatchRackBarcode, normCatchRack.getVesselContainer());
                    }},
                    mapBarcodeToNormCatchTubes,
                    new HashMap<String, VesselContainer<?>>() {{
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
            final String denatureRackBarcode = "DenatureRack";
            List<BettaLimsMessageFactory.CherryPick> denatureCherryPicks = new ArrayList<BettaLimsMessageFactory.CherryPick>();
            List<String> denatureTubeBarcodes = new ArrayList<String>();
            denatureCherryPicks.add(new BettaLimsMessageFactory.CherryPick(poolRackBarcode,
                    "A01", denatureRackBarcode, "A01"));
            denatureTubeBarcodes.add("DenatureTube1");
            PlateCherryPickEvent denatureJaxb = bettaLimsMessageFactory.buildCherryPick("DenatureTransfer",
                    Arrays.asList(poolRackBarcode), Arrays.asList(poolTubeBarcodes), denatureRackBarcode, denatureTubeBarcodes,
                    denatureCherryPicks);
            Map<String, TwoDBarcodedTube> mapBarcodeToDenatureTube = new HashMap<String, TwoDBarcodedTube>();
            LabEvent denatureEntity = labEventFactory.buildCherryPickRackToRackDbFree(denatureJaxb,
                    new HashMap<String, VesselContainer<?>>() {{
                        put(poolRackBarcode, poolingRack.getVesselContainer());
                    }},
                    mapBarcodeToPoolTube,
                    new HashMap<String, VesselContainer<?>>() {{
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
            final String stripTubeHolderBarcode = "StripTubeHolder";
            List<BettaLimsMessageFactory.CherryPick> stripTubeCherryPicks = new ArrayList<BettaLimsMessageFactory.CherryPick>();
            for(int rackPosition = 0; rackPosition < 8; rackPosition++) {
                stripTubeCherryPicks.add(new BettaLimsMessageFactory.CherryPick(denatureRackBarcode,
                        "A01", stripTubeHolderBarcode,
                        Character.toString((char)('A' + rackPosition)) + "01"));
            }
            String stripTubeBarcode = "StripTube1";
            PlateCherryPickEvent stripTubeTransferJaxb = bettaLimsMessageFactory.buildCherryPickToStripTube("StripTubeBTransfer",
                    Arrays.asList(denatureRackBarcode), Arrays.asList(denatureTubeBarcodes),
                    stripTubeHolderBarcode, Arrays.asList(stripTubeBarcode), stripTubeCherryPicks);
            Map<String, StripTube> mapBarcodeToStripTube = new HashMap<String, StripTube>();
            LabEvent stripTubeTransferEntity = labEventFactory.buildCherryPickRackToStripTubeDbFree(stripTubeTransferJaxb,
                    new HashMap<String, VesselContainer<?>>() {{
                        put(denatureRackBarcode, denatureRack.getVesselContainer());
                    }},
                    mapBarcodeToDenatureTube,
                    new HashMap<String, VesselContainer<?>>() {{
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
            PlateTransferEventType flowcellTransferJaxb = bettaLimsMessageFactory.buildPlateToPlate("FlowcellTransfer", stripTubeBarcode, "Flowcell");
            LabEvent flowcellTransferEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(flowcellTransferJaxb, stripTube, null);
            labEventHandler.processEvent(flowcellTransferEntity, null);
            //asserts
            IlluminaFlowcell illuminaFlowcell = (IlluminaFlowcell) flowcellTransferEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(illuminaFlowcell.getVesselContainer().getSampleInstancesAtPosition("1").size(), NUM_POSITIONS_IN_RACK,
                    "Wrong number of samples in flowcell lane");
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
