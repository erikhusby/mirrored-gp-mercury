package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser for name / value pairs in Wallac output, which are not amenable to TableProcessor, because it
 * needs column definitions.
 */
public class WallacRowParser {
    public static final int NAME_COLUMN = 0;
    public static final int VALUE_COLUMN = 1;
    public static final String OVERVIEW_TAB = "(1) Overview";
    public static final String MEASURE_DETAILS_TAB = "(2) Measurement Details";
    public static final String RESULTS_TABLE_TAB = "(7) Results Table";

    private final Workbook workbook;

    public WallacRowParser(Workbook varioskanSpreadsheet) {
        workbook = varioskanSpreadsheet;
    }

    public WallacRowParser(InputStream inputStream) {
        try {
            workbook = WorkbookFactory.create(inputStream);
        } catch (IOException | InvalidFormatException e) {
            throw new RuntimeException(e);
        }
    }

    public enum NameValue {
        RUN_STARTED("Measurement Start Time:", 2, MEASURE_DETAILS_TAB, "EEEE, MMMM d, yyyy HH:mm:ss a"),
        INSTRUMENT_NAME("Instrument:", 1, OVERVIEW_TAB),
        PLATE_BARCODE_1("Barcode Plate 2:", 31, MEASURE_DETAILS_TAB), //'Barcode Plate 1:' is the standards plate
        PLATE_BARCODE_2("Barcode Plate 3:", 32, MEASURE_DETAILS_TAB);

        private String fieldName;
        private int row;
        private String sheetName;
        private String dateFormat;

        NameValue(String fieldName, int row, String sheetName) {
            this.fieldName = fieldName;
            this.row = row;
            this.sheetName = sheetName;
        }

        NameValue(String name, int row, String sheetName, String dateFormat) {
            this(name, row, sheetName);
            this.dateFormat = dateFormat;
        }

        public String getFieldName() {
            return fieldName;
        }

        public int getRow() {
            return row;
        }

        public String getSheetName() {
            return sheetName;
        }

        public String getDateFormat() {
            return dateFormat;
        }
    }

    public Map<NameValue, String> getValues() {
        Map<NameValue, String> mapNameValueToValue = new HashMap<>();
        for (NameValue nameValue : NameValue.values()) {
            Sheet sheet = workbook.getSheet(nameValue.getSheetName());
            Row row = sheet.getRow(nameValue.getRow());
            String name = row.getCell(NAME_COLUMN).getStringCellValue();
            if (!name.equals(nameValue.getFieldName())) {
                throw new RuntimeException("Expected " + nameValue.getFieldName() + ", found " + name);
            }
            String cellValue = PoiSpreadsheetParser.getCellValues(row.getCell(VALUE_COLUMN),
                    nameValue.getDateFormat() != null, true);
            mapNameValueToValue.put(nameValue, cellValue);
        }
        return mapNameValueToValue;
    }
}
