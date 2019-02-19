package org.broadinstitute.gpinformatics.mercury.control.dao.bucket;

import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Test(groups = TestGroups.STUBBY)
@Dependent
public class BucketEntryDaoTest extends StubbyContainerTest {

    public BucketEntryDaoTest(){}

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
    ProductOrderSampleDao productOrderSampleDao;

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
    private BucketEntry testEntry;
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
        if (testBucket == null) {
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
        fetchTestData();
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

    private void fetchTestData() throws InvalidProductException {
        fetchTestData(barcodeKey, sampleKey);
    }

    private BucketEntry fetchTestData(String barcodeKey, String sampleKey) throws InvalidProductException {
        testBucket = bucketDao.findByName(BucketDaoTest.EXTRACTION_BUCKET_NAME);

        testOrder = productOrderDao.findByBusinessKey(testPoBusinessKey);
        if (testOrder == null) {
            testOrder = ProductOrderTestFactory.createDummyProductOrder(testPoBusinessKey);
            testOrder.setTitle(testOrder.getTitle() + today.getTime());
            testOrder.setProduct(productDao.findByBusinessKey(STANDARD_RNA_SEQ_PART_NUMBER));
            testOrder.setResearchProject(researchProjectDao.findByTitle("ADHD"));
            productOrderDao.persist(testOrder);
//            productOrderDao.flush();
        }

        BarcodedTube vessel = tubeDao.findByBarcode(barcodeKey);
        if (vessel == null) {
            vessel = new BarcodedTube(barcodeKey);
            MercurySample mercurySample = new MercurySample(sampleKey, MercurySample.MetadataSource.BSP);
            ProductOrderSample productOrderSample = new ProductOrderSample(sampleKey, new BspSampleData());
            testOrder.addSample(productOrderSample);
            productOrderSampleDao.persist(productOrderSample);
            mercurySample.addProductOrderSample(productOrderSample);
            tubeDao.persist(mercurySample);
            vessel.getMercurySamples().add(mercurySample);
            tubeDao.persist(vessel);
//            tubeDao.flush();
        }

        testEntry = bucketEntryDao.findByVesselAndBucket(vessel, testBucket);
        if (testEntry == null) {
            testEntry = new BucketEntry(vessel, testOrder, testBucket, BucketEntry.BucketEntryType.PDO_ENTRY, 0);
        }
        bucketEntryDao.persist(testEntry);
        bucketEntryDao.flush();
        return testEntry;
    }

    @DataProvider(name = "bucketScenarios")
    public Iterator<Object[]> bucketScenarios() {
        String differentPart = "P-RNA-0001";

        List<ProductOrderSample> productOrderSamples = new ArrayList<>();
        productOrderSamples.add(new ProductOrderSample("SM-1234", new BspSampleData()));
        ProductOrder anotherOrder = new ProductOrder(10_950L, testOrder.getTitle() + today.getTime(),
            productOrderSamples, "GPLB1",
            productDao.findByBusinessKey(differentPart), researchProjectDao.findByTitle("ADHD")
        );

        testBucket = bucketDao.findByName(BucketDaoTest.EXTRACTION_BUCKET_NAME);
        BarcodedTube anotherVessel = new BarcodedTube("A1324"+System.currentTimeMillis());
        anotherVessel.getMercurySamples().add(new MercurySample("SM-1234"+System.currentTimeMillis(), MercurySample.MetadataSource.BSP));
        productOrderDao.persist(anotherOrder);

        BucketEntry anotherEntry =
            new BucketEntry(anotherVessel, anotherOrder, testBucket, BucketEntry.BucketEntryType.PDO_ENTRY, 0);
        bucketEntryDao.persist(anotherEntry);
        bucketEntryDao.flush();
        List<Object[]> testCases = new ArrayList<>();
//        testCases.add(
//            new Object[]{, , });


        return testCases.iterator();
    }

    @Test
    public void testFindBucketEntriesWithVesselList() throws InvalidProductException {
        String suffix = String.valueOf(System.currentTimeMillis());
        String sampleKey1 = "SM-1" + suffix;
        String sampleKey2 = "SM-2" + suffix;
        String sampleKey3 = "SM-3" + suffix;
        String barcode1 = "A1" + suffix;
        String barcode2 = "A2" + suffix;
        String barcode3 = "A3" + suffix;

        BucketEntry testEntry1 = fetchTestData(barcode1, sampleKey1);
        BucketEntry testEntry2 = fetchTestData(barcode2, sampleKey2);
        BucketEntry testEntry3 = fetchTestData(barcode3, sampleKey3);

        List<String> orders = Collections.singletonList(testPoBusinessKey);
        List<BucketEntry> bucketEntries =
            bucketEntryDao.findBucketEntries(testBucket, orders, Arrays.asList(barcode1, barcode2, barcode3));

        Assert.assertTrue(bucketEntries.contains(testEntry1));
        Assert.assertTrue(bucketEntries.contains(testEntry2));
        Assert.assertTrue(bucketEntries.contains(testEntry3));
    }

    @Test
    public void testFindBucketEntriesWithSampleList() throws InvalidProductException {
        String suffix = String.valueOf(System.currentTimeMillis());
        String sampleKey1 = "SM-1" + suffix;
        String sampleKey2 = "SM-2" + suffix;
        String sampleKey3 = "SM-3" + suffix;
        String barcode1 = "A1" + suffix;
        String barcode2 = "A2" + suffix;
        String barcode3 = "A3" + suffix;

        BucketEntry testEntry1 = fetchTestData(barcode1, sampleKey1);
        BucketEntry testEntry2 = fetchTestData(barcode2, sampleKey2);
        BucketEntry testEntry3 = fetchTestData(barcode3, sampleKey3);

        List<String> orders = Collections.singletonList(testPoBusinessKey);
        List<BucketEntry> bucketEntries =
            bucketEntryDao.findBucketEntries(testBucket, orders, Arrays.asList(sampleKey1, sampleKey2, sampleKey3));

        Assert.assertTrue(bucketEntries.contains(testEntry1));
        Assert.assertTrue(bucketEntries.contains(testEntry2));
        Assert.assertTrue(bucketEntries.contains(testEntry3));
    }

    @Test
    public void testFindBucketEntriesWithVesselsAndSamples() throws InvalidProductException {
        String suffix = String.valueOf(System.currentTimeMillis());
        String sampleKey1 = "SM-1" + suffix;
        String sampleKey2 = "SM-2" + suffix;
        String sampleKey3 = "SM-3" + suffix;
        String barcode1 = "A1" + suffix;
        String barcode2 = "A2" + suffix;
        String barcode3 = "A3" + suffix;

        BucketEntry testEntry1 = fetchTestData(barcode1, sampleKey1);
        BucketEntry testEntry2 = fetchTestData(barcode2, sampleKey2);
        BucketEntry testEntry3 = fetchTestData(barcode3, sampleKey3);

        List<String> orders = Collections.singletonList(testPoBusinessKey);
        List<BucketEntry> bucketEntries =
            bucketEntryDao.findBucketEntries(testBucket, orders, Arrays.asList(sampleKey1, barcode1));

        Assert.assertTrue(bucketEntries.contains(testEntry1));
        Assert.assertFalse(bucketEntries.contains(testEntry2));
        Assert.assertFalse(bucketEntries.contains(testEntry3));
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
