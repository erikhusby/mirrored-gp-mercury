package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

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

    private LinkedHashMap<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
    private String            workflowName;
    private ArrayList<String> pdoNames;
    private String            scottmat;
    private String            testLCSetKey;
    private JiraTicketDao     mockJira;
    private List<String>      vesselSampleList;

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() throws Exception {
        testLCSetKey = "LCSet-tst932";

        athenaClientService = AthenaClientProducer.stubInstance();
        labBatchEJB = new LabBatchEjb();
        labBatchEJB.setAthenaClientService(athenaClientService);
        labBatchEJB.setJiraService(JiraServiceProducer.stubInstance());

        mockJira = EasyMock.createMock(JiraTicketDao.class);
        EasyMock.expect(mockJira.fetchByName(testLCSetKey))
                .andReturn(new JiraTicket(JiraServiceProducer.stubInstance(), testLCSetKey));
        labBatchEJB.setJiraTicketDao(mockJira);

        labBatchDAO = EasyMock.createNiceMock(LabBatchDAO.class);
        labBatchEJB.setLabBatchDao(labBatchDAO);

        EasyMock.replay(mockJira, labBatchDAO);

        pdoNames = new ArrayList<String>();
        Collections.addAll(pdoNames, STUB_TEST_PDO_KEY);

        workflowName = "Exome Express";

        vesselSampleList = new ArrayList<String>(6);

        Collections.addAll(vesselSampleList, "SM-423", "SM-243", "SM-765", "SM-143", "SM-9243", "SM-118");

        // starting rack
        for (int sampleIndex = 1; sampleIndex <= vesselSampleList.size(); sampleIndex++) {
            String barcode = "R" + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex;
            String bspStock = vesselSampleList.get(sampleIndex - 1);
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(STUB_TEST_PDO_KEY, bspStock));
            mapBarcodeToTube.put(barcode, bspAliquot);
        }
        scottmat = "scottmat";
    }

    @AfterMethod(groups = TestGroups.DATABASE_FREE)
    public void tearDown() throws Exception {
        EasyMock.verify(labBatchDAO);
    }

    @Test
    public void testCreateLabBatch() throws Exception {

        LabBatch testBatch = labBatchEJB
                .createLabBatch(new LabBatch("", new HashSet<LabVessel>(mapBarcodeToTube.values())), "scottmat", null);

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());
        Assert.assertNotNull(testBatch.getStartingLabVessels());
        Assert.assertEquals(6, testBatch.getStartingLabVessels().size());
        Assert.assertEquals(workflowName + ": " + STUB_TEST_PDO_KEY, testBatch.getBatchName());
        Assert.assertEquals(AthenaClientServiceStub.rpSynopsis, testBatch.getBatchDescription());
        Assert.assertNull(testBatch.getDueDate());
    }

    @Test
    public void testCreateLabBatchWithVesselBarcodes() throws Exception {

        LabBatch testBatch =
                labBatchEJB.createLabBatch(new HashSet<LabVessel>(mapBarcodeToTube.values()), "scottmat", null);

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());
        Assert.assertNotNull(testBatch.getStartingLabVessels());
        Assert.assertEquals(6, testBatch.getStartingLabVessels().size());
        Assert.assertEquals(workflowName + ": " + STUB_TEST_PDO_KEY, testBatch.getBatchName());
        Assert.assertEquals(AthenaClientServiceStub.rpSynopsis, testBatch.getBatchDescription());
        Assert.assertNull(testBatch.getDueDate());
    }

    @Test
    public void testCreateLabBatchWithJiraTicket() throws Exception {

        LabBatch testBatch = labBatchEJB
                .createLabBatch(new LabBatch("", new HashSet<LabVessel>(mapBarcodeToTube.values())), scottmat,
                                testLCSetKey);
        EasyMock.verify(mockJira);

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());
        Assert.assertEquals(testLCSetKey, testBatch.getJiraTicket().getTicketName());
        Assert.assertNotNull(testBatch.getStartingLabVessels());
        Assert.assertEquals(6, testBatch.getStartingLabVessels().size());
        Assert.assertEquals(workflowName + ": " + STUB_TEST_PDO_KEY, testBatch.getBatchName());
        Assert.assertEquals(AthenaClientServiceStub.rpSynopsis, testBatch.getBatchDescription());
        Assert.assertNull(testBatch.getDueDate());

    }

    @Test
    public void testCreateLabBatchWithExtraValues() throws Exception {

        final String description =
                "New User defined description set here at the Create LabBatch call level.  SHould be useful when giving users the ability to set their own description for the LCSET or whatever ticket";
        LabBatch batchInput = new LabBatch("", new HashSet<LabVessel>(mapBarcodeToTube.values()));
        batchInput.setBatchDescription(description);

        LabBatch testBatch = labBatchEJB
                .createLabBatch(batchInput, scottmat,
                                testLCSetKey);
        EasyMock.verify(mockJira);

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());
        Assert.assertEquals(testLCSetKey, testBatch.getJiraTicket().getTicketName());
        Assert.assertNotNull(testBatch.getStartingLabVessels());
        Assert.assertEquals(6, testBatch.getStartingLabVessels().size());
        Assert.assertEquals(workflowName + ": " + STUB_TEST_PDO_KEY, testBatch.getBatchName());
        Assert.assertEquals(description, testBatch.getBatchDescription());

    }
}
