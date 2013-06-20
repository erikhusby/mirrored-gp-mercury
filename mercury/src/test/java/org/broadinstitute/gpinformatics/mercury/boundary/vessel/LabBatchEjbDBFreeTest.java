package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * @author Scott Matthews
 *         Date: 12/7/12
 *         Time: 4:31 PM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class LabBatchEjbDBFreeTest {

    public static final String STUB_TEST_PDO_KEY = "PDO-999";

    private AthenaClientService athenaClientService;

    private LabBatchEjb labBatchEJB;

    private LabBatchDAO labBatchDAO;

    private LabVesselDao tubeDao;

    private LinkedHashMap<String, LabVessel> mapBarcodeToTube = new LinkedHashMap<String, LabVessel>();
    private String workflowName;
    private ArrayList<String> pdoNames;
    private String scottmat;
    private String testLCSetKey;
    private String testFCTKey;
    private JiraTicketDao mockJira;
    private Set<String> vesselSampleList;

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() throws Exception {
        testLCSetKey = "LCSet-tst932";
        testFCTKey = "FCT-tst932";

        vesselSampleList = new HashSet<String>(6);

        Collections.addAll(vesselSampleList, "SM-423", "SM-243", "SM-765", "SM-143", "SM-9243", "SM-118");

        // starting rack
        int sampleIndex = 1;
        for (String sampleName : vesselSampleList) {
            String barcode = "R" + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex;
            String bspStock = sampleName;
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(bspStock));
            bspAliquot.addBucketEntry(new BucketEntry(bspAliquot, STUB_TEST_PDO_KEY));
            mapBarcodeToTube.put(barcode, bspAliquot);
            sampleIndex++;
        }


        athenaClientService = AthenaClientProducer.stubInstance();
        labBatchEJB = new LabBatchEjb();
        labBatchEJB.setAthenaClientService(athenaClientService);
        labBatchEJB.setJiraService(JiraServiceProducer.stubInstance());


        tubeDao = EasyMock.createMock(LabVesselDao.class);
        EasyMock.expect(tubeDao.findByIdentifier(EasyMock.eq("SM-423"))).andReturn(mapBarcodeToTube.get("R111111"));
        EasyMock.expect(tubeDao.findByIdentifier(EasyMock.eq("SM-243"))).andReturn(mapBarcodeToTube.get("R222222"));
        EasyMock.expect(tubeDao.findByIdentifier(EasyMock.eq("SM-765"))).andReturn(mapBarcodeToTube.get("R333333"));
        EasyMock.expect(tubeDao.findByIdentifier(EasyMock.eq("SM-143"))).andReturn(mapBarcodeToTube.get("R444444"));
        EasyMock.expect(tubeDao.findByIdentifier(EasyMock.eq("SM-9243"))).andReturn(mapBarcodeToTube.get("R555555"));
        EasyMock.expect(tubeDao.findByIdentifier(EasyMock.eq("SM-118"))).andReturn(mapBarcodeToTube.get("R666666"));
        labBatchEJB.setTubeDAO(tubeDao);

        mockJira = EasyMock.createMock(JiraTicketDao.class);
        EasyMock.expect(mockJira.fetchByName(testLCSetKey))
                .andReturn(new JiraTicket(JiraServiceProducer.stubInstance(), testLCSetKey)).times(0, 1);
        EasyMock.expect(mockJira.fetchByName(testFCTKey))
                .andReturn(new JiraTicket(JiraServiceProducer.stubInstance(), testFCTKey)).times(0, 1);
        labBatchEJB.setJiraTicketDao(mockJira);

        labBatchDAO = EasyMock.createNiceMock(LabBatchDAO.class);
        labBatchEJB.setLabBatchDao(labBatchDAO);

        pdoNames = new ArrayList<String>();
        Collections.addAll(pdoNames, STUB_TEST_PDO_KEY);

        workflowName = WorkflowName.EXOME_EXPRESS.getWorkflowName();

        EasyMock.replay(mockJira, labBatchDAO, tubeDao);

        scottmat = "scottmat";
    }

    @AfterMethod(groups = TestGroups.DATABASE_FREE)
    public void tearDown() throws Exception {
        EasyMock.verify(labBatchDAO);
    }

    @Test
    public void testCreateLabBatch() throws Exception {

        final String batchName = "Test create batch basic";
        LabBatch testBatch = labBatchEJB
                .createLabBatch(new LabBatch(batchName, new HashSet<LabVessel>(mapBarcodeToTube.values()),
                        LabBatch.LabBatchType.WORKFLOW), "scottmat", CreateFields.IssueType.EXOME_EXPRESS);

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());
        Assert.assertNotNull(testBatch.getStartingLabVessels());

        Assert.assertNotSame(batchName, testBatch.getBatchName());
        Assert.assertEquals(testBatch.getJiraTicket().getTicketName(), testBatch.getBatchName());

        Assert.assertEquals(6, testBatch.getStartingLabVessels().size());
        Assert.assertEquals("6 samples from MyResearchProject PDO-999\n", testBatch.getBatchDescription());
        Assert.assertNull(testBatch.getDueDate());
        Assert.assertEquals(testBatch.getBatchName(), testBatch.getJiraTicket().getTicketName());
        Assert.assertEquals(testBatch.getLabBatchType(), LabBatch.LabBatchType.WORKFLOW);
    }

    @Test
    void testCreateFCTBatch() throws Exception {
        LabBatch testBatch =
                labBatchEJB.createLabBatch(new HashSet<>(mapBarcodeToTube.values()), "scottmat", testFCTKey,
                        LabBatch.LabBatchType.FCT, CreateFields.IssueType.FLOWCELL);
        EasyMock.verify(mockJira);

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());
        Assert.assertNotNull(testBatch.getStartingLabVessels());
        Assert.assertNotNull(testBatch.getBatchName());
        Assert.assertEquals(testFCTKey, testBatch.getBatchName());
        Assert.assertEquals(6, testBatch.getStartingLabVessels().size());
        Assert.assertEquals(testBatch.getBatchDescription(),
                "null" +
                "\n" +
                "\n" + AthenaClientServiceStub.rpSynopsis);
        Assert.assertNull(testBatch.getDueDate());
        Assert.assertEquals(testBatch.getBatchName(), testBatch.getJiraTicket().getTicketName());
        Assert.assertEquals(testBatch.getLabBatchType(), LabBatch.LabBatchType.FCT);
    }

    @Test
    public void testCreateLabBatchWithVesselBarcodes() throws Exception {

        LabBatch testBatch =
                labBatchEJB.createLabBatch("scottmat", vesselSampleList, testLCSetKey, LabBatch.LabBatchType.WORKFLOW,
                        CreateFields.IssueType.EXOME_EXPRESS);
        EasyMock.verify(mockJira, tubeDao);

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());
        Assert.assertNotNull(testBatch.getStartingLabVessels());
        Assert.assertNotNull(testBatch.getBatchName());
        Assert.assertEquals(testLCSetKey, testBatch.getBatchName());
        Assert.assertEquals(6, testBatch.getStartingLabVessels().size());
        Assert.assertEquals(testBatch.getBatchDescription(),
                extractDescriptionPrefix(testBatch) +
                "\n" +
                "\n" + AthenaClientServiceStub.rpSynopsis);
        Assert.assertNull(testBatch.getDueDate());

        Assert.assertEquals(testBatch.getBatchName(), testBatch.getJiraTicket().getTicketName());
    }

    @Test
    public void testCreateLabBatchWithVessels() throws Exception {

        LabBatch testBatch =
                labBatchEJB.createLabBatch(new HashSet<LabVessel>(mapBarcodeToTube.values()), "scottmat", testLCSetKey,
                        LabBatch.LabBatchType.WORKFLOW, CreateFields.IssueType.EXOME_EXPRESS);
        EasyMock.verify(mockJira);

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());
        Assert.assertNotNull(testBatch.getStartingLabVessels());
        Assert.assertNotNull(testBatch.getBatchName());
        Assert.assertEquals(testLCSetKey, testBatch.getBatchName());
        Assert.assertEquals(6, testBatch.getStartingLabVessels().size());
        Assert.assertEquals(testBatch.getBatchDescription(),
                extractDescriptionPrefix(testBatch) +
                "\n" +
                "\n" + AthenaClientServiceStub.rpSynopsis);
        Assert.assertNull(testBatch.getDueDate());
        Assert.assertEquals(testBatch.getBatchName(), testBatch.getJiraTicket().getTicketName());
    }

    @Test
    public void testCreateLabBatchWithJiraTicket() throws Exception {

        final String batchName = "second Test batch name";
        LabBatch testBatch = labBatchEJB
                .createLabBatch(new LabBatch(batchName, new HashSet<LabVessel>(mapBarcodeToTube.values()),
                        LabBatch.LabBatchType.WORKFLOW), scottmat, CreateFields.IssueType.EXOME_EXPRESS);

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());

        Assert.assertNotSame(batchName, testBatch.getBatchName());
        Assert.assertEquals(testBatch.getJiraTicket().getTicketName(), testBatch.getBatchName());

        Assert.assertNotNull(testBatch.getStartingLabVessels());
        Assert.assertEquals(6, testBatch.getStartingLabVessels().size());
        Assert.assertEquals(testBatch.getBatchDescription(), extractDescriptionPrefix(testBatch));
        Assert.assertNull(testBatch.getDueDate());

        Assert.assertEquals(testBatch.getBatchName(), testBatch.getJiraTicket().getTicketName());
    }

    @Test
    public void testCreateLabBatchWithExtraValues() throws Exception {

        final String description =
                "New User defined description set here at the Create LabBatch call level.  SHould be useful when giving users the ability to set their own description for the LCSET or whatever ticket";
        final String batchName = "Third test of batch name.";
        LabBatch batchInput = new LabBatch(batchName, new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        batchInput.setBatchDescription(description);

        LabBatch testBatch = labBatchEJB
                .createLabBatch(batchInput, scottmat, CreateFields.IssueType.EXOME_EXPRESS);

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());

        Assert.assertNotSame(batchName, testBatch.getBatchName());
        Assert.assertEquals(testBatch.getJiraTicket().getTicketName(), testBatch.getBatchName());

        Assert.assertNotNull(testBatch.getStartingLabVessels());
        Assert.assertEquals(6, testBatch.getStartingLabVessels().size());
        Assert.assertEquals(testBatch.getBatchDescription(), extractDescriptionPrefix(testBatch) +
                                                             "\n" +
                                                             "\n" + description);

        Assert.assertEquals(testBatch.getBatchName(), testBatch.getJiraTicket().getTicketName());
    }

    private String extractDescriptionPrefix(LabBatch testBatch) {

        ProductOrder testProductOrder = athenaClientService.retrieveProductOrderDetails(STUB_TEST_PDO_KEY);

        final String descriptionPrefix = testBatch.getStartingLabVessels().size() +
                                         " samples from " +
                                         testProductOrder.getResearchProject().getTitle() + " " + STUB_TEST_PDO_KEY +
                                         "\n";
        return descriptionPrefix;
    }

    @Test
    public void testPriorBatchCreation() throws Exception {

        Assert.assertFalse(labBatchEJB.validatePriorBatch(mapBarcodeToTube.values()));

        Set<LabVessel> workingVessels = new HashSet<LabVessel>(mapBarcodeToTube.values());


        LabBatch testBatch = new LabBatch("Test Batch for vessel Validation", workingVessels,
                LabBatch.LabBatchType.WORKFLOW);

        Assert.assertTrue(labBatchEJB.validatePriorBatch(mapBarcodeToTube.values()));

    }
}
