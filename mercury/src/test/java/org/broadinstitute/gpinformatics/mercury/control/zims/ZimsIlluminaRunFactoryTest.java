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
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.*;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author breilly
 */
public class ZimsIlluminaRunFactoryTest {

    private ZimsIlluminaRunFactory zimsIlluminaRunFactory;
    private IlluminaFlowcell flowcell;
    private TwoDBarcodedTube testTube;

    private BSPSampleDataFetcher mockBSPSampleDataFetcher;
    private AthenaClientService mockAthenaClientService;
    private ControlDao mockControlDao;
    private ProductOrder testProductOrder;

    @BeforeMethod(groups = DATABASE_FREE)
    public void setUp() {
        mockBSPSampleDataFetcher = mock(BSPSampleDataFetcher.class);
        mockAthenaClientService = mock(AthenaClientServiceImpl.class);
        mockControlDao = mock(ControlDao.class);

        JiraService mockJiraService = mock(JiraService.class);
        when(mockJiraService.createTicketUrl(anyString())).thenReturn("jira://LCSET-1");

        // Create a test product
        Product testProduct = new Product("Test Product", new ProductFamily("Test Product Family"), "Test product",
                "P-TEST-1", new Date(), new Date(), 0, 0, 0, 0, "Test samples only", "None", true,
                "Test Workflow", false, "agg type");

        zimsIlluminaRunFactory = new ZimsIlluminaRunFactory(mockBSPSampleDataFetcher, mockAthenaClientService,
                mockControlDao);
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setLabEventRefDataFetcher(new LabEventFactory.LabEventRefDataFetcher() {
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
        ResearchProject testResearchProject = new ResearchProject(101L, "Test Project", "ZimsIlluminaRunFactoryTest project", true);
        testResearchProject.setJiraTicketKey("TestRP-1");

        // Create a test product order
        testProductOrder = new ProductOrder(101L, "Test Order", Collections.singletonList(
                new ProductOrderSample("TestSM-1")), "Quote-1", testProduct, testResearchProject);
        testProductOrder.setJiraTicketKey("TestPDO-1");
        when(mockAthenaClientService.retrieveProductOrderDetails("TestPDO-1")).thenReturn(testProductOrder);

        // Create an LCSET lab batch
        String sourceTubeBarcode = "testTube";
        testTube = new TwoDBarcodedTube(sourceTubeBarcode);
        testTube.addSample(new MercurySample("TestSM-1"));
        testTube.addBucketEntry(new BucketEntry(testTube, "TestPDO-1"));
        JiraTicket lcSetTicket = new JiraTicket(mockJiraService, "LCSET-1");
        LabBatch lcSetBatch = new LabBatch("LCSET-1 batch", Collections.<LabVessel>singleton(testTube),
                LabBatch.LabBatchType.WORKFLOW);
        lcSetBatch.setJiraTicket(lcSetTicket);

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
        TwoDBarcodedTube catchTube = catchTubeFormation. getContainerRole().getVesselAtPosition(VesselPosition.A01);

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
        Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube = new HashMap<>();
        mapBarcodeToSourceTube.put(catchTubeBarcode, catchTube);
        LabEvent stripTubeBTransfer = labEventFactory.buildCherryPickRackToStripTubeDbFree(stripTubeBTransferEvent,
                new HashMap<String, TubeFormation>() {{
                    put(catchRackBarcode, catchTubeFormation);
                }},
                mapBarcodeToSourceTube, null, new HashMap<String, StripTube>(), new HashMap<String, RackOfTubes>());

        // Flowcell transfer
        flowcell = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.HiSeqFlowcell, "testFlowcell");
        PlateTransferEventType flowcellTransferEvent = bettaLimsMessageFactory.buildStripTubeToFlowcell(
                LabEventType.FLOWCELL_TRANSFER.getName(), "testStripTube", "testFlowcell");
        StripTube stripTube = (StripTube) getOnly(stripTubeBTransfer.getTargetLabVessels());
        labEventFactory.buildFromBettaLimsPlateToPlateDbFree(flowcellTransferEvent, stripTube, flowcell);
    }

    private <T> T getOnly(Collection<T> items) {
        assertThat(items.size(), is(1));
        return items.iterator().next();
    }

    @Test(groups = DATABASE_FREE)
    public void testMakeZimsIlluminaRun() {
        Date runDate = new Date(1358889107084L);
        String testRunDirectory = "TestRun";
        IlluminaSequencingRun sequencingRun = new IlluminaSequencingRun(flowcell, testRunDirectory, "Run-123",
                "IlluminaRunServiceImplTest", 101L, true, runDate, null, "/root/path/to/run/" + testRunDirectory);
        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(sequencingRun);

        assertThat(zimsIlluminaRun.getError(), nullValue());
        assertThat(zimsIlluminaRun.getName(), equalTo("TestRun"));
        assertThat(zimsIlluminaRun.getBarcode(), equalTo("Run-123"));
        assertThat(zimsIlluminaRun.getSequencer(), equalTo("IlluminaRunServiceImplTest"));
        assertThat(zimsIlluminaRun.getFlowcellBarcode(), equalTo("testFlowcell"));
        assertThat(zimsIlluminaRun.getRunDateString(), equalTo("01/22/2013 16:11"));
//        assertThat(zimsIlluminaRun.getPairedRun(), is(true)); // TODO SGM will pull from Workflow
//        assertThat(zimsIlluminaRun.getSequencerModel(), equalTo("HiSeq")); // TODO  SGM Will pull from workflow
//        assertThat(zimsIlluminaRun.getLanes().size(), equalTo(8)); // TODO SGM WIll pull from workflow

        for (ZimsIlluminaChamber lane : zimsIlluminaRun.getLanes()) {
            assertThat(lane.getLibraries().size(), is(1));
        }
    }

    @Test(groups = DATABASE_FREE)
    public void testMakeLibraryBean() {

        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.CONTAINER_ID, "BspContainer");
            put(BSPSampleSearchColumn.STOCK_SAMPLE, "Stock1");
            put(BSPSampleSearchColumn.ROOT_SAMPLE, "RootSample");
            put(BSPSampleSearchColumn.SAMPLE_ID, "Aliquot1");
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
            put(BSPSampleSearchColumn.SAMPLE_ID, "sample1");
            put(BSPSampleSearchColumn.SAMPLE_TYPE, "ZimsIlluminaRunFactoryTest");
            put(BSPSampleSearchColumn.RACE, "N/A");
            put(BSPSampleSearchColumn.ETHNICITY, "unknown");
            put(BSPSampleSearchColumn.RACKSCAN_MISMATCH ,"false");
            put(BSPSampleSearchColumn.RIN, "8.4");
        }};

        BSPSampleDTO sampleDTO = new BSPSampleDTO(dataMap);

        Map<String, BSPSampleDTO> mapSampleIdToDto = new HashMap<>();
        mapSampleIdToDto.put("TestSM-1", sampleDTO);
        Map<String, ProductOrder> mapKeyToProductOrder = new HashMap<>();
        mapKeyToProductOrder.put("TestPDO-1", testProductOrder);
        Map<String, Control> mapNameToControl = new HashMap<>();
        LibraryBean libraryBean = zimsIlluminaRunFactory.makeLibraryBeans(testTube, mapSampleIdToDto,
                mapKeyToProductOrder, mapNameToControl).get(0);
        assertThat(libraryBean.getLibrary(), equalTo("testTube")); // TODO: expand with full definition of generated library name
        assertThat(libraryBean.getProject(), equalTo("TestRP-1"));
