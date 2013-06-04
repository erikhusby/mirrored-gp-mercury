package org.broadinstitute.gpinformatics.mercury.boundary.rapsheet;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettalimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettalimsMessageResourceTest;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.vessel.IndexedPlateFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionJaxbBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 *
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class ReworkEjbTest extends Arquillian {

    @Inject
    ReworkEjb reworkEjb;

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private UserTransaction utx;

    @Inject
    private BucketDao bucketDao;

    @Inject
    private BettalimsMessageResource bettalimsMessageResource;

    @Inject
    private IndexedPlateFactory indexedPlateFactory;

    @Inject
    private StaticPlateDAO staticPlateDAO;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Inject
    private BucketEntryDao bucketEntryDao;

    @Inject
    MercurySampleDao mercurySampleDao;

    @Inject
    AppConfig appConfig;

    private Map<String, TwoDBarcodedTube> mapBarcodeToTube = new HashMap<>();
    private ResearchProject researchProject;
    private ProductOrder exExProductOrder1;
    private ProductOrder exExProductOrder2;
    private ProductOrder nonExExProductOrder;
    private List<ProductOrderSample> bucketReadySamples1;
    private List<ProductOrderSample> bucketReadySamples2;
    private List<ProductOrderSample> bucketSamples1;
    private Bucket pBucket;
    private String bucketName;
    private String rpJiraTicketKey;
    private String pdo1JiraKey;
    private String pdo2JiraKey;

    private String pdo3JiraKey;
    private boolean newBucket;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(DEV, BSPSampleSearchServiceStub.class);
    }

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        if (reworkEjb == null) {
            return;
        }

        newBucket = false;
        Date currDate = new Date();

        String testPrefix = "SGM_Test";

        rpJiraTicketKey = "RP-SGM-Rework_tst1" + currDate.getTime() + "RP";
        researchProject = new ResearchProject(bspUserList.getByUsername("scottmat").getUserId(),
                "Rework Integration Test RP " + currDate.getTime() + "RP", "Rework Integration Test RP", false);
        researchProject.setJiraTicketKey(rpJiraTicketKey);
        researchProjectDao.persist(researchProject);

        researchProject = researchProjectDao.findByBusinessKey(rpJiraTicketKey);

        Product exExProduct = productDao.findByPartNumber(
                BettalimsMessageResourceTest.mapWorkflowToPartNum.get(WorkflowName.EXOME_EXPRESS));
        Product nonExExProduct = productDao.findByPartNumber(
                BettalimsMessageResourceTest.mapWorkflowToPartNum.get(WorkflowName.HYBRID_SELECTION));

        bucketReadySamples1 =
                Collections.singletonList(new ProductOrderSample(BSPSampleSearchServiceStub.SM_SGM_Test_Genomic_1));

        bucketReadySamples2 = new ArrayList<>(2);
        bucketReadySamples2.add(new ProductOrderSample(BSPSampleSearchServiceStub.SM_SGM_Test_Genomic_2));
        bucketReadySamples2.add(new ProductOrderSample(BSPSampleSearchServiceStub.SM_SGM_Test_Somatic_1));

        bucketSamples1 = new ArrayList<>(2);
        bucketSamples1.add(new ProductOrderSample(BSPSampleSearchServiceStub.SM_SGM_Test_Genomic_2));
        bucketSamples1.add(new ProductOrderSample(BSPSampleSearchServiceStub.SM_SGM_Test_Somatic_1));


        exExProductOrder1 =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                        "Rework Integration TestOrder 1" + currDate.getTime(),
                        bucketReadySamples1, "GSP-123", exExProduct, researchProject);
        exExProductOrder1.prepareToSave(bspUserList.getByUsername("scottmat"));
        pdo1JiraKey = "PDO-SGM-RWINT_tst" + currDate.getTime() + 1;
        exExProductOrder1.setJiraTicketKey(pdo1JiraKey);
        productOrderDao.persist(exExProductOrder1);
