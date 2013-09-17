package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
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
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class LabBatchEJBTest extends ContainerTest {

    public static final String STUB_TEST_PDO_KEY = "PDO-999";
    public static final String BUCKET_NAME = "Pico/Plating Bucket";

    @Inject
    private LabBatchEjb labBatchEJB;

    @Inject
    private UserTransaction utx;

    @Inject
    private TwoDBarcodedTubeDao tubeDao;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private BucketDao bucketDao;

    private LinkedHashMap<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();
    private ArrayList<String> pdoNames;
    private String scottmat;
    private Bucket bucket;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {

        if (utx == null) {
            return;
        }

        utx.begin();

        bucket = bucketDao.findByName(BUCKET_NAME);

        if (bucket == null) {
            bucket = new Bucket(new WorkflowBucketDef(BUCKET_NAME));
            bucketDao.persist(bucket);
            bucketDao.flush();
            bucketDao.clear();
        }

        pdoNames = new ArrayList<>();
        Collections.addAll(pdoNames, STUB_TEST_PDO_KEY);

        List<String> vesselSampleList = new ArrayList<>(6);

        Collections.addAll(vesselSampleList, "SM-423", "SM-243", "SM-765", "SM-143", "SM-9243", "SM-118");

        // starting rack
        for (int sampleIndex = 1; sampleIndex <= vesselSampleList.size(); sampleIndex++) {
            String barcode = "R" + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex;
            String bspStock = vesselSampleList.get(sampleIndex - 1);
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(bspStock));
            tubeDao.persist(bspAliquot);
            mapBarcodeToTube.put(barcode, bspAliquot);
        }
        tubeDao.flush();
        scottmat = "scottmat";
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        if (utx == null) {
            return;
        }

        utx.rollback();

    }


    @Test
    public void testCreateLabBatch() throws Exception {

        LabBatch testBatch =
                labBatchEJB.createLabBatch(new HashSet<LabVessel>(mapBarcodeToTube.values()), scottmat, "LCSET-123",
                        LabBatch.LabBatchType.WORKFLOW, CreateFields.IssueType.EXOME_EXPRESS);

        final String batchName = testBatch.getBatchName();

        labBatchDao.flush();
        labBatchDao.clear();

        LabBatch testFind = labBatchDao.findByName(batchName);

        Assert.assertNotNull(testFind);
        Assert.assertNotNull(testFind.getJiraTicket());
        Assert.assertNotNull(testFind.getJiraTicket().getTicketName());
        Assert.assertNotNull(testFind.getStartingBatchLabVessels());
        Assert.assertEquals(6, testFind.getStartingBatchLabVessels().size());
        Assert.assertEquals(testFind.getBatchName(), testFind.getJiraTicket().getTicketName());
    }

    @Test
    public void testCreateLabBatchWithValues() throws Exception {

        String batchName = "Test lab Batch Name";
        final String description = "Test of New user input Batch Description";

        Date now = new Date();
        Calendar cal = Calendar.getInstance();

        cal.setTime(now);

        cal.add(Calendar.DATE, 21);

        Date future = cal.getTime();

        SimpleDateFormat dFormatter = new SimpleDateFormat("MM/dd/yy");

        String futureDate = dFormatter.format(future);

        LabBatch batchInput = new LabBatch(batchName, new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        batchInput.setBatchDescription(description);
        batchInput.setDueDate(future);

        labBatchEJB.createLabBatch(batchInput, scottmat, CreateFields.IssueType.EXOME_EXPRESS);

        batchName = batchInput.getBatchName();

        labBatchDao.flush();
        labBatchDao.clear();

        LabBatch testFind = labBatchDao.findByName(batchName);

        Assert.assertNotNull(testFind);
        Assert.assertNotNull(testFind.getJiraTicket());
        Assert.assertNotNull(testFind.getJiraTicket().getTicketName());
        Assert.assertNotNull(testFind.getStartingBatchLabVessels());
        Assert.assertEquals(6, testFind.getStartingBatchLabVessels().size());
        Assert.assertEquals(testFind.getBatchName(), testFind.getJiraTicket().getTicketName());
    }

    @Test
    public void testCreateLabBatchAndRemoveFromBucket() {
        putTubesInBucket();

        HashSet<LabVessel> starters = new HashSet<LabVessel>(mapBarcodeToTube.values());
        LabBatch batch = new LabBatch("LabBatchEJBTest.testCreateLabBatchAndRemoveFromBucket",
                starters, LabBatch.LabBatchType.WORKFLOW);
        LabBatch savedBatch = labBatchEJB.createLabBatchAndRemoveFromBucket(batch, scottmat, BUCKET_NAME,
                LabEvent.UI_EVENT_LOCATION, CreateFields.IssueType.EXOME_EXPRESS);

        //link the JIRA tickets for the batch created to the pdo batches.
        for (String pdoKey : LabVessel.extractPdoKeyList(starters)) {
            labBatchEJB.linkJiraBatchToTicket(pdoKey, savedBatch);
        }

        labBatchDao.flush();
        labBatchDao.clear();
        bucket = bucketDao.findByName(BUCKET_NAME);

        String expectedTicketId =
                CreateFields.ProjectType.getLcsetProjectType().getKeyPrefix() + JiraServiceStub.getCreatedIssueSuffix();
        Assert.assertEquals(expectedTicketId, savedBatch.getBatchName());
        savedBatch = labBatchDao.findByName(expectedTicketId);

        Assert.assertEquals(expectedTicketId, savedBatch.getJiraTicket().getTicketName());
        Assert.assertEquals(6, savedBatch.getStartingBatchLabVessels().size());
        for (TwoDBarcodedTube tube : mapBarcodeToTube.values()) {
            Assert.assertTrue(bucket.findEntry(tube) == null);
        }
    }

    @Test
    public void testCreateLabBatchAndRemoveFromBucketExistingTicket() {
        putTubesInBucket();

        String expectedTicketId = "testCreateLabBatchAndRemoveFromBucketExistingTicket";
        LabBatch savedBatch = labBatchEJB
                .createLabBatchAndRemoveFromBucket(new ArrayList<>(mapBarcodeToTube.keySet()), scottmat,
                        expectedTicketId, BUCKET_NAME, LabBatch.LabBatchType.WORKFLOW,
                        CreateFields.IssueType.EXOME_EXPRESS);

        labBatchDao.flush();
        labBatchDao.clear();
        bucket = bucketDao.findByName(BUCKET_NAME);

        Assert.assertEquals(expectedTicketId, savedBatch.getBatchName());
        savedBatch = labBatchDao.findByName(expectedTicketId);

        Assert.assertEquals(expectedTicketId, savedBatch.getJiraTicket().getTicketName());
        Assert.assertEquals(6, savedBatch.getStartingBatchLabVessels().size());
        for (TwoDBarcodedTube tube : mapBarcodeToTube.values()) {
            Assert.assertTrue(bucket.findEntry(tube) == null);
        }
    }

    private void putTubesInBucket() {
        bucket = bucketDao.findByName(BUCKET_NAME);

        for (LabVessel vessel : mapBarcodeToTube.values()) {
            bucket.addEntry(STUB_TEST_PDO_KEY, vessel,
                    org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry.BucketEntryType.PDO_ENTRY);
        }

        for (LabVessel vessel : mapBarcodeToTube.values()) {
            Assert.assertTrue(bucket.findEntry(vessel) != null);
        }
    }
}
