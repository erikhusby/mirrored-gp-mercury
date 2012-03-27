package org.broadinstitute.sequel;

//import com.jprofiler.api.agent.Controller;
import org.broadinstitute.sequel.bettalims.jaxb.PlateTransferEventType;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.entity.project.BasicProject;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

/**
 * Test messaging
 */
@SuppressWarnings("FeatureEnvy")
public class LabEventTest {
    public static final int NUM_POSITIONS_IN_RACK = 96;

    @Test(groups = {DATABASE_FREE})
    public void testHybridSelection() {
//        Controller.startCPURecording(true);

        // starting rack
        Project project = new BasicProject("LabEventTesting", new JiraTicket(new DummyJiraService(),"TP-0","0"));
        ProjectPlan projectPlan = new ProjectPlan(project,"To test hybrid selection",new WorkflowDescription("HS","8.0",null, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));
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
        Set<SampleInstance> sampleInstancesInWell = shearingCleanupPlate.getSampleInstancesInPosition("A08");
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
        Set<SampleInstance> postIndexingSampleInstances = shearingCleanupPlate.getSampleInstancesInPosition("A01");
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
        Set<SampleInstance> sampleInstancesInPondRegWell = pondRegRack.getSampleInstancesInPosition("A08");
        Assert.assertEquals(sampleInstancesInPondRegWell.size(), 1, "Wrong number of sample instances in position");
        Assert.assertEquals(sampleInstancesInPondRegWell.iterator().next().getStartingSample().getSampleName(), "SM-8", "Wrong sample");

        // PreSelectionPool
        List<String> preSelPoolBarcodes = new ArrayList<String>();
        for(int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK / 2; rackPosition++) {
            preSelPoolBarcodes.add("PreSelPool" + rackPosition);
        }
        Map<String, TwoDBarcodedTube> mapBarcodeToPreSelPoolTube = new HashMap<String, TwoDBarcodedTube>();
        PlateTransferEventType preSelPoolJaxb = bettaLimsMessageFactory.buildRackToRack("PreSelectionPool",
                pondRegRackBarcode, pondRegTubeBarcodes.subList(0, NUM_POSITIONS_IN_RACK / 2), "PreSelPool", preSelPoolBarcodes);
        LabEvent preSelPoolEntity = labEventFactory.buildFromBettaLimsRackToRackDbFree(preSelPoolJaxb,
                pondRegRack, mapBarcodeToPreSelPoolTube);
        labEventHandler.processEvent(preSelPoolEntity);
        preSelPoolJaxb = bettaLimsMessageFactory.buildRackToRack("PreSelectionPool", pondRegRackBarcode,
                pondRegTubeBarcodes.subList(NUM_POSITIONS_IN_RACK / 2, NUM_POSITIONS_IN_RACK), "PreSelPool", preSelPoolBarcodes);
        // todo jmt, should this be one event?
        preSelPoolEntity = labEventFactory.buildFromBettaLimsRackToRackDbFree(preSelPoolJaxb,
                pondRegRack, mapBarcodeToPreSelPoolTube);
        labEventHandler.processEvent(preSelPoolEntity);
        //asserts
        RackOfTubes preSelPoolRack = (RackOfTubes) preSelPoolEntity.getTargetLabVessels().iterator().next();
//        Assert.assertEquals(preSelPoolRack.getSampleInstances().size(),
//                NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");
//        Set<SampleInstance> sampleInstancesInPreSelPoolWell = preSelPoolRack.getSampleInstancesInPosition("A08");
//        Assert.assertEquals(sampleInstancesInPreSelPoolWell.size(), 2, "Wrong number of sample instances in position");

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

        // PreflightNormalization rack event
        // deck calls web services
        // ShearingTransfer rack to plate
        // EndRepair plate event with reagent
        // IndexedAdapterLigation plate to plate
        // BaitSetup tube to plate
        // BaitAddition plate to plate
        // NormalizedCatchRegistration plate to rack
        // PoolingTransfer cherry pick
        // StripTubeBTransfer
        // FlowcellTransfer

    }
}
