package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
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
public class SampleVesselProcessor extends TableProcessor {

    private List<String> headers;

    private final Map<String, ParentVesselBean> mapBarcodeToParentVessel = new HashMap<>();
    private Set<String> tubeBarcodes = new HashSet<>();
    private Set<String> sampleIds = new HashSet<>();

    public SampleVesselProcessor(String sheetName) {
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
    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex) {
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
            addDataMessage("Duplicate tube barcode " + tubeBarcode, dataRowIndex);
        }
        if (!sampleIds.add(sampleId)) {
            addDataMessage("Duplicate sample ID " + sampleId, dataRowIndex);
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
        SAMPLE_ID("Sample ID", 0, REQUIRED_HEADER, REQUIRED_VALUE),
        MANUFACTURER_TUBE_BARCODE("Manufacturer Tube Barcode", 1, REQUIRED_HEADER, REQUIRED_VALUE),
        CONTAINER_BARCODE("Container", 2, REQUIRED_HEADER, REQUIRED_VALUE),
        POSITION("Position", 3, REQUIRED_HEADER, REQUIRED_VALUE),
        ;

        private final String text;
        private final int index;
        private final boolean requiredHeader;
        private final boolean requiredValue;
        private final boolean isString;

        Headers(String text, int index, boolean requiredHeader, boolean requiredValue) {
            this(text, index, requiredHeader, requiredValue, true);
        }

        Headers(String text, int index, boolean requiredHeader, boolean requiredValue,
                boolean isString) {
            this.text = text;
            this.index = index;
            this.requiredHeader = requiredHeader;
            this.requiredValue = requiredValue;
            this.isString = isString;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public boolean isRequiredHeader() {
            return requiredHeader;
        }

        @Override
        public boolean isRequiredValue() {
            return requiredValue;
        }

        @Override
        public boolean isDateColumn() {
            return false;
        }

        @Override
        public boolean isStringColumn() {
            return isString;
        }
    }

    public Map<String, ParentVesselBean> getMapBarcodeToParentVessel() {
        return mapBarcodeToParentVessel;
    }

    public Set<String> getTubeBarcodes() {
        return tubeBarcodes;
    }

    public Set<String> getSampleIds() {
        return sampleIds;
    }
}
