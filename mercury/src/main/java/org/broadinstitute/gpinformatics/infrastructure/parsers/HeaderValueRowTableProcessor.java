package org.broadinstitute.gpinformatics.infrastructure.parsers;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Adds header-value row processing to the TableProcessor, for spreadsheets with
 * header-value rows above the usual horizontal header row such as the various types
 * of ExternalLibrary.xlsx
 */
public abstract class HeaderValueRowTableProcessor extends TableProcessor {
    protected Map<String, String> headerValueMap = new HashMap<>();
    protected Map<String, Integer> headerRowIndexMap = new HashMap<>();

    public HeaderValueRowTableProcessor(String sheetName) {
        super(sheetName);
    }

    public HeaderValueRowTableProcessor(String sheetName, @Nonnull IgnoreTrailingBlankLines ignoreTrailingBlankLines) {
        super(sheetName, ignoreTrailingBlankLines);
        // By default searches spreadsheet for the actual horizonal header row.
        setHeaderRowIndex(-1);
    }

    /** Processes a spreadsheet row as a single row that consists of a header cell followed by value cell(s). */
    public void processHeaderValueRow(Row row) {
        for (Iterator<Cell> iterator = row.cellIterator(); iterator.hasNext(); ) {
            String content = iterator.next().getStringCellValue().trim();
            // The header is the first non-blank cell on the row, and the very next cell is the value.
            if (StringUtils.isNotBlank(content)) {
                for (String headerName : getHeaderValueNames()) {
                    if (headerName.equals(content)) {
                        String value =  iterator.hasNext() ? iterator.next().getStringCellValue().trim() : "";
                        headerValueMap.put(content, value);
                        headerRowIndexMap.put(content, row.getRowNum());
                        break;
                    }
                }
                break;
            }
        }
    }

    /**
     * Generates error messages if the header-value rows are missing any required headers or required values.
     * This should be called after all the header-value rows have been parsed.
     */
    public void validateHeaderValueRows() {
        for (HeaderValueRow headerValueRow : getHeaderValueRows()) {
            String headerName = headerValueRow.getText();
            if (!headerValueMap.containsKey(headerName)) {
                if (headerValueRow.isRequiredHeader()) {
                    getMessages().add("Required row for \"" + headerName + "\" is missing (must appear above row " +
                            (getHeaderRowIndex() + 1) + ").");
                }
            } else {
                if (headerValueRow.isRequiredValue() && StringUtils.isBlank(headerValueMap.get(headerName))) {
                    getMessages().add("Required value for " + headerName + " is blank at row " +
                            (headerRowIndexMap.get(headerName) + 1));
                }
            }
        }
    }

    public abstract HeaderValueRow[] getHeaderValueRows();

    public abstract List<String> getHeaderValueNames();

    /** Returns the mapping from header text to data value. */
    public Map<String, String> getHeaderValueMap() {
        return headerValueMap;
    };

    /** Returns the mapping from header text to the 1-based row number that the header first appeared in. */
    public Map<String, Integer> getHeaderRowIndexMap() {
        return headerRowIndexMap;
    }
}
