package org.broadinstitute.gpinformatics.mercury.control.vessel;

import com.opencsv.bean.CsvToBeanFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.parsers.csv.CsvParser;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parser for tube barcode/well pairs to output vessel barcode from the BDS600 DBS Punch
 */
public class DBSPuncherFileParser {
    public static final int BARCODE_LENGTH = 12;

    // The DBS puncher does in fact output with extra spaces in some columns
    private static final Map<String, String> COL_TO_FIELD_MAP = new HashMap<String, String>() {
        {
            put("Plate Barcode ", "plateBarcode");
            put("Grid Ref", "well");
            put(" Sample Barcode ", "sampleBarcode");
        }
    };
    private static final int PLATE_BARCODE_COL = 2;
    private static final int WELL_COL = 3;
    private static final int SAMPLE_BARCODE_COL = 4;

    public DBSPuncherRun parseRun(InputStream inputStream, MessageCollection messageCollection) {
        CsvToBeanFilter lineFilter = new CsvToBeanFilter() {
            @Override
            public boolean allowLine(String[] line) {
                String plateBarcode = line[PLATE_BARCODE_COL];
                String well = line[WELL_COL];
                String sampleBarcode = line[SAMPLE_BARCODE_COL];
                return !(plateBarcode == null || plateBarcode.isEmpty() || plateBarcode.equals("unusued") ||
                         well == null || well.isEmpty() || sampleBarcode == null || sampleBarcode.isEmpty());
            }
        };
        List<PuncherDataRow> allRows = CsvParser.parseCsvStreamToBeanByMapping(
                inputStream, ',', PuncherDataRow.class, COL_TO_FIELD_MAP, lineFilter, 1);
        Set<PuncherDataRow> dataSet = new HashSet<>(allRows);
        Map<VesselPosition, String> mapPositionToSample = new HashMap<>();
        Set<String> plateBarcodes = new HashSet<>();
        for (PuncherDataRow row: dataSet) {
            plateBarcodes.add(row.getPlateBarcode());
            VesselPosition vesselPosition = VesselPosition.getByName(row.getWell());
            if (vesselPosition == null) {
                messageCollection.addError("Failed to find position " + row.getWell());
            } else {
                mapPositionToSample.put(vesselPosition, row.getSampleBarcode());
            }
        }
        if (plateBarcodes.size() != 1) {
            messageCollection.addError("Expected only one plate barcode but found: " + plateBarcodes.size());
            return null;
        }
        String plateBarcode = plateBarcodes.iterator().next();
        String formattedBarcode = StringUtils.leftPad(plateBarcode, BARCODE_LENGTH, '0');
        return new DBSPuncherRun(formattedBarcode, mapPositionToSample);
    }

    public class DBSPuncherRun {
        private String plateBarcode;
        private Map<VesselPosition, String> mapPositionToSampleBarcode;

        public DBSPuncherRun(String plateBarcode,
                             Map<VesselPosition, String> mapPositionToSampleBarcode) {
            this.plateBarcode = plateBarcode;
            this.mapPositionToSampleBarcode = mapPositionToSampleBarcode;
        }

        public String getPlateBarcode() {
            return plateBarcode;
        }

        public Map<VesselPosition, String> getMapPositionToSampleBarcode() {
            return mapPositionToSampleBarcode;
        }
    }

    public static class PuncherDataRow {
        private String plateBarcode;
        private String well;
        private String sampleBarcode;

        public PuncherDataRow() {
        }

        public String getPlateBarcode() {
            return plateBarcode;
        }

        public void setPlateBarcode(String plateBarcode) {
            this.plateBarcode = plateBarcode;
        }

        public String getWell() {
            return well;
        }

        public void setWell(String well) {
            this.well = well;
        }

        public String getSampleBarcode() {
            return sampleBarcode;
        }

        public void setSampleBarcode(String sampleBarcode) {
            this.sampleBarcode = sampleBarcode;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(plateBarcode).append(sampleBarcode).append(well).toHashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof PuncherDataRow)) {
                return false;
            }

            PuncherDataRow castOther = (PuncherDataRow) other;
            return new EqualsBuilder()
                    .append(plateBarcode, castOther.getPlateBarcode())
                    .append(sampleBarcode, castOther.getSampleBarcode())
                    .append(well, castOther.getWell()).isEquals();
        }
    }
}
