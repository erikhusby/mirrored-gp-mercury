package org.broadinstitute.gpinformatics.infrastructure.parsers.poi;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    protected List<String> validationMessages = new ArrayList<>();

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
     * @throws ValidationException
     */
    public void processRows(Sheet workSheet, TableProcessor processor) throws ValidationException {
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
     * Return a List of all blank lines seen in the input List of blank line indexes which do not represent trailing
     * blank lines.
     *
     * @param allBlankLineIndexesSeen input List of blank line index numbers, in increasing order, 1-based.
     * @param numRows The number of data rows in the spreadsheet.
     * @return A List of non-trailing blank line indexes in increasing order, 1-based.
     */
    static List<Integer> findNonTrailingBlankLineIndexes(List<Integer> allBlankLineIndexesSeen, int numRows) {
        // Trailing blank lines should all be at the end, reverse the input List and start looking for
        // consecutive blank lines, if any.  The loop below will remove any trailing blank lines from this List,
        // any remaining blank lines will be returned to the caller.
        List<Integer> nonTrailingBlankLineIndexes = new ArrayList<>(Lists.reverse(allBlankLineIndexesSeen));
        int currentLastIndex = numRows;

        Iterator<Integer> iterator = nonTrailingBlankLineIndexes.iterator();
        while (iterator.hasNext()) {
            Integer lastIndex = iterator.next();
            // If the trailing blank line is not where it is supposed to be, break out of the loop and
            // report on all the blank lines remaining in the input List of blank lines.
            if (lastIndex != currentLastIndex) {
                break;
            }
            // If the trailing blank line index is where it was expected to be, remove this from the List of
            // reported trailing blank lines.
            currentLastIndex--;
            iterator.remove();
        }

        return Lists.reverse(nonTrailingBlankLineIndexes);
    }

    /**
     * Process the data portion of the spreadsheet.
     *
     * @param processor The processor being used.
     * @param rows The iterator on the excel rows.
     */
    private void processData(TableProcessor processor, Iterator<Row> rows) {
        List<Integer> allBlankLinesSeen = new ArrayList<>();

        // Keep a count of the number of rows seen, this will be important for trailing blank line determination.
        int numRows = 0;
        while (rows.hasNext()) {
            Row row = rows.next();
            numRows++;

            boolean currentLineIsBlank = false;
            // Create a mapping of the headers to the cell values. There are never date values for the header.
            Map<String, String> dataByHeader = new HashMap<>();
            for (int i = 0; i < processor.getHeaderNames().size(); i++) {
                String headerName = processor.getHeaderNames().get(i);
                dataByHeader.put(headerName,
                        extractCellContent(row, headerName, i, processor.isDateColumn(i), processor.isStringColumn(i)));
            }

            if (processor.getIgnoreTrailingBlankLines() && representsBlankLine(dataByHeader.values())) {
                allBlankLinesSeen.add(row.getRowNum());
                currentLineIsBlank = true;
            }

            // Process the row unless the TableProcessor is configured to ignore blank lines and the current line is
            // blank.
            if (!(processor.getIgnoreTrailingBlankLines() && currentLineIsBlank)) {
                // Take the map and turn it into objects and process the data appropriately.
                processor.processRow(dataByHeader, row.getRowNum());
            }
        }

        // If there were any blank lines that do not represent trailing blank lines, report these to the TableProcessor
        // implementation.  This only needs to be handled specially if the processor is configured to ignore trailing
        // blank lines, otherwise the parser would have already been given the blank line via #processRow.
        if (processor.getIgnoreTrailingBlankLines()) {
            Collection<Integer> nonTrailingBlankLineIndexes =
                    findNonTrailingBlankLineIndexes(allBlankLinesSeen, numRows);
            if (!nonTrailingBlankLineIndexes.isEmpty()) {
                processor.generateErrorsForNonTrailingBlankLines(nonTrailingBlankLineIndexes);
            }
        }
    }

    /**
     * Process the headers.
     *
     * @param processor The Table Processor that will be turning rows of data into objects based on headers.
     * @param rows The row iterator.
     */
    private static void processHeaders(TableProcessor processor, Iterator<Row> rows) throws ValidationException {
        int headerRowIndex = processor.getHeaderRowIndex();
        int headerRowNum = 0;
        int numHeaderRows = processor.getNumHeaderRows();
        while (rows.hasNext() && headerRowNum < numHeaderRows) {
            Row headerRow = rows.next();
            if (headerRow.getRowNum() < headerRowIndex) {
                continue;
            }

            List<String> headers = new ArrayList<>();
            Iterator<Cell> cellIterator = headerRow.cellIterator();
            while (cellIterator.hasNext()) {
                // Headers are always strings, so the false is for the date and the true is in case the header looks
                // like a number to excel
                headers.add(getCellValues(cellIterator.next(), false, true));
            }

            // The primary header row is the one that needs to be generally validated.
            if (processor.getPrimaryHeaderRow() == headerRowIndex) {
                if (!processor.validateHeaders(headers)) {
                    throw new ValidationException("Error parsing headers.", processor.getMessages());
                }
            }

            // Turn the header strings for this row into whatever objects are needed to continue on.
            processor.processHeader(headers, headerRowNum++);
        }
    }

    /**
     * Initial point of contact for a parser (except if overwritten by a sub parser).  This method is responsible for
     * coordinating the reading and parsing of a given spreadsheet file.
     *
     * @param fileStream input stream that represents the Spreadsheet that has been uploaded by the user.
     *
     * @throws IOException
     * @throws InvalidFormatException
     * @throws ValidationException
     */
    public void processUploadFile(InputStream fileStream) throws IOException, InvalidFormatException, ValidationException {
        processWorkSheets(WorkbookFactory.create(fileStream));
    }

    public void processUploadFile(File file) throws IOException, InvalidFormatException, ValidationException {
        processWorkSheets(WorkbookFactory.create(file));
    }

    private void processWorkSheets(Workbook workbook) throws ValidationException {
        // Go through each processor and process the sheet specified.
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
     * @param headerName Represents the specific column of the given row to extract
     * @param columnIndex The column number to process
     *
     * @return A string representation of the data in the cell indicated by the given row/column (header) combination
     */
    protected String extractCellContent(Row row, String headerName, int columnIndex, boolean isDate, boolean isString) {

        Cell cell = row.getCell(columnIndex);

        String result = getCellValues(cell, isDate, isString);
        if (StringUtils.isBlank(result)) {
            validationMessages.add("Row # " + row.getRowNum() + ": Unable to determine cell type for " + headerName);
        }

        return result;
    }

    /**
     * We leave all parsing and validating up to the caller by turning everything into a string. We might want to
     * let POI turn things into real objects in the map in the future, but for now this was what callers were
     * expecting.
     * <p/>
     * <b>Note, if your cell contains a formula, this method will return not the calculated value, nor the formula
     * but an empty string instead.</b>
     *
     * @param cell The cell data.
     *
     * @return A string representation of the cell.
     */
    public static String getCellValues(Cell cell, boolean isDate, boolean isString) {
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
                return cell.getStringCellValue();
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
     * @return the list of validation messages, if any
     *
     * @throws InvalidFormatException Formatting issues
     * @throws IOException File issues
     * @throws ValidationException Any problems with the data in the spreadsheet
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
