package org.broadinstitute.gpinformatics.infrastructure.parsers.poi;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

public abstract class AbstractSpreadsheetParser implements Serializable {

    protected List<String> validationMessages = new ArrayList<String>();
    private Workbook workbook;
    private final ColumnHeader[] matchHeaders;

    protected Map<Integer, String[]> dataByRow = new HashMap<Integer, String[]>();

    int numberOfSheets;

    protected AbstractSpreadsheetParser(ColumnHeader[] matchHeaders) {
        this.matchHeaders = matchHeaders;
    }

    public void processWorkbook() throws ValidationException {
        Sheet workingSheet = getSheetAt(0);

        Iterator<Row> rows = workingSheet.rowIterator();
        while (rows.hasNext()) {
            Row row = rows.next();

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

    protected void processUploadFile(InputStream fileStream)
            throws IOException, InvalidFormatException, ValidationException {

        workbook = WorkbookFactory.create(fileStream);

        numberOfSheets = workbook.getNumberOfSheets();
        processWorkbook();


    }

    public Sheet getSheetAt(int sheetIndex) {
        return workbook.getSheetAt(sheetIndex);
    }

    /**
     *
     */
    protected abstract void parseRows();

    public String extractCellContent(Row row, ColumnHeader header) {

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
