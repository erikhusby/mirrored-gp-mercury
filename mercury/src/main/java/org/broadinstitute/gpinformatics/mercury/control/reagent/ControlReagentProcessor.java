package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for a spreadsheet containing tubes of control reagent.
 */
public class ControlReagentProcessor extends TableProcessor {
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
    private List<String> headers;

    private Map<String, ControlDto> mapTubeBarcodeToControl = new LinkedHashMap<>();
    private Map<ControlDto, ControlDto> mapControlToControl = new HashMap<>();

    public static class ControlDto {
        private String control;
        private String lot;
        private Date expiration;

        public ControlDto(String control, String lot, Date expiration) {
            this.control = control;
            this.lot = lot;
            this.expiration = expiration;
        }

        public String getControl() {
            return control;
        }

        public String getLot() {
            return lot;
        }

        public Date getExpiration() {
            return expiration;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ControlDto that = (ControlDto) o;

            if (!control.equals(that.control)) {
                return false;
            }
            if (!expiration.equals(that.expiration)) {
                return false;
            }
            if (!lot.equals(that.lot)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = control.hashCode();
            result = 31 * result + lot.hashCode();
            result = 31 * result + expiration.hashCode();
            return result;
        }
    }

    public ControlReagentProcessor(String sheetName) {
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
    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    @Override
    public void close() {
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex) {
        String tubeBarcode = dataRow.get(Headers.TUBE_BARCODE.getText());
        String control = dataRow.get(Headers.CONTROL.getText());
        String lot = dataRow.get(Headers.LOT.getText());
        String expiration = dataRow.get(Headers.EXPIRATION.getText());

        if (mapTubeBarcodeToControl.containsKey(tubeBarcode)) {
            addDataMessage("Duplicate tube barcode " + tubeBarcode, dataRowIndex);
        }
        try {
            // todo jmt error if expiration varies within lot?
            ControlDto controlKey = new ControlDto(control, lot, simpleDateFormat.parse(expiration));
            ControlDto controlValue = mapControlToControl.get(controlKey);
            if (controlValue == null) {
                mapControlToControl.put(controlKey, controlKey);
                controlValue = controlKey;
            }
            mapTubeBarcodeToControl.put(tubeBarcode, controlValue);
        } catch (ParseException e) {
            addDataMessage("Incorrect data format " + expiration, dataRowIndex);
        }
    }

    private enum Headers implements ColumnHeader {
        TUBE_BARCODE("tube barcode", 0, REQUIRED_HEADER, REQUIRED_VALUE, IS_STRING, NON_DATE),
        CONTROL("control", 1, REQUIRED_HEADER, REQUIRED_VALUE, IS_STRING, NON_DATE),
        LOT("lot", 2, REQUIRED_HEADER, REQUIRED_VALUE, IS_STRING, NON_DATE),
        EXPIRATION("expiration", 3, REQUIRED_HEADER, REQUIRED_VALUE, NON_STRING, IS_DATE),
        ;

        private final String text;
        private final int index;
        private final boolean requiredHeader;
        private final boolean requiredValue;
        private boolean isString;
        private boolean isDate;

        Headers(String text, int index, boolean requiredHeader, boolean requiredValue) {
            this(text, index, requiredHeader, requiredValue, false, false);
        }

        Headers(String text, int index, boolean requiredHeader, boolean requiredValue,
                boolean isString, boolean isDate) {
            this.text = text;
            this.index = index;
            this.requiredHeader = requiredHeader;
            this.requiredValue = requiredValue;
            this.isString = isString;
            this.isDate = isDate;
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
            return isDate;
        }

        @Override
        public boolean isStringColumn() {
            return isString;
        }
    }

    public Map<String, ControlDto> getMapTubeBarcodeToControl() {
        return mapTubeBarcodeToControl;
    }

    public Map<ControlDto, ControlDto> getMapControlToControl() {
        return mapControlToControl;
    }
}
