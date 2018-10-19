package org.broadinstitute.gpinformatics.infrastructure.parsers;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser.getCellValues;

/**
 * This abstract class provides spreadsheet/table parsers with the logic for turning rows of data into objects and
 * processing these into actions that can be communicated, whether it is a preview of what was built or actually
 * saving information into the database. This processes headers and data specific to the import desired.
 */
public abstract class TableProcessor implements Serializable {

    /**
     * If a TableProcessor is constructed with IgnoreTrailingBlankLines.YES, it will silently ignore trailing rows of
     * all-blank cells.
     */
    public enum IgnoreTrailingBlankLines {
        YES,
        NO
    }

    private static final long serialVersionUID = 8122298462727182883L;
    public static final String REQUIRED_VALUE_IS_MISSING = "Required value for \"%s\" is missing.";
    public static final String REQUIRED_HEADER_IS_MISSING = "Required header \"%s\" is missing.";
    public static final String UNKNOWN_HEADER = "Unknown header(s) \"%s\".";
    public static final String DUPLICATE_HEADER = "Duplicate header: \"%s\".";

    protected final List<String> validationMessages = new ArrayList<>();
    private int headerRowIndex;
    private final Set<String> warnings = new LinkedHashSet<>();
    protected Map<String, Integer> headerToColumnIndex = new HashMap<>();

    private final String sheetName;

    private final IgnoreTrailingBlankLines ignoreTrailingBlankLines;

    /**
     * Constructor that creates a TableProcessor that will complain if there are blank rows
     * at the bottom the spreadsheet.
     */
    protected TableProcessor(String sheetName) {
        this(sheetName, IgnoreTrailingBlankLines.NO);
    }

    /**
     * Constructor.
     * @param ignoreTrailingBlankLines if true, will silently ignore trailing rows of all-blank cells.
     */
    protected TableProcessor(String sheetName, @Nonnull IgnoreTrailingBlankLines ignoreTrailingBlankLines) {
        this.sheetName = sheetName;
        this.ignoreTrailingBlankLines = ignoreTrailingBlankLines;
    }

    public int getHeaderRowIndex() {
        return this.headerRowIndex;
     }

    public void setHeaderRowIndex(int headerRowIndex) {
        this.headerRowIndex = headerRowIndex;
    }

    /**
     * The actual header names. These are expected to align 1:1 with the data columns found after the header row.
     *
     * @return the names that will be used to map values to the appropriate columns.
     */
    public abstract List<String> getHeaderNames();

    /**
     * The actual header names found in this spreadsheet are passed to the subclass here.
     * By default, headers are expected to align 1:1 with the data columns in the subsequent rows.
     * The actual headers found will not necessarily be among the Header enums. Sometimes headers
     * are data specific, such as price items on products in the billing tracker.
     *
     * @param headers The header strings in this row
     * @param row     The 0-based row index of the row being processed.
     */
    public abstract void processHeader(List<String> headers, int row);

    /**
     * Processes the data row. Adds messages for missing values that are required.
     * @param dataRow Map of actual header name to value.
     * @param rowIndex The 0-based row index
     */
    public final void processRow(Map<String, String> dataRow, int rowIndex) {
        boolean requiredValuesPresent = hasRequiredValues(dataRow, rowIndex);
        processRowDetails(dataRow, rowIndex, requiredValuesPresent);
    }

    /**
     * Processes one row of data. The concrete implementation is responsible for constructing any
     * necessary entities for the data.
     *  @param dataRow      The row of data mapped by header name.
     * @param rowIndex     The 0-based row index.
     * @param requiredValuesPresent
     */
    public abstract void processRowDetails(Map<String, String> dataRow, int rowIndex, boolean requiredValuesPresent);

    /**
     * Returns true if the given cells constitute a valid header row, meaning all required headers are present.
     * @param cellContents The header names found on this row.
     * @param rowIndex     The 0-based row index.
     */
    public final boolean validateColumnHeaders(List<String> cellContents, int rowIndex) {
        Set<String> uniqueHeaders = new HashSet<>();
        Set<String> unknownCellContents = new HashSet<>(cellContents);
        for (ColumnHeader header : getColumnHeaders()) {
            // Performs matching by using an adjusted header text that allows the spreadsheet some latitude.
            String adjustedHeader = adjustHeaderName(header.getText());
            boolean found = false;
            for (String cellContent : cellContents) {
                if (adjustHeaderName(cellContent).equals(adjustedHeader)) {
                    found = true;
                    unknownCellContents.remove(cellContent);
                    if (!uniqueHeaders.add(adjustedHeader)) {
                        validationMessages.add(String.format(DUPLICATE_HEADER, cellContent));
                    }
                }
            }
            if (header.isRequiredHeader() && !found) {
                validationMessages.add(String.format(REQUIRED_HEADER_IS_MISSING, adjustedHeader));
            }
        }
        if (!unknownCellContents.isEmpty()) {
            addWarning(rowIndex + 1, UNKNOWN_HEADER, StringUtils.join(unknownCellContents, "\", \""));
        }
        validateHeaderRow(cellContents);
        return validationMessages.isEmpty();
    }

    /** Subclass should override this if additional header validation is required. */
    @SuppressWarnings("unused")
    public void validateHeaderRow(List<String> headers) { }

