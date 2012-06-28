package org.broadinstitute.sequel.test;

import org.broadinstitute.sequel.boundary.BaitSet;
import org.broadinstitute.sequel.boundary.BaitSetListResult;
import org.broadinstitute.sequel.boundary.DirectedPass;
import org.broadinstitute.sequel.boundary.GSSRSampleKitRequest;
import org.broadinstitute.sequel.boundary.designation.LibraryRegistrationSOAPService;
import org.broadinstitute.sequel.boundary.designation.LibraryRegistrationSOAPServiceStub;
import org.broadinstitute.sequel.boundary.designation.RegistrationJaxbConverter;
import org.broadinstitute.sequel.boundary.pass.PassTestDataProducer;
import org.broadinstitute.sequel.boundary.pmbridge.PMBridgeService;
import org.broadinstitute.sequel.boundary.pmbridge.PMBridgeServiceStub;
import org.broadinstitute.sequel.boundary.pmbridge.data.ResearchProject;
import org.broadinstitute.sequel.boundary.squid.SequelLibrary;
import org.broadinstitute.sequel.bsp.EverythingYouAskForYouGetAndItsHuman;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.control.pass.PassBatchUtil;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.PassBackedProjectPlan;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.Starter;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.RackOfTubes;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.entity.vessel.VesselPosition;
import org.broadinstitute.sequel.entity.workflow.LabBatch;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.infrastructure.jira.DummyJiraService;
import org.broadinstitute.sequel.infrastructure.jira.JiraService;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.sequel.infrastructure.quote.MockQuoteService;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.broadinstitute.sequel.test.entity.bsp.BSPSampleExportTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

/**
 * A container free test of Exome Express
 */
public class ExomeExpressEndToEndTest {


    // if this test was running in a container the test data might be injected, though that would really only work well
    // in the one test per class scenario, or at most one test per PASS type per class...

    // @Inject
    // @TestData
    // private DirectedPass directedPass;

    // Assuming the jndi-config branch were to be merged for a container version of this test:
    //
    // @Inject
    // PassService passService;

    // for non-container test:
    //
    // PassService passService = new PassServiceStub();


//    @Inject
    LibraryRegistrationSOAPService registrationSOAPService = new LibraryRegistrationSOAPServiceStub();

    // @Inject
    private PMBridgeService pmBridgeService = new PMBridgeServiceStub();

    // @Inject
    private JiraService jiraService = new DummyJiraService();

    /*
        Temporarily adding from ProjectPlanFromPassTest to move test case content along.
     */
    private final long BAIT_ID = 5;
    private final String BAIT_DESIGN_NAME = "interesting genes";




