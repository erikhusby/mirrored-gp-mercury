package org.broadinstitute.gpinformatics.infrastructure.parsers;

import com.google.common.collect.Streams;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds header-value row processing to the TableProcessor, for spreadsheets with
 * header-value rows above the usual horizontal header row such as the various types
 * of ExternalLibrary.xlsx
 */
public abstract class HeaderValueRowTableProcessor extends TableProcessor {
    // Maps the enum-defined header names to the corresponding data value.
    protected Map<String, String> headerValueMap = new HashMap<>();
    // Maps the enum-defined header names to the row index.
    protected Map<String, Integer> headerRowIndexMap = new HashMap<>();
    private static final int NUMBER_OF_WORDS_TO_COMPARE = 2;

    public HeaderValueRowTableProcessor(String sheetName) {
        super(sheetName, IgnoreTrailingBlankLines.YES);
        // By default searches spreadsheet for the actual horizonal header row.
        setHeaderRowIndex(-1);
    }

    /** Processes a spreadsheet row that consists of a header cell followed by a value cell. */
    public void processHeaderValueRow(Row row) {
        // Finds the first non-blank cell on the row, which is expected to be the header.
        Streams.stream(row.cellIterator()).
                filter(cell -> StringUtils.isNotBlank(cell.getStringCellValue())).
                findFirst().ifPresent(headerCell -> {
            // Allows the spreadsheet some latitude with the header names.
            String actualHeaderName = headerCell.getStringCellValue().trim();
            String adjustedHeader = adjustHeaderName(actualHeaderName, NUMBER_OF_WORDS_TO_COMPARE);
            if (StringUtils.isNotBlank(adjustedHeader)) {
                // Looks for a enum-defined header name that matches the adjusted actual header text.
                getHeaderValueNames().stream().
                        filter(name -> adjustedHeader.equals(adjustHeaderName(name, NUMBER_OF_WORDS_TO_COMPARE))).
                        findFirst().ifPresent(enumHeaderName -> {
                    // The next cell on the row is expected to be the value.
                    String value = (headerCell.getColumnIndex() < row.getLastCellNum()) ?
                            row.getCell(headerCell.getColumnIndex() + 1).getStringCellValue().trim() : "";
                    headerValueMap.put(enumHeaderName, value);
                    headerRowIndexMap.put(enumHeaderName, row.getRowNum());
                });
            }
        });
    }

    /**
     * Generates error messages if the header-value rows are missing any required headers or required values.
     * This should be called after all the header-value rows have been parsed.
     */
    public void validateHeaderValueRows() {
        for (HeaderValueRow headerValueRow : getHeaderValueRows()) {
            String enumHeaderName = headerValueRow.getText();
            if (!headerValueMap.containsKey(enumHeaderName)) {
                if (headerValueRow.isRequiredHeader()) {
                    getMessages().add("Required row for \"" + enumHeaderName + "\" is missing (must appear above row " +
                            (getHeaderRowIndex() + 1) + ").");
                }
            } else {
                if (headerValueRow.isRequiredValue() && StringUtils.isBlank(headerValueMap.get(enumHeaderName))) {
                    getMessages().add("Required value for " + enumHeaderName + " is blank at row " +
                            (headerRowIndexMap.get(enumHeaderName) + 1));
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
}
