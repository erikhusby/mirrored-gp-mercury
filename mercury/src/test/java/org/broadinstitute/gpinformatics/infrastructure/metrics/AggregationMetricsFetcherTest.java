package org.broadinstitute.gpinformatics.infrastructure.metrics;

import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTuple;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionBioSampleBean;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterableOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * Tests for the connection to the Picard aggregation metrics database as well as the JPA entity mappings.
 * <p>
 * These tests have to be written in terms of data that is known to be (or not be) in the metrics database because:
 * <ul>
 *     <li>the database user is not allowed to write to the database</li>
 *     <li>we are connecting to the production database because there is not an appropriate dev or test database</li>
 * </ul>
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class AggregationMetricsFetcherTest extends StubbyContainerTest {

    public AggregationMetricsFetcherTest(){}

    /**
     * Sample (a.k.a., "Collaborator Sample ID") for which there are known aggregations in the metrics database.
     */
    public static final String SAMPLE = "NA12878";

    /**
     * Mercury research project for which SAMPLE has been aggregated on.
     */
    public static final String MERCURY_PROJECT = "RP-697";

    /**
     * Datatype for the submission.
     */
    public static final String DATA_TYPE = "WGS";

    /**
     * Version of aggregation on Mercury research project.
     */
    public static final int MERCURY_AGGREGATION_VERSION = 2;

    /**
     * Squid project for which SAMPLE has been aggregated on. This is a good test case because, in the metrics database,
     * there are multiple rows in AGGREGATION where LIBRARY is non-null
     */
    public static final String SQUID_PROJECT = "C1700";

    /**
     * Version of aggregation on Squid project.
     */
    public static final int SQUID_AGGREGATION_VERSION = 1;
    private static final String EXOME = "Exome";

    @Inject
    private AggregationMetricsFetcher fetcher;
    private static final double MIN_LOD = 17.926603;
    private static final double MAX_LOD = 55.771678;

    public void testFetchMetricsForSampleAggregatedByMercuryRP() {
        List<Aggregation> aggregationResults = fetcher.fetch(Collections.singletonList(
                new SubmissionTuple(MERCURY_PROJECT, MERCURY_PROJECT, SAMPLE, Integer.toString(MERCURY_AGGREGATION_VERSION),
                    SubmissionBioSampleBean.ON_PREM, EXOME)));

        Aggregation aggregation = aggregationResults.iterator().next();
        assertThat(aggregation.getProject(), equalTo(MERCURY_PROJECT));
        assertThat(aggregation.getSample(), equalTo(SAMPLE));
        assertThat(aggregation.getVersion(), equalTo(MERCURY_AGGREGATION_VERSION));
        LevelOfDetection lod = aggregation.getLevelOfDetection();
        assertThat(lod.getMax(), equalTo(MAX_LOD));
        assertThat(lod.getMin(), equalTo(MIN_LOD));

        assertThat(aggregation.getPicardAggregationSample().getProductOrderList(), Matchers.containsInAnyOrder("PDO-3853","PDO-3974"));
    }

    public void testFetchMetricsWithBadProject() {
        SubmissionTuple submissionTuple = new SubmissionTuple("BAD-" + MERCURY_PROJECT, "MORE_BAD" + MERCURY_PROJECT, SAMPLE,
            Integer.toString(MERCURY_AGGREGATION_VERSION),
            SubmissionBioSampleBean.ON_PREM, EXOME);
        List<Aggregation> aggregationResults = fetcher.fetch(Collections.singletonList(submissionTuple));
        assertThat(aggregationResults, emptyIterableOf(Aggregation.class));
    }

    public void testFetchMetricsWithBadSample() {
        List<Aggregation> aggregationResults = fetcher.fetch(Collections.singletonList(
                new SubmissionTuple(MERCURY_PROJECT, MERCURY_PROJECT, "BAD-" + SAMPLE, Integer.toString(MERCURY_AGGREGATION_VERSION),
                    SubmissionBioSampleBean.ON_PREM, EXOME)));
        assertThat(aggregationResults, emptyIterableOf(Aggregation.class));
    }

    public void testFetchMetricsForSampleAggregatedBySquidProject() {
        SubmissionTuple submissionTuple =
            new SubmissionTuple(SQUID_PROJECT, MERCURY_PROJECT, SAMPLE, Integer.toString(SQUID_AGGREGATION_VERSION),
                SubmissionBioSampleBean.ON_PREM, EXOME);
        List<Aggregation> aggregationResults = fetcher.fetch(Collections.singletonList(submissionTuple));
        assertThat(aggregationResults, hasSize(1));
        Aggregation aggregation = aggregationResults.get(0);
        assertThat(aggregation.getProject(), equalTo(SQUID_PROJECT));
        assertThat(aggregation.getSample(), equalTo(SAMPLE));
        assertThat(aggregation.getVersion(), equalTo(SQUID_AGGREGATION_VERSION));
        assertThat(aggregation.getAggregationContam().getPctContamination(), equalTo(0.0));
    }
}
