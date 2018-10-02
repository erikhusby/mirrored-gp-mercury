package org.broadinstitute.gpinformatics.infrastructure.parsers;


/**
 * Represents a row on a Spreadsheet that consists of a description or identifier (header) cell followed by
 * a data (value) cell.
 */
public interface HeaderValueRow {
    /** The header cell content, which should be unique for this sheet. */
    String getText();
    boolean isRequiredHeader();
    boolean isRequiredValue();
    boolean isDateColumn();
    boolean isStringColumn();
}
