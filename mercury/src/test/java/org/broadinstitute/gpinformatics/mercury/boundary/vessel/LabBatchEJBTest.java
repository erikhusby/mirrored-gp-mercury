package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
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

@Test(groups = TestGroups.STUBBY, singleThreaded = true)
@Dependent
public class LabBatchEJBTest extends StubbyContainerTest {

    public LabBatchEJBTest(){}

    public static final String STUB_TEST_PDO_KEY = "PDO-999";
    public static final String BUCKET_NAME = "Pico/Plating Bucket";
    public static final String EXTRACTION_BUCKET = "Extract to DNA and RNA";
    public static final String EXTRACTION_TO_DNA_BUCKET = "Extract to DNA";

    @Inject
    private LabBatchEjb labBatchEJB;

    @Inject
    private UserTransaction utx;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private BucketDao bucketDao;

    @Inject
    LabBatchTestUtils labBatchTestUtils;

    private LinkedHashMap<String, BarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();

    private String scottmat;
    private Bucket bucket;

    @BeforeMethod(groups = TestGroups.STUBBY)
    public void setUp() throws Exception {

        if (utx == null) {
            return;
        }

        utx.begin();

        List<String> vesselSampleList = new ArrayList<>(6);

        Collections.addAll(vesselSampleList, "SM-423", "SM-243", "SM-765", "SM-143", "SM-9243", "SM-118");

        mapBarcodeToTube = labBatchTestUtils.initializeTubes(vesselSampleList, MaterialType.CELLS_PELLET_FROZEN);
        scottmat = "scottmat";
    }

    @AfterMethod(groups = TestGroups.STUBBY)
    public void tearDown() throws Exception {
        if (utx == null) {
            return;
        }

        utx.rollback();
    }


