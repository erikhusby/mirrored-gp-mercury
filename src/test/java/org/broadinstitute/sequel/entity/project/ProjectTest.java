package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.TestUtilities;
import org.broadinstitute.sequel.WeldUtil;
import org.broadinstitute.sequel.control.jira.DummyJiraService;
import org.broadinstitute.sequel.control.jira.JiraService;
import org.broadinstitute.sequel.control.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.control.jira.issue.CreateIssueResponse;
import org.broadinstitute.sequel.control.quote.*;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
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
import org.broadinstitute.sequel.entity.workflow.Workflow;
import org.broadinstitute.sequel.entity.workflow.WorkflowEngine;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;
import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;
import static org.testng.Assert.*;

public class ProjectTest {

    @Test(groups = EXTERNAL_INTEGRATION)
    public void test_project_jira() throws Exception {
        WeldUtil weld = TestUtilities.bootANewWeld();
        JiraService jiraService = weld.getFromContainer(JiraService.class);
        
        CreateIssueResponse response = jiraService.createIssue(Project.JIRA_PROJECT_PREFIX,
                CreateIssueRequest.Fields.Issuetype.SequeL_Project,
                "Test run by " + System.getProperty("user.name") + " on " + new SimpleDateFormat("yyyy/MM/dd").format(new Date(System.currentTimeMillis())),
                "Do lots of sequencing");
        assertNotNull(response);
        JiraTicket ticket = new JiraTicket(jiraService,response.getTicketName(),response.getId());
        AbstractProject project = new BasicProject(ticket.getTicketName(),ticket);
        project.addJiraComment("Comment added via Project");
        assertTrue(response.getTicketName().startsWith(Project.JIRA_PROJECT_PREFIX));
        // todo how to verify the comment was added?
    }
    
