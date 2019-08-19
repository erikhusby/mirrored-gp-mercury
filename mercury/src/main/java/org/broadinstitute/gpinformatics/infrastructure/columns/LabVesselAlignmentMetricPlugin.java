package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.analytics.AlignmentMetricsDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.AlignmentMetric;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class LabVesselAlignmentMetricPlugin implements ListPlugin {

    public LabVesselAlignmentMetricPlugin() {
    }

    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup,
                                              @Nonnull SearchContext context) {
        return null;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull SearchContext context) {
        AlignmentMetricsDao alignmentMetricsDao = ServiceAccessUtility.getBean(AlignmentMetricsDao.class);

        LabVessel labVessel = (LabVessel) entity;

        List<ConfigurableList.Header> headers = new ArrayList<>();
        for( SampleMetadataAlignmentMetricsPlugin.VALUE_COLUMN_TYPE valueColumnType :
                SampleMetadataAlignmentMetricsPlugin.VALUE_COLUMN_TYPE.values() ){
            headers.add(valueColumnType.getResultHeader());
        }

        List<ConfigurableList.ResultRow> resultRows = new ArrayList<>();

        Set<SampleInstanceV2> sampleInstancesV2 = labVessel.getSampleInstancesV2();

        Set<String> sampleKeys = sampleInstancesV2.stream()
                .map(SampleInstanceV2::getRootOrEarliestMercurySampleName)
                .collect(Collectors.toSet());

        List<AlignmentMetric> runMetrics = alignmentMetricsDao.findBySampleAlias(new ArrayList<>(sampleKeys));

        // Gather Sequencing Lanes
        for( AlignmentMetric metric: runMetrics) {

            List<String> cells = new ArrayList<>();

            List<ConfigurableList.ResultList> nestedTables = new ArrayList<>();

            nestedTables.add(null);

            String value = metric.getRunName();
            cells.add(value);

            value = ColumnValueType.DATE.format(metric.getRunDate(), "");
            cells.add(value);

            value = metric.getSampleAlias();
            cells.add(value);

            value = String.valueOf(metric.getTotalReads());
            cells.add(value);

            value = String.valueOf(metric.getNumberOfDuplicateMarkedReads());
            cells.add(value);

            value = String.valueOf(metric.getNumberOfDuplicateMarkedRemoved());
            cells.add(value);

            value = String.valueOf(metric.getNumberOfUniqueReads());
            cells.add(value);

            value = String.valueOf(metric.getNumberOfReadsMateSequenced());
            cells.add(value);

            value = String.valueOf(metric.getNumberOfReadsWithoutMateSequenced());
            cells.add(value);

            value = String.valueOf(metric.getNumberOfQcFailedReads());
            cells.add(value);

            value = String.valueOf(metric.getNumberOfMappedReads());
            cells.add(value);

            value = String.valueOf(metric.getNumberOfUniqueMappedReads());
            cells.add(value);

            value = String.valueOf(metric.getNumberOfUnmappedReads());
            cells.add(value);

            value = String.valueOf(metric.getNumberOfSingletonReads());
            cells.add(value);

            value = String.valueOf(metric.getNumberOfPairedReads());
            cells.add(value);

            value = String.valueOf(metric.getNumberOfProperlyPairedReads());
            cells.add(value);

            value = String.valueOf(metric.getNumberOfNotProperlyPairedReads());
            cells.add(value);

            value = String.valueOf(metric.getMapq40Inf());
            cells.add(value);

            value = String.valueOf(metric.getMapq3040());
            cells.add(value);

            value = String.valueOf(metric.getMapq2030());
            cells.add(value);

            value = String.valueOf(metric.getMapq1020());
            cells.add(value);

            value = String.valueOf(metric.getMapq010());
            cells.add(value);

            value = String.valueOf(metric.getMapqNa());
            cells.add(value);

            value = String.valueOf(metric.getReadsIndelR1());
            cells.add(value);

            value = String.valueOf(metric.getReadsIndelR2());
            cells.add(value);

            value = String.valueOf(metric.getSoftClippedBasesR1());
            cells.add(value);

            value = String.valueOf(metric.getSoftClippedBasesR2());
            cells.add(value);

            value = String.valueOf(metric.getTotalAlignments());
            cells.add(value);

            value = String.valueOf(metric.getSecondaryAlignments());
            cells.add(value);

            value = String.valueOf(metric.getSupplementaryAlignments());
            cells.add(value);

            value = String.valueOf(metric.getEstimatedReadLength());
            cells.add(value);

            value = String.valueOf(metric.getAverageSequencingCoverage());
            cells.add(value);

            value = String.valueOf(metric.getInsertLengthMean());
            cells.add(value);

            value = String.valueOf(metric.getInsertLengthStd());
            cells.add(value);

            ConfigurableList.ResultRow resultRow = new ConfigurableList.ResultRow(null, cells, metric.getSampleAlias());
            resultRow.setCellNestedTables(nestedTables);
            resultRows.add(resultRow);
        }

        return new ConfigurableList.ResultList( resultRows, headers, 0, "ASC");
    }
}
