package org.broadinstitute.gpinformatics.mercury.boundary.run;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResourceTest;
import org.broadinstitute.gpinformatics.mercury.boundary.rapsheet.ReworkEjbTest;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.IlluminaRunResourceLiveTest;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.LaneReadStructure;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.glassfish.jersey.client.ClientConfig;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;


/**
 * Test run registration web service
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class SolexaRunRestResourceTest extends StubbyContainerTest {

    public SolexaRunRestResourceTest(){}

    @Inject
    private IlluminaSequencingRunDao runDao;

    @Inject
    private IlluminaFlowcellDao flowcellDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private BSPSampleSearchService bspSampleSearchService;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private AppConfig appConfig;

    @Inject
    private SolexaRunResource solexaRunResource;

    private Date runDate;
    private String flowcellBarcode;
    private IlluminaFlowcell newFlowcell;
    private boolean result;
    private String runBarcode;
    private String reagentKitBarcode;
    private String runFileDirectory;
    private String pdoKey;
    private ProductOrder exexOrder;
    private ResearchProject researchProject;
    private Product exExProduct;
    private ArrayList<ProductOrderSample> bucketReadySamples;
    private String runName;
    private String pdo1JiraKey;

    @BeforeMethod(groups = TestGroups.STUBBY)
    public void setUp() throws Exception {
        runBarcode = "RunBarcode" + System.currentTimeMillis();

        if (flowcellDao == null) {
            return;
        }

        runDate = new Date();
        reagentKitBarcode = "ReagentKitBarcode-" + runDate.getTime();
        String testPrefix = "runResourceTst";

        String rpJiraTicketKey = "RP-" + testPrefix + runDate.getTime() + "RP";
        researchProject = new ResearchProject(bspUserList.getByUsername("scottmat").getUserId(),
                "Rework Integration Test RP " + runDate.getTime() + "RP",
                "Rework Integration Test RP", false,
                ResearchProject.RegulatoryDesignation.RESEARCH_ONLY);
        researchProject.setJiraTicketKey(rpJiraTicketKey);
        researchProjectDao.persist(researchProject);

        exExProduct = productDao.findByPartNumber(
                BettaLimsMessageResourceTest.mapWorkflowToPartNum.get(Workflow.AGILENT_EXOME_EXPRESS));

        final String genomicSample1 = "SM-" + testPrefix + "_Genomic1" + runDate.getTime();

        pdoKey = "PDO-" + runDate.getTime();

        BSPSampleSearchServiceStub bspSampleSearchServiceStub = (BSPSampleSearchServiceStub) bspSampleSearchService;

        final String SM_SGM_Test_Genomic_1_CONTAINER_ID = "A0-" + testPrefix + runDate.getTime();

        bspSampleSearchServiceStub
                .addToMap(genomicSample1, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
                    put(BSPSampleSearchColumn.PARTICIPANT_ID, ReworkEjbTest.SM_SGM_Test_Genomic_1_PATIENT_ID);
                    put(BSPSampleSearchColumn.ROOT_SAMPLE, BSPSampleSearchServiceStub.ROOT);
                    put(BSPSampleSearchColumn.STOCK_SAMPLE, ReworkEjbTest.SM_SGM_Test_Genomic_1_STOCK_SAMP);
                    put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID,
                            ReworkEjbTest.SM_SGM_Test_Genomic_1_COLLAB_SAMP_ID);
                    put(BSPSampleSearchColumn.COLLECTION, ReworkEjbTest.SM_SGM_Test_Genomic_1_COLL);
                    put(BSPSampleSearchColumn.VOLUME, ReworkEjbTest.SM_SGM_Test_Genomic_1_VOLUME);
                    put(BSPSampleSearchColumn.CONCENTRATION, ReworkEjbTest.SM_SGM_Test_Genomic_1_CONC);
                    put(BSPSampleSearchColumn.SPECIES, BSPSampleSearchServiceStub.CANINE_SPECIES);
                    put(BSPSampleSearchColumn.LSID, BSPSampleSearchServiceStub.LSID_PREFIX + genomicSample1);
                    put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID,
                            ReworkEjbTest.SM_SGM_Test_Genomic_1_COLLAB_PID);
                    put(BSPSampleSearchColumn.MATERIAL_TYPE, BSPSampleSearchServiceStub.GENOMIC_MAT_TYPE);
                    put(BSPSampleSearchColumn.TOTAL_DNA, ReworkEjbTest.SM_SGM_Test_Genomic_1_DNA);
                    put(BSPSampleSearchColumn.SAMPLE_TYPE, BspSampleData.NORMAL_IND);
                    put(BSPSampleSearchColumn.PRIMARY_DISEASE, ReworkEjbTest.SM_SGM_Test_Genomic_1_DISEASE);
                    put(BSPSampleSearchColumn.GENDER, BspSampleData.FEMALE_IND);
                    put(BSPSampleSearchColumn.STOCK_TYPE, BspSampleData.ACTIVE_IND);
                    put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Genomic_1_CONTAINER_ID);
                    put(BSPSampleSearchColumn.SAMPLE_ID, genomicSample1);
                }});


        bucketReadySamples = new ArrayList<>(2);
        bucketReadySamples.add(new ProductOrderSample(genomicSample1));

        exexOrder =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                        "Rework Integration TestOrder 1" + runDate.getTime(),
                        bucketReadySamples, "GSP-123", exExProduct, researchProject);
        exexOrder.setProduct(exExProduct);
        exexOrder.prepareToSave(bspUserList.getByUsername("scottmat"));
        pdo1JiraKey = "PDO-" + testPrefix + runDate.getTime() + 1;
        exexOrder.setJiraTicketKey(pdo1JiraKey);
        productOrderDao.persist(exexOrder);


        flowcellBarcode = testPrefix + "Flowcell" + runDate.getTime();

        newFlowcell = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell,
                flowcellBarcode);

        for (ProductOrderSample currSample : exexOrder.getSamples()) {
            newFlowcell.addSample(new MercurySample(currSample.getBspSampleName(), MercurySample.MetadataSource.BSP));
        }

        flowcellDao.persist(newFlowcell);

        SimpleDateFormat dateFormat = new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN);

        runName = "testRunName" + testPrefix + runDate.getTime();
        String baseDirectory = System.getProperty("java.io.tmpdir");
        runFileDirectory = baseDirectory + File.separator + "bin" + File.separator +
                           "testRoot" + File.separator + "finalPath" + runDate.getTime() +
                           File.separator + runName;
        File runFile = new File(runFileDirectory);
        result = runFile.mkdirs();
    }

    @AfterMethod(groups = TestGroups.STUBBY)
    public void tearDown() throws Exception {
        if (flowcellDao == null) {
            return;
        }
        exexOrder = productOrderDao.findByBusinessKey(pdo1JiraKey);
        exexOrder.setOrderStatus(ProductOrder.OrderStatus.Abandoned);
        productOrderDao.persist(exexOrder);
    }

    @Test(groups = TestGroups.STUBBY, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, enabled = false)
    public void testCreateRun() {

        Assert.assertTrue(result);

        ClientConfig clientConfig = JerseyUtils.getClientConfigAcceptCertificate();

        Response response = ClientBuilder.newClient(clientConfig).target(appConfig.getUrl() + "rest/solexarun")
                .request(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .post(Entity.xml(new SolexaRunBean(flowcellBarcode, runBarcode, runDate, "SL-HAL",
                        runFileDirectory, null)), Response.class);


        Assert.assertEquals(response.getStatus(), Response.Status.CREATED);
        System.out.println(response.getStatus());

        String runName = new File(runFileDirectory).getName();

        IlluminaSequencingRun sequencingRun = runDao.findByRunName(runName);

        IlluminaFlowcell createdFlowcell = flowcellDao.findByBarcode(flowcellBarcode);

        Assert.assertEquals(sequencingRun.getSampleCartridge(), createdFlowcell);

        Assert.assertEquals(createdFlowcell.getSequencingRuns().iterator().next(), sequencingRun);

    }

    @Test(groups = TestGroups.STUBBY, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, enabled = false)
    public void testCreate2500Run() {

        Assert.assertTrue(result);


        ClientConfig clientConfig = JerseyUtils.getClientConfigAcceptCertificate();

        Response response = ClientBuilder.newClient(clientConfig).target(appConfig.getUrl() + "rest/solexarun")
                .request(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .post(Entity.xml(new SolexaRunBean(flowcellBarcode, runBarcode, runDate, "SL-HAL",
                        runFileDirectory, reagentKitBarcode)), Response.class);


        Assert.assertEquals(response.getStatus(), Response.Status.CREATED);
        System.out.println(response.getStatus());

        String runName = new File(runFileDirectory).getName();

        IlluminaSequencingRun sequencingRun = runDao.findByRunName(runName);

        IlluminaFlowcell createdFlowcell = flowcellDao.findByBarcode(flowcellBarcode);

        Assert.assertEquals(sequencingRun.getSampleCartridge(), createdFlowcell);

        Assert.assertEquals(createdFlowcell.getSequencingRuns().iterator().next(), sequencingRun);

    }

    @Test(groups = TestGroups.STUBBY,
            dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, enabled = false)
    @RunAsClient
    public void testReadStructureOverHttp(@ArquillianResource URL baseUrl) throws Exception {
        String wsUrl =
                RestServiceContainerTest.convertUrlToSecure(baseUrl) + "rest/solexarun/storeRunReadStructure";

        ReadStructureRequest readStructureData = new ReadStructureRequest();
        readStructureData.setRunBarcode(runBarcode);
        readStructureData.setImagedArea(20.23932);
        readStructureData.setLanesSequenced("2,5");
        readStructureData.setActualReadStructure("71T8B8B101T");
        readStructureData.setActualReadStructure("76T8B8B76T");


        ClientConfig clientConfig = JerseyUtils.getClientConfigAcceptCertificate();
        clientConfig.getClasses().add(JacksonJsonProvider.class);

        Response readStructureResult =
                ClientBuilder.newClient(clientConfig).target(wsUrl)
                        .request(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON)
                        .post(Entity.json(readStructureData), Response.class);

        Assert.assertEquals(((ReadStructureRequest) readStructureResult.getEntity()).getSetupReadStructure(),
                readStructureData.getSetupReadStructure());
        Assert.assertEquals(((ReadStructureRequest) readStructureResult.getEntity()).getActualReadStructure(),
                readStructureData.getActualReadStructure());
        Assert.assertEquals(((ReadStructureRequest) readStructureResult.getEntity()).getLanesSequenced(),
                readStructureData.getLanesSequenced());
        Assert.assertEquals(((ReadStructureRequest) readStructureResult.getEntity()).getImagedArea(),
                readStructureData.getImagedArea());

    }

    @Test(groups = TestGroups.STUBBY, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, enabled = true)
    @RunAsClient
    public void testMercuryLanes(@ArquillianResource URL baseUrl) throws Exception {
        String wsUrl =
                RestServiceContainerTest.convertUrlToSecure(baseUrl) + "rest/solexarun/storeRunReadStructure";

        ReadStructureRequest readStructureData = new ReadStructureRequest();
        readStructureData.setRunBarcode("H7HBEADXX140225");
        readStructureData.setImagedArea(20.23932);
        readStructureData.setLanesSequenced("2,5");
        readStructureData.setActualReadStructure("71T8B8B101T");
        readStructureData.setActualReadStructure("76T8B8B76T");
        for (int i = 1; i <= 8; i++) {
            LaneReadStructure laneReadStructure = new LaneReadStructure();
            laneReadStructure.setLaneNumber(i);
            laneReadStructure.setActualReadStructure("STRUC" + i);
            readStructureData.getLaneStructures().add(laneReadStructure);
        }

        ClientConfig clientConfig = JerseyUtils.getClientConfigAcceptCertificate();
        clientConfig.getClasses().add(JacksonJsonProvider.class);

        ReadStructureRequest returnedReadStructureRequest = ClientBuilder.newClient(clientConfig).target(wsUrl).
                request(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).
                post(Entity.json(readStructureData), ReadStructureRequest.class);

        ZimsIlluminaRun zimsIlluminaRun = IlluminaRunResourceLiveTest.getZimsIlluminaRun(baseUrl,
                "140225_SL-HDJ_0314_AFCH7HBEADXX");
        for (ZimsIlluminaChamber zimsIlluminaChamber : zimsIlluminaRun.getLanes()) {
            Assert.assertEquals(zimsIlluminaChamber.getActualReadStructure(), "STRUC" + zimsIlluminaChamber.getName());
        }
    }
}
