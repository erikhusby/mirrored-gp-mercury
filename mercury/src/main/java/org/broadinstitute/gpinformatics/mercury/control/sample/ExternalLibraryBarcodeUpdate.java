package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.HeaderValueRow;

import java.util.Map;

public class ExternalLibraryBarcodeUpdate extends ExternalLibraryProcessor {

    public ExternalLibraryBarcodeUpdate(String sheetName) {
        super(sheetName);
        headerValueNames.clear();
    }

    @Override
    public HeaderValueRow[] getHeaderValueRows() {
        return new HeaderValueRow[0];
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber, boolean requiredValuesPresent) {
        libraryName.add(getFromRow(dataRow, Headers.LIBRARY_NAME));
        barcodes.add(getFromRow(dataRow, Headers.TUBE_BARCODE));
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    @Override
    public String adjustHeaderName(String headerCell) {
        return headerCell.toLowerCase().contains("library") ? Headers.LIBRARY_NAME.getText() :
                headerCell.toLowerCase().contains("barcode") ? Headers.TUBE_BARCODE.getText() : headerCell;
    }

    @Override
    public void close() {
    }

    public enum Headers implements ColumnHeader, ColumnHeader.Ignorable {
        LIBRARY_NAME("Library Name", REQUIRED_HEADER),
        TUBE_BARCODE("Tube Barcode", REQUIRED_HEADER),
        ;

        private final String text;
        private boolean requiredHeader;
        private boolean requiredValue;
        private boolean isString = true;
        private boolean isDate = false;

        Headers(String text, boolean isRequired) {
            this.text = text;
            this.requiredHeader = isRequired;
            this.requiredValue = isRequired;
        }

        @Override
        public String getText() {
            return text;
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
        public boolean isIgnoredValue() {
            return false;
        }

        @Override
        public boolean isDateColumn() {
            return isDate;
        }

        @Override
        public boolean isStringColumn() {
            return isString;
        }
    }
}
