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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.parsers.GenericTableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.csv.CsvParser;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parses cell data from a Covid manifest file into Dtos.
 */
public class CovidIntakeParser {
    private static int BARCODE_LENGTH = 10;

    private static Map<String, Metadata.Key> headerMap = new HashMap<String, Metadata.Key>() {{
        // For matching, the name is lowercased and stripped of ' ' and '_'.
        put("barcode",Metadata.Key.BROAD_2D_BARCODE);
        put("matrixtube",Metadata.Key.BROAD_2D_BARCODE);
        put("matrixtubeid",Metadata.Key.BROAD_2D_BARCODE);
        put("tubeid",Metadata.Key.BROAD_2D_BARCODE);
        put("sampleid",Metadata.Key.SAMPLE_ID);
        put("collaboratorsampleid",Metadata.Key.SAMPLE_ID);
        put("patientsampleid",Metadata.Key.SAMPLE_ID);
        put("patientname",Metadata.Key.SAMPLE_ID);
    }};

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
                InputStream inputStream = new ByteArrayInputStream(content);
                PoiSpreadsheetParser.processSingleWorksheet(inputStream, processor);
                cellGrid.addAll(processor.getHeaderAndDataRows().stream().
                        filter(row -> row.size() > 0).
                        collect(Collectors.toList()));
            }
        } catch (Exception e) {
            log.error("Manifest file " + filename + " cannot be parsed. " + e.getMessage());
        }

        if (cellGrid.size() > 1) {
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

            // Matches the spreadsheet's headers with the supported header names trying to
            // accommodate some imprecision in the names (case insensitive, with or without spaces and '_').
            // Unknown headers are added as nulls, to maintain a corresponding index with data rows.
            List<Metadata.Key> sheetHeaders = new ArrayList<>();
            for (String column : cellGrid.get(0)) {
                String header = (column == null) ? "" : column.toLowerCase().replaceAll("_", "").replaceAll(" ", "");
                sheetHeaders.add(headerMap.get(header));
            }

            // Parses the expected data values. Unexpected data is ignored.
            for (List<String> row : cellGrid.subList(1, cellGrid.size())) {
                Dto dto = new Dto();
                for (int columnIndex = 0; columnIndex < sheetHeaders.size(); ++columnIndex) {
                    Metadata.Key metadataKey = sheetHeaders.get(columnIndex);
                    String value = (row.size() > columnIndex) ? row.get(columnIndex) : null;
                    if (metadataKey != null && StringUtils.isNotBlank(value)) {
                        // If the spreadsheet has duplicate headers or headers that map to the
                        // same metadata key, uses the first one encountered (the left-most one on a row).
                        if (StringUtils.isBlank(dto.getLabel()) &&
                                metadataKey == Metadata.Key.BROAD_2D_BARCODE &&
                                NumberUtils.isDigits(value)) {
                            value = StringUtils.leftPad(value, BARCODE_LENGTH, '0');
                            dto.setLabel(value);
                            dto.setSampleName(value);
                        } else if (StringUtils.isBlank(dto.getSampleMetadata().get(metadataKey))) {
                            dto.getSampleMetadata().put(metadataKey, value);
                        }
                    }
                }
                // Keeps only valid dtos.
                if (StringUtils.isNotBlank(dto.getLabel()) && StringUtils.isNotBlank(dto.getSampleName())) {
                    dtos.add(dto);
                }
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

    class Dto {
        private String label;
        private String sampleName;
        private Map<Metadata.Key, String> sampleMetadata = new HashMap<>();

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
