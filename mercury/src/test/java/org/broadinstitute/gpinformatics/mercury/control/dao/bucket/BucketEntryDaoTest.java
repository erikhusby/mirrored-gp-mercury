package org.broadinstitute.gpinformatics.mercury.control.dao.bucket;

import org.testng.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Scott Matthews
 *         Date: 11/6/12
 *         Time: 1:06 PM
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BucketEntryDaoTest extends ContainerTest {

    //    public static final String EXTRACTION_BUCKET_NAME = "Extraction Bucket";
    @Inject
    BucketDao bucketDao;

    @Inject
    BucketEntryDao bucketEntryDao;

    @Inject
    TwoDBarcodedTubeDAO tubeDAO;

    @Inject
    private UserTransaction utx;

    private Bucket testBucket;
    private String twoDBarcodeKey;
    private String testPoBusinessKey;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();

        WorkflowBucketDef bucketDef = new WorkflowBucketDef(BucketDaoTest.EXTRACTION_BUCKET_NAME);

        testBucket = new Bucket(bucketDef);

        bucketDao.persist(testBucket);
        bucketDao.flush();
        bucketDao.clear();

        Bucket newTestBucket = bucketDao.findByName(BucketDaoTest.EXTRACTION_BUCKET_NAME);

        twoDBarcodeKey = "SM-1493";
        testPoBusinessKey = "PDO-33";
        BucketEntry testEntry = new BucketEntry(new TwoDBarcodedTube(twoDBarcodeKey), testPoBusinessKey,
                newTestBucket);
        bucketEntryDao.persist(testEntry);
        bucketEntryDao.flush();
        bucketEntryDao.clear();

    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    @Test
    public void testFindEntry() {

        TwoDBarcodedTube foundVessel = tubeDAO.findByBarcode(twoDBarcodeKey);

        Assert.assertNotNull(foundVessel);

        BucketEntry retrievedEntry = bucketEntryDao.findByVesselAndPO(foundVessel, testPoBusinessKey);

        Assert.assertNotNull(retrievedEntry);
        Assert.assertNotNull(retrievedEntry.getBucket());
        Assert.assertNotNull(retrievedEntry.getCreatedDate());
        Assert.assertEquals(retrievedEntry.getStatus(), BucketEntry.Status.Active);

        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yy");

        Assert.assertEquals(dateFormatter.format(new Date()),
                dateFormatter.format(retrievedEntry.getCreatedDate()));

        Bucket findBucket = bucketDao.findByName(BucketDaoTest.EXTRACTION_BUCKET_NAME);

        BucketEntry retrievedEntry2 = bucketEntryDao.findByVesselAndBucket(foundVessel, findBucket);

        Assert.assertNotNull(retrievedEntry2);
        Assert.assertNotNull(retrievedEntry2.getBucket());
        Assert.assertNotNull(retrievedEntry2.getCreatedDate());

        Assert.assertEquals(dateFormatter.format(new Date()), dateFormatter.format(
                retrievedEntry2
                        .getCreatedDate()));
    }

    @Test
    public void testFindDuplicate() {

        Bucket testBucket = bucketDao.findByName(BucketDaoTest.EXTRACTION_BUCKET_NAME);

        TwoDBarcodedTube vesselToDupe = tubeDAO.findByBarcode(twoDBarcodeKey);
        BucketEntry testEntry = new BucketEntry(vesselToDupe, testPoBusinessKey + "dupe",
                testBucket);

        bucketEntryDao.persist(testEntry);
        bucketEntryDao.flush();
        bucketEntryDao.clear();

        TwoDBarcodedTube foundVessel = tubeDAO.findByBarcode(twoDBarcodeKey);

        Assert.assertNotNull(foundVessel);

        BucketEntry retrievedEntry = bucketEntryDao.findByVesselAndPO(foundVessel, testPoBusinessKey);

        Assert.assertNotNull(retrievedEntry);
        Assert.assertNotNull(retrievedEntry.getBucket());
        Assert.assertNotNull(retrievedEntry.getCreatedDate());

        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yy");

        Assert.assertEquals(dateFormatter.format(new Date()), dateFormatter.format(
                retrievedEntry
                        .getCreatedDate()));

        Bucket findBucket = bucketDao.findByName(BucketDaoTest.EXTRACTION_BUCKET_NAME);

        try {
            BucketEntry retrievedEntry2 = bucketEntryDao.findByVesselAndBucket(foundVessel, findBucket);
            Assert.fail("Duplicate Exception Expected");
        } catch (Exception dupeExp) {

        }
    }

    @Test
    public void testUpdateEntry() {

        TwoDBarcodedTube foundVessel = tubeDAO.findByBarcode(twoDBarcodeKey);

        Assert.assertNotNull(foundVessel);

        BucketEntry retrievedEntry = bucketEntryDao.findByVesselAndPO(foundVessel, testPoBusinessKey);

        Assert.assertNotSame(24, retrievedEntry.getProductOrderRanking());

        retrievedEntry.setProductOrderRanking(24);

        bucketEntryDao.flush();
        bucketEntryDao.clear();

        TwoDBarcodedTube newFoundVessel = tubeDAO.findByBarcode(twoDBarcodeKey);

        Assert.assertNotNull(foundVessel);

        BucketEntry newRetrievedEntry = bucketEntryDao.findByVesselAndPO(newFoundVessel, testPoBusinessKey);

        Assert.assertEquals(24, newRetrievedEntry.getProductOrderRanking().intValue());

    }

    @Test
    public void testRemoveEntry() {

        TwoDBarcodedTube foundVessel = tubeDAO.findByBarcode(twoDBarcodeKey);

        Assert.assertNotNull(foundVessel);

        BucketEntry retrievedEntry = bucketEntryDao.findByVesselAndPO(foundVessel, testPoBusinessKey);

        bucketEntryDao.remove(retrievedEntry);
        bucketEntryDao.flush();
        bucketEntryDao.clear();

        TwoDBarcodedTube newFoundVessel = tubeDAO.findByBarcode(twoDBarcodeKey);

        BucketEntry notFoundEntry = bucketEntryDao.findByVesselAndPO(newFoundVessel, testPoBusinessKey);
        Assert.assertNull(notFoundEntry);

    }

}
