package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public class PlateMetadataImportProcessor extends TableProcessor {
    private ColumnHeader[] columnHeaders;
    private List<RowMetadata> rowMetadataRecords = new ArrayList<>();
    static final String UNKNOWN_HEADER_FORMAT = "Unknown header(s) '%s'.";
    static final String DUPLICATE_HEADER_FORMAT = "Duplicate header found: %s";
    static final String NO_HEADER_FOUND_FOR_COLUMN = "Duplicate header found: %s";
    static final String NO_DATA_ERROR = "The uploaded metadata has no records.";
    static final String EMPTY_FILE_ERROR = "The file uploaded was empty.";

    protected PlateMetadataImportProcessor() {
        super(null);
    }

    @Override
    public List<String> getHeaderNames() {
        return Headers.headerNames();
    }

    @Override
    public int getNumHeaderRows() {
        return 2;
    }

    @Override
    public void processHeader(List<String> headers, int row) {
        List<String> errors = new ArrayList<>();
        List<String> seenHeaders = new ArrayList<>();
        for (String header : headers) {
            if (seenHeaders.contains(header) && !header.equalsIgnoreCase("Alias")) {
                addDataMessage(String.format(DUPLICATE_HEADER_FORMAT, header), row);
            }
            seenHeaders.add(header);
        }

        Collection<? extends ColumnHeader> foundHeaders =
                Headers.fromColumnName(errors, headers.toArray(new String[headers.size()]));
        columnHeaders = foundHeaders.toArray(new ColumnHeader[foundHeaders.size()]);
        if (!errors.isEmpty()) {
            addDataMessage(String.format(UNKNOWN_HEADER_FORMAT, errors), row);
        }

    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex) {
        validateRow(dataRow, dataRowIndex);
        RowMetadata manifestRecord = Headers.toMetadata(dataRow);
        rowMetadataRecords.add(manifestRecord);
    }

    private void validateRow(Map<String, String> dataRow, int dataRowIndex) {
        //TODO Validate Plate and such, true/false perhaps, numbers on cell type
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    @Override
    public void close() {

    }

    public List<RowMetadata> getRowMetadataRecords() throws ValidationException {
        if (columnHeaders == null && rowMetadataRecords.isEmpty()) {
            getMessages().add(EMPTY_FILE_ERROR);
        } else if (rowMetadataRecords.isEmpty()) {
            getMessages().add(NO_DATA_ERROR);
        }
        if (!getMessages().isEmpty()) {
            throw new ValidationException("There was an error importing the plate metadata.", getMessages());
        }
        return rowMetadataRecords;
    }

    //TODO DOn't have the stupid collected after thing yet
    private enum Headers implements ColumnHeader {
        PLATE("Plate Barcode", null),
        WELL("Well", null),
        SAMPLE("Sample ID", Metadata.Key.SAMPLE_ID),
        CELL_TYPE("Types of cell", Metadata.Key.CELL_TYPE),
        SPECIES("Species", Metadata.Key.SPECIES),
        POSITIVE_CONTROL("positive control?", Metadata.Key.CELL_TYPE, DataType.BOOLEAN),
        NEGATIVE_CONTROL("negative control?", Metadata.Key.CELL_TYPE, DataType.BOOLEAN),
        COLLABORATOR_PARTICIPANT_ID("Alias", "Collaborator Participant ID", Metadata.Key.COLLABORATOR_PARTICIPANT_ID,
                DataType.STRING),
        COLLABORATOR_SAMPLE_ID("Alias", "Collaborator Sample ID", Metadata.Key.COLLABORATOR_SAMPLE_ID, DataType.STRING),
        CELLS_PER_WELL("Cells per well", Metadata.Key.CELLS_PER_WELL, DataType.NUMBER);

        private final String header;
        private final String subHeader;
        private final DataType dataType;
        private final Metadata.Key metadataKey;

        Headers(String text, Metadata.Key metadataKey) {
            this (text, metadataKey, DataType.STRING);
        }

        Headers(String text, Metadata.Key metadataKey, DataType dataType) {
            this(text, null, metadataKey, dataType);
        }

        Headers(String header, String subHeader, Metadata.Key metadataKey, DataType dataType) {

            this.header = header;
            this.subHeader = subHeader;
            this.metadataKey = metadataKey;
            this.dataType = dataType;
        }

        public static List<String> headerNames(ColumnHeader... columnHeaders) {
            List<String> allHeaderNames = new ArrayList<>(columnHeaders.length);
            for (ColumnHeader manifestHeader : columnHeaders) {
                allHeaderNames.add(manifestHeader.getText());
            }
            return allHeaderNames;
        }

        @Override
        public String getText() {
            return header;
        }

        @Override
        public boolean isRequiredHeader() {
            return metadataKey != null;
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
            return dataType == DataType.STRING;
        }

        public boolean isBooleanColumn() {
            return dataType == DataType.BOOLEAN;
        }

        public boolean isNumberColumn() {
            return dataType == DataType.NUMBER;
        }

        public Metadata.Key getMetadataKey() {
            return metadataKey;
        }

        public DataType getDataType() {
            return dataType;
        }

        static Collection<Headers> fromColumnName(List<String> errors, String... columnNames) {
            List<Headers> matches = new ArrayList<>();
            for (String columnName : columnNames) {
                try {
                    matches.add(fromColumnName(columnName));
                } catch (IllegalArgumentException e) {

                    // If a header cell is not blank.
                    if (!columnName.isEmpty()) {
                        errors.add(columnName);
                    }
                }
            }
            return matches;
        }

        public static Headers fromColumnName(String columnHeader) {
            for (Headers header : Headers.values()) {
                if (header.getText().equals(columnHeader)) {
                    return header;
                }
            }
            throw new IllegalArgumentException(NO_HEADER_FOUND_FOR_COLUMN + columnHeader);
        }

        public static RowMetadata toMetadata(Map<String, String> dataRow) {
            List<Metadata> metadataList = new ArrayList<>(dataRow.size());
            String plateBarcode = null;
            String well = null;
            for (Map.Entry<String, String> columnEntry : dataRow.entrySet()) {
                Headers header = Headers.fromColumnName(columnEntry.getKey());
                if (header == Headers.PLATE) {
                    plateBarcode = columnEntry.getValue();
                } else  if (header == Headers.WELL) {
                    well = columnEntry.getValue();
                } else {
                    Metadata metadata = null;
                    switch (header.getMetadataKey().getDataType()) {
                    case STRING:
                        metadata = new Metadata(header.getMetadataKey(), columnEntry.getValue());
                        break;
                    case NUMBER:
                        metadata = new Metadata(header.getMetadataKey(), new BigDecimal(columnEntry.getValue()));
                        break;
                    case BOOLEAN:
                        metadata = new Metadata(header.getMetadataKey(), Boolean.valueOf(columnEntry.getValue()));
                        break;
                    case DATE:
                        throw new RuntimeException("Date Metadata keys not used in this upload");
                    }
                    metadataList.add(metadata);
                }
            }

            return new RowMetadata(plateBarcode, well, metadataList);
        }
    }

    public static class RowMetadata {
        private final String well;
        private String plateBarcode;
        private List<Metadata> metadata;

        public RowMetadata(String plateBarcode, String well, List<Metadata> metadata) {
            this.plateBarcode = plateBarcode;
            this.well = well;
            this.metadata = metadata;
        }

        public String getPlateBarcode() {
            return plateBarcode;
        }

        public String getWell() {
            return well;
        }

        public List<Metadata> getMetadata() {
            return metadata;
        }
    }

    public enum DataType {
        STRING,
        NUMBER,
        BOOLEAN
    }
}
