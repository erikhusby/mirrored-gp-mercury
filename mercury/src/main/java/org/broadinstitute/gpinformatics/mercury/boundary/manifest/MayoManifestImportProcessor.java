/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.parsers.csv.CsvParser;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.enterprise.context.Dependent;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Parses cell data from Mayo manifest files into sample metadata.
 * Cell data is raw spreadsheet cells in string format for every row in every sheet.
 */
@Dependent
public class MayoManifestImportProcessor {
    public static final String CANNOT_PARSE = "Manifest file %s cannot be parsed due to %s.";
    public static final String DUPLICATE_HEADER = "Manifest file %s has duplicate header %s.";
    public static final String DUPLICATE_POSITION = "Manifest file %s has duplicate rack position %s.";
    public static final String DUPLICATE_TUBE = "Manifest file %s has duplicate tube barcode %s.";
    public static final String INCONSISTENT = "Manifest file %s has inconsistent values for %s.";
    public static final String INVALID_DATA = "Manifest file %s has invalid values for %s.";
    public static final String MISSING_DATA = "Manifest file %s is missing required values for %s.";
    public static final String MISSING_HEADER = "Manifest file %s is missing header %s.";
    public static final String NEEDS_HEADER = "Manifest file %s needs a header for every data column.";
    public static final String UNKNOWN_UNITS = "Manifest file %s has unknown units in headers: %s.";
    public static final String UNKNOWN_HEADER = "Manifest file %s has unknown headers: %s.";

    private List<Header> sheetHeaders = new ArrayList<>();
    /**
     * Header attributes.
     * IS_SAMPLE means a non-blank value indicates the row is a sample row.
     * FOR_SAMPLE means a value is required if the row is a sample row.
     */
    private enum Attribute {IS_SAMPLE, FOR_SAMPLE, HAS_UNITS, IS_DATE, IGNORE}

    public enum Header {
        // For initHeaders() to match, names are in lower case, space delimited, no parenthetical part.
        PACKAGE_ID("package id", Metadata.Key.PACKAGE_ID),
        BIOBANK_SAMPLE_ID("biobank id sample id", Metadata.Key.SAMPLE_ID, Attribute.FOR_SAMPLE, Attribute.IS_SAMPLE),
        BOX_ID("box id", Metadata.Key.BOX_ID),
        RACK_BARCODE("box storageunit id", Metadata.Key.RACK_LABEL, Attribute.FOR_SAMPLE),
        WELL_POSITION("well position", Metadata.Key.WELL_POSITION, Attribute.FOR_SAMPLE),
        SAMPLE_ID("sample id", Metadata.Key.COLLAB_SAMPLE_ID2, Attribute.FOR_SAMPLE, Attribute.IS_SAMPLE),
        PARENT_SAMPLE_ID("parent sample id", Metadata.Key.COLLAB_SAMPLE_ID3, Attribute.IS_SAMPLE),
        // The tube barcode is also used as the Broad sample name (equivalent to the SM-id).
        MATRIX_ID("matrix id", Metadata.Key.BROAD_2D_BARCODE, Attribute.FOR_SAMPLE),
        COLLECTION_DATE("collection date", null, Attribute.IGNORE),
        BIOBANK_ID("biobank id", Metadata.Key.PATIENT_ID, Attribute.FOR_SAMPLE, Attribute.IS_SAMPLE),
        SEX("sex at birth", Metadata.Key.GENDER),
        AGE("age", null, Attribute.IGNORE),
        NY_STATE("ny state", Metadata.Key.NY_STATE),
        SAMPLE_TYPE("sample type", Metadata.Key.MATERIAL_TYPE),
        TREATMENTS("treatments", null, Attribute.IGNORE),
        VOLUME("quantity", Metadata.Key.VOLUME, Attribute.HAS_UNITS),
        CONCENTRATION("total concentration", Metadata.Key.CONCENTRATION, Attribute.HAS_UNITS),
        MASS("total dna", Metadata.Key.MASS, Attribute.HAS_UNITS),
        VISIT_DESCRIPTION("visit description", null, Attribute.IGNORE),
        SAMPLE_SOURCE("sample source", Metadata.Key.ORIGINAL_MATERIAL_TYPE),
        STUDY("study", Metadata.Key.STUDY),
        TRACKING_NUMBER("tracking number", Metadata.Key.TRACKING_NUMBER),
        CONTACT("contact", Metadata.Key.CONTACT),
        EMAIL("email", Metadata.Key.CONTACT_EMAIL),
        STUDY_PI("study pi", Metadata.Key.STUDY_PI),
        TEST_NAME("test name", Metadata.Key.PRODUCT_TYPE),
        FAILURE_MODE("failure mode", null, Attribute.IGNORE),
        FAILURE_MODE_DESC("failure mode desc", null, Attribute.IGNORE);

