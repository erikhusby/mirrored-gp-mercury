package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

/**
 * Tests the upload of a spreadsheet containing sample IDs and 2D tube barcodes.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class UploadSampleVesselsTest {

    @Test
    public void testBasic() {
        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/SampleVessel.xlsx");
        Assert.assertNotNull(testSpreadSheetInputStream);
        try {
            SampleVesselProcessor sampleVesselProcessor = new SampleVesselProcessor("Test");
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());

            parser.processRows(WorkbookFactory.create(testSpreadSheetInputStream).getSheetAt(0), sampleVesselProcessor);
            Assert.assertEquals(sampleVesselProcessor.getMapBarcodeToParentVessel().size(), 1);
            Assert.assertEquals(sampleVesselProcessor.getMapBarcodeToParentVessel().values().iterator().next().
                    getChildVesselBeans().size(), 16);
        } catch (ValidationException | IOException | InvalidFormatException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                testSpreadSheetInputStream.close();
            } catch (IOException ignored) {
            }
        }
    }
}
