package org.broadinstitute.sequel;


import org.broadinstitute.sequel.bettalims.jaxb.PlateTransferEventType;
import org.broadinstitute.sequel.control.bsp.AliquotReceiver;
import org.broadinstitute.sequel.control.bsp.MockBSPConnector;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.control.dao.vessel.LabVesselDAO;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingResponse;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.project.BasicProject;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.queue.AliquotParameters;
import org.broadinstitute.sequel.entity.queue.BSPAliquotWorkQueue;
import org.broadinstitute.sequel.entity.run.RunCartridge;
import org.broadinstitute.sequel.entity.run.RunChamber;
import org.broadinstitute.sequel.entity.run.SequencingRun;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.sample.SampleSheetImpl;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.MolecularEnvelope;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.entity.workflow.WorkflowDescription;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class EndToEndTest  {

    //@Inject
    LabVesselDAO labVesselDAO;
    
    //@Inject
    LabEventHandler handler;

    private JiraTicket createMockJiraTicket() {
        JiraTicket ticket = EasyMock.createMock(JiraTicket.class);
        EasyMock.expect(ticket.addComment((String)EasyMock.anyObject())).andReturn(JiraTicket.JiraResponse.OK).atLeastOnce();
        EasyMock.replay(ticket);
        return ticket;
    }

    private LabVessel createBSPStock(String sampleName,String tubeBarcode,Project project) {
        SampleSheet sampleSheet = new SampleSheetImpl();
        // this seems redundant: we're adding a sample sheet with only the stock
        // name itself.  More often we'll expect to see pre-pooled "samples",
        // in which case the BSP stock id will actually have multiple
        // component collaborator samples.
        sampleSheet.addStartingSample(new BSPSample(sampleName, project,null));
        return new TwoDBarcodedTube(tubeBarcode,sampleSheet);
    }
    
    private LabVessel createBSPAliquot(String aliquotName,String tubeBarcode,Project project) {
        // yowza, it's the same code!
        return createBSPStock(aliquotName,tubeBarcode,project);
    }
    
    @Test(groups = {"DatabaseFree"})
    public void doIt() {

        String masterSample1 = "master sample1";
        String masterSample2 = "master sample2";
        String aliquot1Label = "aliquot1";
        String aliquot2Label = "aliquot2";
        final WorkflowDescription workflow = new WorkflowDescription("Hybrid Selection", "7.0");
        Project project = new BasicProject("Project1",createMockJiraTicket());
        Project project2 = new BasicProject("Project2",createMockJiraTicket());

        LabVessel stock1 = createBSPStock(masterSample1,"00001234",project);
        LabVessel stock2 = createBSPStock(masterSample2,"00005678",project2);

        BSPAliquotWorkQueue aliquotWorkQueue = new BSPAliquotWorkQueue(new MockBSPConnector());

        Assert.assertTrue(project.getAllStarters().isEmpty());
        Assert.assertTrue(project2.getAllStarters().isEmpty());
        // add a sample to the project
        project.addStarter(stock1);
        project2.addStarter(stock2);

        Assert.assertTrue(project.getAllStarters().contains(stock1));
        Assert.assertTrue(project2.getAllStarters().contains(stock2));
        Assert.assertFalse(project.getAllStarters().contains(stock2));


        // load up the bsp aliquot queue
        project.addWorkQueue(aliquotWorkQueue); // when a project is defined,
                                                // a user gesture would add the aliquot queue to the project
        
        // request an aliquot from bsp
        AliquotParameters aliquotParameters = new AliquotParameters(project,0.9f,0.6f);
        AliquotParameters aliquotParameters2 = new AliquotParameters(project2,1.9f,2.6f);
        
        aliquotWorkQueue.add(stock1,aliquotParameters);
        aliquotWorkQueue.add(stock2,aliquotParameters2);

        BSPPlatingResponse platingResponse = aliquotWorkQueue.sendBatch();

        Assert.assertFalse(project.getPendingPlatingRequests().isEmpty());
        Assert.assertEquals(1,project.getPendingPlatingRequests().size());
        Assert.assertEquals(1,project2.getPendingPlatingRequests().size());

        BSPPlatingReceipt project1PlatingReceipt = project.getPendingPlatingRequests().iterator().next().getReceipt();
        BSPPlatingReceipt project2PlatingReceipt = project2.getPendingPlatingRequests().iterator().next().getReceipt();

        // both samples went into the same plating request; they should
        // have the same receipt.
        Assert.assertEquals(project1PlatingReceipt,project2PlatingReceipt);

        LabVessel aliquotTube = createBSPAliquot(aliquot1Label,aliquot1Label,null);
        LabVessel aliquot2Tube = createBSPAliquot(aliquot2Label,aliquot2Label,null);


        Assert.assertTrue(aliquotTube.getAllProjects().isEmpty()); // we just got the aliquot -- we don't know the project yet!
        Assert.assertTrue(aliquot2Tube.getAllProjects().isEmpty()); // we just got the aliquot -- we don't know the project yet!

        Assert.assertTrue(aliquot2Tube.getAllStatusNotes().isEmpty());
        BSPPlatingRequest platingRequest = new AliquotReceiver().receiveAliquot(stock1,aliquotTube,platingResponse.getReceipt());
        BSPPlatingRequest platingRequest2 = new AliquotReceiver().receiveAliquot(stock2,aliquot2Tube,platingResponse.getReceipt());

        Assert.assertTrue(project.getPendingPlatingRequests().isEmpty());
        Assert.assertTrue(project2.getPendingPlatingRequests().isEmpty());
        Assert.assertNotNull(platingRequest);
        Assert.assertFalse(aliquot2Tube.getAllProjects().isEmpty()); // after receiving the aliquot,
                                                                   // we should know the project
        Assert.assertEquals(1,aliquotTube.getAllProjects().size());
        Assert.assertEquals(1,aliquot2Tube.getAllProjects().size());

        // at this point, after receiving aliquots, {@link Project#getAllVessels()} should
        // give us both aliquots.  This will be a good test of the back pointer/authority
        // model


        Assert.assertEquals(aliquotParameters,platingRequest.getAliquotParameters());
        Assert.assertEquals(project,aliquotTube.getAllProjects().iterator().next());

        Assert.assertEquals(aliquotParameters2,platingRequest2.getAliquotParameters());
        Assert.assertEquals(project2,aliquot2Tube.getAllProjects().iterator().next());

        EasyMock.verify(project.getJiraTicket());

        Assert.assertFalse(aliquotTube.getAllStatusNotes().isEmpty());
        Assert.assertFalse(aliquot2Tube.getAllStatusNotes().isEmpty());

        for (StatusNote statusNote : aliquotTube.getAllStatusNotes()) {
            Assert.assertEquals(LabEventName.ALIQUOT_RECEIVED, statusNote.getEventName());
        }

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for(int rackPosition = 1; rackPosition <= 96; rackPosition++) {
            SampleSheetImpl sampleSheet = new SampleSheetImpl();
            sampleSheet.addStartingSample(new BSPSample("SM-" + rackPosition, project,null));
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
                96, "Wrong number of sample instances");

        PlateTransferEventType postShearingTransferCleanupEventJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                "PostShearingTransferCleanup", "ShearPlate", "ShearCleanPlate");
        LabEvent postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLimsDbFree(
                postShearingTransferCleanupEventJaxb, shearingPlate, null);
        labEventHandler.processEvent(postShearingTransferCleanupEntity);

        StaticPlate shearingCleanupPlate = (StaticPlate) postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(shearingCleanupPlate.getSampleInstances().size(),
                96, "Wrong number of sample instances");
        Set<SampleInstance> sampleInstancesInWell = shearingCleanupPlate.getSampleInstancesInWell("A8");
        Assert.assertEquals(sampleInstancesInWell.size(), 1, "Wrong number of sample instances in well");
        Assert.assertEquals(sampleInstancesInWell.iterator().next().getStartingSample().getSampleName(), "SM-8", "Wrong sample");

        PlateTransferEventType indexedAdapterLigationJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                "IndexedAdapterLigation", "IndexPlate", "ShearCleanPlate");
        
        StaticPlate indexPlate = new StaticPlate("IndexPlate");
        LabEvent indexedAdapterLigationEntity = labEventFactory.buildFromBettaLimsDbFree(
                postShearingTransferCleanupEventJaxb, indexPlate, shearingCleanupPlate);
        labEventHandler.processEvent(postShearingTransferCleanupEntity);

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

        /**
         * Todo arz: test {@link Goop#applyReagent(org.broadinstitute.sequel.entity.reagent.Reagent)} by applying
         * a reagent transfer.  Also do a test from a container that has
         * SampleSheets with samples in it to a LabVessel that contains
         * only {@link org.broadinstitute.sequel.entity.reagent.Reagent}, so test scenarios where we add samples
         * into reagent containers.
         */
        /*

        LabVessel firstDestination = new TwoDBarcodedTube(null,"tube1");
        LabVessel secondDestination = new TwoDBarcodedTube(null,"tube2");
        
        LabEvent transferEvent = new GenericLabEvent(false,true,null,null);

        // when aliquotReceiver.receiveAliquot() was called, it should have
        // created a LabVessel with label aliquot1Label.
        transferEvent.addSourceLabVessel(aliquotTube);
        transferEvent.addTargetLabVessel(firstDestination);
        PCRPrimerPairReagent pcrPrimerReagent = new PCRPrimerPairReagent();
        transferEvent.addReagent(pcrPrimerReagent);

        handler.processEvent(transferEvent);
        
        Assert.assertTrue(firstDestination.getGoop().getAppliedReagents().contains(pcrPrimerReagent));

        LabEvent transferEvent2 = new GenericLabEvent(false,true,null,null);
        transferEvent.addSourceLabVessel(firstDestination);
        transferEvent.addTargetLabVessel(secondDestination);

        handler.processEvent(transferEvent2);

        LabEvent reagentAdditionEvent = new GenericLabEvent(false,false,null,null);
        reagentAdditionEvent.addTargetLabVessel(secondDestination);
        MolecularEnvelope indexEnvelope = new IndexEnvelope("ATAT","CTGA","My Index");
        reagentAdditionEvent.addReagent(new MolecularIndexReagent(indexEnvelope));

        handler.processEvent(reagentAdditionEvent);
                
        
        // new up a flowcell and put the
        // new library on each lane
        IlluminaFlowcell flowcell = new IlluminaFlowcell(IlluminaFlowcell.FLOWCELL_TYPE.EIGHT_LANE,
                                                        flowcellBarcode,
                                                        new IlluminaRunConfiguration(76,true));
        for (int i = 1; i < 9; i++) {
            flowcell.addChamber(secondDestination,i++);
        }
        
        Person testUser = new Person("QADude");
        SequencingRun run = new IlluminaSequencingRun(flowcell,"Run1","000Run1","SLX-AB",testUser,false);
        checkForSampleProjectData(run, project, sample1,2,indexEnvelope); // we have three tubes (aliquot, 1st dest, and 2nd dest)
                                                            // but we should only have two sample sheets.  the first
                                                            // vessels (initial aliquot and 1st dest) should share
                                                            // the same sample sheet.  after adding the index,
                                                            // we should have branched and made a new sample sheet.
        */
    }

    private void checkForSampleProjectData(SequencingRun srun,
                                          Project p,
                                          StartingSample sam,
                                          int numberOfSampleSheetsPerSample,
                                          MolecularEnvelope expectedEnvelope) {
        boolean foundSample = false;
        boolean foundProject = false;
        boolean wasIndexFound = false;
        for (RunCartridge cartridge: srun.getSampleCartridge()) {
            for (RunChamber chamber: cartridge.getChambers()) {
                for (SampleInstance sampleInstance : chamber.getSampleInstances()) {
                    if (sam.equals(sampleInstance.getStartingSample())) {
                        foundSample = true;
                        MolecularEnvelope envelope = sampleInstance.getMolecularState().getMolecularEnvelope();

                        // sloppy check on the envelope: we're not checking position relative
                        // to other envelopes; just the presence of this envelope somewhere
                        if (expectedEnvelope.equals(envelope)) {
                            wasIndexFound = true;
                        }
                        while (envelope.getContainedEnvelope() != null) {
                            if (expectedEnvelope.equals(envelope.getContainedEnvelope())) {
                                wasIndexFound = true;
                            }
                        }


                        Project project = sampleInstance.getProject();
                        if (project.equals(p)) {
                            foundProject = true;
                        }
                    }
                    Set<SampleSheet> allSampleSheetsForAliquot = new HashSet<SampleSheet>();
                    if (allSampleSheetsForAliquot.size() != numberOfSampleSheetsPerSample) {
                        Assert.fail("Should have found exactly " + numberOfSampleSheetsPerSample + " sample sheets.  One for the unindexed SampleAliquotInstance and one with the index.");
                    }
                }
            }               

            if (!foundSample) {
                Assert.fail("Failed to find sample " + sam);
            }
            if (!foundProject) {
                Assert.fail("Failed to find project");
            }
            if (!wasIndexFound) {
                Assert.fail("Couldn't find envelope " + expectedEnvelope);
            }
        }
    }
    
    


}
