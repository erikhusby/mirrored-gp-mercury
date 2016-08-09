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

package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTrackerTest;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDtoTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassFileType;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassSearchServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.metrics.AggregationMetricsFetcher;
import org.broadinstitute.gpinformatics.infrastructure.metrics.AggregationTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionDtoFetcherTest {
    private SubmissionsService submissionService = new SubmissionsServiceStub();
    private static final String TEST_SAMPLE = "SM-35BDA";
    private static final String COLLABORATOR_SAMPLE_ID = "BOT2365_T";
    private static final String RESEARCH_PROJECT_ID = "RP-YOMAMA";
    private static final String DATA_TYPE = "Exome";
    private static final String NCBI_ERROR = "And error was returned from NCBI";

    private static final Double QUALITY_METRIC = 1.2;

    /**
     * Create a test research project with an order for a sample. It does not need to be persisted because
     * SubmissionDtoFetcher just needs an object graph connected from ResearchProject to ProductOrderSample.
     *
     * Add a submission tracker using a stub that allows the test to indicate a fixed primary database key. This is
     * necessary in order to avoid persisting the entire object graph and to have a stable submission UUID.
     *
     * Construct mock results to return from:
     *   Bass - for availability of files to submit
     *   aggregation metrics database - for metrics like contamination and LOD
     *
     * Also mocking ProductOrderSampleDao for finding samples by RP. Do we need this or the entity graph or both?
     *
     * @throws Exception
     */
    public void testFetch() throws Exception {

        // Mercury object graph
        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject(RESEARCH_PROJECT_ID);
        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(0);
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        ProductOrderSample productOrderSample = new ProductOrderSample(TEST_SAMPLE);
        productOrderSample.setSampleData(new BspSampleData(new HashMap<BSPSampleSearchColumn, String>() {{
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, COLLABORATOR_SAMPLE_ID);
        }}));
        productOrder.addSample(productOrderSample);
        researchProject.addProductOrder(productOrder);
        researchProject.addSubmissionTracker(new SubmissionTrackerTest.SubmissionTrackerStub(1234L,
                COLLABORATOR_SAMPLE_ID, "", BassFileType.BAM, "1"));

        // Aggregation metrics
        double contamination = 2.2d;
        LevelOfDetection fingerprintLod = new LevelOfDetection(-45d, -13d);
        List<Aggregation> aggregation =
                Collections.singletonList(AggregationTestFactory
                        .buildAggregation(RESEARCH_PROJECT_ID, COLLABORATOR_SAMPLE_ID, contamination,
                                fingerprintLod, DATA_TYPE, QUALITY_METRIC, null, null));
        AggregationMetricsFetcher aggregationMetricsFetcher = Mockito.mock(AggregationMetricsFetcher.class);
        Mockito.when(aggregationMetricsFetcher.fetch(Mockito.anyListOf(String.class), Mockito.anyListOf(String.class),
                Mockito.anyListOf(Integer.class))).thenReturn(aggregation);

        // Bass results
        BassDTO bassResults = BassDtoTestFactory.buildBassResults(RESEARCH_PROJECT_ID, COLLABORATOR_SAMPLE_ID);
        BassSearchService bassSearchService = Mockito.mock(BassSearchServiceImpl.class);
        Mockito.when(bassSearchService.runSearch(Mockito.anyString(), Mockito.<String>anyVararg()))
                .thenReturn(Collections.singletonList(bassResults));

        // ProductOrderSampleDao mock results
        ProductOrderSampleDao productOrderSampleDao = Mockito.mock(ProductOrderSampleDao.class);
        Mockito.when(productOrderSampleDao.findByResearchProject(Mockito.anyString())).thenReturn(
                Collections.singletonList(productOrderSample)
        );

        // Finally actually exercising the code under test
        SubmissionDtoFetcher submissionDtoFetcher =
                new SubmissionDtoFetcher(aggregationMetricsFetcher, bassSearchService, submissionService,
                        productOrderSampleDao);
        List<SubmissionDto> submissionDtoList = submissionDtoFetcher.fetch(researchProject, MessageReporter.UNUSED);

        assertThat(submissionDtoList, is(not(empty())));
        for (SubmissionDto submissionDto : submissionDtoList) {
            assertThat(submissionDto.getSampleName(), equalTo(COLLABORATOR_SAMPLE_ID));
            assertThat(submissionDto.getAggregationProject(), equalTo(RESEARCH_PROJECT_ID));
            assertThat(submissionDto.getResearchProject(), equalTo(RESEARCH_PROJECT_ID));
            assertThat(submissionDto.getFingerprintLOD(), equalTo(fingerprintLod));
            assertThat(submissionDto.getProductOrders(), containsInAnyOrder(productOrder));
            assertThat(submissionDto.getLanesInAggregation(), Matchers.equalTo(2));
            assertThat(submissionDto.getSubmittedStatus(),
                    Matchers.equalTo(SubmissionStatusDetailBean.Status.FAILURE.getKey()));
            assertThat(submissionDto.getSubmissionLibraryDescriptor(), equalTo(SubmissionLibraryDescriptor.WHOLE_GENOME_DESCRIPTION));
            assertThat(submissionDto.getSubmissionRepositoryName(), equalTo(SubmissionRepository.DEFAULT_REPOSITORY_DESCRIPTOR));

            assertThat(submissionDto.getStatusDate(), Matchers.notNullValue());
            assertThat(submissionDto.getSubmittedErrors(), Matchers.contains(NCBI_ERROR));
        }
    }
}
