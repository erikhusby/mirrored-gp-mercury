package org.broadinstitute.gpinformatics.infrastructure.parsers;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.jvnet.inflector.Noun;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.javafx.tools.doclets.formats.html.markup.HtmlStyle.header;
import static com.sun.org.apache.xerces.internal.impl.xpath.regex.CaseInsensitiveMap.get;
import static org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser.getCellValues;
import static org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessor.stripTrimLowerCase;

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
    public static final String REQUIRED_VALUE_IS_MISSING = "Required value for %s is missing.";

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

    public void processSubHeaders(List<String> headers) {
    }

    public int getHeaderRowIndex() {
        return this.headerRowIndex;
     }

    public void setHeaderRowIndex(int headerRowIndex) {
        this.headerRowIndex = headerRowIndex;
    }

    /**
     * Header method used by all parsers to get the names that will be used to map values to the appropriate columns.
     *
     * @return The list of header names.
     */
    public abstract List<String> getHeaderNames();

    /**
     * Take all headers identified for the specified row and pull whatever important data is needed from that to help
     * process the date that comes from the table cells. Note that the headers will not necessarily be all covered by
     * header enums. We often have columns that are data specific (Like price items on products in the tracker).
     *
     * @param headers The header strings in this row
     * @param row     The 0-based row index of the row being processed.
     */
    public abstract void processHeader(List<String> headers, int row);

    /**
     * If all required values are present, processes the row.
     * @param dataRow Map of column name to value.
     * @param rowIndex The 0-based row index
     */
    public final void processRow(Map<String, String> dataRow, int rowIndex) {
        if (hasRequiredValues(dataRow, rowIndex)) {
            processRowDetails(dataRow, rowIndex);
        }
    }

    /**
     * This method defines the specific logic necessary to parse the data in the given table.  The concrete parser will
     * be responsible for constructing any  necessary entities to represent the data found in this table row.
     *
     * @param dataRow      The row of data mapped by header name.
     * @param rowIndex     The 0-based row index.
     */
    public abstract void processRowDetails(Map<String, String> dataRow, int rowIndex);

    /** Returns true if the given cells constitute a valid header row, meaning all required headers are present. */
    public final boolean validateColumnHeaders(List<String> cellContents) {
        Set<String> uniqueHeaders = new HashSet<>();
        Set<String> unknownCellContents = new HashSet<>(cellContents);
        for (ColumnHeader header : getColumnHeaders()) {
            // Applies adjustHeaderCell() to make the match more robust.
            String adjustedHeader = adjustHeaderCell(header.getText());
            boolean found = false;
            for (String cellContent : cellContents) {
                if (adjustHeaderCell(cellContent).equals(adjustedHeader)) {
                    found = true;
                    unknownCellContents.remove(cellContent);
                    if (!uniqueHeaders.add(adjustedHeader)) {
                        validationMessages.add("Duplicate header: \"" + cellContent + "\"" +
                                (adjustedHeader.equals(cellContent) ? "" : " (matches \"" + adjustedHeader + "\")"));
                    }
                }
            }
            if (header.isRequiredHeader() && !found) {
                validationMessages.add("Required header \"" + adjustedHeader + "\" is missing.");
            }
        }
        if (!unknownCellContents.isEmpty()) {
            validationMessages.add("Unknown header: \"" + StringUtils.join(unknownCellContents, "\", \"") + "\"");
        }
        validateHeaderRow(cellContents);
        return validationMessages.isEmpty();
    }

    /**
     * If Processor specific header validation is required, override this method and perform it there.
     */
    @SuppressWarnings("unused")
    public void validateHeaderRow(List<String> headers) { }

    /**
     * Returns true if all of the required values are non-blank.
     * Adds an error message for each missing value.
     *
     * @param dataRow  Map of header name to value.
     * @param rowIndex The 0-based index of the row
     */
    private boolean hasRequiredValues(Map<String, String> dataRow, int rowIndex) {
        boolean hasAll = true;
        for (ColumnHeader header : getColumnHeaders()) {
            if (header.isRequiredValue()) {
                String adjustedHeaderName = adjustHeaderCell(header.getText());
                boolean found = false;
                for (String dataHeaderName : dataRow.keySet()) {
                    if (adjustedHeaderName.equals(adjustHeaderCell(dataHeaderName))) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    addDataMessage(String.format(REQUIRED_VALUE_IS_MISSING, header.getText()), rowIndex + 1);
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
     * that contains all of the required ColumnHeader strings.
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
            if (validateColumnHeaders(theCells)) {
                return row.getRowNum();
            }
            if (StringUtils.join(validationMessages).length() < leastError) {
                leastError = StringUtils.join(validationMessages).length();
                bestGuess = row.getRowNum();
            }
            validationMessages.clear();
        }
        return bestGuess;
    }

    /** Returns the row index and number of missing header columns (ideally 0). */
    public static Map.Entry<Integer, Integer> evaluateHeaderMatch(Sheet worksheet, ColumnHeader[] headers) {
        SortedMap<Integer, Integer> rowIndexToErrorCount = new TreeMap<>();
        for (Iterator<Row> rowIter = worksheet.rowIterator(); rowIter.hasNext(); ) {
            Row row = rowIter.next();
            List<String> theCells = new ArrayList<>();
            for (Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
                String cell = stripTrimLowerCase(getCellValues(cellIter.next(), false, true));
                if (StringUtils.isNotBlank(cell)) {
                    theCells.add(cell);
                }
            }
            int errorCount = 0;
            for (ColumnHeader header : headers) {
                String adjustedHeader = stripTrimLowerCase(header.getText());
                if (header.isRequiredHeader() && !theCells.contains(adjustedHeader)) {
                    ++errorCount;
                }
            }
            rowIndexToErrorCount.put(row.getRowNum(), errorCount);
            // Breaks off the search if an exact match is found.
            if (errorCount == 0) {
                break;
            }
        }
        // Returns the row index having the fewest errors.
        List<Integer> errorCounts = new ArrayList<>(rowIndexToErrorCount.values());
        Collections.sort(errorCounts);
        for (Map.Entry<Integer, Integer> mapEntry : rowIndexToErrorCount.entrySet()) {
            if (mapEntry.getValue().equals(errorCounts.get(0))) {
                return mapEntry;
            }
        }
        return null;
    }

    /** Allows a processor to modify the header cell (trim, lower case, etc.) before matching it with Header text. */
    public String adjustHeaderCell(String headerCell) {
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
    private String getPrefixedMessage(String message, int rowIndex) {
        String prefix = (sheetName == null) ? "" : "Sheet " + sheetName + ", ";
        return prefix + "Row #" + (rowIndex + 1) + " " + message;
    }

    protected void addGeneralMessage(String message) {
        validationMessages.add(message);
    }

    /**
     * Adds a formatted message string.
     * @param rowIndex the 0-based row index.
     */
    protected void addDataMessage(String message, int rowIndex) {
        validationMessages.add(getPrefixedMessage(message, rowIndex));
    }

    /**
     * Adds a formatted message string.
     * @param rowIndex the 0-based row index.
     */
    protected void addWarning(String message, int rowIndex) {
        warnings.add(getPrefixedMessage(message, rowIndex));
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
     * Applies adjustHeaderCell() to make the match more robust.
     */
    public ColumnHeader findColumnHeaderByName(String headerName) {
        String adjustedHeaderName = adjustHeaderCell(headerName);
        for (ColumnHeader columnHeader : getColumnHeaders()) {
            if (adjustedHeaderName.equals(adjustHeaderCell(columnHeader.getText()))) {
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
}
