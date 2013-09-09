package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.mercury.MercuryClientEjb;
import org.broadinstitute.gpinformatics.infrastructure.monitoring.HipChatMessageSender;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConfig;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnectorProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResourceTest;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.VesselTransferEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.MiSeqReagentKitDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500JaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.MiSeqReagentKitJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpJaxbBuilder;
import org.broadinstitute.gpinformatics.mocks.EverythingYouAskForYouGetAndItsHuman;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Tests the methods in the SolexaRunResource without any rest calls
 */
public class SolexaRunResourceNonRestTest extends Arquillian {

    @Inject
    SquidConfig squidConfig;

    @Inject
    IlluminaSequencingRunDao runDao;

    @Inject
    IlluminaFlowcellDao flowcellDao;

    @Inject
    LabVesselDao labVesselDao;

    @Inject
    MiSeqReagentKitDao miSeqReagentKitDao;

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private MercuryClientEjb mercuryClientEjb;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private IlluminaSequencingRunFactory illuminaSequencingRunFactory;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private SystemRouter router;

    @Inject
    private HipChatMessageSender messageSender;

    @Inject
    private VesselTransferEjb vesselTransferEjb;

    @Inject
    private TwoDBarcodedTubeDao twoDBarcodedTubeDao;

    @Inject
    private AppConfig appConfig;

    @Inject
    private ReagentDesignDao reagentDesignDao;


    private Date runDate;
    private String flowcellBarcode;
    private String denatureBarcode;
    private IlluminaFlowcell newFlowcell;
    private String miSeqBarcode;
    private IlluminaFlowcell miSeqFlowcell;
    private boolean result;
    private String runBarcode;
    private String miSeqRunBarcode;
    private String reagentKitBarcode;
    private String runFileDirectory;
    private String pdoKey;
    private ProductOrder exexOrder;
    private ResearchProject researchProject;
    private Product exExProduct;
    private ArrayList<ProductOrderSample> bucketReadySamples1;
    private String runName;
    private String machineName;
    private String pdo1JiraKey;

    @Inject
    BettaLimsMessageResource bettaLimsMessageResource;

    @Inject
    private MiSeqReagentKitDao reagentKitDao;

    @Deployment
    public static WebArchive buildMercuryWar() {

        return DeploymentBuilder
                .buildMercuryWarWithAlternatives(DEV, AthenaClientServiceStub.class,
                        EverythingYouAskForYouGetAndItsHuman.class);
    }

