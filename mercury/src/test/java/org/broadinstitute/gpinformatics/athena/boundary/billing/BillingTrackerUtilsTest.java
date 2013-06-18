package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.testng.Assert;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
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

    public static final String BILLING_TRACKER_TEST_FILENAME = "BillingTracker-ContainerTest.xlsx";
    public static final File BILLING_TRACKER_TEST_FILE = new File("src/test/resources/testdata/" + BILLING_TRACKER_TEST_FILENAME);

    @Test
    public void testExtractPartNumberFromHeader() throws Exception {

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
        } catch ( Exception e) {
            // expect exception to be thrown
            Assert.assertTrue( e.getMessage().contains( "Tracker Sheet Header Format Error.  Could not find product partNumber in column header.") );
        }

        headerTest = "DNA Extract from blood, fresh frozen tissue, cell pellet, stool, or saliva ]P-ESH-0004[";
        try {
            BillingTrackerUtils.extractBillableRefFromHeader(headerTest).getProductPartNumber();
            Assert.fail( "should have thrown on failure to find ppn");
        } catch ( Exception e) {
            // expect exception to be thrown
            Assert.assertTrue( e.getMessage().contains( "Tracker Sheet Header Format Error.  Could not find product partNumber in column header.") );
        }

    }

}
