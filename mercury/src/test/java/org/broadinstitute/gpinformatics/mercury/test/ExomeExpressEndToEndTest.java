package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPPlatingRequestOptions;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPPlatingRequestResult;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPPlatingRequestService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPPlatingRequestServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.ControlWell;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraCustomFieldsUtil;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceTestProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SequencingTemplateFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.CrspPipelineUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.bsp.BSPSampleFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventRefDataFetcher;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.control.zims.ZimsIlluminaRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventName;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.queue.AliquotParameters;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PreFlightEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.entity.bsp.BSPSampleExportTest;
import org.broadinstitute.gpinformatics.mocks.EverythingYouAskForYouGetAndItsHuman;
import org.easymock.EasyMock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * A container free test of Exome Express
 */
@SuppressWarnings("OverlyCoupledClass")
@Test(groups = TestGroups.DATABASE_FREE)
public class ExomeExpressEndToEndTest {


    private final CrspPipelineUtils crspPipelineUtils = new CrspPipelineUtils(Deployment.DEV);

    // if this bombs because of a jira refresh, just switch it to JiraServiceTestProducer.stubInstance();
    // for integration test fun where we post things back to a real jira, try JiraServiceTestProducer.testInstance();
    private JiraService jiraServiceStub = JiraServiceTestProducer.stubInstance();

    private QuoteService quoteService = QuoteServiceProducer.stubInstance();

    /*
        Temporarily adding from ProjectPlanFromPassTest to move test case content along.
     */
    private static final long BAIT_ID = 5L;
    private static final String BAIT_DESIGN_NAME = "interesting genes";

    private final String BILLING_QUOTE = "DNA375";

