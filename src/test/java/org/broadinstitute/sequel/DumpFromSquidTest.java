package org.broadinstitute.sequel;


/*
import edu.mit.broad.prodinfo.squid.bettalims.TransferEntityTraverser;
import edu.mit.broad.prodinfo.squid.bettalims.entity.Plate;
import edu.mit.broad.prodinfo.squid.bettalims.entity.ReceptacleTransferEvent;
import edu.mit.broad.prodinfo.squid.bettalims.entity.StationEvent;
import edu.mit.broad.prodinfo.squid.bettalims.entity.WellDescription;
import edu.mit.broad.prodinfo.squid.entity.HibernateUtil;
import edu.mit.broad.prodinfo.squid.lc.entity.receptacle.Receptacle;
import edu.mit.broad.prodinfo.squid.lc.entity.sample.LcSample;
import edu.mit.broad.prodinfo.squid.lcset.entity.LcsetCart;
import edu.mit.broad.prodinfo.squid.lcset.entity.LcsetCartSample;
import edu.mit.broad.prodinfo.squid.party.entity.Party;
import edu.mit.broad.prodinfo.squid.party.entity.PriviledgedParty;
import edu.mit.broad.prodinfo.squid.project.entity.CoverageType;
import edu.mit.broad.prodinfo.squid.project.entity.Initiative;
import edu.mit.broad.prodinfo.squid.project.entity.SeqProject;
import edu.mit.broad.prodinfo.squid.quant.entity.LibraryQuant;
import edu.mit.broad.prodinfo.squid.services.labopsjira.samples.LcSetSampleDataServiceImpl;
import edu.mit.broad.prodinfo.squid.workrequest.entity.*;
*/

/**
 * Commented out because it's an experiment
 * aimed at exporting squid data into
 * sequel.  Look at sequel's pom.xml for
 * instructions on how to import squid.
 */
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

    /*
    @Test
    public void get_from_squid() throws Exception {

        BSPSampleDataFetcher bspLookup = weldUtil.getFromContainer(BSPSampleDataFetcher.class);
        QuoteService quoteService = weldUtil.getFromContainer(QuoteService.class);
        JiraService jiraService = weldUtil.getFromContainer(JiraService.class);
        EntityManager em = Persistence.createEntityManagerFactory("SquidSequeL").createEntityManager();
        WorkRequest wr = em.find(WorkRequest.class,25030L);

        LcsetCart cart = em.find(LcsetCart.class,1422L);
        Map<String,BasicProject> projectsByName = new HashMap<String, BasicProject>();
        Map<LabEventName,PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();
        Map<String,Collection<LibraryQuant>> quantsForBspSample = new HashMap<String, Collection<LibraryQuant>>();
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
                Party person = cartProject.getCreateUser().getParty();
                BasicProject sequelProject = new BasicProject(new Person(person.getName(),person.getName(),person.getName()),
                        cartProject.getName(),
                        projectTicket);
                sequelProject.addProjectPlan(new ProjectPlan(sequelProject,
                        "Squid cart " + cart.getName(),
                        workflowDescription));
                projectsByName.put(cartProject.getName(),sequelProject);
            }
            BasicProject sequelProject = projectsByName.get(cartProject.getProjectName());
           
            ProjectPlan projectPlan = sequelProject.getAllPlans().iterator().next();
            String bspSampleName = cartSample.getLcSample().getLsid().split("broadinstitute.org:bsp.prod.sample:")[1];
            
            SampleSheet bspSampleSheet = new SampleSheet();
            BSPSampleDTO bspSampleDto = bspLookup.fetchSingleSampleFromBSP(bspSampleName); 
            bspSampleSheet.addStartingSample(new BSPSample(bspSampleName,projectPlan,bspSampleDto));
            LabVessel bspTube = new TwoDBarcodedTube(bspSampleName,bspSampleSheet);
            
            projectPlan.addStarter(bspTube);
            projectPlan.addReagentDesign(new ReagentDesign(cart.getHybselBaitDesign().getName(), ReagentDesign.REAGENT_TYPE.BAIT));

            for (LcSampleWorkReqCheckout checkout: LcSampleWorkReqCheckout.findByWorkRequest(cart.getWorkRequestId(), em)){
                String bspName = checkout.getWorkRequestMaterial().getLcSample().getLsid().split("broadinstitute.org:bsp.prod.sample:")[1];
                if (bspName.equals(bspSampleName)) {
                    Receptacle aliquot = checkout.getSeqContent().getReceptacle();
                    FindAllQuantsCriteria quantFinder = new FindAllQuantsCriteria(aliquot);
                    new TransferEntityTraverser().recurseChildTransfers(aliquot,quantFinder);
                    Collection<LibraryQuant> quants = quantFinder.getLibraryQuants();
                    System.out.println("Found " + quants.size() + " quants for " +bspName);

                    quantsForBspSample.put(bspName,quants);
                }
            }
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

        for (LabVessel starter : allStarters) {
            for (SampleInstance sampleInstance : starter.getSampleInstances()) {
                String sampleName = sampleInstance.getStartingSample().getSampleName();
                if (quantsForBspSample.containsKey(sampleName)) {
                    sampleInstance.getSingleProjectPlan().getProject().addJiraComment("Quant " + quantsForBspSample.get(sampleName).iterator().next().getQuantValue());
                }
            }    
        }


    }

    // copied from LcSetService
    private static class FindAllQuantsCriteria implements TransferEntityTraverser.TransferCriteria {

        private Map<Long, LibraryQuant> libraryQuants = new HashMap<Long, LibraryQuant>();


        public FindAllQuantsCriteria(Receptacle receptacle) {
            for (LibraryQuant quant : receptacle.getLibraryQuants())
                libraryQuants.put(quant.getQuantId(), quant);
        }


        @Override
        public boolean evaluatePlateWell(Plate plate, WellDescription wellDescription, int hopCount, StationEvent stationEvent) {
            return true;
        }

        @Override
        public boolean evaluateReceptacle(Receptacle receptacle, int hopCount, StationEvent stationEvent) {

            for (LibraryQuant quant : receptacle.getLibraryQuants()) {
                libraryQuants.put(quant.getQuantId(), quant);
            }

            return true;
        }

        public Collection<LibraryQuant> getLibraryQuants() {
            return libraryQuants.values();
        }
    }
    */
}
