package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;

import java.util.List;
import java.util.Map;

/**
 * Parses a spreadsheet (from BSP) containing sample IDs and tube barcodes.
 */
public class SampleVesselProcessor extends TableProcessor {

    private List<String> headers;

    public SampleVesselProcessor(String sheetName) {
        super(sheetName);
    }

    @Override
    public List<String> getHeaderNames() {
        return headers;
    }

    @Override
    public void processHeader(List<String> headers, int row) {
        this.headers = headers;
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex) {
        String sampleId = dataRow.get(Headers.SAMPLE_ID.getText());
        String tubeBarcode = dataRow.get(Headers.MANUFACTURER_TUBE_BARCODE.getText());
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    @Override
    public void close() {
        ;
    }

    private enum Headers implements ColumnHeader {
        SAMPLE_ID("Sample ID", 0, REQUIRED_HEADER, REQUIRED_VALUE),
        MANUFACTURER_TUBE_BARCODE("Manufacturer Tube Barcode", 1, REQUIRED_HEADER, REQUIRED_VALUE);

        private final String text;
        private final int index;
        private final boolean requiredHeader;
        private final boolean requiredValue;
        private boolean isString;

        Headers(String text, int index, boolean requiredHeader, boolean requiredValue) {
            this(text, index, requiredHeader, requiredValue, false);
        }

        Headers(String text, int index, boolean requiredHeader, boolean requiredValue,
                boolean isString) {
            this.text = text;
            this.index = index;
            this.requiredHeader = requiredHeader;
            this.requiredValue = requiredValue;
            this.isString = isString;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public boolean isRequiredHeader() {
            return requiredHeader;
        }

        @Override
        public boolean isRequiredValue() {
            return requiredValue;
        }

        @Override
        public boolean isDateColumn() {
            return false;
        }

        @Override
        public boolean isStringColumn() {
            return isString;
        }
    }
}
