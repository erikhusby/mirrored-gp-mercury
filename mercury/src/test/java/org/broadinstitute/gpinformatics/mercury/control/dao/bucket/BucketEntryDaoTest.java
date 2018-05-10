package org.broadinstitute.gpinformatics.mercury.control.dao.bucket;

import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Test(groups = TestGroups.STUBBY)
@RequestScoped
public class BucketEntryDaoTest extends StubbyContainerTest {

    private static final String STANDARD_RNA_SEQ_PART_NUMBER = "P-RNA-0002";
    private static final String STANDARD_RNA_SEQ_PRODUCT_NAME = "Standard RNA Sequencing  - High Coverage (50M pairs)";
    @Inject
    BucketDao bucketDao;

    @Inject
    BucketEntryDao bucketEntryDao;

    @Inject
    BarcodedTubeDao tubeDao;

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    ProductDao productDao;

    @Inject
    ResearchProjectDao researchProjectDao;

    @Inject
    private UserTransaction utx;

    private Bucket testBucket;
    private String barcodeKey;
    private String testPoBusinessKey;
    private ProductOrder testOrder;
    private Date today;
    private String sampleKey;

    @BeforeMethod(groups = TestGroups.STUBBY)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();


        testBucket = bucketDao.findByName(BucketDaoTest.EXTRACTION_BUCKET_NAME);
        if(testBucket == null) {
            WorkflowBucketDef bucketDef = new WorkflowBucketDef(BucketDaoTest.EXTRACTION_BUCKET_NAME);

            testBucket = new Bucket(bucketDef);
            bucketDao.persist(testBucket);
            bucketDao.flush();
            bucketDao.clear();
            testBucket = bucketDao.findByName(BucketDaoTest.EXTRACTION_BUCKET_NAME);
        }

        today = new Date();
        String suffix = "1493" + today.getTime();
        barcodeKey = "A" + suffix;
        sampleKey = "SM-" + suffix;
        testPoBusinessKey = "PDO-33";
        testOrder = productOrderDao.findByBusinessKey(testPoBusinessKey);
        if(testOrder == null) {
            testOrder = ProductOrderTestFactory.createDummyProductOrder(testPoBusinessKey);
            testOrder.setTitle(testOrder.getTitle() + today.getTime());
            testOrder.setProduct(productDao.findByBusinessKey(STANDARD_RNA_SEQ_PART_NUMBER));
            testOrder.setResearchProject(researchProjectDao.findByTitle("ADHD"));
        }
        BarcodedTube vessel = new BarcodedTube(barcodeKey);
        vessel.getMercurySamples().add(new MercurySample(sampleKey, MercurySample.MetadataSource.BSP));

