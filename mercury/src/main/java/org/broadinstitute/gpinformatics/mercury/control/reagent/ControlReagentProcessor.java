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
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
    private List<String> headers;

    private final Map<String, ControlDto> mapTubeBarcodeToControl = new LinkedHashMap<>();
    private final Map<ControlDto, ControlDto> mapControlToControl = new HashMap<>();

    public static class ControlDto {
        private final String control;
        private final String lot;
        private final Date expiration;

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
    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber, boolean requiredValuesPresent) {
        String tubeBarcode = dataRow.get(Headers.TUBE_BARCODE.getText());
        String control = dataRow.get(Headers.CONTROL.getText());
        String lot = dataRow.get(Headers.LOT.getText());
        String expiration = dataRow.get(Headers.EXPIRATION.getText());

        if (mapTubeBarcodeToControl.containsKey(tubeBarcode)) {
            addDataMessage("Duplicate tube barcode " + tubeBarcode, dataRowNumber);
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
            addDataMessage("Incorrect data format " + expiration, dataRowNumber);
        }
    }

    private enum Headers implements ColumnHeader {
        TUBE_BARCODE("tube barcode"),
        CONTROL("control"),
        LOT("lot"),
        EXPIRATION("expiration", IsDate.YES),
        ;

        private final String text;
        private final IsDate isDate;

        private enum IsDate {
            YES, NO
        }

        Headers(String text) {
            this(text, IsDate.NO);
        }

        Headers(String text, IsDate isDate) {
            this.text = text;
            this.isDate = isDate;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean isRequiredHeader() {
            return true;
        }

        @Override
        public boolean isRequiredValue() {
            return true;
        }

        @Override
        public boolean isDateColumn() {
            return isDate == IsDate.YES;
        }

        @Override
        public boolean isStringColumn() {
            return isDate != IsDate.YES;
        }
    }

    public Map<String, ControlDto> getMapTubeBarcodeToControl() {
        return mapTubeBarcodeToControl;
    }

    public Map<ControlDto, ControlDto> getMapControlToControl() {
        return mapControlToControl;
    }
}
