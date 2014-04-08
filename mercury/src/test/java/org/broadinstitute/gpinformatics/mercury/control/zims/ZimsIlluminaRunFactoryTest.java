package org.broadinstitute.gpinformatics.mercury.control.zims;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SequencingTemplateFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventRefDataFetcher;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ZimsIlluminaRunFactoryTest {

    private ZimsIlluminaRunFactory zimsIlluminaRunFactory;
    private IlluminaFlowcell flowcell;
    private TwoDBarcodedTube testTube;

    private BSPSampleDataFetcher mockBSPSampleDataFetcher;
    private AthenaClientService mockAthenaClientService;
    private ControlDao mockControlDao;
    private ProductOrder testProductOrder;
    private Map<String, BSPSampleDTO> mapSampleIdToDto;
    private Map<String, ProductOrder> mapKeyToProductOrder;
    private static final String TEST_SAMPLE_ID = "TestSM-1";
    private static final String PRODUCT_ORDER_KEY = "TestPDO-1";
    private Date runDate;
    private static final short LANE_NUMBER = 1;
    private BSPSampleDTO bspSampleDTO;
    private JiraService mockJiraService;
    @BeforeMethod(groups = DATABASE_FREE)
    public void setUp() {
        runDate = new Date();
        mockBSPSampleDataFetcher = Mockito.mock(BSPSampleDataFetcher.class);
        mockAthenaClientService = Mockito.mock(AthenaClientServiceImpl.class);
        mockControlDao = Mockito.mock(ControlDao.class);

        mockJiraService = Mockito.mock(JiraService.class);
        Mockito.when(mockJiraService.createTicketUrl(Mockito.anyString())).thenReturn("jira://LCSET-1" );

        // Create a test product
        Product testProduct = new Product("Test Product", new ProductFamily("Test Product Family"), "Test product",
                "P-TEST-1", new Date(), new Date(), 0, 0, 0, 0, "Test samples only", "None", true,
                Workflow.AGILENT_EXOME_EXPRESS, false, "agg type");

        zimsIlluminaRunFactory = new ZimsIlluminaRunFactory(mockBSPSampleDataFetcher, mockAthenaClientService,
                mockControlDao, new SequencingTemplateFactory());
        LabEventFactory labEventFactory = new LabEventFactory(null, null);
        labEventFactory.setLabEventRefDataFetcher(new LabEventRefDataFetcher() {
            @Override
            public BspUser getOperator(String userId) {
                return new BSPUserList.QADudeUser("Test", 101L);
            }

            @Override
            public BspUser getOperator(Long bspUserId) {
                return new BSPUserList.QADudeUser("Test", bspUserId);
            }

            @Override
            public LabBatch getLabBatch(String labBatchName) {
                return null;
            }
        });
        // Create a test research project
        ResearchProject testResearchProject =
                new ResearchProject(101L, "Test Project", "ZimsIlluminaRunFactoryTest project", true);
        testResearchProject.setJiraTicketKey("TestRP-1");

        // Create a test product order
        testProductOrder = new ProductOrder(101L, "Test Order", Collections.singletonList(
                new ProductOrderSample(TEST_SAMPLE_ID)), "Quote-1", testProduct, testResearchProject);
        testProductOrder.setJiraTicketKey("TestPDO-1");
        Mockito.when(mockAthenaClientService.retrieveProductOrderDetails(PRODUCT_ORDER_KEY)).thenReturn(testProductOrder);
        flowcell = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.HiSeqFlowcell, "testFlowcell");
        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>() {{
            put(BSPSampleSearchColumn.CONTAINER_ID, "BspContainer");
            put(BSPSampleSearchColumn.STOCK_SAMPLE, "Stock1");
            put(BSPSampleSearchColumn.ROOT_SAMPLE, "RootSample");
            put(BSPSampleSearchColumn.SAMPLE_ID, TEST_SAMPLE_ID);
            put(BSPSampleSearchColumn.PARTICIPANT_ID, "Spencer");
            put(BSPSampleSearchColumn.SPECIES, "Hamster");
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "first_sample");
            put(BSPSampleSearchColumn.COLLECTION, "collection1");
            put(BSPSampleSearchColumn.VOLUME, "7");
            put(BSPSampleSearchColumn.CONCENTRATION, "9");
            put(BSPSampleSearchColumn.LSID, "ZimsIlluminaRunFactoryTest.testMakeLibraryBean.sampleDTO");
            put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, "participant1");
            put(BSPSampleSearchColumn.MATERIAL_TYPE, "Test Material");
            put(BSPSampleSearchColumn.TOTAL_DNA, "42");
            put(BSPSampleSearchColumn.SAMPLE_TYPE, "Test Sample");
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, "Test failure");
            put(BSPSampleSearchColumn.GENDER, "M");
            put(BSPSampleSearchColumn.STOCK_TYPE, "Stock Type");
            put(BSPSampleSearchColumn.FINGERPRINT, "fingerprint");
            put(BSPSampleSearchColumn.SAMPLE_ID, TEST_SAMPLE_ID);
            put(BSPSampleSearchColumn.SAMPLE_TYPE, "ZimsIlluminaRunFactoryTest");
            put(BSPSampleSearchColumn.RACE, "N/A");
            put(BSPSampleSearchColumn.ETHNICITY, "unknown");
            put(BSPSampleSearchColumn.RACKSCAN_MISMATCH, "false");
            put(BSPSampleSearchColumn.RIN, "8.4");
        }};
        bspSampleDTO = new BSPSampleDTO(dataMap);
        mapSampleIdToDto = new HashMap<String, BSPSampleDTO>() {{
                   put(TEST_SAMPLE_ID, bspSampleDTO);
               }};
        mapKeyToProductOrder = new HashMap<String, ProductOrder>() {{
            put(PRODUCT_ORDER_KEY, testProductOrder);
        }};

    }

    private List<ZimsIlluminaRunFactory.SampleInstanceDto> createSampleInstanceDto(
            LabBatch.LabBatchType... testLabBatchTypes) {
        List<ZimsIlluminaRunFactory.SampleInstanceDto> sampleInstanceDtoList=new ArrayList<>(testLabBatchTypes.length);
        String sourceTubeBarcode = "testTube";
        testTube = new TwoDBarcodedTube(sourceTubeBarcode);
        MercurySample mercurySample = new MercurySample(TEST_SAMPLE_ID);
        testTube.addSample(mercurySample);
        BucketEntry bucketEntry = new BucketEntry(testTube, PRODUCT_ORDER_KEY, BucketEntry.BucketEntryType.PDO_ENTRY);

        for (int i = 0; i < testLabBatchTypes.length; i++) {
            LabBatch.LabBatchType testLabBatchType = testLabBatchTypes[i];
            int suffix = i + 1;
            String batchName = testLabBatchType.name() + "-" + suffix;
            if (testLabBatchType == LabBatch.LabBatchType.WORKFLOW) {
                batchName = "LCSET-" + suffix;
            }
            LabBatch batch = new LabBatch(batchName, Collections.<LabVessel>singleton(testTube), testLabBatchType);
            if (testLabBatchType == LabBatch.LabBatchType.WORKFLOW) {
                JiraTicket lcSetTicket = new JiraTicket(mockJiraService, batchName);
                batch.setJiraTicket(lcSetTicket);
                batch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
                batch.addBucketEntry(bucketEntry);
                bucketEntry.setLabBatch(batch);
            }
            // todo jmt revisit this
            SampleInstanceV2 instance = new SampleInstanceV2(testTube);
            sampleInstanceDtoList.add(
                            new ZimsIlluminaRunFactory.SampleInstanceDto(LANE_NUMBER, testTube, instance, TEST_SAMPLE_ID,
                                    PRODUCT_ORDER_KEY, null, null, mercurySample.getSampleKey()));
        }

        return sampleInstanceDtoList;
    }


    @Test(groups = DATABASE_FREE)
    public void testGetLibraryTwoLcSetBatches() {
        List<ZimsIlluminaRunFactory.SampleInstanceDto> instanceDtoList =
                createSampleInstanceDto(LabBatch.LabBatchType.WORKFLOW, LabBatch.LabBatchType.WORKFLOW,
                        LabBatch.LabBatchType.BSP);
        try {
            List<LibraryBean> zimsIlluminaRuns = zimsIlluminaRunFactory.makeLibraryBeans(
                    instanceDtoList, mapSampleIdToDto, mapKeyToProductOrder, Collections.EMPTY_MAP);
        } catch (RuntimeException r){
            assertThat(r.getLocalizedMessage(), equalTo("Expected one LabBatch but found 2."));
        }
    }

    @Test(groups = DATABASE_FREE)
    public void testMakeZimsIlluminaRun() {
        Date runDate = new Date(1358889107084L);
        String testRunDirectory = "TestRun";
        LabVessel denatureTube = flowcell.getNearestTubeAncestorsForLanes().values().iterator().next();

        IlluminaSequencingRun sequencingRun = new IlluminaSequencingRun(flowcell, testRunDirectory, "Run-123",
                "ZimsIlluminaRunFactoryTest", 101L, true, runDate, "/root/path/to/run/" + testRunDirectory);
        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(sequencingRun);

        assertThat(zimsIlluminaRun.getError(), nullValue());
        assertThat(zimsIlluminaRun.getName(), equalTo("TestRun"));
        assertThat(zimsIlluminaRun.getBarcode(), equalTo("Run-123"));
        assertThat(zimsIlluminaRun.getSequencer(), equalTo("ZimsIlluminaRunFactoryTest"));
        assertThat(zimsIlluminaRun.getFlowcellBarcode(), equalTo("testFlowcell"));
        assertThat(zimsIlluminaRun.getRunDateString(), equalTo("01/22/2013 16:11"));
        assertThat(zimsIlluminaRun.getSequencerModel(), equalTo(
                IlluminaFlowcell.FlowcellType.HiSeqFlowcell.getSequencerModel()));
//        assertThat(zimsIlluminaRun.getPairedRun(), is(true)); // TODO SGM will pull from Workflow
//        assertThat(zimsIlluminaRun.getLanes().size(), equalTo(8)); // TODO SGM WIll pull from workflow

        for (ZimsIlluminaChamber lane : zimsIlluminaRun.getLanes()) {
            assertThat(lane.getLibraries().size(), is(1));
            assertThat(lane.getSequencedLibrary(), equals(denatureTube.getLabel()));
        }
        assertThat(zimsIlluminaRun.getSequencerModel(),equalTo("Illumina HiSeq 2000"));
        assertThat(zimsIlluminaRun.getRunFolder(), equalTo("/root/path/to/run/" + testRunDirectory));
    }

    @Test(groups = DATABASE_FREE)
    public void testMakeLibraryBean() {
        List<ZimsIlluminaRunFactory.SampleInstanceDto> instanceDtoList = createSampleInstanceDto(
                LabBatch.LabBatchType.WORKFLOW, LabBatch.LabBatchType.BSP);
        List<LibraryBean> zimsIlluminaRuns = zimsIlluminaRunFactory.makeLibraryBeans(instanceDtoList, mapSampleIdToDto,
                mapKeyToProductOrder, Collections.EMPTY_MAP);
        for (LibraryBean libraryBean : zimsIlluminaRuns) {
            assertThat(libraryBean.getLibrary(),
                    equalTo("testTube")); // TODO: expand with full definition of generated library name
            assertThat(libraryBean.getProject(), nullValue());
//            assertThat(libraryBean.getMolecularIndexingScheme().getName(), equalTo("???")); // TODO
            assertThat(libraryBean.getSpecies(), equalTo("Hamster"));
            assertThat(libraryBean.getLsid(), equalTo("ZimsIlluminaRunFactoryTest.testMakeLibraryBean.sampleDTO"));
            assertThat(libraryBean.getParticipantId(), equalTo("Spencer"));
            assertThat(libraryBean.getLcSet(), equalTo("LCSET-1"));
            assertThat(libraryBean.getProductOrderTitle(), equalTo("Test Order"));
            assertThat(libraryBean.getProductOrderKey(), equalTo("TestPDO-1"));
            assertThat(libraryBean.getResearchProjectName(), equalTo("Test Project"));
            assertThat(libraryBean.getResearchProjectId(), equalTo("TestRP-1"));
            assertThat(libraryBean.getProduct(), equalTo("Test Product"));
            assertThat(libraryBean.getDataType(), equalTo("agg type"));
            assertThat(libraryBean.getProductFamily(), equalTo("Test Product Family"));
            assertThat(libraryBean.getRootSample(), equalTo("RootSample"));
            assertThat(libraryBean.getSampleId(), equalTo(TEST_SAMPLE_ID));
            assertThat(libraryBean.getGender(), equalTo("M"));
            assertThat(libraryBean.getCollection(), equalTo("collection1"));
            assertThat(libraryBean.getPrimaryDisease(), equalTo("Test failure"));
            assertThat(libraryBean.getCollaboratorSampleId(), equalTo("first_sample"));
            assertThat(libraryBean.getCollaboratorParticipantId(), equalTo("participant1"));
            assertThat(libraryBean.getMaterialType(), equalTo("Test Material"));
            assertThat(libraryBean.getIsGssrSample(), equalTo(false));
            assertThat(libraryBean.getPopulation(), equalTo("unknown"));
            assertThat(libraryBean.getRace(), equalTo("N/A"));
            // in mercury, pipeline should always be told to aggregate
            assertThat(libraryBean.doAggregation(), equalTo(true));
            assertThat(libraryBean.getProductOrderSample(), equalTo(TEST_SAMPLE_ID));
        }
    }
}
