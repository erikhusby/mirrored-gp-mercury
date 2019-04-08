package org.broadinstitute.gpinformatics.infrastructure.spreadsheet;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;


public class SpreadsheetCreator {
    private static final int MAX_ROW_NUMBER = 65536;

    public static Workbook createEmptySpreadsheet() {
        return new HSSFWorkbook();
    }

    /**
     * Create a Workbook so the caller can manipulate the output beyond just dumping data into a
     * sheet (adding freezepanes, styles, etc).
     *
     * @return the workbook
     */
    public static Workbook createSpreadsheet(String sheetName, Object[][] rows) {
        // Create a new workbook.
        Workbook workbook = createEmptySpreadsheet();
        createSheet(sheetName, rows, workbook);
        return workbook;
    }

    public static void createSpreadsheet(String sheetName, Object[][] rows,
            OutputStream out) throws IOException {
        // create a new workbook
        Workbook workbook = createSpreadsheet(sheetName, rows);

        workbook.write(out);
        // relies on the caller to close the output stream!!
    }

    public static Workbook createSpreadsheet(Map<String, Object[][]> allSheets) {
        String[] orderedSheetNames = new String[allSheets.size()];
        int count = 0;
        for (String sheetName : allSheets.keySet()) {
            orderedSheetNames[count++] = sheetName;
        }
        return createSpreadsheet(orderedSheetNames, allSheets);
    }

    private static Workbook createSpreadsheet(String[] orderedSheetNames, Map<String, Object[][]> allSheets) {
        // create a new workbook
        Workbook workbook = createEmptySpreadsheet();

        if (orderedSheetNames.length == 0 || allSheets.isEmpty()) {
            throw new RuntimeException("No sheets were found to write out.");
        }

        for (String sheetName : orderedSheetNames) {
            createSheet(sheetName, allSheets.get(sheetName), workbook);
        }
        return workbook;
    }

    public static void createSpreadsheet(Map<String, Object[][]> allSheets,
            OutputStream out) throws IOException {
        Workbook workbook = createSpreadsheet(allSheets);
        workbook.write(out);
    }


    public static void createSpreadsheet(String[] orderedSheetNames,
            Map<String, Object[][]> allSheets,
            OutputStream out) throws IOException {
        Workbook workbook = createSpreadsheet(orderedSheetNames, allSheets);
        // write the workbook to the output stream
        workbook.write(out);
        // replies on the caller to close the output stream!!
    }

    // These magic numbers come from the BuiltinFormats javadoc.
    public enum POIBuiltinFormats {
        General(0),
        Integer(1),
        Decimal(2),
        Date(0x16),
        Text(0x31),
        Header(0x31);

        private String name;

        POIBuiltinFormats(int value) {
            name = BuiltinFormats.getBuiltinFormat(value);
        }

        public CellStyle createStyle(Workbook wb, Font font, DataFormat dataFormat) {
            CellStyle style = wb.createCellStyle();
            style.setFont(font);
            style.setDataFormat(dataFormat.getFormat(name));
            return style;
        }

        public CellStyle createHeaderStyle(Workbook wb, Font font, DataFormat dataFormat) {
            CellStyle style = wb.createCellStyle();
            style.setFont(font);
            style.setDataFormat(dataFormat.getFormat(name));
            style.setBorderBottom(BorderStyle.MEDIUM);
            return style;
        }
    }

    /**
     * Create a worksheet and add it to the workbook.
     *
     * @param sheetName the name of the sheet to add
     * @param rows      the spreadsheet data
     * @param workbook        the workbook to add the sheet to
     */
    public static void createSheet(String sheetName, Object[][] rows, Workbook workbook) {

        if (StringUtils.isEmpty(sheetName)) {
            throw new RuntimeException("Cannot have a blank worksheet name.");
        }

        boolean isHeader = false;

        // create a new sheet
        Sheet sheet = workbook.createSheet(sheetName);

        DataFormat dataFormat = workbook.createDataFormat();
        // create font objects
        Font font = workbook.createFont();
        // set font to 10 point type
        font.setFontHeightInPoints((short) 10);
        // make it black
        font.setColor(Font.COLOR_NORMAL);

        Font headerFont = workbook.createFont();
        // set font to 10 point type
        headerFont.setFontHeightInPoints((short) 10);
        // make it black and bold
        headerFont.setColor(Font.COLOR_NORMAL);
        headerFont.setBold(true);

        // Create cell styles.
        Map<POIBuiltinFormats, CellStyle> styleMap =
                new EnumMap<>(POIBuiltinFormats.class);
        for (POIBuiltinFormats format : POIBuiltinFormats.values()) {
            styleMap.put(format, format.createStyle(workbook, font, dataFormat));
        }
        // Manually create a header format
        styleMap.put(POIBuiltinFormats.Header, POIBuiltinFormats.Header.createHeaderStyle(workbook, headerFont, dataFormat));

        if (rows.length > MAX_ROW_NUMBER) {
            throw new RuntimeException(String.format(
                    "Cannot create a spreadsheet with more then %d rows.",
                    MAX_ROW_NUMBER));
        }

        int rowNum;
        for (rowNum = 0; rowNum < rows.length; rowNum++) {
            // create a row
            Row row = sheet.createRow(rowNum);
            if (rows[rowNum] != null) {
                for (int cellNum = 0; cellNum < rows[rowNum].length; cellNum++) {
                    Cell cell = row.createCell(cellNum);

                    Object data = rows[rowNum][cellNum];
                    if (data instanceof String) {
                        cell.setCellStyle(styleMap.get(POIBuiltinFormats.Text));
                        cell.setCellValue((String) data);
                    } else if (data instanceof Number) {
                        if ((data instanceof Integer) || (data instanceof Long)) {
                            cell.setCellStyle(styleMap.get(POIBuiltinFormats.Integer));
                            cell.setCellValue(((Number) data).intValue());
                        } else {
                            cell.setCellStyle(styleMap.get(POIBuiltinFormats.Decimal));
                            cell.setCellValue(((Number) data).doubleValue());
                        }
                    } else if (data instanceof Date) {
                        cell.setCellStyle(styleMap.get(POIBuiltinFormats.Date));
                        cell.setCellValue((Date) data);
                    } else if (data instanceof ExcelHeader) {
                        cell.setCellStyle(styleMap.get(POIBuiltinFormats.Header));
                        cell.setCellValue(data.toString());
                    } else {
                        cell.setCellStyle(styleMap.get(POIBuiltinFormats.General));
                        if (data != null) {
                            cell.setCellValue(data.toString());
                        }
                    }
                }
            }
        }
        sheet.createRow(rowNum);
        for (int cellNum = 0; cellNum <= rows[rowNum-1].length; cellNum++) {
            sheet.autoSizeColumn(cellNum);
        }
    }

    /**
     * Avery t hin wrapper to allow consumer to wrap values (typically a String)
     *    to flag for format as a header in spreadsheet (or ignore in any String based export)
     */
    public static class ExcelHeader {
        private String label;
        public ExcelHeader( String label ) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