    @Test(enabled = false)
    public void testAll() throws Exception {
        List<ProductOrderSample> productOrderSamples = new ArrayList<>();
        ProductOrder productOrder1 = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123", new Product(
                "Test product", new ProductFamily("Test product family"), "test", "1234", null, null, 10000, 20000, 100,
                40, null, null, true, Workflow.AGILENT_EXOME_EXPRESS, false, "agg type"),
                                                      new ResearchProject(101L, "Test RP", "Test synopsis",
                                                                          false,
                                                                          ResearchProject.RegulatoryDesignation.RESEARCH_ONLY));
        String jiraTicketKey = "PD0-1";
        productOrder1.setJiraTicketKey(jiraTicketKey);
        productOrder1.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        //        DirectedPass directedPass = null; //PassTestDataProducer.produceDirectedPass();

        // unconditionally forward all PASSes to Squid for storage
        //        passService.storePass(directedPass);

        // if this is an EE pass take it through the Mercury process:
        if (true /* R3_725 directedPass.isExomeExpress() */) {
            // PASS with quote IDs, price items (need PMBridge 2 for price items)

            // factory or something to convert from JAX-WS DTOs to entities (or refer to Squid PASS)
            // Check volume and concentration?  Or expose web services to allow PMBridge to check
            // labBatch
            // Project

            //            ResearchProject researchProject = null;
            //            if (directedPass.getResearchProject() != null)
            //                researchProject = pmBridgeService.getResearchProjectByID(directedPass.getResearchProject());

            //TODO change this to PassBackedProjectPlan
            //TODO MLC: tie in ResearchProject above
            //            BasicProject project = new BasicProject("ExomeExpressProject1", new JiraTicket());
            //            String runName = "theRun";

            //SGM:  This "Lane Number" is most likely not needed.  Retrieving Number of lanes from the Project Plan Details
            //            String laneNumber = "3";

            // BasicProjectPlan
            HashMap<LabEventName, QuotePriceItem> billableEvents = new HashMap<>();

            //            BasicProjectPlan projectPlan = new BasicProjectPlan(
            //                    project,
            //                    "ExomeExpressPlan1",
            //                    new WorkflowDescription("HybridSelection", billableEvents, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));

            /*
                Temporarily adding from ProjectPlanFromPassTest to move test case content along.
             */

            SampleDataFetcher bspDataFetcher = new SampleDataFetcher(new EverythingYouAskForYouGetAndItsHuman());
            //            BaitSetListResult baitsCache = new BaitSetListResult();
            //            BaitSet baitSet = new BaitSet();
            //            baitSet.setDesignName(BAIT_DESIGN_NAME);
            //            baitSet.setId(BAIT_ID);
            //            baitsCache.getBaitSetList().add(baitSet);

            // todo when R3_725 comes out, revert to looking this up via the pass
            QuotePriceItem
                    quotePriceItem = new QuotePriceItem("Illumina Sequencing", "1", "Illumina HiSeq Run 44 Base", "15",
                                                        "Bananas", "DNA Sequencing");
            //            WorkflowDescription workflowDescription = new WorkflowDescription("HybridSelection", quotePriceItem,
            //                    CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);

            //            PassBackedProjectPlan projectPlan = new PassBackedProjectPlan(directedPass, bspDataFetcher, baitsCache, quotePriceItem);
            //projectPlan.getWorkflowDescription().initFromFile("HybridSelectionV2.xml");
            //            projectPlan.getWorkflowDescription().initFromFile("HybridSelectionVisualParadigm.xml");

            //            Collection<WorkflowAnnotation> workflowAnnotations = projectPlan.getWorkflowDescription().getAnnotations("PondRegistration");

            boolean hasSeqLibAnnotation = false;
            //            for (WorkflowAnnotation workflowAnnotation : workflowAnnotations) {
            //                if (workflowAnnotation instanceof SequencingLibraryAnnotation) {
            //                    hasSeqLibAnnotation = true;
            //                }
            //            }
            //            Assert.assertTrue(hasSeqLibAnnotation);

            //            Assert.assertEquals(workflowAnnotations.size(), 1);

            // create batches for the pass.  todo add more samples to the pass.
            Collection<LabBatch> labBatches = null;//PassBatchUtil.createBatches(projectPlan, 2, "TESTBatch");
            //            Assert.assertFalse(labBatches.isEmpty());
            //            Assert.assertEquals(labBatches.size(), 1);

            LabBatch testLabBatch = labBatches.iterator().next();
            int STARTER_COUNT =
                    testLabBatch.getStartingBatchLabVessels().size(); //This probably will be labBatch size eventually

            // create the jira ticket for each batch.
            JiraTicket jiraTicket = null;

            // grab the jira custom field definitions
            Map<String, CustomFieldDefinition> requiredFieldsMap =
                    JiraCustomFieldsUtil.getRequiredLcSetFieldDefinitions(jiraServiceStub);
            Assert.assertFalse(requiredFieldsMap.isEmpty());
            Assert.assertEquals(requiredFieldsMap.size(), 9);

            CustomField workRequestCustomField = new CustomField(requiredFieldsMap.get(
                    JiraCustomFieldsUtil.WORK_REQUEST_IDS), "Work Request One Billion!");
            // kludge: expect stock samples to have a different field name (like "BSP STOCKS") when this goes live.  until then, we'll call it GSSR.
            StringBuilder stockSamplesBuilder = new StringBuilder();
            for (LabVessel starter : testLabBatch.getStartingBatchLabVessels()) {
                stockSamplesBuilder.append(" ").append(starter.getLabel());
            }
            CustomField stockSamplesCustomField = new CustomField(requiredFieldsMap.get(
                    JiraCustomFieldsUtil.GSSR_IDS), stockSamplesBuilder.toString());
            CustomField protocolCustomField = new CustomField(requiredFieldsMap.get(
                    JiraCustomFieldsUtil.PROTOCOL), "Protocol to take over the world");
            CustomField descriptionCustomField =
                    new CustomField(requiredFieldsMap.get(JiraCustomFieldsUtil.DESCRIPTION), "Pass ");

            Collection<CustomField> allCustomFields = new HashSet<>();
            allCustomFields.add(workRequestCustomField);
            allCustomFields.add(stockSamplesCustomField);
            allCustomFields.add(protocolCustomField);
            allCustomFields.add(descriptionCustomField);

            for (LabBatch labBatch : labBatches) {
                JiraIssue jira = jiraServiceStub.createIssue(null, //Project.JIRA_PROJECT_PREFIX,
                                                         "hrafal", CreateFields.IssueType.WHOLE_EXOME_HYBSEL,
                                                         labBatch.getBatchName(),
                                                         allCustomFields);
                Assert.assertNotNull(jira);
                Assert.assertNotNull(jira.getKey());
                jiraTicket = new JiraTicket(jiraServiceStub, jira.getKey());
                labBatch.setJiraTicket(jiraTicket);
                //labBatch.get
            }

            // how do we wire up the lab batch and/or jira ticket to the plating request?
            //TODO .. verify ProjectPlan.getStarters() are all starters... labbatch.getStarters() are ONLY starters for that batch ??
            //TODO .. if so BSPPlating should be by each LabBatch . LabBatch.getStarters() should be the group to do BSPPlating ??
            //Plating request to BSP
            //From projectPlan .. build BSPPlatingRequest objects
            //            Collection<Starter> starterStocks = testLabBatch.getStarters();
            //List<StartingSample> startingSamples = new ArrayList<StartingSample>();
            Map<MercurySample, AliquotParameters> starterMap = new HashMap<>();
            //            for (Starter stock : starterStocks) {
            //                starterMap.put((StartingSample) stock, new AliquotParameters(/*projectPlan, */1.9f, 1.6f));
            //            }

            BSPSampleFactory bspSampleFactory = new BSPSampleFactory();
            List<BSPPlatingRequest> bspRequests = bspSampleFactory.buildBSPPlatingRequests(starterMap);
            //            projectPlan.getPendingPlatingRequests().addAll(bspRequests);
            Assert.assertNotNull(bspRequests);
            //            Assert.assertEquals(bspRequests.size(), starterStocks.size(), "Plating Requests returned doesn't match the Starter count");

            //add the controls ??
            List<ControlWell> controls = new ArrayList<>();
            BSPPlatingRequestService bspPlatingService = new BSPPlatingRequestServiceStub();
            BSPPlatingRequestOptions options = bspPlatingService.getBSPPlatingRequestDefaultOptions();
            BSPPlatingRequestResult platingResult = bspPlatingService.issueBSPPlatingRequest(options, bspRequests,
                                                                                             controls, "sampath",
                                                                                             "EE-BSP-PLATING-1",
                                                                                             "BSP Plating Exome Express Test",
                                                                                             "Solexa", "EE-TEST-1");
            Assert.assertNotNull(platingResult); //just Stub any way
            BSPPlatingReceipt platingReceipt = bspSampleFactory.buildPlatingReceipt(bspRequests, platingResult);
            Assert.assertNotNull(platingReceipt);
            Assert.assertEquals(platingReceipt.getPlatingRequests().size(), bspRequests.size(),
                                "BSP Plating Requests in receipt & passed requests count does not match");

            Assert.assertEquals(platingReceipt.getPlatingRequests().size(), STARTER_COUNT,
                                "Started with " + STARTER_COUNT + " samples. BSP Plating requests should be "
                                + STARTER_COUNT);
            //Test BSP Plating EXPORT
            BSPSampleExportTest.runBSPExportTest(platingReceipt, testLabBatch);
            //new GSSRSampleKitRequest();

            //bspPlatingReceipt.getPlatingRequests().iterator().next().
            //            Collection<Starter> starters = projectPlan.getStarters();
            Map<String, LabVessel> stockSampleAliquotMap = new HashMap<>();
            //            for (Starter starter : starters) {
            //                LabVessel aliquot = projectPlan.getAliquotForStarter(starter);
            //                Assert.assertNotNull(aliquot);
            //                stockSampleAliquotMap.put(starter.getLabel(), aliquot);
            //            }

            // factory to convert to entities
            // Receive plastic through kiosk
            // web service callable from Squid kiosk
            // Hybrid Selection Messaging (both systems?)  UPDATE:  SGM: plan to do Hybrid Selection Messaging to just
            //Mercury for Exome Express

            // deck query for barcodes
            // (deck query for workflow)
            // deck sends message, check workflow
            LabEventFactory labEventFactory = new LabEventFactory(null, null);
            labEventFactory.setLabEventRefDataFetcher(new LabEventRefDataFetcher() {
                @Override
                public BspUser getOperator(String userId) {
                    return new BSPUserList.QADudeUser("Test", BSPManagerFactoryStub.QA_DUDE_USER_ID);
                }

                @Override
                public BspUser getOperator(Long bspUserId) {
                    return new BSPUserList.QADudeUser("Test", BSPManagerFactoryStub.QA_DUDE_USER_ID);
                }

                @Override
                public LabBatch getLabBatch(String labBatchName) {
                    return null;
                }
            });

            BucketDao mockBucketDao = EasyMock.createMock(BucketDao.class);

            LabBatchEjb labBatchEJB = new LabBatchEjb();
            labBatchEJB.setJiraService(jiraServiceStub);

            LabVesselDao tubeDao = EasyMock.createNiceMock(LabVesselDao.class);
            labBatchEJB.setTubeDao(tubeDao);

            LabBatchDao labBatchDao = EasyMock.createNiceMock(LabBatchDao.class);
            labBatchEJB.setLabBatchDao(labBatchDao);

            ProductOrderDao mockProductOrderDao = Mockito.mock(ProductOrderDao.class);
            Mockito.when(mockProductOrderDao.findByBusinessKey(Mockito.anyString())).thenAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {

                    Object[] arguments = invocationOnMock.getArguments();

                    return ProductOrderTestFactory.createDummyProductOrder((String) arguments[0]);
                }
            });
            labBatchEJB.setProductOrderDao(mockProductOrderDao);

            ReworkEjb reworkEjb = EasyMock.createNiceMock(ReworkEjb.class);
            BucketDao bucketDao = EasyMock.createNiceMock(BucketDao.class);

