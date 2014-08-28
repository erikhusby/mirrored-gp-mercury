package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.poi.ss.usermodel.IndexedColors;
import org.broadinstitute.gpinformatics.athena.boundary.util.AbstractSpreadsheetExporter;

/**
 * A {@link AbstractSpreadsheetExporter.SpreadSheetWriter} specific to writing billing tracker spreadsheets.
 */
public class SampleLedgerSpreadSheetWriter extends AbstractSpreadsheetExporter.SpreadSheetWriter {

    private static final int FIXED_HEADER_WIDTH = 259 * 15;

    /**
     * Width wide enough for one line of text rotated 90 degrees.
     */
    private static final int VERTICAL_HEADER_WIDTH = 900;

    /**
     * Writes a header cell with styling appropriate for the billing tracker.
     *
     * @param headerText    the text for the header
     * @param vertical      true to have the header text rotated 90 degrees counter-clockwise
     */
    void writeHeaderCell(String headerText, boolean vertical) {
        writeCell(headerText, getWrappedHeaderStyle(new byte[]{(byte) 204, (byte) 204, (byte) 255}, vertical));
        if (vertical) {
            setColumnWidth(VERTICAL_HEADER_WIDTH);
        } else {
            setColumnWidth(FIXED_HEADER_WIDTH);
        }
        // Increase the row height to make room for long headers that wrap to multiple lines.
        setRowHeight((short) (getCurrentSheet().getDefaultRowHeight() * 4));
    }

    void writeHistoricalBilledAmount(double amount) {
        writeCell(amount, AbstractSpreadsheetExporter
                .buildHeaderStyle(getWorkbook(), IndexedColors.GREY_25_PERCENT));
    }
}
