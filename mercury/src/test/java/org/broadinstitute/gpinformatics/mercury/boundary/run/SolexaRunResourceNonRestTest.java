package org.broadinstitute.gpinformatics.mercury.boundary.run;

import com.sun.jersey.api.client.Client;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.monitoring.HipChatMessageSender;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettalimsMessageResourceTest;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.rapsheet.ReworkEjbTest;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Tests the methods in the SolexaRunResource without any rest calls
 */
public class SolexaRunResourceNonRestTest extends Arquillian {


    @Inject
    IlluminaSequencingRunDao runDao;

    @Inject
    IlluminaFlowcellDao flowcellDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private IlluminaSequencingRunFactory illuminaSequencingRunFactory;

    @Inject
    private BSPSampleSearchService bspSampleSearchService;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private MercuryOrSquidRouter router;

    @Inject
    private HipChatMessageSender messageSender;


    @Inject
    AppConfig appConfig;

    private Date runDate;
    private String flowcellBarcode;
    private IlluminaFlowcell newFlowcell;
    private boolean result;
    private String runBarcode;
    private String runFileDirectory;
    private String pdoKey;
    private ProductOrder exexOrder;
    private ResearchProject researchProject;
    private Product exExProduct;
    private ArrayList<ProductOrderSample> bucketReadySamples1;
    private String runName;

    @Deployment
    public static WebArchive buildMercuryWar() {

        return DeploymentBuilder
                .buildMercuryWarWithAlternatives(DEV, AthenaClientServiceStub.class, BSPSampleSearchServiceStub.class);
    }

    @BeforeMethod(groups = EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {

        if (flowcellDao == null) {
            return;
        }

        runDate = new Date();

        String testPrefix = "runResourceTst";

        String rpJiraTicketKey = "RP-" + testPrefix + runDate.getTime() + "RP";
        researchProject = new ResearchProject(bspUserList.getByUsername("scottmat").getUserId(),
                "Rework Integration Test RP " + runDate.getTime() + "RP", "Rework Integration Test RP", false);
        researchProject.setJiraTicketKey(rpJiraTicketKey);
        researchProjectDao.persist(researchProject);

        exExProduct = productDao.findByPartNumber(
                BettalimsMessageResourceTest.mapWorkflowToPartNum.get(WorkflowName.EXOME_EXPRESS.getWorkflowName()));

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
                    put(BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleDTO.NORMAL_IND);
                    put(BSPSampleSearchColumn.PRIMARY_DISEASE, ReworkEjbTest.SM_SGM_Test_Genomic_1_DISEASE);
                    put(BSPSampleSearchColumn.GENDER, BSPSampleDTO.FEMALE_IND);
                    put(BSPSampleSearchColumn.STOCK_TYPE, BSPSampleDTO.ACTIVE_IND);
                    put(BSPSampleSearchColumn.FINGERPRINT, ReworkEjbTest.SM_SGM_Test_Genomic_1_FP);
                    put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Genomic_1_CONTAINER_ID);
                    put(BSPSampleSearchColumn.SAMPLE_ID, genomicSample1);
                }});


        bucketReadySamples1 = new ArrayList<>(2);
        bucketReadySamples1.add(new ProductOrderSample(genomicSample1));

        exexOrder =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                        "Rework Integration TestOrder 1" + runDate.getTime(),
                        bucketReadySamples1, "GSP-123", exExProduct, researchProject);
        exexOrder.setProduct(exExProduct);
        exexOrder.prepareToSave(bspUserList.getByUsername("scottmat"));
        String pdo1JiraKey = "PDO-" + testPrefix + runDate.getTime() + 1;
        exexOrder.setJiraTicketKey(pdo1JiraKey);
        productOrderDao.persist(exexOrder);


        flowcellBarcode = testPrefix + "Flowcell" + runDate.getTime();

        newFlowcell = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell,
                flowcellBarcode);

        for (ProductOrderSample currSample : exexOrder.getSamples()) {
            newFlowcell.addSample(new MercurySample(currSample.getBspSampleName()));
        }

        flowcellDao.persist(newFlowcell);

        SimpleDateFormat dateFormat = new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN);

        runBarcode = flowcellBarcode + dateFormat.format(runDate);
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

        exexOrder.setOrderStatus(ProductOrder.OrderStatus.Abandoned);
        flowcellDao.persist(exexOrder);
    }

    /**
     * Calls the run resource methods that will apply the setup and actual read structures to a sequencing run.  This
     * method will also create a run to associate the read structures.
     */
    @Test(groups = EXTERNAL_INTEGRATION,
            dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    public void testSetReadStructure() {

        ReadStructureRequest readStructure = new ReadStructureRequest();
        readStructure.setRunBarcode(runBarcode);
        readStructure.setSetupReadStructure("71T8B8B101T");

        IlluminaSequencingRun run =
                new IlluminaSequencingRun(newFlowcell, runName, runBarcode, "SL-HAL",
                        bspUserList.getByUsername("scottmat").getUserId(), true, runDate, runFileDirectory);

        runDao.persist(run);

        SolexaRunResource runResource =
                new SolexaRunResource(runDao, illuminaSequencingRunFactory, flowcellDao, router, null, messageSender);

        ReadStructureRequest readstructureResult = runResource.storeRunReadStructure(readStructure);

        run = runDao.findByBarcode(runBarcode);

        Assert.assertEquals(readstructureResult.getRunBarcode(), run.getRunBarcode());
        Assert.assertNotNull(readstructureResult.getSetupReadStructure());
        Assert.assertEquals(readstructureResult.getSetupReadStructure(), run.getSetupReadStructure());
        Assert.assertNull(readstructureResult.getActualReadStructure());

        readStructure.setActualReadStructure("101T8B8B101T");

        readstructureResult = runResource.storeRunReadStructure(readStructure);

        run = runDao.findByBarcode(runBarcode);

        Assert.assertEquals(readstructureResult.getRunBarcode(), run.getRunBarcode());
        Assert.assertNotNull(readstructureResult.getSetupReadStructure());
        Assert.assertEquals(readstructureResult.getSetupReadStructure(), run.getSetupReadStructure());
        Assert.assertNotNull(readstructureResult.getActualReadStructure());
        Assert.assertEquals(readstructureResult.getActualReadStructure(), run.getActualReadStructure());
    }

}
