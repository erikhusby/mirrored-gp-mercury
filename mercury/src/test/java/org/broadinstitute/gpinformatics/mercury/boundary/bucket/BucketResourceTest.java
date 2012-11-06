package org.broadinstitute.gpinformatics.mercury.boundary.bucket;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertTrue;

/**
 * @author Scott Matthews
 *         Date: 10/26/12
 *         Time: 2:10 PM
 */
@Test ( groups = TestGroups.EXTERNAL_INTEGRATION )
public class BucketResourceTest extends ContainerTest {

    @Inject
    BucketBean resource;

    public void testResource () {
        final String bucketCreationName = "Pico Bucket";
        final Person howieTest = new Person ( "hrafal", "Howard", "Rafal" );

        Bucket bucket = new Bucket ( bucketCreationName );

        final String poBusinessKey1 = "PDO-1";
        final String poBusinessKey2 = "PDO-2";
        final String poBusinessKey3 = "PDO-3";

        final String twoDBarcode1 = "SM-321";
        final String twoDBarcode2 = "SM-322";
        final String twoDBarcode3 = "SM-323";
        final String twoDBarcode4 = "SM-324";

        //        BucketEntry testEntry1 = new BucketEntry ( new TwoDBarcodedTube ( twoDBarcode1 ), poBusinessKey1, bucket );
        //        BucketEntry testEntry2 = new BucketEntry ( new TwoDBarcodedTube ( twoDBarcode2 ), poBusinessKey1, bucket );
        //        BucketEntry testEntry3 = new BucketEntry ( new TwoDBarcodedTube ( twoDBarcode3 ), poBusinessKey2, bucket );
        //        BucketEntry testEntry4 = new BucketEntry ( new TwoDBarcodedTube ( twoDBarcode4 ), poBusinessKey3, bucket );

        Assert.assertNotNull(bucket.getBucketEntries());
        Assert.assertTrue(bucket.getBucketEntries().isEmpty());

        BucketEntry testEntry1 = resource.add (new TwoDBarcodedTube ( twoDBarcode1 ), poBusinessKey1, bucket);

        Assert.assertEquals(1,bucket.getBucketEntries().size());
        Assert.assertTrue ( bucket.contains ( testEntry1 ) );


        BucketEntry testEntry2 = resource.add (new TwoDBarcodedTube ( twoDBarcode2 ), poBusinessKey1, bucket);
        BucketEntry testEntry3 = resource.add (new TwoDBarcodedTube ( twoDBarcode3 ), poBusinessKey2, bucket);
        BucketEntry testEntry4 = resource.add (new TwoDBarcodedTube ( twoDBarcode4 ), poBusinessKey3, bucket);

        Assert.assertTrue(bucket.contains(testEntry2));
        Assert.assertTrue(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry4));


        Set<BucketEntry> bucketBatch = new HashSet<BucketEntry>();

        Assert.assertTrue ( Collections.addAll ( bucketBatch, testEntry1, testEntry2, testEntry3 ) );
        //TODO SGM Flush to test

        Assert.assertFalse(testEntry1.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry1.getLabVessel().getInPlaceEvents().size());
        Assert.assertFalse(testEntry2.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry2.getLabVessel().getInPlaceEvents().size());
        Assert.assertFalse(testEntry3.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry3.getLabVessel().getInPlaceEvents().size());
        Assert.assertFalse(testEntry4.getLabVessel().getInPlaceEvents().isEmpty());
        Assert.assertEquals(1, testEntry4.getLabVessel().getInPlaceEvents().size());

        resource.start(bucketBatch, howieTest);

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
