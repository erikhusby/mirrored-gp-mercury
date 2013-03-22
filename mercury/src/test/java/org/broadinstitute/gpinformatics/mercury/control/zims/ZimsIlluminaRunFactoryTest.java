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
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.*;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.infrastructure.test.BettaLimsMessageFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author breilly
 */
public class ZimsIlluminaRunFactoryTest {

    private ZimsIlluminaRunFactory zimsIlluminaRunFactory;
    private LabEventFactory labEventFactory;
    private IlluminaFlowcell flowcell;
    private TwoDBarcodedTube testTube;

    private ProductOrderDao mockProductOrderDao;
    private BSPSampleDataFetcher mockBSPSampleDataFetcher;
    private JiraService mockJiraService;

    @BeforeMethod(groups = DATABASE_FREE)
    public void setUp() throws Exception {
        mockProductOrderDao = mock(ProductOrderDao.class);
        mockBSPSampleDataFetcher = mock(BSPSampleDataFetcher.class);
        mockJiraService = mock(JiraService.class);
        when(mockJiraService.createTicketUrl(anyString())).thenReturn("jira://LCSET-1");

        // Create a simple test workflow
        ProductWorkflowDef testWorkflow = new ProductWorkflowDef("Test Workflow");
        ProductWorkflowDefVersion productWorkflowDefVersion = new ProductWorkflowDefVersion("1", new Date());
        testWorkflow.addProductWorkflowDefVersion(productWorkflowDefVersion);
        WorkflowProcessDef testProcess = new WorkflowProcessDef("Test Process");
        WorkflowProcessDefVersion testProcess1 = new WorkflowProcessDefVersion("1", new Date());
        testProcess.addWorkflowProcessDefVersion(testProcess1);
        WorkflowStepDef step1 = new WorkflowStepDef("Step 1");
        step1.addLabEvent(A_BASE);
        testProcess1.addStep(step1);
        WorkflowStepDef step2 = new WorkflowStepDef("Step 2"); // TODO: signify that this step is important to picard
        step2.addLabEvent(INDEXED_ADAPTER_LIGATION);
        testProcess1.addStep(step2);
        WorkflowStepDef step3 = new WorkflowStepDef("Step 3");
        step3.addLabEvent(POOLING_TRANSFER);
        testProcess1.addStep(step3);
        productWorkflowDefVersion.addWorkflowProcessDef(testProcess);

        // Create a test product
        Product testProduct = new Product("Test Product", new ProductFamily("Test Product Family"), "Test product",
                "P-TEST-1", new Date(), new Date(), 0, 0, 0, 0, "Test samples only", "None", true,
                "Test Workflow", false);

        zimsIlluminaRunFactory = new ZimsIlluminaRunFactory(mockProductOrderDao, mockBSPSampleDataFetcher);
        labEventFactory = new LabEventFactory();
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
        ProductOrder testProductOrder = new ProductOrder(101L, "Test Order", Collections.singletonList(new ProductOrderSample("TestSM-1")), "Quote-1", testProduct, testResearchProject);
        testProductOrder.setJiraTicketKey("TestPDO-1");
        when(mockProductOrderDao.findByBusinessKey("TestPDO-1")).thenReturn(testProductOrder);

        // Create an LCSET lab batch
        testTube = new TwoDBarcodedTube("testTube");
        testTube.addSample(new MercurySample("TestPDO-1", "TestSM-1"));
        JiraTicket lcSetTicket = new JiraTicket(mockJiraService, "LCSET-1");
        LabBatch lcSetBatch = new LabBatch("LCSET-1 batch", Collections.<LabVessel>singleton(testTube),
                LabBatch.LabBatchType.WORKFLOW);
        lcSetBatch.setJiraTicket(lcSetTicket);

        // Record some events for the sample
        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        List<BettaLimsMessageFactory.CherryPick> cherryPicks = new ArrayList<BettaLimsMessageFactory.CherryPick>();
        String stripTubeWells[] = new String[]{"A01", "B01", "C01", "D01", "E01", "F01", "G01", "H01"};
        for (int i = 0; i < 8; i++) {
            cherryPicks.add(new BettaLimsMessageFactory.CherryPick("testRack", "A01", "testStripTubeHolder", stripTubeWells[i]));
        }
        PlateCherryPickEvent stripTubeBTransferEvent = bettaLimsMessageFactory.buildCherryPickToStripTube("StripTubeBTransfer", Collections.singletonList("testRack"), Collections.singletonList(Collections.singletonList("testTube")), "testStripTubeHolder", Collections.singletonList("testStripTube"), cherryPicks);
        HashMap<String, TwoDBarcodedTube> mapBarcodeToSourceTube = new HashMap<String, TwoDBarcodedTube>();
        mapBarcodeToSourceTube.put("testTube", testTube);
        LabEvent stripTubeBTransfer = labEventFactory.buildCherryPickRackToStripTubeDbFree(stripTubeBTransferEvent, new HashMap<String, TubeFormation>(), mapBarcodeToSourceTube, null, new HashMap<String, StripTube>(), new HashMap<String, RackOfTubes>());

        flowcell = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.HiSeqFlowcell, "testFlowcell");
        PlateTransferEventType flowcellTransferEvent = bettaLimsMessageFactory.buildStripTubeToFlowcell("FlowcellTransfer", "testStripTube", "testFlowcell");
        StripTube stripTube = (StripTube) getOnly(stripTubeBTransfer.getTargetLabVessels());
        labEventFactory.buildFromBettaLimsPlateToPlateDbFree(flowcellTransferEvent, stripTube, flowcell);
    }

    private <T> T getOnly(Collection<T> items) {
        assertThat(items.size(), is(1));
        return items.iterator().next();
    }

    @Test(groups = DATABASE_FREE)
    public void testMakeZimsIlluminaRun() throws Exception {
        Date runDate = new Date(1358889107084L);
        final String testRunDirectory = "TestRun";
        IlluminaSequencingRun sequencingRun =
                new IlluminaSequencingRun(flowcell, testRunDirectory, "Run-123", "IlluminaRunServiceImplTest", 101L, true,
                        runDate,
                        null,
                                                 "/root/path/to/run/" + testRunDirectory);
        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(sequencingRun);
//        LibraryBeanFactory libraryBeanFactory = new LibraryBeanFactory();
//        ZimsIlluminaRun zimsIlluminaRun = libraryBeanFactory.buildLibraries(sequencingRun);

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
            if (lane.getName().equals("1")) {
                assertThat(lane.getLibraries().size(), is(1));
            } else {
                assertThat(lane.getLibraries().size(), is(0));
            }
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
        when(mockBSPSampleDataFetcher.fetchSingleSampleFromBSP("TestSM-1")).thenReturn(sampleDTO);

        LibraryBean libraryBean = zimsIlluminaRunFactory.makeLibraryBean(testTube);
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