    @Test
    public void testCreateLabBatch() throws Exception {

        putTubesInBucket(BucketEntry.BucketEntryType.PDO_ENTRY);

        LabBatch testBatch = new LabBatch("LCSET-123", new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);

        for (BarcodedTube barcodedTube : mapBarcodeToTube.values()) {
            for (BucketEntry bucketEntry : barcodedTube.getBucketEntries()) {
                testBatch.addBucketEntry(bucketEntry);
            }
        }

        labBatchEJB.createLabBatch(testBatch, scottmat, CreateFields.IssueType.EXOME_EXPRESS,
                CreateFields.ProjectType.LCSET_PROJECT);

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
        putTubesInBucket(BucketEntry.BucketEntryType.PDO_ENTRY);

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

        for (BarcodedTube barcodedTube : mapBarcodeToTube.values()) {
            for (BucketEntry bucketEntry : barcodedTube.getBucketEntries()) {
                batchInput.addBucketEntry(bucketEntry);
            }
        }

        labBatchEJB.createLabBatch(batchInput, scottmat, CreateFields.IssueType.EXOME_EXPRESS,
                CreateFields.ProjectType.LCSET_PROJECT);

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
    public void testCreateLabBatchAndRemoveFromBucket() throws ValidationException {
        putTubesInBucket(BucketEntry.BucketEntryType.PDO_ENTRY);

        HashSet<LabVessel> starters = new HashSet<LabVessel>(mapBarcodeToTube.values());

        List<Long> bucketIds = new ArrayList<>();
        String bucketName = null;
        for (LabVessel starter : starters) {

            BucketEntry next = starter.getBucketEntries().iterator().next();
            bucketName = next.getBucket().getBucketDefinitionName();
            bucketIds.add(next.getBucketEntryId());
        }

        LabBatch savedBatch = labBatchEJB.createLabBatchAndRemoveFromBucket(LabBatch.LabBatchType.WORKFLOW,
                Workflow.ICE_EXOME_EXPRESS, bucketIds, Collections.<Long>emptyList(),
                "LabBatchEJBTest.testCreateLabBatchAndRemoveFromBucket", "", new Date(), "", scottmat, bucketName );

        //link the JIRA tickets for the batch created to the pdo batches.
        for (String pdoKey : LabVessel.extractPdoKeyList(starters)) {
            labBatchEJB.linkJiraBatchToTicket(pdoKey, savedBatch);
        }

        labBatchDao.flush();
        labBatchDao.clear();
        bucket = bucketDao.findByName(BUCKET_NAME);

        String expectedTicketId =
                CreateFields.ProjectType.LCSET_PROJECT.getKeyPrefix() + JiraServiceStub.getCreatedIssueSuffix();
        Assert.assertEquals(expectedTicketId, savedBatch.getBatchName());
        savedBatch = labBatchDao.findByName(expectedTicketId);

        Assert.assertEquals(expectedTicketId, savedBatch.getJiraTicket().getTicketName());
        Assert.assertEquals(6, savedBatch.getStartingBatchLabVessels().size());
        for (BarcodedTube tube : mapBarcodeToTube.values()) {
            Assert.assertTrue(bucket.findEntry(tube) == null);
        }
    }

    @Test
    public void testCreateXTRLabBatchAndRemoveFromBucket() throws ValidationException {
        this.bucket = labBatchTestUtils.putTubesInSpecificBucket(EXTRACTION_TO_DNA_BUCKET, BucketEntry.BucketEntryType.PDO_ENTRY,
                mapBarcodeToTube);

        HashSet<LabVessel> starters = new HashSet<LabVessel>(mapBarcodeToTube.values());

        List<Long> bucketIds = new ArrayList<>();
        String bucketName = null;
        for (LabVessel starter : starters) {
            for (BucketEntry bucketEntry : starter.getBucketEntries()) {
                bucketIds.add(bucketEntry.getBucketEntryId());
            }
        }

        LabBatch savedBatch = labBatchEJB.createLabBatchAndRemoveFromBucket(LabBatch.LabBatchType.WORKFLOW,
                Workflow.CLINICAL_WHOLE_BLOOD_EXTRACTION, bucketIds, Collections.<Long>emptyList(),
                "LabBatchEJBTest.testCreateLabBatchAndRemoveFromBucket", "", new Date(), "", scottmat, EXTRACTION_TO_DNA_BUCKET);

        //link the JIRA tickets for the batch created to the pdo batches.
        for (String pdoKey : LabVessel.extractPdoKeyList(starters)) {
            labBatchEJB.linkJiraBatchToTicket(pdoKey, savedBatch);
        }

        labBatchDao.flush();
        labBatchDao.clear();
        bucket = bucketDao.findByName(EXTRACTION_TO_DNA_BUCKET);

        String expectedTicketId =
                CreateFields.ProjectType.EXTRACTION_PROJECT.getKeyPrefix() + JiraServiceStub.getCreatedIssueSuffix();
        Assert.assertEquals(expectedTicketId, savedBatch.getBatchName());
        savedBatch = labBatchDao.findByName(expectedTicketId);

        Assert.assertEquals(expectedTicketId, savedBatch.getJiraTicket().getTicketName());
        Assert.assertEquals(6, savedBatch.getStartingBatchLabVessels().size());
        for (BarcodedTube tube : mapBarcodeToTube.values()) {
            Assert.assertTrue(bucket.findEntry(tube) == null);
        }
    }

    @Test
    public void testCreateLabBatchAndRemoveFromBucketExistingTicket() throws ValidationException {
        putTubesInBucket(BucketEntry.BucketEntryType.PDO_ENTRY);

        String expectedTicketId =
                CreateFields.ProjectType.LCSET_PROJECT.getKeyPrefix() + JiraServiceStub.getCreatedIssueSuffix();

        List<Long> bucketIds = new ArrayList<>();
        String selectedBucket = null;
        for (LabVessel vessel : mapBarcodeToTube.values()) {

            BucketEntry next = vessel.getBucketEntries().iterator().next();
            selectedBucket = next.getBucket().getBucketDefinitionName();
            bucketIds.add(next.getBucketEntryId());
        }

        LabBatch savedBatch = labBatchEJB.createLabBatchAndRemoveFromBucket(LabBatch.LabBatchType.WORKFLOW,
                Workflow.ICE_EXOME_EXPRESS, bucketIds, Collections.<Long>emptyList(),
                expectedTicketId, "", new Date(), "", scottmat, selectedBucket);
        labBatchDao.flush();
        labBatchDao.clear();
        bucket = bucketDao.findByName(BUCKET_NAME);

        Assert.assertEquals(savedBatch.getBatchName(), expectedTicketId);
        savedBatch = labBatchDao.findById(LabBatch.class, savedBatch.getLabBatchId());

        Assert.assertEquals(savedBatch.getJiraTicket().getTicketName(), expectedTicketId);
        Assert.assertEquals(6, savedBatch.getStartingBatchLabVessels().size());
        for (BarcodedTube tube : mapBarcodeToTube.values()) {
            Assert.assertTrue(bucket.findEntry(tube) == null);
        }
    }

    @Test
    public void testCreateXTRLabBatchAndRemoveFromBucketExistingTicket() throws ValidationException {
        this.bucket = labBatchTestUtils.putTubesInSpecificBucket(EXTRACTION_TO_DNA_BUCKET, BucketEntry.BucketEntryType.PDO_ENTRY,
                mapBarcodeToTube);

        String expectedTicketId =
                CreateFields.ProjectType.EXTRACTION_PROJECT.getKeyPrefix() + JiraServiceStub.getCreatedIssueSuffix();

        List<Long> bucketIds = new ArrayList<>();

        String selectedBucket = null;
        for (LabVessel vessel : mapBarcodeToTube.values()) {
            for (BucketEntry bucketEntry : vessel.getBucketEntries()) {
                bucketIds.add(bucketEntry.getBucketEntryId());
            }
        }

        LabBatch savedBatch = labBatchEJB.createLabBatchAndRemoveFromBucket(LabBatch.LabBatchType.WORKFLOW,
                Workflow.CLINICAL_WHOLE_BLOOD_EXTRACTION, bucketIds, Collections.<Long>emptyList(),
                expectedTicketId,"", new Date(), "", scottmat, EXTRACTION_TO_DNA_BUCKET);
        labBatchDao.flush();
        labBatchDao.clear();
        bucket = bucketDao.findByName(EXTRACTION_TO_DNA_BUCKET);

        Assert.assertEquals(savedBatch.getBatchName(), expectedTicketId);
        savedBatch = labBatchDao.findById(LabBatch.class, savedBatch.getLabBatchId());

        Assert.assertEquals(savedBatch.getJiraTicket().getTicketName(), expectedTicketId);
        Assert.assertEquals(6, savedBatch.getStartingBatchLabVessels().size());
        for (BarcodedTube tube : mapBarcodeToTube.values()) {
            Assert.assertTrue(bucket.findEntry(tube) == null);
        }
    }

    private void putTubesInBucket(BucketEntry.BucketEntryType bucketEntryType) {
        bucket = labBatchTestUtils.putTubesInSpecificBucket(BUCKET_NAME, bucketEntryType,
                mapBarcodeToTube);
    }
}
