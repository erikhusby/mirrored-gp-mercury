package org.broadinstitute.gpinformatics.mercury.control.vessel;

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
 * Test parsing of Varioskan file.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class VarioskanParserTest {

    @Test
    public void testBasic() {
        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/VarioskanOutput.xls");
        Assert.assertNotNull(testSpreadSheetInputStream);
        try {
            VarioskanPlateProcessor varioskanPlateProcessor = new VarioskanPlateProcessor("QuantitativeCurveFit1"/*, null, null, "thompson"*/);
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());

            parser.processRows(WorkbookFactory.create(testSpreadSheetInputStream).getSheet("QuantitativeCurveFit1"),
                    varioskanPlateProcessor);
            Assert.assertEquals(varioskanPlateProcessor.getPlateWellResults().size(), 192);
            Assert.assertEquals(varioskanPlateProcessor.getMessages().size(), 0);
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
