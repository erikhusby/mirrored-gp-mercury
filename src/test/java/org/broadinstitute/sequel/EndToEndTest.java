package org.broadinstitute.sequel;


import org.broadinstitute.sequel.infrastructure.bsp.AliquotReceiver;
import org.broadinstitute.sequel.infrastructure.bsp.MockBSPConnector;
import org.broadinstitute.sequel.control.dao.vessel.LabVesselDAO;
import org.broadinstitute.sequel.infrastructure.jira.DummyJiraService;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingResponse;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.project.*;
import org.broadinstitute.sequel.entity.queue.AliquotParameters;
import org.broadinstitute.sequel.entity.queue.BSPAliquotWorkQueue;
import org.broadinstitute.sequel.entity.run.RunCartridge;
import org.broadinstitute.sequel.entity.run.RunChamber;
import org.broadinstitute.sequel.entity.run.SequencingRun;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.MolecularEnvelope;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

public class EndToEndTest  {

    //@Inject
    LabVesselDAO labVesselDAO;
    
    //@Inject
    LabEventHandler handler;

    private LabVessel createBSPStock(String sampleName,String tubeBarcode,ProjectPlan projectPlan) {
        SampleSheet sampleSheet = new SampleSheet();
        // this seems redundant: we're adding a sample sheet with only the stock
        // name itself.  More often we'll expect to see pre-pooled "samples",
        // in which case the BSP stock id will actually have multiple
        // component collaborator samples.
        sampleSheet.addStartingSample(new BSPSample(sampleName, projectPlan,null));
        return new TwoDBarcodedTube(tubeBarcode,sampleSheet);
    }
    
    private LabVessel createBSPAliquot(String aliquotName,String tubeBarcode,ProjectPlan projectPlan) {
        // yowza, it's the same code!
        return createBSPStock(aliquotName,tubeBarcode,projectPlan);
    }
    
    @Test(groups = {DATABASE_FREE})
    public void doIt() {

        String masterSample1 = "master sample1";
        String masterSample2 = "master sample2";
        String aliquot1Label = "aliquot1";
        String aliquot2Label = "aliquot2";
        Map<LabEventName,PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();

        PriceItem priceItem = new PriceItem("Specialized Library Construction","1","HS Library","1000","Greenbacks/Dough/Dollars", PriceItem.GSP_PLATFORM_NAME);
        billableEvents.put(LabEventName.ADAPTOR_LIGATION,priceItem);
        final WorkflowDescription workflow = new WorkflowDescription("Hybrid Selection",
                billableEvents,
                CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);
        Project project = new BasicProject("Project1",new JiraTicket(new DummyJiraService(),"TP-0","0"));
        ProjectPlan plan1 = new ProjectPlan(project,"Plan for " + project.getProjectName(),new WorkflowDescription("WGS", billableEvents,CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));
        Project project2 = new BasicProject("Project2", new JiraTicket(new DummyJiraService(),"TP-1","1"));
        ProjectPlan plan2 = new ProjectPlan(project2,"Plan for "  + project2.getProjectName(),new WorkflowDescription("WGS", billableEvents,CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));

        LabVessel stock1 = createBSPStock(masterSample1,"00001234",plan1);
        LabVessel stock2 = createBSPStock(masterSample2,"00005678",plan2);

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
        AliquotParameters aliquotParameters = new AliquotParameters(plan1,0.9f,0.6f);
        AliquotParameters aliquotParameters2 = new AliquotParameters(plan2,1.9f,2.6f);
        
        aliquotWorkQueue.add(stock1,aliquotParameters,plan1.getWorkflowDescription(),null);
        aliquotWorkQueue.add(stock2,aliquotParameters2,plan2.getWorkflowDescription(),null);

        BSPPlatingResponse platingResponse = aliquotWorkQueue.sendBatch();

        Assert.assertFalse(plan1.getPendingPlatingRequests().isEmpty());
        Assert.assertEquals(1, plan1.getPendingPlatingRequests().size());
        Assert.assertEquals(1, plan2.getPendingPlatingRequests().size());

        BSPPlatingReceipt project1PlatingReceipt = plan1.getPendingPlatingRequests().iterator().next().getReceipt();
        BSPPlatingReceipt project2PlatingReceipt = plan2.getPendingPlatingRequests().iterator().next().getReceipt();

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

        Assert.assertTrue(plan1.getPendingPlatingRequests().isEmpty());
        Assert.assertTrue(plan2.getPendingPlatingRequests().isEmpty());
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

        Assert.assertFalse(aliquotTube.getAllStatusNotes().isEmpty());
        Assert.assertFalse(aliquot2Tube.getAllStatusNotes().isEmpty());

        for (StatusNote statusNote : aliquotTube.getAllStatusNotes()) {
            Assert.assertEquals(LabEventName.ALIQUOT_RECEIVED, statusNote.getEventName());
        }

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
                                          ProjectPlan projectPlan,
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


                        ProjectPlan fetchedPlan = sampleInstance.getSingleProjectPlan();
                        if (projectPlan.equals(fetchedPlan)) {
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
