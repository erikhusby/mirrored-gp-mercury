package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.analytics.DemultiplexSampleDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.DemultiplexSampleMetric;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Fetches available sequencing metric for each page of a lab vessel search.
 */
public class SampleMetadataSequencingMetricPlugin implements ListPlugin {

    public SampleMetadataSequencingMetricPlugin() {
    }

    private enum VALUE_COLUMN_TYPE {
        RUN_NAME("Run Name"),
        RUN_DATE("Run Date"),
        FLOWCELL("Flowcell"),
        LANE("Lane"),
        ANALYSIS_NODE("Analysis Node"),
        NUM_OF_ONE_MISMATCH_IDX_READS("# of one mismatch idx reads"),
        NUM_OF_Q30_BASES_PF("# Q30 Bases PF"),
        NUM_OF_PCT_IDX_READS("# Perfect Index Reads"),
        NUM_OF_READS("# Reads"),
        NUM_PERFECT_READS("# Perfect Reads"),
        MEAN_QUALITY_SCORE_PF("Mean Quality Score PF");

        private String displayName;
        private ConfigurableList.Header resultHeader;

        VALUE_COLUMN_TYPE( String displayName ) {
            this.displayName = displayName;
            this.resultHeader = new ConfigurableList.Header(displayName, displayName, "");
        }

        public String getDisplayName() {
            return displayName;
        }

        public ConfigurableList.Header getResultHeader() {
            return resultHeader;
        }

    }

    public static List<ConfigurableList.Header> buildHeaders() {
        // Append headers for metric data of interest.
        List<ConfigurableList.Header> headers = new ArrayList<>();
        for( SampleMetadataSequencingMetricPlugin.VALUE_COLUMN_TYPE valueColumnType :
                SampleMetadataSequencingMetricPlugin.VALUE_COLUMN_TYPE.values() ){
            headers.add(valueColumnType.getResultHeader());
        }
        return headers;
    }

    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup,
                                              @Nonnull SearchContext context) {
        return null;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull SearchContext context) {
        DemultiplexSampleDao demultiplexSampleDao = ServiceAccessUtility.getBean(DemultiplexSampleDao.class);

        MercurySample sample = (MercurySample) entity;

        List<ConfigurableList.Header> headers = buildHeaders();

        List<ConfigurableList.ResultRow> resultRows = new ArrayList<>();

        List<DemultiplexSampleMetric> runMetrics = demultiplexSampleDao.findBySampleAlias(
                Collections.singletonList(sample.getSampleKey()));

        // TODO just sort by lane...
        Map<Integer, DemultiplexSampleMetric> mapSampleToMetric = new TreeMap<>();
        for (DemultiplexSampleMetric metric: runMetrics) {
            mapSampleToMetric.put(metric.getLane(), metric);
        }

        // Gather Sequencing Lanes
        for( Map.Entry<Integer, DemultiplexSampleMetric> entry : mapSampleToMetric.entrySet()) {
            DemultiplexSampleMetric metric = entry.getValue();

            List<String> cells = new ArrayList<>();

            List<ConfigurableList.ResultList> nestedTables = new ArrayList<>();
            // Empty nested table for row name
            nestedTables.add(null);

            cells.add(metric.getRunName());
            cells.add(ColumnValueType.DATE.format(metric.getRunDate(), ""));
            cells.add(metric.getFlowcell());
            cells.add(String.valueOf(metric.getLane()));
            cells.add(metric.getAnalysisNode());
            cells.add(String.valueOf(metric.getNumberOfOneMismatchIndexReads()));
            cells.add(String.valueOf(metric.getNumberOfQ30BasesPF()));
            cells.add(String.valueOf(metric.getNumberOfPerfectIndexReads()));
            cells.add(String.valueOf(metric.getNumberOfReads()));
            cells.add(String.valueOf(metric.getNumberOfPerfectReads()));
            cells.add( String.valueOf(metric.getMeanQualityScorePF()));

            ConfigurableList.ResultRow resultRow = new ConfigurableList.ResultRow(null, cells, metric.getRunName() + " " + metric.getLane());
            resultRow.setCellNestedTables(nestedTables);
            resultRows.add(resultRow);
        }

        return new ConfigurableList.ResultList( resultRows, headers, 0, "ASC");
    }
}
