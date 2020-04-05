package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.parsers.csv.CsvParser;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ColorCovidManifestParser {
    private static final int BARCODE_LENGTH = 10;

    private static Map<String, Metadata.Key> headerMap = new HashMap<String, Metadata.Key>() {{
        // For matching, the name is lowercased and stripped of ' ' and '_'.
        put("matrixid", Metadata.Key.BROAD_2D_BARCODE);
        put("sampleid", Metadata.Key.SAMPLE_ID); // this is Collaborator Sample Id
        put("institutionid", Metadata.Key.INSTITUTE_ID);
        put("timecollected", Metadata.Key.COLLECTION_DATE);
        put("tier", Metadata.Key.CDC_TIER);
        put("welllocation", Metadata.Key.WELL_POSITION); // optional
    }};

    private final byte[] content;
    private final String filename;
    private final List<List<String>> cellGrid = new ArrayList<>();
    private final List<Dto> dtos = new ArrayList<>();

    public ColorCovidManifestParser(byte[] content, String filename) {
        this.content = content;
        this.filename = filename;
    }

    /**
     * Parses the spreadsheet into dtos.
     */
    public void parse(MessageCollection messages) {
        try {
            // Parses file as a .csv spreadsheet.
            CsvParser.parseToCellGrid(new ByteArrayInputStream(content)).
                    stream().
                    filter(line -> StringUtils.isNotBlank(StringUtils.join(line))).
                    map(Arrays::asList).
                    forEach(cellGrid::add);
        } catch (Exception e) {
            messages.addError("Manifest file " + filename + " cannot be parsed. " + e.getMessage());
            return;
        }
        if (cellGrid.size() < 2) {
            messages.addError("Manifest file " + filename + " contains no data rows. ");
            return;
        }

        // Cleans up headers and values by removing characters that may present a problem later.
        int maxColumnIndex = -1;
        for (List<String> columns : cellGrid) {
            for (int i = 0; i < columns.size(); ++i) {
                columns.set(i, cleanupValue(columns.get(i)));
                if (StringUtils.isNotBlank(columns.get(i))) {
                    maxColumnIndex = Math.max(maxColumnIndex, i);
                }
            }
        }

        // Matches the spreadsheet's headers with the supported header names while trying to
        // accommodate some imprecision in the names (case insensitive, with or without spaces and '_').
        // Unknown headers are added as nulls, to maintain a corresponding index with data rows.
        List<Metadata.Key> sheetHeaders = new ArrayList<>();
        for (String column : cellGrid.get(0)) {
            String header = (column == null) ? "" : column.toLowerCase().replaceAll("_", "").replaceAll(" ", "");
            sheetHeaders.add(headerMap.get(header));
        }

        // Parses the expected data values. Unexpected data is ignored.
        int rowNumber = 2;
        for (List<String> row : cellGrid.subList(1, cellGrid.size())) {
            Dto dto = new Dto();
            for (int columnIndex = 0; columnIndex < sheetHeaders.size(); ++columnIndex) {
                // Finds the metadata key for this column. If null the column is ignored.
                Metadata.Key metadataKey = sheetHeaders.get(columnIndex);
                if (metadataKey != null) {
                    String value = (row.size() > columnIndex) ? row.get(columnIndex) : null;
                    if (StringUtils.isNotBlank(value)) {
                        // If the spreadsheet has duplicate headers or headers that map to the
                        // same metadata key, uses the first one encountered (the left-most one on a row).
                        // Matrix_id is leading zero padded to 10 digits.
                        if (StringUtils.isBlank(dto.getLabel()) &&
                                metadataKey == Metadata.Key.BROAD_2D_BARCODE &&
                                NumberUtils.isDigits(value)) {
                            value = StringUtils.leftPad(value, BARCODE_LENGTH, '0');
                            dto.setLabel(value);
                            // Mercury sample name/key is equal to the matrix id.
                            dto.setSampleName(value);
                        } else if (StringUtils.isBlank(dto.getSampleMetadata().get(metadataKey))) {
                            dto.getSampleMetadata().put(metadataKey, value);
                        }
                    }
                }
            }
            // Keeps only valid dtos.
            if (dto.isValid()) {
                dtos.add(dto);
            } else {
                messages.addError("Manifest required data is missing at row " + rowNumber);
            }
            ++rowNumber;
        }
        // Well positions must be present on all rows or on none. If present they must have no duplicates.
        List<String> positions = dtos.stream().
                map(dto -> dto.getSampleMetadata().get(Metadata.Key.WELL_POSITION)).
                filter(StringUtils::isNotBlank).
                collect(Collectors.toList());
        if (positions.size() > 0) {
            if (positions.size() != dtos.size()) {
                messages.addError("Some manifest well positions values are missing.");
            }
            if (positions.size() != positions.stream().distinct().count()) {
                messages.addError("Manifest contains duplicate well positions.");
            }
        }
    }

    /**
     * Returns the string stripped of control characters, line breaks, and >7-bit ascii
     * (which become upside-down ? characters in the database).
     */
    private String cleanupValue(String value) {
        String replacementChar = " ";
        return org.apache.commons.codec.binary.StringUtils.newStringUsAscii(
                org.apache.commons.codec.binary.StringUtils.getBytesUsAscii(value)).
                replaceAll("\\?", replacementChar).
                replaceAll("[\\p{C}\\p{Zl}\\p{Zp}]", replacementChar).
                trim();
    }

    public class Dto {
        private String label;
        private String sampleName;
        private Map<Metadata.Key, String> sampleMetadata = new HashMap<>();
        public final static String DTO_DELIMITER = ";;";
        public final static String TOKEN_DELIMITER = ",,";
        public final static String KEY_DELIMITER = "::";

        public Dto() {
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public void setSampleName(String sampleName) {
            this.sampleName = sampleName;
        }

        public String getLabel() {
            return label;
        }

        public String getSampleName() {
            return sampleName;
        }

        public Map<Metadata.Key, String> getSampleMetadata() {
            return sampleMetadata;
        }

        public boolean isValid() {
            return StringUtils.isNotBlank(label) &&
                    StringUtils.isNotBlank(sampleName) &&
                    !sampleMetadata.isEmpty();
        }
    }

    public List<Dto> getDtos() {
        return dtos;
    }

    /** Returns a single string containing all dtos. */
    public String getDtoString() {
        return dtos.stream().map(dto ->
                // order and delimiters must correspond with setDtos(string)
                dto.getLabel() + Dto.TOKEN_DELIMITER + dto.getSampleName() + Dto.TOKEN_DELIMITER +
                        dto.getSampleMetadata().entrySet().stream().
                                map(mapEntry -> mapEntry.getKey() + Dto.KEY_DELIMITER + mapEntry.getValue()).
                                collect(Collectors.joining(Dto.TOKEN_DELIMITER))).
                collect(Collectors.joining(Dto.DTO_DELIMITER));
    }

    /** Returns a list of dtos reconstructed from a single string. */
    public List<Dto> parseDtos(String dtoString, MessageCollection messages) {
        return Stream.of(dtoString.split(Dto.DTO_DELIMITER)).map(dtoToken -> {
            String[] tokens = dtoToken.split(Dto.TOKEN_DELIMITER);
            if (tokens.length < 3) {
                messages.addError("Cannot parse dto from '" + dtoToken + "'.");
            }
            Dto dto = new Dto();
            dto.setLabel(tokens[0]);
            dto.setSampleName(tokens[1]);
            for (int i = 2; i < tokens.length; ++i) {
                if (!tokens[i].contains(Dto.KEY_DELIMITER)) {
                    messages.addError("Cannot parse metadata from '" + dtoString + "'.");
                }
                String[] keyValue = tokens[i].split(Dto.KEY_DELIMITER);
                dto.getSampleMetadata().put(Metadata.Key.valueOf(keyValue[0]), keyValue[1]);
            }
            return dto;
        }).collect(Collectors.toList());
    }
}
