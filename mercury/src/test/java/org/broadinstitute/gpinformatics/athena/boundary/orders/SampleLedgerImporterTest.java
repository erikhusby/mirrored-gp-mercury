package org.broadinstitute.gpinformatics.athena.boundary.orders;

import junit.framework.Assert;
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

@Test(groups = TestGroups.DATABASE_FREE)
public class SampleLedgerImporterTest {

    public static final File BILLING_TRACKER_TEST_FILE = new File("src/test/data/billing/BillingTracker-2012-11-21.xlsx");

    @Test
    void testImport() throws Exception {

        File testFile = BILLING_TRACKER_TEST_FILE;

        SampleLedgerImporter sampleLedgerImporter = new SampleLedgerImporter();
        FileInputStream fis=null;
        File tempFile=null;

        try {
            fis = new FileInputStream(testFile);
            tempFile = sampleLedgerImporter.copyFromStreamToTempFile(fis);
            Assert.assertNotNull(tempFile);
            Assert.assertNotNull(tempFile.getAbsoluteFile());

        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(fis);
        }

//        try {
//            fis = new FileInputStream(tempFile);
//            String productPartNumber = sampleLedgerImporter.readFromStream( fis );
//            Assert.assertEquals("P-WG-0009", productPartNumber);
//        } catch ( Exception e ) {
//            e.printStackTrace();
//        } finally {
//            IOUtils.closeQuietly(fis);
//        }

        try {
            fis = new FileInputStream(tempFile);
            Workbook workbook = WorkbookFactory.create(fis);
            Sheet sheet = workbook.getSheetAt(0);
            Row row0 = sheet.getRow(0);
            List<SampleLedgerImporter.TrackerColumnInfo> trackerHeaderList = sampleLedgerImporter.parseTrackerSheetHeader(row0, "P-RNA-0004");
            Assert.assertNotNull(trackerHeaderList);
            Assert.assertEquals(4, trackerHeaderList.size());
        } finally {
            IOUtils.closeQuietly(fis);
        }

//
//        try {
//            fis = new FileInputStream(tempFile);
//            Map<String, Map<String, Map<String, OrderBillSummaryStat>>>  dataMap = sampleLedgerImporter.parseTempFile( tempFile );
//            Assert.assertNotNull(dataMap);
//        } finally {
//            IOUtils.closeQuietly(fis);
//        }

    }



    void parseTrackerSheetHeader() throws Exception {

    }

    @Test
    void testExtractPartNumberFromHeader() throws Exception {

        SampleLedgerImporter sampleLedgerImporter = new SampleLedgerImporter();
        String headerTest = "DNA Extract from blood, fresh frozen tissue, cell pellet, stool, or saliva [P-ESH-0004]";
        Assert.assertEquals("P-ESH-0004", sampleLedgerImporter.extractPartNumberFromHeader(headerTest));

        headerTest = "DNA Extract from blood, fresh frozen tissue, cell pellet, stool, or saliva [ ]";
        Assert.assertEquals(" ", sampleLedgerImporter.extractPartNumberFromHeader(headerTest));

        headerTest = "DNA Extract from blood, fresh frozen tissue, cell pellet, stool, or saliva [P-ESH-[0004]]";
        Assert.assertEquals("0004]", sampleLedgerImporter.extractPartNumberFromHeader(headerTest));

        headerTest = "DNA Extract from blood, fresh frozen tissue, cell pellet, stool, or saliva []";
        try {
            sampleLedgerImporter.extractPartNumberFromHeader(headerTest);
            Assert.fail( "should have thrown on failure to find ppn");
        } catch ( Exception e) {}

        headerTest = "DNA Extract from blood, fresh frozen tissue, cell pellet, stool, or saliva ]P-ESH-0004[";
        try {
            sampleLedgerImporter.extractPartNumberFromHeader(headerTest);
            Assert.fail( "should have thrown on failure to find ppn");
        } catch ( Exception e) {}

    }



}
