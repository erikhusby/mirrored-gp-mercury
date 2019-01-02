package org.broadinstitute.gpinformatics.mercury.boundary.run;

//import com.jprofiler.api.agent.Controller;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductOrderJiraUtil;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcherStub;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.monitoring.HipChatMessageSender;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConfig;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnectorProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResourceTest;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.VesselTransferEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.LimsQueryObjectFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.MiSeqReagentKitDao;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.LaneReadStructure;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500JaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.MiSeqReagentKitJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpJaxbBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Tests the methods in the SolexaRunResource without any rest calls
 */
@Test(groups = TestGroups.ALTERNATIVES)
@Dependent
public class SolexaRunResourceNonRestTest extends Arquillian {

    public SolexaRunResourceNonRestTest(){}

    @Inject
    private SquidConfig squidConfig;

    @Inject
    private IlluminaSequencingRunDao runDao;

    @Inject
    private IlluminaFlowcellDao flowcellDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private MiSeqReagentKitDao miSeqReagentKitDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private BSPUserList bspUserList;

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
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private AppConfig appConfig;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private JiraService jiraService;

    @Inject
    private ProductOrderJiraUtil productOrderJiraUtil;

    @Inject
    private BucketEjb bucketEjb;

    private Date runDate;
    private String flowcellBarcode;
    private String denatureBarcode;
    private IlluminaFlowcell newFlowcell;
    private String miSeqBarcode;
    private IlluminaFlowcell miSeqFlowcell;
    private String runBarcode;
    private String miSeqRunBarcode;
    private String reagentKitBarcode;
    private String runFileDirectory;
    private ProductOrder exexOrder;
    private String runName;
    private String machineName;
    private String pdo1JiraKey;

    public static final double IMAGED_AREA = 276.4795532227;
    public static final String LANES_SEQUENCED = "2,3";
    public static final String ACTUAL_READ_STRUCTURE = "101T8B8B101T";
    public static final String SETUP_READ_STRUCTURE = "71T8B8B101T";

    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;

    @Inject
    private MiSeqReagentKitDao reagentKitDao;

    @Deployment
    public static WebArchive buildMercuryWar() {

        return DeploymentBuilder
                .buildMercuryWarWithAlternatives(DEV, SampleDataFetcherStub.EverythingYouAskForYouGetAndItsHuman.class);
    }

    @BeforeMethod(groups = TestGroups.ALTERNATIVES)
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

        ResearchProject researchProject = researchProjectDao.findByTitle("ADHD");

        Product exExProduct = productDao.findByPartNumber(
                BettaLimsMessageResourceTest.mapWorkflowToPartNum.get(Workflow.AGILENT_EXOME_EXPRESS));

