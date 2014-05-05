package org.broadinstitute.gpinformatics.mercury.control.dao.bucket;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderTest;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 11/6/12
 *         Time: 1:05 PM
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BucketDaoTest extends ContainerTest {

    public static final String EXTRACTION_BUCKET_NAME = "Extraction Bucket TEST";
    @Inject
    BucketDao bucketDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private UserTransaction utx;
    private Bucket testBucket;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();

        WorkflowBucketDef bucketDef = new WorkflowBucketDef(EXTRACTION_BUCKET_NAME);

        testBucket = bucketDao.findByName(BucketDaoTest.EXTRACTION_BUCKET_NAME);
        if (testBucket == null) {

            testBucket = new Bucket(bucketDef);
            bucketDao.persist(testBucket);
            bucketDao.flush();
//            bucketDao.clear();
        }

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
    public void testFindPersistedBucket() {

        Assert.assertNotNull(testBucket.getBucketId(), "Bucket not Persisted to the Database");

        Bucket retrievedBucket = bucketDao.findByName(EXTRACTION_BUCKET_NAME);

        Assert.assertNotNull(retrievedBucket);

    }

    @Test
    public void testUpdateBucket() {

        Bucket retrievedBucket = bucketDao.findByName(EXTRACTION_BUCKET_NAME);
        ProductOrder testOrder = ProductOrderTestFactory.createDummyProductOrder();
        testOrder.setTitle(testOrder.getTitle() + ((new Date()).getTime()));
        testOrder.updateAddOnProducts(Collections.<Product>emptyList());
        testOrder.setProduct(productDao.findByBusinessKey(Product.EXOME_EXPRESS_V2_PART_NUMBER));
        testOrder.setResearchProject(researchProjectDao.findByTitle("ADHD"));

        testOrder.setJiraTicketKey(ProductOrderTest.PDO_JIRA_KEY);

        retrievedBucket.addEntry(testOrder, new BarcodedTube("SM-1321"), BucketEntry.BucketEntryType.PDO_ENTRY);

        bucketDao.flush();
        bucketDao.clear();

        retrievedBucket = bucketDao.findByName(EXTRACTION_BUCKET_NAME);

        Assert.assertNotNull(retrievedBucket);

        Assert.assertEquals(1, retrievedBucket.getBucketEntries().size());

        List<BucketEntry> entries = new LinkedList<>(retrievedBucket.getBucketEntries());
        Collections.sort(entries, BucketEntry.byDate);

//        Assert.assertNotNull(entries.get(0).getLabVessel().getLabVesselId());

        Assert.assertNotNull(entries.get(0).getBucket());

        Assert.assertEquals(retrievedBucket, entries.get(0).getBucket());

        Assert.assertEquals(1, entries.get(0).getLabVessel().getBucketEntries().size());
    }

    @Test
    public void testRemoveBucket() {

        Bucket retrievedBucket = bucketDao.findByName(EXTRACTION_BUCKET_NAME);

        bucketDao.remove(retrievedBucket);
        bucketDao.flush();
        bucketDao.clear();

        retrievedBucket = bucketDao.findByName(EXTRACTION_BUCKET_NAME);

        Assert.assertNull(retrievedBucket);

    }

}
