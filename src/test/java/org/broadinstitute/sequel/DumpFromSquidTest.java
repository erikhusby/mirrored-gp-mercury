package org.broadinstitute.sequel;


import edu.mit.broad.prodinfo.squid.bettalims.entity.ReceptacleTransferEvent;
import edu.mit.broad.prodinfo.squid.entity.HibernateUtil;
import edu.mit.broad.prodinfo.squid.lc.entity.sample.LcSample;
import edu.mit.broad.prodinfo.squid.lcset.entity.LcsetCart;
import edu.mit.broad.prodinfo.squid.lcset.entity.LcsetCartSample;
import edu.mit.broad.prodinfo.squid.project.entity.CoverageType;
import edu.mit.broad.prodinfo.squid.project.entity.Initiative;
import edu.mit.broad.prodinfo.squid.project.entity.SeqProject;
import edu.mit.broad.prodinfo.squid.workrequest.entity.*;
import org.broadinstitute.sequel.entity.billing.Quote;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.project.*;
import org.broadinstitute.sequel.entity.queue.FIFOLabWorkQueue;
import org.broadinstitute.sequel.entity.queue.LabWorkQueueName;
import org.broadinstitute.sequel.entity.queue.LcSetParameters;
import org.broadinstitute.sequel.entity.run.IonSequencingTechnology;
import org.broadinstitute.sequel.entity.run.SequencingTechnology;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.sample.SampleSheetImpl;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.entity.workflow.WorkflowEngine;
import org.broadinstitute.sequel.infrastructure.bsp.*;
import org.broadinstitute.sequel.infrastructure.jira.JiraService;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.broadinstitute.sequel.infrastructure.quote.QuoteService;
import org.hibernate.Hibernate;
import org.junit.runner.RunWith;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.*;

public class DumpFromSquidTest extends WeldBooter {

    /**
     * Query for finding WRs that have 1 sample per project
     select distinct
     wrm.work_request_id,
     wr.created_on
     from
     work_request wr,
     work_request_material_descr wrmd,
     seq_project p,
     work_request_material wrm
     where
     wr.work_request_id = wrm.work_request_id
     and
     wrm.work_request_material_id = wrmd.work_request_material_id
     and
     p.project_id = wrmd.project_id
     and
     1 = (select count(distinct proj_dna.dna_source_id) from project_dna_source proj_dna where proj_dna.project_id = p.project_id)
     and
     1 < (select count(distinct wrmd2.project_id)
     from
     work_request_material_descr wrmd2,
     work_request_material wrm2
     where
     wrm2.work_request_material_id = wrmd2.work_request_material_id
     and
     wrm2.work_request_id = wrm.work_request_id)
     order by 2 desc

     */

