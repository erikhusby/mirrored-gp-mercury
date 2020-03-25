package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses a spreadsheet of loose sample vessels (i.e. no parent / child relationship)
 */
public class SampleLooseVesselProcessor extends SampleVesselProcessor {
    private List<String> headers;

    private final List<ParentVesselBean> parentVesselBeans = new ArrayList<>();
    private Set<String> sampleIds = new HashSet<>();
    private Set<String> tubeBarcodes = new HashSet<>();


    public SampleLooseVesselProcessor(String sheetName) {
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
        String vesselType = dataRow.get(Headers.VESSEL_TYPE.getText());
        if (!sampleIds.add(sampleId)) {
            addDataMessage("Duplicate sample ID: " + sampleId, dataRowNumber);
        }
        tubeBarcodes.add(sampleId);
        BarcodedTube.BarcodedTubeType barcodedTubeType = BarcodedTube.BarcodedTubeType.getByDisplayName(vesselType);
        if (barcodedTubeType == null) {
            addDataMessage("Not a valid tube type: " + sampleId, dataRowNumber);
        }
        parentVesselBeans.add(new ParentVesselBean(sampleId, sampleId, vesselType, null));
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
        VESSEL_TYPE("Vessel Type")
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

    @Override
    public List<ParentVesselBean> getParentVesselBeans() {
        return parentVesselBeans;
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
