package org.broadinstitute.gpinformatics.mercury.boundary.bucket;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static junit.framework.Assert.assertTrue;

/**
 * @author Scott Matthews
 *         Date: 10/26/12
 *         Time: 2:10 PM
 */
@Test ( groups = TestGroups.EXTERNAL_INTEGRATION )
public class BucketBeanTest extends ContainerTest {

    @Inject
    BucketBean resource;

    @Inject
    BucketDao bucketDao;

    @Inject
    BucketEntryDao bucketEntryDao;

    @Inject
    TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    private final static Logger logger = Logger.getLogger(BucketBeanTest.class.getName());

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

    @BeforeMethod
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

//        utx.setTransactionTimeout(300);
        utx.begin();

        poBusinessKey1 = "PDO-1";
        poBusinessKey2 = "PDO-2";
        poBusinessKey3 = "PDO-3";

        twoDBarcode1 = "SM-321";
        twoDBarcode2 = "SM-322";
        twoDBarcode3 = "SM-323";
        twoDBarcode4 = "SM-324";

        bucketCreationName = "Pico Bucket";
        hrafalUserName = "hrafal";
        howieTest = hrafalUserName;

        WorkflowBucketDef bucketDef = new WorkflowBucketDef( bucketCreationName );

        bucket = new Bucket ( bucketDef );

        bucketDao.persist( bucket );
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

    public void testResource_start_entries () {

        bucket = bucketDao.findByName(bucketCreationName);

        Assert.assertNotNull( bucket.getBucketEntries ());
        Assert.assertTrue( bucket.getBucketEntries ().isEmpty());

        BucketEntry testEntry1 = resource.add (new TwoDBarcodedTube ( twoDBarcode1 ), poBusinessKey1, bucket,
                                               howieTest, LabEventType.SHEARING_BUCKET);

        Assert.assertEquals(1, bucket.getBucketEntries ().size());
        Assert.assertTrue ( bucket.contains ( testEntry1 ) );


        BucketEntry testEntry2 = resource.add (new TwoDBarcodedTube ( twoDBarcode2 ), poBusinessKey1, bucket,
                                               howieTest, LabEventType.SHEARING_BUCKET);
        BucketEntry testEntry3 = resource.add (new TwoDBarcodedTube ( twoDBarcode3 ), poBusinessKey2, bucket,
                                               howieTest, LabEventType.SHEARING_BUCKET);
        BucketEntry testEntry4 = resource.add (new TwoDBarcodedTube ( twoDBarcode4 ), poBusinessKey3, bucket,
                                               howieTest, LabEventType.SHEARING_BUCKET);

        Assert.assertTrue( bucket.contains ( testEntry2 ));
        Assert.assertTrue( bucket.contains ( testEntry3 ));
        Assert.assertTrue( bucket.contains ( testEntry4 ));

        Set<BucketEntry> bucketBatch = new HashSet<BucketEntry>();

        Assert.assertTrue ( Collections.addAll ( bucketBatch, testEntry1, testEntry2, testEntry3 ) );

        Assert.assertFalse(testEntry1.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry1.getLabVessel().getInPlaceEvents().size());
        Assert.assertFalse(testEntry2.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry2.getLabVessel().getInPlaceEvents().size());
        Assert.assertFalse(testEntry3.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry3.getLabVessel().getInPlaceEvents().size());
        Assert.assertFalse(testEntry4.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry4.getLabVessel().getInPlaceEvents().size());

        resource.start(bucketBatch, howieTest, LabEvent.UI_EVENT_LOCATION );

        Assert.assertFalse(testEntry1.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertFalse(testEntry2.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertFalse(testEntry3.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertFalse(testEntry4.getLabVessel().getInPlaceEvents().isEmpty());

        for(BucketEntry currEntry:bucketBatch) {
            boolean doesEventHavePDO = false;
            for (LabEvent labEvent : currEntry.getLabVessel().getInPlaceEvents()) {
                if (labEvent.getProductOrderId()!= null) {
                    if (currEntry.getPoBusinessKey().equals(labEvent.getProductOrderId())) {
                        doesEventHavePDO = true;
                    }
                }
                // make sure that adding the vessel to the bucket created
                // a new event and tagged it with the appropriate PDO
                assertTrue(doesEventHavePDO);
            }
        }

        Assert.assertFalse( bucket.contains ( testEntry1 ));
        Assert.assertFalse( bucket.contains ( testEntry2 ));
        Assert.assertFalse( bucket.contains ( testEntry3 ));
        Assert.assertTrue ( bucket.contains ( testEntry4 ) );

        resource.cancel( testEntry4, howieTest,
                         "Because the test told me to!!!" );

        Assert.assertFalse( bucket.contains ( testEntry4 ));

        Assert.assertTrue( bucket.getBucketEntries ().isEmpty());
    }


    public void testResource_start_vessels () {


        bucket = bucketDao.findByName(bucketCreationName);

        Assert.assertNotNull(bucket.getBucketEntries());
        Assert.assertTrue(bucket.getBucketEntries().isEmpty());

        BucketEntry testEntry1 = resource.add (new TwoDBarcodedTube ( twoDBarcode1 ), poBusinessKey1, bucket,
                                               howieTest, LabEventType.SHEARING_BUCKET);

        Assert.assertEquals(1,bucket.getBucketEntries().size());
        Assert.assertTrue ( bucket.contains ( testEntry1 ) );


        BucketEntry testEntry2;
        BucketEntry testEntry3;
        BucketEntry testEntry4;


        List<LabVessel> bucketCreateBatch = new LinkedList<LabVessel>();


        Assert.assertTrue( Collections.addAll(bucketCreateBatch, new TwoDBarcodedTube(twoDBarcode2),
                                              new TwoDBarcodedTube(twoDBarcode3), new TwoDBarcodedTube(twoDBarcode4)));


        resource.add ( poBusinessKey1, bucketCreateBatch, bucket, howieTest, "Superman", LabEventType.SHEARING_BUCKET);

        bucketDao.flush ();
        bucketDao.clear ();

        bucket = bucketDao.findByName ( bucketCreationName );


        LabVessel vessel1 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode1);
        LabVessel vessel2 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode2);
        LabVessel vessel3 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode3);
        LabVessel vessel4 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode4);

