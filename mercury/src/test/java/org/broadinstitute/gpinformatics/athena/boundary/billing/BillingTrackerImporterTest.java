package org.broadinstitute.gpinformatics.athena.boundary.billing;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;

@Test(groups = TestGroups.DATABASE_FREE)
public class BillingTrackerImporterTest {

    public static final File BILLING_TRACKER_TEST_FILE = new File("src/test/data/billing/BillingTracker-DBFreeTestData.xlsx");

    private static final Log logger = LogFactory.getLog(BillingTrackerImporterTest.class);

    @Test
    void testImport() throws Exception {

        File testFile = BILLING_TRACKER_TEST_FILE;

        BillingTrackerImporter billingTrackerImporter = new BillingTrackerImporter(null);
        FileInputStream fis=null;
        File tempFile=null;

        try {
            fis = new FileInputStream(testFile);
            tempFile = billingTrackerImporter.copyFromStreamToTempFile(fis);
            Assert.assertNotNull(tempFile);
            Assert.assertNotNull(tempFile.getAbsoluteFile());

        } catch ( Exception e ) {
            logger.error(e);
        } finally {
            IOUtils.closeQuietly(fis);
        }

//        try {
//            fis = new FileInputStream(tempFile);
//            String productPartNumber = billingTrackerImporter.readFromStream( fis );
//            Assert.assertEquals("P-WG-0009", productPartNumber);
//        } catch ( Exception e ) {
//            e.printStackTrace();
//        } finally {
//            IOUtils.closeQuietly(fis);
//        }

    }

}
