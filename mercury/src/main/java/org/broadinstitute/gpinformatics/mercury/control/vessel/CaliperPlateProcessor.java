package org.broadinstitute.gpinformatics.mercury.control.vessel;

import com.opencsv.bean.CsvToBeanFilter;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.csv.CsvParser;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processes plate information from a Caliper csv.  The csv has:
 * <ul>
 * <li>
 * Plate Name in column 1, Well in Column 2, Sample Name in column 3, and Qual Score in column 5;
 * </li>
 * </ul>
 */
public class CaliperPlateProcessor {

    // The quality score sometimes in enclosed in brackets so use a group to extract the score inside
    private static final Pattern SCORE_BRACKET_PATTERN = Pattern.compile("\\[(\\S*)\\]");

    public static final Map<String, String> COL_TO_FIELD_MAP = new HashMap<String, String>() {
        {
            put("Plate Name", "plateName");
            put("Barcode", "barcode");
            put("Date_Time", "dateTimeString");
            put("Well Label", "well");
            put("Sample Name", "sampleName");
            put("RNA Quality Score", "rnaQualityScore");
            put("Instrument Name", "instrumentName");
            put("Lower Marker Time(sec)", "lowerMarkerTime");
            put("Region[200-6000] % of Total Area", "dv200TotalArea");
        }
    };

    private MessageCollection messageCollection;
    private List<CaliperDataRow> dataRows;

    public CaliperPlateProcessor() {
        messageCollection = new MessageCollection();
    }

    public MessageCollection getMessageCollection() {
        return messageCollection;
    }

    public List<CaliperDataRow> getDataRows() {
        return dataRows;
    }

    public CaliperRun parse(InputStream inputStream) throws IOException {
        //Should match 384 Well only, not Ladders
        CsvToBeanFilter columnStartsWithFilter = new ColumnMatchesFilter(1, "[A-P]\\d{2}");
        dataRows = CsvParser.parseCsvStreamToBeanByMapping(
                inputStream, ',', CaliperDataRow.class, COL_TO_FIELD_MAP, columnStartsWithFilter, 0);

        List<PlateWellResultMarker> plateWellResultMarkers = new ArrayList<>();
        CaliperRun caliperRun = new CaliperRun(plateWellResultMarkers);
        for (CaliperDataRow dataRow: dataRows) {
            String well = dataRow.getWell();
            String result = dataRow.getRnaQualityScore();
            String barcode = dataRow.getBarcode();
            double lowerTimeMarker = dataRow.getLowerMarkerTime();
            double dv200TotalArea = dataRow.getDv200TotalArea();
            VesselPosition vesselPosition = VesselPosition.getByName(well);
            if (vesselPosition == null) {
                messageCollection.addError("Failed to find position " + well);
            }
            BigDecimal bigDecimal;
            boolean nan = false;
            if (result.equals("NA")) {
                bigDecimal = new BigDecimal("0");
                nan = true;
            } else {
                Matcher matcher = SCORE_BRACKET_PATTERN.matcher(result);
                if (matcher.matches()) {
                    double score = Double.parseDouble(matcher.group(1));
                    bigDecimal = new BigDecimal(score);
                } else {
                    bigDecimal = new BigDecimal(result);
                }
            }
            bigDecimal = MathUtils.scaleTwoDecimalPlaces(bigDecimal);
            plateWellResultMarkers.add(
                    new PlateWellResultMarker(barcode, vesselPosition, bigDecimal, lowerTimeMarker, dv200TotalArea, nan));
        }

        //Grab run info
        if(!dataRows.isEmpty()) {
            CaliperDataRow row = dataRows.get(0);
            caliperRun.setInstrumentName(row.getInstrumentName());
            caliperRun.setRunDate(row.getDate());
            caliperRun.setPlateBarcode(row.getBarcode());
            caliperRun.setRunName(row.getBarcode() + "_" + row.getDateTimeString());
        }

        return caliperRun;
    }

    /**
     * Filter line in CSV where column index doesn't match a regex
     */
    private class ColumnMatchesFilter implements CsvToBeanFilter {

        private final int columnIndex;
        private final String regex;

        private ColumnMatchesFilter(int columnIndex, String regex) {
            this.columnIndex = columnIndex;
            this.regex = regex;
        }

