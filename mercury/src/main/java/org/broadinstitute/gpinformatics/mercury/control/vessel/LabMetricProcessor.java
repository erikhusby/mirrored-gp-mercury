package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for processing the Quant values entered into the system by way of an uploaded spreadsheet.
 */
public class LabMetricProcessor extends TableProcessor {

    private static final long serialVersionUID = 1837219036890134306L;
    private final Set<LabMetric> metrics = new HashSet<>();

    private final LabMetric.MetricType metricType;

    private List<String> headerNames;

    private final LabVesselDao labVesselDao;
    private final Date metricDate;

    /**
     * This constructor requires classes to pass in the injected values. We might want to make this injectable, but
     * since it collects state over time (the metrics set) and holds the metric type, it seemed wrong to inject those
     * things in.
     *
     * @param labVesselDao The vessel DAO
     * @param metricType The type of metric being pulled in from the spreadsheet.
     */
    public LabMetricProcessor(LabVesselDao labVesselDao, LabMetric.MetricType metricType) {
        super(null);
        this.labVesselDao = labVesselDao;
        this.metricType = metricType;
        this.metricDate = new Date();
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex) {
        String barcode = dataRow.get(LabMetricHeaders.BARCODE.getText());
        String metric = dataRow.get(LabMetricHeaders.METRIC.getText());
        String vesselPosition = dataRow.get(LabMetricHeaders.LOCATION.getText());

        // If the barcode is blank, we just skip the row. This is a valid case since we only want to update
        // rows that have a barcode.
        if (StringUtils.isBlank(barcode) && StringUtils.isBlank(metric)) {
            return;
        }

        // Get the barcode.
        barcode = padBarcode(barcode);

        // Convert to a number.
        try {
            BigDecimal metricDecimal = new BigDecimal(dataRow.get(LabMetricHeaders.METRIC.getText()));
            LabMetric currentMetric = new LabMetric(metricDecimal, metricType, LabMetric.LabUnit.UG_PER_ML,
                    vesselPosition, metricDate);
            LabVessel metricVessel = labVesselDao.findByIdentifier(barcode);
            if (metricVessel == null) {
                addDataMessage("Vessel not found for " + barcode, dataRowIndex);
            } else {

                currentMetric.setLabVessel(metricVessel);
            }

            metrics.add(currentMetric);
        } catch (NumberFormatException e) {
            addDataMessage(
                    "Value for quant: " + dataRow.get(LabMetricHeaders.METRIC.getText()) + " is invalid.",
                    dataRowIndex);
        }
    }

    private static final int SHORT_BARCODE = 10;
    private static final int LONG_BARCODE = 12;

    private static String padBarcode(String inputString) {
        int toLength = (inputString.length() <= SHORT_BARCODE) ? SHORT_BARCODE : LONG_BARCODE;

        while (inputString.length() < toLength) {
            inputString = "0" + inputString;
        }

        return inputString;
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
        return headerNames;
    }

    /**
     * Since there is only one row in this parser, just take the values and assign them.
     *
     * @param headerNames The header strings that the parser found.
     * @param row The row Which row of header is this.
     */
    @Override
    public void processHeader(List<String> headerNames, int row) {
        this.headerNames = headerNames;
    }

    public Set<LabMetric> getMetrics() {
        return metrics;
    }

    /**
     * Definition of the headers defined in the Lab Metrics (Quant) upload file. Barcode MUST be a string but
     * sometimes looks like a numeric.
     */
    private enum LabMetricHeaders implements ColumnHeader {
        LOCATION("Location", 0, ColumnHeader.REQUIRED_HEADER, ColumnHeader.OPTIONAL_VALUE),
        BARCODE("Barcode", 1, ColumnHeader.REQUIRED_HEADER, ColumnHeader.OPTIONAL_VALUE, true),
        METRIC("Quant", 2, ColumnHeader.REQUIRED_HEADER, ColumnHeader.OPTIONAL_VALUE);

        private final String text;
        private final int index;
        private final boolean requredHeader;
        private final boolean requiredValue;
        private boolean isString;

        private LabMetricHeaders(String text, int index, boolean requiredHeader, boolean requiredValue) {
            this(text, index, requiredHeader, requiredValue, false);
        }

        private LabMetricHeaders(String text, int index, boolean requiredHeader, boolean requiredValue,
                                 boolean isString) {
            this.text = text;
            this.index = index;
            this.requredHeader = requiredHeader;
            this.requiredValue = requiredValue;
            this.isString = isString;
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
        public boolean isRequiredHeader() {
            return requredHeader;
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
}
