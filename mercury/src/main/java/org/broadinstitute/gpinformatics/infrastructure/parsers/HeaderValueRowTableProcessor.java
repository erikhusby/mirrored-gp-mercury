package org.broadinstitute.gpinformatics.infrastructure.parsers;

import org.apache.poi.ss.usermodel.Row;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Adds header-value row processing to the TableProcessor, for spreadsheets with
 * header-value rows above the usual horizontal header row such as the various types
 * of ExternalLibrary.xlsx
 */
public abstract class HeaderValueRowTableProcessor extends TableProcessor {
    public HeaderValueRowTableProcessor(String sheetName) {
        super(sheetName);
    }

    public HeaderValueRowTableProcessor(String sheetName, @Nonnull IgnoreTrailingBlankLines ignoreTrailingBlankLines) {
        super(sheetName, ignoreTrailingBlankLines);
        // By default searches spreadsheet for the actual horizonal header row.
        setHeaderRowIndex(-1);
    }

    /** Processes a spreadsheet row as a single row that consists of a header cell followed by value cell(s). */
    public abstract void processHeaderValueRow(Row row);

    protected abstract HeaderValueRow[] getHeaderValueRows();

    public abstract List<String> getHeaderValueNames();
}
