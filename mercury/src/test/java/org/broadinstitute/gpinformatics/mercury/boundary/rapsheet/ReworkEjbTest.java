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
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResourceTest;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.ReworkReasonDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkReason;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
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
    private ReworkEjb reworkEjb;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BucketDao bucketDao;

    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private TwoDBarcodedTubeDao twoDBarcodedTubeDao;

    @Inject
    private AppConfig appConfig;

    @Inject
    private BSPSampleSearchService bspSampleSearchService;

    @Inject
    private BSPSampleDataFetcher bspSampleDataFetcher;

    @Inject
    private LabBatchEjb labBatchEJB;

    @Inject
    private ReworkReasonDao reworkReasonDao;


    private Map<String, TwoDBarcodedTube> mapBarcodeToTube = new HashMap<>();
    private ProductOrder exExProductOrder1;
    private ProductOrder exExProductOrder2;
    private ProductOrder draftProductOrder;
    private ProductOrder nonExExProductOrder;
    private ProductOrder extraProductOrder;
    private List<ProductOrderSample> bucketReadySamples1;
    private List<ProductOrderSample> bucketReadySamples2;
    private List<ProductOrderSample> bucketSamples1;
    private List<ProductOrderSample> bucketSamplesDraft;
    private Bucket pBucket;
    private String bucketName;

    private int existingReworks;
    private Product exExProduct;
    private Product nonExExProduct;
    private ResearchProject researchProject;
    private final Date currDate = new Date();
    private String genomicSample1;
    private String genomicSample2;
    private String genomicSample3;
    private String genomicSampleDraft;
    private String somaticSample1;
    private String somaticSample2;
    private String somaticSample3;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder
                .buildMercuryWarWithAlternatives(DEV, BSPSampleSearchServiceStub.class, JiraServiceStub.class);
    }

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        if (reworkEjb == null) {
            return;
        }

        String testPrefix = "SGM_Test_RWIT";

        somaticSample1 = "SM-SGM_Test_Somatic1" + currDate.getTime();
        somaticSample2 = "SM-SGM_Test_Somatic2" + currDate.getTime();
        somaticSample3 = "SM-SGM_Test_Somatic3" + currDate.getTime();
        genomicSample1 = "SM-SGM_Test_Genomic1" + currDate.getTime();
        genomicSample2 = "SM-SGM_Test_Genomic2" + currDate.getTime();
        genomicSample3 = "SM-SGM_Test_Genomic3" + currDate.getTime();
        genomicSampleDraft = "SM-SGM_Test_GenomicDraft" + currDate.getTime();

        final String SM_SGM_Test_Genomic_1_CONTAINER_ID = "A0-" + testPrefix + currDate.getTime() + "2851";
        final String SM_SGM_Test_Genomic_2_CONTAINER_ID = "A0-" + testPrefix + currDate.getTime() + "2852";
        final String SM_SGM_Test_Genomic_3_CONTAINER_ID = "A0-" + testPrefix + currDate.getTime() + "2855";
        final String SM_SGM_Test_Somatic_1_CONTAINER_ID = "A0-" + testPrefix + currDate.getTime() + "2853";
        final String SM_SGM_Test_Somatic_2_CONTAINER_ID = "A0-" + testPrefix + currDate.getTime() + "2854";
        final String SM_SGM_Test_Somatic_3_CONTAINER_ID = "A0-" + testPrefix + currDate.getTime() + "2856";

        // The injected BSPSampleSearchService must be a stub because it's requested when building the Mercury WAR.
        BSPSampleSearchServiceStub bspSampleSearchServiceStub = (BSPSampleSearchServiceStub) bspSampleSearchService;

        bspSampleSearchServiceStub
                .addToMap(somaticSample2, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
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

        bspSampleSearchServiceStub
                .addToMap(somaticSample1, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
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
        bspSampleSearchServiceStub
                .addToMap(somaticSample3, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
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
        bspSampleSearchServiceStub
                .addToMap(genomicSample2, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
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

        bspSampleSearchServiceStub
                .addToMap(genomicSample1, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
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
                    put(BSPSampleSearchColumn.SAMPLE_ID, genomicSample1);
                }});
        bspSampleSearchServiceStub
                .addToMap(genomicSample3, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
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
        bspSampleSearchServiceStub
                .addToMap(genomicSampleDraft, new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
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
                    put(BSPSampleSearchColumn.SAMPLE_ID, genomicSampleDraft);
                }});


        String rpJiraTicketKey = "RP-SGM-Rework_tst1" + currDate.getTime() + "RP";
        researchProject = new ResearchProject(bspUserList.getByUsername("scottmat").getUserId(),
                "Rework Integration Test RP " + currDate.getTime() + "RP", "Rework Integration Test RP", false);
        researchProject.setJiraTicketKey(rpJiraTicketKey);
        researchProjectDao.persist(researchProject);

        exExProduct = productDao.findByPartNumber(
                BettaLimsMessageResourceTest.mapWorkflowToPartNum.get(Workflow.AGILENT_EXOME_EXPRESS));
        nonExExProduct = productDao.findByPartNumber(
                BettaLimsMessageResourceTest.mapWorkflowToPartNum.get(Workflow.WHOLE_GENOME));

        bucketReadySamples1 = new ArrayList<>(2);
        bucketReadySamples1.add(new ProductOrderSample(genomicSample1));
        bucketReadySamples1.add(new ProductOrderSample(genomicSample2));

        bucketReadySamples2 = new ArrayList<>(2);
        bucketReadySamples2.add(new ProductOrderSample(genomicSample3));
        bucketReadySamples2.add(new ProductOrderSample(somaticSample3));

        bucketSamples1 = new ArrayList<>(2);
        bucketSamples1.add(new ProductOrderSample(somaticSample1));
        bucketSamples1.add(new ProductOrderSample(somaticSample2));

        bucketSamplesDraft = Collections.singletonList(new ProductOrderSample(genomicSampleDraft));

        exExProductOrder1 =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                        "Rework Integration TestOrder 1" + currDate.getTime(),
                        bucketReadySamples1, "GSP-123", exExProduct, researchProject);
        exExProductOrder1.setProduct(exExProduct);
        exExProductOrder1.prepareToSave(bspUserList.getByUsername("scottmat"));
        String pdo1JiraKey = "PDO-SGM-RWINT_tst" + currDate.getTime() + 1;
        exExProductOrder1.setJiraTicketKey(pdo1JiraKey);
        exExProductOrder1.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrderDao.persist(exExProductOrder1);

        exExProductOrder2 =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                        "Rework Integration TestOrder 2" + currDate.getTime(),
                        bucketReadySamples2, "GSP-123", exExProduct, researchProject);
        exExProductOrder2.setProduct(exExProduct);
        exExProductOrder2.prepareToSave(bspUserList.getByUsername("scottmat"));
        String pdo2JiraKey = "PDO-SGM-RWINT_tst" + currDate.getTime() + 2;
        exExProductOrder2.setJiraTicketKey(pdo2JiraKey);
        exExProductOrder2.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrderDao.persist(exExProductOrder2);

        nonExExProductOrder =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                        "Rework Integration TestOrder 3" + currDate.getTime(),
                        bucketSamples1, "GSP-123", nonExExProduct, researchProject);
        nonExExProductOrder.setProduct(nonExExProduct);
        nonExExProductOrder.prepareToSave(bspUserList.getByUsername("scottmat"));
        String pdo3JiraKey = "PDO-SGM-RWINT_tst" + currDate.getTime() + 3;
        nonExExProductOrder.setJiraTicketKey(pdo3JiraKey);
        nonExExProductOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrderDao.persist(nonExExProductOrder);

        extraProductOrder = new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                "Rework Integration TestOrder 4" + currDate.getTime(),
                Collections.singletonList(new ProductOrderSample(genomicSample3)), "GSP-123", exExProduct,
                researchProject);
        extraProductOrder.prepareToSave(bspUserList.getByUsername("scottmat"));
        String pdo4JiraKey = "PDO-SGM-RWINT_tst" + currDate.getTime() + 4;
        extraProductOrder.setJiraTicketKey(pdo4JiraKey);
        extraProductOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrderDao.persist(extraProductOrder);

        draftProductOrder =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                        "Rework Integration TestOrder draft" + currDate.getTime(), bucketSamplesDraft, "GSP-123",
                        exExProduct, researchProject);
        draftProductOrder.setProduct(exExProduct);
        draftProductOrder.prepareToSave(bspUserList.getByUsername("scottmat"));
        String draftPdoJiraKey = "PDO-SGM-RWINT_tst" + currDate.getTime() + "draft";
        draftProductOrder.setJiraTicketKey(draftPdoJiraKey);
        draftProductOrder.setOrderStatus(ProductOrder.OrderStatus.Draft);
        productOrderDao.persist(draftProductOrder);


        WorkflowBucketDef bucketDef = ProductWorkflowDefVersion
                .findBucketDef(Workflow.AGILENT_EXOME_EXPRESS, LabEventType.PICO_PLATING_BUCKET);

        bucketName = bucketDef.getName();

        pBucket = bucketDao.findByName(bucketName);

        if (pBucket == null) {

            pBucket = new Bucket(bucketName);
        }

        existingReworks = reworkEjb.getVesselsForRework("Pico/Plating Bucket").size();

        bucketDao.persist(pBucket);
        resetExExProductWorkflow();
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        if (reworkEjb == null) {
            return;
        }
        exExProductOrder1 = productOrderDao.findByBusinessKey(exExProductOrder1.getBusinessKey());
        exExProductOrder1.setOrderStatus(ProductOrder.OrderStatus.Completed);
        exExProductOrder2 = productOrderDao.findByBusinessKey(exExProductOrder2.getBusinessKey());
        exExProductOrder2.setOrderStatus(ProductOrder.OrderStatus.Completed);
        nonExExProductOrder = productOrderDao.findByBusinessKey(nonExExProductOrder.getBusinessKey());
        nonExExProductOrder.setOrderStatus(ProductOrder.OrderStatus.Completed);
        extraProductOrder = productOrderDao.findByBusinessKey(extraProductOrder.getBusinessKey());
        extraProductOrder.setOrderStatus(ProductOrder.OrderStatus.Completed);
        resetExExProductWorkflow();

        List<ProductOrder> ordersToUpdate = new ArrayList<>();

        Collections
                .addAll(ordersToUpdate, exExProductOrder1, exExProductOrder2, nonExExProductOrder, extraProductOrder);
        for (TwoDBarcodedTube vessel : mapBarcodeToTube.values()) {
            TwoDBarcodedTube tempVessel = twoDBarcodedTubeDao.findByBarcode(vessel.getLabel());
            for (BucketEntry bucketEntry : tempVessel.getBucketEntries()) {
                bucketEntry.setStatus(BucketEntry.Status.Archived);
            }
            twoDBarcodedTubeDao.persist(tempVessel);
        }
        productOrderDao.persistAll(ordersToUpdate);
        productOrderDao.flush();
        productOrderDao.clear();
        mapBarcodeToTube.clear();
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void testHappyPath() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst1");

        for (Map.Entry<String, TwoDBarcodedTube> entry : mapBarcodeToTube.entrySet()) {
            TwoDBarcodedTube vessel = entry.getValue();
            ReworkEjb.BucketCandidate bucketCandidate =
                    new ReworkEjb.BucketCandidate(vessel.getLabel(), exExProductOrder1.getBusinessKey());
            bucketCandidate.setReworkItem(true);
            String unknownReason =
                    ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
            reworkEjb.addAndValidateCandidates(Collections.singleton(
                    bucketCandidate),
                    unknownReason, "test Rework", "scottmat",
                    Workflow.AGILENT_EXOME_EXPRESS, "Pico/Plating Bucket");
        }

        bucketDao.clear();

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework("Pico/Plating Bucket");

        Assert.assertEquals(entries.size(), existingReworks + mapBarcodeToTube.size());

        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testHappyPathFindCandidatesByBarcode() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst2");

        List<ReworkEjb.BucketCandidate> candidates = new ArrayList<>();

        for (String barcode : mapBarcodeToTube.keySet()) {

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1.getBusinessKey(),
                    twoDBarcodedTubeDao.findByBarcode(barcode), BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);

            candidates.addAll(reworkEjb.findBucketCandidates(barcode));
        }

        Assert.assertEquals(candidates.size(), mapBarcodeToTube.size());

        for (ReworkEjb.BucketCandidate candidate : candidates) {
            Assert.assertTrue(mapBarcodeToTube.keySet().contains(candidate.getTubeBarcode()));
        }

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testCannotReworkDraftPdoSamples() throws Exception {
        createInitialTubes(bucketSamplesDraft, String.valueOf((new Date()).getTime()) + "tst2draft");
        for (String barcode : mapBarcodeToTube.keySet()) {
            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(draftProductOrder.getBusinessKey(),
                    twoDBarcodedTubeDao.findByBarcode(barcode), BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);
            Assert.assertEquals(reworkEjb.findBucketCandidates(barcode).size(), 0);
        }
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testHappyPathFindCandidatesByBarcodes() throws Exception {
        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst2");

        for (String barcode : mapBarcodeToTube.keySet()) {
            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1.getBusinessKey(),twoDBarcodedTubeDao.findByBarcode(barcode), BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);
        }

        Collection<ReworkEjb.BucketCandidate> candidates = reworkEjb.findBucketCandidates(new ArrayList<>(mapBarcodeToTube.keySet()));
        Assert.assertEquals(candidates.size(), mapBarcodeToTube.size());

        for (ReworkEjb.BucketCandidate candidate : candidates) {
            Assert.assertTrue(mapBarcodeToTube.keySet().contains(candidate.getTubeBarcode()));
        }
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testWorkflowSensitivityOfBucketCandidates() throws Exception {
        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst2");

        // first set the workflow to something unsupported.  nothing should end up as a candidate.
        for (ProductOrderSample pdoSample : bucketReadySamples1) {
            pdoSample.getProductOrder().getProduct().setWorkflow(Workflow.WHOLE_GENOME);
        }
        Collection<ReworkEjb.BucketCandidate> candidates = reworkEjb.findBucketCandidates(new ArrayList<>(mapBarcodeToTube.keySet()));

        Assert.assertEquals(candidates.size(), 0,"Unsupported workflows may be added incorrectly to the bucket, resulting in general ExEx panic and support burden.");
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testHappyPathFindCandidatesBySampleId() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf(new Date().getTime()) + "tst2");

        List<ReworkEjb.BucketCandidate> candidates = new ArrayList<>();
        for (Map.Entry<String, TwoDBarcodedTube> entry : mapBarcodeToTube.entrySet()) {
            String barcode = entry.getKey();
            TwoDBarcodedTube tube = entry.getValue();

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1.getBusinessKey(),
                    twoDBarcodedTubeDao.findByBarcode(barcode), BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);

            candidates
                    .addAll(reworkEjb.findBucketCandidates(tube.getMercurySamples().iterator().next().getSampleKey()));
        }

        Assert.assertEquals(candidates.size(), mapBarcodeToTube.size());

        for (ReworkEjb.BucketCandidate candidate : candidates) {
            Assert.assertTrue(mapBarcodeToTube.keySet().contains(candidate.getTubeBarcode()));
        }
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testHappyPathFindCandidatesBySampleIds() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf(new Date().getTime()) + "tst2");

        List<String> sampleIds = new ArrayList<>();
        for (Map.Entry<String, TwoDBarcodedTube> entry : mapBarcodeToTube.entrySet()) {
            String barcode = entry.getKey();
            TwoDBarcodedTube tube = entry.getValue();

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1.getBusinessKey(),
                    twoDBarcodedTubeDao.findByBarcode(barcode), BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);

            sampleIds.add(tube.getMercurySamples().iterator().next().getSampleKey());
        }

        Collection<ReworkEjb.BucketCandidate> candidates = reworkEjb.findBucketCandidates(sampleIds);
        Assert.assertEquals(candidates.size(), mapBarcodeToTube.size());

        for (ReworkEjb.BucketCandidate candidate : candidates) {
            Assert.assertTrue(mapBarcodeToTube.keySet().contains(candidate.getTubeBarcode()));
        }
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testSingleSampleTwoPDOs() throws Exception {

        Collection<ReworkEjb.BucketCandidate> candidates = reworkEjb.findBucketCandidates(genomicSample3);
        Assert.assertEquals(candidates.size(), 2);

        /*
         * Set up a collection of all PDOs that there should be candidates for. The collection will be depleted as
         * candidates for the PDOs are found to make sure that all PDOs are accounted for.
         */
        Set<String> expectedPDOs = new HashSet<>();
        expectedPDOs.add(exExProductOrder2.getBusinessKey());
        expectedPDOs.add(extraProductOrder.getBusinessKey());

        for (ReworkEjb.BucketCandidate candidate : candidates) {
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

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst3");

        List<ReworkEjb.BucketCandidate> candidates = new ArrayList<>();

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1.getBusinessKey(),
                    twoDBarcodedTubeDao.findByBarcode(currEntry.getKey()), BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);
        }

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettaLimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT4" + currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                        Workflow.AGILENT_EXOME_EXPRESS,
                        bettaLimsMessageResource,
                        reagentDesignDao, twoDBarcodedTubeDao,
                        appConfig.getUrl(), 2);

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1.getBusinessKey(),
                    twoDBarcodedTubeDao.findByBarcode(barcode), BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);

            for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {
                candidates.addAll(reworkEjb
                        .findBucketCandidates(currEntry.getValue().getSampleNames().iterator().next()));
            }
        }

        Assert.assertEquals(candidates.size(), mapBarcodeToTube.size());

        for (ReworkEjb.BucketCandidate candidate : candidates) {
            //TODO SGM/BR  Need to figure a way to get Stub search really working to validate the Barcode
            Assert.assertTrue(candidate.isValid());
            Assert.assertEquals(candidate.getProductOrderKey(), exExProductOrder1.getBusinessKey());
        }

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testNonExomePathFindCandidates() throws Exception {

        createInitialTubes(bucketSamples1, String.valueOf((new Date()).getTime()) + "tst4");

        List<ReworkEjb.BucketCandidate> candidates = new ArrayList<>();

        for (String barcode : mapBarcodeToTube.keySet()) {
            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(nonExExProductOrder.getBusinessKey(),
                    twoDBarcodedTubeDao.findByBarcode(barcode), BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);

            candidates.addAll(reworkEjb.findBucketCandidates(barcode));
        }

        Assert.assertEquals(candidates.size(),0,"Non exome workflows can make it into the bucket.  Do we support " + nonExExProductOrder.getProduct().getName() + " now?");
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testNonExomePathFindCandidatesWithAncestors() throws Exception {

        createInitialTubes(bucketSamples1, String.valueOf((new Date()).getTime()) + "tst5");

        List<ReworkEjb.BucketCandidate> candidates = new ArrayList<>();

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(nonExExProductOrder.getBusinessKey(),
                    twoDBarcodedTubeDao.findByBarcode(currEntry.getKey()), BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);
        }

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettaLimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT4" + currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                        Workflow.AGILENT_EXOME_EXPRESS,
                        bettaLimsMessageResource,
                        reagentDesignDao, twoDBarcodedTubeDao,
                        appConfig.getUrl(), 2);

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
           bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(nonExExProductOrder.getBusinessKey(),
                    twoDBarcodedTubeDao.findByBarcode(barcode), BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);

            for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {
                candidates.addAll(reworkEjb
                        .findBucketCandidates(currEntry.getValue().getSampleNames().iterator().next()));
            }
        }

        Assert.assertEquals(candidates.size(),0,"Non exome workflows can make it into the bucket.  Do we support " + nonExExProductOrder.getProduct().getName() + " now?");
    }


    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void testHappyPathWithValidation() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst6");

        List<String> validationMessages = new ArrayList<>();

        for (String barcode : mapBarcodeToTube.keySet()) {
            ReworkEjb.BucketCandidate bucketCandidate =
                    new ReworkEjb.BucketCandidate(barcode, exExProductOrder1.getBusinessKey());
            bucketCandidate.setReworkItem(true);
            String unknownReason =
                    ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
            validationMessages.addAll(reworkEjb
                    .addAndValidateCandidates(Collections.singleton(bucketCandidate),
                            unknownReason,
                            "test Rework", "jowalsh",
                            Workflow.AGILENT_EXOME_EXPRESS,
                            "Pico/Plating Bucket"));
        }

        bucketDao.clear();

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework("Pico/Plating Bucket");

        Assert.assertEquals(validationMessages.size(), 2);

        Assert.assertEquals(entries.size(), existingReworks + mapBarcodeToTube.size());

        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));

    }


    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void testAddAndValidateReworksHappyPathWithValidation() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst6");

        Collection<ReworkEjb.BucketCandidate> bucketCandidates = new ArrayList<>();
        for (String barcode : mapBarcodeToTube.keySet()) {
            ReworkEjb.BucketCandidate candidate =
                    new ReworkEjb.BucketCandidate(barcode, exExProductOrder1.getBusinessKey());
            candidate.setReworkItem(true);
            bucketCandidates.add(candidate);
        }
        String unknownReason =
                ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
        Collection<String> validationMessages = reworkEjb
                .addAndValidateCandidates(bucketCandidates,
                        unknownReason, "test Rework",
                        "jowalsh", Workflow.AGILENT_EXOME_EXPRESS, "Pico/Plating Bucket");
        Assert.assertEquals(validationMessages.size(), 2);

        bucketDao.clear();
        Collection<LabVessel> entries = reworkEjb.getVesselsForRework("Pico/Plating Bucket");
        Assert.assertEquals(entries.size(), existingReworks + mapBarcodeToTube.size());
        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));
    }


    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void testHappyPathWithValidationPreviouslyInBucket() throws Exception {

        List<String> validationMessages = new ArrayList<>();

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst7");

        for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1.getBusinessKey(),
                    twoDBarcodedTubeDao.findByBarcode(currEntry.getKey()), BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);

            ReworkEjb.BucketCandidate bucketCandidate =
                    new ReworkEjb.BucketCandidate(currEntry.getKey(), exExProductOrder1.getBusinessKey());
            bucketCandidate.setReworkItem(true);
            String unknownReason =
                    ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
            validationMessages.addAll(reworkEjb.addAndValidateCandidates(Collections.singleton(
                    bucketCandidate),
                    unknownReason, "", "scottmat",
                    Workflow.AGILENT_EXOME_EXPRESS, "Pico/Plating Bucket"));
        }

        bucketDao.clear();

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework("Pico/Plating Bucket");

        Assert.assertEquals(validationMessages.size(), 0);

        Assert.assertEquals(entries.size(), existingReworks + mapBarcodeToTube.size());

        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testHappyPathWithValidationCurrentlyInBucket() throws Exception {

        List<String> validationMessages = new ArrayList<>();

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst8");

        List<BucketEntry> bucketCleanupItems = new ArrayList<>();

        try {
            for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

                bucketDao.findByName(bucketName);
                bucketCleanupItems.add(pBucket.addEntry(exExProductOrder1.getBusinessKey(),
                        twoDBarcodedTubeDao.findByBarcode(currEntry.getKey()), BucketEntry.BucketEntryType.PDO_ENTRY));

                bucketDao.persist(pBucket);
            }
            for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {
                ReworkEjb.BucketCandidate bucketCandidate =
                        new ReworkEjb.BucketCandidate(currEntry.getKey(), exExProductOrder1.getBusinessKey());
                bucketCandidate.setReworkItem(true);
                String unknownReason =
                        ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
                reworkEjb.addAndValidateCandidates(Collections.singleton(
                        bucketCandidate),
                        unknownReason, "", "scottmat",
                        Workflow.AGILENT_EXOME_EXPRESS, "Pico/Plating Bucket");
            }
            Assert.fail("With the tube in the bucket, Calling Rework should throw an Exception");
        } catch (ValidationException e) {

        } finally {
            for (BucketEntry entry : bucketCleanupItems) {
                entry.setStatus(BucketEntry.Status.Archived);
            }
        }
    }


    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void testHappyPathWithAncestorValidation() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst9");

        List<String> validationMessages = new ArrayList<>();

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettaLimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT1" + currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                        Workflow.AGILENT_EXOME_EXPRESS,
                        bettaLimsMessageResource,
                        reagentDesignDao, twoDBarcodedTubeDao,
                        appConfig.getUrl(), 2);

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
            ReworkEjb.BucketCandidate bucketCandidate =
                    new ReworkEjb.BucketCandidate(barcode, exExProductOrder1.getBusinessKey());
            bucketCandidate.setReworkItem(true);
            String unknownReason =
                    ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
            validationMessages.addAll(reworkEjb
                    .addAndValidateCandidates(Collections.singleton(bucketCandidate),
                            unknownReason, "", "scottmat",
                            Workflow.AGILENT_EXOME_EXPRESS,
                            "Pico/Plating Bucket"));
        }

        bucketDao.clear();

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework("Pico/Plating Bucket");

        Assert.assertEquals(validationMessages.size(), 1);

        Assert.assertEquals(entries.size(), existingReworks + hybridSelectionJaxbBuilder.getNormCatchBarcodes().size());

        validateBarcodeExistence(hybridSelectionJaxbBuilder, entries);

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {

            TwoDBarcodedTube currentTube = twoDBarcodedTubeDao.findByBarcode(barcode);
            for (BucketEntry currentEntry : currentTube.getBucketEntries()) {
                currentEntry.setStatus(BucketEntry.Status.Archived);
            }
            twoDBarcodedTubeDao.persist(currentTube);
        }

    }


    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void testMixedDNAWithValidation() throws Exception {

        List<String> validationMessages = new ArrayList<>();

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime()) + "tst10");

        for (String barcode : mapBarcodeToTube.keySet()) {
            ReworkEjb.BucketCandidate bucketCandidate =
                    new ReworkEjb.BucketCandidate(barcode, exExProductOrder2.getBusinessKey());
            bucketCandidate.setReworkItem(true);
            String unknownReason =
                    ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
            validationMessages.addAll(reworkEjb
                    .addAndValidateCandidates(Collections.singleton(bucketCandidate),
                            unknownReason, "", "scottmat",
                            Workflow.AGILENT_EXOME_EXPRESS,
                            "Pico/Plating Bucket"));
        }

        bucketDao.clear();

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework("Pico/Plating Bucket");

        Assert.assertEquals(validationMessages.size(), 2);

        Assert.assertEquals(entries.size(), existingReworks + mapBarcodeToTube.size());

        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));
    }


    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void testMixedDNAWithValidationAndAncestors() throws Exception {

        List<String> validationMessages = new ArrayList<>();

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime()) + "tst11");

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettaLimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT2" + currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                        Workflow.AGILENT_EXOME_EXPRESS,
                        bettaLimsMessageResource,
                        reagentDesignDao, twoDBarcodedTubeDao,
                        appConfig.getUrl(), 2);

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
            ReworkEjb.BucketCandidate bucketCandidate =
                    new ReworkEjb.BucketCandidate(barcode, exExProductOrder2.getBusinessKey());
            bucketCandidate.setReworkItem(true);
            String unknownReason =
                    ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
            validationMessages.addAll(reworkEjb
                    .addAndValidateCandidates(Collections.singleton(bucketCandidate),
                            unknownReason, "", "scottmat", Workflow.AGILENT_EXOME_EXPRESS,
                            "Pico/Plating Bucket"));
        }

        bucketDao.clear();

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework("Pico/Plating Bucket");

        Assert.assertEquals(validationMessages.size(), 1);

        Assert.assertEquals(entries.size(), existingReworks + hybridSelectionJaxbBuilder.getNormCatchBarcodes().size());

        validateBarcodeExistence(hybridSelectionJaxbBuilder, entries);

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {

            TwoDBarcodedTube currentTube = twoDBarcodedTubeDao.findByBarcode(barcode);
            for (BucketEntry currentEntry : currentTube.getBucketEntries()) {
                currentEntry.setStatus(BucketEntry.Status.Archived);
            }
            twoDBarcodedTubeDao.persist(currentTube);
        }

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void testMixedDNAWithValidationAndAncestorsPreviouslyInBucket() throws Exception {

        List<String> validationMessages = new ArrayList<>();

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime()) + "tst12");

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder2.getBusinessKey(),
                    twoDBarcodedTubeDao.findByBarcode(currEntry.getKey()), BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);
        }

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettaLimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT4" + currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                        Workflow.AGILENT_EXOME_EXPRESS,
                        bettaLimsMessageResource,
                        reagentDesignDao, twoDBarcodedTubeDao,
                        appConfig.getUrl(), 2);

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
            ReworkEjb.BucketCandidate bucketCandidate =
                    new ReworkEjb.BucketCandidate(barcode, exExProductOrder2.getBusinessKey());
            bucketCandidate.setReworkItem(true);
            String unknownReason =
                    ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
            validationMessages.addAll(reworkEjb
                    .addAndValidateCandidates(Collections.singleton(bucketCandidate),
                            unknownReason, "", "scottmat",
                            Workflow.AGILENT_EXOME_EXPRESS, "Pico/Plating Bucket"));
        }

        bucketDao.clear();

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework("Pico/Plating Bucket");

        Assert.assertEquals(validationMessages.size(), 0);

        Assert.assertEquals(entries.size(), existingReworks + hybridSelectionJaxbBuilder.getNormCatchBarcodes().size());

        validateBarcodeExistence(hybridSelectionJaxbBuilder, entries);

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {

            TwoDBarcodedTube currentTube = twoDBarcodedTubeDao.findByBarcode(barcode);
            for (BucketEntry currentEntry : currentTube.getBucketEntries()) {
                currentEntry.setStatus(BucketEntry.Status.Archived);
            }
            twoDBarcodedTubeDao.persist(currentTube);
        }

    }


    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void testMixedDNAWithValidationAndAncestorsCurrentlyInBucket() throws Exception {

        List<String> validationMessages = new ArrayList<>();

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime()) + "tst13");

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettaLimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT3" + currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                        Workflow.AGILENT_EXOME_EXPRESS,
                        bettaLimsMessageResource,
                        reagentDesignDao, twoDBarcodedTubeDao,
                        appConfig.getUrl(), 2);

        List<BucketEntry> bucketCleanupItems = new ArrayList<>();

        for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

            bucketDao.findByName(bucketName);
            bucketCleanupItems.add(pBucket.addEntry(exExProductOrder2.getBusinessKey(),
                    twoDBarcodedTubeDao.findByBarcode(currEntry.getKey()), BucketEntry.BucketEntryType.PDO_ENTRY));
            bucketDao.persist(pBucket);
        }

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
            ReworkEjb.BucketCandidate bucketCandidate =
                    new ReworkEjb.BucketCandidate(barcode, exExProductOrder2.getBusinessKey());
            bucketCandidate.setReworkItem(true);
            String unknownReason =
                    ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
            validationMessages.addAll(reworkEjb
                    .addAndValidateCandidates(Collections.singleton(bucketCandidate),
                            unknownReason, "", "scottmat",
                            Workflow.AGILENT_EXOME_EXPRESS,
                            "Pico/Plating Bucket"));
        }

        bucketDao.clear();

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework("Pico/Plating Bucket");

        Assert.assertEquals(validationMessages.size(), 0);

        Assert.assertEquals(entries.size(), existingReworks + hybridSelectionJaxbBuilder.getNormCatchBarcodes().size());

        validateBarcodeExistence(hybridSelectionJaxbBuilder, entries);

        for (BucketEntry entry : bucketCleanupItems) {
            entry.setStatus(BucketEntry.Status.Archived);
        }

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {

            TwoDBarcodedTube currentTube = twoDBarcodedTubeDao.findByBarcode(barcode);
            for (BucketEntry currentEntry : currentTube.getBucketEntries()) {
                currentEntry.setStatus(BucketEntry.Status.Archived);
            }
            twoDBarcodedTubeDao.persist(currentTube);
        }

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testFindCandidatesMultiplePdosNoBuckets() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst2");

        List<ReworkEjb.BucketCandidate> candidates = new ArrayList<>();

        List<ProductOrderSample> bucketReadySamplesDupe = new ArrayList<>(2);
        bucketReadySamplesDupe.add(new ProductOrderSample(genomicSample1));
        bucketReadySamplesDupe.add(new ProductOrderSample(genomicSample2));


        ProductOrder duplicatePO =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                        "Rework Integration TestOrder DuplicatePDORework" + currDate.getTime(),
                        bucketReadySamplesDupe, "GSP-123", exExProduct, researchProject);
        duplicatePO.setProduct(exExProduct);
        duplicatePO.prepareToSave(bspUserList.getByUsername("scottmat"));
        String dupePdo1JiraKey = "PDO-SGM-RWINT_Dupe_tst" + currDate.getTime() + 1;
        duplicatePO.setJiraTicketKey(dupePdo1JiraKey);
        duplicatePO.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrderDao.persist(duplicatePO);

        for (Map.Entry<String, TwoDBarcodedTube> tubes : mapBarcodeToTube.entrySet()) {

            candidates.addAll(reworkEjb.findBucketCandidates(tubes.getValue().getSampleNames().iterator().next()));
        }

        Assert.assertEquals(candidates.size(), mapBarcodeToTube.size() * 2);

        for (ReworkEjb.BucketCandidate candidate : candidates) {
            //TODO SGM/BR  Need to figure a way to get Stub search really working to validate the Barcode
            Assert.assertTrue(candidate.isValid());
//            Assert.assertTrue(mapBarcodeToTube.keySet().contains(candidate.getTubeBarcode()),"Did not find barcode " + candidate.getTubeBarcode() + "In the map of created tubes");
        }
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testFindCandidatesMultiplePdosWithBuckets() throws Exception {

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime()) + "tst2");

        List<ReworkEjb.BucketCandidate> candidates = new ArrayList<>();

        List<ProductOrderSample> bucketReadySamplesDupe = new ArrayList<>(2);
        bucketReadySamplesDupe.add(new ProductOrderSample(genomicSample3));
        bucketReadySamplesDupe.add(new ProductOrderSample(somaticSample3));

        ProductOrder duplicatePO =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                        "Rework Integration TestOrder DuplicatePDORework" + currDate.getTime() + "5",
                        bucketReadySamplesDupe, "GSP-123", exExProduct, researchProject);
        duplicatePO.setProduct(exExProduct);
        duplicatePO.prepareToSave(bspUserList.getByUsername("scottmat"));
        String dupePdo1JiraKey = "PDO-SGM-RWINT_Dupe_tst" + currDate.getTime() + 1;
        duplicatePO.setJiraTicketKey(dupePdo1JiraKey);
        duplicatePO.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrderDao.persist(duplicatePO);

        for (Map.Entry<String, TwoDBarcodedTube> tubes : mapBarcodeToTube.entrySet()) {

            bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(duplicatePO.getBusinessKey(),
                    twoDBarcodedTubeDao.findByBarcode(tubes.getKey()), BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.persist(pBucket);

            candidates.addAll(reworkEjb.findBucketCandidates(tubes.getValue().getSampleNames().iterator().next()));
        }

        // genomicSample3 is in 3 PDOs and somaticSample3 is in 2 PDOs
        Assert.assertEquals(candidates.size(), 5);

        for (ReworkEjb.BucketCandidate candidate : candidates) {

            //TODO SGM/BR  Need to figure a way to get Stub search really working to validate the Barcode

            Assert.assertTrue(candidate.isValid());
        }
    }

    private void createInitialTubes(@Nonnull List<ProductOrderSample> pos,
                                    @Nonnull String barcodePrefix) {

        for (ProductOrderSample currSamp : pos) {
            String twoDBarcode =
                    bspSampleDataFetcher.fetchSingleSampleFromBSP(currSamp.getBspSampleName()).getContainerId();

            TwoDBarcodedTube aliquot = new TwoDBarcodedTube(twoDBarcode);
            aliquot.addSample(new MercurySample(currSamp.getBspSampleName(),
                    bspSampleDataFetcher.fetchSingleSampleFromBSP(currSamp.getBspSampleName())));
            mapBarcodeToTube.put(twoDBarcode, aliquot);
            labVesselDao.persist(aliquot);
        }

//        LabBatch testBatch =
//                labBatchEJB.createLabBatch(new HashSet<LabVessel>(mapBarcodeToTube.values()), "scottmat", "LCSET-123"+barcodePrefix,
//                        LabBatch.LabBatchType.WORKFLOW, CreateFields.IssueType.EXOME_EXPRESS);

    }

    private void validateBarcodeExistence(HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder,
                                          Collection<LabVessel> entries) {
        List<String> reworkEntryBarcodes = new ArrayList<>(entries.size());

        for (LabVessel reworkEntry : entries) {
            reworkEntryBarcodes.add(reworkEntry.getLabel());
        }

        for (String normCatchBarcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {

            Assert.assertTrue(reworkEntryBarcodes.contains(normCatchBarcode),
                    "Norm catch barcode with vessel of " + normCatchBarcode
                    + " Not found in the list of rework entry barcodes");
        }
    }

    public void testFindPotentialPdos() throws Exception {
        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime()) + "tst13");

        List<Long> bucketEntryIds = new ArrayList<>();

        for (Map.Entry<String, TwoDBarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {
            List<BucketEntry> bucketEntries = new ArrayList<>();
            bucketEntries.add(pBucket.addEntry(exExProductOrder1.getBusinessKey(),
                    twoDBarcodedTubeDao.findByBarcode(currEntry.getKey()),
                    BucketEntry.BucketEntryType.PDO_ENTRY));
            bucketEntries.add(pBucket.addEntry(exExProductOrder2.getBusinessKey(),
                    twoDBarcodedTubeDao.findByBarcode(currEntry.getKey()),
                    BucketEntry.BucketEntryType.PDO_ENTRY));
            bucketDao.persist(pBucket);
            for (BucketEntry bucketEntry : bucketEntries) {
                bucketEntryIds.add(bucketEntry.getBucketEntryId());
            }

        }


        Set<String> bucketCandidatePdos =
                reworkEjb.findBucketCandidatePdos(bucketEntryIds);
        Assert.assertFalse(bucketCandidatePdos.isEmpty());
        Assert.assertEquals(bucketCandidatePdos.size(), 2);
    }

    private void resetExExProductWorkflow() {
        if (exExProductOrder2 != null) {
            exExProductOrder2.getProduct().setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        }
        if (exExProductOrder1 != null) {
            exExProductOrder1.getProduct().setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        }
    }

}