    @Test(groups = {DATABASE_FREE})
    public void test_simple_project() {
        AbstractProject project = projectManagerCreatesProject();
        projectManagerAddsFundingSourceToProject(project,"NHGRI");
        ProjectPlan plan = projectManagerAddsProjectPlan(project);
        ReagentDesign bait = projectManagerAddsBait(plan);
        SequencingPlanDetail sequencingDetail = projectManagerAddsSequencingDetails(plan);
        
        // PM would choose samples, or run a search
        // to find samples, maybe by querying the PASS,
        // maybe by running a stored search in BSP,
        // or maybe by dumping in a list of root
        // sample ids.
        LabVessel starter = makeRootSample("000029103912",project);
        StartingSample startingSample = starter.getSampleInstances().iterator().next().getStartingSample();
        projectManagerAddsStartersToPlan(starter,plan);

        assertFalse(project.getAllStarters() == null);

        assertEquals(1,project.getAllStarters().size());

        for (LabVessel vessel : project.getAllStarters()) {
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
        assertEquals(bait.getDesignName(),fetchedDesign.getDesignName());

        Collection<SampleInstance> sampleInstances = new HashSet<SampleInstance>();

        for (LabVessel vessel : project.getAllStarters()) {
            sampleInstances.addAll(vessel.getSampleInstances());
        }

        assertEquals(1,sampleInstances.size());

        for (SampleInstance sampleInstance : sampleInstances) {
            assertEquals(startingSample,sampleInstance.getStartingSample());
            assertEquals(project,sampleInstance.getProject());
        }

        assertEquals(plan.getName(),project.getProjectName() + " Plan",plan.getName());

        assertEquals(1,plan.getPlanDetails().size());

        SequencingPlanDetail planDetail = plan.getPlanDetails().iterator().next();
        assertEquals(sequencingDetail,planDetail);

        assertEquals(SequencingTechnology.TECHNOLOGY_NAME.ION_TORRENT,sequencingDetail.getSequencingTechnology().getTechnologyName());
        assertEquals("HybridSelection",sequencingDetail.getProjectPlan().getWorkflowDescription().getWorkflowName());
        assertEquals(65,((IonSequencingTechnology)sequencingDetail.getSequencingTechnology()).getCycleCount());
        assertEquals(IonSequencingTechnology.CHIP_TYPE.CHIP1,((IonSequencingTechnology)sequencingDetail.getSequencingTechnology()).getChipType());

        assertEquals(30,((XFoldCoverage)sequencingDetail.getCoverageGoal()).getCoverageDepth());

        Collection<Quote> quotes = project.getAvailableQuotes();

        assertEquals(2,quotes.size());

        // PM would pick the queue from a drop down,
        // filtered by the {@link WorkflowDescription}?
        LabWorkQueue lcWorkQueue = createLabWorkQueue();

        assertTrue(lcWorkQueue.isEmpty());
        
        Workflow workflowInstance = projectManagerEnquesLabWork(starter,plan,lcWorkQueue);

        assertFalse(lcWorkQueue.isEmpty());
        
        labStaffStartsWork(starter,plan.getWorkflowDescription(),lcWorkQueue);

        assertTrue(lcWorkQueue.isEmpty());

        assertEquals("work has stated", workflowInstance.getState().getState());
    }
    
    /**
     * Basic project setup: PM names the
     * project.  We automatically generate
     * the corresponding jira ticket.  Perhaps
     * we name the squid project with the jira
     * id, so that we don't run into the
     * "oh, we call this LCSet-21, not Work Request 31029"
     * problem.
     * @return
     */
    private AbstractProject projectManagerCreatesProject() {
        AbstractProject legacyProject = new BasicProject("Legacy Squid Project C203",new JiraTicket(new DummyJiraService(),"TP-0","0"));
        return legacyProject;
    }

    /**
     * After making a project, the PM adds a
     * project plan.
     * @param project
     * @return
     */
    private ProjectPlan projectManagerAddsProjectPlan(Project project) {
        PriceItem priceItem = new PriceItem("Specialized Library Construction","1","HS Library","1000","Greenbacks/Dough/Dollars",PriceItem.GSP_PLATFORM_NAME);

        Map<LabEventName,PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();
        billableEvents.put(LabEventName.SAGE_UNLOADED,priceItem);
        WorkflowDescription workflow = new WorkflowDescription("HybridSelection","9.6",billableEvents);
        ProjectPlan plan = new ProjectPlan(project,project.getProjectName() + " Plan",workflow);
        
        
        return plan;
    }

    /**
     * Once you've got the prep plan, PMs add one or
     * more {@link SequencingPlanDetail} 
     * @param projectPlan
     */
    private SequencingPlanDetail projectManagerAddsSequencingDetails(ProjectPlan projectPlan) {
        return new SequencingPlanDetail(
                new IonSequencingTechnology(65, IonSequencingTechnology.CHIP_TYPE.CHIP1),
                new XFoldCoverage(30),
                projectPlan);
    }
    
    private LabVessel makeRootSample(String sampleName,Project project) {
        SampleSheetImpl sampleSheet = new SampleSheetImpl();
        StartingSample startingSample = new BSPSample("BSPRoot123",project,null);
        sampleSheet.addStartingSample(startingSample);
        // todo: instead of a bogus TwoDBarcodedTube for the root, lookup BSP
        // container information inside a BSPVessel object, most of whose
        // methods throw exceptions that say "Hey, I'm from BSP, you can't do that!"
        LabVessel starter = new TwoDBarcodedTube(sampleName, sampleSheet);
        return starter;
    }
    
    private void projectManagerAddsStartersToPlan(LabVessel starter,
                                                  ProjectPlan projectPlan) {       
        
        projectPlan.addStarter(starter);
    }

    /**
     * Perhaps we're doing hybrid selection.  In that case,
     * the PM would add one or more baits per plan.  This
     * implies that the bait should be added to every
     * sample in the plan.
     * @param projectPlan
     * @return
     */
    private ReagentDesign projectManagerAddsBait(ProjectPlan projectPlan) {
        ReagentDesign bait = new ReagentDesign("agilent_foo", ReagentDesign.REAGENT_TYPE.BAIT);
        projectPlan.addReagentDesign(bait);
        
        return bait;
    }
    
    private LabWorkQueue createLabWorkQueue() {
        WorkflowEngine workflowEngine = new WorkflowEngine();
        LabWorkQueue labWorkQueue = new FIFOLabWorkQueue(LabWorkQueueName.LC,workflowEngine);
        return labWorkQueue;
    }

    private Workflow projectManagerEnquesLabWork(LabVessel starter,
                                             ProjectPlan projectPlan,
                                             LabWorkQueue labWorkQueue) {
       
        WorkflowEngine workflowEngine = labWorkQueue.getWorkflowEngine();
        Collection<Workflow> workflows = workflowEngine.getActiveWorkflows(starter,null);
        assertTrue(workflows.isEmpty());
        assertTrue(labWorkQueue.isEmpty());

        labWorkQueue.add(starter,null,projectPlan.getPlanDetails().iterator().next());

        workflows = workflowEngine.getActiveWorkflows(starter,null);
        assertEquals(1, workflows.size());
        Workflow workflowInstance = workflows.iterator().next();
        assertNull(workflowInstance.getState());
        assertEquals(1,workflowInstance.getAllVessels().size());
        assertTrue(workflowInstance.getAllVessels().contains(starter));
        assertEquals(projectPlan,workflowInstance.getProjectPlan());
        assertFalse(labWorkQueue.isEmpty());
        
        return workflowInstance;
    }


    private void labStaffStartsWork(LabVessel vessel,
                                    WorkflowDescription workflowDescription,
                                    LabWorkQueue labWorkQueue) {
        LabWorkQueueResponse queueResponse = labWorkQueue.startWork(vessel,
                null,
                workflowDescription,
                new Person("tony","Tony","Hawk"));
        
    }

    private void projectManagerAddsFundingSourceToProject(AbstractProject project,
                                                          String grantName) {
        project.addGrant(grantName);
        project.setQuotesCache(buildQuotesCache());
    }

    
    private QuotesCache buildQuotesCache() {
        Quotes quotes = new Quotes();
        quotes.addQuote(new Quote("GF128",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NHGRI")))));
        quotes.addQuote(new Quote("GF129",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NHGRI")))));
        quotes.addQuote(new Quote("GF130",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NCI")))));
        return new QuotesCache(quotes);
    }
}
