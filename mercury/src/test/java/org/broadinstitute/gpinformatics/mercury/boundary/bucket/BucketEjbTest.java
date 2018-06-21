package org.broadinstitute.gpinformatics.mercury.boundary.bucket;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.test.ExomeExpressV2EndToEndTest;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *  This test is singleThreaded because subsequent test methods are called before the @AfterMethod of the previous test method call is complete <br/>
 *  Lifecycle @AfterMethod operates on the injected UserTransaction instance variable while subsequent methods are performing persistence operations.
 *  The previous @AfterMethod rollback call is incomplete so unique constraints are violated.
 */
@Test(groups = TestGroups.STUBBY, singleThreaded = true)
@RequestScoped
public class BucketEjbTest extends StubbyContainerTest {

    public BucketEjbTest(){}

    @Inject
    BucketEjb resource;

    @Inject
    BucketDao bucketDao;

    @Inject
    BucketEntryDao bucketEntryDao;

    @Inject
    ProductDao productDao;

    @Inject
    ResearchProjectDao researchProjectDao;

    @Inject
    BarcodedTubeDao barcodedTubeDao;

    private final static Log logger = LogFactory.getLog(BucketEjbTest.class);

    @Inject
    private UserTransaction utx;
    private Bucket bucket;
    private static final String PICO_PLATING_BUCKET = "Pico/Plating Bucket";
    private String howieTest;
    private String poBusinessKey1;
    private String poBusinessKey2;
    private String poBusinessKey3;
    private String barcode1;
    private String barcode2;
    private String barcode3;
    private String barcode4;
    private String hrafalUserName;
    private ProductOrder productOrder1;
    private ProductOrder productOrder2;
    private ProductOrder productOrder3;
    private BarcodedTube bspAliquot4;
    private BarcodedTube bspAliquot3;
    private BarcodedTube bspAliquot2;
    private BarcodedTube bspAliquot1;

    @BeforeMethod
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();

        List<ProductOrderSample> productOrderSamples = new ArrayList<>();
        poBusinessKey1 = "PDO-8";
        poBusinessKey2 = "PDO-9";
        poBusinessKey3 = "PDO-10";

        long timestamp = (new Date()).getTime();

        productOrder1 = new ProductOrder(101L, "Test PO1", productOrderSamples, "GSP-123",
                                         productDao.findByBusinessKey(Product.EXOME_EXPRESS_V2_PART_NUMBER),
                                         researchProjectDao.findByTitle("ADHD"));
        productOrder1.setTitle(productOrder1.getTitle() + timestamp);

        timestamp += 1000;
        productOrder2 = new ProductOrder(101L, "Test PO2", productOrderSamples, "GSP-123",
                                         productDao.findByBusinessKey(Product.EXOME_EXPRESS_V2_PART_NUMBER),
                                         researchProjectDao.findByTitle("ADHD"));
        productOrder2.setTitle(productOrder2.getTitle() + timestamp);

        timestamp += 1000;
        productOrder3 = new ProductOrder(101L, "Test PO3", productOrderSamples, "GSP-123",
                                         productDao.findByBusinessKey(Product.EXOME_EXPRESS_V2_PART_NUMBER),
                                         researchProjectDao.findByTitle("ADHD"));
        productOrder3.setTitle(productOrder3.getTitle() + timestamp);

        productOrder1.setJiraTicketKey(poBusinessKey1);
        productOrder1.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrder2.setJiraTicketKey(poBusinessKey2);
        productOrder2.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrder3.setJiraTicketKey(poBusinessKey3);
        productOrder3.setOrderStatus(ProductOrder.OrderStatus.Submitted);


        barcode1 = "5234623632";
        barcode2 = "6727357836";
        barcode3 = "6625345234";
        barcode4 = "9202340293";

        List<String> shearingTubeBarcodes = new ArrayList<>()/*Arrays.asList("SH1", "SH2", "SH3")*/;
        Map<String, String> barcodesByRackPositions = new HashMap<>();


        Map<String, BarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();


        shearingTubeBarcodes.add(barcode1);

        String column = ExomeExpressV2EndToEndTest.RACK_COLUMNS.get((0) / ExomeExpressV2EndToEndTest.RACK_ROWS.size());
        String row = ExomeExpressV2EndToEndTest.RACK_ROWS.get((0) % ExomeExpressV2EndToEndTest.RACK_ROWS.size());

        //
        String bspStock = "SM-" + barcode1;

        barcodesByRackPositions.put(row + column, barcode1);

