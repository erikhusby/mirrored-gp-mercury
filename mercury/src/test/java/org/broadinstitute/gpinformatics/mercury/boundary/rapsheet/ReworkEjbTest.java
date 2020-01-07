package org.broadinstitute.gpinformatics.mercury.boundary.rapsheet;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem_;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcherStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResourceTest;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionJaxbBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.enterprise.context.RequestScoped;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

/**
 *  This test is singleThreaded because subsequent test methods are called before the @AfterMethod of the previous test method call is complete <br/>
 *  This wouldn't be a problem if lifecycle methods didn't change the state of instance variables.
 *  Subsequent methods are using variables which are getting stepped on by previous @AfterMethod calls
 */
@Test(groups = TestGroups.ALTERNATIVES, singleThreaded = true)
@RequestScoped
public class ReworkEjbTest extends Arquillian {

    public ReworkEjbTest(){}

    public static final String SM_SGM_Test_Somatic_1_PATIENT_ID = "PT-1TS1";
    public static final String SM_SGM_Test_Somatic_1_STOCK_SAMP = "SM-SGM_Test_Somatic1";
    public static final String SM_SGM_Test_Somatic_1_COLLAB_SAMP_ID = "CollaboratorSampleX";
    public static final String SM_SGM_Test_Somatic_1_COLL = "Hungarian Goulash";
    public static final String SM_SGM_Test_Somatic_1_VOLUME = "1.3";
    public static final String SM_SGM_Test_Somatic_1_CONC = "0.293";
    public static final String SM_SGM_Test_Somatic_1_COLLAB_PID = "CHTN_SEW";
    public static final String SM_SGM_Test_Somatic_1_DNA = "3.765242738037109374";
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
    public static final String SM_SGM_Test_Somatic_2_DISEASE = "Carcinoid Tumor";
    public static final String SM_SGM_Test_Genomic_1_PATIENT_ID = "PT-1TS1";
    public static final String SM_SGM_Test_Genomic_1_STOCK_SAMP = "SM-SGM_Test_Genomic1";
    public static final String SM_SGM_Test_Genomic_1_COLLAB_SAMP_ID = "CollaboratorSampleX";
    public static final String SM_SGM_Test_Genomic_1_COLL = "Hungarian Goulash";
    public static final String SM_SGM_Test_Genomic_1_VOLUME = "1.3";
    public static final String SM_SGM_Test_Genomic_1_CONC = "0.293";
    public static final String SM_SGM_Test_Genomic_1_COLLAB_PID = "CHTN_SEW";
    public static final String SM_SGM_Test_Genomic_1_DNA = "3.765242738037109374";
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
    public static final String SM_SGM_Test_Genomic_2_DISEASE = "Carcinoid Tumor";

    @Inject
    private ReworkEjb reworkEjb;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private PriceItemDao priceItemDao;

    @Inject
    private ProductFamilyDao productFamilyDao;

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
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private AppConfig appConfig;

    @Inject
    private BSPSampleDataFetcher bspSampleDataFetcher;

    private Map<String, BarcodedTube> mapBarcodeToTube = new HashMap<>();
    private ProductOrder exExProductOrder1;
    private ProductOrder exExProductOrder2;
    private ProductOrder draftProductOrder;
    private ProductOrder nonExExProductOrder;
    private ProductOrder extraProductOrder;
    private List<ProductOrderSample> bucketReadySamples1;
    private List<ProductOrderSample> bucketReadySamples2;
    private List<ProductOrderSample> bucketSamples1;
    private List<ProductOrderSample> bucketSamplesDraft;
    private ProductOrderSample extraProductOrderSample;
    private Bucket pBucket;
    private String bucketName;

    private int existingReworks;
    private PriceItem priceItem;
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

    /**
     * Local map of SampleData to use when creating MercurySamples for ProductOrderSamples.
     */
    private Map<String, BspSampleData> sampleDataMap;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder
                .buildMercuryWarWithAlternatives(DEV, JiraServiceStub.class, BSPSampleDataFetcherStub.class);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        if (reworkEjb == null) {
            return;
        }

        setupProducts();

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

        sampleDataMap = new HashMap<>();

