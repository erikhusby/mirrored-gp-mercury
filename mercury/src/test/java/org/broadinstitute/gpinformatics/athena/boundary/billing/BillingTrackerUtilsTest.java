package org.broadinstitute.gpinformatics.athena.boundary.billing;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 12/5/12
 * Time: 11:31 AM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class BillingTrackerUtilsTest extends TestCase {

    public static final File BILLING_TRACKER_TEST_FILE = new File("src/test/data/billing/BillingTracker-DBFreeTestData.xlsx");
    private static final Log logger = LogFactory.getLog(BillingTrackerUtilsTest.class);

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

        try {
            fis = new FileInputStream(tempFile);
            Workbook workbook = WorkbookFactory.create(fis);
            Sheet sheet = workbook.getSheetAt(0);
            Row row0 = sheet.getRow(0);
            List<TrackerColumnInfo> trackerHeaderList = BillingTrackerUtils.parseTrackerSheetHeader(row0, "P-RNA-0004");
            Assert.assertNotNull(trackerHeaderList);
            Assert.assertEquals(4, trackerHeaderList.size());
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    @Test
    void testExtractPartNumberFromHeader() throws Exception {

        BillingTrackerImporter billingTrackerImporter = new BillingTrackerImporter(null);
        String headerTest = "DNA Extract from blood, fresh frozen tissue, cell pellet, stool, or saliva [P-ESH-0004]";
        Assert.assertEquals("P-ESH-0004", BillingTrackerUtils.extractBillableRefFromHeader(headerTest).getProductPartNumber());
        Assert.assertEquals("DNA Extract from blood, fresh frozen tissue, cell pellet, stool, or saliva",
                BillingTrackerUtils.extractBillableRefFromHeader(headerTest).getPriceItemName() );

        headerTest = "DNA Extract from blood, fresh frozen tissue, cell pellet, stool, or saliva [ ]";
        Assert.assertEquals(" ", BillingTrackerUtils.extractBillableRefFromHeader(headerTest).getProductPartNumber() );

        headerTest = "DNA Extract from blood, fresh frozen tissue, cell pellet, stool, or saliva [P-ESH-[0004]]";
        Assert.assertEquals("0004]", BillingTrackerUtils.extractBillableRefFromHeader(headerTest).getProductPartNumber() );

        headerTest = "DNA Extract from blood, fresh frozen tissue, cell pellet, stool, or saliva []";
        try {
            BillingTrackerUtils.extractBillableRefFromHeader(headerTest).getProductPartNumber();
            Assert.fail( "should have thrown on failure to find ppn");
        } catch ( Exception e) {}

        headerTest = "DNA Extract from blood, fresh frozen tissue, cell pellet, stool, or saliva ]P-ESH-0004[";
        try {
            BillingTrackerUtils.extractBillableRefFromHeader(headerTest).getProductPartNumber();
            Assert.fail( "should have thrown on failure to find ppn");
        } catch ( Exception e) {}

    }

}
