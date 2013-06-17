package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for parsing Quant values entered into the system by way of an uploaded spreadsheet.
 */
public class LabMetricProcessor extends TableProcessor {

    private static final long serialVersionUID = 1837219036890134306L;
    private Set<LabMetric> metrics = new HashSet<>();

    private final LabMetric.MetricType metricType;

    private List<String> headersNames;

    private final LabVesselDao vesselDao;

    /**
     * This constructor requires classes to pass in the injected values. We might want to make this injectable, but
     * since it collects state over time (the metrics set) and holds the metric type, it seemed wrong to inject those
     * things in.
     *
     * @param vesselDao The vessel DAO
     * @param metricType The type of metric being pulled in from the spreadsheet.
     */
    public LabMetricProcessor(LabVesselDao vesselDao, LabMetric.MetricType metricType) {
        this.vesselDao = vesselDao;
        this.metricType = metricType;
    }

    @Override
    public void processRow(Map<String, String> dataRow, int dataRowIndex) {
        boolean foundError = false;

        if (hasRequiredValues(dataRow, dataRowIndex)) {

            String barcode = dataRow.get(LabMetricHeaders.BARCODE.getText());

            BigDecimal metric = null;

            try {
                metric = new BigDecimal(dataRow.get(LabMetricHeaders.METRIC.getText()));
            } catch (NumberFormatException e) {
                validationMessages.add("Row #" + dataRow + " value for Quant: " +
                                       dataRow.get(LabMetricHeaders.METRIC.getText()) + " is invalid");
                foundError = true;
            }

            if (!foundError) {
                LabMetric currentMetric = new LabMetric(metric, metricType, LabMetric.LabUnit.UG_PER_ML);
                LabVessel metricVessel = vesselDao.findByIdentifier(barcode);
                if (metricVessel == null) {
                    validationMessages.add("Row #" + (dataRowIndex + 1) + " Vessel not found for " + barcode);
                } else {

                    currentMetric.setLabVessel(metricVessel);
                }

                metrics.add(currentMetric);
            }
        }
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return LabMetricHeaders.values();
    }

    @Override
    public void close() {
    }

    @Override
    public List<String> getHeaderNames() {
        return headersNames;
    }

    /**
     * Since there is only one row in this parser, just take the values and assign them.
     *
     * @param headerValues The header strings that the parser found.
     * @param row The row Which row of header is this.
     */
    @Override
    public void processHeader(List<String> headerValues, int row) {
        headersNames = headerValues;
    }

    public Set<LabMetric> getMetrics() {
        return metrics;
    }

    /**
     * Definition of the headers defined in the Lab Metrics (Quant) upload file
     */
    private enum LabMetricHeaders implements ColumnHeader {
        LOCATION("Location", 0, ColumnHeader.REQUIRED, ColumnHeader.OPTIONAL),
        BARCODE("Barcode", 1, ColumnHeader.REQUIRED, ColumnHeader.REQUIRED),
        METRIC("Quant", 2, ColumnHeader.REQUIRED, ColumnHeader.REQUIRED);

        private final String text;
        private final int index;
        private final boolean requredHeader;
        private final boolean requiredValue;

        private LabMetricHeaders(String text, int index, boolean requiredHeader, boolean requiredValue) {
            this.text = text;
            this.index = index;
            this.requredHeader = requiredHeader;
            this.requiredValue = requiredValue;
        }

        @Override
        public String getText() {
            return this.text;
        }

        @Override
        public int getIndex() {
            return this.index;
        }

        @Override
        public boolean isRequredHeader() {
            return requredHeader;
        }

        @Override
        public boolean isRequiredValue() {
            return requiredValue;
        }
    }
}
