package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UMIReagent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for a spreadsheet containing plate barcodes of UMI reagent.
 */
public class UniqueMolecularIdentifierReagentProcessor extends TableProcessor {

    private List<String> headers;

    private final Map<String, UniqueMolecularIdentifierDto> mapBarcodeToReagentDto = new LinkedHashMap<>();
    private final Map<UniqueMolecularIdentifierDto, UniqueMolecularIdentifierDto> mapUmiToUmi = new HashMap<>();

    public static class UniqueMolecularIdentifierDto {
        private final UMIReagent.UMILocation location;
        private final long length;

        public UniqueMolecularIdentifierDto(UMIReagent.UMILocation location, long length) {
            this.location = location;
            this.length = length;
        }

        public UMIReagent.UMILocation getLocation() {
            return location;
        }

        public long getLength() {
            return length;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            UniqueMolecularIdentifierDto that = (UniqueMolecularIdentifierDto) o;

            if (length != that.length) {
                return false;
            }
            return location == that.location;

        }

        @Override
        public int hashCode() {
            int result = location != null ? location.hashCode() : 0;
            result = 31 * result + (int) (length ^ (length >>> 32));
            return result;
        }
    }

    public UniqueMolecularIdentifierReagentProcessor(String sheetName) {
        super(sheetName);
    }

    @Override
    public List<String> getHeaderNames() {
        return headers;
    }

    @Override
    public void processHeader(List<String> headers, int row) {
        this.headers = headers;
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return UniqueMolecularIdentifierReagentProcessor.Headers.values();
    }

    @Override
    public void close() {
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex) {
        String plateBarcode = dataRow.get(Headers.PLATE_BARCODE.getText());
        String location = dataRow.get(Headers.LOCATION.getText());
        String lengthString = dataRow.get(Headers.LENGTH.getText());

        if (mapBarcodeToReagentDto.containsKey(plateBarcode)) {
            addDataMessage("Duplicate plate barcode " + plateBarcode, dataRowIndex);
        }
        try {
            UMIReagent.UMILocation locationType =
                    UMIReagent.UMILocation.getByName(location);
            int length = Integer.parseInt(lengthString);
            if (locationType != null) {
                UniqueMolecularIdentifierDto umiKey =
                        new UniqueMolecularIdentifierDto(locationType, length);
                UniqueMolecularIdentifierDto umiValue = mapUmiToUmi.get(umiKey);
                if (umiValue == null) {
                    mapUmiToUmi.put(umiKey, umiKey);
                    umiValue = umiKey;
                }
                mapBarcodeToReagentDto.put(plateBarcode, umiValue);
            } else {
                addDataMessage("Unknown location type " + location, dataRowIndex);
            }
        } catch (NumberFormatException e) {
            addDataMessage("Incorrect data format " + lengthString, dataRowIndex);
        }
    }

    private enum Headers implements ColumnHeader {
        PLATE_BARCODE("plate barcode"),
        LENGTH("length"),
        LOCATION("location");

        private final String text;

        Headers(String text) {
            this.text = text;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean isRequiredHeader() {
            return true;
        }

        @Override
        public boolean isRequiredValue() {
            return true;
        }

        @Override
        public boolean isDateColumn() {
            return false;
        }

        @Override
        public boolean isStringColumn() {
            return true;
        }
    }

    public Map<String, UniqueMolecularIdentifierDto> getMapBarcodeToReagentDto() {
        return mapBarcodeToReagentDto;
    }

    public Map<UniqueMolecularIdentifierDto, UniqueMolecularIdentifierDto> getMapUmiToUmi() {
        return mapUmiToUmi;
    }
}