        @NotNull
        private final String columnName;
        private final Metadata.Key metadataKey;
        private final List<Attribute> attributes;
        private BigDecimal factor = BigDecimal.ZERO;

        Header(@NotNull String columnName, @Nullable Metadata.Key metadataKey, Attribute... attributes) {
            this.columnName = columnName;
            this.metadataKey = metadataKey;
            this.attributes = Arrays.asList(attributes);
        }

        public String getText() {
            return columnName;
        }

        @Nullable
        public Metadata.Key getMetadataKey() {
            return metadataKey;
        }

        public boolean indicatesSample () {
            return attributes.contains(Attribute.IS_SAMPLE);
        }

        public boolean isRequiredForSample () {
            return attributes.contains(Attribute.FOR_SAMPLE);
        }

        public boolean isIgnored() {
            return attributes.contains(Attribute.IGNORE);
        }

        public boolean hasUnits() {
            return attributes.contains(Attribute.HAS_UNITS);
        }

        public boolean isDate() {
            return attributes.contains(Attribute.IS_DATE);
        }

        public BigDecimal getFactor() {
            return factor;
        }

        private Pair[] conversionFactors = {
                // Converts weight to nanograms. Ordering is important - first prefix wins.
                Pair.of("g", "1000000000"),
                Pair.of("mg", "1000000"),
                Pair.of("ug", "1000"),
                Pair.of("ng", "1"),
                Pair.of("pg", "0.001"),
                // Converts volume to microliters. Ordering is important - first prefix wins.
                Pair.of("l", "1000000"),
                Pair.of("dl", "100000"),
                Pair.of("ml", "1000"),
                Pair.of("mil", "1000"),
                Pair.of("ul", "1"),
                Pair.of("nl", "0.001"),
                Pair.of("pl", "0.000001"),
        };

        /**
         * Calculates a numeric conversion factor to represent concentration in ng/ul, quantity in ul, mass in ng.
         * @return true if conversion factor was successfully set, false if not.
         */
        private boolean setFactor(String parentheticalPart) {
            // Splits the header suffix into numerator and denominator, or just numerator.
            final String[] parts = parentheticalPart.split("/", 2);
            factor = new BigDecimal(Stream.of(conversionFactors).
                    filter(pair -> parts[0].startsWith((String)pair.getLeft())).
                    map(pair -> (String)pair.getRight()).
                    findFirst().orElse("0"));
            if (parts.length > 1) {
                factor = factor.divide(new BigDecimal(Stream.of(conversionFactors).
                        filter(pair -> parts[1].startsWith((String) pair.getLeft())).
                        map(pair -> (String) pair.getRight()).
                        findFirst().orElse("-1")), BigDecimal.ROUND_UNNECESSARY);
            }
            return factor.signum() > 0;
        }
    }

