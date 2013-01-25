package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.easymock.EasyMock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

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
    private String            workflowName;
    private ArrayList<String> pdoNames;
    private String            scottmat;
    private String            testLCSetKey;
    private JiraTicketDao     mockJira;
    private Set<String>      vesselSampleList;

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() throws Exception {
        testLCSetKey = "LCSet-tst932";

        vesselSampleList = new HashSet<String>(6);

        Collections.addAll(vesselSampleList, "SM-423", "SM-243", "SM-765", "SM-143", "SM-9243", "SM-118");

        // starting rack
        int sampleIndex = 1;
        for(String sampleName:vesselSampleList) {
            String barcode = "R" + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex;
            String bspStock = sampleName;
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(STUB_TEST_PDO_KEY, bspStock));
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
                .andReturn(new JiraTicket(JiraServiceProducer.stubInstance(), testLCSetKey));
        labBatchEJB.setJiraTicketDao(mockJira);

        labBatchDAO = EasyMock.createNiceMock(LabBatchDAO.class);
        labBatchEJB.setLabBatchDao(labBatchDAO);

        pdoNames = new ArrayList<String>();
        Collections.addAll(pdoNames, STUB_TEST_PDO_KEY);

        workflowName = "Exome Express";

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
                .createLabBatch(new LabBatch(batchName, new HashSet<LabVessel>(mapBarcodeToTube.values())), "scottmat");

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());
        Assert.assertNotNull(testBatch.getStartingLabVessels());

        Assert.assertNotSame(batchName, testBatch.getBatchName());
        Assert.assertEquals(testBatch.getJiraTicket().getTicketName(),testBatch.getBatchName());

        Assert.assertEquals(6, testBatch.getStartingLabVessels().size());
        Assert.assertEquals("6 samples from Test RP PDO-999\n", testBatch.getBatchDescription());
        Assert.assertNull(testBatch.getDueDate());
        Assert.assertEquals(testBatch.getBatchName(), testBatch.getJiraTicket().getTicketName());
    }

    @Test
    public void testCreateLabBatchWithVesselBarcodes() throws Exception {

        LabBatch testBatch =
                labBatchEJB.createLabBatch("scottmat", vesselSampleList, testLCSetKey);
        EasyMock.verify(mockJira, tubeDao);

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());
        Assert.assertNotNull(testBatch.getStartingLabVessels());
        Assert.assertNotNull(testBatch.getBatchName());
        Assert.assertEquals(testLCSetKey, testBatch.getBatchName());
        Assert.assertEquals(6, testBatch.getStartingLabVessels().size());
        Assert.assertEquals(AthenaClientServiceStub.rpSynopsis, testBatch.getBatchDescription());
        Assert.assertNull(testBatch.getDueDate());

        Assert.assertEquals(testBatch.getBatchName(), testBatch.getJiraTicket().getTicketName());
    }

    @Test
    public void testCreateLabBatchWithVessels() throws Exception {

        LabBatch testBatch =
                labBatchEJB.createLabBatch(new HashSet<LabVessel>(mapBarcodeToTube.values()), "scottmat", testLCSetKey);
        EasyMock.verify(mockJira);

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());
        Assert.assertNotNull(testBatch.getStartingLabVessels());
        Assert.assertNotNull(testBatch.getBatchName());
        Assert.assertEquals(testLCSetKey, testBatch.getBatchName());
        Assert.assertEquals(6, testBatch.getStartingLabVessels().size());
        Assert.assertEquals(AthenaClientServiceStub.rpSynopsis, testBatch.getBatchDescription());
        Assert.assertNull(testBatch.getDueDate());
        Assert.assertEquals(testBatch.getBatchName(), testBatch.getJiraTicket().getTicketName());
    }

    @Test
    public void testCreateLabBatchWithJiraTicket() throws Exception {

        final String batchName = "second Test batch name";
        LabBatch testBatch = labBatchEJB
                .createLabBatch(new LabBatch(batchName, new HashSet<LabVessel>(mapBarcodeToTube.values())), scottmat
                );

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());

        Assert.assertNotSame(batchName, testBatch.getBatchName());
        Assert.assertEquals(testBatch.getJiraTicket().getTicketName(),testBatch.getBatchName());

        Assert.assertNotNull(testBatch.getStartingLabVessels());
        Assert.assertEquals(6, testBatch.getStartingLabVessels().size());
        Assert.assertEquals("6 samples from Test RP PDO-999\n", testBatch.getBatchDescription());
        Assert.assertNull(testBatch.getDueDate());

        Assert.assertEquals(testBatch.getBatchName(), testBatch.getJiraTicket().getTicketName());
    }

    @Test
    public void testCreateLabBatchWithExtraValues() throws Exception {

        final String description =
                "New User defined description set here at the Create LabBatch call level.  SHould be useful when giving users the ability to set their own description for the LCSET or whatever ticket";
        final String batchName = "Third test of batch name.";
        LabBatch batchInput = new LabBatch(batchName, new HashSet<LabVessel>(mapBarcodeToTube.values()));
        batchInput.setBatchDescription(description);

        LabBatch testBatch = labBatchEJB
                .createLabBatch(batchInput, scottmat
                );

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());

        Assert.assertNotSame(batchName, testBatch.getBatchName());
        Assert.assertEquals(testBatch.getJiraTicket().getTicketName(),testBatch.getBatchName());

        Assert.assertNotNull(testBatch.getStartingLabVessels());
        Assert.assertEquals(6, testBatch.getStartingLabVessels().size());
        Assert.assertEquals(description, testBatch.getBatchDescription());

        Assert.assertEquals(testBatch.getBatchName(), testBatch.getJiraTicket().getTicketName());
    }

    @Test
    public void testPriorBatchCreation() throws Exception {

        Assert.assertFalse(labBatchEJB.validatePriorBatch(mapBarcodeToTube.values()));

        Set<LabVessel> workingVessels = new HashSet<LabVessel>(mapBarcodeToTube.values());


        LabBatch testBatch = new LabBatch( "Test Batch for vessel Validation", workingVessels);

        Assert.assertTrue(labBatchEJB.validatePriorBatch(mapBarcodeToTube.values()));

    }
}
