package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.AbstractSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.ColumnHeader;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class LabMetricParser extends AbstractSpreadsheetParser {

    private Set<LabMetric> metrics = new HashSet<LabMetric>();

    private Set<String> validationMessages = new HashSet<String>();

    private LabMetric.MetricType metricType;

    private final LabVesselDao vesselDao;

    @Inject
    public LabMetricParser(LabVesselDao vesselDao) {
        super(LabMetricHeaders.values());
        this.vesselDao = vesselDao;
    }

    public void parseMetrics(InputStream inputStream, LabMetric.MetricType metricType )
            throws IOException, InvalidFormatException, ValidationException {
        this.metricType = metricType;
        processUploadFile(inputStream);
    }

    @Override
    protected void validateAndProcess() throws ValidationException {

        processWorkbook();

        if (CollectionUtils.isEmpty(validationMessages)) {
            throw new ValidationException("Validation Errors were found while processing quant data",
                    validationMessages);
        }

    }

    @Override
    protected void processRow(Row rowValues) {

        boolean foundError = false;

//        String wellLocation = rowValues.getCell(LabMetricHeaders.LOCATION.getIndex()).getStringCellValue();
//        if(StringUtils.isBlank(wellLocation)) {
//            validationMessages.add("Row #" + rowValues.getRowNum() + " value for Location is missing");
//            foundError = true;
//        }

        String barcode = rowValues.getCell(LabMetricHeaders.BARCODE.getIndex()).getStringCellValue();
        if(StringUtils.isBlank(barcode)) {
            validationMessages.add("Row #" + rowValues.getRowNum() + " value for Barcode is missing");
            foundError = true;
        }

        Double metric = null;
        try {
            metric = rowValues.getCell(LabMetricHeaders.METRIC.getIndex()).getNumericCellValue();
        } catch (NumberFormatException e) {
            validationMessages.add("Row #" + rowValues.getRowNum() + " value for Quant value is invalid");
            foundError = true;
        }

        if(!foundError) {
            LabMetric currentMetric = new LabMetric(new BigDecimal(metric), metricType,
                    LabMetric.LabUnit.UG_PER_ML);
            LabVessel metricVessel = vesselDao.findByIdentifier(barcode);
            if(metricVessel == null) {
                validationMessages.add("Row #" + rowValues.getRowNum() + " Vessel not found for "+barcode);
            } else {

                currentMetric.setLabVessel(metricVessel);
            }

            metrics.add(currentMetric);
        }
    }

    private enum LabMetricHeaders implements ColumnHeader {
        LOCATION("Location", 0),
        BARCODE("Barcode", 1),
        METRIC("Quant", 2);

        private final String text;

        private final int index;

        private LabMetricHeaders(String text, int index) {
            this.text = text;
            this.index = index;
        }

        @Override
        public String getText() {
            return this.text;
        }

        @Override
        public int getIndex() {
            return this.index;
        }

    }

    public Set<LabMetric> getMetrics() {
        return metrics;
    }
}
