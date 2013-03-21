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
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.*;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.test.BettaLimsMessageFactory;
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

    private ProductOrderDao mockProductOrderDao;
    private BSPSampleDataFetcher mockBSPSampleDataFetcher;

    @BeforeMethod(groups = DATABASE_FREE)
    public void setUp() {
        mockProductOrderDao = mock(ProductOrderDao.class);
        mockBSPSampleDataFetcher = mock(BSPSampleDataFetcher.class);
        JiraService mockJiraService = mock(JiraService.class);
        when(mockJiraService.createTicketUrl(anyString())).thenReturn("jira://LCSET-1");

        // Create a test product
        Product testProduct = new Product("Test Product", new ProductFamily("Test Product Family"), "Test product",
                "P-TEST-1", new Date(), new Date(), 0, 0, 0, 0, "Test samples only", "None", true,
                "Test Workflow", false);

        zimsIlluminaRunFactory = new ZimsIlluminaRunFactory(mockProductOrderDao, mockBSPSampleDataFetcher);
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
        ProductOrder testProductOrder = new ProductOrder(101L, "Test Order", Collections.singletonList(
                new ProductOrderSample("TestSM-1")), "Quote-1", testProduct, testResearchProject);
        testProductOrder.setJiraTicketKey("TestPDO-1");
        when(mockProductOrderDao.findByBusinessKey("TestPDO-1")).thenReturn(testProductOrder);

        // Create an LCSET lab batch
        final String sourceTubeBarcode = "testTube";
        testTube = new TwoDBarcodedTube(sourceTubeBarcode);
        testTube.addSample(new MercurySample("TestPDO-1", "TestSM-1"));
        JiraTicket lcSetTicket = new JiraTicket(mockJiraService, "LCSET-1");
        LabBatch lcSetBatch = new LabBatch("LCSET-1 batch", Collections.<LabVessel>singleton(testTube),
                LabBatch.LabBatchType.WORKFLOW);
        lcSetBatch.setJiraTicket(lcSetTicket);

        // Record some events for the sample
        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();

        // Normalized Catch Registration
        String catchTubeBarcode = "catchTube";
        final String catchRackBarcode = "CatchRack";
        PlateTransferEventType catchTransferJaxb = bettaLimsMessageFactory.buildRackToRack(
                LabEventType.NORMALIZED_CATCH_REGISTRATION.getName(),
                "SourceRack", Collections.singletonList(sourceTubeBarcode),
                catchRackBarcode, Collections.singletonList(catchTubeBarcode));
        LabEvent catchTransfer = labEventFactory.buildFromBettaLimsRackToRackDbFree(catchTransferJaxb,
                new HashMap<String, TwoDBarcodedTube>() {{
                    put(sourceTubeBarcode, testTube);
                }},
                null, new HashMap<String, TwoDBarcodedTube>(), null);
        final TubeFormation catchTubeFormation = (TubeFormation) catchTransfer.getTargetLabVessels().iterator().next();
        TwoDBarcodedTube catchTube = catchTubeFormation. getContainerRole().getVesselAtPosition(VesselPosition.A01);

        // Strip tube B transfer
        List<BettaLimsMessageFactory.CherryPick> cherryPicks = new ArrayList<BettaLimsMessageFactory.CherryPick>();
        String[] stripTubeWells = {"A01", "B01", "C01", "D01", "E01", "F01", "G01", "H01"};
        for (int i = 0; i < 8; i++) {
            cherryPicks.add(new BettaLimsMessageFactory.CherryPick(catchRackBarcode, "A01", "testStripTubeHolder", stripTubeWells[i]));
        }
        PlateCherryPickEvent stripTubeBTransferEvent = bettaLimsMessageFactory.buildCherryPickToStripTube(
                LabEventType.STRIP_TUBE_B_TRANSFER.getName(), Collections.singletonList(catchRackBarcode),
                Collections.singletonList(Collections.singletonList(catchTubeBarcode)),
                "testStripTubeHolder", Collections.singletonList("testStripTube"), cherryPicks);
        Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube = new HashMap<String, TwoDBarcodedTube>();
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
        IlluminaSequencingRun sequencingRun =
                new IlluminaSequencingRun(flowcell, testRunDirectory, "Run-123", "IlluminaRunServiceImplTest", 101L, true,
                        runDate,
                        null,
                                                 "/root/path/to/run/" + testRunDirectory);
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
        BSPSampleDTO sampleDTO = new BSPSampleDTO("BspContainer", "Stock1", "RootSample", "Aliquot1", "Spencer", "Hamster",
                "first_sample", "collection1", "7", "9", "ZimsIlluminaRunFactoryTest.testMakeLibraryBean.sampleDTO",
                "participant1", "Test Material", "42", "Test Sample", "Test failure", "M", "Stock Type", "fingerprint",
                "sample1", "ZimsIlluminaRunFactoryTest", "N/A", "unknown", "false");

        Map<String, BSPSampleDTO> mapSampleIdToDto = new HashMap<String, BSPSampleDTO>();
        mapSampleIdToDto.put("TestSM-1", sampleDTO);
        LibraryBean libraryBean = zimsIlluminaRunFactory.makeLibraryBeans(testTube, mapSampleIdToDto).get(0);
        verify(mockProductOrderDao).findByBusinessKey("TestPDO-1");
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
