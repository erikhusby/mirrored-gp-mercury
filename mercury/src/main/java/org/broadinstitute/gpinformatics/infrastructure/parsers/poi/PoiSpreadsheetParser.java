package org.broadinstitute.gpinformatics.infrastructure.parsers.poi;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabMetricProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Handle the processing of a stream of table data using POI to parse an excel file.
 */
public final class PoiSpreadsheetParser implements Serializable {

    private static final String DATE_PATTERN = "MM/dd/yyyy";

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_PATTERN);

    private static final long serialVersionUID = 1294878041185823009L;
    protected List<String> validationMessages = new ArrayList<>();
    private Workbook workbook;

    // This maps table processors by sheet name.
    private final Map<String, ? extends TableProcessor> processorMap;
    private final LabMetricProcessor firstSheetOnlyProcessor;

    int numberOfSheets;

    public PoiSpreadsheetParser(Map<String, ? extends TableProcessor> processorMap) {
        this.processorMap = processorMap;
        firstSheetOnlyProcessor = null;
    }

    public PoiSpreadsheetParser(LabMetricProcessor processor) {
        processorMap = null;
        firstSheetOnlyProcessor = processor;
    }

    /**
     * processWorkbook contains the generic guts of parsing a spreadsheet.  This method is responsible for pulling
     * the data, row by row, from the spreadsheet file and holding that data in such a way that a concrete parser
     * can parse the row data in a way specific to that parser.
     *
     * @throws ValidationException
     */
    protected void processWorkSheet(String sheetName, TableProcessor processor) throws ValidationException {

        Sheet workSheet;
        if (sheetName == null) {
            workSheet = workbook.getSheetAt(0);
        } else {
            workSheet = workbook.getSheet(sheetName);
            if (workSheet == null) {
                throw new ValidationException("No worksheet named " + sheetName + " found");
            }
        }

        Iterator<Row> rows = workSheet.rowIterator();
        processHeaders(processor, rows);
        processData(processor, rows);
    }

    /**
     * Process the data portion of the spreadsheet.
     *
     * @param processor The processor being used.
     * @param rows The iterator on the excel rows.
     */
    private void processData(TableProcessor processor, Iterator<Row> rows) {
        // The data row index starts at 1 in the excel spreadsheet and then we must skip the header rows.
        int dataRowIndex = processor.getNumHeaderRows() + 1;

        while (rows.hasNext()) {
            Row row = rows.next();

            // The column index for grabbing cell data is 0 based.
            int columnIndex = 0;

            // Create a mapping of the headers to the cell values. There are never date values for the header.
            Map<String, String> dataByHeader = new HashMap<>();
            for (String headerName : processor.getHeaderNames()) {
                dataByHeader.put(headerName,
                        extractCellContent(row, headerName, columnIndex, processor.isDateColumn(columnIndex)));
                columnIndex++;
            }

            // Take the map and turn it into objects and process the data appropriately.
            processor.processRow(dataByHeader, dataRowIndex);

            // Go on to the next row.
            dataRowIndex++;
        }
    }

    /**
     * Process the headers.
     *
     * @param processor The Table Processor that will be turning rows of data into objects based on headers.
     * @param rows The row iterator.
     */
    private void processHeaders(TableProcessor processor, Iterator<Row> rows) {
        int headerRowIndex = 0;
        int numHeaderRows = processor.getNumHeaderRows();
        while (rows.hasNext() && headerRowIndex < numHeaderRows) {
            Row headerRow = rows.next();

            List<String> headers = new ArrayList<>();
            Iterator<Cell> cellIterator = headerRow.cellIterator();
            while (cellIterator.hasNext()) {
                headers.add(getCellValues(cellIterator.next(), false));
            }

            // The primary header row is the one that needs to be generally validated.
            if (processor.getPrimaryHeaderRow() == headerRowIndex) {
                processor.validateHeaders(headers);
            }

            // Turn the header strings for this row into whatever objects are needed to continue on.
            processor.processHeader(headers, headerRowIndex);

            // Go on to the next row.
            headerRowIndex++;
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

        workbook = WorkbookFactory.create(fileStream);
        numberOfSheets = workbook.getNumberOfSheets();

        if (processorMap == null) {
            processWorkSheet(null, firstSheetOnlyProcessor);
        } else {
            // Go through each processor and process the sheet specified.
            for (String sheetName : processorMap.keySet()) {
                processWorkSheet(sheetName, processorMap.get(sheetName));
            }
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
    protected String extractCellContent(Row row, String headerName, int columnIndex, boolean isDate) {

        Cell cell = row.getCell(columnIndex);

        String result = getCellValues(cell, isDate);
        if (StringUtils.isBlank(result)) {
            validationMessages.add(
                    "Row # " + row.getRowNum() + ": Unable to determine cell type for " + headerName);
        }

        return result;
    }

    /**
     * We leave all parsing and validating up to the caller by turning everything into a string. We might want to
     * let POI turn things into real objects in the map in the future, but for now this was what callers were
     * expecting.
     *
     * @param cell The cell data.
     *
     * @return A string representation of the cell.
     */
    private String getCellValues(Cell cell, boolean isDate) {
        String result = "";

        if (cell != null) {
            switch (cell.getCellType()) {
            case Cell.CELL_TYPE_BOOLEAN:
                result = String.valueOf(cell.getBooleanCellValue());
                break;
            case Cell.CELL_TYPE_NUMERIC:
                if (isDate) {
                    result = dateFormatter.format(cell.getDateCellValue());
                } else {
                    result = String.valueOf(cell.getNumericCellValue());
                }
                break;
            case Cell.CELL_TYPE_STRING:
                result = cell.getStringCellValue();
                break;
            default:
                break;
            }
        }

        return result;
    }

    /**
     * Get the names of all the worksheets in the tracker file.
     *
     * @param trackerFile The file to open and grab the worksheet names.
     *
     * @return The names.
     */
    public static List<String> getWorksheetNames(File trackerFile) throws IOException, InvalidFormatException {

        InputStream inputStream = null;

        try {
            inputStream = new FileInputStream(trackerFile);
            return getWorksheetNames(inputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
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
     * This is a convenience function for processing a single worksheet from a spreadsheet file.
     *
     * @param spreadsheet The spreadsheet stream of data.
     * @param worksheetName The name of the worksheet to process.
     * @param processor The table processor.
     *
     * @throws InvalidFormatException Formatting issues
     * @throws IOException File issues
     * @throws ValidationException Any problems with the data in the spreadsheet
     */
    public static void processSingleWorksheet(
            InputStream spreadsheet, String worksheetName, TableProcessor processor)
            throws InvalidFormatException, IOException, ValidationException {

        PoiSpreadsheetParser parser = null;

        try {
            Map<String, ? extends TableProcessor> processorsByName =
                    Collections.singletonMap(worksheetName, processor);
            parser = new PoiSpreadsheetParser(processorsByName);
            parser.processUploadFile(spreadsheet);
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }
}
