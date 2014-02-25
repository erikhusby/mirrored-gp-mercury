package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.testng.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.testng.annotations.Test;

/**
 * @author Scott Matthews
 *         Date: 10/26/12
 *         Time: 1:50 PM
 */
public class BucketDBFreeTest {

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testBucketCreation() {

        final String bucketCreationName = "Pico Bucket";

        Bucket bucket = new Bucket(bucketCreationName);

        Assert.assertNotNull(bucket.getBucketDefinitionName());
        Assert.assertEquals(bucketCreationName, bucket.getBucketDefinitionName());

        Assert.assertNotNull(bucket.getBucketEntries());

        Assert.assertTrue(bucket.getBucketEntries().isEmpty());

        final String poBusinessKey1 = "PDO-8";
        final String poBusinessKey2 = "PDO-9";
        final String poBusinessKey3 = "PDO-10";

        final String twoDBarcode1 = "SM-321";
        final String twoDBarcode2 = "SM-322";
        final String twoDBarcode3 = "SM-323";
        final String twoDBarcode4 = "SM-324";

        TwoDBarcodedTube tube1 = new TwoDBarcodedTube(twoDBarcode1);
        TwoDBarcodedTube tube2 = new TwoDBarcodedTube(twoDBarcode2);

        BucketEntry testEntry1 = bucket.addEntry(poBusinessKey1, tube1, BucketEntry.BucketEntryType.PDO_ENTRY);
        Assert.assertNotNull(testEntry1.getBucket());
        Assert.assertEquals(bucket, testEntry1.getBucket());

        BucketEntry testEntry2 = bucket.addEntry(poBusinessKey1, tube2, BucketEntry.BucketEntryType.PDO_ENTRY);
        Assert.assertNotNull(testEntry2.getBucket());
        Assert.assertEquals(bucket, testEntry2.getBucket());

        BucketEntry testEntry3 = bucket.addEntry(poBusinessKey2, new TwoDBarcodedTube(twoDBarcode3),
                BucketEntry.BucketEntryType.PDO_ENTRY);
        Assert.assertNotNull(testEntry3.getBucket());
        Assert.assertEquals(bucket, testEntry3.getBucket());

        BucketEntry testEntry4 = bucket.addEntry(poBusinessKey3, new TwoDBarcodedTube(twoDBarcode4),
                BucketEntry.BucketEntryType.PDO_ENTRY);
        Assert.assertNotNull(testEntry4.getBucket());
        Assert.assertEquals(bucket, testEntry4.getBucket());

        Assert.assertFalse(bucket.getBucketEntries().isEmpty());

        Assert.assertEquals(bucket.getBucketEntries().size(), 4);

        Assert.assertTrue(bucket.contains(testEntry1));
        Assert.assertTrue(bucket.contains(testEntry2));
        Assert.assertTrue(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry4));

        bucket.removeEntry(testEntry3);

        Assert.assertEquals(bucket.getBucketEntries().size(), 3);

        Assert.assertFalse(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry1));
        Assert.assertTrue(bucket.contains(testEntry2));
        Assert.assertTrue(bucket.contains(testEntry4));

        bucket.removeEntry(testEntry2);

        Assert.assertEquals(bucket.getBucketEntries().size(), 2);

        Assert.assertFalse(bucket.contains(testEntry3));
        Assert.assertTrue(bucket.contains(testEntry1));
        Assert.assertFalse(bucket.contains(testEntry2));
        Assert.assertTrue(bucket.contains(testEntry4));

        bucket.removeEntry(testEntry1);

        Assert.assertEquals(bucket.getBucketEntries().size(), 1);

        Assert.assertFalse(bucket.contains(testEntry3));
        Assert.assertFalse(bucket.contains(testEntry1));
        Assert.assertFalse(bucket.contains(testEntry2));
        Assert.assertTrue(bucket.contains(testEntry4));

        bucket.removeEntry(testEntry4);

        Assert.assertEquals(bucket.getBucketEntries().size(), 0);

        Assert.assertFalse(bucket.contains(testEntry3));
        Assert.assertFalse(bucket.contains(testEntry1));
        Assert.assertFalse(bucket.contains(testEntry2));
        Assert.assertFalse(bucket.contains(testEntry4));

    }
}