        BucketEntry testEntry = new BucketEntry(vessel, testOrder, testBucket, BucketEntry.BucketEntryType.PDO_ENTRY);
        bucketEntryDao.persist(testEntry);
        bucketEntryDao.flush();
        bucketEntryDao.clear();

    }

    @AfterMethod(groups = TestGroups.STUBBY)
    public void tearDownAndRollback() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    @Test
    public void testFindEntry() {

        BarcodedTube foundVessel = tubeDao.findByBarcode(barcodeKey);

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

        BucketEntry retrievedEntry2 = bucketEntryDao.findByVesselAndBucket(foundVessel, testBucket);

        Assert.assertNotNull(retrievedEntry2);
        Assert.assertNotNull(retrievedEntry2.getBucket());
        Assert.assertNotNull(retrievedEntry2.getCreatedDate());

        Assert.assertEquals(dateFormatter.format(new Date()), dateFormatter.format(
                retrievedEntry2
                        .getCreatedDate()));
    }


    @Test
    public void testFindByBucketAndSample() {
        List<BucketEntry> bySampleAndBucket =
            bucketEntryDao.findBySampleAndBucket(Collections.singletonList(sampleKey), testBucket);
        Assert.assertEquals(bySampleAndBucket.size(), 1);
        validateEntry(bySampleAndBucket.iterator().next());
    }

    @Test
    public void testFindByJiraKeyAndBucket() {
        List<BucketEntry> bySampleAndBucket =
            bucketEntryDao.findByProductOrderAndBucket(Collections.singletonList(testPoBusinessKey), testBucket);
        Assert.assertEquals(bySampleAndBucket.size(), 1);
        validateEntry(bySampleAndBucket.iterator().next());
    }

    @Test
    public void testFindByPDOTitleAndBucket() {
        List<BucketEntry> bySampleAndBucket =
            bucketEntryDao.findByProductOrderAndBucket(Collections.singletonList(testPoBusinessKey), testBucket);
        Assert.assertEquals(bySampleAndBucket.size(), 1);
        validateEntry(bySampleAndBucket.iterator().next());
    }

    @Test
    public void testFindByPartNumberAndBucket() {
        List<BucketEntry> bySampleAndBucket =
            bucketEntryDao.findByProductAndBucket(Collections.singletonList(STANDARD_RNA_SEQ_PART_NUMBER), testBucket);
        Assert.assertEquals(bySampleAndBucket.size(), 1);
        validateEntry(bySampleAndBucket.iterator().next());
    }

    @Test
    public void testFindProductNameAndBucket() {
        List<BucketEntry> bySampleAndBucket =
            bucketEntryDao.findByProductAndBucket(Collections.singletonList(STANDARD_RNA_SEQ_PRODUCT_NAME), testBucket);
        Assert.assertEquals(bySampleAndBucket.size(), 1);
        validateEntry(bySampleAndBucket.iterator().next());
    }

    @Test
    public void testFindPartialProductAndBucket() throws Exception {
        String differentPart = "P-RNA-0001";
        String differentProductName = "Standard RNA Sequencing - Low Coverage (15M pairs)";
        ProductOrder anotherOrder = ProductOrderTestFactory.createDummyProductOrder(testPoBusinessKey + 33);

        anotherOrder.setTitle(testOrder.getTitle() + today.getTime());
        anotherOrder.setProduct(productDao.findByBusinessKey(differentPart));
        anotherOrder.setResearchProject(researchProjectDao.findByTitle("ADHD"));

        testBucket = bucketDao.findByName(BucketDaoTest.EXTRACTION_BUCKET_NAME);
        BarcodedTube anotherVessel = new BarcodedTube("A1324"+System.currentTimeMillis());
        anotherVessel.getMercurySamples().add(new MercurySample("SM-1234"+System.currentTimeMillis(), MercurySample.MetadataSource.BSP));

        productOrderDao.persist(anotherOrder);
        productOrderDao.flush();
        productOrderDao.clear();

        BucketEntry anotherEntry = new BucketEntry(anotherVessel, anotherOrder, testBucket, BucketEntry.BucketEntryType.PDO_ENTRY);
        bucketEntryDao.persist(anotherEntry);
        bucketEntryDao.flush();
        bucketEntryDao.clear();

        List<BucketEntry> bySampleAndBucket =
            bucketEntryDao.findByProductAndBucket(Collections.singletonList("Standard RNA Sequencing - Low Coverage"), testBucket);
        Assert.assertEquals(bySampleAndBucket.size(), 2);

        List<String> products = Arrays.asList(differentProductName, STANDARD_RNA_SEQ_PRODUCT_NAME);

        bySampleAndBucket.forEach(bucketEntry -> {
            String productName = bucketEntry.getProductOrder().getProduct().getProductName();
            boolean removed = products.remove(productName);
            Assert.assertTrue(removed, String.format("Expected bucket entry with product %s was not found", productName));
        });
    }

    protected void validateEntry(BucketEntry entry) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yy");
        Assert.assertEquals(entry.getBucket(), testBucket);
        Assert.assertEquals(entry.getProductOrder(), testOrder);
        Assert.assertEquals(entry.getProductOrder().getJiraTicketKey(), testPoBusinessKey);
        Assert.assertEquals(entry.getProductOrder().getProduct().getPartNumber(), STANDARD_RNA_SEQ_PART_NUMBER);
        Assert.assertEquals(entry.getProductOrder().getProduct().getProductName(), STANDARD_RNA_SEQ_PRODUCT_NAME);
        Assert.assertEquals(dateFormatter.format(new Date()), dateFormatter.format(entry.getCreatedDate()));
        Assert.assertEquals(entry.getStatus(), BucketEntry.Status.Active);
        Assert.assertEquals(entry.getLabVessel().getLabel(), barcodeKey);
        Assert.assertEquals(entry.getLabVessel().getMercurySamples().iterator().next().getSampleKey(), sampleKey);
    }

    @Test
    public void testFindDuplicate() {

        BarcodedTube vesselToDupe = tubeDao.findByBarcode(barcodeKey);
        ProductOrder testDupeOrder = productOrderDao.findByBusinessKey(testPoBusinessKey + "dupe");
        if(testDupeOrder == null) {
            testDupeOrder = ProductOrderTestFactory.createDummyProductOrder(testPoBusinessKey + "dupe");
            testDupeOrder.setTitle(testDupeOrder.getTitle() + today.getTime());
            try {
                testDupeOrder.setProduct(productDao.findByPartNumber(STANDARD_RNA_SEQ_PART_NUMBER));
            } catch (InvalidProductException e) {
                Assert.fail(e.getMessage());
            }

            testDupeOrder.setResearchProject(researchProjectDao.findByTitle("ADHD"));
            testDupeOrder.updateAddOnProducts(Collections.<Product>emptyList());
        }
        testBucket = bucketDao.findByName(BucketDaoTest.EXTRACTION_BUCKET_NAME);
        BucketEntry testEntry = new BucketEntry(vesselToDupe, testDupeOrder, testBucket,
                                                BucketEntry.BucketEntryType.PDO_ENTRY);

        bucketEntryDao.persist(testEntry);
        bucketEntryDao.flush();
        bucketEntryDao.clear();

        BarcodedTube foundVessel = tubeDao.findByBarcode(barcodeKey);

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

        BarcodedTube foundVessel = tubeDao.findByBarcode(barcodeKey);

        Assert.assertNotNull(foundVessel);

        BucketEntry retrievedEntry = bucketEntryDao.findByVesselAndPO(foundVessel, testOrder);

        Assert.assertNotSame(24, retrievedEntry.getProductOrderRanking());

        retrievedEntry.setProductOrderRanking(24);

        bucketEntryDao.flush();
        bucketEntryDao.clear();

        BarcodedTube newFoundVessel = tubeDao.findByBarcode(barcodeKey);

        Assert.assertNotNull(foundVessel); 
        testOrder = productOrderDao.findByBusinessKey(testPoBusinessKey);

        BucketEntry newRetrievedEntry = bucketEntryDao.findByVesselAndPO(newFoundVessel, testOrder);
        Assert.assertEquals(24, newRetrievedEntry.getProductOrderRanking().intValue());

        ProductOrder replacementOrder = productOrderDao.findByBusinessKey(testPoBusinessKey + "new");
        if(replacementOrder == null) {
            replacementOrder = ProductOrderTestFactory.createDummyProductOrder(testPoBusinessKey + "new");
            replacementOrder.setTitle(replacementOrder.getTitle() + today.getTime());
            try {
                replacementOrder.setProduct(productDao.findByPartNumber(STANDARD_RNA_SEQ_PART_NUMBER));
            } catch (InvalidProductException e) {
                Assert.fail(e.getMessage());
            }
            replacementOrder.setResearchProject(researchProjectDao.findByTitle("ADHD"));
            replacementOrder.updateAddOnProducts(Collections.<Product>emptyList());
        }
        newRetrievedEntry.setProductOrder(replacementOrder);

        bucketEntryDao.persist(newRetrievedEntry);
        bucketEntryDao.flush();
        bucketEntryDao.clear();

        BarcodedTube foundVesselAgain = tubeDao.findByBarcode(barcodeKey);

        replacementOrder = productOrderDao.findByBusinessKey(testPoBusinessKey + "new");

        BucketEntry retrievedEntryAgain = bucketEntryDao.findByVesselAndPO(foundVesselAgain, replacementOrder);

        Assert.assertNotNull(retrievedEntryAgain);

        Assert.assertNotNull(retrievedEntryAgain.getProductOrder());

        Assert.assertEquals(retrievedEntryAgain.getProductOrder().getBusinessKey(), testPoBusinessKey+"new");

    }

    @Test
    public void testRemoveEntry() {

        BarcodedTube foundVessel = tubeDao.findByBarcode(barcodeKey);

        Assert.assertNotNull(foundVessel);

        BucketEntry retrievedEntry = bucketEntryDao.findByVesselAndPO(foundVessel, testOrder);

        bucketEntryDao.remove(retrievedEntry);
        bucketEntryDao.flush();
        bucketEntryDao.clear();

        BarcodedTube newFoundVessel = tubeDao.findByBarcode(barcodeKey);
        testOrder = productOrderDao.findByBusinessKey(testPoBusinessKey);

        BucketEntry notFoundEntry = bucketEntryDao.findByVesselAndPO(newFoundVessel, testOrder);
        Assert.assertNull(notFoundEntry);

    }

}
