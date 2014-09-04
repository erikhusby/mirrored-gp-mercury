package org.broadinstitute.gpinformatics.infrastructure.parsers;


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
    boolean IS_DATE = true;
    boolean NON_DATE = false;
    boolean IS_STRING = true;
    boolean NON_STRING = false;

    /**
     * @return The text used to match the header in the input.  This should be unique in the list of headers
     *         provided to the parser.
     */
    String getText();

    // Index should NOT be used for parsing. It is only available for positioning in generated files. We want to parse
    // any file that comes with columns in any order.
    int getIndex();

    boolean isRequiredHeader();
    boolean isRequiredValue();

    boolean isDateColumn();
    boolean isStringColumn();
}
