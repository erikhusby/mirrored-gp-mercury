package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.TestUtilities;
import org.broadinstitute.sequel.control.quote.*;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.queue.FIFOLabWorkQueue;
import org.broadinstitute.sequel.entity.queue.LabWorkQueue;
import org.broadinstitute.sequel.entity.queue.LabWorkQueueName;
import org.broadinstitute.sequel.entity.queue.LabWorkQueueResponse;
import org.broadinstitute.sequel.entity.run.IonSequencingTechnology;
import org.broadinstitute.sequel.entity.run.SequencingTechnology;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheetImpl;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;

import static org.testng.Assert.*;

import org.broadinstitute.sequel.entity.workflow.WorkflowDescription;
import org.easymock.EasyMock;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.HashSet;

public class ProjectTest {
    
    @Test(groups = {"DatabaseFree"})
    public void test_legacy_squid_project() {

        JiraTicket ticket = EasyMock.createMock(JiraTicket.class);
        EasyMock.expect(ticket.addComment(EasyMock.contains("has started work for plan"))).andReturn(JiraTicket.JiraResponse.OK).times(1);
        EasyMock.replay(ticket);

        AbstractProject legacyProject = new BasicProject("Legacy Squid Project C203",ticket);
        ProjectPlan plan = new ProjectPlan(legacyProject,legacyProject.getProjectName() + " Plan");
        ReagentDesign bait = new ReagentDesign("agilent_foo", ReagentDesign.REAGENT_TYPE.BAIT);
        String aliquotBarcode = "000029103912";
        plan.addReagentDesign(bait);
        WorkflowDescription workflow = new WorkflowDescription("HybridSelection","9.6");

        SequencingPlanDetail ionPlan = new SequencingPlanDetail(workflow,
                new IonSequencingTechnology(65, IonSequencingTechnology.CHIP_TYPE.CHIP1),
                new XFoldCoverage(30),
                plan);

        plan.addSequencingDetail(ionPlan);

        legacyProject.addProjectPlan(plan);

        SampleSheetImpl sampleSheet = new SampleSheetImpl();
        StartingSample startingSample = new BSPSample("BSPRoot123",legacyProject,null);
        sampleSheet.addStartingSample(startingSample);
        LabVessel starter = new TwoDBarcodedTube(aliquotBarcode, sampleSheet);
        
        // todo: instead of a bogus TwoDBarcodedTube for the root, lookup BSP
        // container information inside a BSPVessel object, most of whose
        // methods throw exceptions that say "Hey, I'm from BSP, you can't do that!"
        plan.addStarter(starter);

        assertFalse(legacyProject.getAllStarters() == null);
        
        assertEquals(1,legacyProject.getAllStarters().size());

        for (LabVessel vessel : legacyProject.getAllStarters()) {
            assertEquals(starter,vessel);
        }
        
        assertFalse(plan.getStarters().isEmpty());
        assertEquals(1,plan.getStarters().size());

        for (LabVessel vessel : plan.getStarters()) {
            assertEquals(starter,vessel);
        }
        
        assertFalse(plan.getReagentDesigns() == null);
        assertEquals(1,plan.getReagentDesigns().size());
        
        ReagentDesign fetchedDesign = plan.getReagentDesigns().iterator().next();
        
        assertEquals(ReagentDesign.REAGENT_TYPE.BAIT,fetchedDesign.getReagentType());
        assertEquals("agilent_foo",fetchedDesign.getDesignName());

        Collection<SampleInstance> sampleInstances = new HashSet<SampleInstance>();

        for (LabVessel vessel : legacyProject.getAllStarters()) {
            sampleInstances.addAll(vessel.getSampleInstances());
        }
        
        assertEquals(1,sampleInstances.size());

        for (SampleInstance sampleInstance : sampleInstances) {
            assertEquals(startingSample,sampleInstance.getStartingSample());
            assertEquals(legacyProject,sampleInstance.getProject());
        }
        
        assertEquals(plan.getName(),legacyProject.getProjectName() + " Plan",plan.getName());
        
        assertEquals(1,plan.getPlanDetails().size());
        
        SequencingPlanDetail planDetail = plan.getPlanDetails().iterator().next();
        assertEquals(ionPlan,planDetail);
        
        assertEquals(SequencingTechnology.TECHNOLOGY_NAME.ION_TORRENT,ionPlan.getSequencingTechnology().getTechnologyName());
        assertEquals("HybridSelection",ionPlan.getWorkflow().getWorkflowName());
        assertEquals(65,((IonSequencingTechnology)ionPlan.getSequencingTechnology()).getCycleCount());
        assertEquals(IonSequencingTechnology.CHIP_TYPE.CHIP1,((IonSequencingTechnology)ionPlan.getSequencingTechnology()).getChipType());
        
        assertEquals(30,((XFoldCoverage)ionPlan.getCoverageGoal()).getCoverageDepth());

        legacyProject.addGrant("NHGRI");
        legacyProject.setQuotesCache(buildQuotesCache());
        
        Collection<Quote> quotes = legacyProject.getAvailableQuotes();
        
        assertEquals(2,quotes.size());

        LabWorkQueue labWorkQueue = new FIFOLabWorkQueue(LabWorkQueueName.LC);

        assertTrue(labWorkQueue.isEmpty());
        labWorkQueue.add(starter,null,ionPlan);

        assertFalse(labWorkQueue.isEmpty());
        
        // todo add a transfer event, look for project relationships
        // on destinations
        
        LabWorkQueueResponse queueResponse = labWorkQueue.startWork(starter,null,ionPlan.getWorkflow(),new Person("tony","Tony","Hawk"));

        assertTrue(labWorkQueue.isEmpty());
        EasyMock.verify(ticket);


    }
    
    private QuotesCache buildQuotesCache() {
        Quotes quotes = new Quotes();
        quotes.addQuote(new Quote("GF128",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NHGRI")))));
        quotes.addQuote(new Quote("GF129",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NHGRI")))));
        quotes.addQuote(new Quote("GF130",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NCI")))));
        return new QuotesCache(quotes);
    }
}
