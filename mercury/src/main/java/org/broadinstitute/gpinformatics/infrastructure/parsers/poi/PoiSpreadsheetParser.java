package org.broadinstitute.gpinformatics.infrastructure.parsers.poi;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.HeaderValueRowTableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Handle the processing of a stream of table data using POI to parse an excel file.
 */
public final class PoiSpreadsheetParser {

    private static final Format DATE_FORMATTER = FastDateFormat.getInstance("MM/dd/yyyy");

    // This maps table processors by sheet name.
    private final Map<String, ? extends TableProcessor> processorMap;

    public PoiSpreadsheetParser(@Nonnull Map<String, ? extends TableProcessor> processorMap) {
        this.processorMap = processorMap;
    }

    /**
     * processWorkbook contains the generic guts of parsing a spreadsheet.  This method is responsible for pulling
     * the data, row by row, from the spreadsheet file and holding that data in such a way that a concrete parser
     * can parse the row data in a way specific to that parser.
     *
     * @throws ValidationException if the header row cannot be found or the header is missing required columns.
     * Other validation errors such as missing data values can be found in processor.getMessages().
     */
    public void processRows(Sheet workSheet, TableProcessor processor) throws ValidationException {
        // If the index is invalid then sets it by parsing the spreadsheet and searching for the header strings.
        if (processor.getHeaderRowIndex() < 0 && processor instanceof HeaderValueRowTableProcessor) {
            processor.setHeaderRowIndex(((HeaderValueRowTableProcessor)processor).findHeaderRow(workSheet));
        }
        Iterator<Row> rows = workSheet.rowIterator();
        processHeaders(processor, rows);
        processData(processor, rows);
    }

