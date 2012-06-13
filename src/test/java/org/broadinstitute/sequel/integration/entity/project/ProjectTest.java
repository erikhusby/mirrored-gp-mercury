package org.broadinstitute.sequel.integration.entity.project;

import org.broadinstitute.sequel.bsp.EverythingYouAskForYouGetAndItsHuman;
import org.broadinstitute.sequel.entity.billing.Quote;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.project.BasicProject;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.ReagentDesign;
import org.broadinstitute.sequel.entity.project.SequencingPlanDetail;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.project.XFoldCoverage;
import org.broadinstitute.sequel.entity.queue.FIFOLabWorkQueue;
import org.broadinstitute.sequel.entity.queue.JiraLabWorkQueueResponse;
import org.broadinstitute.sequel.entity.queue.LabWorkQueue;
import org.broadinstitute.sequel.entity.queue.LabWorkQueueName;
import org.broadinstitute.sequel.entity.queue.LabWorkQueueParameters;
import org.broadinstitute.sequel.entity.queue.LcSetParameters;
import org.broadinstitute.sequel.entity.run.IonSequencingTechnology;
import org.broadinstitute.sequel.entity.run.SequencingTechnology;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.infrastructure.jira.JiraService;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.sequel.infrastructure.quote.Funding;
import org.broadinstitute.sequel.infrastructure.quote.FundingLevel;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.broadinstitute.sequel.infrastructure.quote.QuoteFunding;
import org.broadinstitute.sequel.infrastructure.quote.Quotes;
import org.broadinstitute.sequel.infrastructure.quote.QuotesCache;
import org.broadinstitute.sequel.integration.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;
import static org.testng.Assert.*;

public class ProjectTest extends Arquillian {

    @Inject
    private JiraService jiraService;

    @Inject
    private BSPSampleDataFetcher bspFetcher;

