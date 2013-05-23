package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
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
import java.util.Map;
import java.util.Set;

/**
 * Responsible of parsing Quant values entered into the system by way of an uploaded spreadsheet.
 */
public class LabMetricParser extends AbstractSpreadsheetParser {

    private Set<LabMetric> metrics = new HashSet<LabMetric>();

    private LabMetric.MetricType metricType;

    private final LabVesselDao vesselDao;

    @Inject
    public LabMetricParser(LabVesselDao vesselDao) {
        super(LabMetricHeaders.values());
        this.vesselDao = vesselDao;
    }

    /**
     *
     * Initial point of contact for the parser.  processUploadFile will begin the process of reading in and parsing out
     * the lab Metrics based on a given metric type.
     *
     *
     * @param inputStream stream representation of a spreadsheet file uploaded by a user
     * @param metricType type of metrics to be parsed
     * @throws IOException
     * @throws InvalidFormatException
     * @throws ValidationException
     */
    public Set<LabMetric> processUploadFile(InputStream inputStream, LabMetric.MetricType metricType)
            throws IOException, InvalidFormatException, ValidationException {
        this.metricType = metricType;
        processUploadFile(inputStream);
        if (!CollectionUtils.isEmpty(validationMessages)) {
            throw new ValidationException("Validation Errors were found while processing quant data",
                    validationMessages);
        }
          return metrics;
    }

    @Override
    protected void parseRows() {

        boolean foundError = false;

        for (Map.Entry<Integer, String[]> rowValues : dataByRow.entrySet()) {

            if (StringUtils.isNotBlank(rowValues.getValue()[LabMetricHeaders.BARCODE.getIndex()]) &&
                StringUtils.isNotBlank(rowValues.getValue()[LabMetricHeaders.METRIC.getIndex()])) {

                String barcode = rowValues.getValue()[LabMetricHeaders.BARCODE.getIndex()];

                if (StringUtils.isBlank(barcode)) {
                    validationMessages.add("Row #" + rowValues.getKey() + " value for Barcode is missing");
                    foundError = true;
                }

                BigDecimal metric = null;
                try {
                    metric = new BigDecimal(rowValues.getValue()[LabMetricHeaders.METRIC.getIndex()]);
                } catch (NumberFormatException e) {
                    validationMessages.add("Row #" + rowValues.getKey() + " value for Quant value is invalid");
                    foundError = true;
                }

                if (!foundError) {
                    LabMetric currentMetric = new LabMetric(metric, metricType, LabMetric.LabUnit.UG_PER_ML);
                    LabVessel metricVessel = vesselDao.findByIdentifier(barcode);
                    if (metricVessel == null) {
                        validationMessages.add("Row #" + rowValues.getKey() + " Vessel not found for " + barcode);
                    } else {

                        currentMetric.setLabVessel(metricVessel);
                    }

                    metrics.add(currentMetric);
                }
            }
        }
    }

    /**
     * Definition of the headers defined in the Lab Metrics (Quant) upload file
     */
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

}