    /**
     * If all values in this row are blank return true, otherwise false.
     */
    static boolean representsBlankLine(Collection<String> values) {
        for (String value : values) {
            if (!StringUtils.isBlank(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Process the data portion of the spreadsheet.
     *
     * @param processor The processor being used.
     * @param rows The iterator on the excel rows, positioned at the first row after the header row.
     */
    private void processData(TableProcessor processor, Iterator<Row> rows) {
        // Buffer of blank lines used only for TableProcessors where #shouldIgnoreTrailingBlankLines is true.
        List<Pair<Row, Map<String, String>>> blankLineBuffer = new ArrayList<>();
        boolean shouldIgnoreTrailingBlankLines = processor.shouldIgnoreTrailingBlankLines();

        while (rows.hasNext()) {
            Row row = rows.next();
            // Create a mapping of the headers to the cell values. There are never date values for the header.
            Map<String, String> dataByHeader = new HashMap<>();
            for (String headerName : processor.getHeaderToColumnIndex().keySet()) {
                int columnIdx = processor.getHeaderToColumnIndex().get(headerName);
                ColumnHeader columnHeader = processor.findColumnHeaderByName(headerName);
                dataByHeader.put(headerName, extractCellContent(row, columnIdx, columnHeader));
            }

            boolean isBlankLine = false;
            if (shouldIgnoreTrailingBlankLines) {
                isBlankLine = representsBlankLine(dataByHeader.values());
                if (isBlankLine) {
                    blankLineBuffer.add(Pair.of(row, dataByHeader));
                } else {
                    // If there are any blank lines in the buffer they were in the middle of the file, flush them
                    // to the processor.
                    for (Pair<Row, Map<String, String>> pair : blankLineBuffer) {
                        processor.processRow(pair.getRight(), pair.getLeft().getRowNum());
                    }
                    blankLineBuffer.clear();
                }
            }

            // Unless this processor ignores trailing blank lines and the line is blank, the processor should
            // process the row data.
            if (!(shouldIgnoreTrailingBlankLines && isBlankLine)) {
                // Take the map and turn it into objects and process the data appropriately.
                processor.processRow(dataByHeader, row.getRowNum());
            }
        }
        // At this point any blank lines remaining in blankLineBuffer are deliberately ignored and not sent to
        // the processor.
    }


    /**
     * Process the rows up to and including the header row. If the processor is a HeaderValueRowTableProcessor
     * then the rows before the row header are treated as HeaderValueRow rows, which can contain a header column
     * followed by value column(s).
     *
     * @param processor The Table Processor that will be turning rows of data into objects based on headers.
     * @param rows The row iterator which gets advanced past the header row.
     * @throws ValidationException if the header row cannot be found or the header is missing required columns.
     */
    private void processHeaders(TableProcessor processor, Iterator<Row> rows) throws ValidationException {
        HeaderValueRowTableProcessor processorCast = (processor instanceof HeaderValueRowTableProcessor) ?
                (HeaderValueRowTableProcessor) processor : null;
        // headerRowIndex is the 0-based index of the first header row.
        final int headerRowIndex = processor.getHeaderRowIndex();
        List<String> headers = new ArrayList<>();
        while (rows.hasNext()) {
            Row headerRow = rows.next();
            if (headerRow.getRowNum() < headerRowIndex) {
                if (processorCast != null) {
                    processorCast.processHeaderValueRow(headerRow);
                }
            } else {
                Iterator<Cell> cellIterator = headerRow.cellIterator();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    // Expects headers to be strings, possibly modified for more robust matching.
                    // Only maps the headers defined with an enum. Any others are ignored.
                    String headerString = processor.adjustHeaderCell(getCellValues(cell, false, true));
                    ColumnHeader columnHeader = processor.findColumnHeaderByName(headerString);
                    if (columnHeader != null) {
                        headers.add(headerString);
                        // Makes a map of header string to column index for later processing of data rows.
                        processor.getHeaderToColumnIndex().put(headerString, cell.getColumnIndex());
                    }
                }
                break;
            }
        }
        if (!processor.validateColumnHeaders(headers)){
            throw new ValidationException("Failed to validate headers at row " + (headerRowIndex + 1),
                    processor.getMessages());
        }
        if (processorCast != null) {
            processorCast.validateHeaderValueRows();
        }
        // Turn the header strings for this row into whatever objects are needed to continue on.
        processor.processHeader(headers, headerRowIndex + 1);
    }

    /**
     * Initial point of contact for a parser (except if overwritten by a sub parser).  This method is responsible for
     * coordinating the reading and parsing of a given spreadsheet file.
     *
     * @param fileStream input stream that represents the Spreadsheet that has been uploaded by the user.
     *
     * @throws IOException
     * @throws InvalidFormatException
     * @throws ValidationException if the header row cannot be found or is missing required columns.
     * Other validation errors can be found in processor.getMessages().
     */
    public void processUploadFile(InputStream fileStream) throws IOException, InvalidFormatException,
            ValidationException {
        processWorkSheets(WorkbookFactory.create(fileStream));
    }

    public void processUploadFile(File file) throws IOException, InvalidFormatException, ValidationException {
        processWorkSheets(WorkbookFactory.create(file));
    }

    /** Processes the rows on each sheet. */
    private void processWorkSheets(Workbook workbook) throws ValidationException {
        for (String sheetName : processorMap.keySet()) {
            Sheet workSheet = workbook.getSheet(sheetName);
            if (workSheet == null) {
                throw new ValidationException("No worksheet named " + sheetName + " found");
            }

            processRows(workSheet, processorMap.get(sheetName));
        }
    }

    /**
     * Helper method to pull data from individual cells taking into account the cell format.  The cell content is
     * converted to strings to allow for a more generic approach to representing the data.  This allows the abstract
     * parser to handle pulling out the data specific to the POI implementation, and allows the concrete parsers to
     * parse the data not caring whether or not it came from a spreadsheet.
     *
     * @param row Represents a row in the spreadsheet file to be parsed
     * @param columnIndex a 0-based index that identifies which column to extract from the row.
     * @param header the ColumnHeader for this cell.
     *
     * @return A string representation of the data in the cell indicated by the given row/column (header) combination
     */
    protected @NotNull String extractCellContent(Row row, int columnIndex, ColumnHeader header) {
        Cell cell = row.getCell(columnIndex);
        return getCellValues(cell, header.isDateColumn(), header.isStringColumn());
    }

    /**
     * First parses the cell value using POI's notion of boolean, number, or string, then selectively
     * applies the datatype parameters to format the returned String value.
     * <p/>
     * <b>Note, if your cell contains a formula, this method will return not the calculated value, nor the formula
     * but an empty string instead.</b>
     *
     * @param cell The cell data.
     * @param isDate causes a numeric cell to be read as a date.
     * @param isString causes a numeric cell to be read as a string.
     * @return A non-null string representation of the cell.
     */
    public static @NotNull String getCellValues(Cell cell, boolean isDate, boolean isString) {
        if (cell != null) {
            switch (cell.getCellType()) {
            case Cell.CELL_TYPE_BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case Cell.CELL_TYPE_NUMERIC:
                if (isDate) {
                    return DATE_FORMATTER.format(cell.getDateCellValue());
                }
                if (isString) {
                    cell.setCellType(Cell.CELL_TYPE_STRING);
                    return cell.getStringCellValue();
                }
                return String.valueOf(cell.getNumericCellValue());
            case Cell.CELL_TYPE_STRING:
                return cell.getStringCellValue().trim();
            }
        }

        return "";
        // todo jmt this could all be replaced with return new HSSFDataFormatter().formatCellValue( cell );
    }

    /**
     * Get the names of all the worksheets in the tracker file.
     *
     * @param trackerFile The file to open and grab the worksheet names.
     *
     * @return The names.
     */
    public static List<String> getWorksheetNames(File trackerFile) throws IOException, InvalidFormatException {
        try (InputStream inputStream = new FileInputStream(trackerFile)) {
            return getWorksheetNames(inputStream);
        }
    }

    public static List<String> getWorksheetNames(InputStream inputStream) throws IOException, InvalidFormatException {

        List<String> sheetNames = new ArrayList<> ();

        /*
         * JavaDoc for WorkbookFactory.create says the input stream "MUST either support mark/reset, or be wrapped as a
         * PushbackInputStream!"
         */
        if (!inputStream.markSupported()) {
            inputStream = new PushbackInputStream(inputStream);
        }
        Workbook workbook = WorkbookFactory.create(inputStream);
        int numberOfSheets = workbook.getNumberOfSheets();
        for (int i = 0; i < numberOfSheets; i++) {
            Sheet sheet = workbook.getSheetAt(i);
            sheetNames.add(sheet.getSheetName());
        }

        return sheetNames;
    }

    public void close() {
        for (TableProcessor processor : processorMap.values()) {
            processor.close();
        }
    }

    /**
     * Process a single worksheet from a spreadsheet. Only the first sheet is processed, and the sheet name isn't
     * verified.
     *
     * @param spreadsheet The spreadsheet stream of data.
     * @param processor The table processor.
     *
     * @return the list of validation error messages, such as a missing data value when the ColumnHeader
     * indicates that it is required.
     *
     * @throws InvalidFormatException Formatting issues
     * @throws IOException File issues
     * @throws ValidationException if the header row cannot be found or is the header is missing required columns
     * (defined in ColumnHeader). Other validation errors are in the returned list.
     */
    public static List<String> processSingleWorksheet(InputStream spreadsheet, TableProcessor processor)
            throws InvalidFormatException, IOException, ValidationException {
        PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());
        try {
            Workbook workbook = WorkbookFactory.create(spreadsheet);
            processor.validateNumberOfWorksheets(workbook.getNumberOfSheets());
            parser.processRows(workbook.getSheetAt(0), processor);
            return processor.getMessages();
        } finally {
            processor.close();
        }
    }
}