//        exExProductOrder1.placeOrder();

        exExProductOrder2 =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                        "Rework Integration TestOrder 2" + currDate.getTime(),
                        bucketReadySamples2, "GSP-123", exExProduct, researchProject);
        exExProductOrder2.prepareToSave(bspUserList.getByUsername("scottmat"));
        pdo2JiraKey = "PDO-SGM-RWINT_tst" + currDate.getTime() + 2;
        exExProductOrder2.setJiraTicketKey(pdo2JiraKey);
        productOrderDao.persist(exExProductOrder2);
//        exExProductOrder2.placeOrder();

        nonExExProductOrder =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                        "Rework Integration TestOrder 3" + currDate.getTime(),
                        bucketReadySamples2, "GSP-123", nonExExProduct, researchProject);
        nonExExProductOrder.prepareToSave(bspUserList.getByUsername("scottmat"));
        pdo3JiraKey = "PDO-SGM-RWINT_tst" + currDate.getTime() + 3;
        nonExExProductOrder.setJiraTicketKey(pdo3JiraKey);
        productOrderDao.persist(nonExExProductOrder);
//        nonExExProductOrder.placeOrder();

        WorkflowBucketDef bucketDef = LabEventHandler
                .findBucketDef(WorkflowName.EXOME_EXPRESS.getWorkflowName(), LabEventType.PICO_PLATING_BUCKET);

        bucketName = bucketDef.getName();

        pBucket = bucketDao.findByName(bucketName);

        if (pBucket == null) {

            pBucket = new Bucket(bucketName);
            newBucket = true;
        }

        bucketDao.persist(pBucket);

    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        if (reworkEjb == null) {
            return;
        }

        pBucket = bucketDao.findByName(bucketName);

        exExProductOrder1 = productOrderDao.findByBusinessKey(pdo1JiraKey);
        productOrderDao.remove(exExProductOrder1);

        exExProductOrder2 = productOrderDao.findByBusinessKey(pdo2JiraKey);
        productOrderDao.remove(exExProductOrder2);

        nonExExProductOrder = productOrderDao.findByBusinessKey(pdo3JiraKey);
        productOrderDao.remove(nonExExProductOrder);

        researchProject = researchProjectDao.findByBusinessKey(rpJiraTicketKey);

        researchProjectDao.remove(researchProject);

        for (String currVessel : mapBarcodeToTube.keySet()) {
            LabVessel tempTube = labVesselDao.findByIdentifier(currVessel);

            BucketEntry vesselBucketEntry = null;
            if (pBucket != null) {
                vesselBucketEntry = bucketEntryDao.findByVesselAndBucket(tempTube, pBucket);
            }

            labVesselDao.remove(tempTube);
            if (vesselBucketEntry != null) {
                bucketEntryDao.remove(vesselBucketEntry);
            }
        }

        if (newBucket) {

            bucketDao.remove(pBucket);
        }

        mapBarcodeToTube.clear();
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testHappyPath() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()));

//        utx.begin();

        for (String barcode : mapBarcodeToTube.keySet()) {
            reworkEjb.addRework(barcode, ReworkEntry.ReworkReason.UNKNOWN_ERROR, LabEventType.PICO_PLATING_BUCKET,
                    "test Rework",
                    WorkflowName.EXOME_EXPRESS.getWorkflowName());
        }
//        productOrderDao.flush();
//        productOrderDao.clear();
        Collection<LabVessel> entries = reworkEjb.getVesselsForRework();

        Assert.assertEquals(entries.size(), 1);

        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));

