/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.metrics;

import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.DATABASE_FREE)
public class AggregationTest {
    private static final Double EXOME_QUALITY_METRIC = 0.890141;
    private static final Long RNA_QUALITY_METRIC = 22L;
    private static final Double WGS_QUALITY_METRIC = 18.23;
    public static final String NOPROJECT = "noproject";
    public static final String NOSAMPLE = "nosample";
    private Aggregation aggregation;


    public void testExomeQualityMetric() throws Exception {
        aggregation = AggregationTestFactory
                .buildAggregation(NOPROJECT, NOSAMPLE, 1, null, null, BassDTO.DATA_TYPE_EXOME, EXOME_QUALITY_METRIC,
                        RNA_QUALITY_METRIC, WGS_QUALITY_METRIC);
        assertThat(aggregation.getDataType(), is(BassDTO.DATA_TYPE_EXOME));
        assertThat(aggregation.getQualityMetric(BassDTO.DATA_TYPE_EXOME), Matchers.equalTo(EXOME_QUALITY_METRIC));
    }

    public void testRNAQualityMetric() throws Exception {
        aggregation = AggregationTestFactory
                .buildAggregation(NOPROJECT, NOSAMPLE, 1, null, null, BassDTO.DATA_TYPE_RNA, EXOME_QUALITY_METRIC,
                        RNA_QUALITY_METRIC, WGS_QUALITY_METRIC);
        assertThat(aggregation.getDataType(), is(BassDTO.DATA_TYPE_RNA));
        assertThat(aggregation.getQualityMetric(BassDTO.DATA_TYPE_RNA).longValue(),
                Matchers.equalTo(RNA_QUALITY_METRIC));
    }


    public void testExomeQualityMetricIsNull() throws Exception {
        aggregation = AggregationTestFactory
                .buildAggregation(NOPROJECT, NOSAMPLE, 1, null, null, BassDTO.DATA_TYPE_EXOME, null, RNA_QUALITY_METRIC,
                        WGS_QUALITY_METRIC);
        assertThat(aggregation.getQualityMetric(BassDTO.DATA_TYPE_EXOME), Matchers.nullValue());
    }

    public void testUnknownDataTypeQualityMetricIsNull() throws Exception {
        String dataType = "foo";
        aggregation = AggregationTestFactory
                .buildAggregation(NOPROJECT, NOSAMPLE, 1, null, null, dataType, EXOME_QUALITY_METRIC,
                        RNA_QUALITY_METRIC, WGS_QUALITY_METRIC);

        assertThat(aggregation.getDataType(), is(dataType));
        assertThat(aggregation.getQualityMetric(dataType), Matchers.nullValue());
    }

    public void testExomeDataTypeQualityMetricFormat() throws Exception {
        aggregation = AggregationTestFactory
                .buildAggregation(NOPROJECT, NOSAMPLE, 1, null, null, BassDTO.DATA_TYPE_EXOME, EXOME_QUALITY_METRIC,
                        RNA_QUALITY_METRIC, WGS_QUALITY_METRIC);

        assertThat(aggregation.getQualityMetricString(BassDTO.DATA_TYPE_EXOME), Matchers.equalTo("89.01%"));
    }

    public void testRnaDataTypeQualityMetricFormat() throws Exception {
        aggregation = AggregationTestFactory
                .buildAggregation(NOPROJECT, NOSAMPLE, 1, null, null, BassDTO.DATA_TYPE_RNA, EXOME_QUALITY_METRIC,
                        RNA_QUALITY_METRIC, WGS_QUALITY_METRIC);

        assertThat(aggregation.getQualityMetricString(BassDTO.DATA_TYPE_RNA),
                Matchers.equalTo(RNA_QUALITY_METRIC.toString()));
    }

    public void testWGSDataTypeQualityMetricFormat() throws Exception {
        aggregation = AggregationTestFactory
                .buildAggregation(NOPROJECT, NOSAMPLE, 1, null, null, BassDTO.DATA_TYPE_WGS, EXOME_QUALITY_METRIC,
                        RNA_QUALITY_METRIC, WGS_QUALITY_METRIC);

        assertThat(aggregation.getQualityMetricString(BassDTO.DATA_TYPE_WGS), Matchers.equalTo("18.23"));
    }

    public void testNullDataTypeQualityMetricFormat() throws Exception {
        aggregation = AggregationTestFactory
                .buildAggregation(NOPROJECT, NOSAMPLE, 1, null, null, null, EXOME_QUALITY_METRIC, RNA_QUALITY_METRIC,
                        WGS_QUALITY_METRIC);

        assertThat(aggregation.getQualityMetricString(null), Matchers.nullValue());
    }
    public void testUnknownDataTypeQualityMetricFormat() throws Exception {
        aggregation = AggregationTestFactory
                .buildAggregation(NOPROJECT, NOSAMPLE, 1, null, null, "foo", EXOME_QUALITY_METRIC, RNA_QUALITY_METRIC,
                        WGS_QUALITY_METRIC);

        assertThat(aggregation.getQualityMetricString("foo"), Matchers.equalTo("N/A"));
    }
    public void testWgsDataTypeQualityMetricFormat() throws Exception {
        aggregation = AggregationTestFactory
                .buildAggregation(NOPROJECT, NOSAMPLE, 1, null, null, BassDTO.DATA_TYPE_WGS, EXOME_QUALITY_METRIC,
                        RNA_QUALITY_METRIC, WGS_QUALITY_METRIC);

        assertThat(aggregation.getQualityMetric(BassDTO.DATA_TYPE_WGS), Matchers.equalTo(WGS_QUALITY_METRIC));
    }
}