//            EasyMock.expect(mockBucketDao.findByName(EasyMock.eq(LabEventType.SHEARING_BUCKET.getName())))
//                    .andReturn(new LabEventTest.MockBucket(new WorkflowStepDef(LabEventType.SHEARING_BUCKET
//                            .getName()), jiraTicket.getTicketName()));

            EasyMock.replay(mockBucketDao, labBatchDao, tubeDao, reworkEjb, bucketDao);


            TemplateEngine templateEngine = new TemplateEngine();
            templateEngine.postConstruct();
            LabEventHandler labEventHandler = new LabEventHandler();

            BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);
            Map<String, BarcodedTube> mapBarcodeToTube = new HashMap<>();

            for (Map.Entry<String, LabVessel> stockToAliquotEntry : stockSampleAliquotMap.entrySet()) {
                mapBarcodeToTube.put(stockToAliquotEntry.getValue().getLabel(),
                                     (BarcodedTube) stockToAliquotEntry.getValue());
            }

            PreFlightEntityBuilder preFlightEntityBuilder = new PreFlightEntityBuilder(
                    bettaLimsMessageTestFactory, labEventFactory, labEventHandler, mapBarcodeToTube,
                    "testPrefix");//.invoke();

            ShearingEntityBuilder shearingEntityBuilder = new ShearingEntityBuilder(
                    mapBarcodeToTube, preFlightEntityBuilder.getTubeFormation(), bettaLimsMessageTestFactory,
                    labEventFactory,
                    labEventHandler, preFlightEntityBuilder.getRackBarcode(), "testPrefix").invoke();

            LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                    new LibraryConstructionEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, labEventHandler,
                                                         shearingEntityBuilder.getShearingCleanupPlate(),
                                                         shearingEntityBuilder.getShearCleanPlateBarcode(),
                                                         shearingEntityBuilder.getShearingPlate(),
                                                         mapBarcodeToTube.size(), "testPrefix",
                                                         LibraryConstructionEntityBuilder.Indexing.DUAL,
                                                         LibraryConstructionJaxbBuilder.PondType.REGULAR).invoke();

            HybridSelectionEntityBuilder hybridSelectionEntityBuilder =
                    new HybridSelectionEntityBuilder(bettaLimsMessageTestFactory, labEventFactory,
                                                     labEventHandler,
                                                     libraryConstructionEntityBuilder.getPondRegRack(),
                                                     libraryConstructionEntityBuilder
                                                             .getPondRegRackBarcode(),
                                                     libraryConstructionEntityBuilder
                                                             .getPondRegTubeBarcodes(), "testPrefix").invoke(false);

            TubeFormation pondRack = libraryConstructionEntityBuilder.getPondRegRack();
            Assert.assertEquals(pondRack.getSampleInstancesV2().size(), 2);

            // make sure that the pond sample instances contain the starters from the project plan.
            //            for (Starter starter : projectPlan.getStarters()) {
            //                LabVessel aliquot = projectPlan.getAliquotForStarter(starter);
            //                for (SampleInstance aliquotSampleInstance : aliquot.getSampleInstances()) {
            //                    boolean foundIt = false;
            //                    for (SampleInstance pondSampleInstance : pondRack.getSampleInstances()) {
            //                        if (aliquotSampleInstance.getStartingSample().equals(pondSampleInstance.getStartingSample())) {
            //                            foundIt = true;
            //                            System.out.println("Pond has " + pondSampleInstance.getStartingSample().getLabel());
            //                        }
            //                    }
            //                    Assert.assertTrue(foundIt);
            //                }
            //            }

            // make sure that the pond sample instances contain the starters from the project plan.
            //            for (Starter starter : projectPlan.getStarters()) {
            //                LabVessel aliquot = projectPlan.getAliquotForStarter(starter);
            //                for (SampleInstance aliquotSampleInstance : aliquot.getSampleInstances()) {
            //                    boolean foundIt = false;
            //                    for (SampleInstance pondSampleInstance : hybridSelectionEntityBuilder.getNormCatchRack().getSampleInstances()) {
            //                        if (aliquotSampleInstance.getStartingSample().equals(pondSampleInstance.getStartingSample())) {
            //                            foundIt = true;
            //                            System.out.println("Norm has " + pondSampleInstance.getStartingSample().getLabel());
            //                        }
            //                    }
            //                    Assert.assertTrue(foundIt);
            //                }
            //
            //            }

            QtpEntityBuilder qtpEntityBuilder = new QtpEntityBuilder(
                    bettaLimsMessageTestFactory, labEventFactory, labEventHandler,
                    Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchRack()),
                    Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchRackBarcode()),
                    Collections.singletonList(hybridSelectionEntityBuilder.getNormCatchBarcodes()),
                    hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                    "testPrefix");
            qtpEntityBuilder.invoke();

            TubeFormation poolingResult = qtpEntityBuilder.getDenatureRack();
            BarcodedTube denatureTube =
                    qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);


            LabBatch fctBatch = new LabBatch("FCT-3", LabBatch.LabBatchType.FCT,
                    IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, denatureTube, BigDecimal.valueOf(12.33f));


            HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                    new HiSeq2500FlowcellEntityBuilder(bettaLimsMessageTestFactory,
                                                       labEventFactory, labEventHandler,
                                                       qtpEntityBuilder.getDenatureRack(),
                                                       fctBatch.getBusinessKey(),
                                                       "testPrefix", "", ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL,
                                                       "", 2);

            // LC metrics - upload page?
            // LabVessel.addMetric?
            // Post "work done" to Quote Server
            // MockQuoteService.registerNewWork

            final BarcodedTube currEntry = poolingResult.getContainerRole().getVesselAtPosition(VesselPosition.A01);


            int numStartersFromSampleInstances = 0;
            final Collection<String> aliquotsFromProjectPlan = new HashSet<>();
            //            for (Starter starter : projectPlan.getStarters()) {
            //                final LabVessel aliquot = projectPlan.getAliquotForStarter(starter);
            //                for (SampleInstance sampleInstance : aliquot.getSampleInstances()) {
            //                    aliquotsFromProjectPlan.add(sampleInstance.getStartingSample().getLabel());
            //                }
            //            }
            for (SampleInstanceV2 sampleInstance : currEntry.getSampleInstancesV2()) {
                Assert.assertTrue(aliquotsFromProjectPlan.contains(sampleInstance.getEarliestMercurySampleName()));
                numStartersFromSampleInstances++;
                //                Assert.assertEquals(projectPlan, sampleInstance.getSingleProjectPlan());
            }

            //            Assert.assertEquals(startersFromProjectPlan.size(), numStartersFromSampleInstances);

            // todo arz fix semantics: is it "single sample ancestor" or "sequencing library"?
