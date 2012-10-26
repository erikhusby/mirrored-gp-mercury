package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Scott Matthews
 *         Date: 10/26/12
 *         Time: 1:18 PM
 */
public class BucketEntryDBFreeTest {


    @Test( groups = TestGroups.DATABASE_FREE)
    public void testEntryCreation() {

        final String productOrder = "PO-1";
        final String twoDBarcode = "SM-2432";
        final String bucketName = "Pre-flight";

        Bucket bucket = new Bucket(bucketName);

        BucketEntry entry =
                new BucketEntry(new TwoDBarcodedTube( twoDBarcode ),
                                productOrder, bucket );


        Assert.assertNotNull(entry.getPoBusinessKey());
        Assert.assertEquals ( productOrder, entry.getPoBusinessKey () );
        Assert.assertNotNull(entry.getLabVessel());
        Assert.assertEquals ( twoDBarcode, entry.getLabVessel ().getLabel () );

        Assert.assertNotNull(entry.getBucketExistence());
        Assert.assertEquals(bucketName, entry.getBucketExistence().getBucketDefinitionName());

        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yy");

        Assert.assertEquals ( dateFormatter.format ( new Date () ), dateFormatter.format ( entry.getCreatedDate () ) );
    }

}
