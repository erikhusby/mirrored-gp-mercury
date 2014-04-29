package org.broadinstitute.gpinformatics.mercury.control.dao.bucket;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.ThreadEntityManager;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BucketEntryDaoTest extends ContainerTest {

    @Inject
    BucketDao bucketDao;

    @Inject
    BucketEntryDao bucketEntryDao;

    @Inject
    TwoDBarcodedTubeDao tubeDao;

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    ProductDao productDao;

    @Inject
    ResearchProjectDao researchProjectDao;

    @Inject
    private UserTransaction utx;

    private Bucket testBucket;
    private String twoDBarcodeKey;
    private String testPoBusinessKey;
    private ProductOrder testOrder;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }


        testBucket = bucketDao.findByName(BucketDaoTest.EXTRACTION_BUCKET_NAME);
        if(testBucket == null) {
            WorkflowBucketDef bucketDef = new WorkflowBucketDef(BucketDaoTest.EXTRACTION_BUCKET_NAME);

            testBucket = new Bucket(bucketDef);
            bucketEntryDao.persist(testBucket);
            bucketEntryDao.flush();
            bucketEntryDao.clear();
        }

        twoDBarcodeKey = "SM-1493" +(new Date()).getTime();
        testPoBusinessKey = "PDO-33";
        testOrder = productOrderDao.findByBusinessKey(testPoBusinessKey);
        if(testOrder == null) {
            testOrder = ProductOrderTestFactory.createDummyProductOrder(testPoBusinessKey);
            testOrder.setProduct(productDao.findByBusinessKey(Product.EXOME_EXPRESS_V2_PART_NUMBER));
            testOrder.setResearchProject(researchProjectDao.findByTitle("ADHD"));
        }
        BucketEntry testEntry = new BucketEntry(new TwoDBarcodedTube(twoDBarcodeKey), testOrder, testBucket,
                                                BucketEntry.BucketEntryType.PDO_ENTRY);
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

    }

    @Test
    public void testFindEntry() {

        TwoDBarcodedTube foundVessel = tubeDao.findByBarcode(twoDBarcodeKey);

        Assert.assertNotNull(foundVessel);

        BucketEntry retrievedEntry = bucketEntryDao.findByVesselAndPO(foundVessel, testOrder);

        Assert.assertNotNull(retrievedEntry);
        Assert.assertNotNull(retrievedEntry.getBucket());
        Assert.assertNotNull(retrievedEntry.getCreatedDate());
        Assert.assertEquals(retrievedEntry.getStatus(), BucketEntry.Status.Active);

        Assert.assertEquals(retrievedEntry.getProductOrder(),testOrder);

        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yy");

        Assert.assertEquals(dateFormatter.format(new Date()),
                            dateFormatter.format(retrievedEntry.getCreatedDate()));

        testBucket = bucketDao.findByName(BucketDaoTest.EXTRACTION_BUCKET_NAME);
        if(testBucket == null) {
            WorkflowBucketDef bucketDef = new WorkflowBucketDef(BucketDaoTest.EXTRACTION_BUCKET_NAME);

            testBucket = new Bucket(bucketDef);
            bucketEntryDao.persist(testBucket);
            bucketEntryDao.flush();
            bucketEntryDao.clear();
        }

        BucketEntry retrievedEntry2 = bucketEntryDao.findByVesselAndBucket(foundVessel, testBucket);

        Assert.assertNotNull(retrievedEntry2);
        Assert.assertNotNull(retrievedEntry2.getBucket());
        Assert.assertNotNull(retrievedEntry2.getCreatedDate());

        Assert.assertEquals(dateFormatter.format(new Date()), dateFormatter.format(
                retrievedEntry2
                        .getCreatedDate()));
    }

    @Test
    public void testFindDuplicate() {

        TwoDBarcodedTube vesselToDupe = tubeDao.findByBarcode(twoDBarcodeKey);
        testBucket = bucketDao.findByName(BucketDaoTest.EXTRACTION_BUCKET_NAME);
        if(testBucket == null) {
            WorkflowBucketDef bucketDef = new WorkflowBucketDef(BucketDaoTest.EXTRACTION_BUCKET_NAME);

            testBucket = new Bucket(bucketDef);
            bucketEntryDao.persist(testBucket);
            bucketEntryDao.flush();
            bucketEntryDao.clear();
        }

        ProductOrder testDupeOrder = productOrderDao.findByBusinessKey(testPoBusinessKey + "dupe");
        if(testDupeOrder == null) {
            testDupeOrder = ProductOrderTestFactory.createDummyProductOrder(testPoBusinessKey + "dupe");

            testDupeOrder.setProduct(productDao.findByPartNumber(Product.EXOME_EXPRESS_V2_PART_NUMBER));

            testDupeOrder.setResearchProject(researchProjectDao.findByTitle("ADHD"));
            testDupeOrder.updateAddOnProducts(Collections.<Product>emptyList());
        }
        BucketEntry testEntry = new BucketEntry(vesselToDupe, testDupeOrder, testBucket,
                                                BucketEntry.BucketEntryType.PDO_ENTRY);

        bucketEntryDao.persist(testEntry);
        bucketEntryDao.flush();
        bucketEntryDao.clear();

        TwoDBarcodedTube foundVessel = tubeDao.findByBarcode(twoDBarcodeKey);

        Assert.assertNotNull(foundVessel);

        testOrder = productOrderDao.findByBusinessKey(testPoBusinessKey);
        BucketEntry retrievedEntry = bucketEntryDao.findByVesselAndPO(foundVessel, testOrder);

        Assert.assertNotNull(retrievedEntry);
        Assert.assertNotNull(retrievedEntry.getBucket());
        Assert.assertNotNull(retrievedEntry.getCreatedDate());

        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yy");

        Assert.assertEquals(dateFormatter.format(new Date()), dateFormatter.format(
                retrievedEntry
                        .getCreatedDate()));
        Assert.assertEquals(retrievedEntry.getProductOrder(), testOrder);

        Bucket findBucket = bucketDao.findByName(BucketDaoTest.EXTRACTION_BUCKET_NAME);

        try {
            BucketEntry retrievedEntry2 = bucketEntryDao.findByVesselAndBucket(foundVessel, findBucket);
            Assert.fail("Duplicate Exception Expected");
        } catch (Exception dupeExp) {

        }
    }

    @Test
    public void testUpdateEntry() {

        TwoDBarcodedTube foundVessel = tubeDao.findByBarcode(twoDBarcodeKey);

        Assert.assertNotNull(foundVessel);

        BucketEntry retrievedEntry = bucketEntryDao.findByVesselAndPO(foundVessel, testOrder);

        Assert.assertNotSame(24, retrievedEntry.getProductOrderRanking());

        retrievedEntry.setProductOrderRanking(24);

        bucketEntryDao.flush();
        bucketEntryDao.clear();

        TwoDBarcodedTube newFoundVessel = tubeDao.findByBarcode(twoDBarcodeKey);

        Assert.assertNotNull(foundVessel); 
        testOrder = productOrderDao.findByBusinessKey(testPoBusinessKey);

        BucketEntry newRetrievedEntry = bucketEntryDao.findByVesselAndPO(newFoundVessel, testOrder);
        Assert.assertEquals(24, newRetrievedEntry.getProductOrderRanking().intValue());

        ProductOrder replacementOrder = productOrderDao.findByBusinessKey(testPoBusinessKey + "new");
        if(replacementOrder == null) {
            replacementOrder = ProductOrderTestFactory.createDummyProductOrder(testPoBusinessKey + "new");
            replacementOrder.setProduct(productDao.findByPartNumber(Product.EXOME_EXPRESS_V2_PART_NUMBER));
            replacementOrder.setResearchProject(researchProjectDao.findByTitle("ADHD"));
            replacementOrder.updateAddOnProducts(Collections.<Product>emptyList());
        }
        newRetrievedEntry.setProductOrder(replacementOrder);

        bucketEntryDao.persist(newRetrievedEntry);
        bucketEntryDao.flush();
        bucketEntryDao.clear();

        TwoDBarcodedTube foundVesselAgain = tubeDao.findByBarcode(twoDBarcodeKey);

        replacementOrder = productOrderDao.findByBusinessKey(testPoBusinessKey + "new");

        BucketEntry retrievedEntryAgain = bucketEntryDao.findByVesselAndPO(foundVesselAgain, replacementOrder);

        Assert.assertNotNull(retrievedEntryAgain);

        Assert.assertNotNull(retrievedEntryAgain.getProductOrder());

        Assert.assertEquals(retrievedEntryAgain.getProductOrder().getBusinessKey(), testPoBusinessKey+"new");

    }

    @Test
    public void testRemoveEntry() {

        TwoDBarcodedTube foundVessel = tubeDao.findByBarcode(twoDBarcodeKey);

        Assert.assertNotNull(foundVessel);

        BucketEntry retrievedEntry = bucketEntryDao.findByVesselAndPO(foundVessel, testOrder);

        bucketEntryDao.remove(retrievedEntry);
        bucketEntryDao.flush();
        bucketEntryDao.clear();

        TwoDBarcodedTube newFoundVessel = tubeDao.findByBarcode(twoDBarcodeKey);
        testOrder = productOrderDao.findByBusinessKey(testPoBusinessKey);

        BucketEntry notFoundEntry = bucketEntryDao.findByVesselAndPO(newFoundVessel, testOrder);
        Assert.assertNull(notFoundEntry);

    }

}
