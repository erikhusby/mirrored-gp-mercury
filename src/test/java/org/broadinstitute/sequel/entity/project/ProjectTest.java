package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.TestUtilities;
import org.broadinstitute.sequel.WeldUtil;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.control.jira.JiraService;
import org.broadinstitute.sequel.control.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.control.jira.issue.CreateIssueResponse;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.queue.*;
import org.broadinstitute.sequel.entity.run.IonSequencingTechnology;
import org.broadinstitute.sequel.entity.run.SequencingTechnology;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheetImpl;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.entity.workflow.Workflow;
import org.broadinstitute.sequel.entity.workflow.WorkflowEngine;
import org.broadinstitute.sequel.entity.billing.Quote;
import org.broadinstitute.sequel.infrastructure.quote.*;
import org.testng.annotations.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

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
    
    @Test(groups = {EXTERNAL_INTEGRATION})
    public void test_simple_project() {
        WeldUtil weld = TestUtilities.bootANewWeld();
        JiraService jiraService = weld.getFromContainer(JiraService.class);
        BSPSampleDataFetcher bspFetcher = weld.getFromContainer(BSPSampleDataFetcher.class);
        AbstractProject project = projectManagerCreatesProject(jiraService);
        projectManagerAddsFundingSourceToProject(project,"NHGRI");
        ProjectPlan plan = projectManagerAddsProjectPlan(project);
        ReagentDesign bait = projectManagerAddsBait(plan);
        SequencingPlanDetail sequencingDetail = projectManagerAddsSequencingDetails(plan);
        
        // PM would choose samples, or run a search
        // to find samples, maybe by querying the PASS,
        // maybe by running a stored search in BSP,
        // or maybe by dumping in a list of root
        // sample ids.
        LabVessel starter1 = makeRootSample("SM-1P3WY",plan,bspFetcher);
        LabVessel starter2 = makeRootSample("SM-1P3XN",plan,bspFetcher);
        Collection<LabVessel> allStarters = new HashSet<LabVessel>();

        allStarters.add(starter1);
        allStarters.add(starter2);

        int numStartersExpected = 1;
        for (LabVessel starter : allStarters) {
            projectManagerAddsStartersToPlan(starter,plan);

            assertFalse(project.getAllStarters() == null);

            assertEquals(numStartersExpected,project.getAllStarters().size());

            assertTrue(project.getAllStarters().contains(starter));

            assertFalse(plan.getStarters().isEmpty());
            assertEquals(numStartersExpected,plan.getStarters().size());

            assertTrue(plan.getStarters().contains(starter));

            numStartersExpected++;
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

        assertEquals(2,sampleInstances.size());

        for (LabVessel starter : allStarters) {
            boolean foundIt = false;
            for (SampleInstance sampleInstance : sampleInstances) {
                StartingSample startingSample = starter.getSampleInstances().iterator().next().getStartingSample();
                if (startingSample.equals(sampleInstance.getStartingSample())) {
                    if (starter.getSampleInstances().iterator().next().getSingleProjectPlan().equals(sampleInstance.getSingleProjectPlan())) {
                        foundIt  = true;
                    }
                }
            }
            if (!foundIt) {
                fail("Can't find starting sample or project plan relationship for " + starter.getLabCentricName());
            }
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
        LabWorkQueue<LcSetParameters> lcWorkQueue = createLabWorkQueue();

        assertTrue(lcWorkQueue.isEmpty());

        LcSetParameters lcSetParameters = new LcSetParameters();
        Workflow workflowInstance = null;
        assertTrue(lcWorkQueue.isEmpty());
        for (LabVessel starter : allStarters) {
            workflowInstance = projectManagerEnquesLabWork(starter,plan,lcSetParameters,lcWorkQueue);
        }

        assertFalse(lcWorkQueue.isEmpty());



        JiraTicket jiraTicket = labStaffStartsWork(allStarters,
                plan.getWorkflowDescription(),
                lcWorkQueue,
                lcSetParameters,
                jiraService);

        assertTrue(jiraTicket.getTicketName().startsWith(plan.getWorkflowDescription().getJiraProjectPrefix()));
        
        assertTrue(lcWorkQueue.isEmpty());

        assertEquals("work has stated", workflowInstance.getState().getState());

        postSomethingFunToJira(jiraTicket,allStarters,project);
        
        Collection<LabVessel> reworkVessels = new HashSet<LabVessel>();
        reworkVessels.add(allStarters.iterator().next());

        // post notice of the LC set ticket back to the project
        JiraTicket jiraTicketForRework = labStaffStartsWork(reworkVessels,
                plan.getWorkflowDescription(),
                lcWorkQueue,
                lcSetParameters,
                jiraService);

        // oh dear, one sample ended up getting reworked.
        postSomethingFunToJira(jiraTicketForRework,reworkVessels,project);
        
        assertEquals(2,plan.getJiraTickets().size());
        
        assertTrue(plan.getJiraTickets().contains(jiraTicket));
        assertTrue(plan.getJiraTickets().contains(jiraTicketForRework));

    }
    
    private void postSomethingFunToJira(JiraTicket jiraTicket,
                                        Collection<LabVessel> vessels,
                                        Project project) {
        StringBuilder projectJiraMessage = new StringBuilder(jiraTicket.getTicketName() + " has been created for the following samples:\n");
        for (LabVessel vessel : vessels) {
            for (SampleInstance sampleInstance : vessel.getSampleInstances()) {
                StartingSample startingSample = sampleInstance.getStartingSample();
                for (ProjectPlan projectPlan : sampleInstance.getAllProjectPlans()) {
                    projectPlan.addJiraTicket(jiraTicket);
                }
                String sampleURL = "[" + startingSample.getSampleName() + "|http://gapqa01:8080/BSP/samplesearch/SampleSummary.action?sampleId=" + startingSample.getSampleName() + "]";
                projectJiraMessage.append("* ").append(sampleURL).append(" (Patient ").append(startingSample.getPatientId()).append(")\n").append("** Paid for by ").append(sampleInstance.getSingleProjectPlan().getQuoteDTO().getQuoteFunding().getFundingLevel().getFunding().getGrantDescription()).append("\n");
            }
        }
        project.addJiraComment(projectJiraMessage.toString());
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
    private AbstractProject projectManagerCreatesProject(JiraService jiraService) {
        String projectName = "Legacy Squid Project C203";
        CreateIssueResponse jiraResponse = null;

        try {
            jiraResponse = jiraService.createIssue(Project.JIRA_PROJECT_PREFIX,
                    CreateIssueRequest.Fields.Issuetype.SequeL_Project,
                    projectName,
                    "Created by " + getClass().getCanonicalName());
        }
        catch(IOException e ) {
            throw new RuntimeException("Cannot create jira ticket",e);
        }
        AbstractProject legacyProject = new BasicProject(projectName,new JiraTicket(jiraService,jiraResponse.getTicketName(),jiraResponse.getId()));
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
        WorkflowDescription workflow = new WorkflowDescription("HybridSelection","9.6",billableEvents,CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);
        ProjectPlan plan = new ProjectPlan(project,project.getProjectName() + " Plan",workflow);
        String quoteId = "DNA23";
        plan.setQuote(new Quote(quoteId,
                new org.broadinstitute.sequel.infrastructure.quote.Quote(quoteId,new QuoteFunding(new FundingLevel("50",new Funding(Funding.FUNDS_RESERVATION,"NHGRI"))))));
        
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
    
    private LabVessel makeRootSample(String sampleName,ProjectPlan projectPlan,BSPSampleDataFetcher bspFetcher) {
        SampleSheetImpl sampleSheet = new SampleSheetImpl();
        StartingSample startingSample = new BSPSample(sampleName,projectPlan,bspFetcher.fetchSingleSampleFromBSP(sampleName));
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
    
    private LabWorkQueue<LcSetParameters> createLabWorkQueue() {
        WorkflowEngine workflowEngine = new WorkflowEngine();
        LabWorkQueue<LcSetParameters> labWorkQueue = new FIFOLabWorkQueue<LcSetParameters>(LabWorkQueueName.LC,workflowEngine);
        return labWorkQueue;
    }

    private Workflow projectManagerEnquesLabWork(LabVessel starter,
                                             ProjectPlan projectPlan,
                                             LabWorkQueueParameters queueParameters,
                                             LabWorkQueue labWorkQueue) {
       
        WorkflowEngine workflowEngine = labWorkQueue.getWorkflowEngine();
        Collection<Workflow> workflows = workflowEngine.getActiveWorkflows(starter,null);


        labWorkQueue.add(starter,queueParameters,projectPlan.getPlanDetails().iterator().next());

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


    /**
     * Lab user says "I'm going to start work on these tubes".
     * The parameters are the same for each tube because the
     * "batch" that the user is starting consists of samples
     * that are all undergoing the same process, at least
     * for this lab user's portion of the process.
     *
     * This implies that the jira ticket that is created for this
     * portion of the process exists only for this portion
     * of the process.  Put another way, if there is a lab group
     * that does preflight, perhaps we have a preflight ticket
     * for each rack that they work on.  Perhaps after three preflight
     * tickets are done, the samples are batched together
     * for another team, which creates a ticket for a different
     * batch.
     *
     * Or perhaps the same batch exists all the way from BSP
     * aliquoting through sequencing.  If the lab uses a single
     * batch/ticket all the way through, then a {@link LabWorkQueue}
     * could just require an existing ticket instead of creating
     * a new one on the fly.
     * @param vessels
     * @param workflowDescription
     * @param labWorkQueue
     * @param lcSetParameters
     * @param jiraService
     * @return
     */
    private JiraTicket labStaffStartsWork(Collection<LabVessel> vessels,
                                    WorkflowDescription workflowDescription,
                                    LabWorkQueue<LcSetParameters> labWorkQueue,
                                    LcSetParameters lcSetParameters,
                                    JiraService jiraService) {
        Person tonyHawk = new Person("tony","Tony","Hawk");
        for (LabVessel vessel : vessels) {
            LabWorkQueueResponse queueResponse = labWorkQueue.startWork(vessel,
                    lcSetParameters,
                    workflowDescription,
                    tonyHawk);
        }
        CreateIssueResponse jiraResponse = createJiraTicket(vessels,workflowDescription,lcSetParameters,jiraService);

        JiraTicket ticket = new JiraTicket(jiraService,jiraResponse.getTicketName(),jiraResponse.getId());

        return ticket;
    }
    
    private CreateIssueResponse createJiraTicket(Collection<LabVessel> vessels,
                                  WorkflowDescription workflowDescription,
                                  LcSetParameters lcSetParameters,
                                  JiraService jiraService) {
        Collection<Project> allProjects = new HashSet<Project>();
        for (LabVessel vessel : vessels) {
            for (SampleInstance sampleInstance : vessel.getSampleInstances()) {
                for (ProjectPlan projectPlan : sampleInstance.getAllProjectPlans()) {
                    allProjects.add(projectPlan.getProject());
                }
            }
        }
        
        String ticketTitle = null;
        StringBuilder ticketDetails = new StringBuilder();
        if (allProjects.size() == 1) {
            Project singleProject = allProjects.iterator().next();
            ticketTitle = "Work for " + singleProject.getProjectName();
            ticketDetails.append(singleProject.getProjectName());
        }
        else {
            ticketTitle = "Work for " + allProjects.size() + " projects";
            for (Project project : allProjects) {
                ticketDetails.append(project.getProjectName()).append(" ");
            }
        }
        
        CreateIssueResponse jiraTicketCreationResponse =  null;
        
        try {
            jiraTicketCreationResponse = jiraService.createIssue(workflowDescription.getJiraProjectPrefix(),
                    workflowDescription.getJiraIssueType(),
                    ticketTitle,
                    ticketDetails.toString());
            // todo use #lcSetParameters to add more details to the ticket
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to create jira ticket",e);
        }
        return jiraTicketCreationResponse;
    }

    private void projectManagerAddsFundingSourceToProject(AbstractProject project,
                                                          String grantName) {
        project.addGrant(grantName);
        project.setQuotesCache(buildQuotesCache());
    }

    
    private QuotesCache buildQuotesCache() {
        Quotes quotes = new Quotes();
        quotes.addQuote(new org.broadinstitute.sequel.infrastructure.quote.Quote("GF128",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NHGRI")))));
        quotes.addQuote(new  org.broadinstitute.sequel.infrastructure.quote.Quote("GF129",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NHGRI")))));
        quotes.addQuote(new  org.broadinstitute.sequel.infrastructure.quote.Quote("GF130",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NCI")))));
        return new QuotesCache(quotes);
    }
}