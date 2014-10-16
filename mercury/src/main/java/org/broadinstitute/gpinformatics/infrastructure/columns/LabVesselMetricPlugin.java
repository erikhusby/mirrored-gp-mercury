package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
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
                String headerText = metricType.getDisplayName();
                QUANT_VALUE_HEADERS
                        .put(metricType, new ConfigurableList.Header(headerText, headerText, "", ""));
                headerText += " Decision";
                QUANT_DECISION_HEADERS.put(metricType,
                        new ConfigurableList.Header(headerText, headerText, "", ""));
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
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup) {
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

            Map<String, Set<LabMetric>> metricGroups = labVessel.getMetricsForVesselAndDescendants();
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
     * Uses the last metric with a non-null decision after sorting by date and id due to duplicate Pico entries
     *   for descendant lab vessels:  Transfers are done from a rack of tubes to two plates.
     *   The raw values are stored on the plate wells, with no decision, and the average values are stored on the tube,
     *   with a decision.  If all metrics have a null decision, use the last metric after sorting.
     * @param metrics Metrics data of a single type as supplied from LabVessel
     * @param row Reference to the current plugin row
     *        @see org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel#getMetricsForVesselAndDescendants()
     */
    private void addMetricsToRow( Set<LabMetric> metrics, ConfigurableList.Row row ) {
        LabMetric metric = null;
        LabMetricDecision decision;
        ConfigurableList.Cell valueCell, decisionCell;
        String value;

        // Metrics are sorted by date (and optionally, id)..
        Iterator<LabMetric> iter = metrics.iterator();
        while( iter.hasNext() ) {
            LabMetric latestMetric = iter.next();

            // Bail out if we aren't interested in this metric.
            if( !QUANT_VALUE_HEADERS.containsKey(latestMetric.getName()) ) {
                return;
            }

            if( latestMetric.getLabMetricDecision() != null ) {
                // Use latest with a decision
                metric = latestMetric;
            } else if( metric == null && !iter.hasNext() ) {
                // We're at last metric and none have a decision, use the last metric
                metric = latestMetric;
            }
        }
        value = MathUtils.scaleTwoDecimalPlaces(metric.getValue()).toPlainString() + " " + metric.getUnits().getDisplayName();
        valueCell = new ConfigurableList.Cell(QUANT_VALUE_HEADERS.get(metric.getName()), value, value);
        decision = metric.getLabMetricDecision();
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

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull Map<String, Object> context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in LabVesselMetricPlugin");
    }
}
