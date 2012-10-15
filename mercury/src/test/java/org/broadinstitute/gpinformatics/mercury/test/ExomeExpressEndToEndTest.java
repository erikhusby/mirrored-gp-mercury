package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.mercury.boundary.BaitSet;
import org.broadinstitute.gpinformatics.mercury.boundary.BaitSetListResult;
import org.broadinstitute.gpinformatics.mercury.boundary.DirectedPass;
import org.broadinstitute.gpinformatics.mercury.boundary.Sample;
import org.broadinstitute.gpinformatics.mercury.boundary.designation.LibraryRegistrationSOAPService;
import org.broadinstitute.gpinformatics.mercury.boundary.designation.LibraryRegistrationSOAPServiceProducer;
import org.broadinstitute.gpinformatics.mercury.boundary.designation.RegistrationJaxbConverter;
import org.broadinstitute.gpinformatics.mercury.boundary.pass.PassServiceProducer;
import org.broadinstitute.gpinformatics.mercury.boundary.pass.PassTestDataProducer;
import org.broadinstitute.gpinformatics.mercury.boundary.pmbridge.PMBridgeService;
import org.broadinstitute.gpinformatics.mercury.boundary.pmbridge.PMBridgeServiceProducer;
import org.broadinstitute.gpinformatics.mercury.boundary.pmbridge.data.ResearchProject;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.squid.SequelLibrary;
import org.broadinstitute.gpinformatics.mercury.bsp.EverythingYouAskForYouGetAndItsHuman;
import org.broadinstitute.gpinformatics.mercury.control.dao.bsp.BSPSampleFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.pass.PassBatchUtil;
import org.broadinstitute.gpinformatics.mercury.control.pass.PassService;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.zims.LibraryBeanFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPStartingSample;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.mercury.entity.project.*;
import org.broadinstitute.gpinformatics.mercury.entity.queue.AliquotParameters;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.sample.StartingSample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.SequencingLibraryAnnotation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowAnnotation;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.*;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraCustomFieldsUtil;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceProducer;
import org.broadinstitute.gpinformatics.mercury.test.entity.bsp.BSPSampleExportTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;

/**
 * A container free test of Exome Express
 */
@SuppressWarnings("OverlyCoupledClass")
public class ExomeExpressEndToEndTest {

    private LibraryRegistrationSOAPService registrationSOAPService = LibraryRegistrationSOAPServiceProducer.stubInstance();

    private PMBridgeService pmBridgeService = PMBridgeServiceProducer.stubInstance();

    private PassService passService = PassServiceProducer.stubInstance();

    // if this bombs because of a jira refresh, just switch it to JiraServiceProducer.stubInstance();
    // for integration test fun where we post things back to a real jira, try JiraServiceProducer.testInstance();
    private JiraService jiraService = JiraServiceProducer.stubInstance();

    private QuoteService quoteService = QuoteServiceProducer.stubInstance();

    /*
        Temporarily adding from ProjectPlanFromPassTest to move test case content along.
     */
    private static final long BAIT_ID = 5L;
    private static final String BAIT_DESIGN_NAME = "interesting genes";

    private final String BILLING_QUOTE = "DNA375";


