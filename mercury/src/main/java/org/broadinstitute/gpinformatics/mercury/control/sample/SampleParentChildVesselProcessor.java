package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ChildVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses a spreadsheet (from BSP) containing sample IDs and tube barcodes.
 */
public class SampleParentChildVesselProcessor extends SampleVesselProcessor {

    private List<String> headers;

    private final Map<String, ParentVesselBean> mapBarcodeToParentVessel = new HashMap<>();
    private Set<String> tubeBarcodes = new HashSet<>();
    private Set<String> sampleIds = new HashSet<>();

    public SampleParentChildVesselProcessor(String sheetName) {
        super(sheetName, IgnoreTrailingBlankLines.YES);
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
    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber, boolean requiredValuesPresent) {
        String sampleId = dataRow.get(Headers.SAMPLE_ID.getText());
        String tubeBarcode = dataRow.get(Headers.MANUFACTURER_TUBE_BARCODE.getText());
        String containerBarcode = dataRow.get(Headers.CONTAINER_BARCODE.getText());
        String position = dataRow.get(Headers.POSITION.getText());
        ParentVesselBean parentVesselBean = mapBarcodeToParentVessel.get(containerBarcode);
        if (parentVesselBean == null) {
            parentVesselBean = new ParentVesselBean(containerBarcode, null,
                    RackOfTubes.RackType.Matrix96.getDisplayName(), new ArrayList<ChildVesselBean>());
            mapBarcodeToParentVessel.put(containerBarcode, parentVesselBean);
        }
        if (!tubeBarcodes.add(tubeBarcode)) {
            addDataMessage("Duplicate tube barcode " + tubeBarcode, dataRowNumber);
        }
        if (!sampleIds.add(sampleId)) {
            addDataMessage("Duplicate sample ID " + sampleId, dataRowNumber);
        }
        parentVesselBean.getChildVesselBeans().add(new ChildVesselBean(tubeBarcode, sampleId,
                BarcodedTube.BarcodedTubeType.MatrixTube.getDisplayName(), position));
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    @Override
    public void close() {
    }

    private enum Headers implements ColumnHeader {
        SAMPLE_ID("Sample ID"),
        MANUFACTURER_TUBE_BARCODE("Manufacturer Tube Barcode"),
        CONTAINER_BARCODE("Container"),
        POSITION("Position"),
        ;

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
            return false;
        }

        @Override
        public boolean isIgnoreColumn() {
            return false;
        }
    }

    public Map<String, ParentVesselBean> getMapBarcodeToParentVessel() {
        return mapBarcodeToParentVessel;
    }

    @Override
    public List<ParentVesselBean> getParentVesselBeans() {
        return new ArrayList<>(getMapBarcodeToParentVessel().values());
    }

    @Override
    public Set<String> getTubeBarcodes() {
        return tubeBarcodes;
    }

    @Override
    public Set<String> getSampleIds() {
        return sampleIds;
    }
}