    @Test(groups = {DATABASE_FREE}, enabled = true)
    public void testAll() throws Exception {

        DirectedPass directedPass = PassTestDataProducer.instance().produceDirectedPass();

        // unconditionally forward all PASSes to Squid for storage
        // passService.storePass(directedPass);

        // if this is an EE pass take it through the SequeL process:
        if (directedPass.isExomeExpress()) {
            // PASS with quote IDs, price items (need PMBridge 2 for price items)



            // factory or something to convert from JAX-WS DTOs to entities (or refer to Squid PASS)
            // Check volume and concentration?  Or expose web services to allow PMBridge to check
            // labBatch
            // Project

            ResearchProject researchProject = null;
            if ( directedPass.getResearchProject() != null )
                researchProject = pmBridgeService.getResearchProjectByID(directedPass.getResearchProject());



            //TODO SGM: change this to PassBackedProjectPlan
            //TODO MLC: tie in ResearchProject above
//            BasicProject project = new BasicProject("ExomeExpressProject1", new JiraTicket());
            String runName = "theRun";

            //SGM:  This "Lane Number" is most likely not needed.  Retrieving Number of lanes from the Project Plan Details
//            String laneNumber = "3";

            // BasicProjectPlan
            HashMap<LabEventName, PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();

//            BasicProjectPlan projectPlan = new BasicProjectPlan(
//                    project,
//                    "ExomeExpressPlan1",
//                    new WorkflowDescription("HybridSelection", billableEvents, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));

            /*
                Temporarily adding from ProjectPlanFromPassTest to move test case content along.
             */

            BSPSampleDataFetcher bspDataFetcher = new BSPSampleDataFetcher(new EverythingYouAskForYouGetAndItsHuman());
            BaitSetListResult baitsCache = new BaitSetListResult();
            BaitSet baitSet = new BaitSet();
            baitSet.setDesignName(BAIT_DESIGN_NAME);
            baitSet.setId(BAIT_ID);
            baitsCache.getBaitSetList().add(baitSet);

            PassBackedProjectPlan projectPlan = new PassBackedProjectPlan(directedPass,bspDataFetcher,new MockQuoteService(),baitsCache);
            projectPlan.getWorkflowDescription().initFromFile("HybridSelectionV2.bpmn");


            // create batches for the pass.  todo add more samples to the pass.
            Collection<LabBatch> labBatches = PassBatchUtil.createBatches(projectPlan,2,"TESTBatch");
            Assert.assertFalse(labBatches.isEmpty());
            Assert.assertEquals(labBatches.size(),1);

            // create the jira ticket for each batch.
            for (LabBatch labBatch : labBatches) {
                CreateIssueResponse createResponse = jiraService.createIssue(Project.JIRA_PROJECT_PREFIX,
                        CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel,
                        labBatch.getBatchName(),
                        "Pass " + projectPlan.getPass().getProjectInformation().getPassNumber());
                Assert.assertNotNull(createResponse);
                Assert.assertNotNull(createResponse.getTicketName());

                //add jira issue to Project
                projectPlan.addJiraTicket(labBatch.getJiraTicket());
            }

            // how do we wire up the lab batch and/or jira ticket to the plating request?

            // Plating request to BSP
            // BSP Client mock to get receipt?
            // Plating export from BSP
            new GSSRSampleKitRequest();

            //Test BSP Plating EXPORT
            BSPSampleExportTest.BSPPlatingExportEntityBuilder bspExportEntityBuilder = new BSPSampleExportTest.BSPPlatingExportEntityBuilder(projectPlan);

            bspExportEntityBuilder.runTest();
            //bspPlatingReceipt.getPlatingRequests().iterator().next().
            Collection<Starter> starters = projectPlan.getStarters();
            Map<String, LabVessel> stockSampleAliquotMap = new HashMap<String, LabVessel>();
            for (Starter starter : starters) {
                LabVessel aliquot = projectPlan.getAliquot(starter);
                Assert.assertNotNull(aliquot);
                stockSampleAliquotMap.put(starter.getLabel(), aliquot);
            }

            // factory to convert to entities
            // Receive plastic through kiosk
            // web service callable from Squid kiosk
            // Hybrid Selection Messaging (both systems?)  UPDATE:  SGM: plan to do Hybrid Selection Messaging to just
            //SequeL for Exome Express

            // deck query for barcodes
            // (deck query for workflow)
            // deck sends message, check workflow
            LabEventFactory labEventFactory = new LabEventFactory();
            labEventFactory.setPersonDAO(new PersonDAO());
            LabEventHandler labEventHandler = new LabEventHandler();
            BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
            Map<String, TwoDBarcodedTube> mapBarcodeToTube = new HashMap<String, TwoDBarcodedTube>();

            for (Map.Entry<String, LabVessel> stockToAliquotEntry : stockSampleAliquotMap.entrySet()) {
                mapBarcodeToTube.put(stockToAliquotEntry.getValue().getLabel(),(TwoDBarcodedTube)stockToAliquotEntry.getValue());
            }

            LabEventTest.PreFlightEntityBuilder preFlightEntityBuilder = new LabEventTest.PreFlightEntityBuilder(
                    projectPlan.getWorkflowDescription(), bettaLimsMessageFactory, labEventFactory, labEventHandler,
                    mapBarcodeToTube);//.invoke();

            LabEventTest.ShearingEntityBuilder shearingEntityBuilder = new LabEventTest.ShearingEntityBuilder(
                    projectPlan.getWorkflowDescription(), mapBarcodeToTube, bettaLimsMessageFactory, labEventFactory,
                    labEventHandler, preFlightEntityBuilder.getRackBarcode()).invoke();

            LabEventTest.LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = new LabEventTest.LibraryConstructionEntityBuilder(
                    projectPlan.getWorkflowDescription(), bettaLimsMessageFactory, labEventFactory, labEventHandler,
                    shearingEntityBuilder.getShearingCleanupPlate(), shearingEntityBuilder.getShearCleanPlateBarcode(),
                    shearingEntityBuilder.getShearingPlate(), mapBarcodeToTube.size()).invoke();

            LabEventTest.HybridSelectionEntityBuilder hybridSelectionEntityBuilder = new LabEventTest.HybridSelectionEntityBuilder(
                    projectPlan.getWorkflowDescription(), bettaLimsMessageFactory, labEventFactory, labEventHandler,
                    libraryConstructionEntityBuilder.getPondRegRack(), libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                    libraryConstructionEntityBuilder.getPondRegTubeBarcodes()).invoke();

            LabEventTest.QtpEntityBuilder qtpEntityBuilder = new LabEventTest.QtpEntityBuilder(projectPlan.getWorkflowDescription(),
                    bettaLimsMessageFactory, labEventFactory, labEventHandler,
                    hybridSelectionEntityBuilder.getNormCatchRack(), hybridSelectionEntityBuilder.getNormCatchRackBarcode(),
                    hybridSelectionEntityBuilder.getNormCatchBarcodes(), hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes());
            qtpEntityBuilder.invoke();

            RackOfTubes poolingResult = qtpEntityBuilder.getDenatureRack();

            // LC metrics - upload page?
            // LabVessel.addMetric?
            // Post "work done" to Quote Server
            // MockQuoteService.registerNewWork


            final TwoDBarcodedTube currEntry = poolingResult.getVesselContainer().getVesselAtPosition(VesselPosition.A01);

            final SequelLibrary registerLibrary = RegistrationJaxbConverter.squidify(currEntry, projectPlan);

            final Collection<Starter> startersFromProjectPlan = projectPlan.getStarters();

            int numStartersFromSampleInstances = 0;
            final Collection<String> aliquotsFromProjectPlan = new HashSet<String>();
            for (Starter starter : projectPlan.getStarters()) {
                final LabVessel aliquot = projectPlan.getAliquot(starter);
                for (SampleInstance sampleInstance : aliquot.getSampleInstances()) {
                    aliquotsFromProjectPlan.add(sampleInstance.getStartingSample().getLabel());
                }
            }
            for (SampleInstance sampleInstance : currEntry.getSampleInstances()) {
                Assert.assertTrue(aliquotsFromProjectPlan.contains(sampleInstance.getStartingSample().getLabel()));
                numStartersFromSampleInstances++;
                Assert.assertEquals(projectPlan,sampleInstance.getSingleProjectPlan());
            }

            Assert.assertEquals(startersFromProjectPlan.size(), numStartersFromSampleInstances);

            registrationSOAPService.registerSequeLLibrary(registerLibrary);

            registrationSOAPService.registerForDesignation(registerLibrary.getLibraryName(), projectPlan, true);



            // Designation in Squid (7 lanes Squid + 1 lane SequeL)
            // Call Squid web service to add to queue (lanes, read length)
            // ZIMS
            /*
            IlluminaRunResource illuminaRunResource = new IlluminaRunResource();

            ZimsIlluminaRun zimsRun = illuminaRunResource.getRun(runName);

            assertNotNull(zimsRun);
            boolean foundLane = false;
            boolean foundSample = false;
            for (ZimsIlluminaChamber zimsLane : zimsRun.getLanes()) {
                if (laneNumber.equals(zimsLane)) {
                    foundLane = true;
                    Collection<LibraryBean> libraries = zimsLane.getLibraries();
                    assertFalse(libraries.isEmpty());
                    for (LibraryBean library : libraries) {
                        assertEquals(library.getProject(),sourcePass.getResearchProject());
                        // todo how to get from pass bait set id to bait name?
                        assertEquals(library.getBaitSetName(),sourcePass.getBaitSetID());
                        // todo how to get from pass organism id to organism name?
                        assertEquals(library.getOrganism(), sourcePass.getProjectInformation().getOrganismID());
                        for (Sample sample : sourcePass.getSampleDetailsInformation().getSample()) {
                            // todo probably wrong, not sure whether the sample id is lsid or stock id
                            if (library.getLsid().equals(sample)) {
                                foundSample = true;
                            }
                        }
                        assertTrue(foundSample);
                        // todo single sample ancestor comparison
                    }
                }
            }
            assertTrue(foundLane);
            */
        }
    }
}
