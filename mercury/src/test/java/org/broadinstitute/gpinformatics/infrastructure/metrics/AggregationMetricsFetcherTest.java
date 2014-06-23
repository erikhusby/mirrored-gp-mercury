package org.broadinstitute.gpinformatics.infrastructure.metrics;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

/**
 * Tests for the connection to the Picard aggregation metrics database as well as the JPA entity mappings.
 * <p>
 * These tests have to be written in terms of data that is known to be (or not be) in the metrics database because:
 * <ul>
 *     <li>the database user is not allowed to write to the database</li>
 *     <li>we are connecting to the production database because there is not an appropriate dev or test database</li>
 * </ul>
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class AggregationMetricsFetcherTest extends ContainerTest {

    /**
     * Sample (a.k.a., "Collaborator Sample ID") for which there are known aggregations in the metrics database.
     */
    public static final String SAMPLE = "NA12878";

    /**
     * Mercury research project for which SAMPLE has been aggregated on.
     */
    public static final String MERCURY_PROJECT = "RP-697";

    /**
     * Version of aggregation on Mercury research project.
     */
    public static final int MERCURY_AGGREGATION_VERSION = 1;

    /**
     * Data type of aggregation on Mercury research project. Note that, when the aggregation is on Squid project, there
     * is no data type to query on. Bass will not indicate a data type for Squid project aggregated files.
     */
    public static final String DATA_TYPE = "Exome";

    /**
     * Squid project for which SAMPLE has been aggregated on. This is a good test case because, in the metrics database,
     * there are multiple rows in AGGREGATION where LIBRARY is non-null
     */
    public static final String SQUID_PROJECT = "C1700";

    /**
     * Version of aggregation on Squid project.
     */
    public static final int SQUID_AGGREGATION_VERSION = 1;

    @Inject
    private AggregationMetricsFetcher fetcher;

    public void fetchNonExistentMetrics() {
        Aggregation aggregation = fetcher.fetch("RP-1", "SM-TEST", 1, "type");
        assertThat(aggregation, nullValue());
    }

    public void fetchMetricsForSampleAggregatedByMercuryRP() {
        Aggregation aggregation = fetcher.fetch(MERCURY_PROJECT, SAMPLE, MERCURY_AGGREGATION_VERSION, DATA_TYPE);
        assertThat(aggregation.getProject(), equalTo(MERCURY_PROJECT));
        assertThat(aggregation.getSample(), equalTo(SAMPLE));
        assertThat(aggregation.getVersion(), equalTo(MERCURY_AGGREGATION_VERSION));
        assertThat(aggregation.getDataType(), equalTo(DATA_TYPE));
        assertThat(aggregation.getPercentContamination(), closeTo(0.0002, 0.00001));
    }

    public void fetchMetricsForSampleAggregatedByMercuryRPWithoutSupplyingDataType() {
        // TODO: This test may need to fail somehow. Perhaps different test data is needed.
        Aggregation aggregation = fetcher.fetch(MERCURY_PROJECT, SAMPLE, MERCURY_AGGREGATION_VERSION);
        assertThat(aggregation.getProject(), equalTo(MERCURY_PROJECT));
        assertThat(aggregation.getSample(), equalTo(SAMPLE));
        assertThat(aggregation.getVersion(), equalTo(MERCURY_AGGREGATION_VERSION));
        assertThat(aggregation.getDataType(), equalTo(DATA_TYPE));
        assertThat(aggregation.getPercentContamination(), closeTo(0.0002, 0.00001));
    }

    public void fetchMetricsForSampleAggregatedBySquidProject() {
        Aggregation aggregation = fetcher.fetch(SQUID_PROJECT, SAMPLE, SQUID_AGGREGATION_VERSION);
        assertThat(aggregation.getProject(), equalTo(SQUID_PROJECT));
        assertThat(aggregation.getSample(), equalTo(SAMPLE));
        assertThat(aggregation.getVersion(), equalTo(SQUID_AGGREGATION_VERSION));
        assertThat(aggregation.getDataType(), equalTo("N/A"));
        assertThat(aggregation.getPercentContamination(), equalTo(0.0));
    }
}
