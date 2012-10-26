package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

/**
 * @author Scott Matthews
 *         Date: 10/26/12
 *         Time: 1:50 PM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class BucketDBFreeTest extends Arquillian {


    public void testBucketCreation() {

        final String bucketCreationName = "Pico Bucket";

        Bucket bucket = new Bucket(bucketCreationName);

        Assert.assertNotNull (bucket.getBucketDefinitionName());
        Assert.assertEquals(bucketCreationName, bucket.getBucketDefinitionName());

        Assert.assertNotNull(bucket.getBucketEntries());

        Assert.assertTrue(bucket.getBucketEntries().isEmpty());

        final String poBusinessKey1 = "PDO-1";
        final String poBusinessKey2 = "PDO-2";
        final String poBusinessKey3 = "PDO-3";

        final String twoDBarcode1 = "SM-321";
        final String twoDBarcode2 = "SM-322";
        final String twoDBarcode3 = "SM-323";
        final String twoDBarcode4 = "SM-324";

        BucketEntry testEntry1 = new BucketEntry(new TwoDBarcodedTube( twoDBarcode1 ), poBusinessKey1,bucket);
        BucketEntry testEntry2 = new BucketEntry(new TwoDBarcodedTube( twoDBarcode2 ), poBusinessKey1,bucket);
        BucketEntry testEntry3 = new BucketEntry(new TwoDBarcodedTube( twoDBarcode3 ), poBusinessKey2,bucket);
        BucketEntry testEntry4 = new BucketEntry(new TwoDBarcodedTube( twoDBarcode4 ), poBusinessKey3,bucket);

        bucket.addEntry(testEntry1);
        bucket.addEntry(testEntry2);
        bucket.addEntry(poBusinessKey2, new TwoDBarcodedTube(twoDBarcode3));
        bucket.addEntry(poBusinessKey3, new TwoDBarcodedTube(twoDBarcode4));

        Assert.assertFalse(bucket.getBucketEntries().isEmpty());

        Assert.assertEquals(4, bucket.getBucketEntries().size());

        Assert.assertTrue(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry1));
        Assert.assertTrue(bucket.contains(testEntry2));
        Assert.assertTrue(bucket.contains(testEntry4));


        bucket.removeEntry(testEntry3);

        Assert.assertEquals(3, bucket.getBucketEntries().size());

        Assert.assertFalse(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry1));
        Assert.assertTrue(bucket.contains(testEntry2));
        Assert.assertTrue(bucket.contains(testEntry4));

        bucket.removeEntry(testEntry2);

        Assert.assertEquals(2, bucket.getBucketEntries().size());

        Assert.assertFalse(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry1));
        Assert.assertFalse(bucket.contains(testEntry2));
        Assert.assertTrue(bucket.contains(testEntry4));

        bucket.removeEntry(testEntry1);

        Assert.assertEquals(1, bucket.getBucketEntries().size());

        Assert.assertFalse(bucket.contains(testEntry3));
        Assert.assertFalse(bucket.contains(testEntry1));
        Assert.assertFalse(bucket.contains(testEntry2));
        Assert.assertTrue(bucket.contains(testEntry4));

        bucket.removeEntry(testEntry4);

        Assert.assertEquals(0, bucket.getBucketEntries().size());

        Assert.assertFalse(bucket.contains(testEntry3));
        Assert.assertFalse(bucket.contains(testEntry1));
        Assert.assertFalse(bucket.contains(testEntry2));
        Assert.assertFalse(bucket.contains(testEntry4));

    }
}
