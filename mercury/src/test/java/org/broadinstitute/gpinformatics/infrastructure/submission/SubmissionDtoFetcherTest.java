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
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.metrics.AggregationMetricsFetcher;
import org.broadinstitute.gpinformatics.infrastructure.metrics.AggregationTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class SubmissionDtoFetcherTest {
    private SubmissionsService submissionService = new SubmissionsServiceStub();
    private static final String TEST_SAMPLE = "SM-35BDA";
    private static final String COLLABORATOR_SAMPLE_ID = "BOT2365_T";
    private static final String RESEARCH_PROJECT_ID = "RP-YOMAMA";
    private static final String DATA_TYPE = "Exome";
    private static final String NCBI_ERROR = "And error was returned from NCBI";

    private static final Double QUALITY_METRIC = 1.2;

    public void testFetch() throws Exception {
        double contamination = 2.2d;
        LevelOfDetection fingerprintLod = new LevelOfDetection(-45d, -13d);
        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject(RESEARCH_PROJECT_ID);

        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(0);
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        ProductOrderSample productOrderSample = new ProductOrderSample(TEST_SAMPLE);
        productOrderSample.setSampleData(new BspSampleData(new HashMap<BSPSampleSearchColumn, String>() {{
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, COLLABORATOR_SAMPLE_ID);
        }}));
        productOrder.addSample(productOrderSample);
        researchProject.addProductOrder(productOrder);
        researchProject.addSubmissionTracker(
                new SubmissionTrackerTest.SubmissionTrackerStub(1234L, COLLABORATOR_SAMPLE_ID, BassFileType.BAM, "1"));
        researchProject.addSubmissionTracker(new SubmissionTrackerTest.SubmissionTrackerStub(1234L, COLLABORATOR_SAMPLE_ID,
                BassFileType.BAM, "1"));
        List<Aggregation> aggregation =
                Collections.singletonList(AggregationTestFactory
                        .buildAggregation(RESEARCH_PROJECT_ID, COLLABORATOR_SAMPLE_ID, contamination,
                                fingerprintLod, DATA_TYPE, QUALITY_METRIC, null, null));
        BassDTO bassResults = BassDtoTestFactory.buildBassResults(RESEARCH_PROJECT_ID, COLLABORATOR_SAMPLE_ID);

        AggregationMetricsFetcher aggregationMetricsFetcher = Mockito.mock(AggregationMetricsFetcher.class);
        Mockito.when(aggregationMetricsFetcher.fetch(Mockito.anyListOf(String.class), Mockito.anyListOf(String.class),
                Mockito.anyListOf(Integer.class))).thenReturn(aggregation);

        BassSearchService bassSearchService = Mockito.mock(BassSearchServiceImpl.class);
        Mockito.when(bassSearchService.runSearch(Mockito.anyString(), Mockito.<String>anyVararg()))
                .thenReturn(Collections.singletonList(bassResults));

        Map<String, BspSampleData> bspSampleDataMap = new HashMap<>();
        HashMap<BSPSampleSearchColumn, String> dataMap = new HashMap<>();
        dataMap.put(BSPSampleSearchColumn.SAMPLE_ID, TEST_SAMPLE);
        dataMap.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, COLLABORATOR_SAMPLE_ID);

        BSPSampleDataFetcher bspSampleDataFetcher = Mockito.mock(BSPSampleDataFetcher.class);
        bspSampleDataMap.put(TEST_SAMPLE, new BspSampleData(dataMap));

        Mockito.when(bspSampleDataFetcher.fetchSampleData(Mockito.anyCollectionOf(String.class),
                Mockito.any(BSPSampleSearchColumn.class))).thenReturn(
                bspSampleDataMap);

        BSPConfig testBspConfig = new BSPConfig(Deployment.STUBBY);

        ProductOrderSampleDao productOrderSampleDao = Mockito.mock(ProductOrderSampleDao.class);
        Mockito.when(productOrderSampleDao.findByResearchProject(Mockito.anyString())).thenReturn(
                Collections.singletonList(productOrderSample)
        );
        SubmissionDtoFetcher submissionDtoFetcher =
                new SubmissionDtoFetcher(aggregationMetricsFetcher, bassSearchService, bspSampleDataFetcher, submissionService,
                        testBspConfig, productOrderSampleDao);
        List<SubmissionDto> submissionDtoList = submissionDtoFetcher.fetch(researchProject);

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
