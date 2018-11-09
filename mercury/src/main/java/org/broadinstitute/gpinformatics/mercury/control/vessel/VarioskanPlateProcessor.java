package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
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
    private static final String SAMPLE_PREFIX = "Un_";
    public static final int SCALE = 2;
    private List<String> headers;
    private List<PlateWellResult> plateWellResults = new ArrayList<>();
    private LabMetric.MetricType metricType;
    private final boolean extrapolateIfNaN;
    private final VarioskanPlateProcessorTwoCurve.PicoCurve curve;

    public VarioskanPlateProcessor(String sheetName, LabMetric.MetricType metricType) {
        this(sheetName, metricType, true, VarioskanPlateProcessorTwoCurve.PicoCurve.BROAD_RANGE);
    }

    public VarioskanPlateProcessor(String sheetName, LabMetric.MetricType metricType, boolean extrapolateIfNaN, VarioskanPlateProcessorTwoCurve.PicoCurve curve) {
        super(sheetName);
        this.metricType = metricType;
        this.extrapolateIfNaN = extrapolateIfNaN;
        this.curve = curve;
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
        private boolean naN;
        private VarioskanPlateProcessorTwoCurve.PicoCurve curve;
        private BigDecimal value;
        private boolean overTheCurve = false;

        public PlateWellResult(String plateBarcode,
                               VesselPosition vesselPosition, BigDecimal result) {
            this(plateBarcode, vesselPosition, result, false, VarioskanPlateProcessorTwoCurve.PicoCurve.BROAD_RANGE,
                    null);
        }

        public PlateWellResult(String plateBarcode, VesselPosition vesselPosition, BigDecimal result, boolean naN,
                               VarioskanPlateProcessorTwoCurve.PicoCurve curve, BigDecimal value) {
            this.plateBarcode = plateBarcode;
            this.vesselPosition = vesselPosition;
            this.result = result;
            this.naN = naN;
            this.curve = curve;
            this.value = value;
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

        public boolean isNaN() {
            return naN;
        }

        public void setResult(BigDecimal result) {
            this.result = result;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setOverTheCurve(boolean overTheCurve) {
            this.overTheCurve = overTheCurve;
        }

        public boolean isOverTheCurve() {
            return overTheCurve;
        }
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber, boolean requiredValuesPresent) {
        String plate = dataRow.get(Headers.PLATE.getText());
        String sample = dataRow.get(Headers.SAMPLE.getText());
        String well = dataRow.get(Headers.WELL.getText());
        String value = dataRow.get(Headers.VALUE.getText());
        String result = dataRow.get(Headers.RESULT.getText());

        if (plate != null && !plate.isEmpty()) {
            if (sample !=null && sample.startsWith(SAMPLE_PREFIX)) {
                Matcher matcher = BARCODE_PATTERN.matcher(plate);
                // value and result empty means the row is in the Curve Fit Calibrator Table
                if (matcher.matches() && !StringUtils.isEmpty(value) && !StringUtils.isEmpty(result)) {
                    String paddedBarcode = StringUtils.leftPad(matcher.group(1), 12, '0');
                    VesselPosition vesselPosition = VesselPosition.getByName(well.trim());
                    if (vesselPosition == null) {
                        addDataMessage("Failed to find position " + well, dataRowNumber);
                    }
                    try {
                        BigDecimal bigDecimal;
                        if (result.equals("NaN")) {
                            if (extrapolateIfNaN) {
                                if (metricType == LabMetric.MetricType.PLATING_RIBO) {
                                    throw new RuntimeException("NaN not currently supported for RIBO");
                                }
                                // result = 0.73 * value + 0.69
                                bigDecimal =
                                        new BigDecimal(value).multiply(new BigDecimal("0.73"))
                                                .add(new BigDecimal("0.69"));
                            } else {
                                bigDecimal = null;
                            }
                        } else {
                            bigDecimal = new BigDecimal(result);
                        }
                        bigDecimal = MathUtils.scaleTwoDecimalPlaces(bigDecimal);
                        plateWellResults.add(new PlateWellResult(paddedBarcode, vesselPosition, bigDecimal, bigDecimal == null, curve, new BigDecimal(value)));
                    } catch (NumberFormatException e) {
                        addDataMessage("Failed to parse number " + result, dataRowNumber);
                    }
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
