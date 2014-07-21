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

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDtoTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.metrics.AggregationMetricsFetcher;
import org.broadinstitute.gpinformatics.infrastructure.metrics.AggregationTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.metrics.LevelOfDetection;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionDtoFetcherTest {

    public static final String TEST_SAMPLE = "SM-35BDA";
    public static final String COLLABORATOR_SAMPLE_ID = "BOT2365_T";
    public static final String RESEARCH_PROJECT_ID = "RP-YOMAMA";
    private static final String DATA_TYPE = "Exome";
    private static final Double QUALITY_METRIC = 1.2;

    public void testFetch() throws Exception {
        Date dateCompleted = DateUtils.parseDate("01/01/2014");
        double contamination = 2.2d;
        LevelOfDetection fingerprintLod = new LevelOfDetection(-45d, -13d);
        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject(RESEARCH_PROJECT_ID);

        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(0);
        productOrder.addSample(new ProductOrderSample(TEST_SAMPLE));
        researchProject.addProductOrder(productOrder);
        Aggregation aggregation =
                AggregationTestFactory.buildAggregation(RESEARCH_PROJECT_ID, COLLABORATOR_SAMPLE_ID, contamination,
                        fingerprintLod, DATA_TYPE, QUALITY_METRIC, null,null);
        BassDTO bassResults = BassDtoTestFactory.buildBassResults(RESEARCH_PROJECT_ID, COLLABORATOR_SAMPLE_ID);

        AggregationMetricsFetcher aggregationMetricsFetcher = Mockito.mock(AggregationMetricsFetcher.class);
        Mockito.when(aggregationMetricsFetcher.fetch(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(aggregation);

        BassSearchService bassSearchService = Mockito.mock(BassSearchService.class);
        Mockito.when(bassSearchService.runSearch(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Arrays.asList(bassResults));

        Map<String, BSPSampleDTO> bspSampleDTOMap = new HashMap<>();
        HashMap<BSPSampleSearchColumn, String> dataMap = new HashMap<>();
        dataMap.put(BSPSampleSearchColumn.SAMPLE_ID, TEST_SAMPLE);
        dataMap.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, COLLABORATOR_SAMPLE_ID);

        BSPSampleDataFetcher bspSampleDataFetcher = Mockito.mock(BSPSampleDataFetcher.class);
        bspSampleDTOMap.put(TEST_SAMPLE, new BSPSampleDTO(dataMap));

        Mockito.when(bspSampleDataFetcher.fetchSamplesFromBSP(Mockito.anyCollectionOf(String.class))).thenReturn(bspSampleDTOMap);

        SubmissionDtoFetcher submissionDtoFetcher =
                new SubmissionDtoFetcher(aggregationMetricsFetcher, bassSearchService, bspSampleDataFetcher);
        List<SubmissionDto> submissionDtoList = submissionDtoFetcher.fetch(researchProject, 1);

        assertThat(submissionDtoList, is(not(empty())));
        for (SubmissionDto submissionDto : submissionDtoList) {
            assertThat(submissionDto.getSampleName(), equalTo(COLLABORATOR_SAMPLE_ID));
            assertThat(submissionDto.getAggregationProject(), equalTo(RESEARCH_PROJECT_ID));
            assertThat(submissionDto.getResearchProject(), equalTo(RESEARCH_PROJECT_ID));
            assertThat(submissionDto.getFingerprintLOD(), equalTo(fingerprintLod));
            assertThat(submissionDto.getProductOrders(), containsInAnyOrder(productOrder));
            assertThat(submissionDto.getLanesInAggregation(), Matchers.equalTo(2));
            assertThat(submissionDto.getDateCompleted(), Matchers.equalTo(dateCompleted));

        }


    }

    public void testDate() throws ParseException {
        Timestamp ts = new Timestamp(new Date().getTime());
        ts.setNanos(100);

        Date dateCompleted=new Date(ts.getTime());

        Assert.assertEquals((Date)ts, dateCompleted);
    }
}