    @Test
    public void get_from_squid() throws Exception {

        BSPSampleDataFetcher bspLookup = weldUtil.getFromContainer(BSPSampleDataFetcher.class);
        QuoteService quoteService = weldUtil.getFromContainer(QuoteService.class);
        JiraService jiraService = weldUtil.getFromContainer(JiraService.class);
        EntityManager em = Persistence.createEntityManagerFactory("SquidSequeL").createEntityManager();
        WorkRequest wr = em.find(WorkRequest.class,25030L);

        /*
        Set<Initiative> initiatves = new HashSet<Initiative>();

        Project project = new BasicProject("Foo",null);
        
        ProjectPlan plan = new ProjectPlan(project,"Squid WR " + wr.getWorkRequestId(),new WorkflowDescription(
                wr.getWorkRequestType().getWorkRequestDomain().getName() + " " + wr.getWorkRequestType().getName(),
                null,
                null,
                null));

        if (HibernateUtil.proxySafeIsInstance(wr, SolexaWorkRequest.class)) {
            String hybSelDesign = HibernateUtil.proxySafeCast(wr,SolexaWorkRequest.class).getHybSelDesign().getName();
            plan.addReagentDesign(new ReagentDesign(hybSelDesign,ReagentDesign.REAGENT_TYPE.BAIT));
        }

        
        Collection<LcSampleWorkReqCheckout> checkouts = LcSampleWorkReqCheckout.findByWorkRequest(wr,em);
        
        for (AbstractWorkRequestMaterial workRequestMaterial : wr.getWorkRequestMaterials()) {
            LcSample sam = workRequestMaterial.getLcSample();
            String derivedFrom = sam.getDerivedFrom();
            LcSample parentSample = sam.getParentSample();
            initiatves.add(workRequestMaterial.getProject().getInitiative());
            String bspSampleName = sam.getLsid().split("broadinstitute.org:bsp.prod.sample:")[1];
            BSPSampleDTO bspDTO = bspLookup.fetchSingleSampleFromBSP(bspSampleName);
            plan.setQuote(new Quote(workRequestMaterial.getQuoteId(),quoteService.getQuoteFromQuoteServer(workRequestMaterial.getQuoteId())));
            SampleSheet sampleSheet = new SampleSheetImpl();
            sampleSheet.addStartingSample(new BSPSample(bspSampleName,plan));
            plan.addStarter(new TwoDBarcodedTube(bspSampleName,sampleSheet));
            System.out.println(bspDTO.getCollection());

            for (LcSampleWorkReqCheckout checkout : checkouts) {
                if (checkout.getWorkRequestMaterial().equals(workRequestMaterial)) {
                    System.out.println("Checkout from " + workRequestMaterial.getLcSample().getRootLsid() + " to " + checkout.getAliquot().getLsid());
                }
                Collection<ReceptacleTransferEvent> transfers = checkout.getSeqContent().getReceptacle().getReceptacleTransferEventsThisAsSource();
                System.out.println(transfers.size() + " transfers from " + checkout.getAliquot().getLsid());
            }
        }

        */
        LcsetCart cart = em.find(LcsetCart.class,1422L);
        Map<String,BasicProject> projectsByName = new HashMap<String, BasicProject>();
        Map<LabEventName,PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();
        WorkflowDescription workflowDescription = new WorkflowDescription("HS","7.0",billableEvents, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);
        for (LcsetCartSample cartSample : cart.getLcsetCartSamples()) {
            String quoteAlpha = cartSample.getSequencingQuote();
            SeqProject cartProject = cartSample.getProject();
            LcSampleDetectionTechnology detTech = cart.getTechnology();
            CoverageType coverageType = cartProject.getCoverageType();
            
            if (!projectsByName.containsKey(cartProject.getName())) {
                CreateIssueResponse jiraResponse = jiraService.createIssue(Project.JIRA_PROJECT_PREFIX,
                        CreateIssueRequest.Fields.Issuetype.SequeL_Project,
                        cartProject.getInitiative().getInitiativeName(),
                        "Squid project " + cartProject.getProjectName());

                JiraTicket projectTicket = new JiraTicket(jiraService,jiraResponse.getTicketName(),jiraResponse.getId());
                BasicProject sequelProject = new BasicProject(cartProject.getName(),projectTicket);
                sequelProject.addProjectPlan(new ProjectPlan(sequelProject,
                        "Squid cart " + cart.getName(),
                        workflowDescription));
                projectsByName.put(cartProject.getName(),sequelProject);
            }
            BasicProject sequelProject = projectsByName.get(cartProject.getProjectName());
           
            ProjectPlan projectPlan = sequelProject.getAllPlans().iterator().next();
            String bspSampleName = cartSample.getLcSample().getLsid().split("broadinstitute.org:bsp.prod.sample:")[1];
            
            SampleSheet bspSampleSheet = new SampleSheetImpl();
            BSPSampleDTO bspSampleDto = bspLookup.fetchSingleSampleFromBSP(bspSampleName); 
            bspSampleSheet.addStartingSample(new BSPSample(bspSampleName,projectPlan,bspSampleDto));
            LabVessel bspTube = new TwoDBarcodedTube(bspSampleName,bspSampleSheet);
            
            projectPlan.addStarter(bspTube);
            projectPlan.addReagentDesign(new ReagentDesign(cart.getHybselBaitDesign().getName(), ReagentDesign.REAGENT_TYPE.BAIT));
        }

        // create jira tickets for each project

        // take all the projects from the cart and add them to the work queue.
        Collection<LabVessel> allStarters = new HashSet<LabVessel>();
        LcSetParameters lcSetParams = new LcSetParameters();
        FIFOLabWorkQueue<LcSetParameters> labQueue = new FIFOLabWorkQueue<LcSetParameters>(LabWorkQueueName.LC,new WorkflowEngine(),jiraService);
        for (BasicProject sequelProject : projectsByName.values()) {
            for (ProjectPlan projectPlan: sequelProject.getAllPlans()) {
                for (LabVessel starter : projectPlan.getStarters()) {
                    allStarters.add(starter);
                    SequencingPlanDetail sequencingPlan = new SequencingPlanDetail(new IonSequencingTechnology(5, IonSequencingTechnology.CHIP_TYPE.CHIP1),
                            new PercentXFoldCoverage(80,20),
                            projectPlan);
                    labQueue.add(starter,lcSetParams,sequencingPlan);
                }   
            }        
        }
        // now dequeue things from the work queue.
        labQueue.startWork(allStarters,lcSetParams,workflowDescription,new Person("rodney","Rodeny","Dangerfield"));
        
        // rack to rack transfer to aliquots

        // rack to plate transfers, one of which shows up in jira, one of which doesn't, one of which gets billed.
        // ui drill down for events and such

        // quant attachments: when uploaded, dump to project ticket.  Highlight quant out of range for
        // a single sample, rollover to show tumor/normal, case/control?


    }
}
