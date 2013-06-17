package org.broadinstitute.gpinformatics.infrastructure.parsers.poi;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

/**
 *
 * Base class for all (hopefully) spreadsheet parsing duties.  Children of this class only need to define how to
 * map the data to ... whatever it maps to for that particular need.
 *
 *
 */
public abstract class AbstractSpreadsheetParser implements Serializable {

    protected final List<String> validationMessages = new ArrayList<String>();
    private Workbook workbook;
    private final ColumnHeader[] matchHeaders;

    protected Map<Integer, String[]> dataByRow = new HashMap<Integer, String[]>();

    int numberOfSheets;

    protected AbstractSpreadsheetParser(ColumnHeader[] matchHeaders) {
        this.matchHeaders = matchHeaders;
    }

    /**
     * processWorkbook contains the generic guts of parsing a spreadsheet.  This method is responsible for pulling
     * the data, row by row, from the spreadsheet file and holding that data in such a way that a concrete parser
     * can parse the row data in a way specific to that parser.
     * @throws ValidationException
     */
    protected void processWorkbook() throws ValidationException {
        Sheet workingSheet =  workbook.getSheetAt(0);

        Iterator<Row> rows = workingSheet.rowIterator();
        while (rows.hasNext()) {
            Row row = rows.next();

            // TODO SGM:  Allow concrete parsers to determine the amount of rows that make up the header and how many
            // rows to skip to get to the first row of data.
            if (row.getRowNum() == 0) {
                row = rows.next();
            }
            dataByRow.put(row.getRowNum(), new String[matchHeaders.length + 1]);
            for (ColumnHeader header : matchHeaders) {
                dataByRow.get(row.getRowNum())[header.getIndex()] =
                        extractCellContent(row, header);
            }

        }
        parseRows();
    }

    /**
     * Initial point of contact for a parser (except if overwritten by a sub parser).  This method is responsible for
     * coordinating the reading and parsing of a given spreadsheet file.
     * @param fileStream input stream that represents the Spreadsheet that has been uploaded by the user.
     * @throws IOException
     * @throws InvalidFormatException
     * @throws ValidationException
     */
    public void processUploadFile(InputStream fileStream)
            throws IOException, InvalidFormatException, ValidationException {

        workbook = WorkbookFactory.create(fileStream);

        numberOfSheets = workbook.getNumberOfSheets();
        processWorkbook();

    }

    /**
     * Implemented by concrete classes, this method must define the specific logic necessary to parse the data in the
     * given spreadsheet based on that parsers needs.  The concrete parser will be responsible for constructing any
     * necessary entities to represent the data found in the Spreadsheet.
     */
    protected abstract void parseRows();

    /**
     * Helper method to pull data from individual cells taking into account the cell format.  The cell content is
     * converted to strings to allow for a more generic approach to representing the data.  This allows the abstract
     * parser to handle pulling out the data specific to the POI implementation, and allows the concrete parsers to
     * parse the data not caring whether or not it came from a spreadsheet
     * @param row Represents a row in the spreadsheet file to be parsed
     * @param header Represents the specific column of the given row to extract
     * @return A string representation of the data in the cell indicated by the given row/column (header) combination
     */
    protected String extractCellContent(Row row, ColumnHeader header) {

        Cell cell = row.getCell(header.getIndex());

        String result = "";
        if (cell != null) {
            switch (cell.getCellType()) {
            case Cell.CELL_TYPE_BOOLEAN:
                result = String.valueOf(cell.getBooleanCellValue());
                break;
            case Cell.CELL_TYPE_NUMERIC:
                result = String.valueOf(cell.getNumericCellValue());
                break;
            case Cell.CELL_TYPE_STRING:
                result = cell.getStringCellValue();
                break;
            default:
                validationMessages.add("Row # " + row.getRowNum() + ": Unable to determine cell type for " +
                                       header.getText());
                break;
            }
        }
        return result;
    }

}
