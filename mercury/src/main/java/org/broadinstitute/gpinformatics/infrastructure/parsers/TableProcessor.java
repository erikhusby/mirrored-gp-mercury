package org.broadinstitute.gpinformatics.infrastructure.parsers;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.jvnet.inflector.Noun;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
     * @param dataRowNumber The 1-based row number.
     */
    public final void processRow(Map<String, String> dataRow, int dataRowNumber) {
        // Validate the required fields.
        if (hasRequiredValues(dataRow, dataRowNumber)) {
            processRowDetails(dataRow, dataRowNumber);
        }
    }

    /**
     * This method defines the specific logic necessary to parse the data in the given table.  The concrete parser will
     * be responsible for constructing any  necessary entities to represent the data found in this table row.
     *
     * @param dataRow      The row of data mapped by header name.
     * @param dataRowNumber The 1-based row number.
     */
    public abstract void processRowDetails(Map<String, String> dataRow, int dataRowNumber);

    /**
     * Returns true if the given cell contents constitute a valid header row.
     *
     * @param cellContent strings that were modified by adjustHeaderCells().
     */
    public final boolean validateColumnHeaders(List<String> cellContent) {

        // If any of the required headers missing, then return false.
        List<String> missingHeaders = new ArrayList<>();
        for (ColumnHeader header : getColumnHeaders()) {
            String adjustedHeader = adjustHeaderCell(header.getText());
            if (header.isRequiredHeader() && !cellContent.contains(adjustedHeader)) {
                missingHeaders.add(adjustedHeader);
            }
        }
        if (!missingHeaders.isEmpty()) {
            validationMessages.add(
                    String.format("Required %s missing: %s.", Noun.pluralOf("header", missingHeaders.size()),
                            StringUtils.join(missingHeaders, ", ")));
        }
        validateHeaderRow(cellContent);
        return validationMessages.isEmpty();
    }

    /**
     * If Processor specific header validation is required, override this method and perform it there.
     */
    @SuppressWarnings("unused")
    public void validateHeaderRow(List<String> headers) { }

    /**
     * This method makes sure that all values in the row that are deemed 'required' have values. This means that
     * subclasses need not litter their code with these easy validations.
     *
     * @param dataRow      The row of data
     * @param dataRowIndex The index into the row
     *
     * @return Is the required value there?
     */
    private boolean hasRequiredValues(Map<String, String> dataRow, int dataRowIndex) {

        boolean hasValue = true;
        // If any of the required values are empty or missing in the data row, return false.
        for (ColumnHeader header : getColumnHeaders()) {
            if (header.isRequiredValue() &&
                (!dataRow.containsKey(header.getText()) || StringUtils.isBlank(dataRow.get(header.getText())))) {
                addDataMessage(String.format(REQUIRED_VALUE_IS_MISSING, header.getText()), dataRowIndex);
                hasValue = false;
            }
        }

        return hasValue;
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
                // Headers are always strings, and possibly made more uniform for the match.
                String cell = adjustHeaderCell(getCellValues(cellIter.next(), false, true));
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

    private String getPrefixedMessage(String message, int dataRowIndex) {
        String prefix = (sheetName == null) ? "" : "Sheet " + sheetName + ", ";
        return prefix + "Row #" + (dataRowIndex) + " " + message;
    }

    protected void addGeneralMessage(String message) {
        validationMessages.add(message);
    }

    protected void addDataMessage(String message, int dataRowIndex) {
        validationMessages.add(getPrefixedMessage(message, dataRowIndex));
    }

    protected void addWarning(String message, int dataRowIndex) {
        warnings.add(getPrefixedMessage(message, dataRowIndex));
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

    public ColumnHeader findColumnHeaderByName(String headerName) {
        for (ColumnHeader columnHeader : getColumnHeaders()) {
            if (headerName.equals(columnHeader.getText())) {
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