//        utx.rollback();

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testHappyPathWithValidation() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()));


        List<String> validationMessages = new ArrayList<>();

        for (String barcode : mapBarcodeToTube.keySet()) {
            validationMessages
                    .addAll(reworkEjb.addAndValidateRework(barcode, ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                            LabEventType.PICO_PLATING_BUCKET, "test Rework",
                            WorkflowName.EXOME_EXPRESS.getWorkflowName()));
        }

        productOrderDao.flush();
        productOrderDao.clear();
        Collection<LabVessel> entries = reworkEjb.getVesselsForRework();

        Assert.assertEquals(validationMessages.size(), 1);

        Assert.assertEquals(entries.size(), 1);

        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));


    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testHappyPathWithValidationPreviouslyInBucket() throws Exception {

        List<String> validationMessages = new ArrayList<>();

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()));


        for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1.getBusinessKey(),
                    twoDBarcodedTubeDAO.findByBarcode(currEntry.getKey()));
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);
            bucketDao.flush();
            bucketDao.clear();

            validationMessages
                    .addAll(reworkEjb
                            .addAndValidateRework(currEntry.getKey(), ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                                    LabEventType.PICO_PLATING_BUCKET, "",
                                    WorkflowName.EXOME_EXPRESS.getWorkflowName()));
        }

        productOrderDao.flush();
        productOrderDao.clear();
        Collection<LabVessel> entries = reworkEjb.getVesselsForRework();

        Assert.assertEquals(validationMessages.size(), 0);

        Assert.assertEquals(entries.size(), 1);

        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));


    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testHappyPathWithValidationCurrentlyInBucket() throws Exception {

        List<String> validationMessages = new ArrayList<String>();

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()));


        try {
            for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

                bucketDao.findByName(bucketName);
                pBucket.addEntry(exExProductOrder1.getBusinessKey(),
                        twoDBarcodedTubeDAO.findByBarcode(currEntry.getKey()));
                bucketDao.persist(pBucket);
                bucketDao.flush();
                bucketDao.clear();
            }
            for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

                validationMessages
                        .addAll(reworkEjb
                                .addAndValidateRework(currEntry.getKey(), ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                                        LabEventType.PICO_PLATING_BUCKET, "",
                                        WorkflowName.EXOME_EXPRESS.getWorkflowName()));
            }
            Assert.fail("With the tube in the bucket, Calling Rework should throw an Exception");
        } catch (ValidationException e) {

        }


    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testHappyPathWithAncestorValidation() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()));


        List<String> validationMessages = new ArrayList<String>();

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettalimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT", mapBarcodeToTube, bettaLimsMessageFactory,
                        WorkflowName.EXOME_EXPRESS,
                        bettalimsMessageResource,
                        indexedPlateFactory, staticPlateDAO, reagentDesignDao, twoDBarcodedTubeDAO,
                        appConfig.getUrl());

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
            validationMessages
                    .addAll(reworkEjb.addAndValidateRework(barcode, ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                            LabEventType.PICO_PLATING_BUCKET, "", WorkflowName.EXOME_EXPRESS.getWorkflowName()));
        }

        productOrderDao.flush();
        productOrderDao.clear();
        Collection<LabVessel> entries = reworkEjb.getVesselsForRework();

        Assert.assertEquals(validationMessages.size(), 1);

        Assert.assertEquals(entries.size(), 1);

        for (LabVessel reworkEntry : entries) {
            Assert.assertTrue(hybridSelectionJaxbBuilder.getNormCatchBarcodes().contains(reworkEntry.getLabel()));
        }


    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testMixedDNAWithValidation() throws Exception {

        List<String> validationMessages = new ArrayList<String>();

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime()));


        for (String barcode : mapBarcodeToTube.keySet()) {
            validationMessages
                    .addAll(reworkEjb.addAndValidateRework(barcode, ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                            LabEventType.PICO_PLATING_BUCKET, "", WorkflowName.EXOME_EXPRESS.getWorkflowName()));
        }

        productOrderDao.flush();
        productOrderDao.clear();
        Collection<LabVessel> entries = reworkEjb.getVesselsForRework();

        Assert.assertEquals(validationMessages.size(), 3);

        Assert.assertEquals(entries.size(), 2);

        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));


    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testMixedDNAWithValidationAndAncestors() throws Exception {

        List<String> validationMessages = new ArrayList<String>();

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime()));


        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettalimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT", mapBarcodeToTube, bettaLimsMessageFactory,
                        WorkflowName.EXOME_EXPRESS,
                        bettalimsMessageResource,
                        indexedPlateFactory, staticPlateDAO, reagentDesignDao, twoDBarcodedTubeDAO,
                        appConfig.getUrl());

        productOrderDao.flush();
        productOrderDao.clear();
        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
            validationMessages
                    .addAll(reworkEjb.addAndValidateRework(barcode, ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                            LabEventType.PICO_PLATING_BUCKET, "", WorkflowName.EXOME_EXPRESS.getWorkflowName()));
        }

        productOrderDao.flush();
        productOrderDao.clear();
        Collection<LabVessel> entries = reworkEjb.getVesselsForRework();

        Assert.assertEquals(validationMessages.size(), 3);

        Assert.assertEquals(entries.size(), 2);

        for (LabVessel reworkEntry : entries) {
            Assert.assertTrue(hybridSelectionJaxbBuilder.getNormCatchBarcodes().contains(reworkEntry.getLabel()));
        }


    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testMixedDNAWithValidationAndAncestorsPreviouslyInBucket() throws Exception {

        List<String> validationMessages = new ArrayList<String>();

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime()));


        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettalimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT", mapBarcodeToTube, bettaLimsMessageFactory,
                        WorkflowName.EXOME_EXPRESS,
                        bettalimsMessageResource,
                        indexedPlateFactory, staticPlateDAO, reagentDesignDao, twoDBarcodedTubeDAO,
                        appConfig.getUrl());

        for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1.getBusinessKey(),
                    twoDBarcodedTubeDAO.findByBarcode(currEntry.getKey()));
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);
            bucketDao.flush();
            bucketDao.clear();

        }

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
            validationMessages
                    .addAll(reworkEjb.addAndValidateRework(barcode, ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                            LabEventType.PICO_PLATING_BUCKET, "", WorkflowName.EXOME_EXPRESS.getWorkflowName()));
        }

        productOrderDao.flush();
        productOrderDao.clear();
        Collection<LabVessel> entries = reworkEjb.getVesselsForRework();

        Assert.assertEquals(validationMessages.size(), 3);

        Assert.assertEquals(entries.size(), 2);

        for (LabVessel reworkEntry : entries) {
            Assert.assertTrue(hybridSelectionJaxbBuilder.getNormCatchBarcodes().contains(reworkEntry.getLabel()));
        }


    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testMixedDNAWithValidationAndAncestorsCurrentlyInBucket() throws Exception {

        List<String> validationMessages = new ArrayList<String>();

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime()));


        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettalimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT", mapBarcodeToTube, bettaLimsMessageFactory,
                        WorkflowName.EXOME_EXPRESS,
                        bettalimsMessageResource,
                        indexedPlateFactory, staticPlateDAO, reagentDesignDao, twoDBarcodedTubeDAO,
                        appConfig.getUrl());

        try {
            for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

                bucketDao.findByName(bucketName);
                pBucket.addEntry(exExProductOrder1.getBusinessKey(),
                        twoDBarcodedTubeDAO.findByBarcode(currEntry.getKey()));
                bucketDao.persist(pBucket);
                bucketDao.flush();
                bucketDao.clear();

            }

            for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
                validationMessages
                        .addAll(reworkEjb.addAndValidateRework(barcode, ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                                LabEventType.PICO_PLATING_BUCKET, "",
                                WorkflowName.EXOME_EXPRESS.getWorkflowName()));
            }

            Assert.fail("With an ancestor in the bucket currently, adding Rework should fail");
        } catch (ValidationException e) {

        }

    }


    private void createInitialTubes(@Nonnull List<ProductOrderSample> pos,
                                    @Nonnull String barcodePrefix) {

        int counter = 1;
        for (ProductOrderSample currSamp : pos) {
            String twoDBarcode = "RWIT" + barcodePrefix + counter;
            TwoDBarcodedTube aliquot = new TwoDBarcodedTube(twoDBarcode);
            aliquot.addSample(new MercurySample(currSamp.getBspSampleName()));
            mapBarcodeToTube.put(twoDBarcode, aliquot);
            labVesselDao.persist(aliquot);
            counter++;
        }
    }
}
