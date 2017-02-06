package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;


@Test(groups = TestGroups.DATABASE_FREE)
public class UploadPooledTubesTest {

    @Test
    public void testTubeUpload() {
        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/PooledTubeReg.xlsx");
        Assert.assertNotNull(testSpreadSheetInputStream);
        try {
            VesselPooledTubesProcessor vesselSpreadsheetProcessor = new VesselPooledTubesProcessor("Sheet1");
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());

            parser.processRows(WorkbookFactory.create(testSpreadSheetInputStream).getSheetAt(0), vesselSpreadsheetProcessor);
            Assert.assertEquals(vesselSpreadsheetProcessor.getBarcodes().size(), 1);
            Assert.assertEquals(vesselSpreadsheetProcessor.getMolecularIndexingScheme().get(0), "Illumina_P5-Nijow_P7-Waren");
            Assert.assertEquals(vesselSpreadsheetProcessor.getSingleSampleLibraryName().size(), 1);

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