        ArrayList<ProductOrderSample> bucketReadySamples1 = new ArrayList<>(2);

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
        exexOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrderDao.persist(exexOrder);
        try {
            productOrderJiraUtil.createIssueForOrder(exexOrder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        pdo1JiraKey = exexOrder.getJiraTicketKey();

        Map<String, BarcodedTube> mapBarcodeToTube = BettaLimsMessageResourceTest.buildSampleTubes(testPrefix,
                BaseEventTest.NUM_POSITIONS_IN_RACK, barcodedTubeDao, MercurySample.MetadataSource.BSP, exexOrder);

        bucketAndBatch(testPrefix, exexOrder, mapBarcodeToTube);
        // message
        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);
        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettaLimsMessageResourceTest.sendMessagesUptoCatch(
                testPrefix,
                mapBarcodeToTube, bettaLimsMessageFactory, Workflow.AGILENT_EXOME_EXPRESS, bettaLimsMessageResource,
                reagentDesignDao, barcodedTubeDao,
                appConfig.getUrl(), BaseEventTest.NUM_POSITIONS_IN_RACK);

        final QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchBarcodes()),
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchRackBarcode()),
                true, QtpJaxbBuilder.PcrType.VIIA_7);
        qtpJaxbBuilder.invokeToQuant();
        qtpJaxbBuilder.invokePostQuant();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            BettaLimsMessageResourceTest.sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }

        MiSeqReagentKitJaxbBuilder miseqJaxbBuilder =
                new MiSeqReagentKitJaxbBuilder(new HashMap<String, VesselPosition>() {{
                    put(qtpJaxbBuilder.getDenatureTubeBarcode(), VesselPosition.A01);
                }}, reagentKitBarcode, null, bettaLimsMessageFactory).invoke();

        for (BettaLIMSMessage bettaLIMSMessage : miseqJaxbBuilder.getMessageList()) {
            BettaLimsMessageResourceTest.sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }

        HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder = new HiSeq2500JaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(qtpJaxbBuilder.getDenatureTubeBarcode()),
                qtpJaxbBuilder.getDenatureRackBarcode(), "FCT-1", ProductionFlowcellPath.DENATURE_TO_FLOWCELL,
                BaseEventTest.NUM_POSITIONS_IN_RACK, null, 2).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : hiSeq2500JaxbBuilder.getMessageList()) {
            BettaLimsMessageResourceTest.sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }

        flowcellBarcode = hiSeq2500JaxbBuilder.getFlowcellBarcode();

        newFlowcell = flowcellDao.findByBarcode(flowcellBarcode);
        miSeqFlowcell = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.MiSeqFlowcell, miSeqBarcode);

        for (ProductOrderSample currSample : exexOrder.getSamples()) {
            MercurySample sample = mercurySampleDao.findBySampleKey(currSample.getSampleKey());

            newFlowcell.addSample(sample);
            miSeqFlowcell.addSample(sample);
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
        runFile.mkdirs();
    }

    @AfterMethod(groups = TestGroups.ALTERNATIVES)
    public void tearDown() throws Exception {
        if (flowcellDao == null) {
            return;
        }

        exexOrder = productOrderDao.findByBusinessKey(pdo1JiraKey);

        exexOrder.setOrderStatus(ProductOrder.OrderStatus.Abandoned);
        productOrderDao.persist(exexOrder);
    }

    @Test(groups = TestGroups.ALTERNATIVES,
          dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    public void testRunResource() {

        BarcodedTube tube = new BarcodedTube(denatureBarcode);
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

        run = TestUtils.getFirst(runDao.findByBarcode(miSeqRunBarcode));
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
     * Calls storeRunReadStructure with a ReadStructureRequest that has a runName but no runBarcode. This
     * method will also create a run to associate the read structures.
     */
    @Test(groups = TestGroups.ALTERNATIVES)
    public void testGetReadStructureByName() {
        SolexaRunResource runResource =
                new SolexaRunResource(runDao, illuminaSequencingRunFactory, flowcellDao, vesselTransferEjb, router,
                                      SquidConnectorProducer.stubInstance(), messageSender, squidConfig, reagentKitDao);
        SolexaRunBean runBean =
                new SolexaRunBean(miSeqBarcode, miSeqRunBarcode, runDate, machineName, runFileDirectory,
                                  reagentKitBarcode);
        runResource.registerRun(runBean, miSeqFlowcell);
        IlluminaSequencingRun run = TestUtils.getFirst(runDao.findByBarcode(miSeqRunBarcode));

        ReadStructureRequest readStructureWithRunName = LimsQueryObjectFactory
                .createReadStructureRequest(run.getRunName(), null, SETUP_READ_STRUCTURE, ACTUAL_READ_STRUCTURE,
                                            IMAGED_AREA, LANES_SEQUENCED, null);

        Response readStructureStoreResponse = runResource.storeRunReadStructure(readStructureWithRunName);

        Assert.assertEquals(readStructureStoreResponse.getStatus(), Response.Status.OK.getStatusCode());
        ReadStructureRequest readStructureResultByName = (ReadStructureRequest) readStructureStoreResponse.getEntity();

        Assert.assertEquals(readStructureResultByName.getRunName(), run.getRunName());
        Assert.assertEquals(readStructureResultByName.getRunBarcode(), miSeqRunBarcode);
    }

    /**
     * Calls storeRunReadStructure with a ReadStructureRequest that has a runName but no runBarcode. This
     * method will also create a run to associate the read structures.
     */
    @Test(groups = TestGroups.ALTERNATIVES)
    public void testLaneReadStructure() {
        SolexaRunResource runResource =
                new SolexaRunResource(runDao, illuminaSequencingRunFactory, flowcellDao, vesselTransferEjb, router,
                                      SquidConnectorProducer.stubInstance(), messageSender, squidConfig, reagentKitDao);
        SolexaRunBean runBean =
                new SolexaRunBean(miSeqBarcode, miSeqRunBarcode, runDate, machineName, runFileDirectory,
                                  reagentKitBarcode);
        runResource.registerRun(runBean, miSeqFlowcell);
        IlluminaSequencingRun run = TestUtils.getFirst(runDao.findByBarcode(miSeqRunBarcode));

        ReadStructureRequest readStructureWithRunName = LimsQueryObjectFactory
                .createReadStructureRequest(run.getRunName(), null, SETUP_READ_STRUCTURE, ACTUAL_READ_STRUCTURE,
                                            IMAGED_AREA, LANES_SEQUENCED, null);
        LaneReadStructure laneReadStructure = new LaneReadStructure();
        laneReadStructure.setLaneNumber(1);
        laneReadStructure.setActualReadStructure("STRUC1");
        readStructureWithRunName.getLaneStructures().add(laneReadStructure);

        Response readStructureStoreResponse = runResource.storeRunReadStructure(readStructureWithRunName);

        Assert.assertEquals(readStructureStoreResponse.getStatus(), Response.Status.OK.getStatusCode());
        ReadStructureRequest readStructureResultByName = (ReadStructureRequest) readStructureStoreResponse.getEntity();

        Assert.assertEquals(readStructureResultByName.getRunName(), run.getRunName());
        Assert.assertEquals(readStructureResultByName.getRunBarcode(), miSeqRunBarcode);
        Assert.assertEquals(readStructureResultByName.getLaneStructures().size(), 1);
        Assert.assertEquals(readStructureResultByName.getLaneStructures().get(0).getActualReadStructure(), "STRUC1");
    }

    /**
     * Calls the run resource methods that will apply the setup and actual read structures to a sequencing run.  This
     * method will also create a run to associate the read structures.
     */
    @Test(groups = TestGroups.ALTERNATIVES)
    public void testGetReadStructureByBarcode() {
        ReadStructureRequest readStructure = new ReadStructureRequest();
        readStructure.setRunBarcode(runBarcode);
        readStructure.setSetupReadStructure(SETUP_READ_STRUCTURE);

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
        ReadStructureRequest readStructureResult = (ReadStructureRequest) readStructureStoreResponse.getEntity();

        run = TestUtils.getFirst(runDao.findByBarcode(runBarcode));

        Assert.assertEquals(readStructureResult.getRunBarcode(), run.getRunBarcode());
        Assert.assertNotNull(readStructureResult.getSetupReadStructure());
        Assert.assertEquals(readStructureResult.getSetupReadStructure(), run.getSetupReadStructure());
        Assert.assertNull(readStructureResult.getActualReadStructure());
        Assert.assertNull(readStructureResult.getImagedArea());

        readStructure.setActualReadStructure(ACTUAL_READ_STRUCTURE);

        readStructureStoreResponse = runResource.storeRunReadStructure(readStructure);
        Assert.assertEquals(readStructureStoreResponse.getStatus(), Response.Status.OK.getStatusCode());
        readStructureResult = (ReadStructureRequest) readStructureStoreResponse.getEntity();

        run = TestUtils.getFirst(runDao.findByBarcode(runBarcode));

        Assert.assertEquals(readStructureResult.getRunBarcode(), run.getRunBarcode());
        Assert.assertNotNull(readStructureResult.getSetupReadStructure());
        Assert.assertEquals(readStructureResult.getSetupReadStructure(), run.getSetupReadStructure());
        Assert.assertNotNull(readStructureResult.getActualReadStructure());
        Assert.assertEquals(readStructureResult.getActualReadStructure(), run.getActualReadStructure());
        Assert.assertNull(readStructureResult.getImagedArea());
        Assert.assertNull(readStructureResult.getLanesSequenced());

        readStructure.setImagedArea(IMAGED_AREA);
        readStructure.setLanesSequenced(LANES_SEQUENCED);

        readStructureStoreResponse = runResource.storeRunReadStructure(readStructure);
        Assert.assertEquals(readStructureStoreResponse.getStatus(), Response.Status.OK.getStatusCode());
        readStructureResult = (ReadStructureRequest) readStructureStoreResponse.getEntity();

        run = TestUtils.getFirst(runDao.findByBarcode(runBarcode));

        Assert.assertEquals(readStructureResult.getRunBarcode(), run.getRunBarcode());
        Assert.assertNotNull(readStructureResult.getSetupReadStructure());
        Assert.assertEquals(readStructureResult.getSetupReadStructure(), run.getSetupReadStructure());
        Assert.assertNotNull(readStructureResult.getActualReadStructure());
        Assert.assertEquals(readStructureResult.getActualReadStructure(), run.getActualReadStructure());
        Assert.assertEquals(readStructureResult.getImagedArea(), IMAGED_AREA);
        Assert.assertEquals(readStructureResult.getLanesSequenced(), LANES_SEQUENCED);
    }

    /**
     * Calls the run resource methods that will apply the setup and actual read structures to a sequencing run.  This
     * method will also create a run to associate the read structures.
     */
    @Test(groups = TestGroups.ALTERNATIVES,
          dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    public void testFailSetReadStructureInSquid() {
        ReadStructureRequest readStructure = new ReadStructureRequest();
        final String squidRunBarcode = "squid" + runBarcode;
        readStructure.setRunBarcode(squidRunBarcode);
        readStructure.setSetupReadStructure(SETUP_READ_STRUCTURE);

        SolexaRunResource runResource =
                new SolexaRunResource(runDao, illuminaSequencingRunFactory, flowcellDao, vesselTransferEjb, router,
                                      SquidConnectorProducer.failureStubInstance(), messageSender, squidConfig,
                                      reagentKitDao);

        Response readStructureStoreResponse = runResource.storeRunReadStructure(readStructure);

        Assert.assertEquals(readStructureStoreResponse.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        ReadStructureRequest readstructureResult = (ReadStructureRequest) readStructureStoreResponse.getEntity();

        Assert.assertEquals(readstructureResult.getRunBarcode(), squidRunBarcode);
        Assert.assertNotNull(readstructureResult.getSetupReadStructure());
        Assert.assertEquals(readstructureResult.getSetupReadStructure(), SETUP_READ_STRUCTURE);
        Assert.assertNull(readstructureResult.getActualReadStructure());
        Assert.assertNull(readstructureResult.getImagedArea());

        readStructure.setActualReadStructure(ACTUAL_READ_STRUCTURE);

        readStructureStoreResponse = runResource.storeRunReadStructure(readStructure);
        Assert.assertEquals(readStructureStoreResponse.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        readstructureResult = (ReadStructureRequest) readStructureStoreResponse.getEntity();

        Assert.assertEquals(readstructureResult.getRunBarcode(), squidRunBarcode);
        Assert.assertNotNull(readstructureResult.getSetupReadStructure());
        Assert.assertEquals(readstructureResult.getSetupReadStructure(), SETUP_READ_STRUCTURE);
        Assert.assertNotNull(readstructureResult.getActualReadStructure());
        Assert.assertEquals(readstructureResult.getActualReadStructure(), ACTUAL_READ_STRUCTURE);
        Assert.assertNull(readstructureResult.getImagedArea());
        Assert.assertNull(readstructureResult.getLanesSequenced());

        readStructure.setImagedArea(IMAGED_AREA);
        readStructure.setLanesSequenced(LANES_SEQUENCED);

        readStructureStoreResponse = runResource.storeRunReadStructure(readStructure);
        Assert.assertEquals(readStructureStoreResponse.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        readstructureResult = (ReadStructureRequest) readStructureStoreResponse.getEntity();

        Assert.assertEquals(readstructureResult.getRunBarcode(), squidRunBarcode);
        Assert.assertNotNull(readstructureResult.getSetupReadStructure());
        Assert.assertEquals(readstructureResult.getSetupReadStructure(), SETUP_READ_STRUCTURE);
        Assert.assertNotNull(readstructureResult.getActualReadStructure());
        Assert.assertEquals(readstructureResult.getActualReadStructure(), ACTUAL_READ_STRUCTURE);
        Assert.assertEquals(readstructureResult.getImagedArea(), IMAGED_AREA);
        Assert.assertEquals(readstructureResult.getLanesSequenced(), LANES_SEQUENCED);
    }

    /**
     * Add a product order's samples to the bucket, and create a batch from the bucket
     *
     * @param testPrefix       make unique
     * @param productOrder     contains sample
     * @param mapBarcodeToTube tubes
     */
    private void bucketAndBatch(String testPrefix, ProductOrder productOrder,
                                Map<String, BarcodedTube> mapBarcodeToTube) throws ValidationException {
        HashSet<LabVessel> starters = new HashSet<LabVessel>(mapBarcodeToTube.values());
        bucketEjb.addSamplesToBucket(productOrder);

        String batchName = "LCSET-MsgTest-" + testPrefix;
        List<Long> bucketIds = new ArrayList<>();
        String bucketName = null;
        for (LabVessel starter : starters) {
            bucketName = starter.getBucketEntries().iterator().next().getBucket().getBucketDefinitionName();
            bucketIds.add(starter.getBucketEntries().iterator().next().getBucketEntryId());
        }

        LabBatch labBatch = labBatchEjb.createLabBatchAndRemoveFromBucket(LabBatch.LabBatchType.WORKFLOW,
                Workflow.AGILENT_EXOME_EXPRESS, bucketIds,
                Collections.<Long>emptyList(), batchName, "", new Date(), "", "jowalsh", bucketName);
        labBatch.setValidationBatch(true);
    }
}