        productOrderSamples.add(new ProductOrderSample(bspStock));
        bspAliquot1 = new BarcodedTube(barcode1);

        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<>();
        dataMap.put(BSPSampleSearchColumn.MATERIAL_TYPE, MaterialType.DNA_DNA_GENOMIC.getDisplayName());
        BspSampleData bspSampleData = new BspSampleData(dataMap);
        bspAliquot1.addSample(new MercurySample(bspStock, bspSampleData));
        mapBarcodeToTube.put(barcode1, bspAliquot1);


        shearingTubeBarcodes.add(barcode2);

        column = ExomeExpressV2EndToEndTest.RACK_COLUMNS.get((0) / ExomeExpressV2EndToEndTest.RACK_ROWS.size());
        row = ExomeExpressV2EndToEndTest.RACK_ROWS.get((0) % ExomeExpressV2EndToEndTest.RACK_ROWS.size());

        bspStock = "SM-" + barcode2;

        barcodesByRackPositions.put(row + column, barcode2);

        productOrderSamples.add(new ProductOrderSample(bspStock));
        bspAliquot2 = new BarcodedTube(barcode2);
        bspAliquot2.addSample(new MercurySample(bspStock, bspSampleData));
        mapBarcodeToTube.put(barcode2, bspAliquot2);


        shearingTubeBarcodes.add(barcode3);

        column = ExomeExpressV2EndToEndTest.RACK_COLUMNS.get((0) / ExomeExpressV2EndToEndTest.RACK_ROWS.size());
        row = ExomeExpressV2EndToEndTest.RACK_ROWS.get((0) % ExomeExpressV2EndToEndTest.RACK_ROWS.size());

        bspStock = "SM-" + barcode3;

        barcodesByRackPositions.put(row + column, barcode3);

        productOrderSamples.add(new ProductOrderSample(bspStock));
        bspAliquot3 = new BarcodedTube(barcode3);
        bspAliquot3.addSample(new MercurySample(bspStock, bspSampleData));
        mapBarcodeToTube.put(barcode3, bspAliquot3);


        shearingTubeBarcodes.add(barcode4);

        column = ExomeExpressV2EndToEndTest.RACK_COLUMNS.get((0) / ExomeExpressV2EndToEndTest.RACK_ROWS.size());
        row = ExomeExpressV2EndToEndTest.RACK_ROWS.get((0) % ExomeExpressV2EndToEndTest.RACK_ROWS.size());

        bspStock = "SM-" + barcode4;

        barcodesByRackPositions.put(row + column, barcode4);

        productOrderSamples.add(new ProductOrderSample(bspStock));
        bspAliquot4 = new BarcodedTube(barcode4);
        bspAliquot4.addSample(new MercurySample(bspStock, bspSampleData));
        mapBarcodeToTube.put(barcode4, bspAliquot4);

