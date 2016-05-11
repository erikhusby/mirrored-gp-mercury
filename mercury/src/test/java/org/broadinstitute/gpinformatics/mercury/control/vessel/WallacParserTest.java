package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
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
import java.util.Map;

/**
 * Test parsing of Wallac output file.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class WallacParserTest {

    public static final String WALLAC_96_OUTPUT = "Wallac96WellOutput.xls";
    public static final String WALLAC_384_OUTPUT = "Wallac384WellOutput.xls";

    @Test
    public void testBasic96Well() {
        try {
            Workbook workbook = WorkbookFactory.create(VarioskanParserTest.getSpreadsheet(WALLAC_96_OUTPUT));

            WallacRowParser rowParser = new WallacRowParser(workbook);
            Map<WallacRowParser.NameValue, String> mapNameValueToValue = rowParser.getValues();
            Assert.assertEquals("VICTOR", mapNameValueToValue.get(WallacRowParser.NameValue.INSTRUMENT_NAME));
            Assert.assertNotNull(mapNameValueToValue.get(WallacRowParser.NameValue.RUN_STARTED));
            String plateBarcode1 = mapNameValueToValue.get(WallacRowParser.NameValue.PLATE_BARCODE_1);
            String plateBarcode2 = mapNameValueToValue.get(WallacRowParser.NameValue.PLATE_BARCODE_2);

            WallacPlateProcessor wallacPlateProcessor = new WallacPlateProcessor(WallacRowParser.RESULTS_TABLE_TAB,
                    plateBarcode1, plateBarcode2);
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());

            parser.processRows(workbook.getSheet(WallacRowParser.RESULTS_TABLE_TAB), wallacPlateProcessor);

            Assert.assertEquals(wallacPlateProcessor.getPlateWellResults().size(), 192);
            Assert.assertEquals(wallacPlateProcessor.getMessages().size(), 0);



        } catch (ValidationException | IOException | InvalidFormatException e) {
            throw new RuntimeException(e);
        }
    }
}
