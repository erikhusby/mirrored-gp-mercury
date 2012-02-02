package org.broadinstitute.sequel;


import org.broadinstitute.sequel.bettalims.jaxb.PlateTransferEventType;
import org.broadinstitute.sequel.bettalims.jaxb.PlateType;
import org.broadinstitute.sequel.bettalims.jaxb.PositionMapType;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class EndToEndTest  {

    @Inject LabVesselDAO labVesselDAO;
    
    @Inject LabEventHandler handler;

    private JiraTicket createMockJiraTicket() {
        JiraTicket ticket = EasyMock.createMock(JiraTicket.class);
        EasyMock.expect(ticket.addComment((String)EasyMock.anyObject())).andReturn(JiraTicket.JiraResponse.OK).atLeastOnce();
        EasyMock.replay(ticket);
        return ticket;
    }

    private LabVessel createBSPStock(String sampleName,String tubeBarcode) {
        SampleSheet sampleSheet = new SampleSheetImpl();
        // this seems redundant: we're adding a sample sheet with only the stock
        // name itself.  More often we'll expect to see pre-pooled "samples",
        // in which case the BSP stock id will actually have multiple
        // component collaborator samples.
        sampleSheet.addSample(new SampleInstanceImpl(new BSPSample(sampleName), SampleInstance.GSP_CONTROL_ROLE.NONE,null,null,null));
        return new TwoDBarcodedTube(new BaseGoop(sampleName,sampleSheet),tubeBarcode);
    }
    
    private LabVessel createBSPAliquot(String aliquotName,String tubeBarcode) {
        // yowza, it's the same code!
        return createBSPStock(aliquotName,tubeBarcode);
    }
    
    @Test
    public void doIt() {

        String masterSample1 = "master sample1";
        String masterSample2 = "master sample2";
        String aliquot1Label = "aliquot1";
        String aliquot2Label = "aliquot2";
        final WorkflowDescription workflow = new WorkflowDescription("Hybrid Selection", "7.0");
        Project project = new BasicProject("Project1",createMockJiraTicket());
        Project project2 = new BasicProject("Project2",createMockJiraTicket());

        LabVessel stock1 = createBSPStock(masterSample1,"00001234");
        LabVessel stock2 = createBSPStock(masterSample2,"00005678");

        BSPAliquotWorkQueue aliquotWorkQueue = new BSPAliquotWorkQueue(new MockBSPConnector());

        Assert.assertTrue(project.getAllLabTangibles().isEmpty());
        Assert.assertTrue(project2.getAllLabTangibles().isEmpty());
        // add a sample to the project
        project.addLabTangible(stock1, workflow);
        project2.addLabTangible(stock2, workflow);

        Assert.assertTrue(project.getAllLabTangibles().contains(stock1));
        Assert.assertTrue(project2.getAllLabTangibles().contains(stock2));
        Assert.assertTrue(project.getLabTangibles(workflow).contains(stock1));
        Assert.assertFalse(project.getAllLabTangibles().contains(stock2));


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

        LabVessel aliquotTube = createBSPAliquot(aliquot1Label,aliquot1Label);
        LabVessel aliquot2Tube = createBSPAliquot(aliquot2Label,aliquot2Label);

        Goop aliquot = aliquotTube.getGoop();
        Goop aliquot2 = aliquot2Tube.getGoop();

        Assert.assertTrue(aliquot.getAllProjects().isEmpty()); // we just got the aliquot -- we don't know the project yet!
        Assert.assertTrue(aliquot2.getAllProjects().isEmpty()); // we just got the aliquot -- we don't know the project yet!

        Assert.assertTrue(aliquot2.getAllStatusNotes().isEmpty());
        BSPPlatingRequest platingRequest = new AliquotReceiver().receiveAliquot(stock1,aliquotTube,platingResponse.getReceipt());
        BSPPlatingRequest platingRequest2 = new AliquotReceiver().receiveAliquot(stock2,aliquot2Tube,platingResponse.getReceipt());

        Assert.assertTrue(project.getPendingPlatingRequests().isEmpty());
        Assert.assertTrue(project2.getPendingPlatingRequests().isEmpty());
        Assert.assertNotNull(platingRequest);
        Assert.assertFalse(aliquot.getAllProjects().isEmpty()); // after receiving the aliquot,
                                                                   // we should know the project
        Assert.assertEquals(1,aliquot.getAllProjects().size());
        Assert.assertEquals(1,aliquot2.getAllProjects().size());


        Assert.assertEquals(aliquotParameters,platingRequest.getAliquotParameters());
        Assert.assertEquals(project,aliquot.getAllProjects().iterator().next());

        Assert.assertEquals(aliquotParameters2,platingRequest2.getAliquotParameters());
        Assert.assertEquals(project2,aliquot2.getAllProjects().iterator().next());

        EasyMock.verify(project.getJiraTicket());

        Assert.assertFalse(aliquot.getAllStatusNotes().isEmpty());
        Assert.assertFalse(aliquot2.getAllStatusNotes().isEmpty());

        for (StatusNote statusNote : aliquot.getAllStatusNotes()) {
            Assert.assertEquals(LabEventName.ALIQUOT_RECEIVED, statusNote.getEventName());
        }

        // package structure DDD (Bien in parens)
        //    application (control)
        //       impl
        //       util
        //    domain (entity)
        //       model
        //          <area1 etc.>
        //       service
        //       shared
        //    infrastructure
        //       messaging
        //       persistence
        //       routing
        //    interface (boundary)
        //       <area1 etc.>
        //           web
        //           ws

        PlateTransferEventType plateTransferEvent = new PlateTransferEventType();
        PlateType sourcePlate = new PlateType();
        plateTransferEvent.setSourcePlate(sourcePlate);
        plateTransferEvent.setSourcePositionMap(new PositionMapType());
        PlateType destinationPlate = new PlateType();
        plateTransferEvent.setPlate(destinationPlate);

        // kiosk associates tubes with rack?
        RackOfTubes rackOfTubes = new RackOfTubes("KioskRack1");
        rackOfTubes.addContainedVessel(aliquotTube); // need ability to add at well position
        rackOfTubes.addContainedVessel(aliquot2Tube); // need ability to add at well position
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
         * Todo arz: test {@link Goop#applyReagent(Reagent)} by applying
         * a reagent transfer.  Also do a test from a container that has
         * SampleSheets with samples in it to a LabVessel that contains
         * only {@link Reagent}, so test scenarios where we add samples
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
                                          Goop sam,
                                          int numberOfSampleSheetsPerSample,
                                          MolecularEnvelope expectedEnvelope) {
        boolean foundSample = false;
        boolean foundProject = false;
        boolean wasIndexFound = false;
        for (RunCartridge cartridge: srun.getSampleCartridge()) {
            for (RunChamber chamber: cartridge.getChambers()) {
                for (SampleSheet sampleSheet : chamber.getGoop().getSampleSheets()) {
                    for (SampleInstance sampleInstance: sampleSheet.getSamples()) {
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
