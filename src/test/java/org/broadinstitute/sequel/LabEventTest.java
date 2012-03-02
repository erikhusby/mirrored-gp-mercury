package org.broadinstitute.sequel;

import org.broadinstitute.sequel.bettalims.jaxb.PlateTransferEventType;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.project.BasicProject;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.reagent.IndexEnvelope;
import org.broadinstitute.sequel.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheetImpl;
import org.broadinstitute.sequel.entity.vessel.PlateWell;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.entity.vessel.WellName;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Test messaging
 */
public class LabEventTest {
    public static final int NUM_POSITIONS_IN_RACK = 96;

    @Test
    public void testHybridSelection() {
        // Hybrid selection transfers
        TestUtilities testUtilities = new TestUtilities();
        Project project = new BasicProject("LabEventTesting", testUtilities.createMockJiraTicket());
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for(int rackPosition = 1; rackPosition <= NUM_POSITIONS_IN_RACK; rackPosition++) {
            SampleSheetImpl sampleSheet = new SampleSheetImpl();
            sampleSheet.addStartingSample(new BSPSample("SM-" + rackPosition, project));
            String barcode = "R" + rackPosition;
            mapBarcodeToTube.put(barcode, new TwoDBarcodedTube(barcode, sampleSheet));
        }

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setPersonDAO(new PersonDAO());
        LabEventHandler labEventHandler = new LabEventHandler();

        PlateTransferEventType shearingTransferEventJaxb = bettaLimsMessageFactory.buildRackToPlate(
                new ArrayList<String>(mapBarcodeToTube.keySet()), "ShearingTransfer", "KioskRack", "ShearPlate");
        LabEvent shearingTransferEventEntity = labEventFactory.buildFromBettaLimsDbFree(
                shearingTransferEventJaxb, mapBarcodeToTube, null);
        labEventHandler.processEvent(shearingTransferEventEntity);

        StaticPlate shearingPlate = (StaticPlate) shearingTransferEventEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(shearingPlate.getSampleInstances().size(),
                NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

        PlateTransferEventType postShearingTransferCleanupEventJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                "PostShearingTransferCleanup", "ShearPlate", "ShearCleanPlate");
        LabEvent postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLimsDbFree(
                postShearingTransferCleanupEventJaxb, shearingPlate, null);
        labEventHandler.processEvent(postShearingTransferCleanupEntity);

        StaticPlate shearingCleanupPlate = (StaticPlate) postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(shearingCleanupPlate.getSampleInstances().size(),
                NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");
        Set<SampleInstance> sampleInstancesInWell = shearingCleanupPlate.getSampleInstancesInWell("A08");
        Assert.assertEquals(sampleInstancesInWell.size(), 1, "Wrong number of sample instances in well");
        Assert.assertEquals(sampleInstancesInWell.iterator().next().getStartingSample().getSampleName(), "SM-8", "Wrong sample");

        PlateTransferEventType indexedAdapterLigationJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                "IndexedAdapterLigation", "IndexPlate", "ShearCleanPlate");
        StaticPlate indexPlate = new StaticPlate("IndexPlate");
        PlateWell plateWell = new PlateWell(indexPlate, new WellName("A01"));
        MolecularIndexReagent index301 = new MolecularIndexReagent(new IndexEnvelope("ATCGATCG", null, "tagged_301"));
        plateWell.addReagent(index301);
        indexPlate.addWell(plateWell, "A01");
        plateWell = new PlateWell(indexPlate, new WellName("A02"));
        IndexEnvelope index502 = new IndexEnvelope("TCGATCGA", null, "tagged_502");
        plateWell.addReagent(new MolecularIndexReagent(index502));
        indexPlate.addWell(plateWell, "A02");
        LabEvent indexedAdapterLigationEntity = labEventFactory.buildFromBettaLimsDbFree(
                indexedAdapterLigationJaxb, indexPlate, shearingCleanupPlate);
        labEventHandler.processEvent(indexedAdapterLigationEntity);

        Set<SampleInstance> postIndexingSampleInstances = shearingCleanupPlate.getSampleInstancesInWell("A01");
        PlateWell plateWellA1PostIndex = shearingCleanupPlate.getWellAtPosition("A01");
        Assert.assertEquals(plateWellA1PostIndex.getAppliedReagents().iterator().next(), index301, "Wrong reagent");
        SampleInstance sampleInstance = postIndexingSampleInstances.iterator().next();
        Assert.assertEquals(sampleInstance.getMolecularState().getMolecularEnvelope().get3PrimeAttachment().getAppendageName(), "tagged_301", "Wrong index");
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
