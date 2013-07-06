package org.broadinstitute.gpinformatics.infrastructure.parsers;


/**
 * Derived implementation from BSP core code.  This interface is intended to represent a column on a Spreadsheet
 * that is to be parsed by the {@link TableProcessor}.  This interface will assist in making the parser generic
 * enough to be used by part of the system.
 */
public interface ColumnHeader {

    public static final boolean REQUIRED_HEADER = true;
    public static final boolean OPTIONAL_HEADER = false;
    public static final boolean REQUIRED_VALUE = true;
    public static final boolean OPTIONAL_VALUE = false;

    /**
     * @return The text used to match the header in the input.  This should be unique in the list of headers
     *         provided to the parser.
     */
    String getText();

    int getIndex();

    boolean isRequiredHeader();
    boolean isRequiredValue();

    boolean isDateColumn();
}
