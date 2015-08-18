package org.broadinstitute.gpinformatics.mercury.control.zims;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SequencingTemplateFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.CrspPipelineUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventRefDataFetcher;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StripTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.test.CrspControlsTestUtils;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.broadinstitute.gpinformatics.Matchers.argThat;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

// TODO: extend and use capabilities of BaseEventTest
@Test(groups = TestGroups.DATABASE_FREE)
public class ZimsIlluminaRunFactoryTest {

    private static final String POSITIVE_CONTROL_SAMPLE_PARTICIPANT_ID = "NA12878";
    private static final String BSP_SM_ID_FOR_POSITIVE_CONTROL = "SM-59YAY";
    private static final SampleData POSITIVE_CONTROL_SAMPLE_DTO = new BspSampleData(new HashMap<BSPSampleSearchColumn, String>() {{
        put(BSPSampleSearchColumn.STOCK_SAMPLE, BSP_SM_ID_FOR_POSITIVE_CONTROL);
        put(BSPSampleSearchColumn.ROOT_SAMPLE, BSP_SM_ID_FOR_POSITIVE_CONTROL);
        put(BSPSampleSearchColumn.SAMPLE_ID, BSP_SM_ID_FOR_POSITIVE_CONTROL);
        put(BSPSampleSearchColumn.PARTICIPANT_ID, POSITIVE_CONTROL_SAMPLE_PARTICIPANT_ID);
    }});

    private CrspPipelineUtils crspPipelineUtils = new CrspPipelineUtils(Deployment.DEV);
    private ResearchProject crspPositiveControlsResearchProject;
    private ResearchProjectDao mockResearchProjectDao;
    private static final String PRODUCT_ORDER_KEY = "TestPDO-1";
    private final String labBatchName = "LCSET-1";
    private static final short LANE_NUMBER = 1;
    private final Map<String, SampleData> mapSampleIdToDto = new HashMap<>();
    private final Map<String, ProductOrder> mapKeyToProductOrder = new HashMap<>();
    private final List<String> testSampleIds = new ArrayList<>();
    private BarcodedTube testTube;
    private ZimsIlluminaRunFactory zimsIlluminaRunFactory;
    private IlluminaFlowcell flowcell;
    private SampleDataFetcher mockSampleDataFetcher;
    private ControlDao mockControlDao;
    private ProductOrder testProductOrder;
    private JiraService mockJiraService;
    private ProductOrderDao productOrderDao;
    private List<MolecularIndexReagent> reagents;
    private static final ResearchProject.RegulatoryDesignation
            REGULATORY_DESIGNATION = ResearchProject.RegulatoryDesignation.RESEARCH_ONLY;
    private Map<String, Control> controlMap;

    private void setupCrsp() {
        CrspControlsTestUtils crspControlsTestUtils = new CrspControlsTestUtils();
        crspPositiveControlsResearchProject = crspControlsTestUtils.getMockCrspControlsProject();
        mockResearchProjectDao = crspControlsTestUtils.getMockResearchProjectDao();
    }


