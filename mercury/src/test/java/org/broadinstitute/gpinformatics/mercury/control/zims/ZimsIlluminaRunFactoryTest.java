package org.broadinstitute.gpinformatics.mercury.control.zims;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SequencingTemplateFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventRefDataFetcher;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.DATABASE_FREE)
public class ZimsIlluminaRunFactoryTest {

    private static final String PRODUCT_ORDER_KEY = "TestPDO-1";
    private static final short LANE_NUMBER = 1;
    private final Map<String, BSPSampleDTO> mapSampleIdToDto = new HashMap<>();
    private final Map<String, ProductOrder> mapKeyToProductOrder = new HashMap<>();
    private final List<String> testSampleIds = new ArrayList<>();
    private ZimsIlluminaRunFactory zimsIlluminaRunFactory;
    private IlluminaFlowcell flowcell;
    private BSPSampleDataFetcher mockBSPSampleDataFetcher;
    private ControlDao mockControlDao;
    private ProductOrder testProductOrder;
    private JiraService mockJiraService;
    private ProductOrderDao productOrderDao;
    private List<MolecularIndexReagent> reagents;

    @BeforeMethod(groups = DATABASE_FREE)
    public void setUp() {
        testSampleIds.clear();
        testSampleIds.add("TestSM-0");
        testSampleIds.add("TestSM-1");
        testSampleIds.add("TestSM-2");

        mockBSPSampleDataFetcher = Mockito.mock(BSPSampleDataFetcher.class);
        mockControlDao = Mockito.mock(ControlDao.class);

        mockJiraService = Mockito.mock(JiraService.class);
        productOrderDao = Mockito.mock(ProductOrderDao.class);
        Mockito.when(mockJiraService.createTicketUrl(Mockito.anyString())).thenReturn("jira://LCSET-1");

        // Create a test product
        Product testProduct = new Product("Test Product", new ProductFamily("Test Product Family"), "Test product",
                                          "P-TEST-1", new Date(), new Date(), 0, 0, 0, 0, "Test samples only", "None",
                                          true, Workflow.AGILENT_EXOME_EXPRESS, false, "agg type");

        zimsIlluminaRunFactory = new ZimsIlluminaRunFactory(mockBSPSampleDataFetcher, mockControlDao,
                new SequencingTemplateFactory(), productOrderDao);
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
        List<ProductOrderSample> pdoSamples = new ArrayList<>();
        for (String testSampleId : testSampleIds) {
            pdoSamples.add(new ProductOrderSample(testSampleId));
        }
        testProductOrder =
                new ProductOrder(101L, "Test Order", pdoSamples, "Quote-1", testProduct, testResearchProject);
        testProductOrder.setJiraTicketKey("TestPDO-1");

        Mockito.when(productOrderDao.findByBusinessKey(PRODUCT_ORDER_KEY)).thenReturn(testProductOrder);

        reagents = makeTestReagents(testSampleIds.size(), false);

        mapSampleIdToDto.clear();

        // Makes BSP data for each sample.
        for (int i = 0; i < testSampleIds.size(); ++i) {
            final String sampleId = testSampleIds.get(i);
            Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>() {{
                put(BSPSampleSearchColumn.CONTAINER_ID, "BspContainer");
                put(BSPSampleSearchColumn.STOCK_SAMPLE, sampleId + "stock");
                put(BSPSampleSearchColumn.ROOT_SAMPLE, sampleId + "root");
                put(BSPSampleSearchColumn.SAMPLE_ID, sampleId);
                put(BSPSampleSearchColumn.PARTICIPANT_ID, "Spencer");
                put(BSPSampleSearchColumn.SPECIES, "Hamster");
                put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, sampleId + "collaborator");
                put(BSPSampleSearchColumn.COLLECTION, "collection1");
                put(BSPSampleSearchColumn.VOLUME, "7");
                put(BSPSampleSearchColumn.CONCENTRATION, "9");
                put(BSPSampleSearchColumn.LSID, "ZimsIlluminaRunFactoryTest.testMakeLibraryBean.sampleDTO");
                put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, sampleId + "participant");
                put(BSPSampleSearchColumn.MATERIAL_TYPE, "Test Material");
                put(BSPSampleSearchColumn.TOTAL_DNA, "42");
                put(BSPSampleSearchColumn.SAMPLE_TYPE, "Test Sample");
                put(BSPSampleSearchColumn.PRIMARY_DISEASE, "Test failure");
                put(BSPSampleSearchColumn.GENDER, "M");
                put(BSPSampleSearchColumn.STOCK_TYPE, "Stock Type");
                put(BSPSampleSearchColumn.SAMPLE_TYPE, "ZimsIlluminaRunFactoryTest");
                put(BSPSampleSearchColumn.RACE, "N/A");
                put(BSPSampleSearchColumn.ETHNICITY, "unknown");
                put(BSPSampleSearchColumn.RACKSCAN_MISMATCH, "false");
                put(BSPSampleSearchColumn.RIN, "8.4");
            }};
            mapSampleIdToDto.put(sampleId, new BSPSampleDTO(dataMap));
        }

        mapKeyToProductOrder.clear();
        mapKeyToProductOrder.put(PRODUCT_ORDER_KEY, testProductOrder);

        flowcell = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.HiSeqFlowcell, "testFlowcell");

    }

    /** Creates multiple dtos, one for each combination of testSampleId and testLabBatchType. */
    private List<ZimsIlluminaRunFactory.SampleInstanceDto> createSampleInstanceDto(
            LabBatch.LabBatchType... testLabBatchTypes) {
        List<ZimsIlluminaRunFactory.SampleInstanceDto> sampleInstanceDtoList = new ArrayList<>();

        for (int sampleIdx = 0; sampleIdx < testSampleIds.size(); ++sampleIdx) {
            String sourceTubeBarcode = "testTube" + sampleIdx;
            BarcodedTube testTube = new BarcodedTube(sourceTubeBarcode);

            MercurySample mercurySample = new MercurySample(testSampleIds.get(sampleIdx),
                    MercurySample.MetadataSource.BSP);
            testTube.addSample(mercurySample);

            BucketEntry bucketEntry =
                    new BucketEntry(testTube, testProductOrder, BucketEntry.BucketEntryType.PDO_ENTRY);

            for (int batchTypeIdx = 0; batchTypeIdx < testLabBatchTypes.length; batchTypeIdx++) {
                LabBatch.LabBatchType testLabBatchType = testLabBatchTypes[batchTypeIdx];
                int suffix = batchTypeIdx + 1;
                String batchName = testLabBatchType.name() + "-" + suffix;
                if (testLabBatchType == LabBatch.LabBatchType.WORKFLOW) {
                    batchName = "LCSET-" + suffix;
                }
                LabBatch batch = new LabBatch(batchName, Collections.<LabVessel>singleton(testTube), testLabBatchType);

                SampleInstanceV2 instance = new SampleInstanceV2(testTube);
                instance.addReagent(reagents.get(sampleIdx));

                if (testLabBatchType == LabBatch.LabBatchType.WORKFLOW) {
                    JiraTicket lcSetTicket = new JiraTicket(mockJiraService, batchName);
                    batch.setJiraTicket(lcSetTicket);
                    batch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
                    batch.addBucketEntry(bucketEntry);
                    bucketEntry.setLabBatch(batch);
                }
                sampleInstanceDtoList.add(new ZimsIlluminaRunFactory.SampleInstanceDto(LANE_NUMBER, testTube, instance,
                        testSampleIds.get(sampleIdx), PRODUCT_ORDER_KEY, null, null, mercurySample.getSampleKey()));
            }
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

        } catch (RuntimeException r) {
            assertThat(r.getLocalizedMessage(), equalTo("Expected one LabBatch but found 2."));
        }
    }

    @Test(groups = DATABASE_FREE)
    public void testMakeZimsIlluminaRun() {
        Date runDate = new Date(1358889107084L);
        String testRunDirectory = "TestRun";
        LabVessel denatureTube = flowcell.getNearestTubeAncestorsForLanes().values().iterator().next();

        IlluminaSequencingRun sequencingRun = new IlluminaSequencingRun(flowcell, testRunDirectory, "Run-123",
                                                                        "ZimsIlluminaRunFactoryTest", 101L, true,
                                                                        runDate,
                                                                        "/root/path/to/run/" + testRunDirectory);
        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(sequencingRun);

        assertThat(zimsIlluminaRun.getError(), nullValue());
        assertThat(zimsIlluminaRun.getName(), equalTo("TestRun"));
        assertThat(zimsIlluminaRun.getBarcode(), equalTo("Run-123"));
        assertThat(zimsIlluminaRun.getSequencer(), equalTo("ZimsIlluminaRunFactoryTest"));
        assertThat(zimsIlluminaRun.getFlowcellBarcode(), equalTo("testFlowcell"));
        assertThat(zimsIlluminaRun.getRunDateString(), equalTo("01/22/2013 16:11"));
        assertThat(zimsIlluminaRun.getSequencerModel(), equalTo(
                IlluminaFlowcell.FlowcellType.HiSeqFlowcell.getSequencerModel()));

        for (ZimsIlluminaChamber lane : zimsIlluminaRun.getLanes()) {
            assertThat(lane.getLibraries().size(), is(1));
            assertThat(lane.getSequencedLibrary(), equals(denatureTube.getLabel()));
        }
        assertThat(zimsIlluminaRun.getSequencerModel(), equalTo("Illumina HiSeq 2000"));
        assertThat(zimsIlluminaRun.getRunFolder(), equalTo("/root/path/to/run/" + testRunDirectory));
    }

    @Test(groups = DATABASE_FREE)
    public void testMakeLibraryBean() {
        final int numberOfBatchTypeParameters = 2;
        List<ZimsIlluminaRunFactory.SampleInstanceDto> instanceDtoList =
                createSampleInstanceDto(LabBatch.LabBatchType.WORKFLOW, LabBatch.LabBatchType.BSP);
        assertThat(instanceDtoList.size(), equalTo(testSampleIds.size() * numberOfBatchTypeParameters));

        List<LibraryBean>  zimsIlluminaRuns = zimsIlluminaRunFactory.makeLibraryBeans(
                instanceDtoList, mapSampleIdToDto, mapKeyToProductOrder, Collections.EMPTY_MAP);

        for (LibraryBean libraryBean : zimsIlluminaRuns) {
            String sampleId = libraryBean.getSampleId();
            int testSampleIdsIndex = findSampleIndex(sampleId);
            Assert.assertTrue(testSampleIdsIndex >= 0, "Unknown sampleId " + sampleId);
            String reagentIndexName = reagents.get(testSampleIdsIndex).getMolecularIndexingScheme().getName();
            Assert.assertNotNull(reagentIndexName, "no index for " + sampleId);
            String tubeBarcode = "testTube" + testSampleIdsIndex;

            assertThat(libraryBean.getLibrary(), equalTo(tubeBarcode + "_" + reagentIndexName));
            assertThat(libraryBean.getProject(), nullValue());
            assertThat(libraryBean.getMolecularIndexingScheme().getName(), equalTo(reagentIndexName));
            assertThat(libraryBean.getSpecies(), equalTo("Hamster"));
            assertThat(libraryBean.getLsid(), equalTo("ZimsIlluminaRunFactoryTest.testMakeLibraryBean.sampleDTO"));
            assertThat(libraryBean.getParticipantId(), equalTo("Spencer"));
//xxx            assertThat(libraryBean.getLcSet(), equalTo("LCSET-1"));
            assertThat(libraryBean.getProductOrderTitle(), equalTo("Test Order"));
            assertThat(libraryBean.getProductOrderKey(), equalTo("TestPDO-1"));
            assertThat(libraryBean.getResearchProjectName(), equalTo("Test Project"));
            assertThat(libraryBean.getResearchProjectId(), equalTo("TestRP-1"));
            assertThat(libraryBean.getProduct(), equalTo("Test Product"));
            assertThat(libraryBean.getDataType(), equalTo("agg type"));
            assertThat(libraryBean.getProductFamily(), equalTo("Test Product Family"));
            assertThat(libraryBean.getRootSample(), equalTo(sampleId + "root"));
            assertThat(libraryBean.getGender(), equalTo("M"));
            assertThat(libraryBean.getCollection(), equalTo("collection1"));
            assertThat(libraryBean.getPrimaryDisease(), equalTo("Test failure"));
            assertThat(libraryBean.getCollaboratorSampleId(), equalTo(sampleId + "collaborator"));
            assertThat(libraryBean.getCollaboratorParticipantId(), equalTo(sampleId + "participant"));
            assertThat(libraryBean.getMaterialType(), equalTo("Test Material"));
            assertThat(libraryBean.getIsGssrSample(), equalTo(false));
            assertThat(libraryBean.getPopulation(), equalTo("unknown"));
            assertThat(libraryBean.getRace(), equalTo("N/A"));
            // in mercury, pipeline should always be told to aggregate
            assertThat(libraryBean.doAggregation(), equalTo(true));
            assertThat(libraryBean.getProductOrderSample(), equalTo(sampleId));
        }
    }

    /**
     * Starting with multiple SampleInstanceDtos having the same molecular index barcodes and the same sampleId,
     * tests that only one consolidated library bean is created.  See FCT-18466.
     */
    @Test(groups = DATABASE_FREE)
    public void testSplitRejoinWorkflowConsolidation() {
        List<ZimsIlluminaRunFactory.SampleInstanceDto> dtoList1 =
                createSampleInstanceDto(LabBatch.LabBatchType.WORKFLOW);
        // Hacks the dtos so all have the same molecular index name.
        for (ZimsIlluminaRunFactory.SampleInstanceDto dto : dtoList1) {
            dto.getSampleInstance().getReagents().clear();
            dto.getSampleInstance().addReagent(reagents.get(0));
        }

        // First tests that no consolidation happens, because the sample names are different.
        List<LibraryBean> unconsolidatedBeans1 = zimsIlluminaRunFactory.makeLibraryBeans(dtoList1,
                mapSampleIdToDto, mapKeyToProductOrder, Collections.EMPTY_MAP);
        assertThat(unconsolidatedBeans1.size(), equalTo(testSampleIds.size()));


        // Makes dtos with two having the same sampleId (but different molecular index names).
        testSampleIds.remove(1);
        testSampleIds.add(testSampleIds.get(0));
        List<ZimsIlluminaRunFactory.SampleInstanceDto> dtoList2 =
                createSampleInstanceDto(LabBatch.LabBatchType.WORKFLOW);

        // Second tests no consolidation of same sample, different molecular name dtos.
        List<LibraryBean> unconsolidatedBeans2 = zimsIlluminaRunFactory.makeLibraryBeans(dtoList2,
                mapSampleIdToDto, mapKeyToProductOrder, Collections.EMPTY_MAP);
        assertThat(unconsolidatedBeans2.size(), equalTo(testSampleIds.size()));

        // Using the dto list with first two dtos having same sampleId, make all the molecular index names the same.
        for (ZimsIlluminaRunFactory.SampleInstanceDto dto : dtoList2) {
            dto.getSampleInstance().getReagents().clear();
            dto.getSampleInstance().addReagent(reagents.get(0));
        }

        // Tests consolidation of the first two dtos.
        List<LibraryBean> consolidatedBeans = zimsIlluminaRunFactory.makeLibraryBeans(dtoList2,
                mapSampleIdToDto, mapKeyToProductOrder, Collections.EMPTY_MAP);
        assertThat(consolidatedBeans.size(), equalTo(testSampleIds.size() - 1));
    }


    /** Creates some reagents having molecular barcodes for test purposes. */
    public static List<MolecularIndexReagent> makeTestReagents(int numberOfReagents, final boolean doubleEnded) {
        final Random random = new Random(System.currentTimeMillis());
        List<MolecularIndexReagent> reagents = new ArrayList<>(numberOfReagents);
        for (int i = 0; i < numberOfReagents; ++i) {
            // Builds a string of 8 random nucleotide designators (i.e. A, C, G, T).
            final StringBuilder molecularIndex = new StringBuilder();
            for (int j = 0; j < 8; ++j) {
                molecularIndex.append("ACGT".charAt(random.nextInt(4)));
            }
            Map<MolecularIndexingScheme.IndexPosition, MolecularIndex> positionIndexMap =
                    new HashMap<MolecularIndexingScheme.IndexPosition, MolecularIndex>(){{
                        put(MolecularIndexingScheme.IndexPosition.ILLUMINA_P5,
                                new MolecularIndex(molecularIndex.toString()));
                        if (doubleEnded) {
                            put(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7,
                                    new MolecularIndex(molecularIndex.toString()));
                        }
                    }};

            MolecularIndexingScheme mis = new MolecularIndexingScheme(positionIndexMap);
            mis.setName(molecularIndex.toString().toLowerCase());
            reagents.add(new MolecularIndexReagent(mis));
        }
        return reagents;
    }

    /** Returns the index into testSampleIds of the given sampleId, or -1 if not found. */
    private int findSampleIndex(String sampleId) {
        for (int i = 0; i < testSampleIds.size(); ++i) {
            if (testSampleIds.get(i).equals(sampleId)) {
                return i;
            }
        }
        return -1;
    }

}