        hrafalUserName = "hrafal";
        howieTest = hrafalUserName;
    }

    @AfterMethod
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    public void testResource_start_entries() throws Exception {
        bucket = bucketDao.findByName(PICO_PLATING_BUCKET);
        int originalBucketSize = bucket.getBucketEntries().size();

        Pair<ProductWorkflowDefVersion, Collection<BucketEntry>> workflowBucketEntriesPair =
                resource.applyBucketCriteria(Collections.<LabVessel>singletonList(bspAliquot1), productOrder1,
                        howieTest, ProductWorkflowDefVersion.BucketingSource.PDO_SUBMISSION, new Date());
        Thread.sleep(2L); // Pause to prevent events with same location, date, and disambiguator rejected as duplicate by Oracle
        Collection<BucketEntry> testEntries1 = workflowBucketEntriesPair.getRight();
        Assert.assertEquals(testEntries1.size(), 1);
        BucketEntry testEntry1 = testEntries1.iterator().next();

        Assert.assertEquals(originalBucketSize + 1, bucket.getBucketEntries().size());
        Assert.assertTrue(bucket.contains(testEntry1));
        
        Pair<ProductWorkflowDefVersion, Collection<BucketEntry>> workflowBucketEntriesPair2 =
                resource.applyBucketCriteria( Collections.<LabVessel>singletonList(bspAliquot2), productOrder2,
                        howieTest, ProductWorkflowDefVersion.BucketingSource.PDO_SUBMISSION, new Date());
        Thread.sleep(2L);
        Collection<BucketEntry> testEntries2 = workflowBucketEntriesPair2.getRight();
        Assert.assertEquals(testEntries2.size(), 1);
        BucketEntry testEntry2 = testEntries2.iterator().next();

        Pair<ProductWorkflowDefVersion, Collection<BucketEntry>> workflowBucketEntriesPair3 =
                resource.applyBucketCriteria(Collections.<LabVessel>singletonList(bspAliquot3), productOrder3,
                        howieTest, ProductWorkflowDefVersion.BucketingSource.PDO_SUBMISSION, new Date());
        Thread.sleep(2L);
        Collection<BucketEntry> testEntries3 = workflowBucketEntriesPair3.getRight();
        Assert.assertEquals(testEntries3.size(), 1);
        BucketEntry testEntry3 = testEntries3.iterator().next();

        Pair<ProductWorkflowDefVersion, Collection<BucketEntry>> workflowBucketEntriesPair4 =
                resource.applyBucketCriteria(Collections.<LabVessel>singletonList(bspAliquot4), productOrder3,
                        howieTest, ProductWorkflowDefVersion.BucketingSource.PDO_SUBMISSION, new Date());
        Thread.sleep(2L);
        Collection<BucketEntry> testEntries4 = workflowBucketEntriesPair4.getRight();

        Assert.assertEquals(testEntries4.size(), 1);
        BucketEntry testEntry4 = testEntries4.iterator().next();

        Pair<ProductWorkflowDefVersion, Collection<BucketEntry>> workflowBucketEntriesPair5 =
                resource.applyBucketCriteria(Collections.<LabVessel>singletonList(bspAliquot1), productOrder1,
                        howieTest, ProductWorkflowDefVersion.BucketingSource.PDO_SUBMISSION, new Date());
        Thread.sleep(2L);
        Collection<BucketEntry> duplicateEntry = workflowBucketEntriesPair5.getRight();
        Assert.assertTrue(duplicateEntry.isEmpty());
        
        Assert.assertTrue(bucket.contains(testEntry2));
        Assert.assertTrue(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry4));

        Set<BucketEntry> bucketBatch = new HashSet<>();

        Assert.assertTrue(Collections.addAll(bucketBatch, testEntry1, testEntry2, testEntry3));

        Assert.assertFalse(testEntry1.getLabVessel().getInPlaceEventsWithContainers().isEmpty());
        Assert.assertEquals(1, testEntry1.getLabVessel().getInPlaceEventsWithContainers().size());
        Assert.assertFalse(testEntry2.getLabVessel().getInPlaceEventsWithContainers().isEmpty());
        Assert.assertEquals(1, testEntry2.getLabVessel().getInPlaceEventsWithContainers().size());
        Assert.assertFalse(testEntry3.getLabVessel().getInPlaceEventsWithContainers().isEmpty());
        Assert.assertEquals(1, testEntry3.getLabVessel().getInPlaceEventsWithContainers().size());
        Assert.assertFalse(testEntry4.getLabVessel().getInPlaceEventsWithContainers().isEmpty());
        Assert.assertEquals(1, testEntry4.getLabVessel().getInPlaceEventsWithContainers().size());

        resource.moveFromBucketToCommonBatch(bucketBatch);

        Assert.assertFalse(testEntry1.getLabVessel().getInPlaceEventsWithContainers().isEmpty());
        Assert.assertFalse(testEntry2.getLabVessel().getInPlaceEventsWithContainers().isEmpty());
        Assert.assertFalse(testEntry3.getLabVessel().getInPlaceEventsWithContainers().isEmpty());
        Assert.assertFalse(testEntry4.getLabVessel().getInPlaceEventsWithContainers().isEmpty());

        Assert.assertFalse(bucket.contains(testEntry1));
        Assert.assertFalse(bucket.contains(testEntry2));
        Assert.assertFalse(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry4));

        resource.removeEntry(testEntry4, "Because the test told me to!!!");

        Assert.assertFalse(bucket.contains(testEntry4));

        Assert.assertEquals(originalBucketSize, bucket.getBucketEntries().size());
    }


    public void testResource_start_vessels() throws Exception {
        bucket = bucketDao.findByName(PICO_PLATING_BUCKET);
        int originalBucketSize = bucket.getBucketEntries().size();

        Pair<ProductWorkflowDefVersion, Collection<BucketEntry>> workflowBucketEntriesPair =
                resource.applyBucketCriteria(Collections.<LabVessel>singletonList(bspAliquot1), productOrder1,
                        howieTest, ProductWorkflowDefVersion.BucketingSource.PDO_SUBMISSION, new Date());
        Thread.sleep(2L); // Pause to prevent events with same location, date, and disambiguator rejected as duplicate by Oracle
        Collection<BucketEntry> testEntries1 = workflowBucketEntriesPair.getRight();
        Assert.assertEquals(testEntries1.size(), 1);
        BucketEntry testEntry1 = testEntries1.iterator().next();

        Assert.assertEquals(originalBucketSize + 1, bucket.getBucketEntries().size());
        Assert.assertTrue(bucket.contains(testEntry1));

        BucketEntry testEntry2;
        BucketEntry testEntry3;
        BucketEntry testEntry4;
        
        List<LabVessel> bucketCreateBatch = new LinkedList<>();

        Assert.assertTrue(Collections.addAll(bucketCreateBatch, bspAliquot2,
                bspAliquot3, bspAliquot4));

        resource.applyBucketCriteria(bucketCreateBatch, productOrder3, howieTest,
                ProductWorkflowDefVersion.BucketingSource.PDO_SUBMISSION, new Date());
        Thread.sleep(2L);

        bucketDao.flush();
        bucketDao.clear();
        bucket = bucketDao.findByName(PICO_PLATING_BUCKET);

        LabVessel vessel1 = barcodedTubeDao.findByBarcode(barcode1);
        LabVessel vessel2 = barcodedTubeDao.findByBarcode(barcode2);
        LabVessel vessel3 = barcodedTubeDao.findByBarcode(barcode3);
        LabVessel vessel4 = barcodedTubeDao.findByBarcode(barcode4);

        testEntry1 = bucketEntryDao.findByVesselAndBucket(vessel1, bucket);
        testEntry2 = bucketEntryDao.findByVesselAndBucket(vessel2, bucket);
        testEntry3 = bucketEntryDao.findByVesselAndBucket(vessel3, bucket);
        testEntry4 = bucketEntryDao.findByVesselAndBucket(vessel4, bucket);

        Set<BucketEntry> bucketBatch = new HashSet<>();
        Assert.assertTrue(Collections.addAll(bucketBatch, testEntry1, testEntry2, testEntry3));

        Assert.assertTrue(bucket.contains(testEntry2));
        Assert.assertTrue(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry4));

        Set<LabVessel> vesselBucketBatch = new HashSet<>();

        Assert.assertTrue(Collections.addAll(vesselBucketBatch, vessel1, vessel2, vessel3));

        Assert.assertFalse(vessel1.getInPlaceEventsWithContainers().isEmpty());
        Assert.assertEquals(1, vessel1.getInPlaceEventsWithContainers().size());
        Assert.assertFalse(vessel2.getInPlaceEventsWithContainers().isEmpty());
        Assert.assertEquals(1, vessel2.getInPlaceEventsWithContainers().size());
        Assert.assertFalse(vessel3.getInPlaceEventsWithContainers().isEmpty());
        Assert.assertEquals(1, vessel3.getInPlaceEventsWithContainers().size());
        Assert.assertFalse(vessel4.getInPlaceEventsWithContainers().isEmpty());
        Assert.assertEquals(1, vessel4.getInPlaceEventsWithContainers().size());

        resource.createEntriesAndBatchThem(vesselBucketBatch, bucket);

        bucketDao.flush();
        bucketDao.clear();
        bucket = bucketDao.findByName(PICO_PLATING_BUCKET);
        vessel1 = barcodedTubeDao.findByBarcode(barcode1);
        vessel2 = barcodedTubeDao.findByBarcode(barcode2);
        vessel3 = barcodedTubeDao.findByBarcode(barcode3);
        vessel4 = barcodedTubeDao.findByBarcode(barcode4);

        Assert.assertFalse(vessel1.getInPlaceEventsWithContainers().isEmpty());
        Assert.assertFalse(vessel2.getInPlaceEventsWithContainers().isEmpty());
        Assert.assertFalse(vessel3.getInPlaceEventsWithContainers().isEmpty());
        Assert.assertFalse(vessel4.getInPlaceEventsWithContainers().isEmpty());

        testEntry1 = bucketEntryDao.findByVesselAndBucket(vessel1, bucket);
        testEntry2 = bucketEntryDao.findByVesselAndBucket(vessel2, bucket);
        testEntry3 = bucketEntryDao.findByVesselAndBucket(vessel3, bucket);
        testEntry4 = bucketEntryDao.findByVesselAndBucket(vessel4, bucket);

        Assert.assertNotNull(testEntry1);
        Assert.assertNotNull(testEntry2);
        Assert.assertNotNull(testEntry3);
        Assert.assertNotNull(testEntry4);

        Assert.assertFalse(bucket.contains(testEntry1));
        Assert.assertFalse(bucket.contains(testEntry2));
        Assert.assertFalse(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry4));

        testEntry4 = bucketEntryDao.findByVesselAndBucket(vessel4, bucket);
        resource.removeEntry(testEntry4, "Because the test told me to!!!");

        bucketDao.flush();
        bucketDao.clear();
        bucket = bucketDao.findByName(PICO_PLATING_BUCKET);

        testEntry4 = bucketEntryDao.findByVesselAndBucket(vessel4, bucket);
        Assert.assertNotNull(testEntry4);
        Assert.assertFalse(bucket.contains(testEntry4));

        Assert.assertEquals(originalBucketSize, bucket.getBucketEntries().size());
    }


    public void testResource_start_vessel_count() throws Exception {
        bucket = bucketDao.findByName(PICO_PLATING_BUCKET);
        int originalBucketSize = bucket.getBucketEntries().size();
        
        Pair<ProductWorkflowDefVersion, Collection<BucketEntry>> workflowBucketEntriesPair =
                resource.applyBucketCriteria(Collections.<LabVessel>singletonList(bspAliquot1), productOrder1,
                        howieTest, ProductWorkflowDefVersion.BucketingSource.PDO_SUBMISSION, new Date());
        Thread.sleep(2L); // Pause to prevent events with same location, date, and disambiguator rejected as duplicate by Oracle
        Collection<BucketEntry> testEntries1 = workflowBucketEntriesPair.getRight();

        Assert.assertEquals(testEntries1.size(), 1);
        BucketEntry testEntry1 = testEntries1.iterator().next();

        Assert.assertEquals(originalBucketSize + 1, bucket.getBucketEntries().size());
        Assert.assertTrue(bucket.contains(testEntry1));

        BucketEntry testEntry2;
        BucketEntry testEntry3;
        BucketEntry testEntry4;

        List<LabVessel> bucketCreateBatch = new LinkedList<>();

        Assert.assertTrue(Collections.addAll(bucketCreateBatch, bspAliquot2, bspAliquot3, bspAliquot4));
        resource.applyBucketCriteria(bucketCreateBatch, productOrder3, howieTest,
                ProductWorkflowDefVersion.BucketingSource.PDO_SUBMISSION, new Date());
        Thread.sleep(2L);

        bucketDao.flush();
        bucketDao.clear();

        bucket = bucketDao.findByName(PICO_PLATING_BUCKET);


        LabVessel vessel1 = barcodedTubeDao.findByBarcode(barcode1);
        LabVessel vessel2 = barcodedTubeDao.findByBarcode(barcode2);
        LabVessel vessel3 = barcodedTubeDao.findByBarcode(barcode3);
        LabVessel vessel4 = barcodedTubeDao.findByBarcode(barcode4);

        testEntry1 = bucketEntryDao.findByVesselAndBucket(vessel1, bucket);
        testEntry2 = bucketEntryDao.findByVesselAndBucket(vessel2, bucket);
        testEntry3 = bucketEntryDao.findByVesselAndBucket(vessel3, bucket);
        testEntry4 = bucketEntryDao.findByVesselAndBucket(vessel4, bucket);

        Set<BucketEntry> bucketBatch = new HashSet<>();
        Assert.assertTrue(Collections.addAll(bucketBatch, testEntry1, testEntry2, testEntry3));

        Assert.assertTrue(bucket.contains(testEntry2));
        Assert.assertTrue(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry4));


        Assert.assertFalse(testEntry1.getLabVessel().getInPlaceEventsWithContainers().isEmpty());
        Assert.assertEquals(1, testEntry1.getLabVessel().getInPlaceEventsWithContainers().size());
        Assert.assertFalse(testEntry2.getLabVessel().getInPlaceEventsWithContainers().isEmpty());
        Assert.assertEquals(1, testEntry2.getLabVessel().getInPlaceEventsWithContainers().size());
        Assert.assertFalse(testEntry3.getLabVessel().getInPlaceEventsWithContainers().isEmpty());
        Assert.assertEquals(1, testEntry3.getLabVessel().getInPlaceEventsWithContainers().size());
        Assert.assertFalse(testEntry4.getLabVessel().getInPlaceEventsWithContainers().isEmpty());
        Assert.assertEquals(1, testEntry4.getLabVessel().getInPlaceEventsWithContainers().size());

        logger.info("Before the start method.  The bucket has " + bucket.getBucketEntries().size() + " Entries in it");

    }
}
