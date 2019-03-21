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
import org.broadinstitute.gpinformatics.infrastructure.parsers.GenericTableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.csv.CsvParser;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.enterprise.context.Dependent;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses cell data from Mayo manifest files into sample metadata.
 * Cell data is raw spreadsheet cells in string format for every row in every sheet.
 * The spreadsheet should have a sheet for package information, and a sheet for sample detail.
 */
@Dependent
public class MayoManifestImportProcessor {
    public static final String CANNOT_PARSE = "Manifest file %s cannot be parsed due to %s.";
    public static final String DUPLICATE_HEADER = "Manifest file %s has duplicate header %s.";
    public static final String MISSING_DATA = "Manifest file %s is missing a required value for %s.";
    public static final String MISSING_HEADER = "Manifest file %s is missing header %s.";
    public static final String MISSING_SHEET = "Manifest file %s does not have a %s sheet and will not be used.";
    public static final String NOT_NUMBER = "Manifest file %s has a %s value \"%s\" that is not a number.";
    public static final String UNKNOWN_UNITS = "Manifest file %s has header %s that has unknown units \"%s\".";
    public static final String UNKNOWN_HEADER = "Manifest file %s has unknown header \"%s\" which will be ignored.";
    private static final String DETAILS_SHEETNAME = "Details";

    private enum Attribute {REQUIRED, HAS_UNITS, IS_DATE}

    public enum Header {
        BOX_ID("Box ID", Metadata.Key.BOX_ID, Attribute.REQUIRED),             // Mercury rack barcode
        WELL_LOCATION("Well Location", Metadata.Key.WELL, Attribute.REQUIRED), // Mercury vessel position
        SAMPLE_ID("Sample ID", Metadata.Key.SAMPLE_ID, Attribute.REQUIRED),    // MercurySample name
        PARENT_SAMPLE_ID("Parent Sample ID", Metadata.Key.PARENT_SAMPLE_ID),
        MATRIX_ID("Matrix ID", Metadata.Key.MATRIX_ID, Attribute.REQUIRED),    // Mercury tube barcode
        COLLECTION_DATE("Collection Date", Metadata.Key.COLLECTION_DATE, Attribute.IS_DATE),
        BIOBANK_ID("Biobank ID", Metadata.Key.BIOBANK_ID),
        SEX("Sex at birth", Metadata.Key.GENDER),
        SAMPLE_TYPE("Sample Type", Metadata.Key.MATERIAL_TYPE),
        TREATMENTS("Treatments", Metadata.Key.TREATMENTS),
        QUANTITY("Quantity", Metadata.Key.QUANTITY, Attribute.HAS_UNITS),
        CONCENTRATION("Total DNA Concentration", Metadata.Key.CONCENTRATION, Attribute.HAS_UNITS),
        VISIT_DESCRIPTION("Visit Description", Metadata.Key.VISIT_DESCRIPTION),
        SAMPLE_SOURCE("Sample Source", Metadata.Key.ORIGINAL_MATERIAL_TYPE),
        STUDY("Study", Metadata.Key.STUDY),;

        @NotNull
        private final String columnName;
        @NotNull
        private final Metadata.Key metadataKey;
        private final List<Attribute> attributes;
        private BigDecimal factor = BigDecimal.ZERO;

        Header(@NotNull String columnName, @NotNull Metadata.Key metadataKey, Attribute... attributes) {
            this.columnName = columnName;
            this.metadataKey = metadataKey;
            this.attributes = Arrays.asList(attributes);
        }

        public String getText() {
            return columnName;
        }

        @NotNull
        public Metadata.Key getMetadataKey() {
            return metadataKey;
        }

