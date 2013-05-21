package org.broadinstitute.gpinformatics.infrastructure.parsers.poi;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Iterator;

public abstract class AbstractSpreadsheetParser implements Serializable {


    private Workbook workbook;
    private final ColumnHeader[] matchHeaders;

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

            processRow(row);

        }

    }

    protected abstract void validateAndProcess() throws ValidationException;

    public void processUploadFile(InputStream fileStream) throws IOException, InvalidFormatException, ValidationException {

        workbook = WorkbookFactory.create(fileStream);

        numberOfSheets = workbook.getNumberOfSheets();
        validateAndProcess();

    }

    public Sheet getSheetAt(int sheetIndex) {
        return workbook.getSheetAt(sheetIndex);
    }

    protected abstract void processRow(Row rowValues);

}
