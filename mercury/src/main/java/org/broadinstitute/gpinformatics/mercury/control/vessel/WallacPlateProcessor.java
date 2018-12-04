package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Processes plate information from a Wallac spreadsheet.
 */
public class WallacPlateProcessor  extends TableProcessor {
    private static final String PLATE_1_GROUP = "2";
    private static final String PLATE_2_GROUP = "3";
    private final String plateBarcode1;
    private final String plateBarcode2;
    private List<VarioskanPlateProcessor.PlateWellResult> plateWellResults = new ArrayList<>();
    private List<String> headers;

    public WallacPlateProcessor(String sheetName, String plateBarcode1, String plateBarcode2) {
        super(sheetName);
        this.plateBarcode1 = StringUtils.leftPad(plateBarcode1, 12, '0');
        this.plateBarcode2 = StringUtils.leftPad(plateBarcode2, 12, '0');
    }

    @Override
    public List<String> getHeaderNames() {
        return headers;
    }

    @Override
    public void processHeader(List<String> headerNames, int row) {
        headers = new ArrayList<>();
        // "Group" header name appears in column A but the values for it are actually found in column B.
        headers.add(headerNames.get(1));
        headers.add(headerNames.get(0));
        headers.addAll(headerNames.subList(2, headerNames.size()));
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber, boolean requiredValuesPresent) {
        String group = dataRow.get(Headers.GROUP.getText());
        String plate = dataRow.get(Headers.PLATE.getText());
        String well = dataRow.get(Headers.WELL.getText());
        String value = dataRow.get(Headers.VALUE.getText());
        if (group != null && !group.isEmpty() && !group.toLowerCase().contains("standard")) {
            if (plate != null && !plate.isEmpty()) {
                String plateBarcode = null;
                if (plate.equals(PLATE_1_GROUP))
                    plateBarcode = plateBarcode1;
                else if (plate.equals(PLATE_2_GROUP))
                    plateBarcode = plateBarcode2;
                if (plateBarcode != null) {
                    VesselPosition vesselPosition = VesselPosition.getByName(well.trim());
                    if (vesselPosition == null) {
                        addDataMessage("Failed to find position " + well, dataRowNumber);
                    }
                    try {
                        BigDecimal bigDecimal = new BigDecimal(value);
                        bigDecimal = MathUtils.scaleTwoDecimalPlaces(bigDecimal);
                        plateWellResults.add(
                                new VarioskanPlateProcessor.PlateWellResult(plateBarcode, vesselPosition, bigDecimal));
                    } catch (NumberFormatException e) {
                        addDataMessage("Failed to parse number " + value, dataRowNumber);
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

    public List<VarioskanPlateProcessor.PlateWellResult> getPlateWellResults() {
        return plateWellResults;
    }

    private enum Headers implements ColumnHeader {
        GROUP("Group", IS_STRING),
        PLATE("Plate", IS_STRING),
        WELL("Wells", IS_STRING),
        VALUE("Sample Conc. Mean");

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