        public boolean isRequired() {
            return attributes.contains(Attribute.REQUIRED);
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

        private Pair[] massConversionFactors = {
                // Converts weight to nanograms. Ordering is important - first prefix wins.
                Pair.of("g", "1000000000"),
                Pair.of("mg", "1000000"),
                Pair.of("millig", "1000000"),
                Pair.of("ug", "1000"),
                Pair.of("microg", "1000"),
                Pair.of("mcg", "1000"),
                Pair.of("ng", "1"),
                Pair.of("nanog", "1"),
                Pair.of("pg", "0.001"),
                Pair.of("picog", "0.001"),
        };
        private Pair[] volumeConversionFactors = {
                // Converts volume to microliters. Ordering is important - first prefix wins.
                Pair.of("l", "1000000"),
                Pair.of("dl", "100000"),
                Pair.of("ml", "1000"),
                Pair.of("mil", "1000"),
                Pair.of("ul", "1"),
                Pair.of("microl", "1"),
                Pair.of("mcl", "1"),
                Pair.of("nl", "0.001"),
                Pair.of("nanol", "0.001"),
                Pair.of("pl", "0.000001"),
                Pair.of("picol", "0.000001"),
        };

        /**
         * Calculates a numeric conversion factor to represent concentration in ng/ul, and quantity in ul.
         */
        private void setFactor(String headerSuffix, String filename, MessageCollection messages) {
            // A factor of 0 or -1 means there was an error parsing the units string.
            factor = BigDecimal.ZERO;
            // Lower case, removes spaces, removes parentheses, splits into numerator and denominator or just numerator.
            String[] parts = StringUtils.remove(headerSuffix, " ").replaceAll("[()]", "").toLowerCase().split("/");
            if (getMetadataKey() == Metadata.Key.CONCENTRATION && parts.length == 2) {
                for (Pair<String, String> pair : massConversionFactors) {
                    if (parts[0].startsWith(pair.getLeft())) {
                        factor = new BigDecimal(pair.getRight());
                        break;
                    }
                }
                BigDecimal denominator = BigDecimal.valueOf(-1);
                for (Pair<String, String> pair : volumeConversionFactors) {
                    if (parts[1].startsWith(pair.getLeft())) {
                        denominator = new BigDecimal(pair.getRight());
                        break;
                    }
                }
                factor = factor.divide(denominator, BigDecimal.ROUND_HALF_EVEN);
            } else if (getMetadataKey() == Metadata.Key.QUANTITY && parts.length == 1 && !parts[0].isEmpty()) {
                for (Pair<String, String> pair : volumeConversionFactors) {
                    if (parts[0].startsWith(pair.getLeft())) {
                        factor = new BigDecimal(pair.getRight());
                        break;
                    }
                }
            }
            if (factor.compareTo(BigDecimal.ZERO) <= 0) {
                messages.addError(UNKNOWN_UNITS, filename, getText(), headerSuffix);
            }
        }
    }

    private Map<String, Header> nameToHeader = new HashMap<>();
    private List<Header> sheetHeaders = new ArrayList<>();

    public MayoManifestImportProcessor() {
        for (Header header : Header.values()) {
            nameToHeader.put(header.getText(), header);
        }
    }

    /**
     * Parses the header text, extracts the units if any, and sets the header's conversion factor.
     */
    public void initHeaders(List<String> headerRow, String filename, @Nullable MessageCollection messages) {
        for (String column : headerRow) {
            boolean found = nameToHeader.containsKey(column);
            if (!found) {
                for (Header header : Header.values()) {
                    // Some columns are expected to have units appended to the header name, possibly in parentheses.
                    if (header.hasUnits() && column.startsWith(header.getText())) {
                        if (messages != null) {
                            header.setFactor(StringUtils.substringAfter(column, header.getText()), filename, messages);
                        }
                        nameToHeader.put(column, header);
                        found = true;
                    }
                }
                if (!found && messages != null) {
                    messages.addInfo(UNKNOWN_HEADER, filename, column);
                }
            }
            sheetHeaders.add(nameToHeader.get(column));
        }
    }

    public void fixupDates(List<List<String>> dataRows, String filename) {
        for (List<String> columns : dataRows) {
            for (int columnIndex = 0; columnIndex < columns.size(); ++columnIndex) {
                Header header = sheetHeaders.get(columnIndex);
                if (header != null) {
                    String value = columns.get(columnIndex);
                    if (header.isDate() && StringUtils.isNotBlank(value) && NumberUtils.isParsable(value) &&
                            isExcel(filename)) {
                        // Makes a date string when the column is an Excel internal date representation.
                        columns.set(columnIndex, PoiSpreadsheetParser.convertDoubleStringToDateString(value));
                    }
                }
            }
        }
    }

