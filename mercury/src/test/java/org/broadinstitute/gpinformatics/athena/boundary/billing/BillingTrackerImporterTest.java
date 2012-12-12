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

    public static final String BILLING_TRACKER_TEST_FILENAME = new String("BillingTracker-ContainerTest.xlsx");
    public static final File BILLING_TRACKER_TEST_FILE = new File("src/test/resources/testdata/" + BILLING_TRACKER_TEST_FILENAME);
    private static final Log logger = LogFactory.getLog(BillingTrackerImporterTest.class);

    @Test
    void testCopyFromStream() throws Exception {

        File testFile = BILLING_TRACKER_TEST_FILE;
        BillingTrackerImporter billingTrackerImporter = new BillingTrackerImporter(null);
        FileInputStream fis=null;

        // Test the copying from stream
        try {
            fis = new FileInputStream(testFile);
            File tempFile = billingTrackerImporter.copyFromStreamToTempFile(fis);
            Assert.assertNotNull(tempFile);
            Assert.assertNotNull(tempFile.getAbsoluteFile());
        } catch ( Exception e ) {
            logger.error(e);
            Assert.fail( "Failed : " + e.getMessage() );
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }
}
