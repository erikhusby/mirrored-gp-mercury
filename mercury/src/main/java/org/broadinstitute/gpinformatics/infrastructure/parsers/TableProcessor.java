package org.broadinstitute.gpinformatics.infrastructure.parsers;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * This abstract class provides spreadsheet/table parsers with the logic for turning rows of data into objects and
 * processing these into actions that can be communicated, whether it is a preview of what was built or actually
 * saving information into the database. This processes headers and data specific to the import desired.
 */
public abstract class TableProcessor implements Serializable {

    private static final long serialVersionUID = 8122298462727182883L;

    private List<String> validationMessages = new ArrayList<>();

    private final String sheetName;

    protected TableProcessor(String sheetName) {
        this.sheetName = sheetName;
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
     * @param row The row
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
     * @param dataRow The row of data mapped by header name.
     * @param dataRowIndex The current row of data we are working with.
     */
    public abstract void processRowDetails(Map<String, String> dataRow, int dataRowIndex);

    public final boolean validateHeaders(List<String> headers) {
        // If any of the required headers are NOT in the header list, then return false.
        for (ColumnHeader header : getColumnHeaders()) {
            if (header.isRequredHeader() && !headers.contains(header.getText())) {
                validationMessages.add("Required header: " + header.getText() + " is missing");
                return false;
            }
        }

        return true;
    }

    /**
     * This method makes sure that all values in the row that are deemed 'required' have values. This means that
     * subclasses need not litter their code with these easy validations.
     *
     * @param dataRow The row of data
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
                addDataMessage("Required value for " + header.getText() + " is missing", dataRowIndex);
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

    protected void addDataMessage(String message, int dataRowIndex) {
        String prefix = (sheetName == null) ? "" : "Sheet " + sheetName + ", ";

        validationMessages.add(prefix + "Row #" + (dataRowIndex + getNumHeaderRows()) + " " + message);
    }
}