    @BeforeMethod(groups = DATABASE_FREE)
    public void setUp() {
        setupCrsp();
        testSampleIds.clear();
        testSampleIds.add("SM-59HLV");
        testSampleIds.add("SM-59HLW");
        testSampleIds.add("SM-59HLX");
        testSampleIds.add(BSP_SM_ID_FOR_POSITIVE_CONTROL);

        mockSampleDataFetcher = Mockito.mock(SampleDataFetcher.class);
        mockControlDao = Mockito.mock(ControlDao.class);

        mockJiraService = Mockito.mock(JiraService.class);
        productOrderDao = Mockito.mock(ProductOrderDao.class);

        Mockito.when(mockJiraService.createTicketUrl(Mockito.anyString())).thenReturn("jira://LCSET-1");

        // Create a test product
        Product testProduct = new Product("Test Product", new ProductFamily("Test Product Family"), "Test product",
                                          "P-EX-0011", new Date(), new Date(), 0, 0, 0, 0, "Test samples only", "None",
                                          true, Workflow.AGILENT_EXOME_EXPRESS, false, "agg type");
        testProduct.setAnalysisTypeKey("Resequencing");

        zimsIlluminaRunFactory = new ZimsIlluminaRunFactory(mockSampleDataFetcher, mockControlDao,
                new SequencingTemplateFactory(), productOrderDao, mockResearchProjectDao, crspPipelineUtils);
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
                new ResearchProject(101L, "Test Project", "ZimsIlluminaRunFactoryTest project", true,
                        REGULATORY_DESIGNATION);
        testResearchProject.setJiraTicketKey("TestRP-1");
        testResearchProject.setReferenceSequenceKey("Homo_sapiens_assembly19|1");

        // Create a test product order
        List<ProductOrderSample> pdoSamples = new ArrayList<>();
        for (String testSampleId : testSampleIds) {
            if (!testSampleId.equals(BSP_SM_ID_FOR_POSITIVE_CONTROL)) {
                pdoSamples.add(new ProductOrderSample(testSampleId));
            }
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
            if (isPositiveControl(sampleId)) {
                mapSampleIdToDto.put(sampleId, POSITIVE_CONTROL_SAMPLE_DTO);
            } else {
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
                put(BSPSampleSearchColumn.SAMPLE_TYPE, "Primary");
                put(BSPSampleSearchColumn.RACE, "N/A");
                put(BSPSampleSearchColumn.ETHNICITY, "unknown");
                put(BSPSampleSearchColumn.RACKSCAN_MISMATCH, "false");
                put(BSPSampleSearchColumn.RIN, "8.4");
            }};
            mapSampleIdToDto.put(sampleId, new BspSampleData(dataMap));
            }
        }

        mapKeyToProductOrder.clear();
        mapKeyToProductOrder.put(PRODUCT_ORDER_KEY, testProductOrder);

        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<>();
        dataMap.put(BSPSampleSearchColumn.ROOT_SAMPLE, "TestSM-0");
        dataMap.put(BSPSampleSearchColumn.SAMPLE_ID, "TestSM-1");
        Map<String, SampleData> bspSampleDataMap = new HashMap<>();
        bspSampleDataMap.put("TestSM-1", new BspSampleData(dataMap));
        Mockito.when(mockSampleDataFetcher.fetchSampleData(argThat(Matchers.contains("TestSM-1"))))
                .thenReturn(bspSampleDataMap);

        // Create an LCSET lab batch
        String sourceTubeBarcode = "testTube";
        testTube = new BarcodedTube(sourceTubeBarcode);
        testTube.addSample(new MercurySample("TestSM-1", MercurySample.MetadataSource.BSP));
        BucketEntry bucketEntry = new BucketEntry(testTube, testProductOrder, BucketEntry.BucketEntryType.PDO_ENTRY);
        testTube.addBucketEntry(bucketEntry);
        JiraTicket lcSetTicket = new JiraTicket(mockJiraService, labBatchName);
        LabBatch lcSetBatch = new LabBatch(labBatchName, Collections.<LabVessel>singleton(testTube),
                LabBatch.LabBatchType.WORKFLOW);
        lcSetBatch.setJiraTicket(lcSetTicket);
        lcSetBatch.setWorkflowName("Exome Express");
        lcSetBatch.addBucketEntry(bucketEntry);
        bucketEntry.setLabBatch(lcSetBatch);

        // Record some events for the sample
        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        // Normalized Catch Registration
        String catchTubeBarcode = "catchTube";
        final String catchRackBarcode = "CatchRack";
        PlateTransferEventType catchTransferJaxb = bettaLimsMessageFactory.buildRackToRack(
                LabEventType.NORMALIZED_CATCH_REGISTRATION.getName(),
                "SourceRack", Collections.singletonList(sourceTubeBarcode),
                catchRackBarcode, Collections.singletonList(catchTubeBarcode));
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(sourceTubeBarcode, testTube);
        LabEvent catchTransfer = labEventFactory.buildFromBettaLims(catchTransferJaxb, mapBarcodeToVessel);
        final TubeFormation catchTubeFormation = (TubeFormation) catchTransfer.getTargetLabVessels().iterator().next();
        BarcodedTube catchTube = catchTubeFormation. getContainerRole().getVesselAtPosition(VesselPosition.A01);

        // Index addition
        StaticPlate indexPlate = LabEventTest.buildIndexPlate(null, null,
                Collections.singletonList(MolecularIndexingScheme.IndexPosition.ILLUMINA_P5),
                Collections.singletonList("indexPlate")).get(0);
        mapBarcodeToVessel.put("indexPlate", indexPlate);
        PlateTransferEventType indexAdditionJaxb = bettaLimsMessageFactory
                .buildPlateToRack(LabEventType.INDEX_P5_POND_ENRICHMENT.getName(), "indexPlate", catchRackBarcode,
                        Collections.singletonList(catchTubeBarcode));
        LabEvent indexAddition = labEventFactory.buildFromBettaLims(indexAdditionJaxb, mapBarcodeToVessel);

        // Strip tube B transfer
        List<BettaLimsMessageTestFactory.CherryPick> cherryPicks = new ArrayList<>();
        String[] stripTubeWells = {"A01", "B01", "C01", "D01", "E01", "F01", "G01", "H01"};
        for (int i = 0; i < 8; i++) {
            cherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(catchRackBarcode, "A01", "testStripTubeHolder", stripTubeWells[i]));
        }
        PlateCherryPickEvent stripTubeBTransferEvent = bettaLimsMessageFactory.buildCherryPickToStripTube(
                LabEventType.STRIP_TUBE_B_TRANSFER.getName(), Collections.singletonList(catchRackBarcode),
                Collections.singletonList(Collections.singletonList(catchTubeBarcode)),
                "testStripTubeHolder", Collections.singletonList("testStripTube"), cherryPicks);
        Map<String, BarcodedTube> mapBarcodeToSourceTube = new HashMap<>();
        mapBarcodeToSourceTube.put(catchTubeBarcode, catchTube);
        LabEvent stripTubeBTransfer = labEventFactory.buildCherryPickRackToStripTubeDbFree(stripTubeBTransferEvent,
                new HashMap<String, TubeFormation>() {{
                    put(catchRackBarcode, catchTubeFormation);
                }},
                mapBarcodeToSourceTube, null, new HashMap<String, StripTube>(), new HashMap<String, RackOfTubes>());

        flowcell = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.HiSeqFlowcell, "Flowcell1");
        // Flowcell transfer
        PlateTransferEventType flowcellTransferEvent = bettaLimsMessageFactory.buildStripTubeToFlowcell(
                LabEventType.FLOWCELL_TRANSFER.getName(), "testStripTube", "Flowcell1");
        StripTube stripTube = (StripTube) getOnly(stripTubeBTransfer.getTargetLabVessels());
        labEventFactory.buildFromBettaLimsPlateToPlateDbFree(flowcellTransferEvent, stripTube, flowcell);

        setupPositiveControlMap();
    }

    private <T> T getOnly(Collection<T> items) {
        assertThat(items.size(), is(1));
        return items.iterator().next();
    }

    /** Creates multiple dtos, one for each combination of testSampleId and testLabBatchType. */
    private List<ZimsIlluminaRunFactory.SampleInstanceDto> createSampleInstanceDto(
            boolean areCrspSamples, LabBatch.LabBatchType... testLabBatchTypes) {
        List<ZimsIlluminaRunFactory.SampleInstanceDto> sampleInstanceDtoList = new ArrayList<>();
        String metadataSourceForPipeline = MercurySample.BSP_METADATA_SOURCE;
        if (areCrspSamples) {
            metadataSourceForPipeline = MercurySample.MERCURY_METADATA_SOURCE;
        }

        for (int sampleIdx = 0; sampleIdx < testSampleIds.size(); ++sampleIdx) {
            String sourceTubeBarcode = "testTube" + sampleIdx;
            BarcodedTube testTube = new BarcodedTube(sourceTubeBarcode);

            String sampleId = testSampleIds.get(sampleIdx);
            MercurySample mercurySample = new MercurySample(sampleId,
                    MercurySample.MetadataSource.BSP);
            testTube.addSample(mercurySample);

            if (!isPositiveControl(sampleId)) {
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
                            sampleId, getPdoKeyForSample(sampleId), null, null, mercurySample.getSampleKey(),
                            areCrspSamples,metadataSourceForPipeline));
                }
            }
            else {
                SampleInstanceV2 instance = new SampleInstanceV2(testTube);
                instance.addReagent(reagents.get(sampleIdx));
                sampleInstanceDtoList.add(new ZimsIlluminaRunFactory.SampleInstanceDto(LANE_NUMBER, testTube, instance,
                        sampleId, getPdoKeyForSample(sampleId), null, null, mercurySample.getSampleKey(),
                        areCrspSamples,metadataSourceForPipeline));
            }



        }
        return sampleInstanceDtoList;
    }

    private String getPdoKeyForSample(String sampleId) {
        String pdoKey = PRODUCT_ORDER_KEY;
        if (isPositiveControl(sampleId)) {
            pdoKey = null;
        }
        return pdoKey;
    }

    private boolean isPositiveControl(String sampleId) {
        return BSP_SM_ID_FOR_POSITIVE_CONTROL.equals(sampleId);
    }


    @Test(groups = DATABASE_FREE)
    public void testGetLibraryTwoLcSetBatches() {
        List<ZimsIlluminaRunFactory.SampleInstanceDto> instanceDtoList =
                createSampleInstanceDto(false, LabBatch.LabBatchType.WORKFLOW, LabBatch.LabBatchType.WORKFLOW,
                                        LabBatch.LabBatchType.BSP);
        try {
            List<LibraryBean> zimsIlluminaRuns = zimsIlluminaRunFactory.makeLibraryBeans(
                    instanceDtoList, mapSampleIdToDto, mapKeyToProductOrder, Collections.EMPTY_MAP,
                    crspPositiveControlsResearchProject);

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
        assertThat(zimsIlluminaRun.getFlowcellBarcode(), equalTo("Flowcell1"));
        assertThat(zimsIlluminaRun.getRunDateString(), equalTo("01/22/2013 16:11"));
        assertThat(zimsIlluminaRun.getSequencerModel(), equalTo(
                IlluminaFlowcell.FlowcellType.HiSeqFlowcell.getSequencerModel()));

        assertThat(zimsIlluminaRun.getLanes().size(), equalTo(8));
        for (ZimsIlluminaChamber lane : zimsIlluminaRun.getLanes()) {
            assertThat(lane.getLibraries().size(), is(1));
            assertThat(lane.getSequencedLibrary(), equalTo(denatureTube.getLabel()));
            assertThat(lane.getLibraries().size(), equalTo(1));
            LibraryBean libraryBean = lane.getLibraries().iterator().next();
            assertThat(libraryBean.getRootSample(), equalTo("TestSM-0"));
            assertThat(libraryBean.getSampleId(), equalTo("TestSM-1"));
            assertThat(libraryBean.getProductOrderSample(), equalTo("TestSM-1"));
        }
        assertThat(zimsIlluminaRun.getSequencerModel(), equalTo("Illumina HiSeq 2000"));
        assertThat(zimsIlluminaRun.getRunFolder(), equalTo("/root/path/to/run/" + testRunDirectory));
    }

    @Test(groups = DATABASE_FREE)
    public void testMakeLibraryBean() {
        final int numberOfBatchTypeParameters = 2;
        List<ZimsIlluminaRunFactory.SampleInstanceDto> instanceDtoList =
                createSampleInstanceDto(false, LabBatch.LabBatchType.WORKFLOW, LabBatch.LabBatchType.BSP);
        int numSampleInstancesExpected = (testSampleIds.size() * numberOfBatchTypeParameters) - 1; // subtract out the control since it's not in a batch
        assertThat(instanceDtoList.size(), equalTo(numSampleInstancesExpected));

        List<LibraryBean>  zimsIlluminaRuns = zimsIlluminaRunFactory.makeLibraryBeans(
                instanceDtoList, mapSampleIdToDto, mapKeyToProductOrder, Collections.EMPTY_MAP,
                crspPositiveControlsResearchProject);

        int numPositiveControls = 0;
        boolean foundNonControlSamples = false;
        for (LibraryBean libraryBean : zimsIlluminaRuns) {
            String sampleId = libraryBean.getSampleId();
            int testSampleIdsIndex = findSampleIndex(sampleId);
            Assert.assertTrue(testSampleIdsIndex >= 0, "Unknown sampleId " + sampleId);
            String reagentIndexName = reagents.get(testSampleIdsIndex).getMolecularIndexingScheme().getName();
            Assert.assertNotNull(reagentIndexName, "no index for " + sampleId);
            String tubeBarcode = "testTube" + testSampleIdsIndex;

            if (!isPositiveControl(sampleId)) {
                foundNonControlSamples = true;
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
                assertThat("The pipeline API expects enum names for regulatory designations",libraryBean.getRegulatoryDesignation(),equalTo(REGULATORY_DESIGNATION.name()));
                assertThat(libraryBean.getMetadataSource(),equalTo(MercurySample.BSP_METADATA_SOURCE));
                assertThat(libraryBean.getSampleType(), equalTo("Tumor"));
            }
            else {
                numPositiveControls++;
            }
        }
        Assert.assertEquals(numPositiveControls,1);
        Assert.assertTrue(foundNonControlSamples);
    }

    private void setupPositiveControlMap() {
        controlMap = new HashMap<>();
        for (String sampleId : testSampleIds) {
            if (sampleId.equals(BSP_SM_ID_FOR_POSITIVE_CONTROL)) {
                SampleData sampleData = mapSampleIdToDto.get(sampleId);
                String collaboratorParticipantId = sampleData.getCollaboratorParticipantId();
                controlMap.put(collaboratorParticipantId,new Control(collaboratorParticipantId, Control.ControlType.POSITIVE));
            }
        }
    }

    @Test(groups = DATABASE_FREE)
    public void testCrspPositiveControls() {
        testProductOrder.getResearchProject().setRegulatoryDesignation(ResearchProject.RegulatoryDesignation.GENERAL_CLIA_CAP);

        List<ZimsIlluminaRunFactory.SampleInstanceDto> instanceDtoList =
                createSampleInstanceDto(true, LabBatch.LabBatchType.WORKFLOW);

        List<LibraryBean>  libraryBeans = zimsIlluminaRunFactory.makeLibraryBeans(
                instanceDtoList, mapSampleIdToDto, mapKeyToProductOrder, controlMap,
                crspPositiveControlsResearchProject);

        boolean hasPositiveControl = false;
        for (LibraryBean libraryBean : libraryBeans) {
            Assert.assertEquals(libraryBean.getRootSample(),libraryBean.getSampleId());
            Assert.assertEquals(libraryBean.getMetadataSource(), MercurySample.MERCURY_METADATA_SOURCE);
            Assert.assertEquals(libraryBean.getReferenceSequence(), "Homo_sapiens_assembly19");
            Assert.assertEquals(libraryBean.getReferenceSequenceVersion(), "1");
            Assert.assertEquals(libraryBean.getAnalysisType(), "HybridSelection.Resequencing");
            Assert.assertEquals(libraryBean.getDataType(), "agg type");
            Assert.assertTrue(libraryBean.doAggregation());

            if (Boolean.TRUE.equals(libraryBean.isPositiveControl())) {
                hasPositiveControl = true;
                Assert.assertTrue(libraryBean.getLsid().startsWith("org.broadinstitute:crsp:"));
                Assert.assertEquals(libraryBean.getCollaboratorParticipantId(),libraryBean.getCollaboratorSampleId());
                Assert.assertEquals(libraryBean.getProductPartNumber(), "P-EX-0011");
            }
        }
        Assert.assertTrue(hasPositiveControl);
    }

    /**
     * Starting with multiple SampleInstanceDtos having the same molecular index barcodes and the same sampleId,
     * tests that only one consolidated library bean is created.  See FCT-18466.
     */
    @Test(groups = DATABASE_FREE)
    public void testSplitRejoinWorkflowConsolidation() {
        List<ZimsIlluminaRunFactory.SampleInstanceDto> dtoList1 =
                createSampleInstanceDto(false, LabBatch.LabBatchType.WORKFLOW);
        // Hacks the dtos so all have the same molecular index name.
        for (ZimsIlluminaRunFactory.SampleInstanceDto dto : dtoList1) {
            dto.getSampleInstance().getReagents().clear();
            dto.getSampleInstance().addReagent(reagents.get(0));
        }

        // First tests that no consolidation happens, because the sample names are different.
        List<LibraryBean> unconsolidatedBeans1 = zimsIlluminaRunFactory.makeLibraryBeans(dtoList1,
                mapSampleIdToDto, mapKeyToProductOrder, Collections.EMPTY_MAP, crspPositiveControlsResearchProject);
        assertThat(unconsolidatedBeans1.size(), equalTo(testSampleIds.size()));


        // Makes dtos with two having the same sampleId (but different molecular index names).
        testSampleIds.remove(1);
        testSampleIds.add(testSampleIds.get(0));
        List<ZimsIlluminaRunFactory.SampleInstanceDto> dtoList2 =
                createSampleInstanceDto(false, LabBatch.LabBatchType.WORKFLOW);

        // Second tests no consolidation of same sample, different molecular name dtos.
        List<LibraryBean> unconsolidatedBeans2 = zimsIlluminaRunFactory.makeLibraryBeans(dtoList2,
                mapSampleIdToDto, mapKeyToProductOrder, Collections.EMPTY_MAP, crspPositiveControlsResearchProject);
        assertThat(unconsolidatedBeans2.size(), equalTo(testSampleIds.size()));

        // Using the dto list with first two dtos having same sampleId, make all the molecular index names the same.
        for (ZimsIlluminaRunFactory.SampleInstanceDto dto : dtoList2) {
            dto.getSampleInstance().getReagents().clear();
            dto.getSampleInstance().addReagent(reagents.get(0));
        }

        // Tests consolidation of the first two dtos.
        List<LibraryBean> consolidatedBeans = zimsIlluminaRunFactory.makeLibraryBeans(dtoList2,
                mapSampleIdToDto, mapKeyToProductOrder, Collections.EMPTY_MAP, crspPositiveControlsResearchProject);
        assertThat(consolidatedBeans.size(), equalTo(testSampleIds.size() - 1));
    }

    @Test
    public void testControlPartNumber() {
        Assert.assertEquals(zimsIlluminaRunFactory.getControlProductPartNumber(
                Collections.singleton(ZimsIlluminaRunFactory.AGILENT_SOMATIC_PART_NUMBER)),
                ZimsIlluminaRunFactory.AGILENT_GERMLINE_PART_NUMBER);

        Assert.assertEquals(zimsIlluminaRunFactory.getControlProductPartNumber(
                new HashSet<String>() {{
                    add(ZimsIlluminaRunFactory.AGILENT_SOMATIC_PART_NUMBER);
                    add(ZimsIlluminaRunFactory.AGILENT_GERMLINE_PART_NUMBER);
                }}),
                ZimsIlluminaRunFactory.AGILENT_GERMLINE_PART_NUMBER);

        Assert.assertEquals(zimsIlluminaRunFactory.getControlProductPartNumber(
                new HashSet<String>() {{
                    add(ZimsIlluminaRunFactory.AGILENT_SOMATIC_PART_NUMBER);
                    add(ZimsIlluminaRunFactory.ICE_GERMLINE_PART_NUMBER);
                }}),
                null);
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