//        assertThat(libraryBean.getMolecularIndexingScheme(), equalTo("???")); // TODO
        assertThat(libraryBean.getSpecies(), equalTo("Hamster"));
        assertThat(libraryBean.getLsid(), equalTo("ZimsIlluminaRunFactoryTest.testMakeLibraryBean.sampleDTO"));
        assertThat(libraryBean.getParticipantId(), equalTo("Spencer"));
        assertThat(libraryBean.getLcSet(), equalTo("LCSET-1")); // TODO
        assertThat(libraryBean.getProductOrderTitle(), equalTo("Test Order"));
        assertThat(libraryBean.getProductOrderKey(), equalTo("TestPDO-1"));
        assertThat(libraryBean.getResearchProjectName(), equalTo("Test Project"));
        assertThat(libraryBean.getResearchProjectId(), equalTo("TestRP-1"));
        assertThat(libraryBean.getProduct(), equalTo("Test Product"));
        assertThat(libraryBean.getDataType(), equalTo("agg type"));
        assertThat(libraryBean.getProductFamily(), equalTo("Test Product Family"));
        assertThat(libraryBean.getRootSample(), equalTo("RootSample"));
        assertThat(libraryBean.getSampleId(), equalTo("sample1"));
        assertThat(libraryBean.getGender(), equalTo("M"));
        assertThat(libraryBean.getCollection(), equalTo("collection1"));
        assertThat(libraryBean.getPrimaryDisease(), equalTo("Test failure"));
        assertThat(libraryBean.getCollaboratorSampleId(), equalTo("first_sample"));
        assertThat(libraryBean.getCollaboratorParticipantId(), equalTo("participant1"));
        assertThat(libraryBean.getMaterialType(), equalTo("Test Material"));
        assertThat(libraryBean.getIsGssrSample(), equalTo(false));
        assertThat(libraryBean.getPopulation(), equalTo("unknown"));
        assertThat(libraryBean.getRace(), equalTo("N/A"));
    }
}
