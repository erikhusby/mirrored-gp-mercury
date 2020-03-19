package org.broadinstitute.gpinformatics.infrastructure.parsers;


import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Derived implementation from BSP core code.  This interface is intended to represent a column on a Spreadsheet
 * that is to be parsed by the {@link TableProcessor}.  This interface will assist in making the parser generic
 * enough to be used by part of the system.
 */
public interface ColumnHeader {
    boolean REQUIRED_HEADER = true;
    boolean OPTIONAL_HEADER = false;
    boolean REQUIRED_VALUE = true;
    boolean OPTIONAL_VALUE = false;
    boolean IS_STRING = true;
    boolean NON_STRING = false;

    /**
     * Convenience method for retrieving ColumnHeader text given an list of headers
     *
     * @param columnHeaders Array of ColumnHeaders you would like the text for.
     *
     * @return a List of header names.
     */
    static List<String> headerNames(ColumnHeader... columnHeaders) {
        List<String> allHeaderNames = new ArrayList<>(columnHeaders.length);
        for (ColumnHeader manifestHeader : columnHeaders) {
            allHeaderNames.add(manifestHeader.getText());
        }
        return allHeaderNames;
    }

    /**
     * @return The text used to match the header in the input.  This should be unique in the list of headers
     *         provided to the parser.
     */
    String getText();

    boolean isRequiredHeader();
    boolean isRequiredValue();

    boolean isDateColumn();
    boolean isStringColumn();

    /** Interface that extends the data presence indicators. */
    interface Ignorable {
        ExternalLibraryProcessor.DataPresence getDataPresenceIndicator();
    }
}
