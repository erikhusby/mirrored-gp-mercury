package org.broadinstitute.gpinformatics.infrastructure.parsers;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.jvnet.inflector.Noun;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private final List<String> validationMessages = new ArrayList<>();

    private final Set<String> warnings = new LinkedHashSet<>();

    private final String sheetName;

    private final IgnoreTrailingBlankLines ignoreTrailingBlankLines;

    /**
     * Legacy constructor that creates a TableProcessor with TolerateBlankLines.NO, so blank lines in the
     * spreadsheet will generate errors.
     */
    protected TableProcessor(String sheetName) {
        this(sheetName, IgnoreTrailingBlankLines.NO);
    }

    /**
     * Constructor that allows for specification of whether trailing blank lines are ignored.
     */
    protected TableProcessor(String sheetName, @Nonnull IgnoreTrailingBlankLines ignoreTrailingBlankLines) {
        this.sheetName = sheetName;
        this.ignoreTrailingBlankLines = ignoreTrailingBlankLines;
    }

    /**
     * The number of rows that will be on the parser's spreadsheet. Tells the parser how many lines to send to process
     * header. The default is to have one row of headers.
     *
     * @return The number of header rows
     */
    public int getNumHeaderRows() {
        return 1;
    }

    public int getHeaderRowIndex() {
        return 0;
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
     * @param row     The row
     */
    public abstract void processHeader(List<String> headers, int row);

    /**
     * The primary header row is the one that is validated in hasRequiredHeaders.
     *
     * @return By default it is 0, but any header row could be the primary.
     */
    public int getPrimaryHeaderRow() {
        return 0;
    }

    public final void processRow(Map<String, String> dataRow, int dataRowIndex) {
        // Validate the required fields.
        if (hasRequiredValues(dataRow, dataRowIndex)) {
            processRowDetails(dataRow, dataRowIndex);
        }
    }

    /**
     * This method defines the specific logic necessary to parse the data in the given table.  The concrete parser will
     * be responsible for constructing any  necessary entities to represent the data found in this table row.
     *
     * @param dataRow      The row of data mapped by header name.
     * @param dataRowIndex The current row of data we are working with.
     */
    public abstract void processRowDetails(Map<String, String> dataRow, int dataRowIndex);

    public final boolean validateColumnHeaders(List<String> headers) {

        // If any of the required headers are NOT in the header list, then return false.
        List<String> missingHeaders = new ArrayList<>();
        for (ColumnHeader header : getColumnHeaders()) {
            if (header.isRequiredHeader() && !headers.contains(header.getText())) {
                missingHeaders.add(header.getText());
            }
        }
        if (!missingHeaders.isEmpty()) {
            validationMessages.add(
                    String.format("Required %s missing: %s.", Noun.pluralOf("header", missingHeaders.size()),
                            StringUtils.join(missingHeaders, ", ")));
        }
        validateHeaderRow(headers);
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

    public boolean isDateColumn(int columnIndex) {
        String headerNameAtIndex = getHeaderNames().get(columnIndex);
        ColumnHeader columnHeader = findColumnHeaderByName(headerNameAtIndex);
        return columnHeader != null && columnHeader.isDateColumn();
    }

    /**
     * This is used for columns that might show up as numeric but REALLY MUST be treated as a string. This is to
     * get around odd formatting problems of scientific notation that Excel may cause.
     *
     * @param columnIndex The index of the column being checked.
     *
     * @return Whether this MUST be a string.
     */
    public boolean isStringColumn(int columnIndex) {
        String headerNameAtIndex = getHeaderNames().get(columnIndex);
        ColumnHeader columnHeader = findColumnHeaderByName(headerNameAtIndex);
        return columnHeader != null && columnHeader.isStringColumn();
    }

    protected ColumnHeader findColumnHeaderByName(String headerName) {
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

    public boolean quitOnMatch(Collection<String> dataByHeader) {
        return false;
    }
}
