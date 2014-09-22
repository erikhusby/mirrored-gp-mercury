package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fetches available lab metric and decision data for each page of a lab vessel search.
 */
public class LabVesselMetricPlugin implements ListPlugin {

    public LabVesselMetricPlugin() {}

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

        // Build and append headers for metric data of interest.
        // Each header is associated with applicable column value
        ConfigurableList.Header hdrInitPico =
                new ConfigurableList.Header(LabMetric.MetricType.INITIAL_PICO.getDisplayName(), "", "", "");
        headerGroup.addHeader(hdrInitPico);

        ConfigurableList.Header hdrInitPicoDec =
                new ConfigurableList.Header(LabMetric.MetricType.INITIAL_PICO.getDisplayName() + " Decision", "", "", "");
        headerGroup.addHeader(hdrInitPicoDec);

        ConfigurableList.Header hdrFingPico =
                new ConfigurableList.Header(LabMetric.MetricType.FINGERPRINT_PICO.getDisplayName(), "", "", "");
        headerGroup.addHeader(hdrFingPico);

        ConfigurableList.Header hdrFingPicoDec =
                new ConfigurableList.Header(LabMetric.MetricType.FINGERPRINT_PICO.getDisplayName() + " Decision", "", "", "");
        headerGroup.addHeader(hdrFingPicoDec);

        ConfigurableList.Header hdrShearPico =
                new ConfigurableList.Header(LabMetric.MetricType.SHEARING_PICO.getDisplayName(), "", "", "");
        headerGroup.addHeader(hdrShearPico);

        ConfigurableList.Header hdrShearPicoDec =
                new ConfigurableList.Header(LabMetric.MetricType.SHEARING_PICO.getDisplayName() + " Decision", "", "", "");
        headerGroup.addHeader(hdrShearPicoDec);

        ConfigurableList.Header hdrPondPico =
                new ConfigurableList.Header(LabMetric.MetricType.POND_PICO.getDisplayName(), "", "", "");
        headerGroup.addHeader(hdrPondPico);

        ConfigurableList.Header hdrPondPicoDec =
                new ConfigurableList.Header(LabMetric.MetricType.POND_PICO.getDisplayName() + " Decision", "", "", "");
        headerGroup.addHeader(hdrPondPicoDec);

        ConfigurableList.Header hdrCatchPico =
                new ConfigurableList.Header(LabMetric.MetricType.CATCH_PICO.getDisplayName(), "", "", "");
        headerGroup.addHeader(hdrCatchPico);

        ConfigurableList.Header hdrCatchPicoDec =
                new ConfigurableList.Header(LabMetric.MetricType.CATCH_PICO.getDisplayName() + " Decision", "", "", "");
        headerGroup.addHeader( hdrCatchPicoDec );

        ConfigurableList.Header hdrQpcr =
                new ConfigurableList.Header(LabMetric.MetricType.ECO_QPCR.getDisplayName(), "", "", "");
        headerGroup.addHeader(hdrQpcr);

        ConfigurableList.Header hdrQpcrDec =
                new ConfigurableList.Header(LabMetric.MetricType.ECO_QPCR.getDisplayName() + " Decision", "", "", "");
        headerGroup.addHeader( hdrQpcrDec );

        // Populate rows with any available metrics data.
        for( LabVessel labVessel : labVesselList ) {
            ConfigurableList.Row row = new ConfigurableList.Row( labVessel.getLabel() );

            Map<String, Set<LabMetric>> metricGroups = labVessel.getMetricsForVesselAndDescendants();

            addMetricsToRow(metricGroups, LabMetric.MetricType.INITIAL_PICO, row, hdrInitPico, hdrInitPicoDec );
            addMetricsToRow(metricGroups, LabMetric.MetricType.FINGERPRINT_PICO, row, hdrFingPico, hdrFingPicoDec );
            addMetricsToRow(metricGroups, LabMetric.MetricType.SHEARING_PICO, row, hdrShearPico, hdrShearPicoDec );
            addMetricsToRow(metricGroups, LabMetric.MetricType.POND_PICO, row, hdrPondPico, hdrPondPicoDec );
            addMetricsToRow(metricGroups, LabMetric.MetricType.CATCH_PICO, row, hdrCatchPico, hdrCatchPicoDec );
            addMetricsToRow(metricGroups, LabMetric.MetricType.ECO_QPCR, row, hdrQpcr, hdrQpcrDec );

            metricRows.add(row);
        }

        return metricRows;
    }

    /**
     * Adds latest metric of type specified to the plugin row. Framework quietly ignores empty cells.
     * Uses the last metric with a non-null decision after sorting by date and id.
     * If all metrics have a null decision, use the last after sorting.
     * @param metricGroups Metrics data structure as supplied from LabVessel
     * @param metricType The type of metric to search for
     * @param row Reference to the current plugin row
     * @param valueHeader Maps cell to it's related header
     * @param decisionHeader Maps associated decision cell to it's related header
     *                       @see org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel#getMetricsForVesselAndDescendants()
     */
    private void addMetricsToRow( Map<String, Set<LabMetric>> metricGroups, LabMetric.MetricType metricType
            , ConfigurableList.Row row, ConfigurableList.Header valueHeader, ConfigurableList.Header decisionHeader ) {
        Set<LabMetric> metrics;
        LabMetric metric = null;
        LabMetricDecision decision;
        ConfigurableList.Cell valueCell, decisionCell;
        String value;

        metrics = metricGroups.get( metricType.getDisplayName() );
        if( metrics != null && !metrics.isEmpty() ) {
            // Metrics are sorted by date (and optionally, id)..
            Iterator<LabMetric> iter = metrics.iterator();
            while( iter.hasNext() ) {
                LabMetric latestMetric = iter.next();
                if( latestMetric.getLabMetricDecision() != null ) {
                    // Use latest with a decision
                    metric = latestMetric;
                } else if( metric == null && !iter.hasNext() ) {
                    // We're at last metric and none have a decision, use the last metric
                    metric = latestMetric;
                }
            }
            value = metric.getValue().toPlainString() + " " + metric.getUnits().getDisplayName();
            valueCell = new ConfigurableList.Cell(valueHeader, value, value);
            decision = metric.getLabMetricDecision();
            if (decision != null) {
                decisionCell =
                        new ConfigurableList.Cell(decisionHeader, decision.getDecision().toString(),
                                decision.getDecision().toString());
            } else {
                decisionCell = new ConfigurableList.Cell(decisionHeader, "(None)", "(None)");
            }
            row.addCell(valueCell);
            row.addCell(decisionCell);
        }
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull Map<String, Object> context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in LabVesselMetricPlugin");
    }
}
