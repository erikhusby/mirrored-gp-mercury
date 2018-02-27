package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalLibraryBarcodeUpdate extends TableProcessor {
    private List<String> headerNames = new ArrayList<>();
    private Map<String, String> adjustedNames = new HashMap<>();
    private List<String> libraryNames = new ArrayList<>();
    private List<String> barcodes = new ArrayList<>();

    public ExternalLibraryBarcodeUpdate(String sheetName) {
        super(sheetName, IgnoreTrailingBlankLines.YES);
    }

    private String getFromRow(Map<String, String> dataRow, Headers header) {
        return dataRow.get(adjustedNames.get(adjustHeaderName(header.getText())));
    }

    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber, boolean requiredValuesPresent) {
        libraryNames.add(getFromRow(dataRow, Headers.LIBRARY_NAME));
        barcodes.add(getFromRow(dataRow, Headers.TUBE_BARCODE));
    }

    public List<String> getLibraryNames() {
        return libraryNames;
    }

    public List<String> getBarcodes() {
        return barcodes;
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    @Override
    public List<String> getHeaderNames() {
        return headerNames;
    }

    @Override
    public void processHeader(List<String> headers, int row) {
        headerNames.addAll(headers);
        for (String header : headers) {
            adjustedNames.put(adjustHeaderName(header), header);
        }
    }

    public String adjustHeaderName(String headerCell) {
        return headerCell.toLowerCase().contains("library") ? Headers.LIBRARY_NAME.getText() :
                headerCell.toLowerCase().contains("barcode") ? Headers.TUBE_BARCODE.getText() : headerCell;
    }

    @Override
    public void close() {
    }

    public enum Headers implements ColumnHeader {
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
        public boolean isDateColumn() {
            return isDate;
        }

        @Override
        public boolean isStringColumn() {
            return isString;
        }
    }
}
