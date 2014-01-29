package org.broadinstitute.gpinformatics.mercury.boundary.bucket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.test.ExomeExpressV2EndToEndTest;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertTrue;

/**
 * @author Scott Matthews
 *         Date: 10/26/12
 *         Time: 2:10 PM
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BucketEjbTest extends ContainerTest {

    @Inject
    BucketEjb resource;

    @Inject
    BucketDao bucketDao;

    @Inject
    BucketEntryDao bucketEntryDao;

    @Inject
    TwoDBarcodedTubeDao twoDBarcodedTubeDao;

    private final static Log logger = LogFactory.getLog(BucketEjbTest.class);

    @Inject
    private UserTransaction utx;
    private Bucket bucket;
    private String bucketCreationName;
    private String howieTest;
    private String poBusinessKey1;
    private String poBusinessKey2;
    private String poBusinessKey3;
    private String twoDBarcode1;
    private String twoDBarcode2;
    private String twoDBarcode3;
    private String twoDBarcode4;
    private String hrafalUserName;
    private ProductOrder productOrder1;
    private ProductOrder productOrder2;
    private ProductOrder productOrder3;
    private TwoDBarcodedTube bspAliquot4;
    private TwoDBarcodedTube bspAliquot3;
    private TwoDBarcodedTube bspAliquot2;
    private TwoDBarcodedTube bspAliquot1;

    @BeforeMethod
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

//        utx.setTransactionTimeout(300);
        utx.begin();

        List<ProductOrderSample> productOrderSamples = new ArrayList<>();
        poBusinessKey1 = "PDO-8";
        poBusinessKey2 = "PDO-9";
        poBusinessKey3 = "PDO-10";

        productOrder1 = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123", new Product(
                "Test product", new ProductFamily("Test product family"), "test", "1234", null, null, 10000, 20000, 100,
                40, null, null, true, Workflow.AGILENT_EXOME_EXPRESS, false, "agg type"),
                new ResearchProject(101L, "Test RP", "Test synopsis",
                        false));
        productOrder2 = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123", new Product(
                "Test product", new ProductFamily("Test product family"), "test", "1234", null, null, 10000, 20000, 100,
                40, null, null, true, Workflow.AGILENT_EXOME_EXPRESS, false, "agg type"),
                new ResearchProject(101L, "Test RP", "Test synopsis",
                        false));
        productOrder3 = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123", new Product(
                "Test product", new ProductFamily("Test product family"), "test", "1234", null, null, 10000, 20000, 100,
                40, null, null, true, Workflow.AGILENT_EXOME_EXPRESS, false, "agg type"),
                new ResearchProject(101L, "Test RP", "Test synopsis",
                        false));

        productOrder1.setJiraTicketKey(poBusinessKey1);
        productOrder1.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrder2.setJiraTicketKey(poBusinessKey2);
        productOrder2.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrder3.setJiraTicketKey(poBusinessKey3);
        productOrder3.setOrderStatus(ProductOrder.OrderStatus.Submitted);


        twoDBarcode1 = "5234623632";
        twoDBarcode2 = "6727357836";
        twoDBarcode3 = "6625345234";
        twoDBarcode4 = "9202340293";

        List<String> shearingTubeBarcodes = new ArrayList<>()/*Arrays.asList("SH1", "SH2", "SH3")*/;
        Map<String, String> barcodesByRackPositions = new HashMap<>();


        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();


        shearingTubeBarcodes.add(twoDBarcode1);

        String column = ExomeExpressV2EndToEndTest.RACK_COLUMNS.get((0) / ExomeExpressV2EndToEndTest.RACK_ROWS.size());
        String row = ExomeExpressV2EndToEndTest.RACK_ROWS.get((0) % ExomeExpressV2EndToEndTest.RACK_ROWS.size());

        //
        String bspStock = "SM-" + twoDBarcode1;

        barcodesByRackPositions.put(row + column, twoDBarcode1);

        productOrderSamples.add(new ProductOrderSample(bspStock));
        bspAliquot1 = new TwoDBarcodedTube(twoDBarcode1);
        bspAliquot1.addSample(new MercurySample(bspStock));
        bspAliquot1.addBucketEntry(new BucketEntry(bspAliquot1, poBusinessKey1, BucketEntry.BucketEntryType.PDO_ENTRY));
        mapBarcodeToTube.put(twoDBarcode1, bspAliquot1);


        shearingTubeBarcodes.add(twoDBarcode2);

        column = ExomeExpressV2EndToEndTest.RACK_COLUMNS.get((0) / ExomeExpressV2EndToEndTest.RACK_ROWS.size());
        row = ExomeExpressV2EndToEndTest.RACK_ROWS.get((0) % ExomeExpressV2EndToEndTest.RACK_ROWS.size());

        bspStock = "SM-" + twoDBarcode2;

        barcodesByRackPositions.put(row + column, twoDBarcode2);

        productOrderSamples.add(new ProductOrderSample(bspStock));
        bspAliquot2 = new TwoDBarcodedTube(twoDBarcode2);
        bspAliquot2.addSample(new MercurySample(bspStock));
        bspAliquot2.addBucketEntry(new BucketEntry(bspAliquot2, poBusinessKey2, BucketEntry.BucketEntryType.PDO_ENTRY));
        mapBarcodeToTube.put(twoDBarcode2, bspAliquot2);


        shearingTubeBarcodes.add(twoDBarcode3);

        column = ExomeExpressV2EndToEndTest.RACK_COLUMNS.get((0) / ExomeExpressV2EndToEndTest.RACK_ROWS.size());
        row = ExomeExpressV2EndToEndTest.RACK_ROWS.get((0) % ExomeExpressV2EndToEndTest.RACK_ROWS.size());

        bspStock = "SM-" + twoDBarcode3;

        barcodesByRackPositions.put(row + column, twoDBarcode3);

        productOrderSamples.add(new ProductOrderSample(bspStock));
        bspAliquot3 = new TwoDBarcodedTube(twoDBarcode3);
        bspAliquot3.addSample(new MercurySample(bspStock));
        bspAliquot3.addBucketEntry(new BucketEntry(bspAliquot3, poBusinessKey3, BucketEntry.BucketEntryType.PDO_ENTRY));
        mapBarcodeToTube.put(twoDBarcode3, bspAliquot3);


        shearingTubeBarcodes.add(twoDBarcode4);

        column = ExomeExpressV2EndToEndTest.RACK_COLUMNS.get((0) / ExomeExpressV2EndToEndTest.RACK_ROWS.size());
        row = ExomeExpressV2EndToEndTest.RACK_ROWS.get((0) % ExomeExpressV2EndToEndTest.RACK_ROWS.size());

        bspStock = "SM-" + twoDBarcode4;

        barcodesByRackPositions.put(row + column, twoDBarcode4);

        productOrderSamples.add(new ProductOrderSample(bspStock));
        bspAliquot4 = new TwoDBarcodedTube(twoDBarcode4);
        bspAliquot4.addSample(new MercurySample(bspStock));
        bspAliquot4.addBucketEntry(new BucketEntry(bspAliquot4, poBusinessKey3, BucketEntry.BucketEntryType.PDO_ENTRY));
        mapBarcodeToTube.put(twoDBarcode4, bspAliquot4);


        bucketCreationName = "Pico Bucket";
        hrafalUserName = "hrafal";
        howieTest = hrafalUserName;

        WorkflowBucketDef bucketDef = new WorkflowBucketDef(bucketCreationName);

        bucket = new Bucket(bucketDef);

        bucketDao.persist(bucket);
        bucketDao.flush();
        bucketDao.clear();

    }

    @AfterMethod
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    public void testResource_start_entries() {

        bucket = bucketDao.findByName(bucketCreationName);

        Assert.assertNotNull(bucket.getBucketEntries());
        Assert.assertTrue(bucket.getBucketEntries().isEmpty());

        Collection<BucketEntry> testEntries1 = resource.add(Collections.<LabVessel>singleton(bspAliquot1), bucket,
                BucketEntry.BucketEntryType.PDO_ENTRY, howieTest, LabEvent.UI_EVENT_LOCATION,
                LabEvent.UI_PROGRAM_NAME, LabEventType.SHEARING_BUCKET, poBusinessKey1);
        Assert.assertEquals(testEntries1.size(), 1);
        BucketEntry testEntry1 = testEntries1.iterator().next();

        Assert.assertEquals(1, bucket.getBucketEntries().size());
        Assert.assertTrue(bucket.contains(testEntry1));


        Collection<BucketEntry> testEntries2 = resource.add(Collections.<LabVessel>singleton(bspAliquot2), bucket,
                BucketEntry.BucketEntryType.PDO_ENTRY, howieTest, LabEvent.UI_EVENT_LOCATION,
                LabEvent.UI_PROGRAM_NAME, LabEventType.SHEARING_BUCKET, poBusinessKey2);
        Assert.assertEquals(testEntries2.size(), 1);
        BucketEntry testEntry2 = testEntries2.iterator().next();

        Collection<BucketEntry> testEntries3 = resource.add(Collections.<LabVessel>singleton(bspAliquot3), bucket,
                BucketEntry.BucketEntryType.PDO_ENTRY, howieTest, LabEvent.UI_EVENT_LOCATION,
                LabEvent.UI_PROGRAM_NAME, LabEventType.SHEARING_BUCKET, poBusinessKey3);
        Assert.assertEquals(testEntries3.size(), 1);
        BucketEntry testEntry3 = testEntries3.iterator().next();

        Collection<BucketEntry> testEntries4 = resource.add(Collections.<LabVessel>singleton(bspAliquot4), bucket,
                BucketEntry.BucketEntryType.PDO_ENTRY, howieTest, LabEvent.UI_EVENT_LOCATION,
                LabEvent.UI_PROGRAM_NAME, LabEventType.SHEARING_BUCKET, poBusinessKey3);
        Assert.assertEquals(testEntries4.size(), 1);
        BucketEntry testEntry4 = testEntries4.iterator().next();

        Assert.assertTrue(bucket.contains(testEntry2));
        Assert.assertTrue(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry4));

        Set<BucketEntry> bucketBatch = new HashSet<>();

        Assert.assertTrue(Collections.addAll(bucketBatch, testEntry1, testEntry2, testEntry3));

        Assert.assertFalse(testEntry1.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry1.getLabVessel().getInPlaceEvents().size());
        Assert.assertFalse(testEntry2.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry2.getLabVessel().getInPlaceEvents().size());
        Assert.assertFalse(testEntry3.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry3.getLabVessel().getInPlaceEvents().size());
        Assert.assertFalse(testEntry4.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry4.getLabVessel().getInPlaceEvents().size());

        resource.moveFromBucketToCommonBatch(bucketBatch);

        Assert.assertFalse(testEntry1.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertFalse(testEntry2.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertFalse(testEntry3.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertFalse(testEntry4.getLabVessel().getInPlaceEvents().isEmpty());

        for (BucketEntry currEntry : bucketBatch) {
            boolean doesEventHavePDO = false;
            for (LabEvent labEvent : currEntry.getLabVessel().getInPlaceEvents()) {
                if (labEvent.getProductOrderId() != null) {
                    if (currEntry.getPoBusinessKey().equals(labEvent.getProductOrderId())) {
                        doesEventHavePDO = true;
                    }
                }
                // make sure that adding the vessel to the bucket created
                // a new event and tagged it with the appropriate PDO
                assertTrue(doesEventHavePDO);
            }
        }

        Assert.assertFalse(bucket.contains(testEntry1));
        Assert.assertFalse(bucket.contains(testEntry2));
        Assert.assertFalse(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry4));

        resource.removeEntry(testEntry4, "Because the test told me to!!!");

        Assert.assertFalse(bucket.contains(testEntry4));

        Assert.assertTrue(bucket.getBucketEntries().isEmpty());
    }


    public void testResource_start_vessels() {


        bucket = bucketDao.findByName(bucketCreationName);

        Assert.assertNotNull(bucket.getBucketEntries());
        Assert.assertTrue(bucket.getBucketEntries().isEmpty());

        Collection<BucketEntry> testEntries1 = resource.add(Collections.<LabVessel>singleton(bspAliquot1), bucket,
                BucketEntry.BucketEntryType.PDO_ENTRY, howieTest, LabEvent.UI_EVENT_LOCATION,
                LabEvent.UI_PROGRAM_NAME, LabEventType.SHEARING_BUCKET, poBusinessKey1);
        Assert.assertEquals(testEntries1.size(), 1);
        BucketEntry testEntry1 = testEntries1.iterator().next();

        Assert.assertEquals(1, bucket.getBucketEntries().size());
        Assert.assertTrue(bucket.contains(testEntry1));


        BucketEntry testEntry2;
        BucketEntry testEntry3;
        BucketEntry testEntry4;


        List<LabVessel> bucketCreateBatch = new LinkedList<>();


        Assert.assertTrue(Collections.addAll(bucketCreateBatch, bspAliquot2,
                bspAliquot3, bspAliquot4));


        resource.add(bucketCreateBatch, bucket, BucketEntry.BucketEntryType.PDO_ENTRY, howieTest, "Superman",
                LabEvent.UI_PROGRAM_NAME, LabEventType.SHEARING_BUCKET, poBusinessKey3);

        bucketDao.flush();
        bucketDao.clear();
        bucket = bucketDao.findByName(bucketCreationName);


        LabVessel vessel1 = twoDBarcodedTubeDao.findByBarcode(twoDBarcode1);
        LabVessel vessel2 = twoDBarcodedTubeDao.findByBarcode(twoDBarcode2);
        LabVessel vessel3 = twoDBarcodedTubeDao.findByBarcode(twoDBarcode3);
        LabVessel vessel4 = twoDBarcodedTubeDao.findByBarcode(twoDBarcode4);

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

        Assert.assertTrue(Collections.addAll(vesselBucketBatch, vessel1,
                vessel2, vessel3));

        Assert.assertFalse(vessel1.getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, vessel1.getInPlaceEvents().size());
        Assert.assertFalse(vessel2.getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, vessel2.getInPlaceEvents().size());
        Assert.assertFalse(vessel3.getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, vessel3.getInPlaceEvents().size());
        Assert.assertFalse(vessel4.getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, vessel4.getInPlaceEvents().size());

        resource.createEntriesAndBatchThem(vesselBucketBatch, bucket);

        bucketDao.flush();
        bucketDao.clear();
        bucket = bucketDao.findByName(bucketCreationName);
        vessel1 = twoDBarcodedTubeDao.findByBarcode(twoDBarcode1);
        vessel2 = twoDBarcodedTubeDao.findByBarcode(twoDBarcode2);
        vessel3 = twoDBarcodedTubeDao.findByBarcode(twoDBarcode3);
        vessel4 = twoDBarcodedTubeDao.findByBarcode(twoDBarcode4);

        Assert.assertFalse(vessel1.getInPlaceEvents().isEmpty());
        Assert.assertFalse(vessel2.getInPlaceEvents().isEmpty());
        Assert.assertFalse(vessel3.getInPlaceEvents().isEmpty());
        Assert.assertFalse(vessel4.getInPlaceEvents().isEmpty());

        for (BucketEntry currEntry : bucketBatch) {
            boolean doesEventHavePDO = false;
            for (LabEvent labEvent : currEntry.getLabVessel().getInPlaceEvents()) {
                if (labEvent.getProductOrderId() != null) {
                    if (currEntry.getPoBusinessKey().equals(labEvent.getProductOrderId())) {
                        doesEventHavePDO = true;
                    }
                }
                // make sure that adding the vessel to the bucket created
                // a new event and tagged it with the appropriate PDO
                assertTrue(doesEventHavePDO);
            }
        }

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
        bucket = bucketDao.findByName(bucketCreationName);

        testEntry4 = bucketEntryDao.findByVesselAndBucket(vessel4, bucket);
        Assert.assertNotNull(testEntry4);
        Assert.assertFalse(bucket.contains(testEntry4));

        Assert.assertTrue(bucket.getBucketEntries().isEmpty());
    }


    public void testResource_start_vessel_count() {

        bucket = bucketDao.findByName(bucketCreationName);

        Assert.assertNotNull(bucket.getBucketEntries());
        Assert.assertTrue(bucket.getBucketEntries().isEmpty());

        Collection<BucketEntry> testEntries1 = resource.add(Collections.<LabVessel>singleton(bspAliquot1), bucket,
                BucketEntry.BucketEntryType.PDO_ENTRY, howieTest, LabEvent.UI_EVENT_LOCATION,
                LabEvent.UI_PROGRAM_NAME, LabEventType.SHEARING_BUCKET, poBusinessKey1);
        Assert.assertEquals(testEntries1.size(), 1);
        BucketEntry testEntry1 = testEntries1.iterator().next();

        Assert.assertEquals(1, bucket.getBucketEntries().size());
        Assert.assertTrue(bucket.contains(testEntry1));

        BucketEntry testEntry2;
        BucketEntry testEntry3;
        BucketEntry testEntry4;

        List<LabVessel> bucketCreateBatch = new LinkedList<>();

        Assert.assertTrue(Collections.addAll(bucketCreateBatch, bspAliquot2, bspAliquot3, bspAliquot4));

        resource.add(bucketCreateBatch, bucket, BucketEntry.BucketEntryType.PDO_ENTRY, howieTest, "Superman",
                LabEvent.UI_PROGRAM_NAME, LabEventType.SHEARING_BUCKET, poBusinessKey3);

        bucketDao.flush();
        bucketDao.clear();

        bucket = bucketDao.findByName(bucketCreationName);


        LabVessel vessel1 = twoDBarcodedTubeDao.findByBarcode(twoDBarcode1);
        LabVessel vessel2 = twoDBarcodedTubeDao.findByBarcode(twoDBarcode2);
        LabVessel vessel3 = twoDBarcodedTubeDao.findByBarcode(twoDBarcode3);
        LabVessel vessel4 = twoDBarcodedTubeDao.findByBarcode(twoDBarcode4);

        testEntry1 = bucketEntryDao.findByVesselAndBucket(vessel1, bucket);
        testEntry2 = bucketEntryDao.findByVesselAndBucket(vessel2, bucket);
        testEntry3 = bucketEntryDao.findByVesselAndBucket(vessel3, bucket);
        testEntry4 = bucketEntryDao.findByVesselAndBucket(vessel4, bucket);

        Set<BucketEntry> bucketBatch = new HashSet<>();
        Assert.assertTrue(Collections.addAll(bucketBatch, testEntry1, testEntry2, testEntry3));

        Assert.assertTrue(bucket.contains(testEntry2));
        Assert.assertTrue(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry4));


        Assert.assertFalse(testEntry1.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry1.getLabVessel().getInPlaceEvents().size());
        Assert.assertFalse(testEntry2.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry2.getLabVessel().getInPlaceEvents().size());
        Assert.assertFalse(testEntry3.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry3.getLabVessel().getInPlaceEvents().size());
        Assert.assertFalse(testEntry4.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry4.getLabVessel().getInPlaceEvents().size());

        logger.info("Before the start method.  The bucket has " + bucket.getBucketEntries().size() + " Entries in it");

        resource.selectEntriesAndBatchThem(3, bucket, Workflow.AGILENT_EXOME_EXPRESS);

        logger.info("After the start method.  The bucket has " + bucket.getBucketEntries().size() + " Entries in it");


        Assert.assertFalse(testEntry1.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertFalse(testEntry2.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertFalse(testEntry3.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertFalse(testEntry4.getLabVessel().getInPlaceEvents().isEmpty());

        for (BucketEntry currEntry : bucketBatch) {
            boolean doesEventHavePDO = false;
            for (LabEvent labEvent : currEntry.getLabVessel().getInPlaceEvents()) {
                if (labEvent.getProductOrderId() != null) {
                    if (currEntry.getPoBusinessKey().equals(labEvent.getProductOrderId())) {
                        doesEventHavePDO = true;
                    }
                }
                // make sure that adding the vessel to the bucket created
                // a new event and tagged it with the appropriate PDO
                assertTrue(doesEventHavePDO);
            }
        }

        Assert.assertFalse(bucket.contains(testEntry1));
        Assert.assertFalse(bucket.contains(testEntry2));
        Assert.assertFalse(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry4));

        resource.removeEntry(testEntry4, "Because the test told me to!!!");

        Assert.assertFalse(bucket.contains(testEntry4));

        Assert.assertTrue(bucket.getBucketEntries().isEmpty());
    }

}
