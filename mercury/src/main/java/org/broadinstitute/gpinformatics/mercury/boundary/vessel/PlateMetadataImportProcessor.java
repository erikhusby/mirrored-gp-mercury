package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class PlateMetadataImportProcessor extends TableProcessor {
    private ColumnHeader[] columnHeaders;
    private List<RowMetadata> rowMetadataRecords = new ArrayList<>();
    static final String UNKNOWN_HEADER_FORMAT = "Unknown header(s) '%s'.";
    static final String DUPLICATE_HEADER_FORMAT = "Duplicate header found: %s";
    static final String NO_HEADER_FOUND_FOR_COLUMN = "Duplicate header found: %s";
    static final String NO_DATA_ERROR = "The uploaded metadata has no records.";
    static final String NON_UNIQUE_BARCODES = "More than one plate barcode found in file";
    static final String EMPTY_FILE_ERROR = "The file uploaded was empty.";
    static final String BAD_CONTROL_ERROR = "Expect only 'Positive Control' or 'Negative Control' in Control column";
    static final SimpleDateFormat collectionDateFormat = new SimpleDateFormat("dd-MMM-yyyy"); //10-May-2010

    public PlateMetadataImportProcessor() {
        super(null, IgnoreTrailingBlankLines.YES);
    }

    @Override
    public List<String> getHeaderNames() {
        return Headers.headerNames(columnHeaders);
    }

    @Override
    public void processHeader(List<String> headers, int row) {
        List<String> errors = new ArrayList<>();
        List<String> seenHeaders = new ArrayList<>();
        for (String header : headers) {
            if (!header.isEmpty()) {
                if (seenHeaders.contains(header)) {
                    addDataMessage(String.format(DUPLICATE_HEADER_FORMAT, header), row);
                }
                seenHeaders.add(header);
            }
        }

        Collection<? extends ColumnHeader> foundHeaders =
                Headers.fromColumnName(errors, headers.toArray(new String[headers.size()]));
        columnHeaders = foundHeaders.toArray(new ColumnHeader[foundHeaders.size()]);
        if (!errors.isEmpty()) {
            addDataMessage(String.format(UNKNOWN_HEADER_FORMAT, errors), row);
        }

    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex, boolean requiredValuesPresent) {
        MessageCollection messageCollection = new MessageCollection();
        if (validateRow(dataRow, dataRowIndex)) {
            RowMetadata manifestRecord = Headers.toMetadata(dataRow, messageCollection);
            if (messageCollection.hasErrors()) {
                for (String error : messageCollection.getErrors()) {
                    addDataMessage(error, dataRowIndex);
                }
            }
            rowMetadataRecords.add(manifestRecord);
        }
    }

    private boolean validateRow(Map<String, String> dataRow, int dataRowIndex) {
        boolean valid = true;
        for(Map.Entry<String, String> rowEntry: dataRow.entrySet()) {
            Headers header = Headers.fromColumnName(rowEntry.getKey());
            String val = rowEntry.getValue();
            if (rowEntry.getKey().equalsIgnoreCase(Headers.CONTROL.header)) {
                if (StringUtils.isNotBlank(val)) {
                    if (!val.equalsIgnoreCase("Positive Control") &&
                        !val.equalsIgnoreCase("Negative Control")) {
                        addDataMessage("Invalid format for column: " + header.getText() + " Expect Positive Control, Negative Control, Or the empty string",
                                dataRowIndex);
                        valid = false;
                    }
                }
            }
            if (header.getMetadataKey() == Metadata.Key.CELLS_PER_WELL && !StringUtils.isNumeric(val)) {
                addDataMessage("Invalid format for column: " + header.getText() + " must be a number.", dataRowIndex);
                valid = false;
            }
            if (header.getMetadataKey() == Metadata.Key.COLLECTION_DATE) {
                try {
                    collectionDateFormat.parse(val);
                } catch (ParseException e) {
                    addDataMessage("Invalid format for column: " + header.getText() + " must be a date of format: dd-MMM-yyyy e.g 10-May-2017.", dataRowIndex);
                    valid = false;
                }
            }
            if (header.getText().equals(Headers.PLATE.getText())) {
                if (StringUtils.isBlank(val)) {
                    addDataMessage("Plate Barcode field cannot be empty.", dataRowIndex);
                    valid = false;
                }
            }
            if (header.getText().equals(Headers.WELL.getText())) {
                if (StringUtils.isBlank(val)) {
                    addDataMessage("Well field cannot be empty.", dataRowIndex);
                    valid = false;
                }
            }
            if (header.getText().equals(Headers.COLLABORATOR_PARTICIPANT_ID.getText())) {
                if (StringUtils.isBlank(val)) {
                    addDataMessage("Collaborator Participant ID field cannot be empty.", dataRowIndex);
                    valid = false;
                }
            }
            if (header.getText().equals(Headers.COLLABORATOR_SAMPLE_ID.getText())) {
                if (StringUtils.isBlank(val)) {
                    addDataMessage("Collaborator Sample ID field cannot be empty.", dataRowIndex);
                    valid = false;
                }
            }
            if (header.getText().equals(Headers.CELL_TYPE.getText())) {
                if (StringUtils.isBlank(val)) {
                    addDataMessage("Cell Type field cannot be empty.", dataRowIndex);
                    valid = false;
                }
            }
            if (header.getText().equals(Headers.SPECIES.getText())) {
                if (StringUtils.isBlank(val)) {
                    addDataMessage("Species field cannot be empty.", dataRowIndex);
                    valid = false;
                }
            }
            if (header.getText().equals(Headers.VOLUME.getText())) {
                if (StringUtils.isBlank(val)) {
                    addDataMessage("Volume field cannot be empty.", dataRowIndex);
                    valid = false;
                } else {
                    try {
                        BigDecimal bigDecimal = new BigDecimal(val);
                    } catch (NumberFormatException e) {
                        addDataMessage("Volume field must be a number.", dataRowIndex);
                        valid = false;
                    }
                }
            }
        }
        return valid;
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
        } else if (getMessages().isEmpty() && rowMetadataRecords.isEmpty()) {
            getMessages().add(NO_DATA_ERROR);
        }

        Set<String> plateBarcodes = rowMetadataRecords.stream()
                .map(RowMetadata::getPlateBarcode)
                .collect(Collectors.toSet());
        if (plateBarcodes.size() > 1) {
            getMessages().add(NON_UNIQUE_BARCODES);
        }

        if (!getMessages().isEmpty()) {
            throw new ValidationException("There was an error importing the plate metadata.", getMessages());
        }

        return rowMetadataRecords;
    }

    private enum Headers implements ColumnHeader {
        PLATE("Plate Barcode", null),
        WELL("Well", null),
        CONTROL("Control", null),
        VOLUME("Volume", null),
        CELL_TYPE("Types Of Cell", Metadata.Key.CELL_TYPE),
        SPECIES("Species", Metadata.Key.SPECIES),
        COLLABORATOR_PARTICIPANT_ID("Collaborator Participant ID", Metadata.Key.PATIENT_ID),
        COLLABORATOR_SAMPLE_ID("Collaborator Sample ID", Metadata.Key.SAMPLE_ID),
        COLLECTION_DATE("Collection Date", Metadata.Key.COLLECTION_DATE),
        CELLS_PER_WELL("Cells Per Well", Metadata.Key.CELLS_PER_WELL);

        private final String header;
        private final Metadata.Key metadataKey;

        Headers(String header, Metadata.Key metadataKey) {
            this.header = header;
            this.metadataKey = metadataKey;
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
            return true;
        }

        @Override
        public boolean isRequiredValue() {
            return false;
        }

        @Override
        public boolean isDateColumn() {
            return metadataKey != null && metadataKey.getDataType() == Metadata.DataType.DATE;
        }

        @Override
        public boolean isStringColumn() {
            return metadataKey == null || metadataKey.getDataType() == Metadata.DataType.STRING;
        }

        @Override
        public boolean isIgnoreColumn() {
            return false;
        }

        public Metadata.Key getMetadataKey() {
            return metadataKey;
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

        public static RowMetadata toMetadata(Map<String, String> dataRow, MessageCollection messageCollection) {
            List<Metadata> metadataList = new ArrayList<>(dataRow.size());
            String plateBarcode = null;
            String well = null;
            BigDecimal volume = null;
            Boolean positiveControl = null;
            Boolean negativeControl = null;
            for (Map.Entry<String, String> columnEntry : dataRow.entrySet()) {
                Headers header = Headers.fromColumnName(columnEntry.getKey());
                if (header == Headers.PLATE) {
                    plateBarcode = columnEntry.getValue();
                    plateBarcode = StringUtils.leftPad(plateBarcode, 12, '0');
                } else  if (header == Headers.WELL) {
                    well = columnEntry.getValue();
                } else if (header == Headers.VOLUME) {
                    volume = new BigDecimal(columnEntry.getValue());
                } else if (header == Headers.CONTROL) {
                    if (StringUtils.isNotBlank(columnEntry.getValue())) {
                        if (columnEntry.getValue().trim().equalsIgnoreCase("Positive Control")) {
                            positiveControl = true;
                            metadataList.add(new Metadata(Metadata.Key.POSITIVE_CONTROL, columnEntry.getValue()));
                        } else if (columnEntry.getValue().trim().equalsIgnoreCase("Negative Control")) {
                            negativeControl = true;
                            metadataList.add(new Metadata(Metadata.Key.NEGATIVE_CONTROL, columnEntry.getValue()));
                        } else {
                            messageCollection.addError(BAD_CONTROL_ERROR + columnEntry.getValue());
                        }
                    }
                } else {
                    Metadata metadata = null;
                    switch (header.getMetadataKey().getDataType()) {
                    case STRING:
                        metadata = new Metadata(header.getMetadataKey(), columnEntry.getValue());
                        break;
                    case NUMBER:
                        metadata = new Metadata(header.getMetadataKey(), new BigDecimal(columnEntry.getValue()));
                        break;
                    case DATE:
                        throw new RuntimeException("Date Metadata keys not used in this upload");
                    }
                    metadataList.add(metadata);
                }
            }

            return new RowMetadata(plateBarcode, well, volume, positiveControl, negativeControl, metadataList);
        }
    }
}
