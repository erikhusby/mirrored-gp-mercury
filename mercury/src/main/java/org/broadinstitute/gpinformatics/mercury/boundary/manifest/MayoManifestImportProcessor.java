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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.parsers.GenericTableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.csv.CsvParser;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoReceivingActionBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.enterprise.context.Dependent;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses cell data from Mayo manifest files into sample metadata.
 * Cell data is raw spreadsheet cells in string format for every row in every sheet.
 */
@Dependent
public class MayoManifestImportProcessor {
    private static final String DETAILS_SHEETNAME = "Details";
    public static final String CANNOT_PARSE = "Manifest file %s cannot be parsed due to %s.";
    public static final String DUPLICATE_HEADER = "Manifest file %s has duplicate header %s.";
    public static final String INVALID_DATA = "Manifest file %s has invalid values for %s.";
    public static final String MISSING_DATA = "Manifest file %s is missing required values for %s.";
    public static final String MISSING_HEADER = "Manifest file %s is missing header %s.";
    public static final String MISSING_SHEET = "Manifest file %s does not have a %s sheet and will not be used.";
    public static final String UNKNOWN_UNITS = "Manifest file %s has unknown units in headers: %s.";
    public static final String UNKNOWN_HEADER = "Manifest file %s has unknown headers: %s.";

    private List<Header> sheetHeaders = new ArrayList<>();

    private enum Attribute {REQUIRED, HAS_UNITS, IS_DATE, IGNORE}

    public enum Header {
        // For initHeaders() to match, defines names in lower case, space delimited, no parenthetical part.
        PACKAGE_ID("package id", Metadata.Key.PACKAGE_ID, Attribute.REQUIRED),
        BIOBANK_SAMPLE_ID("biobank id sample id", Metadata.Key.SAMPLE_ID, Attribute.REQUIRED),
        BOX_ID("box id", Metadata.Key.BOX_ID, Attribute.REQUIRED),
        WELL_POSITION("well position", Metadata.Key.WELL_POSITION, Attribute.REQUIRED),
        SAMPLE_ID("sample id", Metadata.Key.COLLAB_SAMPLE_ID2, Attribute.REQUIRED),
        PARENT_SAMPLE_ID("parent sample id", Metadata.Key.COLLAB_PARTICIPANT_ID2),
        MATRIX_ID("matrix id", Metadata.Key.BROAD_2D_BARCODE, Attribute.REQUIRED),
        COLLECTION_DATE("collection date", null, Attribute.IGNORE),
        BIOBANK_ID("biobank id", Metadata.Key.PATIENT_ID, Attribute.REQUIRED),
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
        REQUESTING_PHYSICIAN("requesting physician", Metadata.Key.REQUESTING_PHYSICIAN),
        TEST_NAME("test name", Metadata.Key.PRODUCT_TYPE);

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