//            Map<MercurySample, Collection<LabVessel>> singleSampleAncestors =
//                    poolingResult.getContainerRole().getSingleSampleAncestors(VesselPosition.A01);

            //            for (Starter starter : projectPlan.getStarters()) {
            //                LabVessel aliquot = projectPlan.getAliquotForStarter(starter);
            //                Assert.assertNotNull(aliquot);
            //
            //                Assert.assertEquals(aliquot.getSampleInstances().size(), 1);
            //
            //                for (SampleInstance aliquotSampleInstance : aliquot.getSampleInstances()) {
            //                    StartingSample aliquotStartingSample = aliquotSampleInstance.getStartingSample();
            //                    Collection<LabVessel> sequencingLibs = singleSampleAncestors.get(aliquotStartingSample);
            //                    Assert.assertEquals(sequencingLibs.size(), 1);
            //                    Assert.assertTrue(sequencingLibs.iterator().next().getLabel().startsWith(LabEventTest.POND_REGISTRATION_TUBE_PREFIX));
            //                }
            //            }
//            Assert.assertEquals(singleSampleAncestors.size(), 2);

            Collection<LabBatch> nearestBatches = poolingResult.getContainerRole().getNearestLabBatches(null);
            Assert.assertEquals(nearestBatches.size(), 1);
            LabBatch labBatch = nearestBatches.iterator().next();

            Assert.assertEquals(labBatch.getJiraTicket(), jiraTicket);

            //            Quote quoteDTO = projectPlan.getQuoteDTO(quoteService);

            //            Assert.assertNotNull(quoteDTO);

            //            R3_725
            //            Assert.assertEquals(projectPlan.getWorkflowDescription().getPriceItem().getName(),directedPass.getFundingInformation().getGspPriceItem().getName());

            //            for (Starter starter : labBatch.getStarters()) {
            //                ProjectPlan batchPlan = labBatch.getProjectPlan();
            //                Assert.assertEquals(projectPlan, batchPlan);
            //                batchPlan.doBilling(starter, labBatch, quoteService);
            //            }

            // todo add call to quote server to get all work done during the time period and verify
            // that our work was included: https://iwww.broadinstitute.org/blogs/quote/?page_id=210


            // Designation in Squid (7 lanes Squid + 1 lane Mercury)
            // Call Squid web service to add to queue (lanes, read length)
            // Register run
            IlluminaSequencingRunFactory illuminaSequencingRunFactory =
                    new IlluminaSequencingRunFactory(EasyMock.createNiceMock(JiraCommentUtil.class));
            IlluminaSequencingRun illuminaSequencingRun;
            try {
                illuminaSequencingRun = illuminaSequencingRunFactory.buildDbFree(new SolexaRunBean(
                        hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell().getCartridgeBarcode(), "Run1", new Date(),
                        "SL-HAL",
                        File.createTempFile("RunDir", ".txt").getAbsolutePath(), null), hiSeq2500FlowcellEntityBuilder
                                                                                         .getIlluminaFlowcell());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Assert.assertNotNull(illuminaSequencingRun.getSampleCartridge(),
                                 "No registered flowcell");

            // ZIMS
            ProductOrderDao productOrderDao = Mockito.mock(ProductOrderDao.class);
            Mockito.when(productOrderDao.findByBusinessKey(Mockito.anyString())).then(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {

                    Object[] arguments = invocationOnMock.getArguments();

                    return ProductOrderTestFactory.createDummyProductOrder((String) arguments[0]);
                }
            });

            AttributeArchetypeDao attributeArchetypeDao = Mockito.mock(AttributeArchetypeDao.class);
            Mockito.when(attributeArchetypeDao.findWorkflowMetadata(Mockito.anyString())).then(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return null;
                }
            });

            // Method calls on factory will always use our list of flowcell designations.
            // Need not put anything into it for now.
            final List<FlowcellDesignation> flowcellDesignations = new ArrayList<>();
            FlowcellDesignationEjb testFlowcellDesignationEjb = new FlowcellDesignationEjb(){
                @Override
                public List<FlowcellDesignation> getFlowcellDesignations(LabBatch fct) {
                return flowcellDesignations;
            }
                @Override
                public List<FlowcellDesignation> getFlowcellDesignations(Collection<LabVessel> loadingTubes) {
                    return flowcellDesignations;
                }
            };

            ZimsIlluminaRunFactory zimsIlluminaRunFactory = new ZimsIlluminaRunFactory(new SampleDataFetcher(),
                    null, new SequencingTemplateFactory(), productOrderDao, crspPipelineUtils,
                    testFlowcellDesignationEjb, attributeArchetypeDao);
            ZimsIlluminaRun zimsRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(illuminaSequencingRun);

            // how to populate BspSampleData?  Ease of use from EL suggests an entity that can load itself, but this
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
                        //                        Assert.assertEquals(library.getProject(), directedPass.getResearchProject());
                        // todo how to get from pass bait set id to bait name?
                        //                        Assert.assertEquals(library.getBaitSetName(),directedPass.getBaitSetID());
                        // todo how to get from pass organism id to organism name?
                        //                        Assert.assertEquals(library.getOrganism(), directedPass.getProjectInformation().getOrganismID());
                        //                        for (Sample sample : directedPass.getSampleDetailsInformation().getSample()) {
                        //                            // todo probably wrong, not sure whether the sample id is lsid or stock id
                        //                            if (library.getLsid().equals("lsid:" + sample.getBspSampleID())) {
                        //                                foundSample = true;
                        //                            }
                        //                        }
                        //                        Assert.assertTrue(foundSample);

                        // todo single sample ancestor comparison
                    }
                }
            }
            Assert.assertTrue(foundLane);

            EasyMock.verify(mockBucketDao);
        }
    }
}
