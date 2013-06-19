package org.broadinstitute.gpinformatics.mercury.boundary.bucket;

import org.testng.Assert;
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
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.test.ExomeExpressV2EndToEndTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.*;

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
    TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

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

        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>();
        poBusinessKey1 = "PDO-1";
        poBusinessKey2 = "PDO-2";
        poBusinessKey3 = "PDO-3";

        productOrder1 = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123", new Product(
                "Test product", new ProductFamily("Test product family"), "test", "1234", null, null, 10000, 20000, 100,
                40, null, null, true, WorkflowName.EXOME_EXPRESS.getWorkflowName(), false, "agg type"), new ResearchProject(101L, "Test RP", "Test synopsis",
                false));
        productOrder2 = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123", new Product(
                "Test product", new ProductFamily("Test product family"), "test", "1234", null, null, 10000, 20000, 100,
                40, null, null, true, WorkflowName.EXOME_EXPRESS.getWorkflowName(), false, "agg type"), new ResearchProject(101L, "Test RP", "Test synopsis",
                false));
        productOrder3 = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123", new Product(
                "Test product", new ProductFamily("Test product family"), "test", "1234", null, null, 10000, 20000, 100,
                40, null, null, true, WorkflowName.EXOME_EXPRESS.getWorkflowName(), false, "agg type"), new ResearchProject(101L, "Test RP", "Test synopsis",
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

        List<String> shearingTubeBarcodes = new ArrayList<String>()/*Arrays.asList("SH1", "SH2", "SH3")*/;
        Map<String, String> barcodesByRackPositions = new HashMap<String, String>();


        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();


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

        BucketEntry testEntry1 = resource.add(bspAliquot1, bucket,
                howieTest, LabEventType.SHEARING_BUCKET, LabEvent.UI_EVENT_LOCATION);

        Assert.assertEquals(1, bucket.getBucketEntries().size());
        Assert.assertTrue(bucket.contains(testEntry1));


        BucketEntry testEntry2 = resource.add(bspAliquot2, bucket,
                howieTest, LabEventType.SHEARING_BUCKET, LabEvent.UI_EVENT_LOCATION);
        BucketEntry testEntry3 = resource.add(bspAliquot3, bucket,
                howieTest, LabEventType.SHEARING_BUCKET, LabEvent.UI_EVENT_LOCATION);
        BucketEntry testEntry4 = resource.add(bspAliquot4, bucket,
                howieTest, LabEventType.SHEARING_BUCKET, LabEvent.UI_EVENT_LOCATION);

        Assert.assertTrue(bucket.contains(testEntry2));
        Assert.assertTrue(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry4));

        Set<BucketEntry> bucketBatch = new HashSet<BucketEntry>();

        Assert.assertTrue(Collections.addAll(bucketBatch, testEntry1, testEntry2, testEntry3));

        Assert.assertFalse(testEntry1.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry1.getLabVessel().getInPlaceEvents().size());
        Assert.assertFalse(testEntry2.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry2.getLabVessel().getInPlaceEvents().size());
        Assert.assertFalse(testEntry3.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry3.getLabVessel().getInPlaceEvents().size());
        Assert.assertFalse(testEntry4.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry4.getLabVessel().getInPlaceEvents().size());

        resource.start(bucketBatch, howieTest, LabEvent.UI_EVENT_LOCATION);

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

        resource.cancel(testEntry4, howieTest,
                "Because the test told me to!!!");

        Assert.assertFalse(bucket.contains(testEntry4));

        Assert.assertTrue(bucket.getBucketEntries().isEmpty());
    }


    public void testResource_start_vessels() {


        bucket = bucketDao.findByName(bucketCreationName);

        Assert.assertNotNull(bucket.getBucketEntries());
        Assert.assertTrue(bucket.getBucketEntries().isEmpty());

        BucketEntry testEntry1 = resource.add(bspAliquot1, bucket,
                howieTest, LabEventType.SHEARING_BUCKET, LabEvent.UI_EVENT_LOCATION);

        Assert.assertEquals(1, bucket.getBucketEntries().size());
        Assert.assertTrue(bucket.contains(testEntry1));


        BucketEntry testEntry2;
        BucketEntry testEntry3;
        BucketEntry testEntry4;


        List<LabVessel> bucketCreateBatch = new LinkedList<LabVessel>();


        Assert.assertTrue(Collections.addAll(bucketCreateBatch, bspAliquot2,
                bspAliquot3, bspAliquot4));


        resource.add(bucketCreateBatch, bucket, howieTest, "Superman", LabEventType.SHEARING_BUCKET);

        bucketDao.flush();
        bucketDao.clear();
        bucket = bucketDao.findByName(bucketCreationName);


        LabVessel vessel1 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode1);
        LabVessel vessel2 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode2);
        LabVessel vessel3 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode3);
        LabVessel vessel4 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode4);

        testEntry1 = bucketEntryDao.findByVesselAndBucket(vessel1, bucket);
        testEntry2 = bucketEntryDao.findByVesselAndBucket(vessel2, bucket);
        testEntry3 = bucketEntryDao.findByVesselAndBucket(vessel3, bucket);
        testEntry4 = bucketEntryDao.findByVesselAndBucket(vessel4, bucket);

        Set<BucketEntry> bucketBatch = new HashSet<BucketEntry>();
        Assert.assertTrue(Collections.addAll(bucketBatch, testEntry1, testEntry2, testEntry3));

        Assert.assertTrue(bucket.contains(testEntry2));
        Assert.assertTrue(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry4));

        Set<LabVessel> vesselBucketBatch = new HashSet<LabVessel>();

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

        resource.start(howieTest, vesselBucketBatch, bucket, LabEvent.UI_EVENT_LOCATION);

        bucketDao.flush();
        bucketDao.clear();
        bucket = bucketDao.findByName(bucketCreationName);
        vessel1 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode1);
        vessel2 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode2);
        vessel3 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode3);
        vessel4 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode4);

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

        Assert.assertNotNull(bucketEntryDao.findByVesselAndBucket(vessel1, bucket));
        Assert.assertNotNull(bucketEntryDao.findByVesselAndBucket(vessel2, bucket));
        Assert.assertNotNull(bucketEntryDao.findByVesselAndBucket(vessel3, bucket));

        Assert.assertFalse(bucket.contains(testEntry1));
        Assert.assertFalse(bucket.contains(testEntry2));
        Assert.assertFalse(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry4));

        testEntry4 = bucketEntryDao.findByVesselAndBucket(vessel4, bucket);
        resource.cancel(testEntry4, howieTest,
                "Because the test told me to!!!");

        bucketDao.flush();
        bucketDao.clear();
        bucket = bucketDao.findByName(bucketCreationName);

        Assert.assertNotNull(bucketEntryDao.findByVesselAndBucket(vessel4, bucket));
        Assert.assertFalse(bucket.contains(testEntry4));

        Assert.assertTrue(bucket.getBucketEntries().isEmpty());
    }


    public void testResource_start_vessel_count() {

        bucket = bucketDao.findByName(bucketCreationName);

        Assert.assertNotNull(bucket.getBucketEntries());
        Assert.assertTrue(bucket.getBucketEntries().isEmpty());

        BucketEntry testEntry1 = resource.add(bspAliquot1, bucket,
                howieTest, LabEventType.SHEARING_BUCKET, LabEvent.UI_EVENT_LOCATION);

        Assert.assertEquals(1, bucket.getBucketEntries().size());
        Assert.assertTrue(bucket.contains(testEntry1));

        BucketEntry testEntry2;
        BucketEntry testEntry3;
        BucketEntry testEntry4;

        List<LabVessel> bucketCreateBatch = new LinkedList<LabVessel>();

        Assert.assertTrue(Collections.addAll(bucketCreateBatch, bspAliquot2, bspAliquot3, bspAliquot4));

        resource.add(bucketCreateBatch, bucket, howieTest, "Superman", LabEventType.SHEARING_BUCKET);

        bucketDao.flush();
        bucketDao.clear();

        bucket = bucketDao.findByName(bucketCreationName);


        LabVessel vessel1 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode1);
        LabVessel vessel2 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode2);
        LabVessel vessel3 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode3);
        LabVessel vessel4 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode4);

        testEntry1 = bucketEntryDao.findByVesselAndBucket(vessel1, bucket);
        testEntry2 = bucketEntryDao.findByVesselAndBucket(vessel2, bucket);
        testEntry3 = bucketEntryDao.findByVesselAndBucket(vessel3, bucket);
        testEntry4 = bucketEntryDao.findByVesselAndBucket(vessel4, bucket);

        Set<BucketEntry> bucketBatch = new HashSet<BucketEntry>();
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

        resource.start(howieTest, 3, bucket);

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

        resource.cancel(testEntry4, howieTest,
                "Because the test told me to!!!");

        Assert.assertFalse(bucket.contains(testEntry4));

        Assert.assertTrue(bucket.getBucketEntries().isEmpty());
    }

}
