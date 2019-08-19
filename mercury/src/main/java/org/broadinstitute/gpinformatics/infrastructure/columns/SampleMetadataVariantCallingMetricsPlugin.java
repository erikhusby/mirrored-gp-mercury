package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.analytics.VariantCallMetricsDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.VariantCallMetric;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SampleMetadataVariantCallingMetricsPlugin implements ListPlugin {

    public enum VALUE_COLUMN_TYPE {
        RUN_NAME("Run Name"),
        RUN_DATE("Run Date"),
        METRIC_TYPE("Metric Type"),
        SAMPLE_ALIAS("Sample Alias"),
        TOTAL("Total"),
        BIALLELIC("Biallelic"),
        MULTIALLELIC("Multiallelic"),
        SNPS("Snps"),
        INDELS("Indels"),
        MNPS("Mnps"),
        CHR_X_NUMBER_OF_SNPS("Chr X Number Of Snps"),
        CHR_Y_NUMBER_OF_SNPS("Chr Y Number Of Snps"),
        SNP_TRANSITIONS("Snp Transitions"),
        SNP_TRANVERSIONS("Snp Tranversions"),
        HETEROZYGOUS("Heterozygous"),
        HOMOZYGOUS("Homozygous"),
        IN_DBSNP("In DB Snp"),
        NOT_IN_DBSNP("Not In DB Snp"),
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

    public SampleMetadataVariantCallingMetricsPlugin() {
    }

    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup,
                                              @Nonnull SearchContext context) {
        VariantCallMetricsDao variantCallMetricsDao = ServiceAccessUtility.getBean(VariantCallMetricsDao.class);

        List<MercurySample> samples = (List<MercurySample>) entityList;

        for( VALUE_COLUMN_TYPE valueColumnType : VALUE_COLUMN_TYPE.values() ){
            headerGroup.addHeader(valueColumnType.getResultHeader());
        }

        List<ConfigurableList.Row> metricRows = new ArrayList<>();

        List<String> sampleAlias = samples.stream().map(MercurySample::getSampleKey).collect(Collectors.toList());
        List<VariantCallMetric> runMetrics = variantCallMetricsDao.findBySampleAlias(sampleAlias).stream()
                .filter(vc -> vc.getMetricType().equals("VARIANT CALLER POSTFILTER"))
                .collect(Collectors.toList());

        for (VariantCallMetric metric: runMetrics) {
            ConfigurableList.Row row = new ConfigurableList.Row(metric.getSampleAlias());
            metricRows.add(row);

            String value = metric.getRunName();
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.RUN_NAME.getResultHeader(), value, value));

            value = ColumnValueType.DATE.format(metric.getRunDate(), "");
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.RUN_DATE.getResultHeader(), value, value));

            value = metric.getSampleAlias();
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.SAMPLE_ALIAS.getResultHeader(), value, value));

            value = metric.getMetricType();
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.METRIC_TYPE.getResultHeader(), value, value));

            value = metric.getTotal();
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.TOTAL.getResultHeader(), value, value));

            value = String.valueOf(metric.getBiallelic());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.BIALLELIC.getResultHeader(), value, value));

            value = String.valueOf(metric.getMultiallelic());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.MULTIALLELIC.getResultHeader(), value, value));

            value = String.valueOf(metric.getSnps());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.SNPS.getResultHeader(), value, value));

            value = String.valueOf(metric.getIndels());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.INDELS.getResultHeader(), value, value));

            value = String.valueOf(metric.getMnps());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.MNPS.getResultHeader(), value, value));

            value = String.valueOf(metric.getChrXNumberOfSnps());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.CHR_X_NUMBER_OF_SNPS.getResultHeader(), value, value));

            value = String.valueOf(metric.getChrYNumberOfSnps());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.CHR_Y_NUMBER_OF_SNPS.getResultHeader(), value, value));

            value = String.valueOf(metric.getSnpTransitions());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.SNP_TRANSITIONS.getResultHeader(), value, value));

            value = String.valueOf(metric.getSnpTranversions());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.SNP_TRANVERSIONS.getResultHeader(), value, value));

            value = String.valueOf(metric.getHeterozygous());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.HETEROZYGOUS.getResultHeader(), value, value));

            value = String.valueOf(metric.getHomozygous());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.HOMOZYGOUS.getResultHeader(), value, value));

            value = String.valueOf(metric.getInDbSnp());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.IN_DBSNP.getResultHeader(), value, value));

            value = String.valueOf(metric.getNotInDbSnp());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.NOT_IN_DBSNP.getResultHeader(), value, value));
        }

        return metricRows;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull SearchContext context) {
        return null;
    }
}
