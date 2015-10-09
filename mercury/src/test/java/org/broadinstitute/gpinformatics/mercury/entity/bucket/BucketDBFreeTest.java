package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Scott Matthews
 *         Date: 10/26/12
 *         Time: 1:50 PM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class BucketDBFreeTest {

    public void testBucketCreation() {

        final String bucketCreationName = "Pico Bucket";

        Bucket bucket = new Bucket(bucketCreationName);

        Assert.assertNotNull(bucket.getBucketDefinitionName());
        Assert.assertEquals(bucketCreationName, bucket.getBucketDefinitionName());

        Assert.assertNotNull(bucket.getBucketEntries());

        Assert.assertTrue(bucket.getBucketEntries().isEmpty());

        ProductOrder pdo1 = ProductOrderTestFactory.createDummyProductOrder();
        ProductOrder pdo2 = ProductOrderTestFactory.createDummyProductOrder();
        ProductOrder pdo3 = ProductOrderTestFactory.createDummyProductOrder();
        pdo1.setJiraTicketKey("PDO-8");
        pdo2.setJiraTicketKey("PDO-9");
        pdo3.setJiraTicketKey("PDO-10");

        final String barcode1 = "SM-321";
        final String barcode2 = "SM-322";
        final String barcode3 = "SM-323";
        final String barcode4 = "SM-324";

        BarcodedTube tube1 = new BarcodedTube(barcode1);
        BarcodedTube tube2 = new BarcodedTube(barcode2);

        BucketEntry testEntry1 = bucket.addEntry(pdo1, tube1, BucketEntry.BucketEntryType.PDO_ENTRY);
        Assert.assertNotNull(testEntry1.getBucket());
        Assert.assertEquals(bucket, testEntry1.getBucket());

        BucketEntry testEntry2 = bucket.addEntry(pdo1, tube2, BucketEntry.BucketEntryType.PDO_ENTRY);
        Assert.assertNotNull(testEntry2.getBucket());
        Assert.assertEquals(bucket, testEntry2.getBucket());

        BucketEntry testEntry3 = bucket.addEntry(pdo2, new BarcodedTube(barcode3),
                BucketEntry.BucketEntryType.PDO_ENTRY);
        Assert.assertNotNull(testEntry3.getBucket());
        Assert.assertEquals(bucket, testEntry3.getBucket());

        BucketEntry testEntry4 = bucket.addEntry(pdo3, new BarcodedTube(barcode4),
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
