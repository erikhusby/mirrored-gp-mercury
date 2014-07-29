package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.boundary.util.AbstractSpreadsheetExporter;

/**
 */
public class SampleLedgerSpreadSheetWriter extends AbstractSpreadsheetExporter.SpreadSheetWriter {

    private static final int FIXED_HEADER_WIDTH = 259 * 15;

    void writeHeaderCell(String headerText, boolean vertical) {
        writeCell(headerText, getWrappedHeaderStyle(new byte[]{(byte) 204, (byte) 204, (byte) 255}, vertical));
        if (vertical) {
            setColumnWidth(900);
        } else {
            setColumnWidth(FIXED_HEADER_WIDTH);
        }
        setRowHeight((short) (getCurrentSheet().getDefaultRowHeight() * 4));
    }
}