    /**
     * Checks if any required value is blank and adds an error message for each missing value.
     *
     * @param dataRow  Map of header name to value.
     * @param rowIndex The 0-based index of the row
     */
    private boolean hasRequiredValues(Map<String, String> dataRow, int rowIndex) {
        boolean hasAll = true;
        for (ColumnHeader header : getColumnHeaders()) {
            if (header.isRequiredValue()) {
                String adjustedHeaderName = adjustHeaderName(header.getText());
                boolean found = false;
                for (Map.Entry<String, String> headerAndValue : dataRow.entrySet()) {
                    if (adjustedHeaderName.equals(adjustHeaderName(headerAndValue.getKey()))) {
                        found = StringUtils.isNotBlank(headerAndValue.getValue());
                        break;
                    }
                }
                if (!found) {
                    validationMessages.add(TableProcessor.getPrefixedMessage(String.format(REQUIRED_VALUE_IS_MISSING,
                            header.getText()), sheetName, rowIndex));
                    hasAll = false;
                }
            }
        }
        return hasAll;
    }

    protected abstract ColumnHeader[] getColumnHeaders();

    public abstract void close();

    /**
     * Returns the 0-based index of the horizontal header row, which is the first row
     * that contains all of the required ColumnHeader strings. If no such row is found
     * then uses the row having the closest match, and expects that the later validation
     * will show the missing header messages.
     */
    public int findHeaderRow(Sheet worksheet) {
        int bestGuess = 0;
        int leastError = Integer.MAX_VALUE;
        for (Iterator<Row> rowIter = worksheet.rowIterator(); rowIter.hasNext(); ) {
            Row row = rowIter.next();
            List<String> theCells = new ArrayList<>();
            for (Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
                // Headers are always strings.
                String cell = getCellValues(cellIter.next(), false, true);
                if (StringUtils.isNotBlank(cell)) {
                    theCells.add(cell);
                }
            }
            if (validateColumnHeaders(theCells, row.getRowNum())) {
                return row.getRowNum();
            }
            if (StringUtils.join(validationMessages).length() < leastError) {
                leastError = StringUtils.join(validationMessages).length();
                bestGuess = row.getRowNum();
            }
            warnings.clear();
            validationMessages.clear();
        }
        return bestGuess;
    }

    /** Allows a processor to modify the header cell (trim, lower case, etc.) before matching it with Header text. */
    public String adjustHeaderName(String headerCell, int numberOfWords) {
        return headerCell;
    }

    /** Allows a processor to modify the header cell (trim, lower case, etc.) before matching it with Header text. */
    public String adjustHeaderName(String headerCell) {
        return headerCell;
    }

    public List<String> getMessages() {
        return validationMessages;
    }

    public Collection<String> getWarnings() {
        return warnings;
    }

    /**
     * Returns a formatted message string.
     * @param rowIndex the 0-based row index. A 1-based number is put into the message in order to
     *                 display to the user the same number shown by Excel on the far left of each row.
     */
    public static String getPrefixedMessage(String message, String sheetName, int rowIndex) {
        return (StringUtils.isBlank(sheetName) ? "" :
                (!sheetName.toLowerCase().startsWith("sheet") ? "Sheet " : "") + sheetName + ", ") +
                "Row #" + (rowIndex + 1) + " " + message;
    }

    protected void addGeneralMessage(String message) {
        validationMessages.add(message);
    }

    /**
     * Adds a formatted message string.
     * @param rowIndex the 0-based row index.
     */
    protected void addDataMessage(String message, int rowIndex) {
        validationMessages.add(getPrefixedMessage(message, sheetName, rowIndex));
    }

    protected void addDataMessage(int rowIndex, String format, String... formatParams) {
        validationMessages.add(getPrefixedMessage(String.format(format, formatParams), sheetName, rowIndex));
    }

    /**
     * Adds a formatted message string.
     * @param rowIndex the 0-based row index.
     */
    protected void addWarning(String message, int rowIndex) {
        warnings.add(getPrefixedMessage(message, sheetName, rowIndex));
    }

    protected void addWarning(int rowIndex, String format, String... formatParams) {
        warnings.add(getPrefixedMessage(String.format(format, formatParams), sheetName, rowIndex));
    }

    public boolean isDateColumn(String headerName) {
        ColumnHeader columnHeader = findColumnHeaderByName(headerName);
        return columnHeader != null && columnHeader.isDateColumn();
    }

    /**
     * This is used for columns that might show up as numeric but REALLY MUST be treated as a string. This is to
     * get around odd formatting problems of scientific notation that Excel may cause.
     *
     * @return Whether this MUST be a string.
     */
    public boolean isStringColumn(String headerName) {
        ColumnHeader columnHeader = findColumnHeaderByName(headerName);
        return columnHeader != null && columnHeader.isStringColumn();
    }

    /**
     * Returns the header enum for the header name, or null if no match found.
     * Performs matching using an adjusted header name to allow the spreadsheet some latitude.
     */
    public @Nullable ColumnHeader findColumnHeaderByName(String headerName) {
        String adjustedHeaderName = adjustHeaderName(headerName);
        for (ColumnHeader columnHeader : getColumnHeaders()) {
            if (adjustedHeaderName.equals(adjustHeaderName(columnHeader.getText()))) {
                return columnHeader;
            }
        }
        return null;
    }

    /**
     * If your requirements state that a workbook must have a certain amount of worksheets you can override
     * to include that logic.
     *
     * @param actualNumberOfSheets number of sheets in workbook
     *
     * @throws ValidationException if the actual number of worksheets differs from your requirements.
     */
    public void validateNumberOfWorksheets(int actualNumberOfSheets) throws ValidationException {    }

    public boolean shouldIgnoreTrailingBlankLines() {
        return ignoreTrailingBlankLines == IgnoreTrailingBlankLines.YES;
    }

    public Map<String, Integer> getHeaderToColumnIndex() {
        return headerToColumnIndex;
    }


    public boolean quitOnMatch(Collection<String> dataByHeader) {
        return false;
    }
}