        @Override
        public boolean allowLine(String[] line) {
            String value = line[columnIndex];
            return value != null && value.matches(regex);
        }
    }

    public class CaliperRun {
        private Date runDate;
        private String plateBarcode;
        private String instrumentName;
        private String runName;
        private List<PlateWellResultMarker> plateWellResultMarkers;

        public CaliperRun(
                List<PlateWellResultMarker> plateWellResultMarkers) {
            this.plateWellResultMarkers = plateWellResultMarkers;
        }

        public Date getRunDate() {
            return runDate;
        }

        public void setRunDate(Date runDate) {
            this.runDate = runDate;
        }

        public String getPlateBarcode() {
            return plateBarcode;
        }

        public void setPlateBarcode(String plateBarcode) {
            this.plateBarcode = plateBarcode;
        }

        public String getInstrumentName() {
            return instrumentName;
        }

        public void setInstrumentName(String instrumentName) {
            this.instrumentName = instrumentName;
        }

        public String getRunName() {
            return runName;
        }

        public void setRunName(String runName) {
            this.runName = runName;
        }

        public List<PlateWellResultMarker> getPlateWellResultMarkers() {
            return plateWellResultMarkers;
        }
    }

    public static class PlateWellResultMarker {

        private String plateBarcode;
        private VesselPosition vesselPosition;
        private BigDecimal result;
        private double lowerMarkerTime;
        private double dv200TotalArea;
        private final boolean nan;

        public PlateWellResultMarker(String plateBarcode,
                                     VesselPosition vesselPosition, BigDecimal result, double lowerMarkerTime,
                                     double dv200TotalArea, boolean nan) {
            this.plateBarcode = plateBarcode;
            this.vesselPosition = vesselPosition;
            this.result = result;
            this.lowerMarkerTime = lowerMarkerTime;
            this.dv200TotalArea = dv200TotalArea;
            this.nan = nan;
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

        public double getLowerMarkerTime() {
            return lowerMarkerTime;
        }

        public double getDv200TotalArea() {
            return dv200TotalArea;
        }

        public boolean isNan() {
            return nan;
        }
    }

    /**
     * Bean representing a Caliper CSV row for RNA results
     */
    public static class CaliperDataRow implements Serializable {
        public static final String DATE_FORMAT = "yyyyMMdd HH:mm:ss";

        private DateFormat sdf;
        private String plateName;
        private String well;
        private String sampleName;
        private String rnaQualityScore;
        private Date date;
        private String dateTimeString;
        private String barcode;
        private String instrumentName;
        private double lowerMarkerTime;
        private double dv200TotalArea;

        public CaliperDataRow() {
            sdf = new SimpleDateFormat(DATE_FORMAT);
        }

        public String getPlateName() {
            return plateName;
        }

        public void setPlateName(String plateName) {
            this.plateName = plateName;
        }

        public String getWell() {
            return well;
        }

        public void setWell(String well) {
            this.well = well;
        }

        public String getSampleName() {
            return sampleName;
        }

        public void setSampleName(String sampleName) {
            this.sampleName = sampleName;
        }

        public String getRnaQualityScore() {
            return rnaQualityScore;
        }

        public void setRnaQualityScore(String rnaQualityScore) {
            this.rnaQualityScore = rnaQualityScore;
        }

        public void setDateTimeString(String dateTimeString) throws ParseException {
            this.dateTimeString = dateTimeString;
            if(dateTimeString != null)
                setDate(sdf.parse(dateTimeString));
        }

        public String getDateTimeString() {
            return dateTimeString;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public String getInstrumentName() {
            return instrumentName;
        }

        public void setInstrumentName(String instrumentName) {
            this.instrumentName = instrumentName;
        }

        public double getLowerMarkerTime() {
            return lowerMarkerTime;
        }

        public void setLowerMarkerTime(double lowerMarkerTime) {
            this.lowerMarkerTime = lowerMarkerTime;
        }

        public double getDv200TotalArea() {
            return dv200TotalArea;
        }

        public void setDv200TotalArea(double dv200TotalArea) {
            this.dv200TotalArea = dv200TotalArea;
        }
    }
}