        public boolean isRequired() {
            return attributes.contains(Attribute.REQUIRED);
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
         * Calculates a numeric conversion factor to represent concentration in ng/ul, and quantity in ul.
         */
        private void setFactor(String headerSuffix, @Nullable List<String> unknownUnits) {
            if (StringUtils.isBlank(headerSuffix)) {
                if (unknownUnits != null) {
                    unknownUnits.add(getText());
                }
            } else{
                // Splits the header suffix into numerator and denominator, or just numerator.
                String[] parts = headerSuffix.split("/", 2);
                // The only ratio should be concentration.
                if (getMetadataKey() == Metadata.Key.CONCENTRATION ^ parts.length != 1) {
                    if (unknownUnits != null) {
                        unknownUnits.add(getText());
                    }
                } else {
                    BigDecimal number = null;
                    for (int idx = 0; idx < parts.length; ++idx) {
                        for (Pair<String, String> pair : conversionFactors) {
                            if (parts[idx].startsWith(pair.getLeft())) {
                                number = new BigDecimal(pair.getRight());
                                break;
                            }
                        }
                        if (number == null) {
                            if (unknownUnits != null) {
                                unknownUnits.add(getText());
                            }
                            factor = BigDecimal.ZERO;
                            break;
                        } else if (idx == 0) {
                            factor = number;
                        } else {
                            factor = factor.divide(number, BigDecimal.ROUND_UNNECESSARY);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns spreadsheet cell contents as strings. Blank lines are removed.
     */
    @NotNull
    static List<List<String>> parseAsCellGrid(byte[] content, String filename, MessageCollection messages) {
        List<List<String>> cellGrid = new ArrayList<>();
        try {
            if (isExcel(filename)) {
                // Parses file as an Excel spreadsheet. Uses the "Details" sheet if it exists.
                if (PoiSpreadsheetParser.getWorksheetNames(new ByteArrayInputStream(content)).stream().
                        filter(DETAILS_SHEETNAME::equals).
                        findFirst().isPresent()) {
                    // Makes a cell grid without expecting a specific sheet layout or headers.
                    GenericTableProcessor processor = new GenericTableProcessor();
                    processor.setHeaderNamesSupplier(() -> Collections.emptyList());
                    PoiSpreadsheetParser parser = new PoiSpreadsheetParser(
                            Collections.singletonMap(DETAILS_SHEETNAME, processor));
                    parser.processUploadFile(new ByteArrayInputStream(content));
                    processor.removeBlankRows();
                    cellGrid.addAll(processor.getHeaderAndDataRows());
                } else {
                    messages.addInfo(MISSING_SHEET, filename, DETAILS_SHEETNAME);
                }
            } else {
                // Parses file as a .csv spreadsheet.
                CsvParser.parseToCellGrid(new ByteArrayInputStream(content)).
                        stream().
                        filter(line -> line != null && !StringUtils.isAllBlank(line)).
                        map(Arrays::asList).
                        forEach(cellGrid::add);
            }
        } catch(Exception e) {
            messages.addWarning(CANNOT_PARSE, filename, e.toString());
        }
        return cellGrid;
    }

    /**
     * Makes ManifestRecords from the spreadsheet data which is given as an unstructured cell grid per sheet.
     * One ManifestRecord is made for each spreadsheet data row.
     * Returns a map of manifest key to corresponding ManifestRecords.
     */
    @NotNull
    Multimap<String, ManifestRecord> makeManifestRecords(List<List<String>> cellGrid, String filename,
            MessageCollection messages) {
        Multimap<String, ManifestRecord> records = HashMultimap.create();

        if (CollectionUtils.isNotEmpty(cellGrid)) {
            // Cleans up headers and values by removing characters that may present a problem later.
            for (List<String> columns : cellGrid) {
                for (int i = 0; i < columns.size(); ++i) {
                    columns.set(i, cleanupValue(columns.get(i)));
                }
            }
            // Makes headers from the first row in the cell grid.
            List<String> unknownUnits = new ArrayList<>();
            List<String> unknownHeaders = new ArrayList<>();
            initHeaders(cellGrid.get(0), unknownUnits, unknownHeaders);
            fixupDates(cellGrid.subList(1, cellGrid.size()), filename);

            // Finds missing required headers.
            List<String> missingHeaders = Arrays.asList(Header.values()).stream().
                    filter(header -> header.isRequired() && !sheetHeaders.contains(header)).
                    map(Header::getText).collect(Collectors.toList());
            // Finds duplicate headers.
            List<String> duplicateHeaders = CollectionUtils.getCardinalityMap(
                    sheetHeaders.stream().filter(header -> header != null).collect(Collectors.toList())).
                    entrySet().stream().
                    filter(mapEntry -> mapEntry.getValue() > 1).
                    map(mapEntry -> mapEntry.getKey().getText()).
                    sorted().
                    collect(Collectors.toList());

            Set<String> badValues = new HashSet<>();
            Set<String> missingValues = new HashSet<>();
            if (missingHeaders.isEmpty() && unknownUnits.isEmpty() && unknownHeaders.isEmpty() &&
                    duplicateHeaders.isEmpty()) {
                // Validates the data before attempting to persist ManifestRecords.
                for (List<String> row : cellGrid.subList(1, cellGrid.size())) {
                    for (int columnIndex = 0; columnIndex < sheetHeaders.size(); ++columnIndex) {
                        Header header = sheetHeaders.get(columnIndex);
                        if (header != null && !header.isIgnored()) {
                            String value = (row.size() > columnIndex) ? row.get(columnIndex) : null;
                            if (StringUtils.isNotBlank(value)) {
                                if (header.hasUnits() && !NumberUtils.isParsable(value)) {
                                    badValues.add(header.getText());
                                }
                            } else if (header.isRequired()) {
                                missingValues.add(header.getText());
                            }
                        }
                    }

                }
            }
            if (filename != null && messages != null) {
                if (!missingHeaders.isEmpty()) {
                    messages.addWarning(MISSING_HEADER, filename, StringUtils.join(missingHeaders, ", "));
                }
                if (!unknownHeaders.isEmpty()) {
                    messages.addWarning(UNKNOWN_HEADER, filename, StringUtils.join(unknownHeaders, ", "));
                }
                if (!unknownUnits.isEmpty()) {
                    messages.addWarning(UNKNOWN_UNITS, filename, StringUtils.join(unknownUnits, ", "));
                }
                if (!duplicateHeaders.isEmpty()) {
                    messages.addWarning(DUPLICATE_HEADER, filename, StringUtils.join(duplicateHeaders, ", "));
                }
                if (!missingValues.isEmpty()) {
                    messages.addWarning(MISSING_DATA, filename, StringUtils.join(missingValues, ", "));
                }
                if (!badValues.isEmpty()) {
                    messages.addWarning(INVALID_DATA, filename, StringUtils.join(badValues, ", "));
                }
            }

            if (missingHeaders.isEmpty() && unknownUnits.isEmpty() && unknownHeaders.isEmpty() &&
                    duplicateHeaders.isEmpty() && badValues.isEmpty() && missingValues.isEmpty()) {
                // Makes a ManifestRecord for each row of data.
                for (List<String> row : cellGrid.subList(1, cellGrid.size())) {
                    ManifestRecord manifestRecord = new ManifestRecord();
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
                    String manifestKey = MayoReceivingActionBean.makeManifestKey(sheetHeaders, row);
                    records.put(manifestKey, manifestRecord);
                }
            }
        }
        return records;
    }


    /**
     * Extracts the rack barcodes from the spreadsheet data.
     */
    @NotNull
    Set<String> extractManifestKeys(List<List<String>> cellGrid) {
        Set<String> manifestKeys = new HashSet<>();
        if (CollectionUtils.isNotEmpty(cellGrid)) {
            // Cleanup values, i.e. removes characters that may present a problem later.
            for (List<String> row : cellGrid) {
                for (int i = 0; i < row.size(); ++i) {
                    row.set(i, cleanupValue(row.get(i)));
                }
            }
            // Makes headers from the first row in the cell grid.
            initHeaders(cellGrid.get(0), null, null);
            for (List<String> row : cellGrid.subList(1, cellGrid.size())) {
                String manifestKey = MayoReceivingActionBean.makeManifestKey(sheetHeaders, row);
                if (StringUtils.isNotBlank(manifestKey)) {
                    manifestKeys.add(manifestKey);
                }
            }
        }
        return manifestKeys;
    }

    private void initHeaders(List<String> headerRow, @Nullable List<String> unknownUnits,
            @Nullable List<String> unknownHeaders) {
        sheetHeaders.clear();
        sheetHeaders.addAll(MayoManifestImportProcessor.extractHeaders(headerRow, unknownUnits, unknownHeaders));
    }

    /**
     * Parses the header text, extracts the units if any, and sets the header's conversion factor.
     */
    static List<Header> extractHeaders(List<String> headerRow, @Nullable List<String> unknownUnits,
            @Nullable List<String> unknownHeaders) {

        Map<String, Header> nameToHeader = Stream.of(Header.values()).
                collect(Collectors.toMap(Header::getText, Function.identity()));

        List<Header> headers = new ArrayList<>();
        for (String column : headerRow) {
            String header = column.toLowerCase().replaceAll("_", " ").replace('(', ' ').replace(')', ' ').
                    // Put space between name and id (e.g. sampleId -> sample id)
                    // and between "<name>id " and <name> id (e.g. biobankid sample id -> biobank id sample id)
                            replaceAll("id$", " id").
                            replaceAll("id ", " id ").
                    // Removes spaces around '/' so that if units are present they are always the last single token.
                            replace(" /", "/").replace("/ ", "/").
                    // Collapse multiple spaces into one and trim.
                            replaceAll("[ ]+", " ").trim();
            // Looks for the header name. If no match, try without the last token in case it's the units.
            Header match = nameToHeader.get(header);
            String units = "";
            if (match == null && header.contains(" ")) {
                match = nameToHeader.get(StringUtils.substringBeforeLast(header, " "));
                units = StringUtils.substringAfterLast(header, " ");
            }
            if (match == null && unknownHeaders != null) {
                unknownHeaders.add(column);
            } else if (match != null && match.hasUnits()) {
                match.setFactor(units, unknownUnits);
            }
            // An unknown header is added as a null, to maintain a corresponding index with data rows.
            headers.add(match);
        }
        return headers;
    }

    /**
     * Converts Excel internal date representation into a date string. Updates the value in-place.
     */
    private void fixupDates(List<List<String>> dataRows, String filename) {
        if (isExcel(filename)) {
            for (List<String> columns : dataRows) {
                for (int columnIndex = 0; columnIndex < columns.size(); ++columnIndex) {
                    Header header = sheetHeaders.get(columnIndex);
                    if (header != null) {
                        String value = columns.get(columnIndex);
                        if (header.isDate() && StringUtils.isNotBlank(value) && NumberUtils.isParsable(value)) {
                            // Makes a date string when the column is an Excel internal date representation.
                            columns.set(columnIndex, PoiSpreadsheetParser.convertDoubleStringToDateString(value));
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the string stripped of control characters, line breaks, and >7-bit ascii
     * (which become upside-down ? characters in the database).
     */
    public static String cleanupValue(String value) {
        return org.apache.commons.codec.binary.StringUtils.newStringUsAscii(
                org.apache.commons.codec.binary.StringUtils.getBytesUsAscii(value)).
                replaceAll("\\?","").
                replaceAll("[\\p{C}\\p{Zl}\\p{Zp}]", "").
                trim();
    }

    /**
     * Determines if the file is an Excel spreadsheet.
     */
    private static boolean isExcel(String filename) {
        return filename.endsWith("xls") || filename.endsWith("xlsx");
    }
}
