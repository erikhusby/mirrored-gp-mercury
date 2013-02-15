package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.testng.annotations.Test;

/**
 * @author Scott Matthews
 *         Date: 10/26/12
 *         Time: 1:50 PM
 */
public class BucketDBFreeTest {


    @Test ( groups = TestGroups.DATABASE_FREE )
    public void testBucketCreation () {

        final String bucketCreationName = "Pico Bucket";

        Bucket bucket = new Bucket ( bucketCreationName );

        Assert.assertNotNull ( bucket.getBucketDefinitionName () );
        Assert.assertEquals ( bucketCreationName, bucket.getBucketDefinitionName () );

        Assert.assertNotNull ( bucket.getBucketEntries () );

        Assert.assertTrue ( bucket.getBucketEntries ().isEmpty () );

        final String poBusinessKey1 = "PDO-1";
        final String poBusinessKey2 = "PDO-2";
        final String poBusinessKey3 = "PDO-3";

        final String twoDBarcode1 = "SM-321";
        final String twoDBarcode2 = "SM-322";
        final String twoDBarcode3 = "SM-323";
        final String twoDBarcode4 = "SM-324";

        BucketEntry testEntry1 = new BucketEntry ( new TwoDBarcodedTube ( twoDBarcode1 ), poBusinessKey1, bucket );
        BucketEntry testEntry2 = new BucketEntry ( new TwoDBarcodedTube ( twoDBarcode2 ), poBusinessKey1, bucket );

        bucket.addEntry ( testEntry1 );
        Assert.assertNotNull ( testEntry1.getBucket() );
        Assert.assertEquals ( bucket, testEntry1.getBucket() );

        bucket.addEntry ( testEntry2 );
        Assert.assertNotNull ( testEntry2.getBucket() );
        Assert.assertEquals ( bucket, testEntry2.getBucket() );

        BucketEntry testEntry3 = bucket.addEntry ( poBusinessKey2, new TwoDBarcodedTube ( twoDBarcode3 ) );
        Assert.assertNotNull ( testEntry3.getBucket() );
        Assert.assertEquals ( bucket, testEntry3.getBucket() );

        BucketEntry testEntry4 = bucket.addEntry ( poBusinessKey3, new TwoDBarcodedTube ( twoDBarcode4 ) );
        Assert.assertNotNull ( testEntry3.getBucket() );
        Assert.assertEquals ( bucket, testEntry3.getBucket() );

        Assert.assertFalse ( bucket.getBucketEntries ().isEmpty () );

        Assert.assertEquals ( 4, bucket.getBucketEntries ().size () );

        Assert.assertTrue ( bucket.contains ( testEntry1 ) );
        Assert.assertTrue ( bucket.contains ( testEntry2 ) );
        Assert.assertTrue ( bucket.contains ( testEntry3 ) );
        Assert.assertTrue ( bucket.contains ( testEntry4 ) );


        bucket.removeEntry ( testEntry3 );

        Assert.assertEquals ( 3, bucket.getBucketEntries ().size () );

        Assert.assertFalse ( bucket.contains ( testEntry3 ) );
        Assert.assertTrue ( bucket.contains ( testEntry1 ) );
        Assert.assertTrue ( bucket.contains ( testEntry2 ) );
        Assert.assertTrue ( bucket.contains ( testEntry4 ) );

        bucket.removeEntry ( testEntry2 );

        Assert.assertEquals ( 2, bucket.getBucketEntries ().size () );

        Assert.assertFalse ( bucket.contains ( testEntry3 ) );
        Assert.assertTrue ( bucket.contains ( testEntry1 ) );
        Assert.assertFalse ( bucket.contains ( testEntry2 ) );
        Assert.assertTrue ( bucket.contains ( testEntry4 ) );

        bucket.removeEntry ( testEntry1 );

        Assert.assertEquals ( 1, bucket.getBucketEntries ().size () );

        Assert.assertFalse ( bucket.contains ( testEntry3 ) );
        Assert.assertFalse ( bucket.contains ( testEntry1 ) );
        Assert.assertFalse ( bucket.contains ( testEntry2 ) );
        Assert.assertTrue ( bucket.contains ( testEntry4 ) );

        bucket.removeEntry ( testEntry4 );

        Assert.assertEquals ( 0, bucket.getBucketEntries ().size () );

        Assert.assertFalse ( bucket.contains ( testEntry3 ) );
        Assert.assertFalse ( bucket.contains ( testEntry1 ) );
        Assert.assertFalse ( bucket.contains ( testEntry2 ) );
        Assert.assertFalse ( bucket.contains ( testEntry4 ) );

    }
}
