package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Processes plate information from a Varioskan spreadsheet.  The spreadsheet has:
 * <ul>
 * <li>
 * a title in row 2;
 * </li>
 * <li>
 * Parameters in rows 4-19, with name in column B, and value in column F;
 * </li>
 * <li>
 * Plate information with headers (see Headers enum) in row 21;
 * The plate information includes a standards plate with a barcode like "Curve" or "Standard Plate",
 * and two or three sample plates with numeric barcodes;
 * </li>
 * <li>
 * Curve Fit Calibrator Table typically in row 430 (depends on number of plates in previous table) with same headers,
 * but no sample plates.
 * </li>
 * </ul>
 * To focus on the Plate information, the parser ignores rows with non-numeric barcodes.
 */
public class VarioskanPlateProcessor extends TableProcessor {

    private static final Pattern BARCODE_PATTERN = Pattern.compile("[\\d\\.]*");
    private List<String> headers;
    private List<PlateWellResult> plateWellResults = new ArrayList<>();

    protected VarioskanPlateProcessor(String sheetName) {
        super(sheetName);
    }

    @Override
    public List<String> getHeaderNames() {
        return headers;
    }

    @Override
    public int getHeaderRowIndex() {
        return 20;
    }

    @Override
    public void processHeader(List<String> headers, int row) {
        this.headers = headers;
    }

    public class PlateWellResult {
        private String plateBarcode;
        private VesselPosition vesselPosition;
        private BigDecimal result;

        public PlateWellResult(String plateBarcode,
                VesselPosition vesselPosition, BigDecimal result) {
            this.plateBarcode = plateBarcode;
            this.vesselPosition = vesselPosition;
            this.result = result;
        }

        public String getPlateBarcode() {
            return plateBarcode;
        }

        public VesselPosition getVesselPosition() {
            return vesselPosition;
        }

        public BigDecimal getResult() {
            return result;
        }
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex) {
        String plate = dataRow.get(Headers.PLATE.getText());
        String well = dataRow.get(Headers.WELL.getText());
        String value = dataRow.get(Headers.VALUE.getText());
        String result = dataRow.get(Headers.RESULT.getText());

        if (plate != null && !plate.isEmpty() && BARCODE_PATTERN.matcher(plate).matches()) {
            String paddedBarcode = StringUtils.leftPad(plate, 12, '0');
            VesselPosition vesselPosition = VesselPosition.getByName(well.trim());
            if (vesselPosition == null) {
                addDataMessage("Failed to find position " + well, dataRowIndex);
            }
            try {
                BigDecimal bigDecimal = new BigDecimal(result);
                plateWellResults.add(new PlateWellResult(paddedBarcode, vesselPosition, bigDecimal));
            } catch (NumberFormatException e) {
                addDataMessage("Failed to find position " + well, dataRowIndex);
            }
        }
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    @Override
    public void close() {

    }

    public List<PlateWellResult> getPlateWellResults() {
        return plateWellResults;
    }

    private enum Headers implements ColumnHeader {
        PLATE("Plate", 0, REQUIRED_HEADER, OPTIONAL_VALUE),
        WELL("Well", 1, REQUIRED_HEADER, OPTIONAL_VALUE),
        SAMPLE("Sample", 2, REQUIRED_HEADER, OPTIONAL_VALUE),
        VALUE("Value", 3, REQUIRED_HEADER, OPTIONAL_VALUE),
        RESULT("Result", 4, REQUIRED_HEADER, OPTIONAL_VALUE);

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
