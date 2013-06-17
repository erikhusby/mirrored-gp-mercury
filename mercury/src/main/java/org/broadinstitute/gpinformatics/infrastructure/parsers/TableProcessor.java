package org.broadinstitute.gpinformatics.infrastructure.parsers;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * Base class for all (hopefully) spreadsheet parsing duties.  Children of this class only need to define how to
 * map the data to ... whatever it maps to for that particular need.
 *
 *
 */
public abstract class TableProcessor implements Serializable {

    private static final long serialVersionUID = 8122298462727182883L;

    protected List<String> validationMessages = new ArrayList<>();

    protected TableProcessor() {
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
     * Header method used by all parasers to get the names that will be used to map values to the appropriate columns.
     *
     * @return The list of header names.
     */
    public abstract List<String> getHeaderNames();

    /**
     * Take all headers identified for the spscified row and pull whatever important data is needed from that to help
     * process the date that comes from the table cells.
     *
     * @param headerValues The header strings in this row
     * @param row The row
     */
    public abstract void processHeader(List<String> headerValues, int row);

    /**
     * The primary header row is the one that is validated in hasRequiredHeaders.
     *
     * @return By default it is 0, but any header row could be the primary.
     */
    public int getPrimaryHeaderRow() {
        return 0;
    }

    /**
     * This method defines the specific logic necessary to parse the data in the given table.  The concrete parser will
     * be responsible for constructing any  necessary entities to represent the data found in this table row.
     *
     * @param dataRow The row of data mapped by header name.
     * @param dataRowIndex The current row of data we are working with.
     */
    public abstract void processRow(Map<String, String> dataRow, int dataRowIndex);

    public boolean validateHeaders(List<String> headers) {
        // If any of the required headers are NOT in the header list, then return false.
        for (ColumnHeader header : getColumnHeaders()) {
            if (header.isRequredHeader() && !headers.contains(header.getText())) {
                validationMessages.add("Required header: " + header.getText() + " is missing");
                return false;
            }
        }

        return true;
    }

    public boolean hasRequiredValues(Map<String, String> dataRow, int dataRowIndex) {
        boolean hasValue = true;

        // If any of the required values are empty or missing in the data row, return false.
        for (ColumnHeader header : getColumnHeaders()) {
            if (header.isRequiredValue() &&
                (!dataRow.containsKey(header.getText()) || StringUtils.isBlank(dataRow.get(header.getText())))) {
                validationMessages.add("Row #" + dataRowIndex + " value for " + header.getText() + " is missing");
                hasValue = false;
            }
        }

        return hasValue;
    }

    protected abstract ColumnHeader[] getColumnHeaders();
}
