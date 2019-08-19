package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.analytics.AlignmentMetricsDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.AlignmentMetric;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SampleMetadataAlignmentMetricsPlugin implements ListPlugin {

    public SampleMetadataAlignmentMetricsPlugin() {
    }

    public enum VALUE_COLUMN_TYPE {
        RUN_NAME("Run Name"),
        RUN_DATE("Run Date"),
        SAMPLE_ALIAS("Sample Alias"),
        TOTAL_READS("Total Reads"),
        NUM_DUP_MARKED_READS("# Duplicate Marked Reads"),
        NUM_DUP_MARKED_REMOVED("# Duplicate Marked Removed"),
        NUM_UNIQUE_READS("# Unique Reads"),
        NUM_READS_MATE_SEQ("# Reads Mate Sequenced"),
        NUM_READS_WO_MATE_SEQ("# Reads w/o Mate Sequenced"),
        NUM_QC_FAILED_READS("# QC Failed Reads"),
        NUM_MAPPED_READS("# Mapped Reads"),
        NUM_UNIQUE_MAPPED_READS("# Unique Mapped Reads"),
        NUM_UNMAPPED_READS("# Unmapped Reads"),
        NUM_SINGLETON_READS("# Singleton Reads"),
        NUM_PAIRED_READS("# Paired Reads"),
        NUM_PROPERLY_PAIRED_READS("# Properly Paired Reads"),
        NUM_NOT_PROPERLY_PAIRED_READS("# Not Properly Paired Reads"),
        MAPQ_40_INF("Mapq 40-Inf"),
        MAPQ_30_40("Mapq 30-40"),
        MAPQ_20_30("Mapq 20-30"),
        MAPQ_10_20("Mapq 10-20"),
        MAPQ0_10("Mapq 0-10"),
        MAPQ_NA("Mapq NA"),
        READS_INDEL_R1("Reads Indel R1"),
        READS_INDEL_R2("Reads Indel R2"),
        SOFT_CLIPPED_BASES_R1("Soft Clipped Bases R1"),
        SOFT_CLIPPED_BASES_R2("Soft Clipped Bases R2"),
        TOTAL_ALIGNMENTS("Total Alignments"),
        SECONDARY_ALIGNMENTS("Secondary Alignments"),
        SUPPLEMENTARY_ALIGNMENTS("Supplementary Alignments"),
        EST_READ_LENGTH("Est. Read Length"),
        AVG_SEQ_COVERAGE("Avg. Seq Coverage"),
        INSERT_LENGTH_MEAN("Insert Length Mean"),
        INSERT_LENGTH_STD("Insert Length STD."),
        ;

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

    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup,
                                              @Nonnull SearchContext context) {
        AlignmentMetricsDao alignmentMetricsDao = ServiceAccessUtility.getBean(AlignmentMetricsDao.class);

        List<MercurySample> samples = (List<MercurySample>) entityList;

        for( VALUE_COLUMN_TYPE valueColumnType :
                VALUE_COLUMN_TYPE.values() ){
            headerGroup.addHeader(valueColumnType.getResultHeader());
        }

        List<ConfigurableList.Row> metricRows = new ArrayList<>();

        List<String> sampleAlias = samples.stream().map(MercurySample::getSampleKey).collect(Collectors.toList());
        List<AlignmentMetric> runMetrics = alignmentMetricsDao.findBySampleAlias(sampleAlias);

        // Gather Sequencing Lanes
        for( AlignmentMetric metric: runMetrics) {
            ConfigurableList.Row row = new ConfigurableList.Row(metric.getSampleAlias());
            metricRows.add(row);

            String value = metric.getRunName();
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.RUN_NAME.getResultHeader(), value, value));

            value = ColumnValueType.DATE.format(metric.getRunDate(), "");
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.RUN_DATE.getResultHeader(), value, value));

            value = metric.getSampleAlias();
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.SAMPLE_ALIAS.getResultHeader(), value, value));

            value = String.valueOf(metric.getTotalReads());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.TOTAL_READS.getResultHeader(), value, value));

            value = String.valueOf(metric.getNumberOfDuplicateMarkedReads());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.NUM_DUP_MARKED_READS.getResultHeader(), value, value));

            value = String.valueOf(metric.getNumberOfDuplicateMarkedRemoved());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.NUM_DUP_MARKED_REMOVED.getResultHeader(), value, value));

            value = String.valueOf(metric.getNumberOfUniqueReads());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.NUM_UNIQUE_READS.getResultHeader(), value, value));

            value = String.valueOf(metric.getNumberOfReadsMateSequenced());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.NUM_READS_MATE_SEQ.getResultHeader(), value, value));

            value = String.valueOf(metric.getNumberOfReadsWithoutMateSequenced());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.NUM_READS_WO_MATE_SEQ.getResultHeader(), value, value));

            value = String.valueOf(metric.getNumberOfQcFailedReads());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.NUM_QC_FAILED_READS.getResultHeader(), value, value));

            value = String.valueOf(metric.getNumberOfMappedReads());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.NUM_MAPPED_READS.getResultHeader(), value, value));

            value = String.valueOf(metric.getNumberOfUniqueMappedReads());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.NUM_UNIQUE_MAPPED_READS.getResultHeader(), value, value));

            value = String.valueOf(metric.getNumberOfUnmappedReads());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.NUM_UNMAPPED_READS.getResultHeader(), value, value));

            value = String.valueOf(metric.getNumberOfSingletonReads());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.NUM_SINGLETON_READS.getResultHeader(), value, value));

            value = String.valueOf(metric.getNumberOfPairedReads());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.NUM_PAIRED_READS.getResultHeader(), value, value));

            value = String.valueOf(metric.getNumberOfProperlyPairedReads());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.NUM_PROPERLY_PAIRED_READS.getResultHeader(), value, value));

            value = String.valueOf(metric.getNumberOfNotProperlyPairedReads());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.NUM_NOT_PROPERLY_PAIRED_READS.getResultHeader(), value, value));

            value = String.valueOf(metric.getMapq40Inf());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.MAPQ_40_INF.getResultHeader(), value, value));

            value = String.valueOf(metric.getMapq3040());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.MAPQ_30_40.getResultHeader(), value, value));

            value = String.valueOf(metric.getMapq2030());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.MAPQ_20_30.getResultHeader(), value, value));

            value = String.valueOf(metric.getMapq1020());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.MAPQ_10_20.getResultHeader(), value, value));

            value = String.valueOf(metric.getMapq010());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.MAPQ0_10.getResultHeader(), value, value));

            value = String.valueOf(metric.getMapqNa());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.MAPQ_NA.getResultHeader(), value, value));

            value = String.valueOf(metric.getReadsIndelR1());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.READS_INDEL_R1.getResultHeader(), value, value));

            value = String.valueOf(metric.getReadsIndelR2());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.READS_INDEL_R2.getResultHeader(), value, value));

            value = String.valueOf(metric.getSoftClippedBasesR1());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.SOFT_CLIPPED_BASES_R1.getResultHeader(), value, value));

            value = String.valueOf(metric.getSoftClippedBasesR2());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.SOFT_CLIPPED_BASES_R2.getResultHeader(), value, value));

            value = String.valueOf(metric.getTotalAlignments());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.TOTAL_ALIGNMENTS.getResultHeader(), value, value));

            value = String.valueOf(metric.getSecondaryAlignments());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.SECONDARY_ALIGNMENTS.getResultHeader(), value, value));

            value = String.valueOf(metric.getSupplementaryAlignments());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.SUPPLEMENTARY_ALIGNMENTS.getResultHeader(), value, value));

            value = String.valueOf(metric.getEstimatedReadLength());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.EST_READ_LENGTH.getResultHeader(), value, value));

            value = String.valueOf(metric.getAverageSequencingCoverage());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.AVG_SEQ_COVERAGE.getResultHeader(), value, value));

            value = String.valueOf(metric.getInsertLengthMean());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.INSERT_LENGTH_MEAN.getResultHeader(), value, value));

            value = String.valueOf(metric.getInsertLengthStd());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.INSERT_LENGTH_STD.getResultHeader(), value, value));
        }

        return metricRows;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull SearchContext context) {
        return null;
    }
}
