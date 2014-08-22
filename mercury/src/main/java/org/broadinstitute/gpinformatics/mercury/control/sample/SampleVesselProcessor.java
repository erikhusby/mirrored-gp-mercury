package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ChildVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Parses a spreadsheet (from BSP) containing sample IDs and tube barcodes.
 */
public class SampleVesselProcessor extends TableProcessor {

    private List<String> headers;
    private final LabVesselFactory labVesselFactory;
    private final LabVesselDao labVesselDao;
    private final String userName;

    private final List<ParentVesselBean> parentVesselBeans = new ArrayList<>();
    private List<ChildVesselBean> childVesselBeans = new ArrayList<>();
    private List<LabVessel> labVessels;

    public SampleVesselProcessor(String sheetName, LabVesselFactory labVesselFactory, LabVesselDao labVesselDao,
            String userName) {
        super(sheetName);
        this.labVesselFactory = labVesselFactory;
        this.labVesselDao = labVesselDao;
        this.userName = userName;
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
        if (parentVesselBeans.isEmpty()) {
            parentVesselBeans.add(new ParentVesselBean(containerBarcode, null,
                    RackOfTubes.RackType.Matrix96.getDisplayName(), childVesselBeans));
        }
        childVesselBeans.add(new ChildVesselBean(tubeBarcode, sampleId,
                BarcodedTube.BarcodedTubeType.MatrixTube.getDisplayName(), position));
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    @Override
    public void close() {
        // todo jmt validate that tubes and samples don't exist already?
        labVessels = labVesselFactory.buildLabVessels(parentVesselBeans, userName, new Date(), null,
                MercurySample.MetadataSource.MERCURY);
        labVesselDao.persistAll(labVessels);
    }

    private enum Headers implements ColumnHeader {
        SAMPLE_ID("Sample ID", 0, REQUIRED_HEADER, REQUIRED_VALUE),
        MANUFACTURER_TUBE_BARCODE("Manufacturer Tube Barcode", 1, REQUIRED_HEADER, REQUIRED_VALUE),
        CONTAINER_BARCODE("Container", 1, REQUIRED_HEADER, REQUIRED_VALUE),
        POSITION("Position", 1, REQUIRED_HEADER, REQUIRED_VALUE),
        ;

        private final String text;
        private final int index;
        private final boolean requiredHeader;
        private final boolean requiredValue;
        private boolean isString;

        Headers(String text, int index, boolean requiredHeader, boolean requiredValue) {
            this(text, index, requiredHeader, requiredValue, false);
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

    List<ParentVesselBean> getParentVesselBeans() {
        return parentVesselBeans;
    }

    public List<LabVessel> getLabVessels() {
        return labVessels;
    }
}
