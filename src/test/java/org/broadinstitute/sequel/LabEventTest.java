package org.broadinstitute.sequel;

//import com.jprofiler.api.agent.Controller;
import org.broadinstitute.sequel.bettalims.jaxb.PlateCherryPickEvent;
import org.broadinstitute.sequel.bettalims.jaxb.PlateEventType;
import org.broadinstitute.sequel.bettalims.jaxb.PlateTransferEventType;
import org.broadinstitute.sequel.bettalims.jaxb.ReceptaclePlateTransferEvent;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.control.workflow.WorkflowParser;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.BasicProject;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.reagent.GenericReagent;
import org.broadinstitute.sequel.entity.run.IlluminaFlowcell;
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
import java.util.Collection;
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
                Thread.currentThread().getContextClassLoader().getResourceAsStream("HybridSelection.bpmn"));
        workflowDescription.setStartState(workflowParser.getStartState());
        workflowDescription.setMapNameToTransitionList(workflowParser.getMapNameToTransitionList());

        // starting rack
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
        validateWorkflow(workflowDescription, "ShearingTransfer", mapBarcodeToTube.values());
        String shearPlateBarcode = "ShearPlate";
        PlateTransferEventType shearingTransferEventJaxb = bettaLimsMessageFactory.buildRackToPlate(
                "ShearingTransfer", "KioskRack", new ArrayList<String>(mapBarcodeToTube.keySet()), shearPlateBarcode);
        // for each vessel, get most recent event, check whether it's a predecessor to the proposed event
        LabEvent shearingTransferEventEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                shearingTransferEventJaxb, mapBarcodeToTube, null);
        labEventHandler.processEvent(shearingTransferEventEntity, null);
        // asserts
        final StaticPlate shearingPlate = (StaticPlate) shearingTransferEventEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(shearingPlate.getSampleInstances().size(),
                NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

        // PostShearingTransferCleanup
        validateWorkflow(workflowDescription, "PostShearingTransferCleanup", shearingPlate);
        String shearCleanPlateBarcode = "ShearCleanPlate";
        PlateTransferEventType postShearingTransferCleanupEventJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                "PostShearingTransferCleanup", shearPlateBarcode, shearCleanPlateBarcode);
        LabEvent postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                postShearingTransferCleanupEventJaxb, shearingPlate, null);
        labEventHandler.processEvent(postShearingTransferCleanupEntity, null);
        // asserts
        final StaticPlate shearingCleanupPlate = (StaticPlate) postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(shearingCleanupPlate.getSampleInstances().size(),
                NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");
        Set<SampleInstance> sampleInstancesInWell = shearingCleanupPlate.getVesselContainer().getSampleInstancesAtPosition("A08");
        Assert.assertEquals(sampleInstancesInWell.size(), 1, "Wrong number of sample instances in well");
        Assert.assertEquals(sampleInstancesInWell.iterator().next().getStartingSample().getSampleName(), "SM-8", "Wrong sample");

        // ShearingQC
        validateWorkflow(workflowDescription, "ShearingQC", shearingCleanupPlate);
        String shearQcPlateBarcode = "ShearQcPlate";
        PlateTransferEventType shearingQcEventJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                "ShearingQC", shearCleanPlateBarcode, shearQcPlateBarcode);
        LabEvent shearingQcEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                shearingQcEventJaxb, shearingCleanupPlate, null);
        labEventHandler.processEvent(shearingQcEntity);

        // EndRepair
        validateWorkflow(workflowDescription, "EndRepair", shearingCleanupPlate);
        PlateEventType endRepairJaxb = bettaLimsMessageFactory.buildPlateEvent("EndRepair", shearPlateBarcode);
        LabEvent endRepairEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(endRepairJaxb, shearingCleanupPlate);
        labEventHandler.processEvent(endRepairEntity);

        // EndRepairCleanup
        validateWorkflow(workflowDescription, "EndRepairCleanup", shearingCleanupPlate);
        PlateEventType endRepairCleanupJaxb = bettaLimsMessageFactory.buildPlateEvent("EndRepairCleanup", shearPlateBarcode);
        LabEvent endRepairCleanupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(endRepairCleanupJaxb, shearingCleanupPlate);
        labEventHandler.processEvent(endRepairCleanupEntity);

        // ABase
        validateWorkflow(workflowDescription, "ABase", shearingCleanupPlate);
        PlateEventType aBaseJaxb = bettaLimsMessageFactory.buildPlateEvent("ABase", shearPlateBarcode);
        LabEvent aBaseEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(aBaseJaxb, shearingCleanupPlate);
        labEventHandler.processEvent(aBaseEntity);

        // ABaseCleanup
        validateWorkflow(workflowDescription, "ABaseCleanup", shearingCleanupPlate);
        PlateEventType aBaseCleanupJaxb = bettaLimsMessageFactory.buildPlateEvent("ABaseCleanup", shearPlateBarcode);
        LabEvent aBaseCleanupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(aBaseCleanupJaxb, shearingCleanupPlate);
        labEventHandler.processEvent(aBaseCleanupEntity);

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
        labEventHandler.processEvent(ligationCleanupEntity);
        StaticPlate ligationCleanupPlate = (StaticPlate) ligationCleanupEntity.getTargetLabVessels().iterator().next();

        // PondEnrichment
        validateWorkflow(workflowDescription, "PondEnrichment", ligationCleanupPlate);
        PlateEventType pondEnrichmentJaxb = bettaLimsMessageFactory.buildPlateEvent("PondEnrichment", shearPlateBarcode);
        LabEvent pondEnrichmentEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(pondEnrichmentJaxb, shearingCleanupPlate);
        labEventHandler.processEvent(pondEnrichmentEntity);

        // HybSelPondEnrichmentCleanup
        validateWorkflow(workflowDescription, "HybSelPondEnrichmentCleanup", shearingCleanupPlate);
        String pondCleanupBarcode = "pondCleanupPlate";
        PlateTransferEventType pondCleanupJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                "HybSelPondEnrichmentCleanup", shearPlateBarcode, pondCleanupBarcode);
        LabEvent pondCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                pondCleanupJaxb, shearingPlate, null);
        labEventHandler.processEvent(pondCleanupEntity);
        StaticPlate pondCleanupPlate = (StaticPlate) pondCleanupEntity.getTargetLabVessels().iterator().next();

        // PondRegistration
        validateWorkflow(workflowDescription, "PondRegistration", pondCleanupPlate);
        List<String> pondRegTubeBarcodes = new ArrayList<String>();
        for(int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK; rackPosition++) {
            pondRegTubeBarcodes.add("PondReg" + rackPosition);
        }
        String pondRegRackBarcode = "PondReg";
        PlateTransferEventType pondRegistrationJaxb = bettaLimsMessageFactory.buildPlateToRack(
                "PondRegistration", pondCleanupBarcode, pondRegRackBarcode, pondRegTubeBarcodes);
        Map<String, TwoDBarcodedTube> mapBarcodeToPondRegTube = new HashMap<String, TwoDBarcodedTube>();
        LabEvent pondRegistrationEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(
                pondRegistrationJaxb, shearingCleanupPlate, mapBarcodeToPondRegTube);
        labEventHandler.processEvent(pondRegistrationEntity, null);
        // asserts
        RackOfTubes pondRegRack = (RackOfTubes) pondRegistrationEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(pondRegRack.getSampleInstances().size(),
                NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");
        Set<SampleInstance> sampleInstancesInPondRegWell = pondRegRack.getVesselContainer().getSampleInstancesAtPosition("A08");
        Assert.assertEquals(sampleInstancesInPondRegWell.size(), 1, "Wrong number of sample instances in position");
        Assert.assertEquals(sampleInstancesInPondRegWell.iterator().next().getStartingSample().getSampleName(), "SM-8", "Wrong sample");

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
        labEventHandler.processEvent(beadAdditionEntity);

        // APWash
        validateWorkflow(workflowDescription, "APWash", hybridizationPlate);
        PlateEventType apWashJaxb = bettaLimsMessageFactory.buildPlateEvent("APWash", hybridizationPlateBarcode);
        LabEvent apWashEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(apWashJaxb, hybridizationPlate);
        labEventHandler.processEvent(apWashEntity);

        // GSWash1
        validateWorkflow(workflowDescription, "GSWash1", hybridizationPlate);
        PlateEventType gsWash1Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash1", hybridizationPlateBarcode);
        LabEvent gsWash1Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(gsWash1Jaxb, hybridizationPlate);
        labEventHandler.processEvent(gsWash1Entity);

        // GSWash2
        validateWorkflow(workflowDescription, "GSWash2", hybridizationPlate);
        PlateEventType gsWash2Jaxb = bettaLimsMessageFactory.buildPlateEvent("GSWash2", hybridizationPlateBarcode);
        LabEvent gsWash2Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(gsWash2Jaxb, hybridizationPlate);
        labEventHandler.processEvent(gsWash2Entity);

        // CatchEnrichmentSetup
        validateWorkflow(workflowDescription, "CatchEnrichmentSetup", hybridizationPlate);
        PlateEventType catchEnrichmentSetupJaxb = bettaLimsMessageFactory.buildPlateEvent("CatchEnrichmentSetup", hybridizationPlateBarcode);
        LabEvent catchEnrichmentSetupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(catchEnrichmentSetupJaxb, hybridizationPlate);
        labEventHandler.processEvent(catchEnrichmentSetupEntity);

        // CatchEnrichmentCleanup
        validateWorkflow(workflowDescription, "CatchEnrichmentCleanup", hybridizationPlate);
        String catchCleanupBarcode = "catchCleanPlate";
        PlateTransferEventType catchEnrichmentCleanupJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                "CatchEnrichmentCleanup", hybridizationPlateBarcode, catchCleanupBarcode);
        LabEvent catchEnrichmentCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                catchEnrichmentCleanupJaxb, hybridizationPlate, null);
        labEventHandler.processEvent(catchEnrichmentCleanupEntity);

        // NormalizedCatchRegistration
        validateWorkflow(workflowDescription, "NormalizedCatchRegistration", hybridizationPlate);
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
        labEventHandler.processEvent(normCatchEntity, null);
        final RackOfTubes normCatchRack = (RackOfTubes) normCatchEntity.getTargetLabVessels().iterator().next();

        // PoolingTransfer
        validateWorkflow(workflowDescription, "PoolingTransfer", normCatchRack);
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
                new HashMap<String, VesselContainer>() {{
                    put(poolRackBarcode, poolingRack.getVesselContainer());
                }},
                mapBarcodeToPoolTube,
                new HashMap<String, VesselContainer>() {{
                    put(denatureRackBarcode, null);
                }}, mapBarcodeToDenatureTube
        );
        labEventHandler.processEvent(denatureEntity);
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
                new HashMap<String, VesselContainer>() {{
                    put(denatureRackBarcode, denatureRack.getVesselContainer());
                }},
                mapBarcodeToDenatureTube,
                new HashMap<String, VesselContainer>() {{
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
}
