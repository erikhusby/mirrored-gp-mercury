package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UniqueMolecularIdentifier;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselTypeGeometry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for a spreadsheet containing plate barcodes of UMI reagent.
 */
public class UniqueMolecularIdentifierReagentProcessor extends TableProcessor {

    private List<String> headers;

    private final Map<String, List<UniqueMolecularIdentifierDto>> mapBarcodeToReagentDto = new LinkedHashMap<>();
    private final Map<UniqueMolecularIdentifierDto, UniqueMolecularIdentifierDto> mapUmiToUmi = new HashMap<>();

    public static class UniqueMolecularIdentifierDto {
        private final UniqueMolecularIdentifier.UMILocation location;
        private final long length;
        private final long spacerLength;
        private final VesselTypeGeometry vesselTypeGeometry;

        public UniqueMolecularIdentifierDto(
                UniqueMolecularIdentifier.UMILocation location, long length, long spacerLength, VesselTypeGeometry vesselTypeGeometry) {
            this.location = location;
            this.length = length;
            this.spacerLength = spacerLength;
            this.vesselTypeGeometry = vesselTypeGeometry;
        }

        public UniqueMolecularIdentifier.UMILocation getLocation() {
            return location;
        }

        public long getLength() {
            return length;
        }

        public long getSpacerLength() {
            return spacerLength;
        }

        public VesselTypeGeometry getVesselTypeGeometry() {
            return vesselTypeGeometry;
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
            if (spacerLength != that.spacerLength) {
                return false;
            }
            if (location != that.location) {
                return false;
            }
            return vesselTypeGeometry.equals(that.vesselTypeGeometry);

        }

        @Override
        public int hashCode() {
            int result = location.hashCode();
            result = 31 * result + (int) (length ^ (length >>> 32));
            result = 31 * result + (int) (spacerLength ^ (spacerLength >>> 32));
            result = 31 * result + vesselTypeGeometry.hashCode();
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
    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex, boolean requiredValuesPresent) {
        String barcode = dataRow.get(Headers.BARCODE.getText());
        String location = dataRow.get(Headers.LOCATION.getText());
        String lengthString = dataRow.get(Headers.LENGTH.getText());
        String spacerLengthString = dataRow.get(Headers.SPACER_LENGTH.getText());
        String vesselType = dataRow.get(Headers.VESSEL_TYPE.getText());
        try {
            VesselTypeGeometry vesselTypeGeometry = StaticPlate.PlateType.getByAutomationName(vesselType);
            String formattedBarcode = StringUtils.leftPad(barcode, 12, '0');
            if (vesselTypeGeometry == null) {
                vesselTypeGeometry =
                        BarcodedTube.BarcodedTubeType.getByAutomationName(vesselType);
                formattedBarcode = StringUtils.leftPad(barcode, 10, '0');
                if (vesselTypeGeometry == null) {
                    addDataMessage("Unknown labware vessel type " + vesselType, dataRowIndex);
                } else if (vesselTypeGeometry !=  BarcodedTube.BarcodedTubeType.MatrixTube075) {
                    addDataMessage(
                            "Vessel Type must be either: MatrixTube075, IndexedAdapterPlate96, or Eppendorf96", dataRowIndex);
                    vesselTypeGeometry = null;
                }
            } else if (vesselTypeGeometry != StaticPlate.PlateType.Eppendorf96 &&
                       vesselTypeGeometry != StaticPlate.PlateType.IndexedAdapterPlate96) {
                addDataMessage(
                        "Vessel Type must be either: MatrixTube075, IndexedAdapterPlate96, or Eppendorf96", dataRowIndex);
                vesselTypeGeometry = null;
            }
            barcode = formattedBarcode;
            if (vesselTypeGeometry != null) {
                UniqueMolecularIdentifier.UMILocation locationType =
                        UniqueMolecularIdentifier.UMILocation.getByName(location);
                int length = Integer.parseInt(lengthString);
                int spacerLength = Integer.parseInt(spacerLengthString);
                if (length <= 0) {
                    addDataMessage("UMI must have a defined length > 0", dataRowIndex);
                }
                if (spacerLength < 0) {
                    addDataMessage("UMI must have a defined spacer length >= 0", dataRowIndex);
                }
                if (locationType != null) {
                    UniqueMolecularIdentifierDto umiKey =
                            new UniqueMolecularIdentifierDto(locationType, length, spacerLength, vesselTypeGeometry);
                    UniqueMolecularIdentifierDto umiValue = mapUmiToUmi.get(umiKey);
                    if (umiValue == null) {
                        mapUmiToUmi.put(umiKey, umiKey);
                        umiValue = umiKey;
                    }
                    if (!mapBarcodeToReagentDto.containsKey(barcode)) {
                        mapBarcodeToReagentDto.put(barcode, new ArrayList<UniqueMolecularIdentifierDto>());
                    } else {
                        for (UniqueMolecularIdentifierDto dto: mapBarcodeToReagentDto.get(barcode)) {
                            if (dto.getLocation() == locationType) {
                                addDataMessage("Barcode has duplicate locations: " + barcode, dataRowIndex);
                                break;
                            }
                        }
                    }
                    mapBarcodeToReagentDto.get(barcode).add(umiValue);
                } else {
                    addDataMessage("Unknown location type " + location, dataRowIndex);
                }
            }
        } catch (NumberFormatException e) {
            addDataMessage("Incorrect data format " + lengthString, dataRowIndex);
        }
    }

    private enum Headers implements ColumnHeader {
        BARCODE("Barcode"),
        LENGTH("UMI Length"),
        SPACER_LENGTH("Spacer Length"),
        VESSEL_TYPE("Vessel Type"),
        LOCATION("Location");

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

        @Override
        public boolean isIgnoreColumn() {
            return false;
        }
    }

    public Map<String, List<UniqueMolecularIdentifierDto>> getMapBarcodeToReagentDto() {
        return mapBarcodeToReagentDto;
    }

    public Map<UniqueMolecularIdentifierDto, UniqueMolecularIdentifierDto> getMapUmiToUmi() {
        return mapUmiToUmi;
    }
}
