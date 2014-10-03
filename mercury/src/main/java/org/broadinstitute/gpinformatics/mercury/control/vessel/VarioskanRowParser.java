package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser for name / value pairs in Varioskan output, which are not amenable to TableProcessor, because it
 * needs column definitions.
 */
public class VarioskanRowParser {

    public static final int NAME_COLUMN = 1;
    public static final int VALUE_COLUMN = 5;
    public static final String GENERAL_INFO_TAB = "General_Info";
    public static final String QUANTITATIVE_CURVE_FIT1_TAB = "QuantitativeCurveFit1";

    private final Workbook workbook;

    public VarioskanRowParser(Workbook varioskanSpreadsheet) {
        workbook = varioskanSpreadsheet;
    }

    public enum NameValue {
        RUN_NAME("Run name", 6, GENERAL_INFO_TAB),
        RUN_STARTED("Run started", 8, GENERAL_INFO_TAB, "MM/dd/yyyy hh:mm:ss a"),
        INSTRUMENT_NAME("Instrument name", 13, GENERAL_INFO_TAB),
        INSTRUMENT_SERIAL_NUMBER("Instrument serial number", 15, GENERAL_INFO_TAB),

        CORRELATION_COEFFICIENT_R2("Correlation Coefficient R2", 17, QUANTITATIVE_CURVE_FIT1_TAB),
        ;

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

    public VarioskanRowParser(InputStream inputStream) {
        try {
            workbook = WorkbookFactory.create(inputStream);
        } catch (IOException | InvalidFormatException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<NameValue, String> getValues() {
        Map<NameValue, String> mapNameValueToValue = new HashMap<>();
        for (NameValue nameValue : NameValue.values()) {
            Sheet sheet = workbook.getSheet(nameValue.getSheetName());
            Row row = sheet.getRow(nameValue.getRow());
            String name = row.getCell(NAME_COLUMN).getStringCellValue();
            if (!name.equals(nameValue.getFieldName())) {
                // todo jmt accumulate errors?
                throw new RuntimeException("Expected " + nameValue.getFieldName() + ", found " + name);
            }
            String cellValue = PoiSpreadsheetParser.getCellValues(row.getCell(VALUE_COLUMN),
                    nameValue.getDateFormat() != null, true);
            mapNameValueToValue.put(nameValue, cellValue);
        }
        return mapNameValueToValue;
    }
}