    /**
     * Returns spreadsheet cell contents as strings. Blank lines are removed.
     */
    @NotNull
    static List<List<String>> parseAsCellGrid(byte[] content, String filename, MessageCollection messages) {
        List<List<String>> cellGrid = new ArrayList<>();
        try {
            // Parses file as a .csv spreadsheet.
            CsvParser.parseToCellGrid(new ByteArrayInputStream(content)).
                    stream().
                    filter(line -> StringUtils.isNotBlank(StringUtils.join(line))).
                    map(Arrays::asList).
                    forEach(cellGrid::add);
        } catch(Exception e) {
            messages.addWarning(CANNOT_PARSE, filename, e.toString());
        }
        return cellGrid;
    }

    /**
     * Makes ManifestRecords from the spreadsheet data which is given as an unstructured cell grid per sheet.
     * One ManifestRecord is made for each spreadsheet data row.
     */
    @NotNull
    List<ManifestRecord> makeManifestRecords(List<List<String>> cellGrid, String filename, MessageCollection messages) {
        List<List<String>> sampleRows = new ArrayList<>();
        List<ManifestRecord> records = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(cellGrid)) {
            int maxColumnIndex = -1;
            // Cleans up headers and values by removing characters that may present a problem later.
            for (List<String> columns : cellGrid) {
                for (int i = 0; i < columns.size(); ++i) {
                    columns.set(i, cleanupValue(columns.get(i)));
                    if (StringUtils.isNotBlank(columns.get(i))) {
                        maxColumnIndex = Math.max(maxColumnIndex, i);
                    }
                }
            }
            // Makes headers from the first row in the cell grid.
            List<String> unknownUnits = new ArrayList<>();
            List<String> unknownHeaders = new ArrayList<>();
            initHeaders(cellGrid.get(0), unknownUnits, unknownHeaders);

            // Every data column must have a header.
            if (sheetHeaders.size() < maxColumnIndex + 1) {
                messages.addError(NEEDS_HEADER, filename);
            }
            // Finds missing required headers. Assumes a spreadsheet will have at least one sample row.
            List<String> missingHeaders = CollectionUtils.subtract(
                    Arrays.stream(Header.values()).
                            filter(header -> header.isRequiredForSample()).
                            collect(Collectors.toList()),
                    sheetHeaders).
                    stream().map(Header::getText).collect(Collectors.toList());
            // Finds duplicate non-ignored headers.
            List<String> duplicateHeaders = CollectionUtils.getCardinalityMap(sheetHeaders.stream().
                    filter(header -> header != null && !header.isIgnored()).collect(Collectors.toList())).
                    entrySet().stream().
                    filter(mapEntry -> mapEntry.getValue() > 1).
                    map(mapEntry -> mapEntry.getKey().getText()).
                    sorted().
                    collect(Collectors.toList());

            Set<String> badValues = new HashSet<>();
            Set<String> missingValues = new HashSet<>();
            Set<String> packageIds = new HashSet<>();
            Set<String> rackAndPosition = new HashSet<>();
            List<String> duplicateRackAndPosition = new ArrayList<>();
            Set<String> tubeBarcodes = new HashSet<>();
            List<String> duplicateTubeBarcodes = new ArrayList<>();
            if (!messages.hasErrors() && missingHeaders.isEmpty() && unknownUnits.isEmpty() &&
                    duplicateHeaders.isEmpty()) {
                // Validates the data before attempting to persist ManifestRecords.
                for (List<String> row : cellGrid.subList(1, cellGrid.size())) {
                    String rackBarcode = null;
                    String position = null;
                    // Scans row for any non-blank sample indicators.
                    boolean isSampleRow = IntStream.range(0, sheetHeaders.size()).
                            anyMatch(columnIndex -> {
                                Header header = sheetHeaders.get(columnIndex);
                                return header != null && header.indicatesSample() &&
                                        row.size() > columnIndex &&
                                        StringUtils.isNotBlank(row.get(columnIndex));
                            });
                    if (isSampleRow) {
                        sampleRows.add(row);
                    }
                    // Validates each column's data.
                    for (int columnIndex = 0; columnIndex < sheetHeaders.size(); ++columnIndex) {
                        Header header = sheetHeaders.get(columnIndex);
                        if (header != null && !header.isIgnored()) {
                            String value = (row.size() > columnIndex) ? row.get(columnIndex) : null;
                            if (StringUtils.isBlank(value)) {
                                if (header.isRequiredForSample() && isSampleRow) {
                                    missingValues.add(header.getText());
                                }
                            } else {
                                if (header.hasUnits()) {
                                    // Strips out any commas if the value is expected to be a number.
                                    value = value.replaceAll(",", "");
                                    if (!NumberUtils.isParsable(value)) {
                                        badValues.add(header.getText());
                                    } else {
                                        row.set(columnIndex, value);
                                    }
                                }
                                if (header == Header.PACKAGE_ID) {
                                    packageIds.add(value);
                                } else if (header == Header.MATRIX_ID) {
                                    if (!tubeBarcodes.add(value)) {
                                        duplicateTubeBarcodes.add(value);
                                    }
                                } else if (header == Header.RACK_BARCODE) {
                                    rackBarcode = value;
                                } else if (header == Header.WELL_POSITION) {
                                    position = value;
                                }
                            }
                        }
                    }
                    if (!rackAndPosition.add(rackBarcode + position)) {
                        duplicateRackAndPosition.add(rackBarcode + " " + position);
                    }
                }
            }
            // Error if the data is unacceptable. Otherwise make it informational.
            boolean dataOk = !messages.hasErrors();
            if (!missingHeaders.isEmpty()) {
                messages.addError(MISSING_HEADER, filename, StringUtils.join(missingHeaders, ", "));
                dataOk = false;
            }
            if (!unknownHeaders.isEmpty()) {
                messages.addInfo(UNKNOWN_HEADER, filename, StringUtils.join(unknownHeaders, ", "));
            }
            if (!unknownUnits.isEmpty()) {
                messages.addError(UNKNOWN_UNITS, filename, StringUtils.join(unknownUnits, ", "));
                dataOk = false;
            }
            if (!duplicateHeaders.isEmpty()) {
                messages.addError(DUPLICATE_HEADER, filename, StringUtils.join(duplicateHeaders, ", "));
                dataOk = false;
            }
            if (!missingValues.isEmpty()) {
                messages.addError(MISSING_DATA, filename, StringUtils.join(missingValues, ", "));
                dataOk = false;
            }
            if (!badValues.isEmpty()) {
                messages.addError(INVALID_DATA, filename, StringUtils.join(badValues, ", "));
                dataOk = false;
            }
            if (packageIds.size() > 1) {
                messages.addError(String.format(INCONSISTENT, filename,
                        packageIds.stream().sorted().collect(Collectors.joining(", "))));
                dataOk = false;
            }
            if (!duplicateTubeBarcodes.isEmpty()) {
                duplicateTubeBarcodes.sort(Comparator.naturalOrder());
                messages.addError(DUPLICATE_TUBE, filename, StringUtils.join(duplicateTubeBarcodes, ", "));
                dataOk = false;
            }
            if (!duplicateRackAndPosition.isEmpty()) {
                duplicateRackAndPosition.sort(Comparator.naturalOrder());
                messages.addError(DUPLICATE_POSITION, filename, StringUtils.join(duplicateRackAndPosition, ", "));
                dataOk = false;
            }
            if (dataOk) {
                // Makes a ManifestRecord for each row having sample data.
                for (List<String> row : sampleRows) {
                    ManifestRecord manifestRecord = new ManifestRecord();
                    records.add(manifestRecord);
                    for (int columnIndex = 0; columnIndex < sheetHeaders.size(); ++columnIndex) {
                        Header header = sheetHeaders.get(columnIndex);
                        if (header != null && !header.isIgnored()) {
                            String value = (row.size() > columnIndex) ? row.get(columnIndex) : null;
                            if (StringUtils.isNotBlank(value)) {
                                if (header.hasUnits()) {
                                    // For values with units, calculates metadata value = (column value) * factor.
                                    value = new BigDecimal(value).multiply(header.getFactor()).
                                            setScale(2, BigDecimal.ROUND_HALF_EVEN).
                                            toPlainString();
                                }
                                manifestRecord.addMetadata(header.getMetadataKey(), value);
                            }
                        }
                    }
                }
            }
        }
        return records;
    }


    private void initHeaders(List<String> headerRow, @Nullable List<String> unknownUnits,
            @Nullable List<String> unknownHeaders) {
        sheetHeaders.clear();
        sheetHeaders.addAll(extractHeaders(headerRow, unknownUnits, unknownHeaders));
    }

    /**
     * Parses the header text, extracts the units if any, and sets the header's conversion factor.
     */
    private List<Header> extractHeaders(List<String> headerRow, @Nullable List<String> unknownUnits,
            @Nullable List<String> unknownHeaders) {

        Map<String, Header> nameToHeader = Stream.of(Header.values()).
                collect(Collectors.toMap(Header::getText, Function.identity()));

        List<Header> headers = new ArrayList<>();
        for (String column : headerRow) {
            // Only keeps the substring before '/' (e.g. box id/plate id -> box id) and before parenthesis.
            String header = StringUtils.substringBefore(StringUtils.substringBefore(column.toLowerCase(), "("), "/").
                    // Removes underscores
                            replaceAll("_", " ").
                    // Puts space between name and id e.g. "sampleId" -> "sample id"
                    // and "biobankid sample id" -> "biobank id sample id"
                            replaceAll("id$", " id").
                            replaceAll("id ", " id ").
                    // Collapse multiple spaces into one and trim.
                            replaceAll("[ ]+", " ").trim();
            Header match = nameToHeader.get(header);
            // Accepts either package storageunit id or box storageunit id.
            if (match == null && header.endsWith("storageunit id")) {
                match = Header.RACK_BARCODE;
            }
            if (match == null) {
                if (unknownHeaders != null) {
                    unknownHeaders.add(column);
                }
            } else {
                if (match.getMetadataKey() == Metadata.Key.CONCENTRATION ||
                        match.getMetadataKey() == Metadata.Key.VOLUME ||
                        match.getMetadataKey() == Metadata.Key.MASS) {
                    String parentheticalPart = StringUtils.substringAfter(column.toLowerCase(), "(").
                            // Removes parentheses and spaces including embedded ones.
                                    replace("(", "").replace(")", "").replace(" ", "");
                    if (parentheticalPart.isEmpty() ||
                            match.getMetadataKey() == Metadata.Key.CONCENTRATION ^ parentheticalPart.contains("/")) {
                        if (unknownUnits != null) {
                            unknownUnits.add(column);
                        }
                    } else if (!match.setFactor(parentheticalPart)) {
                        if (unknownUnits != null) {
                            unknownUnits.add(column);
                        }
                    }
                }
            }
            // An unknown header is still added as a null, to maintain a corresponding index with data rows.
            headers.add(match);
        }
        return headers;
    }

    /**
     * Returns the string stripped of control characters, line breaks, and >7-bit ascii
     * (which become upside-down ? characters in the database).
     */
    private String cleanupValue(String value) {
        return cleanupValue(value, "");
    }

    /** Replaces control characters and non-7-bit ascii characters with the replacementChar. */
    public static String cleanupValue(String value, String replacementChar) {
        return org.apache.commons.codec.binary.StringUtils.newStringUsAscii(
                org.apache.commons.codec.binary.StringUtils.getBytesUsAscii(value)).
                replaceAll("\\?", replacementChar).
                replaceAll("[\\p{C}\\p{Zl}\\p{Zp}]", replacementChar).
                trim();
    }

}
