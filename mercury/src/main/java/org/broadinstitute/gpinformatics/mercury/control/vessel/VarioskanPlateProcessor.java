package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
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
 * Curve Fit Calibrator Table typically in row 430 (depends on number of plates in previous table) with similar headers,
 * but no sample plates.
 * </li>
 * </ul>
 * To focus on the Plate information, the parser ignores rows with non-numeric barcodes.
 */
public class VarioskanPlateProcessor extends TableProcessor {

    // The barcode sometimes has a trailing .0, so use a group to extract only the digits before that
    private static final Pattern BARCODE_PATTERN = Pattern.compile("(\\d*)\\.?\\d*");
    public static final int SCALE = 2;
    private List<String> headers;
    private List<PlateWellResult> plateWellResults = new ArrayList<>();

    public VarioskanPlateProcessor(String sheetName) {
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

    public static class PlateWellResult {
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

        if (plate != null && !plate.isEmpty()) {
            Matcher matcher = BARCODE_PATTERN.matcher(plate);
            if (matcher.matches()) {
                String paddedBarcode = StringUtils.leftPad(matcher.group(1), 12, '0');
                VesselPosition vesselPosition = VesselPosition.getByName(well.trim());
                if (vesselPosition == null) {
                    addDataMessage("Failed to find position " + well, dataRowIndex);
                }
                try {
                    BigDecimal bigDecimal;
                    if (result.equals("NaN")) {
                        // result = 0.73 * value + 0.69
                        bigDecimal = new BigDecimal(value).multiply(new BigDecimal("0.73")).add(new BigDecimal("0.69"));
                    } else {
                        bigDecimal = new BigDecimal(result);
                    }
                    bigDecimal = bigDecimal.setScale(SCALE, BigDecimal.ROUND_HALF_UP);
                    plateWellResults.add(new PlateWellResult(paddedBarcode, vesselPosition, bigDecimal));
                } catch (NumberFormatException e) {
                    addDataMessage("Failed to find position " + well, dataRowIndex);
                }
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
        PLATE("Plate", IS_STRING),
        WELL("Well", IS_STRING),
        SAMPLE("Sample", IS_STRING),
        VALUE("Value"),
        RESULT("Result");

        private final String text;
        private final boolean isString;

        Headers(String text, boolean isString) {
            this.text = text;
            this.isString = isString;
        }

        Headers(String text) {
            this(text, false);
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
            return false;
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