        testEntry1 = bucketEntryDao.findByVesselAndBucket(vessel1, bucket);
        testEntry2 = bucketEntryDao.findByVesselAndBucket(vessel2, bucket);
        testEntry3 = bucketEntryDao.findByVesselAndBucket(vessel3, bucket);
        testEntry4 = bucketEntryDao.findByVesselAndBucket(vessel4, bucket);

        Set<BucketEntry> bucketBatch = new HashSet<BucketEntry>();
        Assert.assertTrue ( Collections.addAll ( bucketBatch, testEntry1, testEntry2, testEntry3 ) );

        Assert.assertTrue(bucket.contains(testEntry2));
        Assert.assertTrue(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry4));

        Set<LabVessel> vesselBucketBatch = new HashSet<LabVessel>();

        vessel1 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode1);
        vessel2 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode2);
        vessel3 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode3);
        vessel4 = twoDBarcodedTubeDAO.findByBarcode(twoDBarcode4);



        Assert.assertTrue ( Collections.addAll ( vesselBucketBatch, vessel1,
                                                 vessel2, vessel3) );

        Assert.assertFalse(vessel1.getInPlaceEvents ().isEmpty());
        Assert.assertEquals(1, vessel1.getInPlaceEvents ().size());
        Assert.assertFalse(vessel2.getInPlaceEvents ().isEmpty());
        Assert.assertEquals(1, vessel2.getInPlaceEvents ().size());
        Assert.assertFalse(vessel3.getInPlaceEvents ().isEmpty());
        Assert.assertEquals(1, vessel3.getInPlaceEvents ().size());
        Assert.assertFalse(vessel4.getInPlaceEvents ().isEmpty());
        Assert.assertEquals(1, vessel4.getInPlaceEvents ().size());

        resource.start(howieTest,vesselBucketBatch,bucket, LabEvent.UI_EVENT_LOCATION );

        testEntry1 = bucketEntryDao.findByVesselAndBucket(vessel1, bucket);
        testEntry2 = bucketEntryDao.findByVesselAndBucket(vessel2, bucket);
        testEntry3 = bucketEntryDao.findByVesselAndBucket(vessel3, bucket);
        testEntry4 = bucketEntryDao.findByVesselAndBucket(vessel4, bucket);


        Assert.assertFalse(testEntry1.getLabVessel().getInPlaceEvents ().isEmpty());
        Assert.assertFalse(testEntry2.getLabVessel().getInPlaceEvents ().isEmpty());
        Assert.assertFalse(testEntry3.getLabVessel().getInPlaceEvents ().isEmpty());
        Assert.assertFalse(testEntry4.getLabVessel().getInPlaceEvents ().isEmpty());

        for(BucketEntry currEntry:bucketBatch) {
            boolean doesEventHavePDO = false;
            for (LabEvent labEvent : currEntry.getLabVessel().getInPlaceEvents()) {
                if (labEvent.getProductOrderId()!= null) {
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
        Assert.assertTrue ( bucket.contains ( testEntry4 ) );

        resource.cancel( testEntry4, howieTest,
                         "Because the test told me to!!!" );

        Assert.assertFalse(bucket.contains(testEntry4));

        Assert.assertTrue(bucket.getBucketEntries().isEmpty());
    }


    public void testResource_start_vessel_count () {

        bucket = bucketDao.findByName(bucketCreationName);

        Assert.assertNotNull(bucket.getBucketEntries());
        Assert.assertTrue(bucket.getBucketEntries().isEmpty());

        BucketEntry testEntry1 = resource.add (new TwoDBarcodedTube ( twoDBarcode1 ), poBusinessKey1, bucket,
                                               howieTest, LabEventType.SHEARING_BUCKET);

        Assert.assertEquals(1,bucket.getBucketEntries().size());
        Assert.assertTrue ( bucket.contains ( testEntry1 ) );

        BucketEntry testEntry2;
        BucketEntry testEntry3;
        BucketEntry testEntry4;

        List<LabVessel> bucketCreateBatch = new LinkedList<LabVessel> ();

        Assert.assertTrue ( Collections.addAll ( bucketCreateBatch, new TwoDBarcodedTube(twoDBarcode2),
                                                 new TwoDBarcodedTube(twoDBarcode3),
                                                 new TwoDBarcodedTube(twoDBarcode4)) ) ;

        resource.add ( poBusinessKey1, bucketCreateBatch, bucket, howieTest, "Superman", LabEventType.SHEARING_BUCKET);

        bucketDao.flush ();
        bucketDao.clear ();

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
        Assert.assertTrue ( Collections.addAll ( bucketBatch, testEntry1, testEntry2, testEntry3 ) );

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

        logger.log( Level.INFO, "Before the start method.  The bucket has " + bucket.getBucketEntries ().size () +
                                     " Entries in it" );

        resource.start(howieTest,3,bucket);

        logger.log ( Level.INFO, "After the start method.  The bucket has " + bucket.getBucketEntries ().size () +
                " Entries in it" );


        Assert.assertFalse(testEntry1.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertFalse(testEntry2.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertFalse(testEntry3.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertFalse(testEntry4.getLabVessel().getInPlaceEvents().isEmpty());

        for(BucketEntry currEntry:bucketBatch) {
            boolean doesEventHavePDO = false;
            for (LabEvent labEvent : currEntry.getLabVessel().getInPlaceEvents()) {
                if (labEvent.getProductOrderId()!= null) {
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
        Assert.assertTrue ( bucket.contains ( testEntry4 ) );

        resource.cancel( testEntry4, howieTest,
                         "Because the test told me to!!!" );

        Assert.assertFalse(bucket.contains(testEntry4));

        Assert.assertTrue(bucket.getBucketEntries().isEmpty());
    }

}
