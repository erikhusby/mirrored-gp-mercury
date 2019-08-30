package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fetches available lab metric and decision data for each page of a lab vessel search.
 */
public class LabVesselMetricPlugin implements ListPlugin {

    public LabVesselMetricPlugin() {}

    // Build headers for metric data of interest.
    private static Map<LabMetric.MetricType, ConfigurableList.Header> QUANT_VALUE_HEADERS = new LinkedHashMap<>();
    private static Map<LabMetric.MetricType, ConfigurableList.Header> QUANT_DECISION_HEADERS = new LinkedHashMap<>();
    static {
        for( LabMetric.MetricType metricType : LabMetric.MetricType.values() ) {
            if(metricType.getCategory() == LabMetric.MetricType.Category.CONCENTRATION ) {
                String headerText;
                // Include measurement default units in header
                headerText = metricType.getDisplayName() + " ng/uL";
                QUANT_VALUE_HEADERS
                        .put(metricType, new ConfigurableList.Header(headerText, headerText, ""));
                headerText = metricType.getDisplayName() + " Decision";
                QUANT_DECISION_HEADERS.put(metricType,
                        new ConfigurableList.Header(headerText, headerText, ""));
            }
        }
    }

    /**
     * Gathers metric data of interest and associates with LabVessel row in search results.
     * @param entityList  List of LabVessel entities for which to return LabMetrics data
     * @param headerGroup List of headers associated with columns selected by user.  This plugin appends column headers
     *                    for LabMetrics and Decisions of interest.
     * @return A list of rows, each corresponding to a LabVessel row in search results.
     */
    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup
            , @Nonnull SearchContext context) {
        List<LabVessel> labVesselList = (List<LabVessel>) entityList;
        List<ConfigurableList.Row> metricRows = new ArrayList<>();

        // Append headers for metric data of interest.
        for(Map.Entry<LabMetric.MetricType, ConfigurableList.Header> valueHeaderEntry: QUANT_VALUE_HEADERS.entrySet() ){
            headerGroup.addHeader(valueHeaderEntry.getValue());
            headerGroup.addHeader(QUANT_DECISION_HEADERS.get(valueHeaderEntry.getKey()));
        }

        // Populate rows with any available metrics data.
        for( LabVessel labVessel : labVesselList ) {
            ConfigurableList.Row row = new ConfigurableList.Row( labVessel.getLabel() );

            Map<LabMetric.MetricType, Set<LabMetric>> metricGroups = labVessel.getMetricsForVesselAndRelatives();
            for(Set<LabMetric> metrics : metricGroups.values()) {
                if( metrics != null && !metrics.isEmpty() ) {
                    addMetricsToRow(metrics, row);
                }
            }

            metricRows.add(row);
        }

        return metricRows;
    }

    /**
     * Adds latest metric of type specified to the plugin row. Framework quietly ignores empty cells.
     *
     * @param metrics Metrics data of a single type as supplied from LabVessel
     * @param row Reference to the current plugin row
     *        @see org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel#getMetricsForVesselAndRelatives()
     */
    private void addMetricsToRow( Set<LabMetric> metrics, ConfigurableList.Row row ) {
        ConfigurableList.Cell valueCell, decisionCell;

        // Bail out if we aren't interested in this metric.
        if (CollectionUtils.isEmpty(metrics) ||
            !QUANT_VALUE_HEADERS.containsKey(metrics.iterator().next().getName())) {
            return;
        }
        LabMetric metric = latestTubeMetric(new ArrayList<>(metrics));
        String value = ColumnValueType.TWO_PLACE_DECIMAL.format( metric.getValue(), "" );
        // Display measurement units if not default
        if( metric.getUnits() != LabMetric.LabUnit.UG_PER_ML && metric.getUnits() != LabMetric.LabUnit.NG_PER_UL ) {
            value += " " + metric.getUnits().getDisplayName();
        }
        valueCell = new ConfigurableList.Cell(QUANT_VALUE_HEADERS.get(metric.getName()), value, value);
        LabMetricDecision decision = metric.getLabMetricDecision();
        if (decision != null) {
            decisionCell =
                    new ConfigurableList.Cell(QUANT_DECISION_HEADERS.get(metric.getName()), decision.getDecision().toString(),
                            decision.getDecision().toString());
        } else {
            decisionCell = new ConfigurableList.Cell(QUANT_DECISION_HEADERS.get(metric.getName()), "(None)", "(None)");
        }

        row.addCell(valueCell);
        row.addCell(decisionCell);
    }

    /**
     * Returns the most recent tube metric, if a tube is present, otherwise the most recent metric.
     * The background is: the average quant is desired, and it is found on the tube even though the
     * raw quant was done on a plate, and the plate metric is dated after the tube metric since pico
     * transfers are done from a rack of tubes to two plates.
     *
     * Includes any re-pico having neither metric run nor decision (a generic upload) (see GPLIM-3991).
     */
    public static LabMetric latestTubeMetric(List<LabMetric> labMetrics) {
        if (CollectionUtils.isNotEmpty(labMetrics)) {
            return labMetrics.stream().
                    filter(VesselMetricDetailsPlugin.sourceMetricPredicate).
                    min((o1, o2) -> {
                        // Puts recent metric before older metric.
                        int dateCompare = o2.getCreatedDate().compareTo(o1.getCreatedDate());
                        return dateCompare == 0 ? o2.getLabMetricId().compareTo(o1.getLabMetricId()) : dateCompare;
                    }).orElse(null);
        } else {
            return null;
        }
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull SearchContext context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in LabVesselMetricPlugin");
    }
}