    public boolean isDateColumn(int columnIndex) {
        return sheetHeaders.size() > columnIndex &&
                sheetHeaders.get(columnIndex) != null &&
                sheetHeaders.get(columnIndex).isDate();
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
                        filter(sheetname -> DETAILS_SHEETNAME.equals(sheetname)).
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
                for (String[] line : CsvParser.parseToCellGrid(new ByteArrayInputStream(content))) {
                    if (line != null && !StringUtils.isAllBlank(line)) {
                        cellGrid.add(Arrays.asList(line));
                    }
                }
            }
        } catch(Exception e) {
            messages.addError(CANNOT_PARSE, filename, e.toString());
        }
        return cellGrid;
    }

    /**
     * Makes ManifestRecords from the spreadsheet data which is given as an unstructured cell grid per sheet.
     *
     * @param cellGrid is a list of rows, each of which is a list of string cell values.
     *                 The parser should have already stripped out completely blank rows.
     * @param messages passes back to caller any error, warning, and info messages.
     * @return the ManifestRecords, one per data row in the spreadsheet.
     */
    @NotNull
    List<ManifestRecord> makeManifestRecords(@NotNull List<List<String>> cellGrid, String filename,
            MessageCollection messages) {
        List<ManifestRecord> records = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(cellGrid)) {
            for (List<String> columns : cellGrid) {
                for (int i = 0; i < columns.size(); ++i) {
                    columns.set(i, cleanupValue(columns.get(i)));
                }
            }
            // Makes headers from the first row in the cell grid.
            initHeaders(cellGrid.get(0), filename, messages);
            fixupDates(cellGrid.subList(1, cellGrid.size()), filename);
            // Error if a required header is missing from the spreadsheet.
            Arrays.asList(Header.values()).stream().
                    filter(header -> header.isRequired() && !sheetHeaders.contains(header)).
                    map(Header::getText).
                    forEach(headerName -> messages.addError(MISSING_HEADER, filename, headerName));
            // Error if a spreadsheet header appears twice.
            CollectionUtils.getCardinalityMap(sheetHeaders).entrySet().stream().
                    filter(mapEntry -> mapEntry.getValue() > 1).
                    forEachOrdered(mapEntry ->
                            messages.addError(DUPLICATE_HEADER, filename, mapEntry.getKey().getText()));

            if (!messages.hasErrors()) {
                // Makes a ManifestRecord for each row of data.
                for (List<String> columns : cellGrid.subList(1, cellGrid.size())) {
                    ManifestRecord manifestRecord = new ManifestRecord();
                    for (int columnIndex = 0; columnIndex < columns.size(); ++columnIndex) {
                        Header header = sheetHeaders.get(columnIndex);
                        if (header != null) {
                            String value = columns.get(columnIndex);
                            if (StringUtils.isNotBlank(value)) {
                                if (header.hasUnits()) {
                                    // For values with units, calculates metadata value = (column value) * factor.
                                    if (NumberUtils.isParsable(value)) {
                                        value = new BigDecimal(value).
                                                multiply(header.getFactor()).
                                                setScale(2, BigDecimal.ROUND_HALF_EVEN).
                                                toPlainString();
                                    } else {
                                        messages.addError(NOT_NUMBER, filename, header.getText(), value);
                                        value = "";
                                    }
                                }
                                manifestRecord.addMetadata(header.getMetadataKey(), value);

                            } else if (header.isRequired()) {
                                messages.addError(MISSING_DATA, filename, header.getText());
                            }
                        }
                    }
                    records.add(manifestRecord);
                }
            }
        }
        return records;
    }

    /**
     * Returns the string stripped of control characters, line breaks, and >7-bit ascii
     * (which become "¿" characters in the database).
     */
    private static String cleanupValue(String value) {
        return org.apache.commons.codec.binary.StringUtils.newStringUsAscii(
                org.apache.commons.codec.binary.StringUtils.getBytesUsAscii(value)).
                replaceAll("\\?","").
                replaceAll("[\\p{C}\\p{Zl}\\p{Zp}]", "").
                trim();
    }

    /**
     * Determines if the file is an Excel spreadsheet.
     */
    public static boolean isExcel(String filename) {
        return filename.endsWith("xls") || filename.endsWith("xlsx");
    }
}
