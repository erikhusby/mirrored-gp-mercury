package org.broadinstitute.sequel.test;

import org.broadinstitute.sequel.boundary.BaitSet;
import org.broadinstitute.sequel.boundary.BaitSetListResult;
import org.broadinstitute.sequel.boundary.DirectedPass;
import org.broadinstitute.sequel.boundary.GSSRSampleKitRequest;
import org.broadinstitute.sequel.boundary.designation.LibraryRegistrationSOAPService;
import org.broadinstitute.sequel.boundary.designation.LibraryRegistrationSOAPServiceStub;
import org.broadinstitute.sequel.boundary.designation.RegistrationJaxbConverter;
import org.broadinstitute.sequel.boundary.pass.PassServiceProducer;
import org.broadinstitute.sequel.boundary.pass.PassTestDataProducer;
import org.broadinstitute.sequel.boundary.pmbridge.PMBridgeService;
import org.broadinstitute.sequel.boundary.pmbridge.PMBridgeServiceProducer;
import org.broadinstitute.sequel.boundary.pmbridge.data.ResearchProject;
import org.broadinstitute.sequel.boundary.squid.SequelLibrary;
import org.broadinstitute.sequel.bsp.EverythingYouAskForYouGetAndItsHuman;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.control.pass.PassBatchUtil;
import org.broadinstitute.sequel.control.pass.PassService;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.PassBackedProjectPlan;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.Starter;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.RackOfTubes;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.entity.vessel.VesselPosition;
import org.broadinstitute.sequel.entity.workflow.LabBatch;
import org.broadinstitute.sequel.entity.workflow.WorkflowAnnotation;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.infrastructure.jira.DummyJiraService;
import org.broadinstitute.sequel.infrastructure.jira.JiraService;
import org.broadinstitute.sequel.infrastructure.jira.JiraServiceImpl;
import org.broadinstitute.sequel.infrastructure.jira.TestLabObsJira;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.sequel.infrastructure.quote.MockQuoteService;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.broadinstitute.sequel.test.entity.bsp.BSPSampleExportTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

/**
 * A container free test of Exome Express
 */
public class ExomeExpressEndToEndTest {

    LibraryRegistrationSOAPService registrationSOAPService = new LibraryRegistrationSOAPServiceStub();

    private PMBridgeService pmBridgeService = PMBridgeServiceProducer.produceStub();

    private PassService passService = PassServiceProducer.produceStub();

    private JiraService jiraService = new DummyJiraService(); // fun to play with this instead, and look @ jira in your web browser: new JiraServiceImpl(new TestLabObsJira());

    /*
        Temporarily adding from ProjectPlanFromPassTest to move test case content along.
     */
    private final long BAIT_ID = 5;
    private final String BAIT_DESIGN_NAME = "interesting genes";




    @Test(groups = {DATABASE_FREE}, enabled = true)
    public void testAll() throws Exception {

        DirectedPass directedPass = PassTestDataProducer.produceDirectedPass();

        // unconditionally forward all PASSes to Squid for storage
        passService.storePass(directedPass);

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
            //projectPlan.getWorkflowDescription().initFromFile("HybridSelectionV2.xml");
            projectPlan.getWorkflowDescription().initFromFile("HybridSelectionVisualParadigm.xml");

            Collection<WorkflowAnnotation> workflowAnnotations = projectPlan.getWorkflowDescription().getAnnotations("PondRegistration");

            Assert.assertTrue(workflowAnnotations.contains(WorkflowAnnotation.SINGLE_SAMPLE_LIBRARY));
            Assert.assertEquals(workflowAnnotations.size(),1);

            // create batches for the pass.  todo add more samples to the pass.
            Collection<LabBatch> labBatches = PassBatchUtil.createBatches(projectPlan,2,"TESTBatch");
            Assert.assertFalse(labBatches.isEmpty());
            Assert.assertEquals(labBatches.size(),1);

            // create the jira ticket for each batch.
            JiraTicket jiraTicket = null;
            for (LabBatch labBatch : labBatches) {
                CreateIssueResponse createResponse = jiraService.createIssue(Project.JIRA_PROJECT_PREFIX,
                        CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel,
                        labBatch.getBatchName(),
                        "Pass " + projectPlan.getPass().getProjectInformation().getPassNumber());
                Assert.assertNotNull(createResponse);
                Assert.assertNotNull(createResponse.getTicketName());
                jiraTicket = new JiraTicket(jiraService,createResponse.getTicketName(),createResponse.getId());
                labBatch.setJiraTicket(jiraTicket);
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

            RackOfTubes pondRack = libraryConstructionEntityBuilder.getPondRegRack();
            Assert.assertEquals(pondRack.getSampleInstances().size(),2);

            // make sure that the pond sample instances contain the starters from the project plan.
            for (Starter starter : projectPlan.getStarters()) {
                LabVessel aliquot = projectPlan.getAliquot(starter);
                for (SampleInstance aliquotSampleInstance : aliquot.getSampleInstances()) {
                    boolean foundIt = false;
                    for (SampleInstance pondSampleInstance : pondRack.getSampleInstances()) {
                        if (aliquotSampleInstance.getStartingSample().equals(pondSampleInstance.getStartingSample())) {
                            foundIt = true;
                            System.out.println("Pond has " + pondSampleInstance.getStartingSample().getLabel());
                        }
                    }
                    Assert.assertTrue(foundIt);
                }
            }

            // make sure that the pond sample instances contain the starters from the project plan.
            for (Starter starter : projectPlan.getStarters()) {
                LabVessel aliquot = projectPlan.getAliquot(starter);
                for (SampleInstance aliquotSampleInstance : aliquot.getSampleInstances()) {
                    boolean foundIt = false;
                    for (SampleInstance pondSampleInstance : hybridSelectionEntityBuilder.getNormCatchRack().getSampleInstances()) {
                        if (aliquotSampleInstance.getStartingSample().equals(pondSampleInstance.getStartingSample())) {
                            foundIt = true;
                            System.out.println("Norm has " + pondSampleInstance.getStartingSample().getLabel());
                        }
                    }
                    Assert.assertTrue(foundIt);
                }

            }

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

            // todo arz fix semantics: is it "single sample ancestor" or "sequencing library"?
            Map<StartingSample,Collection<LabVessel>> singleSampleAncestors = poolingResult.getVesselContainer().getSingleSampleAncestors(VesselPosition.A01);

            for (Starter starter : projectPlan.getStarters()) {
                LabVessel aliquot = projectPlan.getAliquot(starter);
                Assert.assertNotNull(aliquot);

                Assert.assertEquals(aliquot.getSampleInstances().size(),1);

                for (SampleInstance aliquotSampleInstance : aliquot.getSampleInstances()) {
                    StartingSample aliquotStartingSample = aliquotSampleInstance.getStartingSample();
                    Collection<LabVessel> sequencingLibs = singleSampleAncestors.get(aliquotStartingSample);
                    Assert.assertEquals(sequencingLibs.size(),1);
                    Assert.assertTrue(sequencingLibs.iterator().next().getLabel().startsWith(LabEventTest.POND_REGISTRATION_TUBE_PREFIX));
                }
            }
            Assert.assertEquals(singleSampleAncestors.size(),2);

            Collection<LabBatch> nearestBatches = poolingResult.getVesselContainer().getNearestLabBatches(VesselPosition.A01);
            Assert.assertEquals(nearestBatches.size(),1);
            LabBatch labBatch = nearestBatches.iterator().next();

            Assert.assertEquals(labBatch.getJiraTicket(),jiraTicket);

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