    @Deployment
    public static WebArchive buildSequeLWar() {
        return DeploymentBuilder.buildSequelWarWithAlternatives(EverythingYouAskForYouGetAndItsHuman.class);
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void test_project_jira() throws Exception {
        CreateIssueResponse response = jiraService.createIssue(Project.JIRA_PROJECT_PREFIX,
                CreateIssueRequest.Fields.Issuetype.SequeL_Project,
                "Test run by " + System.getProperty("user.name") + " on " + new SimpleDateFormat("yyyy/MM/dd").format(new Date(System.currentTimeMillis())),
                "Do lots of sequencing");
        assertNotNull(response);
        JiraTicket ticket = new JiraTicket(jiraService,response.getTicketName(),response.getId());
        Project project = new BasicProject(ticket.getTicketName(),ticket);
        project.addJiraComment("Comment added via Project");
        assertTrue(response.getTicketName().startsWith(Project.JIRA_PROJECT_PREFIX));

        // todo how to verify the comment was added?
    }
    
    @Test(groups = {EXTERNAL_INTEGRATION})
    public void test_simple_project() {
        Project project = projectManagerCreatesProject(jiraService);
        projectManagerAddsFundingSourceToProject(project,"NHGRI");
        ProjectPlan plan = projectManagerAddsProjectPlan(project);
        ReagentDesign bait = projectManagerAddsBait(plan);
        SequencingPlanDetail sequencingDetail = projectManagerAddsSequencingDetails(plan);
        
        // PM would choose samples, or run a search
        // to find samples, maybe by querying the PASS,
        // maybe by running a stored search in BSP,
        // or maybe by dumping in a list of root
        // sample ids.
        LabVessel starter1 = makeRootSample("SM-1P3WY",plan, bspFetcher);
        LabVessel starter2 = makeRootSample("SM-1P3XN",plan, bspFetcher);
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

        assertEquals(30, ((XFoldCoverage) sequencingDetail.getCoverageGoal()).getCoverageDepth());


        Collection<Quote> quotes = project.getAvailableQuotes();

        assertEquals(2,quotes.size());

        // PM would pick the queue from a drop down,
        // filtered by the {@link WorkflowDescription}?
        FIFOLabWorkQueue<LcSetParameters> lcWorkQueue = createLabWorkQueue(jiraService);

        assertTrue(lcWorkQueue.isEmpty());

        LcSetParameters lcSetParameters = new LcSetParameters();
//        Workflow workflowInstance = null;
        assertTrue(lcWorkQueue.isEmpty());
        for (LabVessel starter : allStarters) {
            projectManagerEnquesLabWork(starter,plan,lcSetParameters,lcWorkQueue);
        }

        assertFalse(lcWorkQueue.isEmpty());



        JiraTicket jiraTicket = labStaffStartsWork(allStarters,
                plan.getWorkflowDescription(),
                lcWorkQueue,
                lcSetParameters);

        assertTrue(jiraTicket.getTicketName().startsWith(plan.getWorkflowDescription().getJiraProjectPrefix()));
        
        assertTrue(lcWorkQueue.isEmpty());

        // todo jmt what is the current equivalent?
//        assertEquals("work has stated", workflowInstance.getState().getState());

        postSomethingFunToJira(jiraTicket,allStarters,project);
        
        Collection<LabVessel> reworkVessels = new HashSet<LabVessel>();
        reworkVessels.add(starter1);

        // post notice of the LC set ticket back to the project
        JiraTicket jiraTicketForRework = labStaffStartsWork(reworkVessels,
                plan.getWorkflowDescription(),
                lcWorkQueue,
                lcSetParameters);

        // oh dear, one sample ended up getting reworked.
        postSomethingFunToJira(jiraTicketForRework,reworkVessels,project);
        
        assertEquals(2,plan.getJiraTickets().size());
        
        assertTrue(plan.getJiraTickets().contains(jiraTicket));
        assertTrue(plan.getJiraTickets().contains(jiraTicketForRework));

        assertEquals(starter1.getJiraTickets().size(),2);
        assertEquals(1,starter2.getJiraTickets().size());
        
        assertTrue(starter1.getJiraTickets().contains(jiraTicket));
        assertTrue(starter1.getJiraTickets().contains(jiraTicketForRework));
        
        assertTrue(starter2.getJiraTickets().contains(jiraTicket));
        assertFalse(starter2.getJiraTickets().contains(jiraTicketForRework));

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
    private Project projectManagerCreatesProject(JiraService jiraService) {
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
        Project legacyProject = new BasicProject(projectName,new JiraTicket(jiraService,jiraResponse.getTicketName(),jiraResponse.getId()));
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
        WorkflowDescription workflow = new WorkflowDescription("HybridSelection", billableEvents,CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);
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

        // todo: instead of a bogus TwoDBarcodedTube for the root, lookup BSP
        // container information inside a BSPVessel object, most of whose
        // methods throw exceptions that say "Hey, I'm from BSP, you can't do that!"
        LabVessel starter = new TwoDBarcodedTube(sampleName, new BSPSample(sampleName,projectPlan,bspFetcher.fetchSingleSampleFromBSP(sampleName)));
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
    
    private FIFOLabWorkQueue<LcSetParameters> createLabWorkQueue(JiraService jiraService) {
        FIFOLabWorkQueue<LcSetParameters> labWorkQueue = new FIFOLabWorkQueue<LcSetParameters>(LabWorkQueueName.LC,jiraService);
        return labWorkQueue;
    }

    /**
     * The UI would have a {@link ProjectPlan} context.  The PM adds
     * a starter to the plan and chooses the {@link LcSetParameters}
     * work queue parameters exactly as they are currently expressed
     * in Jira.  In other words, the "queueing" UI is a thin wrapper
     * around Jira that adds SequeL project context.
     *
     * Using the createmeta jira webservice, we can discover the
     * fields required for the queue so that when a PM adds stuff
     * to a queue, they pick the appropriate fields.
     * @param starter
     * @param projectPlan
     * @param queueParameters
     * @param labWorkQueue
     * @return
     */
    private void projectManagerEnquesLabWork(LabVessel starter,
                                             ProjectPlan projectPlan,
                                             LabWorkQueueParameters queueParameters,
                                             LabWorkQueue labWorkQueue) {
       
        labWorkQueue.add(starter,queueParameters,projectPlan.getWorkflowDescription(),null);

        assertFalse(labWorkQueue.isEmpty());
        
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
     * @return
     */
    private JiraTicket labStaffStartsWork(Collection<LabVessel> vessels,
                                    WorkflowDescription workflowDescription,
                                    FIFOLabWorkQueue<LcSetParameters> labWorkQueue,
                                    LcSetParameters lcSetParameters) {
        Person tonyHawk = new Person("tony","Tony","Hawk");
        JiraLabWorkQueueResponse queueResponse = labWorkQueue.startWork(vessels,
                    lcSetParameters,
                    workflowDescription,
                    tonyHawk);
        return queueResponse.getJiraTicket();
    }

    private void projectManagerAddsFundingSourceToProject(Project project,
                                                          String grantName) {
        project.addGrant(grantName);
        QuotesCache quotesCache = buildQuotesCache();
        for (org.broadinstitute.sequel.infrastructure.quote.Quote quoteDTO: quotesCache.getQuotes()) {
            if (grantName.equalsIgnoreCase(quoteDTO.getQuoteFunding().getFundingLevel().getFunding().getGrantDescription())) {
                project.addAvailableQuote(new Quote(quoteDTO.getAlphanumericId(),quoteDTO));
            }
        }
    }

    
    private QuotesCache buildQuotesCache() {
        Quotes quotes = new Quotes();
        quotes.addQuote(new org.broadinstitute.sequel.infrastructure.quote.Quote("GF128",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NHGRI")))));
        quotes.addQuote(new  org.broadinstitute.sequel.infrastructure.quote.Quote("GF129",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NHGRI")))));
        quotes.addQuote(new  org.broadinstitute.sequel.infrastructure.quote.Quote("GF130",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NCI")))));
        return new QuotesCache(quotes);
    }
}