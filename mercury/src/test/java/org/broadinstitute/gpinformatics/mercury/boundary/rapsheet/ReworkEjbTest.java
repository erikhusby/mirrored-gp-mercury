package org.broadinstitute.gpinformatics.mercury.boundary.rapsheet;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 *
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class ReworkEjbTest extends Arquillian {

    public static final String SM_SGM_Test_Somatic_1_PATIENT_ID = "PT-1TS1";
    public static final String SM_SGM_Test_Somatic_1_STOCK_SAMP = "SM-SGM_Test_Somatic1";
    public static final String SM_SGM_Test_Somatic_1_COLLAB_SAMP_ID = "CollaboratorSampleX";
    public static final String SM_SGM_Test_Somatic_1_COLL = "Hungarian Goulash";
    public static final String SM_SGM_Test_Somatic_1_VOLUME = "1.3";
    public static final String SM_SGM_Test_Somatic_1_CONC = "0.293";
    public static final String SM_SGM_Test_Somatic_1_COLLAB_PID = "CHTN_SEW";
    public static final String SM_SGM_Test_Somatic_1_DNA = "3.765242738037109374";
    public static final String SM_SGM_Test_Somatic_1_FP =
            "AACTCCCCGGAAAGCTACAAAACG--AATTAGAGTTAATTCTCCAATTGTCTAG--GGACAGGGGGTTCTAAACCCAA--GTCTCCCGCTAGTTTTGGAGAGAGCCGGAGCCCTTTCCAGAGTTCTCTAGTTGGCTGGAGTTCCAAAACTTTCCAATTCTTTGTCGCCGGTTTTACCCCCGGAGAGCTCCCT";
    public static final String SM_SGM_Test_Somatic_1_DISEASE = "Carcinoid Tumor";
    public static final String SM_SGM_Test_Somatic_2_PATIENT_ID = "PT-1TS1";
    public static final String SM_SGM_Test_Somatic_2_STOCK_SAMP = "SM-SGM_Test_Somatic2";
    public static final String SM_SGM_Test_Somatic_3_STOCK_SAMP = "SM-SGM_Test_Somatic3";
    public static final String SM_SGM_Test_Somatic_2_COLLAB_SAMP_ID = "CollaboratorSampleX";
    public static final String SM_SGM_Test_Somatic_2_COLL = "Hungarian Goulash";
    public static final String SM_SGM_Test_Somatic_2_VOLUME = "1.3";
    public static final String SM_SGM_Test_Somatic_2_CONC = "0.293";
    public static final String SM_SGM_Test_Somatic_2_COLLAB_PID = "CHTN_SEW";
    public static final String SM_SGM_Test_Somatic_2_DNA = "3.765242738037109374";
    public static final String SM_SGM_Test_Somatic_2_FP =
            "AACTCCCCGGAAAGCTACAAAACG--AATTAGAGTTAATTCTCCAATTGTCTAG--GGACAGGGGGTTCTAAACCCAA--GTCTCCCGCTAGTTTTGGAGAGAGCCGGAGCCCTTTCCAGAGTTCTCTAGTTGGCTGGAGTTCCAAAACTTTCCAATTCTTTGTCGCCGGTTTTACCCCCGGAGAGCTCCCT";
    public static final String SM_SGM_Test_Somatic_2_DISEASE = "Carcinoid Tumor";
    public static final String SM_SGM_Test_Genomic_1_PATIENT_ID = "PT-1TS1";
    public static final String SM_SGM_Test_Genomic_1_STOCK_SAMP = "SM-SGM_Test_Genomic1";
    public static final String SM_SGM_Test_Genomic_1_COLLAB_SAMP_ID = "CollaboratorSampleX";
    public static final String SM_SGM_Test_Genomic_1_COLL = "Hungarian Goulash";
    public static final String SM_SGM_Test_Genomic_1_VOLUME = "1.3";
    public static final String SM_SGM_Test_Genomic_1_CONC = "0.293";
    public static final String SM_SGM_Test_Genomic_1_COLLAB_PID = "CHTN_SEW";
    public static final String SM_SGM_Test_Genomic_1_DNA = "3.765242738037109374";
    public static final String SM_SGM_Test_Genomic_1_FP =
            "AACTCCCCGGAAAGCTACAAAACG--AATTAGAGTTAATTCTCCAATTGTCTAG--GGACAGGGGGTTCTAAACCCAA--GTCTCCCGCTAGTTTTGGAGAGAGCCGGAGCCCTTTCCAGAGTTCTCTAGTTGGCTGGAGTTCCAAAACTTTCCAATTCTTTGTCGCCGGTTTTACCCCCGGAGAGCTCCCT";
    public static final String SM_SGM_Test_Genomic_1_DISEASE = "Carcinoid Tumor";
    public static final String SM_SGM_Test_Genomic_2_PATIENT_ID = "PT-1TS1";
    public static final String SM_SGM_Test_Genomic_2_STOCK_SAMP = "SM-SGM_Test_Genomic2";
    public static final String SM_SGM_Test_Genomic_3_STOCK_SAMP = "SM-SGM_Test_Genomic3";
    public static final String SM_SGM_Test_Genomic_2_COLLAB_SAMP_ID = "CollaboratorSampleX";
    public static final String SM_SGM_Test_Genomic_2_COLL = "Hungarian Goulash";
    public static final String SM_SGM_Test_Genomic_2_VOLUME = "1.3";
    public static final String SM_SGM_Test_Genomic_2_CONC = "0.293";
    public static final String SM_SGM_Test_Genomic_2_COLLAB_PID = "CHTN_SEW";
    public static final String SM_SGM_Test_Genomic_2_DNA = "3.765242738037109374";
    public static final String SM_SGM_Test_Genomic_2_FP =
            "AACTCCCCGGAAAGCTACAAAACG--AATTAGAGTTAATTCTCCAATTGTCTAG--GGACAGGGGGTTCTAAACCCAA--GTCTCCCGCTAGTTTTGGAGAGAGCCGGAGCCCTTTCCAGAGTTCTCTAGTTGGCTGGAGTTCCAAAACTTTCCAATTCTTTGTCGCCGGTTTTACCCCCGGAGAGCTCCCT";
    public static final String SM_SGM_Test_Genomic_2_DISEASE = "Carcinoid Tumor";

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

    @Inject
    private BSPSampleSearchService bspSampleSearchService;

    @Inject
    private BSPSampleDataFetcher bspSampleDataFetcher;

    private Map<String, TwoDBarcodedTube> mapBarcodeToTube = new HashMap<>();
    private ProductOrder exExProductOrder1;
    private ProductOrder exExProductOrder2;
    private ProductOrder nonExExProductOrder;
    private ProductOrder extraProductOrder;
    private List<ProductOrderSample> bucketReadySamples1;
    private List<ProductOrderSample> bucketReadySamples2;
    private List<ProductOrderSample> bucketSamples1;
    private Bucket pBucket;
    private String bucketName;

    private int existingReworks;
    private Product exExProduct;
    private Product nonExExProduct;
    private ResearchProject researchProject;
    private final Date currDate = new Date();
    private final String genomicSample3 = "SM-SGM_Test_Genomic3" + currDate.getTime();

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(DEV, BSPSampleSearchServiceStub.class);
    }

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        if (reworkEjb == null) {
            return;
        }

        String testPrefix = "SGM_Test";

        final String somaticSample1 = "SM-SGM_Test_Somatic1" + currDate.getTime();
        final String somaticSample2 = "SM-SGM_Test_Somatic2" + currDate.getTime();
        final String somaticSample3 = "SM-SGM_Test_Somatic3" + currDate.getTime();

        final String genomicSample1 = "SM-SGM_Test_Genomic1" + currDate.getTime();
        final String genomicSample2 = "SM-SGM_Test_Genomic2" + currDate.getTime();

        final String SM_SGM_Test_Genomic_1_CONTAINER_ID = "CO-" + testPrefix + currDate.getTime() + "2851";
        final String SM_SGM_Test_Genomic_2_CONTAINER_ID = "CO-" + testPrefix + currDate.getTime() + "2852";
        final String SM_SGM_Test_Genomic_3_CONTAINER_ID = "CO-" + testPrefix + currDate.getTime() + "2855";
        final String SM_SGM_Test_Somatic_1_CONTAINER_ID = "CO-" + testPrefix + currDate.getTime() + "2853";
        final String SM_SGM_Test_Somatic_2_CONTAINER_ID = "CO-" + testPrefix + currDate.getTime() + "2854";
        final String SM_SGM_Test_Somatic_3_CONTAINER_ID = "CO-" + testPrefix + currDate.getTime() + "2856";

        // The injected BSPSampleSearchService must be a stub because it's requested when building the Mercury WAR.
        BSPSampleSearchServiceStub bspSampleSearchServiceStub = (BSPSampleSearchServiceStub) bspSampleSearchService;

        bspSampleSearchServiceStub.addToMap(somaticSample2, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.PARTICIPANT_ID, SM_SGM_Test_Somatic_2_PATIENT_ID);
            put(BSPSampleSearchColumn.ROOT_SAMPLE, BSPSampleSearchServiceStub.ROOT);
            put(BSPSampleSearchColumn.STOCK_SAMPLE, SM_SGM_Test_Somatic_2_STOCK_SAMP);
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, SM_SGM_Test_Somatic_2_COLLAB_SAMP_ID);
            put(BSPSampleSearchColumn.COLLECTION, SM_SGM_Test_Somatic_2_COLL);
            put(BSPSampleSearchColumn.VOLUME, SM_SGM_Test_Somatic_2_VOLUME);
            put(BSPSampleSearchColumn.CONCENTRATION, SM_SGM_Test_Somatic_2_CONC);
            put(BSPSampleSearchColumn.SPECIES, BSPSampleSearchServiceStub.CANINE_SPECIES);
            put(BSPSampleSearchColumn.LSID, BSPSampleSearchServiceStub.LSID_PREFIX + somaticSample2);
            put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, SM_SGM_Test_Somatic_2_COLLAB_PID);
            put(BSPSampleSearchColumn.MATERIAL_TYPE, BSPSampleSearchServiceStub.SOMATIC_MAT_TYPE);
            put(BSPSampleSearchColumn.TOTAL_DNA, SM_SGM_Test_Somatic_2_DNA);
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleDTO.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_SGM_Test_Somatic_2_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BSPSampleDTO.FEMALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BSPSampleDTO.ACTIVE_IND);
            put(BSPSampleSearchColumn.FINGERPRINT, SM_SGM_Test_Somatic_2_FP);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Somatic_2_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, somaticSample2);
        }});

        bspSampleSearchServiceStub.addToMap(somaticSample1, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.PARTICIPANT_ID, SM_SGM_Test_Somatic_1_PATIENT_ID);
            put(BSPSampleSearchColumn.ROOT_SAMPLE, BSPSampleSearchServiceStub.ROOT);
            put(BSPSampleSearchColumn.STOCK_SAMPLE, SM_SGM_Test_Somatic_1_STOCK_SAMP);
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, SM_SGM_Test_Somatic_1_COLLAB_SAMP_ID);
            put(BSPSampleSearchColumn.COLLECTION, SM_SGM_Test_Somatic_1_COLL);
            put(BSPSampleSearchColumn.VOLUME, SM_SGM_Test_Somatic_1_VOLUME);
            put(BSPSampleSearchColumn.CONCENTRATION, SM_SGM_Test_Somatic_1_CONC);
            put(BSPSampleSearchColumn.SPECIES, BSPSampleSearchServiceStub.CANINE_SPECIES);
            put(BSPSampleSearchColumn.LSID, BSPSampleSearchServiceStub.LSID_PREFIX + somaticSample1);
            put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, SM_SGM_Test_Somatic_1_COLLAB_PID);
            put(BSPSampleSearchColumn.MATERIAL_TYPE, BSPSampleSearchServiceStub.SOMATIC_MAT_TYPE);
            put(BSPSampleSearchColumn.TOTAL_DNA, SM_SGM_Test_Somatic_1_DNA);
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleDTO.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_SGM_Test_Somatic_1_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BSPSampleDTO.FEMALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BSPSampleDTO.ACTIVE_IND);
            put(BSPSampleSearchColumn.FINGERPRINT, SM_SGM_Test_Somatic_1_FP);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Somatic_1_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, somaticSample1);
        }});
        bspSampleSearchServiceStub.addToMap(somaticSample3, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.PARTICIPANT_ID, SM_SGM_Test_Somatic_1_PATIENT_ID);
            put(BSPSampleSearchColumn.ROOT_SAMPLE, BSPSampleSearchServiceStub.ROOT);
            put(BSPSampleSearchColumn.STOCK_SAMPLE, SM_SGM_Test_Somatic_3_STOCK_SAMP);
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, SM_SGM_Test_Somatic_1_COLLAB_SAMP_ID);
            put(BSPSampleSearchColumn.COLLECTION, SM_SGM_Test_Somatic_1_COLL);
            put(BSPSampleSearchColumn.VOLUME, SM_SGM_Test_Somatic_1_VOLUME);
            put(BSPSampleSearchColumn.CONCENTRATION, SM_SGM_Test_Somatic_1_CONC);
            put(BSPSampleSearchColumn.SPECIES, BSPSampleSearchServiceStub.CANINE_SPECIES);
            put(BSPSampleSearchColumn.LSID, BSPSampleSearchServiceStub.LSID_PREFIX + somaticSample3);
            put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, SM_SGM_Test_Somatic_1_COLLAB_PID);
            put(BSPSampleSearchColumn.MATERIAL_TYPE, BSPSampleSearchServiceStub.SOMATIC_MAT_TYPE);
            put(BSPSampleSearchColumn.TOTAL_DNA, SM_SGM_Test_Somatic_1_DNA);
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleDTO.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_SGM_Test_Somatic_1_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BSPSampleDTO.FEMALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BSPSampleDTO.ACTIVE_IND);
            put(BSPSampleSearchColumn.FINGERPRINT, SM_SGM_Test_Somatic_1_FP);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Somatic_3_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, somaticSample3);
        }});
        bspSampleSearchServiceStub.addToMap(genomicSample2, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.PARTICIPANT_ID, SM_SGM_Test_Genomic_2_PATIENT_ID);
            put(BSPSampleSearchColumn.ROOT_SAMPLE, BSPSampleSearchServiceStub.ROOT);
            put(BSPSampleSearchColumn.STOCK_SAMPLE, SM_SGM_Test_Genomic_2_STOCK_SAMP);
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, SM_SGM_Test_Genomic_2_COLLAB_SAMP_ID);
            put(BSPSampleSearchColumn.COLLECTION, SM_SGM_Test_Genomic_2_COLL);
            put(BSPSampleSearchColumn.VOLUME, SM_SGM_Test_Genomic_2_VOLUME);
            put(BSPSampleSearchColumn.CONCENTRATION, SM_SGM_Test_Genomic_2_CONC);
            put(BSPSampleSearchColumn.SPECIES, BSPSampleSearchServiceStub.CANINE_SPECIES);
            put(BSPSampleSearchColumn.LSID, BSPSampleSearchServiceStub.LSID_PREFIX + genomicSample2);
            put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, SM_SGM_Test_Genomic_2_COLLAB_PID);
            put(BSPSampleSearchColumn.MATERIAL_TYPE, BSPSampleSearchServiceStub.GENOMIC_MAT_TYPE);
            put(BSPSampleSearchColumn.TOTAL_DNA, SM_SGM_Test_Genomic_2_DNA);
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleDTO.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_SGM_Test_Genomic_2_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BSPSampleDTO.FEMALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BSPSampleDTO.ACTIVE_IND);
            put(BSPSampleSearchColumn.FINGERPRINT, SM_SGM_Test_Genomic_2_FP);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Genomic_2_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, genomicSample2);
        }});

        bspSampleSearchServiceStub.addToMap(genomicSample1, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.PARTICIPANT_ID, SM_SGM_Test_Genomic_1_PATIENT_ID);
            put(BSPSampleSearchColumn.ROOT_SAMPLE, BSPSampleSearchServiceStub.ROOT);
            put(BSPSampleSearchColumn.STOCK_SAMPLE, SM_SGM_Test_Genomic_1_STOCK_SAMP);
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, SM_SGM_Test_Genomic_1_COLLAB_SAMP_ID);
            put(BSPSampleSearchColumn.COLLECTION, SM_SGM_Test_Genomic_1_COLL);
            put(BSPSampleSearchColumn.VOLUME, SM_SGM_Test_Genomic_1_VOLUME);
            put(BSPSampleSearchColumn.CONCENTRATION, SM_SGM_Test_Genomic_1_CONC);
            put(BSPSampleSearchColumn.SPECIES, BSPSampleSearchServiceStub.CANINE_SPECIES);
            put(BSPSampleSearchColumn.LSID, BSPSampleSearchServiceStub.LSID_PREFIX + genomicSample1);
            put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, SM_SGM_Test_Genomic_1_COLLAB_PID);
            put(BSPSampleSearchColumn.MATERIAL_TYPE, BSPSampleSearchServiceStub.GENOMIC_MAT_TYPE);
            put(BSPSampleSearchColumn.TOTAL_DNA, SM_SGM_Test_Genomic_1_DNA);
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleDTO.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_SGM_Test_Genomic_1_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BSPSampleDTO.FEMALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BSPSampleDTO.ACTIVE_IND);
            put(BSPSampleSearchColumn.FINGERPRINT, SM_SGM_Test_Genomic_1_FP);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Genomic_1_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, genomicSample2);
        }});
        bspSampleSearchServiceStub.addToMap(genomicSample3, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.PARTICIPANT_ID, SM_SGM_Test_Genomic_1_PATIENT_ID);
            put(BSPSampleSearchColumn.ROOT_SAMPLE, BSPSampleSearchServiceStub.ROOT);
            put(BSPSampleSearchColumn.STOCK_SAMPLE, SM_SGM_Test_Genomic_3_STOCK_SAMP);
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, SM_SGM_Test_Genomic_1_COLLAB_SAMP_ID);
            put(BSPSampleSearchColumn.COLLECTION, SM_SGM_Test_Genomic_1_COLL);
            put(BSPSampleSearchColumn.VOLUME, SM_SGM_Test_Genomic_1_VOLUME);
            put(BSPSampleSearchColumn.CONCENTRATION, SM_SGM_Test_Genomic_1_CONC);
            put(BSPSampleSearchColumn.SPECIES, BSPSampleSearchServiceStub.CANINE_SPECIES);
            put(BSPSampleSearchColumn.LSID, BSPSampleSearchServiceStub.LSID_PREFIX + genomicSample3);
            put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, SM_SGM_Test_Genomic_1_COLLAB_PID);
            put(BSPSampleSearchColumn.MATERIAL_TYPE, BSPSampleSearchServiceStub.GENOMIC_MAT_TYPE);
            put(BSPSampleSearchColumn.TOTAL_DNA, SM_SGM_Test_Genomic_1_DNA);
            put(BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleDTO.NORMAL_IND);
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_SGM_Test_Genomic_1_DISEASE);
            put(BSPSampleSearchColumn.GENDER, BSPSampleDTO.FEMALE_IND);
            put(BSPSampleSearchColumn.STOCK_TYPE, BSPSampleDTO.ACTIVE_IND);
            put(BSPSampleSearchColumn.FINGERPRINT, SM_SGM_Test_Genomic_1_FP);
            put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Genomic_3_CONTAINER_ID);
            put(BSPSampleSearchColumn.SAMPLE_ID, genomicSample3);
        }});


        String rpJiraTicketKey = "RP-SGM-Rework_tst1" + currDate.getTime() + "RP";
        researchProject = new ResearchProject(bspUserList.getByUsername("scottmat").getUserId(),
                "Rework Integration Test RP " + currDate.getTime() + "RP", "Rework Integration Test RP", false);
        researchProject.setJiraTicketKey(rpJiraTicketKey);
        researchProjectDao.persist(researchProject);

        exExProduct = productDao.findByPartNumber(
                BettalimsMessageResourceTest.mapWorkflowToPartNum.get(WorkflowName.EXOME_EXPRESS.getWorkflowName()));
        nonExExProduct = productDao.findByPartNumber(
                BettalimsMessageResourceTest.mapWorkflowToPartNum.get(WorkflowName.WHOLE_GENOME.getWorkflowName()));

        bucketReadySamples1  = new ArrayList<>(2);
        bucketReadySamples1.add(new ProductOrderSample(genomicSample1));
        bucketReadySamples1.add(new ProductOrderSample(genomicSample2));

        bucketReadySamples2 = new ArrayList<>(2);
        bucketReadySamples2.add(new ProductOrderSample(genomicSample3));
        bucketReadySamples2.add(new ProductOrderSample(somaticSample3));

        bucketSamples1 = new ArrayList<>(2);
        bucketSamples1.add(new ProductOrderSample(somaticSample1));
        bucketSamples1.add(new ProductOrderSample(somaticSample2));

        exExProductOrder1 =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                        "Rework Integration TestOrder 1" + currDate.getTime(),
                        bucketReadySamples1, "GSP-123", exExProduct, researchProject);
        exExProductOrder1.setProduct(exExProduct);
        exExProductOrder1.prepareToSave(bspUserList.getByUsername("scottmat"));
        String pdo1JiraKey = "PDO-SGM-RWINT_tst" + currDate.getTime() + 1;
        exExProductOrder1.setJiraTicketKey(pdo1JiraKey);
        productOrderDao.persist(exExProductOrder1);

        exExProductOrder2 =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                        "Rework Integration TestOrder 2" + currDate.getTime(),
                        bucketReadySamples2, "GSP-123", exExProduct, researchProject);
        exExProductOrder2.setProduct(exExProduct);
        exExProductOrder2.prepareToSave(bspUserList.getByUsername("scottmat"));
        String pdo2JiraKey = "PDO-SGM-RWINT_tst" + currDate.getTime() + 2;
        exExProductOrder2.setJiraTicketKey(pdo2JiraKey);
        productOrderDao.persist(exExProductOrder2);

        nonExExProductOrder =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                        "Rework Integration TestOrder 3" + currDate.getTime(),
                        bucketSamples1, "GSP-123", nonExExProduct, researchProject);
        nonExExProductOrder.setProduct(nonExExProduct);
        nonExExProductOrder.prepareToSave(bspUserList.getByUsername("scottmat"));
        String pdo3JiraKey = "PDO-SGM-RWINT_tst" + currDate.getTime() + 3;
        nonExExProductOrder.setJiraTicketKey(pdo3JiraKey);
        productOrderDao.persist(nonExExProductOrder);

        extraProductOrder = new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                "Rework Integration TestOrder 4" + currDate.getTime(),
                Collections.singletonList(new ProductOrderSample(genomicSample3)), "GSP-123", exExProduct,
                researchProject);
        extraProductOrder.prepareToSave(bspUserList.getByUsername("scottmat"));
        String pdo4JiraKey = "PDO-SGM-WRINT_tst" + currDate.getTime() + 4;
        extraProductOrder.setJiraTicketKey(pdo4JiraKey);
        productOrderDao.persist(extraProductOrder);

        WorkflowBucketDef bucketDef = LabEventHandler
                .findBucketDef(WorkflowName.EXOME_EXPRESS.getWorkflowName(), LabEventType.PICO_PLATING_BUCKET);

        bucketName = bucketDef.getName();

        pBucket = bucketDao.findByName(bucketName);

        if (pBucket == null) {

            pBucket = new Bucket(bucketName);
        }

        existingReworks = reworkEjb.getVesselsForRework().size();

        bucketDao.persist(pBucket);

    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        if (reworkEjb == null) {
            return;
        }

        mapBarcodeToTube.clear();
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testHappyPath() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime())+"tst1");

        for (String barcode : mapBarcodeToTube.keySet()) {
            reworkEjb.addRework(barcode, ReworkEntry.ReworkReason.UNKNOWN_ERROR, LabEventType.PICO_PLATING_BUCKET,
                    "test Rework",
                    WorkflowName.EXOME_EXPRESS.getWorkflowName());
        }

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework();

        Assert.assertEquals(entries.size(), existingReworks + mapBarcodeToTube.size());

        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testHappyPathFindCandidates() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime())+"tst2");

        List<ReworkEjb.ReworkCandidate> candiates = new ArrayList<>();

        for (String barcode : mapBarcodeToTube.keySet()) {

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1.getBusinessKey(),
                    twoDBarcodedTubeDAO.findByBarcode(barcode));
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);

            candiates.addAll(reworkEjb.findReworkCandidates(barcode));
        }

        Assert.assertEquals(candiates.size(), mapBarcodeToTube.size());

        for(ReworkEjb.ReworkCandidate candidate:candiates) {
            Assert.assertTrue(mapBarcodeToTube.keySet().contains(candidate.getTubeBarcode()));
        }

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testSingleSampleTwoPDOs() throws Exception {

        Collection<ReworkEjb.ReworkCandidate> candidates = reworkEjb.findReworkCandidates(genomicSample3);
        Assert.assertEquals(candidates.size(), 2);

        /*
         * Set up a collection of all PDOs that there should be candidates for. The collection will be depleted as
         * candidates for the PDOs are found to make sure that all PDOs are accounted for.
         */
        Set<String> expectedPDOs = new HashSet<>();
        expectedPDOs.add(exExProductOrder2.getBusinessKey());
        expectedPDOs.add(extraProductOrder.getBusinessKey());

        for(ReworkEjb.ReworkCandidate candidate : candidates) {
            Assert.assertEquals(candidate.getSampleKey(), genomicSample3);
            Assert.assertTrue(candidate.isValid());
            Assert.assertTrue(expectedPDOs.contains(candidate.getProductOrderKey()));
            expectedPDOs.remove(candidate.getProductOrderKey());
        }

        // Make sure all that candidates were found for all expected PDOs.
        Assert.assertTrue(expectedPDOs.isEmpty());
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testHappyPathFindCandidatesWithAncestors() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime())+"tst3");

        List<ReworkEjb.ReworkCandidate> candidates = new ArrayList<>();

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1.getBusinessKey(),
                    twoDBarcodedTubeDAO.findByBarcode(currEntry.getKey()));
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);
        }

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettalimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT4"+currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                        WorkflowName.EXOME_EXPRESS,
                        bettalimsMessageResource,
                        indexedPlateFactory, staticPlateDAO, reagentDesignDao, twoDBarcodedTubeDAO,
                        appConfig.getUrl(), 2);

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1.getBusinessKey(),
                    twoDBarcodedTubeDAO.findByBarcode(barcode));
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);

            candidates.addAll(reworkEjb.findReworkCandidates(barcode));
        }

        Assert.assertEquals(candidates.size(), hybridSelectionJaxbBuilder.getNormCatchBarcodes().size());

        for(ReworkEjb.ReworkCandidate candidate:candidates) {
            Assert.assertTrue(hybridSelectionJaxbBuilder.getNormCatchBarcodes().contains(candidate.getTubeBarcode()));
        }

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testNonExomePathFindCandidates() throws Exception {

        createInitialTubes(bucketSamples1, String.valueOf((new Date()).getTime())+"tst4");

        List<ReworkEjb.ReworkCandidate> candidates = new ArrayList<>();

        for (String barcode : mapBarcodeToTube.keySet()) {

            //TODO SGM:  This way of forcing finding a PDO for non Exome Express seems sketchy since non EXEX
            // product orders cannot enter a bucket to get a bucket
            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(nonExExProductOrder.getBusinessKey(),
                    twoDBarcodedTubeDAO.findByBarcode(barcode));
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);

            candidates.addAll(reworkEjb.findReworkCandidates(barcode));
        }

        Assert.assertEquals(candidates.size(), mapBarcodeToTube.size());

        for(ReworkEjb.ReworkCandidate candidate:candidates) {
            Assert.assertFalse(candidate.isValid());
            Assert.assertTrue(mapBarcodeToTube.keySet().contains(candidate.getTubeBarcode()));
        }

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testNonExomePathFindCandidatesWithAncestors() throws Exception {

        createInitialTubes(bucketSamples1, String.valueOf((new Date()).getTime())+"tst5");

        List<ReworkEjb.ReworkCandidate> candidates = new ArrayList<>();

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(nonExExProductOrder.getBusinessKey(),
                    twoDBarcodedTubeDAO.findByBarcode(currEntry.getKey()));
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);
        }

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettalimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT4"+currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                        WorkflowName.EXOME_EXPRESS,
                        bettalimsMessageResource,
                        indexedPlateFactory, staticPlateDAO, reagentDesignDao, twoDBarcodedTubeDAO,
                        appConfig.getUrl(), 2);

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {

            //TODO SGM:  This way of forcing finding a PDO for non Exome Express seems sketchy since non EXEX
            // product orders cannot enter a bucket to get a bucket
            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(nonExExProductOrder.getBusinessKey(),
                    twoDBarcodedTubeDAO.findByBarcode(barcode));
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);

            candidates.addAll(reworkEjb.findReworkCandidates(barcode));
        }

        Assert.assertEquals(candidates.size(), hybridSelectionJaxbBuilder.getNormCatchBarcodes().size());

        for(ReworkEjb.ReworkCandidate candidate:candidates) {
            Assert.assertFalse(candidate.isValid());
            Assert.assertTrue(hybridSelectionJaxbBuilder.getNormCatchBarcodes().contains(candidate.getTubeBarcode()));
        }

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testHappyPathWithValidation() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime())+"tst6");

        List<String> validationMessages = new ArrayList<>();

        for (String barcode : mapBarcodeToTube.keySet()) {
            validationMessages
                    .addAll(reworkEjb.addAndValidateRework(barcode, ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                            LabEventType.PICO_PLATING_BUCKET, "test Rework",
                            WorkflowName.EXOME_EXPRESS.getWorkflowName()));
        }

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework();

        Assert.assertEquals(validationMessages.size(), 2);

        Assert.assertEquals(entries.size(), existingReworks + mapBarcodeToTube.size());

        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testHappyPathWithValidationPreviouslyInBucket() throws Exception {

        List<String> validationMessages = new ArrayList<>();

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime())+"tst7");

        for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1.getBusinessKey(),
                    twoDBarcodedTubeDAO.findByBarcode(currEntry.getKey()));
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);

            validationMessages
                    .addAll(reworkEjb
                            .addAndValidateRework(currEntry.getKey(), ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                                    LabEventType.PICO_PLATING_BUCKET, "",
                                    WorkflowName.EXOME_EXPRESS.getWorkflowName()));
        }

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework();

        Assert.assertEquals(validationMessages.size(), 0);

        Assert.assertEquals(entries.size(), existingReworks + mapBarcodeToTube.size());

        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testHappyPathWithValidationCurrentlyInBucket() throws Exception {

        List<String> validationMessages = new ArrayList<String>();

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime())+"tst8");

        try {
            for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

                bucketDao.findByName(bucketName);
                pBucket.addEntry(exExProductOrder1.getBusinessKey(),
                        twoDBarcodedTubeDAO.findByBarcode(currEntry.getKey()));
                bucketDao.persist(pBucket);
            }
            for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

                reworkEjb.addAndValidateRework(currEntry.getKey(), ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                        LabEventType.PICO_PLATING_BUCKET, "", WorkflowName.EXOME_EXPRESS.getWorkflowName());
            }
            Assert.fail("With the tube in the bucket, Calling Rework should throw an Exception");
        } catch (ValidationException e) {

        }

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testHappyPathWithAncestorValidation() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime())+"tst9");

        List<String> validationMessages = new ArrayList<String>();

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettalimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT1"+currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                        WorkflowName.EXOME_EXPRESS,
                        bettalimsMessageResource,
                        indexedPlateFactory, staticPlateDAO, reagentDesignDao, twoDBarcodedTubeDAO,
                        appConfig.getUrl(), 2);

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
            validationMessages
                    .addAll(reworkEjb.addAndValidateRework(barcode, ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                            LabEventType.PICO_PLATING_BUCKET, "", WorkflowName.EXOME_EXPRESS.getWorkflowName()));
        }

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework();

        Assert.assertEquals(validationMessages.size(), 1);

        Assert.assertEquals(entries.size(), existingReworks + hybridSelectionJaxbBuilder.getNormCatchBarcodes().size());

        validateBarcodeExistence(hybridSelectionJaxbBuilder, entries);

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testMixedDNAWithValidation() throws Exception {

        List<String> validationMessages = new ArrayList<String>();

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime())+"tst10");

        for (String barcode : mapBarcodeToTube.keySet()) {
            validationMessages
                    .addAll(reworkEjb.addAndValidateRework(barcode, ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                            LabEventType.PICO_PLATING_BUCKET, "", WorkflowName.EXOME_EXPRESS.getWorkflowName()));
        }

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework();

        Assert.assertEquals(validationMessages.size(), 3);

        Assert.assertEquals(entries.size(), existingReworks + mapBarcodeToTube.size());

        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testMixedDNAWithValidationAndAncestors() throws Exception {

        List<String> validationMessages = new ArrayList<String>();

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime())+"tst11");

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettalimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT2"+currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                        WorkflowName.EXOME_EXPRESS,
                        bettalimsMessageResource,
                        indexedPlateFactory, staticPlateDAO, reagentDesignDao, twoDBarcodedTubeDAO,
                        appConfig.getUrl(), 2);

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
            validationMessages
                    .addAll(reworkEjb.addAndValidateRework(barcode, ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                            LabEventType.PICO_PLATING_BUCKET, "", WorkflowName.EXOME_EXPRESS.getWorkflowName()));
        }

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework();

        Assert.assertEquals(validationMessages.size(), 2);

        Assert.assertEquals(entries.size(), existingReworks + hybridSelectionJaxbBuilder.getNormCatchBarcodes().size());

        validateBarcodeExistence(hybridSelectionJaxbBuilder, entries);
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testMixedDNAWithValidationAndAncestorsPreviouslyInBucket() throws Exception {

        List<String> validationMessages = new ArrayList<String>();

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime())+"tst12");

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder2.getBusinessKey(),
                    twoDBarcodedTubeDAO.findByBarcode(currEntry.getKey()));
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);
        }

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettalimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT4"+currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                        WorkflowName.EXOME_EXPRESS,
                        bettalimsMessageResource,
                        indexedPlateFactory, staticPlateDAO, reagentDesignDao, twoDBarcodedTubeDAO,
                        appConfig.getUrl(), 2);

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
            validationMessages
                    .addAll(reworkEjb.addAndValidateRework(barcode, ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                            LabEventType.PICO_PLATING_BUCKET, "", WorkflowName.EXOME_EXPRESS.getWorkflowName()));
        }

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework();

        Assert.assertEquals(validationMessages.size(), 1);

        Assert.assertEquals(entries.size(), existingReworks + hybridSelectionJaxbBuilder.getNormCatchBarcodes().size());

        validateBarcodeExistence(hybridSelectionJaxbBuilder, entries);

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testMixedDNAWithValidationAndAncestorsCurrentlyInBucket() throws Exception {

        List<String> validationMessages = new ArrayList<String>();

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime())+"tst13");

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettalimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT3"+currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                        WorkflowName.EXOME_EXPRESS,
                        bettalimsMessageResource,
                        indexedPlateFactory, staticPlateDAO, reagentDesignDao, twoDBarcodedTubeDAO,
                        appConfig.getUrl(), 2);

            for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

                bucketDao.findByName(bucketName);
                pBucket.addEntry(exExProductOrder2.getBusinessKey(),
                        twoDBarcodedTubeDAO.findByBarcode(currEntry.getKey()));
                bucketDao.persist(pBucket);
            }

            for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
                validationMessages
                        .addAll(reworkEjb.addAndValidateRework(barcode, ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                                LabEventType.PICO_PLATING_BUCKET, "",
                                WorkflowName.EXOME_EXPRESS.getWorkflowName()));
            }

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework();

        Assert.assertEquals(validationMessages.size(), 1);

        Assert.assertEquals(entries.size(), existingReworks + hybridSelectionJaxbBuilder.getNormCatchBarcodes().size());

        validateBarcodeExistence(hybridSelectionJaxbBuilder, entries);
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testFindCandidatesMultiplePdosNoBuckets() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime())+"tst2");

        List<ReworkEjb.ReworkCandidate> candiates = new ArrayList<>();

        ProductOrder duplicatePO =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                        "Rework Integration TestOrder DuplicatePDORework" + currDate.getTime(),
                        bucketReadySamples1, "GSP-123", exExProduct, researchProject);
        duplicatePO.setProduct(exExProduct);
        duplicatePO.prepareToSave(bspUserList.getByUsername("scottmat"));
        String dupePdo1JiraKey = "PDO-SGM-RWINT_Dupe_tst" + currDate.getTime() + 1;
        duplicatePO.setJiraTicketKey(dupePdo1JiraKey);
        productOrderDao.persist(duplicatePO);

        for (Map.Entry<String, TwoDBarcodedTube> tubes : mapBarcodeToTube.entrySet()) {

            candiates.addAll(reworkEjb.findReworkCandidates(tubes.getValue().getSampleNames().iterator().next()));
        }

        Assert.assertEquals(candiates.size(), mapBarcodeToTube.size() * 2);

        for(ReworkEjb.ReworkCandidate candidate:candiates) {
            Assert.assertTrue(mapBarcodeToTube.keySet().contains(candidate.getTubeBarcode()));
        }

    }


    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testFindCandidatesMultiplePdosWithBuckets() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime())+"tst2");

        List<ReworkEjb.ReworkCandidate> candiates = new ArrayList<>();

        ProductOrder duplicatePO =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                        "Rework Integration TestOrder DuplicatePDORework" + currDate.getTime(),
                        bucketReadySamples1, "GSP-123", exExProduct, researchProject);
        duplicatePO.setProduct(exExProduct);
        duplicatePO.prepareToSave(bspUserList.getByUsername("scottmat"));
        String dupePdo1JiraKey = "PDO-SGM-RWINT_Dupe_tst" + currDate.getTime() + 1;
        duplicatePO.setJiraTicketKey(dupePdo1JiraKey);
        productOrderDao.persist(duplicatePO);

        for (Map.Entry<String, TwoDBarcodedTube> tubes : mapBarcodeToTube.entrySet()) {

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(duplicatePO.getBusinessKey(),
                    twoDBarcodedTubeDAO.findByBarcode(tubes.getKey()));
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);

            candiates.addAll(reworkEjb.findReworkCandidates(tubes.getValue().getSampleNames().iterator().next()));
        }

        Assert.assertEquals(candiates.size(), mapBarcodeToTube.size());

        for(ReworkEjb.ReworkCandidate candidate:candiates) {
            Assert.assertTrue(mapBarcodeToTube.keySet().contains(candidate.getTubeBarcode()));
        }

    }



    private void createInitialTubes(@Nonnull List<ProductOrderSample> pos,
                                    @Nonnull String barcodePrefix) {

        int counter = 1;
        for (ProductOrderSample currSamp : pos) {
            String twoDBarcode = "RWIT" + barcodePrefix + counter;
            TwoDBarcodedTube aliquot = new TwoDBarcodedTube(twoDBarcode);
            aliquot.addSample(new MercurySample(currSamp.getBspSampleName(),
                    bspSampleDataFetcher.fetchSingleSampleFromBSP(currSamp.getBspSampleName())));
            mapBarcodeToTube.put(twoDBarcode, aliquot);
            labVesselDao.persist(aliquot);
            counter++;
        }
    }

    private void validateBarcodeExistence(HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder,
                                          Collection<LabVessel> entries) {
        List<String> reworkEntryBarcodes = new ArrayList<>(entries.size());

        for (LabVessel reworkEntry : entries) {
            reworkEntryBarcodes.add(reworkEntry.getLabel());
        }

        for(String normCatchBarcode:hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {

            Assert.assertTrue(reworkEntryBarcodes.contains(normCatchBarcode),
                    "Norm catch barcode with vessel of " + normCatchBarcode
                    + " Not found in the list of rework entry barcodes");
        }
    }

}