        sampleDataMap.put(somaticSample2,
                new BspSampleData(new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
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
                    put(BSPSampleSearchColumn.SAMPLE_TYPE, BspSampleData.NORMAL_IND);
                    put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_SGM_Test_Somatic_2_DISEASE);
                    put(BSPSampleSearchColumn.GENDER, BspSampleData.FEMALE_IND);
                    put(BSPSampleSearchColumn.STOCK_TYPE, BspSampleData.ACTIVE_IND);
                    put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Somatic_2_CONTAINER_ID);
                    put(BSPSampleSearchColumn.SAMPLE_ID, somaticSample2);
                }}));

        sampleDataMap.put(somaticSample1,
                new BspSampleData(new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
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
                    put(BSPSampleSearchColumn.SAMPLE_TYPE, BspSampleData.NORMAL_IND);
                    put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_SGM_Test_Somatic_1_DISEASE);
                    put(BSPSampleSearchColumn.GENDER, BspSampleData.FEMALE_IND);
                    put(BSPSampleSearchColumn.STOCK_TYPE, BspSampleData.ACTIVE_IND);
                    put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Somatic_1_CONTAINER_ID);
                    put(BSPSampleSearchColumn.SAMPLE_ID, somaticSample1);
                }}));

        sampleDataMap.put(somaticSample3,
                new BspSampleData(new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
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
                    put(BSPSampleSearchColumn.SAMPLE_TYPE, BspSampleData.NORMAL_IND);
                    put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_SGM_Test_Somatic_1_DISEASE);
                    put(BSPSampleSearchColumn.GENDER, BspSampleData.FEMALE_IND);
                    put(BSPSampleSearchColumn.STOCK_TYPE, BspSampleData.ACTIVE_IND);
                    put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Somatic_3_CONTAINER_ID);
                    put(BSPSampleSearchColumn.SAMPLE_ID, somaticSample3);
                }}));

        sampleDataMap.put(genomicSample2,
                new BspSampleData(new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
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
                    put(BSPSampleSearchColumn.SAMPLE_TYPE, BspSampleData.NORMAL_IND);
                    put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_SGM_Test_Genomic_2_DISEASE);
                    put(BSPSampleSearchColumn.GENDER, BspSampleData.FEMALE_IND);
                    put(BSPSampleSearchColumn.STOCK_TYPE, BspSampleData.ACTIVE_IND);
                    put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Genomic_2_CONTAINER_ID);
                    put(BSPSampleSearchColumn.SAMPLE_ID, genomicSample2);
                }}));

        sampleDataMap.put(genomicSample1,
                new BspSampleData(new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
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
                    put(BSPSampleSearchColumn.SAMPLE_TYPE, BspSampleData.NORMAL_IND);
                    put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_SGM_Test_Genomic_1_DISEASE);
                    put(BSPSampleSearchColumn.GENDER, BspSampleData.FEMALE_IND);
                    put(BSPSampleSearchColumn.STOCK_TYPE, BspSampleData.ACTIVE_IND);
                    put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Genomic_1_CONTAINER_ID);
                    put(BSPSampleSearchColumn.SAMPLE_ID, genomicSample1);
                }}));

        BspSampleData genomicSample3SampleData =
                new BspSampleData(new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
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
                    put(BSPSampleSearchColumn.SAMPLE_TYPE, BspSampleData.NORMAL_IND);
                    put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_SGM_Test_Genomic_1_DISEASE);
                    put(BSPSampleSearchColumn.GENDER, BspSampleData.FEMALE_IND);
                    put(BSPSampleSearchColumn.STOCK_TYPE, BspSampleData.ACTIVE_IND);
                    put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Genomic_3_CONTAINER_ID);
                    put(BSPSampleSearchColumn.SAMPLE_ID, genomicSample3);
                }});
        sampleDataMap.put(genomicSample3, genomicSample3SampleData);

        // This test asks for a BSPSampleDataFetcherStub in its @Deployment, so this cast is safe.
        ((BSPSampleDataFetcherStub) bspSampleDataFetcher)
                .stubFetchSampleData(genomicSample3, genomicSample3SampleData, SM_SGM_Test_Genomic_3_CONTAINER_ID);

        sampleDataMap.put(genomicSampleDraft,
                new BspSampleData(new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
                    put(BSPSampleSearchColumn.PARTICIPANT_ID, SM_SGM_Test_Genomic_1_PATIENT_ID);
                    put(BSPSampleSearchColumn.ROOT_SAMPLE, BSPSampleSearchServiceStub.ROOT);
                    put(BSPSampleSearchColumn.STOCK_SAMPLE, SM_SGM_Test_Genomic_3_STOCK_SAMP);
                    put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, SM_SGM_Test_Genomic_1_COLLAB_SAMP_ID);
                    put(BSPSampleSearchColumn.COLLECTION, SM_SGM_Test_Genomic_1_COLL);
                    put(BSPSampleSearchColumn.VOLUME, SM_SGM_Test_Genomic_1_VOLUME);
                    put(BSPSampleSearchColumn.CONCENTRATION, SM_SGM_Test_Genomic_1_CONC);
                    put(BSPSampleSearchColumn.SPECIES, BSPSampleSearchServiceStub.CANINE_SPECIES);
                    put(BSPSampleSearchColumn.LSID, BSPSampleSearchServiceStub.LSID_PREFIX + genomicSampleDraft);
                    put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, SM_SGM_Test_Genomic_1_COLLAB_PID);
                    put(BSPSampleSearchColumn.MATERIAL_TYPE, BSPSampleSearchServiceStub.GENOMIC_MAT_TYPE);
                    put(BSPSampleSearchColumn.TOTAL_DNA, SM_SGM_Test_Genomic_1_DNA);
                    put(BSPSampleSearchColumn.SAMPLE_TYPE, BspSampleData.NORMAL_IND);
                    put(BSPSampleSearchColumn.PRIMARY_DISEASE, SM_SGM_Test_Genomic_1_DISEASE);
                    put(BSPSampleSearchColumn.GENDER, BspSampleData.FEMALE_IND);
                    put(BSPSampleSearchColumn.STOCK_TYPE, BspSampleData.ACTIVE_IND);
                    put(BSPSampleSearchColumn.CONTAINER_ID, SM_SGM_Test_Genomic_3_CONTAINER_ID);
                    put(BSPSampleSearchColumn.SAMPLE_ID, genomicSampleDraft);
                }}));


        String rpJiraTicketKey = "RP-SGM-Rework_tst1" + currDate.getTime() + "RP";
        researchProject = new ResearchProject(bspUserList.getByUsername("scottmat").getUserId(),
                                              "Rework Integration Test RP " + currDate.getTime() + "RP",
                                              "Rework Integration Test RP", false,
                                              ResearchProject.RegulatoryDesignation.RESEARCH_ONLY);
        researchProject.setJiraTicketKey(rpJiraTicketKey);
        researchProjectDao.persist(researchProject);

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

        extraProductOrderSample = new ProductOrderSample(genomicSample3);
        extraProductOrder = new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                                             "Rework Integration TestOrder 4" + currDate.getTime(),
                                             Collections.singletonList(extraProductOrderSample),
                                             "GSP-123", exExProduct,
                                             researchProject);
        extraProductOrder.prepareToSave(bspUserList.getByUsername("scottmat"));
        String pdo4JiraKey = "PDO-SGM-RWINT_tst" + currDate.getTime() + 4;
        extraProductOrder.setJiraTicketKey(pdo4JiraKey);
        extraProductOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrderDao.persist(extraProductOrder);

        draftProductOrder =
                new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                                 "Rework Integration TestOrder draft" + currDate.getTime(), bucketSamplesDraft,
                                 "GSP-123",
                                 exExProduct, researchProject);
        draftProductOrder.setProduct(exExProduct);
        draftProductOrder.prepareToSave(bspUserList.getByUsername("scottmat"));
        String draftPdoJiraKey = "PDO-SGM-RWINT_tst" + currDate.getTime() + "draft";
        draftProductOrder.setJiraTicketKey(draftPdoJiraKey);
        draftProductOrder.setOrderStatus(ProductOrder.OrderStatus.Draft);
        productOrderDao.persist(draftProductOrder);


        WorkflowBucketDef bucketDef = findBucketDef(Workflow.AGILENT_EXOME_EXPRESS, LabEventType.PICO_PLATING_BUCKET);

        bucketName = bucketDef.getName();

        pBucket = bucketDao.findByName(bucketName);

        if (pBucket == null) {

            pBucket = new Bucket(bucketName);
            bucketDao.persist(pBucket);
        }

        existingReworks = reworkEjb.getVesselsForRework("Pico/Plating Bucket").size();

        resetExExProductWorkflow();
    }

    /**
     * findBucketDef will utilize the WorkflowConfig to return an instance of a {@link org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef} based
     * on a given workflow definition and and step labEventType
     */
    public static WorkflowBucketDef findBucketDef(@Nonnull String workflow, @Nonnull LabEventType stepDef) {

        WorkflowConfig workflowConfig = (new WorkflowLoader()).getWorkflowConfig();
        assert (workflowConfig != null && workflowConfig.getProductWorkflowDefs() != null &&
                !workflowConfig.getProductWorkflowDefs().isEmpty());
        ProductWorkflowDef productWorkflowDef = workflowConfig.getWorkflow(workflow);
        ProductWorkflowDefVersion versionResult = productWorkflowDef.getEffectiveVersion();

        ProductWorkflowDefVersion.LabEventNode labEventNode =
                versionResult.findStepByEventType(stepDef.getName());

        WorkflowBucketDef bucketDef = null;
        if (labEventNode != null) {
            bucketDef = (WorkflowBucketDef) labEventNode.getStepDef();
        }
        return bucketDef;
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (reworkEjb == null) {
            return;
        }

        ((BSPSampleDataFetcherStub) bspSampleDataFetcher)
                .clearStubFetchSampleData();

        exExProductOrder1 = productOrderDao.findByBusinessKey(exExProductOrder1.getBusinessKey());
        exExProductOrder1.setOrderStatus(ProductOrder.OrderStatus.Completed);
        exExProductOrder2 = productOrderDao.findByBusinessKey(exExProductOrder2.getBusinessKey());
        exExProductOrder2.setOrderStatus(ProductOrder.OrderStatus.Completed);
        nonExExProductOrder = productOrderDao.findByBusinessKey(nonExExProductOrder.getBusinessKey());
        nonExExProductOrder.setOrderStatus(ProductOrder.OrderStatus.Completed);
        extraProductOrder = productOrderDao.findByBusinessKey(extraProductOrder.getBusinessKey());
        extraProductOrder.setOrderStatus(ProductOrder.OrderStatus.Completed);
        resetExExProductWorkflow();

        for (BarcodedTube vessel : mapBarcodeToTube.values()) {
            BarcodedTube tempVessel = barcodedTubeDao.findByBarcode(vessel.getLabel());
            for (BucketEntry bucketEntry : tempVessel.getBucketEntries()) {
                bucketEntry.setStatus(BucketEntry.Status.Archived);
            }
        }
        productOrderDao.flush();
        productOrderDao.clear();
    }

    /**
     * Creates (or re-uses) two products that are specific to this test.
     * On product has a mercury-tracked workflow; the other does not.
     */
    private void setupProducts() {
        String productFamilyName = ProductFamily.ProductFamilyInfo.EXOME.getFamilyName();
        ProductFamily productFamily = productFamilyDao.find(productFamilyName);

        if (productFamily == null) {
            productFamily = new ProductFamily(productFamilyName);
            productFamilyDao.persist(productFamily);
        }

        String quoteServerId = "ReworkEjbTest";
        priceItem = priceItemDao.findSingle(PriceItem.class, PriceItem_.quoteServerId, quoteServerId);

        if (priceItem == null) {
            priceItem = new PriceItem(quoteServerId, quoteServerId, "Delicious Foods", "Tacos");
            priceItemDao.persist(priceItem);
        }

        String exExProductPartNumber = "ReworkEjbPartNumber1";
        exExProduct = productDao.findByPartNumber(exExProductPartNumber);

        if (exExProduct == null) {
            exExProduct = createProduct(true, productFamily, exExProductPartNumber);
            productDao.persist(exExProduct);
        }

        String nonExExProductPartNumber = "ReworkEjbPartNumber2";
        nonExExProduct = productDao.findByPartNumber(nonExExProductPartNumber);

        if (nonExExProduct == null) {
            nonExExProduct = createProduct(false, productFamily, nonExExProductPartNumber);
            productDao.persist(nonExExProduct);
        }
    }

    private Product createProduct(boolean isExomeExpress, ProductFamily productFamily, String name) {
        String workflow = isExomeExpress ? Workflow.AGILENT_EXOME_EXPRESS : Workflow.WHOLE_GENOME;
        Product product = new Product(name,
                                      productFamily,
                                      "Description",
                                      name,
                                      new Date(),
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      true,
                                      workflow,
                                      false,
                                      null);

        product.setPrimaryPriceItem(priceItem);
        return product;
    }

    @Test
    public void testNonRework() throws Exception {
        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst1");

        Collection<String> bucketedTubeLabels = new ArrayList<>();
        for (BarcodedTube vessel : mapBarcodeToTube.values()) {
            ReworkEjb.BucketCandidate bucketCandidate =
                    new ReworkEjb.BucketCandidate(vessel.getLabel(), exExProductOrder1);
            reworkEjb.addAndValidateCandidates(Collections.singleton(bucketCandidate), "", "test non-rework", "breilly",
                    "Pico/Plating Bucket");
            bucketedTubeLabels.add(vessel.getLabel());
        }

        bucketDao.clear();

        Bucket bucket = bucketDao.findByName("Pico/Plating Bucket");
        Collection<String> bucketEntryVesselLabels = new ArrayList<>();
        for (BucketEntry bucketEntry : bucket.getBucketEntries()) {
            bucketEntryVesselLabels.add(bucketEntry.getLabVessel().getLabel());
        }
        assertThat(bucketEntryVesselLabels,
                hasItems(bucketedTubeLabels.toArray(new String[bucketedTubeLabels.size()])));
    }

    @Test
    public void testHappyPath() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst1");

        for (Map.Entry<String, BarcodedTube> entry : mapBarcodeToTube.entrySet()) {
            BarcodedTube vessel = entry.getValue();
            ReworkEjb.BucketCandidate bucketCandidate =
                    new ReworkEjb.BucketCandidate(vessel.getLabel(), exExProductOrder1);
            bucketCandidate.setReworkItem(true);
            String unknownReason =
                    ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
            reworkEjb.addAndValidateCandidates(Collections.singleton(bucketCandidate), unknownReason,
                                               "test Rework", "scottmat", "Pico/Plating Bucket");
        }

        bucketDao.clear();

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework("Pico/Plating Bucket");

        Assert.assertEquals(entries.size(), existingReworks + mapBarcodeToTube.size());

        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));

    }

    @Test
    public void testHappyPathFindCandidatesByBarcode() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst2");

        List<ReworkEjb.BucketCandidate> candidates = new ArrayList<>();

        for (String barcode : mapBarcodeToTube.keySet()) {

            pBucket = bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1, barcodedTubeDao.findByBarcode(barcode),
                                                    BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.flush();

            candidates.addAll(reworkEjb.findBucketCandidates(barcode));
        }

        Assert.assertEquals(candidates.size(), mapBarcodeToTube.size());

        for (ReworkEjb.BucketCandidate candidate : candidates) {
            Assert.assertTrue(mapBarcodeToTube.keySet().contains(candidate.getTubeBarcode()));
        }

    }

    @Test
    public void testCannotReworkDraftPdoSamples() throws Exception {
        createInitialTubes(bucketSamplesDraft, String.valueOf((new Date()).getTime()) + "tst2draft");
        for (String barcode : mapBarcodeToTube.keySet()) {
            pBucket = bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(draftProductOrder, barcodedTubeDao.findByBarcode(barcode),
                                                    BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.flush();
            Assert.assertEquals(reworkEjb.findBucketCandidates(barcode).size(), 0);
        }
    }

    @Test
    public void testHappyPathFindCandidatesByBarcodes() throws Exception {
        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst2");

        for (String barcode : mapBarcodeToTube.keySet()) {
            pBucket = bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1, barcodedTubeDao.findByBarcode(barcode),
                                                    BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.flush();
        }

        Collection<ReworkEjb.BucketCandidate> candidates = reworkEjb.findBucketCandidates(new ArrayList<>(
                mapBarcodeToTube.keySet()));
        Assert.assertEquals(candidates.size(), mapBarcodeToTube.size());

        for (ReworkEjb.BucketCandidate candidate : candidates) {
            Assert.assertTrue(mapBarcodeToTube.keySet().contains(candidate.getTubeBarcode()));
        }
    }

    @Test
    public void testWorkflowSensitivityOfBucketCandidates() throws Exception {
        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst2");

        // first set the workflow to something unsupported.  nothing should end up as a candidate.
        for (ProductOrderSample pdoSample : bucketReadySamples1) {
            pdoSample.getProductOrder().getProduct().setWorkflowName(Workflow.WHOLE_GENOME);
        }
        Collection<ReworkEjb.BucketCandidate> candidates = reworkEjb.findBucketCandidates(new ArrayList<>(
                mapBarcodeToTube.keySet()));

        Assert.assertEquals(candidates.size(), 0,
                            "Unsupported workflows may be added incorrectly to the bucket, resulting in general ExEx panic and support burden.");
    }

    @Test
    public void testHappyPathFindCandidatesBySampleId() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf(new Date().getTime()) + "tst2");

        List<ReworkEjb.BucketCandidate> candidates = new ArrayList<>();
        for (Map.Entry<String, BarcodedTube> entry : mapBarcodeToTube.entrySet()) {
            String barcode = entry.getKey();
            BarcodedTube tube = entry.getValue();

            pBucket = bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1, barcodedTubeDao.findByBarcode(barcode),
                                                    BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.flush();

            candidates
                    .addAll(reworkEjb.findBucketCandidates(tube.getMercurySamples().iterator().next().getSampleKey()));
        }

        Assert.assertEquals(candidates.size(), mapBarcodeToTube.size());

        for (ReworkEjb.BucketCandidate candidate : candidates) {
            Assert.assertTrue(mapBarcodeToTube.keySet().contains(candidate.getTubeBarcode()));
        }
    }

    @Test
    public void testHappyPathFindCandidatesBySampleIds() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf(new Date().getTime()) + "tst2");

        List<String> sampleIds = new ArrayList<>();
        for (Map.Entry<String, BarcodedTube> entry : mapBarcodeToTube.entrySet()) {
            String barcode = entry.getKey();
            BarcodedTube tube = entry.getValue();

            pBucket = bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1, barcodedTubeDao.findByBarcode(barcode),
                                                    BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.flush();

            sampleIds.add(tube.getMercurySamples().iterator().next().getSampleKey());
        }

        Collection<ReworkEjb.BucketCandidate> candidates = reworkEjb.findBucketCandidates(sampleIds);
        Assert.assertEquals(candidates.size(), mapBarcodeToTube.size());

        for (ReworkEjb.BucketCandidate candidate : candidates) {
            Assert.assertTrue(mapBarcodeToTube.keySet().contains(candidate.getTubeBarcode()));
        }
    }

    @Test
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
            Assert.assertTrue(expectedPDOs.contains(candidate.getProductOrder().getBusinessKey()));
            expectedPDOs.remove(candidate.getProductOrder().getBusinessKey());
        }

        // Make sure all that candidates were found for all expected PDOs.
        Assert.assertTrue(expectedPDOs.isEmpty());
    }

    @Test
    public void testHappyPathFindCandidatesWithAncestors() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst3");

        List<ReworkEjb.BucketCandidate> candidates = new ArrayList<>();

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        pBucket = bucketDao.findByName(bucketName);
        for (Map.Entry<String, BarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1, barcodedTubeDao.findByBarcode(
                    currEntry.getKey()), BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.flush();
        }

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettaLimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT4" + currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                                       Workflow.AGILENT_EXOME_EXPRESS,
                                       bettaLimsMessageResource,
                                       reagentDesignDao, barcodedTubeDao,
                                       appConfig.getUrl(), 2);

        exExProductOrder1 = refreshPdo(exExProductOrder1.getBusinessKey());
        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {

            pBucket = bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1, barcodedTubeDao.findByBarcode(barcode),
                                                    BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.flush();

            for (Map.Entry<String, BarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {
                candidates.addAll(reworkEjb
                                          .findBucketCandidates(
                                                  currEntry.getValue().getSampleNames().iterator().next()));
            }
        }

        Assert.assertEquals(candidates.size(), mapBarcodeToTube.size());

        for (ReworkEjb.BucketCandidate candidate : candidates) {
            //TODO Need to figure a way to get Stub search really working to validate the Barcode
            Assert.assertTrue(candidate.isValid());
            Assert.assertEquals(candidate.getProductOrder().getBusinessKey(), exExProductOrder1.getBusinessKey());
        }

    }

    @Test
    public void testNonExomePathFindCandidates() throws Exception {

        createInitialTubes(bucketSamples1, String.valueOf((new Date()).getTime()) + "tst4");

        List<ReworkEjb.BucketCandidate> candidates = new ArrayList<>();

        for (String barcode : mapBarcodeToTube.keySet()) {
            pBucket = bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(nonExExProductOrder, barcodedTubeDao.findByBarcode(barcode),
                                                    BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.flush();

            candidates.addAll(reworkEjb.findBucketCandidates(barcode));
        }

        Assert.assertEquals(candidates.size(), 0,
                            "Non exome workflows can make it into the bucket.  Do we support " + nonExExProductOrder
                                    .getProduct().getName() + " now?");
    }

    @Test
    public void testNonExomePathFindCandidatesWithAncestors() throws Exception {

        createInitialTubes(bucketSamples1, String.valueOf((new Date()).getTime()) + "tst5");

        List<ReworkEjb.BucketCandidate> candidates = new ArrayList<>();

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        for (Map.Entry<String, BarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

            pBucket = bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(nonExExProductOrder, barcodedTubeDao.findByBarcode(
                    currEntry.getKey()), BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.flush();
        }

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettaLimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT4" + currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                                       Workflow.AGILENT_EXOME_EXPRESS,
                                       bettaLimsMessageResource,
                                       reagentDesignDao, barcodedTubeDao,
                                       appConfig.getUrl(), 2);

        nonExExProductOrder = refreshPdo(nonExExProductOrder.getBusinessKey());
        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
            pBucket = bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(nonExExProductOrder, barcodedTubeDao.findByBarcode(barcode),
                                                    BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.flush();

            for (Map.Entry<String, BarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {
                candidates.addAll(reworkEjb
                                          .findBucketCandidates(
                                                  currEntry.getValue().getSampleNames().iterator().next()));
            }
        }

        Assert.assertEquals(candidates.size(), 0,
                            "Non exome workflows can make it into the bucket.  Do we support " + nonExExProductOrder
                                    .getProduct().getName() + " now?");
    }


    @Test
    public void testHappyPathWithValidation() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst6");

        List<String> validationMessages = new ArrayList<>();

        for (String barcode : mapBarcodeToTube.keySet()) {
            ReworkEjb.BucketCandidate bucketCandidate =
                    new ReworkEjb.BucketCandidate(barcode, exExProductOrder1);
            bucketCandidate.setReworkItem(true);
            String unknownReason =
                    ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
            validationMessages.addAll(reworkEjb.addAndValidateCandidates(Collections.singleton(bucketCandidate),
                                                                         unknownReason, "test Rework", "jowalsh",
                                                                         "Pico/Plating Bucket"));
        }

        bucketDao.clear();

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework("Pico/Plating Bucket");

        Assert.assertEquals(validationMessages.size(), 0);

        Assert.assertEquals(entries.size(), existingReworks + mapBarcodeToTube.size());

        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));

    }


    @Test
    public void testAddAndValidateReworksHappyPathWithValidation() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst6");

        Collection<ReworkEjb.BucketCandidate> bucketCandidates = new ArrayList<>();
        for (String barcode : mapBarcodeToTube.keySet()) {
            ReworkEjb.BucketCandidate candidate =
                    new ReworkEjb.BucketCandidate(barcode, exExProductOrder1);
            candidate.setReworkItem(true);
            bucketCandidates.add(candidate);
            Thread.sleep(2L); // Event creation fails on duplicate location, date, and disambiguator
        }
        String unknownReason = ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
        Collection<String> validationMessages = reworkEjb.addAndValidateCandidates(bucketCandidates, unknownReason,
                                                                                   "test Rework", "jowalsh",
                                                                                   "Pico/Plating Bucket");
        Assert.assertEquals(validationMessages.size(), 0);

        bucketDao.clear();
        Collection<LabVessel> entries = reworkEjb.getVesselsForRework("Pico/Plating Bucket");
        Assert.assertEquals(entries.size(), existingReworks + mapBarcodeToTube.size());
        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));
    }


    @Test
    public void testHappyPathWithValidationPreviouslyInBucket() throws Exception {

        List<String> validationMessages = new ArrayList<>();

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst7");

        for (Map.Entry<String, BarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

            pBucket = bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder1, barcodedTubeDao.findByBarcode(
                    currEntry.getKey()), BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.flush();

            ReworkEjb.BucketCandidate bucketCandidate =
                    new ReworkEjb.BucketCandidate(currEntry.getKey(), exExProductOrder1);
            bucketCandidate.setReworkItem(true);
            String unknownReason =
                    ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
            validationMessages.addAll(reworkEjb.addAndValidateCandidates(Collections.singleton(bucketCandidate),
                                                                         unknownReason, "", "scottmat",
                                                                         "Pico/Plating Bucket"));
        }

        bucketDao.clear();

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework("Pico/Plating Bucket");

        Assert.assertEquals(validationMessages.size(), 0);

        Assert.assertEquals(entries.size(), existingReworks + mapBarcodeToTube.size());

        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));

    }

    @Test
    public void testHappyPathWithValidationCurrentlyInBucket() throws Exception {

        List<String> validationMessages = new ArrayList<>();

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst8");

        List<BucketEntry> bucketCleanupItems = new ArrayList<>();

        try {
            for (Map.Entry<String, BarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

                pBucket = bucketDao.findByName(bucketName);
                bucketCleanupItems.add(pBucket.addEntry(exExProductOrder1, barcodedTubeDao.findByBarcode(
                        currEntry.getKey()), BucketEntry.BucketEntryType.PDO_ENTRY));

                bucketDao.flush();
            }
            for (Map.Entry<String, BarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {
                ReworkEjb.BucketCandidate bucketCandidate =
                        new ReworkEjb.BucketCandidate(currEntry.getKey(), exExProductOrder1);
                bucketCandidate.setReworkItem(true);
                String unknownReason =
                        ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
                reworkEjb.addAndValidateCandidates(Collections.singleton(bucketCandidate), unknownReason, "",
                                                   "scottmat", "Pico/Plating Bucket");
            }
            Assert.fail("With the tube in the bucket, Calling Rework should throw an Exception");
        } catch (ValidationException e) {

        } finally {
            for (BucketEntry entry : bucketCleanupItems) {
                entry.setStatus(BucketEntry.Status.Archived);
            }
        }
    }


    @Test
    public void testHappyPathWithAncestorValidation() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst9");

        List<String> validationMessages = new ArrayList<>();

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettaLimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT1" + currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                                       Workflow.AGILENT_EXOME_EXPRESS,
                                       bettaLimsMessageResource,
                                       reagentDesignDao, barcodedTubeDao,
                                       appConfig.getUrl(), 2);

        exExProductOrder1 = refreshPdo(exExProductOrder1.getBusinessKey());
        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
            ReworkEjb.BucketCandidate bucketCandidate =
                    new ReworkEjb.BucketCandidate(barcode, exExProductOrder1);
            bucketCandidate.setReworkItem(true);
            String unknownReason =
                    ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
            validationMessages.addAll(reworkEjb.addAndValidateCandidates(Collections.singleton(bucketCandidate),
                                                                         unknownReason, "", "scottmat",
                                                                         "Pico/Plating Bucket"));
        }

        bucketDao.clear();

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework("Pico/Plating Bucket");

        Assert.assertEquals(validationMessages.size(), 0);

        Assert.assertEquals(entries.size(), existingReworks + hybridSelectionJaxbBuilder.getNormCatchBarcodes().size());

        validateBarcodeExistence(hybridSelectionJaxbBuilder, entries);

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {

            BarcodedTube currentTube = barcodedTubeDao.findByBarcode(barcode);
            for (BucketEntry currentEntry : currentTube.getBucketEntries()) {
                currentEntry.setStatus(BucketEntry.Status.Archived);
            }
        }
        barcodedTubeDao.flush();

    }


    @Test
    public void testMixedDNAWithValidation() throws Exception {

        List<String> validationMessages = new ArrayList<>();

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime()) + "tst10");

        for (String barcode : mapBarcodeToTube.keySet()) {
            ReworkEjb.BucketCandidate bucketCandidate =
                    new ReworkEjb.BucketCandidate(barcode, exExProductOrder2);
            bucketCandidate.setReworkItem(true);
            String unknownReason =
                    ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
            validationMessages.addAll(reworkEjb.addAndValidateCandidates(Collections.singleton(bucketCandidate),
                                                                         unknownReason, "", "scottmat",
                                                                         "Pico/Plating Bucket"));
        }

        bucketDao.clear();

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework("Pico/Plating Bucket");

        Assert.assertEquals(validationMessages.size(), 0);

        Assert.assertEquals(entries.size(), existingReworks + mapBarcodeToTube.size());

        Assert.assertTrue(entries.contains(mapBarcodeToTube.values().iterator().next()));
    }


    @Test
    public void testMixedDNAWithValidationAndAncestors() throws Exception {

        List<String> validationMessages = new ArrayList<>();

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime()) + "tst11");

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettaLimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT2" + currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                                       Workflow.AGILENT_EXOME_EXPRESS,
                                       bettaLimsMessageResource,
                                       reagentDesignDao, barcodedTubeDao,
                                       appConfig.getUrl(), 2);

        exExProductOrder2 = refreshPdo(exExProductOrder2.getBusinessKey());
        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
            ReworkEjb.BucketCandidate bucketCandidate =
                    new ReworkEjb.BucketCandidate(barcode, exExProductOrder2);
            bucketCandidate.setReworkItem(true);
            String unknownReason =
                    ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
            validationMessages.addAll(reworkEjb.addAndValidateCandidates(Collections.singleton(bucketCandidate),
                                                                         unknownReason, "", "scottmat",
                                                                         "Pico/Plating Bucket"));
        }

        bucketDao.clear();

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework("Pico/Plating Bucket");

        Assert.assertEquals(validationMessages.size(), 0);

        Assert.assertEquals(entries.size(), existingReworks + hybridSelectionJaxbBuilder.getNormCatchBarcodes().size());

        validateBarcodeExistence(hybridSelectionJaxbBuilder, entries);

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {

            BarcodedTube currentTube = barcodedTubeDao.findByBarcode(barcode);
            for (BucketEntry currentEntry : currentTube.getBucketEntries()) {
                currentEntry.setStatus(BucketEntry.Status.Archived);
            }
        }
        barcodedTubeDao.flush();

    }

    @Test(enabled = true)
    public void testMixedDNAWithValidationAndAncestorsPreviouslyInBucket() throws Exception {

        List<String> validationMessages = new ArrayList<>();

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime()) + "tst12");

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        for (Map.Entry<String, BarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

            pBucket = bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(exExProductOrder2, barcodedTubeDao.findByBarcode(
                    currEntry.getKey()), BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.flush();
        }

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettaLimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT4" + currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                                       Workflow.AGILENT_EXOME_EXPRESS,
                                       bettaLimsMessageResource,
                                       reagentDesignDao, barcodedTubeDao,
                                       appConfig.getUrl(), 2);

        exExProductOrder2 = refreshPdo(exExProductOrder2.getBusinessKey());
        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
            ReworkEjb.BucketCandidate bucketCandidate =
                    new ReworkEjb.BucketCandidate(barcode, exExProductOrder2);
            bucketCandidate.setReworkItem(true);
            String unknownReason =
                    ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
            validationMessages.addAll(reworkEjb.addAndValidateCandidates(Collections.singleton(bucketCandidate),
                                                                         unknownReason, "", "scottmat",
                                                                         "Pico/Plating Bucket"));
        }

        bucketDao.clear();

        Collection<LabVessel> entries = reworkEjb.getVesselsForRework("Pico/Plating Bucket");

        Assert.assertEquals(validationMessages.size(), 0);

        Assert.assertEquals(entries.size(), existingReworks + hybridSelectionJaxbBuilder.getNormCatchBarcodes().size());

        validateBarcodeExistence(hybridSelectionJaxbBuilder, entries);

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {

            BarcodedTube currentTube = barcodedTubeDao.findByBarcode(barcode);
            for (BucketEntry currentEntry : currentTube.getBucketEntries()) {
                currentEntry.setStatus(BucketEntry.Status.Archived);
            }
        }
        barcodedTubeDao.flush();

    }


    @Test
    public void testMixedDNAWithValidationAndAncestorsCurrentlyInBucket() throws Exception {

        List<String> validationMessages = new ArrayList<>();

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime()) + "tst13");

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = BettaLimsMessageResourceTest
                .sendMessagesUptoCatch("SGM_RWIT3" + currDate.getTime(), mapBarcodeToTube, bettaLimsMessageFactory,
                                       Workflow.AGILENT_EXOME_EXPRESS,
                                       bettaLimsMessageResource,
                                       reagentDesignDao, barcodedTubeDao,
                                       appConfig.getUrl(), 2);

        List<BucketEntry> bucketCleanupItems = new ArrayList<>();

        exExProductOrder2 = refreshPdo(exExProductOrder2.getBusinessKey());
        for (Map.Entry<String, BarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {

            pBucket = bucketDao.findByName(bucketName);
            bucketCleanupItems.add(pBucket.addEntry(exExProductOrder2, barcodedTubeDao.findByBarcode(
                    currEntry.getKey()), BucketEntry.BucketEntryType.PDO_ENTRY));
            bucketDao.flush();
        }

        for (String barcode : hybridSelectionJaxbBuilder.getNormCatchBarcodes()) {
            ReworkEjb.BucketCandidate bucketCandidate =
                    new ReworkEjb.BucketCandidate(barcode, exExProductOrder2);
            bucketCandidate.setReworkItem(true);
            String unknownReason =
                    ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue();
            validationMessages.addAll(reworkEjb.addAndValidateCandidates(Collections.singleton(bucketCandidate),
                                                                         unknownReason, "", "scottmat",
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

            BarcodedTube currentTube = barcodedTubeDao.findByBarcode(barcode);
            for (BucketEntry currentEntry : currentTube.getBucketEntries()) {
                currentEntry.setStatus(BucketEntry.Status.Archived);
            }
        }
        barcodedTubeDao.flush();

    }

    @Test
    public void testFindCandidatesMultiplePdosNoBuckets() throws Exception {

        createInitialTubes(bucketReadySamples1, String.valueOf((new Date()).getTime()) + "tst2");

        List<ReworkEjb.BucketCandidate> candidates = new ArrayList<>();

        List<ProductOrderSample> bucketReadySamplesDupe = new ArrayList<>(2);
        ProductOrderSample productOrderSample1 = new ProductOrderSample(genomicSample1);
        bucketReadySamplesDupe.add(productOrderSample1);

        ProductOrderSample productOrderSample2 = new ProductOrderSample(genomicSample2);
        bucketReadySamplesDupe.add(productOrderSample2);

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
        bucketReadySamples1.get(0).getMercurySample().addProductOrderSample(productOrderSample1);
        bucketReadySamples1.get(1).getMercurySample().addProductOrderSample(productOrderSample2);

        for (Map.Entry<String, BarcodedTube> tubes : mapBarcodeToTube.entrySet()) {

            candidates.addAll(reworkEjb.findBucketCandidates(tubes.getValue().getSampleNames().iterator().next()));
        }

        Assert.assertEquals(candidates.size(), mapBarcodeToTube.size() * 2);

        for (ReworkEjb.BucketCandidate candidate : candidates) {
            //TODO Need to figure a way to get Stub search really working to validate the Barcode
            Assert.assertTrue(candidate.isValid());
//            Assert.assertTrue(mapBarcodeToTube.keySet().contains(candidate.getTubeBarcode()),"Did not find barcode " + candidate.getTubeBarcode() + "In the map of created tubes");
        }
    }

    @Test
    public void testFindCandidatesMultiplePdosWithBuckets() throws Exception {

        createInitialTubes(bucketReadySamples2, String.valueOf((new Date()).getTime()) + "tst2");

        List<ReworkEjb.BucketCandidate> candidates = new ArrayList<>();

        List<ProductOrderSample> bucketReadySamplesDupe = new ArrayList<>(2);
        ProductOrderSample productOrderSample1 = new ProductOrderSample(genomicSample3);
        bucketReadySamplesDupe.add(productOrderSample1);

        ProductOrderSample productOrderSample2 = new ProductOrderSample(somaticSample3);
        bucketReadySamplesDupe.add(productOrderSample2);

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
        bucketReadySamples2.get(0).getMercurySample().addProductOrderSample(productOrderSample1);
        bucketReadySamples2.get(0).getMercurySample().addProductOrderSample(extraProductOrderSample);
        bucketReadySamples2.get(1).getMercurySample().addProductOrderSample(productOrderSample2);

        for (Map.Entry<String, BarcodedTube> tubes : mapBarcodeToTube.entrySet()) {

            pBucket = bucketDao.findByName(bucketName);
            BucketEntry newEntry = pBucket.addEntry(duplicatePO, barcodedTubeDao.findByBarcode(tubes.getKey()),
                                                    BucketEntry.BucketEntryType.PDO_ENTRY);
            newEntry.setStatus(BucketEntry.Status.Archived);
            bucketDao.flush();

            candidates.addAll(reworkEjb.findBucketCandidates(tubes.getValue().getSampleNames().iterator().next()));
        }

        // genomicSample3 is in 3 PDOs and somaticSample3 is in 2 PDOs
        Assert.assertEquals(candidates.size(), 5);

        for (ReworkEjb.BucketCandidate candidate : candidates) {

            //TODO Need to figure a way to get Stub search really working to validate the Barcode

            Assert.assertTrue(candidate.isValid());
        }
    }

    private void createInitialTubes(@Nonnull List<ProductOrderSample> pos,
                                    @Nonnull String barcodePrefix) {

        for (ProductOrderSample currSamp : pos) {
            BspSampleData sampleData = sampleDataMap.get(currSamp.getSampleKey());
            String barcode = sampleData.getContainerId();

            BarcodedTube aliquot = new BarcodedTube(barcode);
            MercurySample mercurySample = new MercurySample(currSamp.getSampleKey(), sampleData);
            mercurySample.addProductOrderSample(currSamp);
            aliquot.addSample(mercurySample);
            mapBarcodeToTube.put(barcode, aliquot);
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

        for (Map.Entry<String, BarcodedTube> currEntry : mapBarcodeToTube.entrySet()) {
            List<BucketEntry> bucketEntries = new ArrayList<>();
            Thread.sleep(2L); // LabVessel#addBucketEntry() fails on npn-unique bucket, labVessel, and createdDate
            bucketEntries.add(pBucket.addEntry(exExProductOrder1, barcodedTubeDao.findByBarcode(currEntry.getKey()),
                                               BucketEntry.BucketEntryType.PDO_ENTRY));
            Thread.sleep(2L);
            bucketEntries.add(pBucket.addEntry(exExProductOrder2, barcodedTubeDao.findByBarcode(currEntry.getKey()),
                                               BucketEntry.BucketEntryType.PDO_ENTRY));
            bucketDao.flush();
            for (BucketEntry bucketEntry : bucketEntries) {
                bucketEntryIds.add(bucketEntry.getBucketEntryId());
            }

        }
        bucketReadySamples2.get(0).getMercurySample().addProductOrderSample(extraProductOrderSample);

        Set<String> bucketCandidatePdos =
                reworkEjb.findBucketCandidatePdos(bucketEntryIds);
        Assert.assertFalse(bucketCandidatePdos.isEmpty());
        Assert.assertEquals(bucketCandidatePdos.size(), 2);
    }

    private void resetExExProductWorkflow() {
        if (exExProductOrder2 != null) {
            exExProductOrder2.getProduct().setWorkflowName(Workflow.AGILENT_EXOME_EXPRESS);
        }
        if (exExProductOrder1 != null) {
            exExProductOrder1.getProduct().setWorkflowName(Workflow.AGILENT_EXOME_EXPRESS);
        }
    }

    private ProductOrder refreshPdo(String key) {
        ProductOrder productOrder = productOrderDao.findByBusinessKey(key);
        for (ProductOrderSample sample : productOrder.getSamples()) {
            sample.getMercurySample().setSampleData(sampleDataMap.get(sample.getSampleKey()));
        }

        return productOrder;
    }
}
