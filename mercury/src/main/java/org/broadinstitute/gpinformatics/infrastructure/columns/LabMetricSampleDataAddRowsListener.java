package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.search.LabMetricSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An Add Rows Listener that loads sample data (from BSP or Mercury)
 */
public class LabMetricSampleDataAddRowsListener implements ConfigurableList.AddRowsListener {

    private Map<String, SampleData> mapSampleIdToData;

    /**
     * If no BSP related columns in requested result columns, then skip the overhead of performing
     * SampleInstancesV2 traversal and related BSP service access
     */
    private boolean shouldFetchFromBsp( SearchContext context ) {
        Set<String> columnNames = new HashSet<>();
        columnNames.addAll(context.getSearchInstance().getPredefinedViewColumns());
        if( context.getSearchInstance().getColumnSetColumnNameList() != null ) {
            columnNames.addAll(context.getSearchInstance().getColumnSetColumnNameList());
        }
        if( context.getSearchInstance().getPredefinedDownloadColumns() != null ) {
            columnNames.addAll(context.getSearchInstance().getPredefinedDownloadColumns());
        }
        return columnNames.contains(LabMetricSearchDefinition.MultiRefTerm.BSP_MATERIAL.getTermRefName())
                || columnNames.contains(LabMetricSearchDefinition.MultiRefTerm.BSP_PARTICIPANT.getTermRefName());
    }

    @Override
    public void addRows(List<?> entityList, SearchContext context, List<ColumnTabulation> nonPluginTabulations) {

        if( !shouldFetchFromBsp(context)) {
            mapSampleIdToData = Collections.EMPTY_MAP;
            return;
        }

        List<LabMetric> labMetrics = (List<LabMetric>) entityList;
        Set<MercurySample> mercurySamples = new HashSet<>();
        for (LabMetric labMetric : labMetrics) {
            for (SampleInstanceV2 sampleInstanceV2 : labMetric.getLabVessel().getSampleInstancesV2()) {
                for (MercurySample mercurySample : sampleInstanceV2.getRootMercurySamples()) {
                    mercurySamples.add(mercurySample);
                }
            }
        }
        SampleDataFetcher sampleDataFetcher = ServiceAccessUtility.getBean(SampleDataFetcher.class);
        mapSampleIdToData = sampleDataFetcher.fetchSampleDataForSamples(mercurySamples,
                BSPSampleSearchColumn.QUANT_UPLOAD_COLUMNS);
    }

    public Map<String, SampleData> getMapSampleIdToData() {
        return mapSampleIdToData;
    }

    @Override
    public void reset() {
        mapSampleIdToData.clear();
    }
}
