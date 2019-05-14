package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.jvnet.inflector.Noun;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GeminiPlateProcessor extends TableProcessor {

    private static final String GROUP_PREFIX = "Group: ";
    private static final Pattern BARCODE_PATTERN = Pattern.compile(GROUP_PREFIX + "([0-9,]+)");
    private static final String UNKNOWNS_GROUP = "Group: Unknowns_NoDiln";
    public static final String DATE_PREFIX = "Original Filename: .*; Date Last Saved: ";
    public static final String DATE_REGEX = DATE_PREFIX + "(.*)";
    private static final Pattern RUN_START_PATTERN = Pattern.compile(DATE_REGEX);


    private List<String> headers;
    private final List<String> barcodes;
    private final int headerRowIndex;
    private List<VarioskanPlateProcessor.PlateWellResult> plateWellResults = new ArrayList<>();
    private boolean twoCurve;

    public GeminiPlateProcessor(Sheet sheet, List<String> barcodes, int headerRowIndex) {
        super(sheet.getSheetName());
        this.barcodes = barcodes;
        this.headerRowIndex = headerRowIndex;
    }

    public static Pair<GeminiRunInfo, List<GeminiPlateProcessor>> parse(Workbook workbook, String filename)
            throws ValidationException {
        List<GeminiPlateProcessor> results = new ArrayList<>();
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        String runName = filename;
        Date runStart = null;

        for (Row row: sheet) {
            Cell cell = row.getCell(0);
            if (cell != null && cell.getCellType() == Cell.CELL_TYPE_STRING) {
                Matcher runStartMatcher = RUN_START_PATTERN.matcher(cell.getStringCellValue());
                if (runStartMatcher.matches()) {
                    SimpleDateFormat simpleDateFormat =
                            new SimpleDateFormat(VarioskanRowParser.NameValue.RUN_STARTED.getDateFormat());
                    try {
                        runStart = simpleDateFormat.parse(runStartMatcher.group(1));
                    } catch (ParseException e) {
                        throw new RuntimeException("Failed to parse run date");
                    }
                }
            }
        }

        if (runStart == null) {
            runStart = new Date();
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Cell cell = row.getCell(0);
            if (cell != null && cell.getCellType() == Cell.CELL_TYPE_STRING) {
                Matcher barcodeMatcher = BARCODE_PATTERN.matcher(cell.getStringCellValue());
                if (barcodeMatcher.matches()) {
                    row = rowIterator.next();

                    // Duplicate Pico Stores Barcode in same row with a comma
                    List<String> plateBarcodes = Arrays.asList(barcodeMatcher.group(1).split(","));

                    // Pad Each Barcode to 12 digits
                    plateBarcodes = plateBarcodes.stream()
                            .map(b -> StringUtils.leftPad(b, 12, '0'))
                            .collect(Collectors.toList());

                    GeminiPlateProcessor plateProcessor =
                            fetchPlateWellResultsForPlate(plateBarcodes, sheet, row.getRowNum());
                    results.add(plateProcessor);
                } else if (UNKNOWNS_GROUP.matches(cell.getStringCellValue())) {
                    // Initial Pico Protocol is being run which has plate barcode column
                    row = rowIterator.next();
                    GeminiPlateProcessor geminiPlateProcessor = new GeminiPlateProcessor(sheet, null, row.getRowNum());
                    PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.emptyMap());
                    parser.processRows(sheet, geminiPlateProcessor);
                    results.add(geminiPlateProcessor);
                    break;
                }
            }
        }
        GeminiRunInfo runInfo = new GeminiRunInfo(runName, runStart);
        return Pair.of(runInfo, results);
    }

    private static GeminiPlateProcessor fetchPlateWellResultsForPlate(List<String> barcode,
                                                                      Sheet sheet, int headerRowIndex)
            throws ValidationException {
        GeminiPlateProcessor geminiPlateProcessor = new GeminiPlateProcessor(sheet, barcode, headerRowIndex);
        PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.emptyMap());
        parser.processRows(sheet, geminiPlateProcessor);
        return geminiPlateProcessor;
    }

    public List<VarioskanPlateProcessor.PlateWellResult> getPlateWellResults() {
        return plateWellResults;
    }

    @Override
    public void validateHeaderRow(List<String> headers) {
        List<String> errors = new ArrayList<>();
        Headers.fromColumnName(errors, headers.toArray(new String[headers.size()]));
        if (!errors.isEmpty()){
            getMessages()
                    .add(String.format("Unknown %s '%s' present.", Noun.pluralOf("header", errors.size()), errors));
        }
    }

    @Override
    public List<String> getHeaderNames() {
        return headers;
    }

    @Override
    public void processHeader(List<String> headers, int row) {
        this.twoCurve = headers.contains("Conc with BroadRange") && headers.contains("Conc with HighSense");
        this.headers = headers;
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex, boolean requiredValuesPresent) {
        try {
            String well = dataRow.get(Headers.WELL.getText());
            VesselPosition vesselPosition = VesselPosition.getByName(well);
            if (vesselPosition == null) {
                addDataMessage("Failed to find well with name " + well, dataRowIndex);
            }

            BigDecimal concentration = fetchConcentration(dataRow);

            String barcode = fetchBarcode(dataRow);
            if (barcode == null) {
                addDataMessage("Failed to parse barcode.", dataRowIndex);
                return;
            }

            VarioskanPlateProcessor.PlateWellResult plateWellResult = new VarioskanPlateProcessor.PlateWellResult(
                    barcode, vesselPosition, concentration);
            plateWellResults.add(plateWellResult);
        } catch (NumberFormatException nfe) {
            addDataMessage("Failed to parse concentration", dataRowIndex);
        }
    }

    /**
     * Barcodes stored in duplicate on table or one per read.
     */
    private String fetchBarcode(Map<String, String> dataRow) {
        if (barcodes == null) {
            return StringUtils.leftPad(dataRow.get(Headers.PLATE_BARCODE.getText()), 12, '0');
        }
        if (barcodes.size() == 1) {
            return barcodes.get(0);
        }
        if (barcodes.size() == 2) {
            if (!StringUtils.isBlank(dataRow.get(Headers.SAMPLE.getText()))) {
                return barcodes.get(0);
            } else {
                return barcodes.get(1);
            }
        }
        return null;
    }

    private BigDecimal fetchConcentration(Map<String, String> dataRow) {
        return twoCurve ? fetchTwoCurveConcentration(dataRow)
                : new BigDecimal(dataRow.get(Headers.CONCENTRATION.getText()));
    }

    private BigDecimal fetchTwoCurveConcentration(Map<String, String> dataRow) {
        BigDecimal broadRange = new BigDecimal(dataRow.get(Headers.CONCENTRATION_BR.getText()));
        BigDecimal highSense = new BigDecimal(dataRow.get(Headers.CONCENTRATION_HS.getText()));
        if (broadRange.compareTo(VarioskanPlateProcessorTwoCurve.PicoCurve.BROAD_RANGE.getLowestAccurateRead()) > 0) {
            return broadRange;
        } else {
            return highSense;
        }
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    @Override
    public int getHeaderRowIndex() {
        return headerRowIndex;
    }

    @Override
    public boolean quitOnMatch(Collection<String> dataByHeader) {
        for (String data: dataByHeader) {
            if (data.matches("Group Column")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() {

    }

    private enum Headers implements ColumnHeader {
        SAMPLE("Sample"),
        WELL("Wells"),
        CONCENTRATION("Concentration"),
        MEAN_CONC("MeanConc"),
        CONCENTRATION_BR("Conc with BroadRange"),
        CONCENTRATION_HS("Conc with HighSense"),
        RFU_VALUES("RFU_Values"),
        SD("SD"),
        CV("CV"),
        MEAN_CONC_WITH_BR("MeanConc with BR"),
        MEAN_CONC_WITH_HS("MeanConc with HS"),
        VALUES("Values"),
        MEAN_RESULT("MeanResult"),
        PLATE_BARCODE("Plate Barcode");

        private final String text;
        private final boolean requiredHeader;

        Headers(String text, boolean requiredHeader) {
            this.text = text;
            this.requiredHeader = requiredHeader;
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
            return requiredHeader;
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
            return true;
        }

        /**
         * Lookup all ColumnHeaders matching column names.
         *
         * @param columnNames column names to find enum values for.
         *
         * @return Collection of ColumnHeaders for the columnNames
         */
        static Collection<Headers> fromColumnName(List<String> errors, String... columnNames) {
            List<Headers> matches = new ArrayList<>();
            for (String columnName : columnNames) {
                try {
                    matches.add(Headers.fromColumnName(columnName));
                } catch (IllegalArgumentException e) {

                    // If a header cell is not blank.
                    if (!columnName.isEmpty()) {
                        errors.add(columnName);
                    }
                }
            }
            return matches;
        }

        /**
         * Look up the ManifestHeader for given columnHeader.
         *
         * @param columnHeader column to search for.
         *
         * @return ManifestHeader for given column.
         *
         * @throws IllegalArgumentException if enum does not exist for columnHeader.
         */
        public static Headers fromColumnName(String columnHeader) {
            for (Headers header : Headers.values()) {
                if (header.getText().equals(columnHeader)) {
                    return header;
                }
            }
            throw new IllegalArgumentException("No header found with name " + columnHeader);
        }
    }

    public static class GeminiRunInfo {
        private String runName;
        private Date runStart;

        public GeminiRunInfo(String runName, Date runStart) {
            this.runName = runName;
            this.runStart = runStart;
        }

        public String getRunName() {
            return runName;
        }

        public Date getRunStart() {
            return runStart;
        }
    }
}