    @Test(groups = {DATABASE_FREE}, enabled = true)
    public void testAll() throws Exception {

        DirectedPass directedPass = PassTestDataProducer.produceDirectedPass();

        // unconditionally forward all PASSes to Squid for storage
        passService.storePass(directedPass);

        // if this is an EE pass take it through the Mercury process:
        if (true /* R3_725 directedPass.isExomeExpress() */) {
            // PASS with quote IDs, price items (need PMBridge 2 for price items)


            // factory or something to convert from JAX-WS DTOs to entities (or refer to Squid PASS)
            // Check volume and concentration?  Or expose web services to allow PMBridge to check
            // labBatch
            // Project

            ResearchProject researchProject = null;
            if (directedPass.getResearchProject() != null)
                researchProject = pmBridgeService.getResearchProjectByID(directedPass.getResearchProject());


            //TODO SGM: change this to PassBackedProjectPlan
            //TODO MLC: tie in ResearchProject above
//            BasicProject project = new BasicProject("ExomeExpressProject1", new JiraTicket());
//            String runName = "theRun";

            //SGM:  This "Lane Number" is most likely not needed.  Retrieving Number of lanes from the Project Plan Details
//            String laneNumber = "3";

            // BasicProjectPlan
//            HashMap<LabEventName, PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();

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

            // todo when R3_725 comes out, revert to looking this up via the pass
            PriceItem priceItem = new PriceItem("Illumina Sequencing", "1", "Illumina HiSeq Run 44 Base", "15", "Bananas", "DNA Sequencing");

            PassBackedProjectPlan projectPlan = new PassBackedProjectPlan(directedPass, bspDataFetcher, baitsCache, priceItem);
            //projectPlan.getWorkflowDescription().initFromFile("HybridSelectionV2.xml");
            projectPlan.getWorkflowDescription().initFromFile("HybridSelectionVisualParadigm.xml");

            Collection<WorkflowAnnotation> workflowAnnotations = projectPlan.getWorkflowDescription().getAnnotations("PondRegistration");

            boolean hasSeqLibAnnotation = false;
            for (WorkflowAnnotation workflowAnnotation : workflowAnnotations) {
                if (workflowAnnotation instanceof SequencingLibraryAnnotation) {
                    hasSeqLibAnnotation = true;
                }
            }
            Assert.assertTrue(hasSeqLibAnnotation);

            Assert.assertEquals(workflowAnnotations.size(), 1);

            // create batches for the pass.  todo add more samples to the pass.
            Collection<LabBatch> labBatches = PassBatchUtil.createBatches(projectPlan, 2, "TESTBatch");
            Assert.assertFalse(labBatches.isEmpty());
            Assert.assertEquals(labBatches.size(), 1);

            LabBatch testLabBatch = labBatches.iterator().next();
            int STARTER_COUNT = testLabBatch.getStarters().size(); //This probably will be labBatch size eventually

            // create the jira ticket for each batch.
            JiraTicket jiraTicket = null;

            // grab the jira custom field definitions
            final Map<String, CustomFieldDefinition> requiredFieldsMap = JiraCustomFieldsUtil.getRequiredLcSetFieldDefinitions(jiraService);
            Assert.assertFalse(requiredFieldsMap.isEmpty());
            Assert.assertEquals(requiredFieldsMap.size(), 10);


            final CustomField workRequestCustomField = new CustomField(requiredFieldsMap.get(JiraCustomFieldsUtil.WORK_REQUEST_IDS), "Work Request One Billion!");
            // kludge: expect stock samples to have a different field name (like "BSP STOCKS") when this goes live.  until then, we'll call it GSSR.
            final StringBuilder stockSamplesBuilder = new StringBuilder();
            for (Starter starter : testLabBatch.getStarters()) {
                stockSamplesBuilder.append(" ").append(starter.getLabel());
            }
            final CustomField stockSamplesCustomField = new CustomField(requiredFieldsMap.get(JiraCustomFieldsUtil.GSSR_IDS), stockSamplesBuilder.toString());
            final CustomField protocolCustomField = new CustomField(requiredFieldsMap.get(JiraCustomFieldsUtil.PROTOCOL), "Protocol to take over the world");

            final Collection<CustomField> allCustomFields = new HashSet<CustomField>();
            allCustomFields.add(workRequestCustomField);
            allCustomFields.add(stockSamplesCustomField);
            allCustomFields.add(protocolCustomField);

            for (LabBatch labBatch : labBatches) {
                CreateIssueResponse createResponse = jiraService.createIssue(Project.JIRA_PROJECT_PREFIX,
                        CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel,
                        labBatch.getBatchName(),
                        "Pass " + projectPlan.getPass().getProjectInformation().getPassNumber(), allCustomFields);
                Assert.assertNotNull(createResponse);
                Assert.assertNotNull(createResponse.getTicketName());
                jiraTicket = new JiraTicket(jiraService, createResponse.getTicketName(), createResponse.getId());
                labBatch.setJiraTicket(jiraTicket);
                //labBatch.get
            }

            // how do we wire up the lab batch and/or jira ticket to the plating request?
            //TODO .. verify ProjectPlan.getStarters() are all starters... labbatch.getStarters() are ONLY starters for that batch ??
            //TODO .. if so BSPPlating should be by each LabBatch . LabBatch.getStarters() should be the group to do BSPPlating ??
            //Plating request to BSP
            //From projectPlan .. build BSPPlatingRequest objects
            Collection<Starter> starterStocks = testLabBatch.getStarters();
            //List<StartingSample> startingSamples = new ArrayList<StartingSample>();
            Map<StartingSample, AliquotParameters> starterMap = new HashMap<StartingSample, AliquotParameters>();
            for (Starter stock : starterStocks) {
                starterMap.put((StartingSample) stock, new AliquotParameters(projectPlan, 1.9f, 1.6f));
            }

            BSPSampleFactory bspSampleFactory = new BSPSampleFactory();
            List<BSPPlatingRequest> bspRequests = bspSampleFactory.buildBSPPlatingRequests(starterMap);
            projectPlan.getPendingPlatingRequests().addAll(bspRequests);
            Assert.assertNotNull(bspRequests);
            Assert.assertEquals(bspRequests.size(), starterStocks.size(), "Plating Requests returned doesn't match the Starter count");

            //add the controls ??
            List<ControlWell> controls = new ArrayList<ControlWell>();
            BSPPlatingRequestService bspPlatingService = new BSPPlatingRequestServiceStub();
            BSPPlatingRequestOptions options = bspPlatingService.getBSPPlatingRequestDefaultOptions();
            BSPPlatingRequestResult platingResult = bspPlatingService.issueBSPPlatingRequest(options, bspRequests, controls, "sampath",
                    "EE-BSP-PLATING-1", "BSP Plating Exome Express Test", "Solexa", "EE-TEST-1");
            Assert.assertNotNull(platingResult); //just Stub any way
            BSPPlatingReceipt platingReceipt = bspSampleFactory.buildPlatingReceipt(bspRequests, platingResult);
            Assert.assertNotNull(platingReceipt);
            Assert.assertEquals(platingReceipt.getPlatingRequests().size(), bspRequests.size() , "BSP Plating Requests in receipt & passed requests count does not match");

            Assert.assertEquals(platingReceipt.getPlatingRequests().size() , STARTER_COUNT, "Started with " + STARTER_COUNT + " samples. BSP Plating requests should be " + STARTER_COUNT);
            //Test BSP Plating EXPORT
            BSPSampleExportTest.runBSPExportTest(platingReceipt, testLabBatch);
            //new GSSRSampleKitRequest();

            //bspPlatingReceipt.getPlatingRequests().iterator().next().
            Collection<Starter> starters = projectPlan.getStarters();
            Map<String, LabVessel> stockSampleAliquotMap = new HashMap<String, LabVessel>();
            for (Starter starter : starters) {
                LabVessel aliquot = projectPlan.getAliquotForStarter(starter);
                Assert.assertNotNull(aliquot);
                stockSampleAliquotMap.put(starter.getLabel(), aliquot);
            }

            // factory to convert to entities
            // Receive plastic through kiosk
            // web service callable from Squid kiosk
            // Hybrid Selection Messaging (both systems?)  UPDATE:  SGM: plan to do Hybrid Selection Messaging to just
            //Mercury for Exome Express

            // deck query for barcodes
            // (deck query for workflow)
            // deck sends message, check workflow
            LabEventFactory labEventFactory = new LabEventFactory();
            labEventFactory.setLabEventRefDataFetcher(new LabEventFactory.LabEventRefDataFetcher() {
                @Override
                public Person getOperator(String userId) {
                    return new Person(userId);
                }

                @Override
                public LabBatch getLabBatch(String labBatchName) {
                    return null;
                }
            });
            LabEventHandler labEventHandler = new LabEventHandler();
            BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
            Map<String, TwoDBarcodedTube> mapBarcodeToTube = new HashMap<String, TwoDBarcodedTube>();

            for (Map.Entry<String, LabVessel> stockToAliquotEntry : stockSampleAliquotMap.entrySet()) {
                mapBarcodeToTube.put(stockToAliquotEntry.getValue().getLabel(), (TwoDBarcodedTube) stockToAliquotEntry.getValue());
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
            Assert.assertEquals(pondRack.getSampleInstances().size(), 2);

            // make sure that the pond sample instances contain the starters from the project plan.
            for (Starter starter : projectPlan.getStarters()) {
                LabVessel aliquot = projectPlan.getAliquotForStarter(starter);
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
                LabVessel aliquot = projectPlan.getAliquotForStarter(starter);
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
                final LabVessel aliquot = projectPlan.getAliquotForStarter(starter);
                for (SampleInstance sampleInstance : aliquot.getSampleInstances()) {
                    aliquotsFromProjectPlan.add(sampleInstance.getStartingSample().getLabel());
                }
            }
            for (SampleInstance sampleInstance : currEntry.getSampleInstances()) {
                Assert.assertTrue(aliquotsFromProjectPlan.contains(sampleInstance.getStartingSample().getLabel()));
                numStartersFromSampleInstances++;
                Assert.assertEquals(projectPlan, sampleInstance.getSingleProjectPlan());
            }

            Assert.assertEquals(startersFromProjectPlan.size(), numStartersFromSampleInstances);

            // todo arz fix semantics: is it "single sample ancestor" or "sequencing library"?
            Map<StartingSample, Collection<LabVessel>> singleSampleAncestors = poolingResult.getVesselContainer().getSingleSampleAncestors(VesselPosition.A01);

            for (Starter starter : projectPlan.getStarters()) {
                LabVessel aliquot = projectPlan.getAliquotForStarter(starter);
                Assert.assertNotNull(aliquot);

                Assert.assertEquals(aliquot.getSampleInstances().size(), 1);

                for (SampleInstance aliquotSampleInstance : aliquot.getSampleInstances()) {
                    StartingSample aliquotStartingSample = aliquotSampleInstance.getStartingSample();
                    Collection<LabVessel> sequencingLibs = singleSampleAncestors.get(aliquotStartingSample);
                    Assert.assertEquals(sequencingLibs.size(), 1);
                    Assert.assertTrue(sequencingLibs.iterator().next().getLabel().startsWith(LabEventTest.POND_REGISTRATION_TUBE_PREFIX));
                }
            }
            Assert.assertEquals(singleSampleAncestors.size(), 2);

            Collection<LabBatch> nearestBatches = poolingResult.getVesselContainer().getNearestLabBatches(VesselPosition.A01);
            Assert.assertEquals(nearestBatches.size(), 1);
            LabBatch labBatch = nearestBatches.iterator().next();

            Assert.assertEquals(labBatch.getJiraTicket(), jiraTicket);

            Quote quoteDTO = projectPlan.getQuoteDTO(quoteService);

            Assert.assertNotNull(quoteDTO);

//            R3_725
//            Assert.assertEquals(projectPlan.getWorkflowDescription().getPriceItem().getName(),directedPass.getFundingInformation().getGspPriceItem().getName());

            for (Starter starter : labBatch.getStarters()) {
                ProjectPlan batchPlan = labBatch.getProjectPlan();
                Assert.assertEquals(projectPlan, batchPlan);
                batchPlan.doBilling(starter, labBatch, quoteService);
            }

            // todo add call to quote server to get all work done during the time period and verify
            // that our work was included: https://iwww.broadinstitute.org/blogs/quote/?page_id=210


            registrationSOAPService.registerSequeLLibrary(registerLibrary);

            registrationSOAPService.registerForDesignation(registerLibrary.getLibraryName(), projectPlan, true);

            // Designation in Squid (7 lanes Squid + 1 lane Mercury)
            // Call Squid web service to add to queue (lanes, read length)
            // Register run
            IlluminaSequencingRunFactory illuminaSequencingRunFactory = new IlluminaSequencingRunFactory();
            IlluminaSequencingRun illuminaSequencingRun;
            try {
                illuminaSequencingRun = illuminaSequencingRunFactory.buildDbFree(
                        new SolexaRunBean(qtpEntityBuilder.getIlluminaFlowcell().getCartridgeBarcode(), "Run1", new Date(), "SL-HAL",
                                File.createTempFile("RunDir", ".txt").getAbsolutePath(), null), qtpEntityBuilder.getIlluminaFlowcell());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Assert.assertNotNull(illuminaSequencingRun.getSampleCartridge().iterator().next(), "No registered flowcell");

            // We're container-free, so we have to populate the BSPSampleDTO ourselves
            for (Starter starter : projectPlan.getStarters()) {
                BSPSampleAuthorityTwoDTube aliquot = (BSPSampleAuthorityTwoDTube) projectPlan.getAliquotForStarter(starter);
                BSPStartingSample bspStartingSample = (BSPStartingSample) aliquot.getAliquot();
                bspStartingSample.setBspDTO(new BSPSampleDTO("1", "", "", "", "", "", "", "", "", "", "lsid:" + bspStartingSample.getSampleName(),
                                                             "", "", "","", "", "", "",""));
            }

            // ZIMS
            LibraryBeanFactory libraryBeanFactory = new LibraryBeanFactory();
            ZimsIlluminaRun zimsRun = libraryBeanFactory.buildLibraries(illuminaSequencingRun);

            // how to populate BSPSampleDTO?  Ease of use from EL suggests an entity that can load itself, but this
            // would require injecting a service, or using a singleton

            Assert.assertNotNull(zimsRun);
            boolean foundLane = false;
            boolean foundSample = false;
            for (ZimsIlluminaChamber zimsLane : zimsRun.getLanes()) {
                if ("1".equals(zimsLane.getName())) {
                    foundLane = true;
                    Collection<LibraryBean> libraries = zimsLane.getLibraries();
                    Assert.assertFalse(libraries.isEmpty());
                    for (LibraryBean library : libraries) {
                        Assert.assertEquals(library.getProject(), directedPass.getResearchProject());
                        // todo how to get from pass bait set id to bait name?
//                        Assert.assertEquals(library.getBaitSetName(),directedPass.getBaitSetID());
                        // todo how to get from pass organism id to organism name?
//                        Assert.assertEquals(library.getOrganism(), directedPass.getProjectInformation().getOrganismID());
                        for (Sample sample : directedPass.getSampleDetailsInformation().getSample()) {
                            // todo probably wrong, not sure whether the sample id is lsid or stock id
                            if (library.getLsid().equals("lsid:" + sample.getBspSampleID())) {
                                foundSample = true;
                            }
                        }
//                        Assert.assertTrue(foundSample);

                        // todo single sample ancestor comparison
                    }
                }
            }
            Assert.assertTrue(foundLane);
        }
    }
}
