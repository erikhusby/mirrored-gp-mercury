package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.HeaderValueRow;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExternalLibraryBarcodeUpdate extends ExternalLibraryProcessor {
    private List<String> libraryNames = new ArrayList<>();
    private List<String> barcodes = new ArrayList<>();

    public ExternalLibraryBarcodeUpdate(String sheetName) {
        super(sheetName);
        headerValueNames.clear();
    }

    public enum Headers implements ColumnHeader, ColumnHeader.Ignorable {
        LIBRARY_NAME("Library Name", REQUIRED),
        TUBE_BARCODE("Tube Barcode", REQUIRED),
        ;

        private final String text;
        private boolean required;
        private boolean isString = true;
        private boolean isDate = false;

        Headers(String text, boolean isRequired) {
            this.text = text;
            this.required = isRequired;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean isRequiredHeader() {
            return required;
        }

        @Override
        public boolean isRequiredValue() {
            return required;
        }

        @Override
        public boolean isIgnoredValue() {
            return false;
        }

        @Override
        public boolean isDateColumn() {
            return isDate;
        }

        @Override
        public boolean isStringColumn() {
            return isString;
        }
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber, boolean requiredValuesPresent) {
        libraryNames.add(getFromRow(dataRow, Headers.LIBRARY_NAME));
        barcodes.add(getFromRow(dataRow, Headers.TUBE_BARCODE));
    }

    @Override
    public HeaderValueRow[] getHeaderValueRows() {
        return new HeaderValueRow[0];
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    @Override
    public String adjustHeaderName(String headerCell) {
        return headerCell.toLowerCase().contains("library") ? Headers.LIBRARY_NAME.getText() :
                headerCell.toLowerCase().contains("barcode") ? Headers.TUBE_BARCODE.getText() : headerCell;
    }

    @Override
    public void close() {
    }

    /**
     * Does self-consistency and other validation checks on the data.
     * Entities fetched for the row data are accessed through maps referenced in the dtos.
     */
    @Override
    public void validateAllRows(List<SampleInstanceEjb.RowDto> dtos,  boolean overwrite, MessageCollection messages) {
        if (!dtos.isEmpty() && !overwrite) {
            messages.addError("Set the Overwrite checkbox to update tube barcodes.");
        }
        Set<String> uniqueLibraryNames = new HashSet<>();
        for (SampleInstanceEjb.RowDto dto : dtos) {
            // A library name must only appear in one row.
            if (!uniqueLibraryNames.add(dto.getLibraryName())) {
                messages.addError(String.format(SampleInstanceEjb.DUPLICATE, dto.getRowNumber(), "Library Name"));
            }
            // There must be an existing SampleInstanceEntity.
            if (dto.getSampleInstanceEntity() == null) {
                messages.addError(String.format(SampleInstanceEjb.NONEXISTENT, dto.getRowNumber(),
                        ExternalLibraryBarcodeUpdate.Headers.LIBRARY_NAME.getText(),
                        dto.getLibraryName(), "Mercury"));
            }
            if (getLabVesselMap().get(dto.getBarcode()) != null) {
                messages.addError(String.format(SampleInstanceEjb.ALREADY_EXISTS, dto.getRowNumber(),
                        "tube barcode", dto.getBarcode(), "Mercury"));
            }
        }
    }

    /**
     * Updates the LabVessel barcodes but does not persist them.
     * New/modified entities are added to the dto shared collection getEntitiesToUpdate().
     * @return the empty list. No SampleInstanceEntities are generated but this processor.
     */
    @Override
    public List<SampleInstanceEntity> makeOrUpdateEntities(List<SampleInstanceEjb.RowDto> dtos) {
        for (SampleInstanceEjb.RowDto dto : dtos) {
            // Changes the tube barcode of the SampleInstanceEntity's lab vessel.
            LabVessel labVessel = dto.getSampleInstanceEntity().getLabVessel();
            if (labVessel != null) {
                labVessel.setLabel(dto.getBarcode());
                getEntitiesToPersist().add(labVessel);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getBarcodes() {
        return barcodes;
    }

    @Override
    public List<String> getLibraryNames() {
        return libraryNames;
    }

    @Override
    public boolean supportsSampleKitRequest() {
        return false;
    }
}