    @BeforeMethod(groups = EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {

        if (flowcellDao == null) {
            return;
        }

        runDate = new Date();
        reagentKitBarcode = "ReagentKit-" + runDate.getTime();
        miSeqBarcode = "miSeqFlowcell-" + runDate.getTime();
        denatureBarcode = "DenatureTube-" + runDate.getTime();
        String testPrefix = "runResourceTst" + runDate.getTime();
        machineName = "SL-HAL";

        researchProject = researchProjectDao.findByTitle("ADHD");

        exExProduct = productDao.findByPartNumber(
                BettaLimsMessageResourceTest.mapWorkflowToPartNum.get(Workflow.EXOME_EXPRESS));

        final String genomicSample1 = "SM-" + testPrefix + "_Genomic1" + runDate.getTime();

        pdoKey = "PDO-" + runDate.getTime();

        bucketReadySamples1 = new ArrayList<>(2);

        for (int rackPosition = 1; rackPosition <= 96; rackPosition++) {
            String bspStock = "SM-" + testPrefix + rackPosition;
            bucketReadySamples1.add(new ProductOrderSample(bspStock));
        }

        exexOrder =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                        "Solexa RunResource No Rest Test" + runDate.getTime(),
                        bucketReadySamples1, "GSP-123", exExProduct, researchProject);
        exexOrder.setProduct(exExProduct);
        exexOrder.prepareToSave(bspUserList.getByUsername("scottmat"));
        productOrderDao.persist(exexOrder);
        try {
            exexOrder.placeOrder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        pdo1JiraKey = exexOrder.getJiraTicketKey();

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = BettaLimsMessageResourceTest.buildSampleTubes(testPrefix,
                BaseEventTest.NUM_POSITIONS_IN_RACK, twoDBarcodedTubeDao);
        bucketAndBatch(testPrefix, exexOrder, mapBarcodeToTube);
        // message
        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);
        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettaLimsMessageResourceTest.sendMessagesUptoCatch(
                testPrefix,
                mapBarcodeToTube, bettaLimsMessageFactory, Workflow.EXOME_EXPRESS, bettaLimsMessageResource,
                reagentDesignDao, twoDBarcodedTubeDao,
                appConfig.getUrl(), BaseEventTest.NUM_POSITIONS_IN_RACK);

        final QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchBarcodes()),
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchRackBarcode()),
                false).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            BettaLimsMessageResourceTest.sendMessage(bettaLIMSMessage, bettaLimsMessageResource,
                    appConfig.getUrl());
        }

        MiSeqReagentKitJaxbBuilder miseqJaxbBuilder =
                new MiSeqReagentKitJaxbBuilder(new HashMap<String, VesselPosition>() {{
                    put(qtpJaxbBuilder.getDenatureTubeBarcode(), VesselPosition.A01);
                }}, reagentKitBarcode, null, bettaLimsMessageFactory).invoke();

        for (BettaLIMSMessage bettaLIMSMessage : miseqJaxbBuilder.getMessageList()) {
            BettaLimsMessageResourceTest.sendMessage(bettaLIMSMessage, bettaLimsMessageResource,
                    appConfig.getUrl());
        }

        HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder = new HiSeq2500JaxbBuilder(bettaLimsMessageFactory, testPrefix,
                qtpJaxbBuilder.getDenatureTubeBarcode(), qtpJaxbBuilder.getDenatureRackBarcode(), "FCT-1",
                ProductionFlowcellPath.DENATURE_TO_FLOWCELL,
                BaseEventTest.NUM_POSITIONS_IN_RACK, null, 2).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : hiSeq2500JaxbBuilder.getMessageList()) {
            BettaLimsMessageResourceTest.sendMessage(bettaLIMSMessage, bettaLimsMessageResource,
                    appConfig.getUrl());
        }

        flowcellBarcode = hiSeq2500JaxbBuilder.getFlowcellBarcode();

        newFlowcell = flowcellDao.findByBarcode(flowcellBarcode);
        miSeqFlowcell = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.MiSeqFlowcell, miSeqBarcode);

        for (ProductOrderSample currSample : exexOrder.getSamples()) {
            newFlowcell.addSample(new MercurySample(currSample.getBspSampleName()));
            miSeqFlowcell.addSample(new MercurySample(currSample.getBspSampleName()));
        }

        flowcellDao.persist(miSeqFlowcell);

        SimpleDateFormat dateFormat = new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN);

        runBarcode = flowcellBarcode + dateFormat.format(runDate);
        miSeqRunBarcode = miSeqBarcode + dateFormat.format(runDate);
        runName = "testRunName" + testPrefix + runDate.getTime();
        String baseDirectory = System.getProperty("java.io.tmpdir");
        runFileDirectory = baseDirectory + File.separator + "bin" + File.separator +
                           "testRoot" + File.separator + "finalPath" + runDate.getTime() +
                           File.separator + runName;
        File runFile = new File(runFileDirectory);
        result = runFile.mkdirs();
    }

    @AfterMethod(groups = EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        if (flowcellDao == null) {
            return;
        }

        exexOrder = productOrderDao.findByBusinessKey(pdo1JiraKey);

        exexOrder.setOrderStatus(ProductOrder.OrderStatus.Abandoned);
        productOrderDao.persist(exexOrder);
    }

    @Test(groups = EXTERNAL_INTEGRATION,
            dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    public void testRunResource() {

        Map<String, VesselPosition> denatureRackMap = new HashMap<>();
        denatureRackMap.put(denatureBarcode, VesselPosition.A01);
        TwoDBarcodedTube tube = new TwoDBarcodedTube(denatureBarcode);
        labVesselDao.persist(tube);
        labVesselDao.flush();

        IlluminaSequencingRun run;
        SolexaRunResource runResource =
                new SolexaRunResource(runDao, illuminaSequencingRunFactory, flowcellDao, vesselTransferEjb, router,
                        SquidConnectorProducer.stubInstance(), messageSender, squidConfig, reagentKitDao);

        SolexaRunBean runBean =
                new SolexaRunBean(miSeqBarcode, miSeqRunBarcode, runDate, machineName, runFileDirectory,
                        reagentKitBarcode);
        runResource.registerRun(runBean, miSeqFlowcell);

        run = runDao.findByBarcode(miSeqRunBarcode);
        Assert.assertNotNull(run);
        Assert.assertEquals(run.getRunName(), runName);
        Assert.assertEquals(run.getMachineName(), machineName);
        Assert.assertEquals(run.getRunBarcode(), miSeqRunBarcode);
        Assert.assertEquals(run.getRunDirectory(), runFileDirectory);
        IlluminaFlowcell illuminaFlowcell = (IlluminaFlowcell) run.getSampleCartridge();
        Assert.assertEquals(illuminaFlowcell.getFlowcellType(), IlluminaFlowcell.FlowcellType.MiSeqFlowcell);
        Assert.assertEquals(illuminaFlowcell.getLabel(), miSeqBarcode);
        Set<CherryPickTransfer> cherryPickTransfersTo = illuminaFlowcell.getContainerRole().getCherryPickTransfersTo();
        Assert.assertEquals(cherryPickTransfersTo.size(), 1);
        CherryPickTransfer cherryPickTransfer = cherryPickTransfersTo.toArray(new CherryPickTransfer[1])[0];
        Assert.assertEquals(cherryPickTransfer.getLabEvent().getLabEventType(),
                LabEventType.REAGENT_KIT_TO_FLOWCELL_TRANSFER);
        MiSeqReagentKit reagentKit = (MiSeqReagentKit) cherryPickTransfer.getSourceVesselContainer().getEmbedder();
        Assert.assertEquals(reagentKit.getLabel(), reagentKitBarcode);
        Assert.assertEquals(cherryPickTransfer.getSourcePosition(), VesselPosition.D04);
        Assert.assertEquals(cherryPickTransfer.getTargetPosition(), VesselPosition.LANE1);
        IlluminaFlowcell targetFlowcell =
                (IlluminaFlowcell) cherryPickTransfer.getTargetVesselContainer().getEmbedder();
        Assert.assertEquals(targetFlowcell.getLabel(), miSeqBarcode);
        Assert.assertEquals(targetFlowcell.getSequencingRuns().size(), 1);
    }

    /**
     * Calls the run resource methods that will apply the setup and actual read structures to a sequencing run.  This
     * method will also create a run to associate the read structures.
     */
    @Test(groups = EXTERNAL_INTEGRATION,
            dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    public void testSetReadStructure() {

        Double imagedArea = new Double("276.4795532227");
        String lanesSequenced = "2,3";
        ReadStructureRequest readStructure = new ReadStructureRequest();
        readStructure.setRunBarcode(runBarcode);
        readStructure.setSetupReadStructure("71T8B8B101T");

        IlluminaSequencingRun run;
        SolexaRunResource runResource =
                new SolexaRunResource(runDao, illuminaSequencingRunFactory, flowcellDao, vesselTransferEjb, router,
                        SquidConnectorProducer.stubInstance(), messageSender, squidConfig, reagentKitDao);

        SolexaRunBean runBean =
                new SolexaRunBean(flowcellBarcode, runBarcode, runDate, machineName, runFileDirectory,
                        null);
        runResource.registerRun(runBean, newFlowcell);

        Response readStructureStoreResponse = runResource.storeRunReadStructure(readStructure);

        Assert.assertEquals(readStructureStoreResponse.getStatus(), Response.Status.OK.getStatusCode());
        ReadStructureRequest readstructureResult = (ReadStructureRequest) readStructureStoreResponse.getEntity();

        run = runDao.findByBarcode(runBarcode);

        Assert.assertEquals(readstructureResult.getRunBarcode(), run.getRunBarcode());
        Assert.assertNotNull(readstructureResult.getSetupReadStructure());
        Assert.assertEquals(readstructureResult.getSetupReadStructure(), run.getSetupReadStructure());
        Assert.assertNull(readstructureResult.getActualReadStructure());
        Assert.assertNull(readstructureResult.getImagedArea());

        readStructure.setActualReadStructure("101T8B8B101T");


        readStructureStoreResponse = runResource.storeRunReadStructure(readStructure);
        Assert.assertEquals(readStructureStoreResponse.getStatus(), Response.Status.OK.getStatusCode());
        readstructureResult = (ReadStructureRequest) readStructureStoreResponse.getEntity();

        run = runDao.findByBarcode(runBarcode);

        Assert.assertEquals(readstructureResult.getRunBarcode(), run.getRunBarcode());
        Assert.assertNotNull(readstructureResult.getSetupReadStructure());
        Assert.assertEquals(readstructureResult.getSetupReadStructure(), run.getSetupReadStructure());
        Assert.assertNotNull(readstructureResult.getActualReadStructure());
        Assert.assertEquals(readstructureResult.getActualReadStructure(), run.getActualReadStructure());
        Assert.assertNull(readstructureResult.getImagedArea());
        Assert.assertNull(readstructureResult.getLanesSequenced());

        readStructure.setImagedArea(imagedArea);
        readStructure.setLanesSequenced(lanesSequenced);

        readStructureStoreResponse = runResource.storeRunReadStructure(readStructure);
        Assert.assertEquals(readStructureStoreResponse.getStatus(), Response.Status.OK.getStatusCode());
        readstructureResult = (ReadStructureRequest) readStructureStoreResponse.getEntity();

        run = runDao.findByBarcode(runBarcode);

        Assert.assertEquals(readstructureResult.getRunBarcode(), run.getRunBarcode());
        Assert.assertNotNull(readstructureResult.getSetupReadStructure());
        Assert.assertEquals(readstructureResult.getSetupReadStructure(), run.getSetupReadStructure());
        Assert.assertNotNull(readstructureResult.getActualReadStructure());
        Assert.assertEquals(readstructureResult.getActualReadStructure(), run.getActualReadStructure());
        Assert.assertEquals(readstructureResult.getImagedArea(), imagedArea);
        Assert.assertEquals(readstructureResult.getLanesSequenced(), lanesSequenced);
    }

    /**
     * Calls the run resource methods that will apply the setup and actual read structures to a sequencing run.  This
     * method will also create a run to associate the read structures.
     */
    @Test(groups = EXTERNAL_INTEGRATION,
            dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    public void testFailSetReadStructureInSquid() {

        Double imagedArea = new Double("276.4795532227");
        String lanesSequenced = "2,3";
        ReadStructureRequest readStructure = new ReadStructureRequest();
        final String squidRunBarcode = "squid" + runBarcode;
        readStructure.setRunBarcode(squidRunBarcode);
        final String setupReadStructure = "71T8B8B101T";
        readStructure.setSetupReadStructure(setupReadStructure);

        SolexaRunResource runResource =
                new SolexaRunResource(runDao, illuminaSequencingRunFactory, flowcellDao, vesselTransferEjb, router,
                        SquidConnectorProducer.failureStubInstance(), messageSender, squidConfig, reagentKitDao);

        Response readStructureStoreResponse = runResource.storeRunReadStructure(readStructure);

        Assert.assertEquals(readStructureStoreResponse.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        ReadStructureRequest readstructureResult = (ReadStructureRequest) readStructureStoreResponse.getEntity();


        Assert.assertEquals(readstructureResult.getRunBarcode(), squidRunBarcode);
        Assert.assertNotNull(readstructureResult.getSetupReadStructure());
        Assert.assertEquals(readstructureResult.getSetupReadStructure(), setupReadStructure);
        Assert.assertNull(readstructureResult.getActualReadStructure());
        Assert.assertNull(readstructureResult.getImagedArea());

        final String actualReadStructure = "101T8B8B101T";
        readStructure.setActualReadStructure(actualReadStructure);

        readStructureStoreResponse = runResource.storeRunReadStructure(readStructure);
        Assert.assertEquals(readStructureStoreResponse.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        readstructureResult = (ReadStructureRequest) readStructureStoreResponse.getEntity();

        Assert.assertEquals(readstructureResult.getRunBarcode(), squidRunBarcode);
        Assert.assertNotNull(readstructureResult.getSetupReadStructure());
        Assert.assertEquals(readstructureResult.getSetupReadStructure(), setupReadStructure);
        Assert.assertNotNull(readstructureResult.getActualReadStructure());
        Assert.assertEquals(readstructureResult.getActualReadStructure(), actualReadStructure);
        Assert.assertNull(readstructureResult.getImagedArea());
        Assert.assertNull(readstructureResult.getLanesSequenced());

        readStructure.setImagedArea(imagedArea);
        readStructure.setLanesSequenced(lanesSequenced);

        readStructureStoreResponse = runResource.storeRunReadStructure(readStructure);
        Assert.assertEquals(readStructureStoreResponse.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        readstructureResult = (ReadStructureRequest) readStructureStoreResponse.getEntity();

        Assert.assertEquals(readstructureResult.getRunBarcode(), squidRunBarcode);
        Assert.assertNotNull(readstructureResult.getSetupReadStructure());
        Assert.assertEquals(readstructureResult.getSetupReadStructure(), setupReadStructure);
        Assert.assertNotNull(readstructureResult.getActualReadStructure());
        Assert.assertEquals(readstructureResult.getActualReadStructure(), actualReadStructure);
        Assert.assertEquals(readstructureResult.getImagedArea(), imagedArea);
        Assert.assertEquals(readstructureResult.getLanesSequenced(), lanesSequenced);
    }

    /**
     * Add a product order's samples to the bucket, and create a batch from the bucket
     *
     * @param testPrefix       make unique
     * @param productOrder     contains sample
     * @param mapBarcodeToTube tubes
     */
    private void bucketAndBatch(String testPrefix, ProductOrder productOrder,
                                Map<String, TwoDBarcodedTube> mapBarcodeToTube) {
        HashSet<LabVessel> starters = new HashSet<LabVessel>(mapBarcodeToTube.values());
        mercuryClientEjb.addFromProductOrder(productOrder);

        String batchName = "LCSET-MsgTest-" + testPrefix;
        LabBatch labBatch = new LabBatch(batchName, starters, LabBatch.LabBatchType.WORKFLOW);
        labBatch.setValidationBatch(true);
        labBatch.setWorkflow(Workflow.EXOME_EXPRESS);
        labBatch.setJiraTicket(new JiraTicket(JiraServiceProducer.stubInstance(), batchName));
        labBatchEjb.createLabBatchAndRemoveFromBucket(labBatch, "jowalsh", "Pico/Plating Bucket",
                LabEvent.UI_EVENT_LOCATION, CreateFields.IssueType.EXOME_EXPRESS);
    }
}
