package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.meanbean.test.BeanTester;
import org.meanbean.test.Configuration;
import org.meanbean.test.ConfigurationBuilder;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 10/26/12
 *         Time: 1:18 PM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class BucketEntryDBFreeTest {


    public void testEntryCreation() {

        final ProductOrder productOrder = ProductOrderTestFactory.createProductOrder();
        productOrder.setJiraTicketKey("PO-1");
        final String twoDBarcode = "SM-2432";
        final String bucketName = "Pre-flight";

        Bucket bucket = new Bucket(bucketName);

        BucketEntry entry =
                new BucketEntry(new TwoDBarcodedTube(twoDBarcode),
                                productOrder, bucket, BucketEntry.BucketEntryType.PDO_ENTRY);


        Assert.assertNotNull(entry.getProductOrder());
        Assert.assertEquals(productOrder, entry.getProductOrder());
        Assert.assertNotNull(entry.getLabVessel());
        Assert.assertEquals(twoDBarcode, entry.getLabVessel().getLabel());

        Assert.assertNotNull(entry.getBucket());
        Assert.assertEquals(bucketName, entry.getBucket().getBucketDefinitionName());

        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yy");

        Assert.assertEquals(dateFormatter.format(new Date()), dateFormatter.format(entry.getCreatedDate()));
        Assert.assertEquals(entry.getStatus(), BucketEntry.Status.Active);

        entry.setStatus(BucketEntry.Status.Archived);
        Assert.assertEquals(entry.getStatus(), BucketEntry.Status.Archived);
    }

    public void testBasicBeaniness() {
        BeanTester tester = new BeanTester();
        Configuration configuration = new ConfigurationBuilder().
                                                                        ignoreProperty("bucket").
                                                                        ignoreProperty("labBatch").
                                                                        ignoreProperty("labVessel").
                                                                        build();

        tester.testBean(BucketEntry.class, configuration);
    }

    public void testUpdatePdo() {
        String bucketName = "Pico/Plating Bucket";
        ProductOrder productOrder1 = ProductOrderTestFactory.createProductOrder();
        productOrder1.setJiraTicketKey("PDO-1");
        ProductOrder productOrder2 = ProductOrderTestFactory.createProductOrder();
        productOrder2.setJiraTicketKey("PDO-2");
        String twoDBarcode1 = "A1234567890";
        String twoDBarcode2 = "A2345678901";

        Bucket bucket = new Bucket(bucketName);
        List<BucketEntry> bucketEntries = new ArrayList<>(2);
        bucketEntries.add(new BucketEntry(new TwoDBarcodedTube(twoDBarcode1), productOrder1, bucket,
                                          BucketEntry.BucketEntryType.PDO_ENTRY));
        Assert.assertEquals(bucketEntries.size(), 1);
        Assert.assertNotNull(bucketEntries.iterator().next());

        bucketEntries.add(new BucketEntry(new TwoDBarcodedTube(twoDBarcode2), productOrder1, bucket,
                                          BucketEntry.BucketEntryType.PDO_ENTRY));
        Assert.assertEquals(bucketEntries.size(), 2);
        Assert.assertNotNull(bucketEntries.iterator().next());

        ProductOrderDao productOrderDao = Mockito.mock(ProductOrderDao.class);
        Mockito.when(productOrderDao.findByBusinessKey(Mockito.eq("PDO-2"))).thenReturn(productOrder2);
        Mockito.when(productOrderDao.findByBusinessKey(Mockito.eq("PDO-1"))).thenReturn(productOrder1);

        BucketEjb bucketEjb = new BucketEjb(
                Mockito.mock(LabEventFactory.class),
                Mockito.mock(JiraService.class),
                Mockito.mock(BucketDao.class),
                null, Mockito.mock(LabVesselDao.class), null,
                null, null, null, productOrderDao);

        for (BucketEntry bucketEntry : bucketEntries) {
            Assert.assertEquals(bucketEntry.getProductOrder(), productOrder1);
        }

        bucketEjb.updateEntryPdo(bucketEntries, productOrder2.getBusinessKey());

        for (BucketEntry bucketEntry : bucketEntries) {
            Assert.assertEquals(bucketEntry.getProductOrder(), productOrder2);
        }

    }
}
