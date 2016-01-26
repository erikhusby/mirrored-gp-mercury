package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A multi-column configurable list plugin that displays ancestor LabMetrics
 */
public class AncestorLabMetricPlugin implements ListPlugin {
    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup,
            @Nonnull SearchContext context) {
        List<LabMetric> labMetricList = (List<LabMetric>) entityList;
        List<ConfigurableList.Row> metricRows = new ArrayList<>();

        List<LabMetric.MetricType> concMetricTypes = new ArrayList<>();
        Map<String, ConfigurableList.Header> mapNameToHeader = new HashMap<>();
        for( LabMetric.MetricType metricType : LabMetric.MetricType.values() ) {
            if (metricType.getCategory() == LabMetric.MetricType.Category.CONCENTRATION) {
                mapNameToHeader.put(metricType.getDisplayName(), new ConfigurableList.Header(metricType.getDisplayName(),
                        metricType.getDisplayName(), ""));
                concMetricTypes.add(metricType);
            }
        }

        for (LabMetric labMetric : labMetricList) {
            Map<LabMetric.MetricType, Set<LabMetric>> mapTypeToMetrics =
                    labMetric.getLabVessel().getMetricsForVesselAndAncestors();
            ConfigurableList.Row row = new ConfigurableList.Row(labMetric.getLabMetricId().toString());
            for (LabMetric.MetricType concMetricType : concMetricTypes) {
                Set<LabMetric> labMetrics = mapTypeToMetrics.get(concMetricType);
                if (labMetrics != null && !labMetrics.isEmpty()) {
                    ConfigurableList.Header header = mapNameToHeader.get(concMetricType.getDisplayName());
                    if (!headerGroup.getHeaderMap().containsKey(concMetricType.getDisplayName())) {
                        headerGroup.addHeader(header);
                    }
                    // todo jmt most recent
                    LabMetric next = labMetrics.iterator().next();
                    BigDecimal value = next.getValue();
                    row.addCell(new ConfigurableList.Cell(header, value, value.toString()));
                }
            }
            metricRows.add(row);
        }
        return metricRows;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
            @Nonnull SearchContext context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in LabVesselMetricPlugin");
    }
}
