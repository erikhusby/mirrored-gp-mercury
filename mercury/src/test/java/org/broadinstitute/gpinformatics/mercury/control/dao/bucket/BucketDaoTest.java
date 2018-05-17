package org.broadinstitute.gpinformatics.mercury.control.dao.bucket;

import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderTest;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
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

import javax.enterprise.context.Dependent;
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
@Test(groups = TestGroups.STUBBY)
@Dependent
public class BucketDaoTest extends StubbyContainerTest {

    public BucketDaoTest(){}

    public static final String EXTRACTION_BUCKET_NAME = "Extraction Bucket";
    @Inject
    BucketDao bucketDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private UserTransaction utx;
    private Bucket testBucket;
    private String tempBucketSuffix;
    private String extractionTestBucketName;

    @BeforeMethod(groups = TestGroups.STUBBY)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();

        tempBucketSuffix = " Test temp Bucket";
        extractionTestBucketName = EXTRACTION_BUCKET_NAME + tempBucketSuffix;
        WorkflowBucketDef bucketDef = new WorkflowBucketDef(extractionTestBucketName);

        testBucket = bucketDao.findByName(extractionTestBucketName);
        if (testBucket == null) {

            testBucket = new Bucket(bucketDef);
            bucketDao.persist(testBucket);
            bucketDao.flush();
        }

    }

    @AfterMethod(groups = TestGroups.STUBBY)
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

        Bucket retrievedBucket = bucketDao.findByName(extractionTestBucketName);

        Assert.assertNotNull(retrievedBucket);

    }

    @Test
    public void testUpdateBucket() {

        Bucket retrievedBucket = bucketDao.findByName(extractionTestBucketName);
        ProductOrder testOrder = ProductOrderTestFactory.createDummyProductOrder();
        testOrder.setTitle(testOrder.getTitle() + ((new Date()).getTime()));
        testOrder.updateAddOnProducts(Collections.<Product>emptyList());
        try {
            testOrder.setProduct(productDao.findByBusinessKey(Product.EXOME_EXPRESS_V2_PART_NUMBER));
        } catch (InvalidProductException e) {
            Assert.fail(e.getMessage());
        }
        testOrder.setResearchProject(researchProjectDao.findByTitle("ADHD"));

        testOrder.setJiraTicketKey(ProductOrderTest.PDO_JIRA_KEY);

        retrievedBucket.addEntry(testOrder, new BarcodedTube("SM-1321"), BucketEntry.BucketEntryType.PDO_ENTRY);

        bucketDao.flush();
        bucketDao.clear();

        retrievedBucket = bucketDao.findByName(extractionTestBucketName);

        Assert.assertNotNull(retrievedBucket);

        Assert.assertEquals(retrievedBucket.getBucketEntries().size(),1);

        List<BucketEntry> entries = new LinkedList<>(retrievedBucket.getBucketEntries());
        Collections.sort(entries, BucketEntry.byDate);

        Assert.assertNotNull(entries.get(0).getBucket());

        Assert.assertEquals(entries.get(0).getBucket(),retrievedBucket);

        Assert.assertEquals( entries.get(0).getLabVessel().getBucketEntries().size(),1);
    }

    @Test
    public void testRemoveBucket() {

        Bucket retrievedBucket = bucketDao.findByName(extractionTestBucketName);

        bucketDao.remove(retrievedBucket);
        bucketDao.flush();
        bucketDao.clear();

        retrievedBucket = bucketDao.findByName(extractionTestBucketName);

        Assert.assertNull(retrievedBucket);

    }

}
