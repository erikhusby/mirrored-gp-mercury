package org.broadinstitute.sequel;

//import com.jprofiler.api.agent.Controller;
import org.broadinstitute.sequel.bettalims.jaxb.PlateCherryPickEvent;
import org.broadinstitute.sequel.bettalims.jaxb.PlateTransferEventType;
import org.broadinstitute.sequel.bettalims.jaxb.ReceptaclePlateTransferEvent;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.BasicProject;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.reagent.GenericReagent;
import org.broadinstitute.sequel.entity.run.IlluminaFlowcell;
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
import org.broadinstitute.sequel.entity.sample.SampleSheetImpl;
import org.broadinstitute.sequel.entity.vessel.PlateWell;
import org.broadinstitute.sequel.entity.vessel.RackOfTubes;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.entity.vessel.WellName;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
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

        // starting rack
        Map<LabEventName,PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();
        Project project = new BasicProject("LabEventTesting", new JiraTicket(new DummyJiraService(),"TP-0","0"));
        ProjectPlan projectPlan = new ProjectPlan(project,"To test hybrid selection",new WorkflowDescription("HS","8.0",billableEvents, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for(int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK; rackPosition++) {
            SampleSheetImpl sampleSheet = new SampleSheetImpl();
            sampleSheet.addStartingSample(new BSPSample("SM-" + rackPosition, projectPlan, null));
            String barcode = "R" + rackPosition;
            mapBarcodeToTube.put(barcode, new TwoDBarcodedTube(barcode, sampleSheet));
        }

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setPersonDAO(new PersonDAO());
        LabEventHandler labEventHandler = new LabEventHandler();

        // ShearingTransfer
        String shearPlateBarcode = "ShearPlate";
        PlateTransferEventType shearingTransferEventJaxb = bettaLimsMessageFactory.buildRackToPlate(
                "ShearingTransfer", "KioskRack", new ArrayList<String>(mapBarcodeToTube.keySet()), shearPlateBarcode);
        LabEvent shearingTransferEventEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                shearingTransferEventJaxb, mapBarcodeToTube, null);
        labEventHandler.processEvent(shearingTransferEventEntity);
        // asserts
        StaticPlate shearingPlate = (StaticPlate) shearingTransferEventEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(shearingPlate.getSampleInstances().size(),
                NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

        // PostShearingTransferCleanup
        String shearCleanPlateBarcode = "ShearCleanPlate";
        PlateTransferEventType postShearingTransferCleanupEventJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                "PostShearingTransferCleanup", shearPlateBarcode, shearCleanPlateBarcode);
        LabEvent postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                postShearingTransferCleanupEventJaxb, shearingPlate, null);
        labEventHandler.processEvent(postShearingTransferCleanupEntity);
        // asserts
        StaticPlate shearingCleanupPlate = (StaticPlate) postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(shearingCleanupPlate.getSampleInstances().size(),
                NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");
        Set<SampleInstance> sampleInstancesInWell = shearingCleanupPlate.getVesselContainer().getSampleInstancesAtPosition("A08");
        Assert.assertEquals(sampleInstancesInWell.size(), 1, "Wrong number of sample instances in well");
        Assert.assertEquals(sampleInstancesInWell.iterator().next().getStartingSample().getSampleName(), "SM-8", "Wrong sample");

        // IndexedAdapterLigation
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
        labEventHandler.processEvent(indexedAdapterLigationEntity);
        // asserts
        Set<SampleInstance> postIndexingSampleInstances = shearingCleanupPlate.getVesselContainer().getSampleInstancesAtPosition("A01");
        PlateWell plateWellA1PostIndex = shearingCleanupPlate.getVesselContainer().getVesselAtPosition("A01");
        Assert.assertEquals(plateWellA1PostIndex.getAppliedReagents().iterator().next(), index301, "Wrong reagent");
        SampleInstance sampleInstance = postIndexingSampleInstances.iterator().next();
        Assert.assertEquals(sampleInstance.getMolecularState().getMolecularEnvelope().get3PrimeAttachment().getAppendageName(),
                "tagged_301", "Wrong index");

        // PondRegistration
        List<String> pondRegTubeBarcodes = new ArrayList<String>();
        for(int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK; rackPosition++) {
            pondRegTubeBarcodes.add("PondReg" + rackPosition);
        }
        String pondRegRackBarcode = "PondReg";
        PlateTransferEventType pondRegistrationJaxb = bettaLimsMessageFactory.buildPlateToRack(
                "PondRegistration", shearCleanPlateBarcode, pondRegRackBarcode, pondRegTubeBarcodes);
        Map<String, TwoDBarcodedTube> mapBarcodeToPondRegTube = new HashMap<String, TwoDBarcodedTube>();
        LabEvent pondRegistrationEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(
                pondRegistrationJaxb, shearingCleanupPlate, mapBarcodeToPondRegTube);
        labEventHandler.processEvent(pondRegistrationEntity);
        // asserts
        RackOfTubes pondRegRack = (RackOfTubes) pondRegistrationEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(pondRegRack.getSampleInstances().size(),
                NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");
        Set<SampleInstance> sampleInstancesInPondRegWell = pondRegRack.getVesselContainer().getSampleInstancesAtPosition("A08");
        Assert.assertEquals(sampleInstancesInPondRegWell.size(), 1, "Wrong number of sample instances in position");
        Assert.assertEquals(sampleInstancesInPondRegWell.iterator().next().getStartingSample().getSampleName(), "SM-8", "Wrong sample");

        // PreSelectionPool
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
        labEventHandler.processEvent(preSelPoolEntity);
        RackOfTubes preSelPoolRack = (RackOfTubes) preSelPoolEntity.getTargetLabVessels().iterator().next();
        PlateTransferEventType preSelPoolJaxb2 = bettaLimsMessageFactory.buildRackToRack("PreSelectionPool", pondRegRackBarcode,
                pondRegTubeBarcodes.subList(NUM_POSITIONS_IN_RACK / 2, NUM_POSITIONS_IN_RACK), preSelPoolRackBarcode, preSelPoolBarcodes);
        LabEvent preSelPoolEntity2 = labEventFactory.buildFromBettaLimsRackToRackDbFree(preSelPoolJaxb2,
                pondRegRack, preSelPoolRack);
        labEventHandler.processEvent(preSelPoolEntity2);
        //asserts
        Assert.assertEquals(preSelPoolRack.getSampleInstances().size(),
                NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");
        Set<SampleInstance> sampleInstancesInPreSelPoolWell = preSelPoolRack.getVesselContainer().getSampleInstancesAtPosition("A08");
        Assert.assertEquals(sampleInstancesInPreSelPoolWell.size(), 2, "Wrong number of sample instances in position");

        // Hybridization
        String hybridizationPlateBarcode = "Hybrid";
        PlateTransferEventType hybridizationJaxb = bettaLimsMessageFactory.buildRackToPlate(
                "Hybridization", preSelPoolRackBarcode, preSelPoolBarcodes, hybridizationPlateBarcode);
        LabEvent hybridizationEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(hybridizationJaxb, preSelPoolRack, null);
        labEventHandler.processEvent(hybridizationEntity);
        StaticPlate hybridizationPlate = (StaticPlate) hybridizationEntity.getTargetLabVessels().iterator().next();

        // BaitSetup
        String baitTubeBarcode = "Bait";
        String baitSetupBarcode = "BaitSetup";
        ReceptaclePlateTransferEvent baitSetupJaxb = bettaLimsMessageFactory.buildTubeToPlate("BaitSetup", baitTubeBarcode,
                baitSetupBarcode);
        TwoDBarcodedTube baitTube = new TwoDBarcodedTube(baitTubeBarcode);
        baitTube.addReagent(new GenericReagent("BaitSet", "xyz"));
        LabEvent baitSetupEntity = labEventFactory.buildVesselToSectionDbFree(baitSetupJaxb, baitTube, null, "ALL96");
        labEventHandler.processEvent(baitSetupEntity);
        StaticPlate baitSetupPlate = (StaticPlate) baitSetupEntity.getTargetLabVessels().iterator().next();

        // BaitAddition
        PlateTransferEventType baitAdditionJaxb = bettaLimsMessageFactory.buildPlateToPlate("BaitAddition", baitSetupBarcode,
                hybridizationPlateBarcode);
        LabEvent baitAdditionEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(baitAdditionJaxb, baitSetupPlate, hybridizationPlate);
        labEventHandler.processEvent(baitAdditionEntity);

        // NormalizedCatchRegistration
        List<String> normCatchBarcodes = new ArrayList<String>();
        for(int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK / 2; rackPosition++) {
            normCatchBarcodes.add("NormCatch" + rackPosition);
        }
        final String normCatchRackBarcode = "NormCatchRack";
        PlateTransferEventType normCatchJaxb = bettaLimsMessageFactory.buildPlateToRack(
                "NormalizedCatchRegistration", hybridizationPlateBarcode, normCatchRackBarcode, normCatchBarcodes);
        Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes = new HashMap<String, TwoDBarcodedTube>();
        LabEvent normCatchEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(normCatchJaxb, hybridizationPlate,
                mapBarcodeToNormCatchTubes);
        labEventHandler.processEvent(normCatchEntity);
        final RackOfTubes normCatchRack = (RackOfTubes) normCatchEntity.getTargetLabVessels().iterator().next();

        // PoolingTransfer
        final String poolRackBarcode = "PoolRack";
        List<BettaLimsMessageFactory.CherryPick> poolingCherryPicks = new ArrayList<BettaLimsMessageFactory.CherryPick>();
        List<String> poolTubeBarcodes = new ArrayList<String>();
        for(int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK / 2; rackPosition++) {
            poolingCherryPicks.add(new BettaLimsMessageFactory.CherryPick(normCatchRackBarcode,
                    bettaLimsMessageFactory.buildWellName(rackPosition), poolRackBarcode, "A01"));
            poolTubeBarcodes.add("Pool" + rackPosition);
        }
        PlateCherryPickEvent cherryPickJaxb = bettaLimsMessageFactory.buildCherryPick("PoolingTransfer",
                Arrays.asList(normCatchRackBarcode), Arrays.asList(normCatchBarcodes), poolRackBarcode, poolTubeBarcodes,
                poolingCherryPicks);
        Map<String, TwoDBarcodedTube> mapBarcodeToPoolTube = new HashMap<String, TwoDBarcodedTube>();
        LabEvent poolingEntity = labEventFactory.buildCherryPickRackToRackDbFree(cherryPickJaxb,
                new HashMap<String, VesselContainer>() {{
                    put(normCatchRackBarcode, normCatchRack.getVesselContainer());
                }},
                mapBarcodeToNormCatchTubes,
                new HashMap<String, VesselContainer>() {{
                    put(poolRackBarcode, null);
                }}, mapBarcodeToPoolTube
        );
        labEventHandler.processEvent(poolingEntity);
        // asserts
        final RackOfTubes poolingRack = (RackOfTubes) poolingEntity.getTargetLabVessels().iterator().next();
        Set<SampleInstance> pooledSampleInstances = poolingRack.getVesselContainer().getSampleInstancesAtPosition("A01");
        Assert.assertEquals(pooledSampleInstances.size(), NUM_POSITIONS_IN_RACK, "Wrong number of pooled samples");

        // StripTubeBTransfer
        final String stripTubeHolderBarcode = "StripTubeHolder";
        List<BettaLimsMessageFactory.CherryPick> stripTubeCherryPicks = new ArrayList<BettaLimsMessageFactory.CherryPick>();
        for(int rackPosition = 0; rackPosition < 8; rackPosition++) {
            stripTubeCherryPicks.add(new BettaLimsMessageFactory.CherryPick(poolRackBarcode,
                    Character.toString((char)('A' + rackPosition)) + "01", stripTubeHolderBarcode,
                    Character.toString((char)('A' + rackPosition)) + "01"));
        }
        String stripTubeBarcode = "StripTube1";
        PlateCherryPickEvent stripTubeTransferJaxb = bettaLimsMessageFactory.buildCherryPickToStripTube("StripTubeBTransfer",
                Arrays.asList(poolRackBarcode), Arrays.asList(poolTubeBarcodes),
                stripTubeHolderBarcode, Arrays.asList(stripTubeBarcode), stripTubeCherryPicks);
        Map<String, StripTube> mapBarcodeToStripTube = new HashMap<String, StripTube>();
        LabEvent stripTubeTransferEntity = labEventFactory.buildCherryPickRackToStripTubeDbFree(stripTubeTransferJaxb,
                new HashMap<String, VesselContainer>() {{
                    put(poolRackBarcode, poolingRack.getVesselContainer());
                }},
                mapBarcodeToPoolTube,
                new HashMap<String, VesselContainer>() {{
                    put(stripTubeHolderBarcode, null);
                }},
                mapBarcodeToStripTube
        );
        labEventHandler.processEvent(stripTubeTransferEntity);
        // asserts
        StripTube stripTube = (StripTube) stripTubeTransferEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(stripTube.getVesselContainer().getSampleInstancesAtPosition("1").size(), NUM_POSITIONS_IN_RACK,
                "Wrong number of samples in strip tube well");

        // FlowcellTransfer
        PlateTransferEventType flowcellTransferJaxb = bettaLimsMessageFactory.buildPlateToPlate("FlowcellTransfer", stripTubeBarcode, "Flowcell");
        LabEvent flowcellTransferEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(flowcellTransferJaxb, stripTube, null);
        labEventHandler.processEvent(flowcellTransferEntity);
        //asserts
        IlluminaFlowcell illuminaFlowcell = (IlluminaFlowcell) flowcellTransferEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(illuminaFlowcell.getVesselContainer().getSampleInstancesAtPosition("1").size(), NUM_POSITIONS_IN_RACK,
                "Wrong number of samples in flowcell lane");

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
}
