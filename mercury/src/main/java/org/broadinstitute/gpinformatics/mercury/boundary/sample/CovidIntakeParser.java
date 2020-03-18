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

package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.parsers.GenericTableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.csv.CsvParser;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses cell data from a Covid manifest file into Dtos.
 */
public class CovidIntakeParser {
    /**
     * Required headers. If others are present they are ignored.
     */
    public enum Header {
        // The order is not important here. For matching, names are lowercased and stripped of ' ' and '_'
        BARCODE("barcode", Metadata.Key.BROAD_2D_BARCODE), // also used for MercurySample name.
        SAMPLE_ID("sampleid", Metadata.Key.SAMPLE_ID),
        ;

        @NotNull
        private final String columnName;
        private final Metadata.Key metadataKey;

        Header(@NotNull String columnName, @Nullable Metadata.Key metadataKey) {
            this.columnName = columnName;
            this.metadataKey = metadataKey;
        }

        public String getText() {
            return columnName;
        }

        @Nullable
        public Metadata.Key getMetadataKey() {
            return metadataKey;
        }
    }

    final private Log log = LogFactory.getLog(getClass());
    final private byte[] content;
    final private String filename;
    final private List<List<String>> cellGrid = new ArrayList<>();
    final private List<Dto> dtos = new ArrayList<>();

    public CovidIntakeParser(byte[] content, String filename) {
        this.content = content;
        this.filename = filename;
    }

    /**
     * Parses the spreadsheet into dtos that have lab vessel label, mercury sample name, and sample metadata.
     */
    public void parse() {
        try {
            if (filename.toLowerCase().endsWith(".csv")) {
                // Parses file as a .csv spreadsheet.
                CsvParser.parseToCellGrid(new ByteArrayInputStream(content)).
                        stream().
                        filter(line -> StringUtils.isNotBlank(StringUtils.join(line))).
                        map(Arrays::asList).
                        forEach(cellGrid::add);
            } else {
                // Parses file as Excel spreadsheet.
                GenericTableProcessor processor = new GenericTableProcessor();
                InputStream inputStream = IOUtils.toInputStream(StringUtils.toEncodedString(content,
                        Charset.defaultCharset()));
                PoiSpreadsheetParser.processSingleWorksheet(inputStream, processor);
                cellGrid.addAll(processor.getHeaderAndDataRows().stream().
                        filter(row -> row.size() > 0).
                        collect(Collectors.toList()));
            }
        } catch(Exception e) {
            log.error("Manifest file " + filename + " cannot be parsed.", e);
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

        // Makes headers from the first row in the cell grid.
        List<Header> sheetHeaders = extractHeaders(cellGrid.get(0));

        // Parses the expected data values. Unexpected data is ignored.
        for (List<String> row : cellGrid.subList(1, cellGrid.size())) {
            Dto dto = new Dto();
            for (int columnIndex = 0; columnIndex < sheetHeaders.size(); ++columnIndex) {
                Header header = sheetHeaders.get(columnIndex);
                String value = (row.size() > columnIndex) ? row.get(columnIndex) : null;
                if (header != null && StringUtils.isNotBlank(value)) {
                    if (header.metadataKey == Metadata.Key.BROAD_2D_BARCODE) {
                        dto.setLabel(value);
                        dto.setSampleName(value);
                    } else if (header.getMetadataKey() != null) {
                        dto.getSampleMetadata().put(header.getMetadataKey(), value);
                    }
                }
            }
        }
    }

    /**
     * Parses the header row.
     */
    private List<Header> extractHeaders(List<String> headerRow) {
        Map<String, Header> nameToHeader = Stream.of(Header.values()).
                collect(Collectors.toMap(Header::getText, Function.identity()));

        List<Header> headers = new ArrayList<>();
        for (String column : headerRow) {
            // Lowercases the header string.
            // Only keeps the substring before '/' (e.g. box id/plate id -> box id)
            // and before parenthesis if they appear.
            // Removes underscores and spaces (e.g. bar_code -> barcode and sample id -> sampleid)
            String header = StringUtils.substringBefore(StringUtils.substringBefore(column.toLowerCase(), "("), "/").
                    replaceAll("_", "").replaceAll(" ", "");
            Header match = nameToHeader.get(header);
            // Unknown headers are added as nulls, to maintain a corresponding index with data rows.
            headers.add(match);
        }
        return headers;
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

    class Dto {
        private String label;
        private String sampleName;
        private Map<Metadata.Key, String> sampleMetadata;

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
    }

    public List<Dto> getDtos() {
        return dtos;
    }
}
