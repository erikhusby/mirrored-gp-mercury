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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeminiPlateProcessor extends TableProcessor {

    private static final Pattern BARCODE_PATTERN = Pattern.compile("Group: (\\d+)");
    private static final Pattern RUN_START_PATTERN = Pattern.compile("Original Filename: .*; Date Last Saved: (.*)");

    private List<String> headers;
    private final String barcode;
    private final int headerRowIndex;
    private List<VarioskanPlateProcessor.PlateWellResult> plateWellResults = new ArrayList<>();
    private boolean twoCurve;

    public GeminiPlateProcessor(Sheet sheet, String barcode, int headerRowIndex) {
        super(sheet.getSheetName());
        this.barcode = barcode;
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
                    String picoPlateBarcode = barcodeMatcher.group(1);
                    picoPlateBarcode = StringUtils.leftPad(picoPlateBarcode, 12, '0');
                    GeminiPlateProcessor plateProcessor =
                            fetchPlateWellResultsForPlate(picoPlateBarcode, sheet, row.getRowNum());
                    results.add(plateProcessor);
                }
            }
        }
        GeminiRunInfo runInfo = new GeminiRunInfo(runName, runStart);
        return Pair.of(runInfo, results);
    }

    private static GeminiPlateProcessor fetchPlateWellResultsForPlate(String barcode,
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
    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex) {
        try {
            String well = dataRow.get(Headers.WELL.getText());
            VesselPosition vesselPosition = VesselPosition.getByName(well);
            if (vesselPosition == null) {
                addDataMessage("Failed to find well with name " + well, dataRowIndex);
            }

            BigDecimal concentration = fetchConcentration(dataRow);

            VarioskanPlateProcessor.PlateWellResult plateWellResult = new VarioskanPlateProcessor.PlateWellResult(
                    barcode, vesselPosition, concentration);
            plateWellResults.add(plateWellResult);
        } catch (NumberFormatException nfe) {
            addDataMessage("Failed to parse concentration", dataRowIndex);
        }
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
        WELL("Wells"),
        CONCENTRATION("Concentration"),
        MEAN_CONC("MeanConc"),
        CONCENTRATION_BR("Conc with BroadRange"),
        CONCENTRATION_HS("Conc with HighSense");

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
