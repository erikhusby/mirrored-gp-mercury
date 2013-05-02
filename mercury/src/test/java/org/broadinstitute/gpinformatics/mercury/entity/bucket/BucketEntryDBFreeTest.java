package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.testng.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.meanbean.test.BeanTester;
import org.meanbean.test.Configuration;
import org.meanbean.test.ConfigurationBuilder;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Scott Matthews
 *         Date: 10/26/12
 *         Time: 1:18 PM
 */
@Test( groups = TestGroups.DATABASE_FREE)
public class BucketEntryDBFreeTest {


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

        Assert.assertNotNull(entry.getBucket());
        Assert.assertEquals(bucketName, entry.getBucket().getBucketDefinitionName());

        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yy");

        Assert.assertEquals ( dateFormatter.format ( new Date () ), dateFormatter.format ( entry.getCreatedDate () ) );
        Assert.assertEquals(entry.getStatus(), BucketEntry.Status.Active);

        entry.setStatus(BucketEntry.Status.Archived);
        Assert.assertEquals(entry.getStatus(), BucketEntry.Status.Archived);
    }

    public void testBasicBeaniness()  {
        BeanTester tester = new BeanTester();
        Configuration configuration = new ConfigurationBuilder().ignoreProperty("bucket").ignoreProperty("labBatch").
                build();

        tester.testBean(BucketEntry.class, configuration);
    }

}
