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
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTuple;
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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionDtoFetcherTest {
    private static final String TEST_SAMPLE = "SM-35BDA";
    private static final String COLLABORATOR_SAMPLE_ID = "BOT2365_T";
    private static final String RESEARCH_PROJECT_ID = "RP-YOMAMA";
    private static final String DATA_TYPE = "Exome";
    private static final String NCBI_ERROR = "And error was returned from NCBI";
    private static final Double QUALITY_METRIC = 1.2;
    private static final double CONTAMINATION = 2.2d;
    private static final LevelOfDetection FINGERPRINT_LOD = new LevelOfDetection(-45d, -13d);

    // Unit under test
    private SubmissionDtoFetcher submissionDtoFetcher;

    // Mercury entities
    private ResearchProject researchProject;
    private ProductOrder productOrder;

    // Test doubles
    private SubmissionsService submissionService = new SubmissionsServiceStub();
    private AggregationMetricsFetcher aggregationMetricsFetcher = Mockito.mock(AggregationMetricsFetcher.class);
    private BassSearchService bassSearchService = Mockito.mock(BassSearchServiceImpl.class);
    private ProductOrderSampleDao productOrderSampleDao = Mockito.mock(ProductOrderSampleDao.class);

    // Collections returned from mocks
    private List<BassDTO> bassDTOs;
    private List<Aggregation> aggregations;

    @BeforeMethod
    public void setUp() {
        // Prepare mocks
        bassDTOs = new ArrayList<>();
        Mockito.when(bassSearchService.runSearch(Mockito.anyString(), Mockito.<String>anyVararg()))
                .thenReturn(bassDTOs);
        aggregations = new ArrayList<>();
        Mockito.when(aggregationMetricsFetcher.fetch(Mockito.anyListOf(SubmissionTuple.class)))
                .thenReturn(aggregations);

        // Create Mercury RP and PDO with one sample
        researchProject = ResearchProjectTestFactory.createTestResearchProject(RESEARCH_PROJECT_ID);
        productOrder = ProductOrderTestFactory.buildExExProductOrder(0);
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        ProductOrderSample productOrderSample = new ProductOrderSample(TEST_SAMPLE);
        productOrderSample.setSampleData(new BspSampleData(new HashMap<BSPSampleSearchColumn, String>() {{
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, COLLABORATOR_SAMPLE_ID);
        }}));
        productOrder.addSample(productOrderSample);
        researchProject.addProductOrder(productOrder);
        Mockito.when(productOrderSampleDao.findByResearchProject(Mockito.anyString())).thenReturn(
                Collections.singletonList(productOrderSample)
        );

        // Create unit under test
        submissionDtoFetcher = new SubmissionDtoFetcher(aggregationMetricsFetcher, bassSearchService, submissionService,
                productOrderSampleDao);
    }

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
        addBassFile(RESEARCH_PROJECT_ID, COLLABORATOR_SAMPLE_ID, 1);
        researchProject.addSubmissionTracker(new SubmissionTrackerTest.SubmissionTrackerStub(1234L, RESEARCH_PROJECT_ID,
                COLLABORATOR_SAMPLE_ID, "1", BassFileType.BAM));

        List<SubmissionDto> submissionDtoList = submissionDtoFetcher.fetch(researchProject, MessageReporter.UNUSED);

        assertThat(submissionDtoList, hasSize(1));
        for (SubmissionDto submissionDto : submissionDtoList) {
            assertThat(submissionDto.getSampleName(), equalTo(COLLABORATOR_SAMPLE_ID));
            assertThat(submissionDto.getAggregationProject(), equalTo(RESEARCH_PROJECT_ID));
            assertThat(submissionDto.getResearchProject(), equalTo(RESEARCH_PROJECT_ID));
            assertThat(submissionDto.getFingerprintLOD(), equalTo(FINGERPRINT_LOD));
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

    public void testFetchWithMultipleBamsForSameSample() {
        // Add BAMs to Bass
        String submittedAggregationProjectId = "P123";
        BassDTO bassDTO1 = addBassFile(RESEARCH_PROJECT_ID, COLLABORATOR_SAMPLE_ID, 1);
        BassDTO bassDTO2 = addBassFile(submittedAggregationProjectId, COLLABORATOR_SAMPLE_ID, 1);

        // Record submission of one of the BAMs
        researchProject.addSubmissionTracker(new SubmissionTrackerTest.SubmissionTrackerStub(1234L,
                submittedAggregationProjectId, COLLABORATOR_SAMPLE_ID, "1", BassFileType.BAM));

        // Finally actually exercising the code under test
        List<SubmissionDto> submissionDtoList = submissionDtoFetcher.fetch(researchProject, MessageReporter.UNUSED);

        assertThat(submissionDtoList, hasSize(2));
        for (SubmissionDto submissionDto : submissionDtoList) {
            assertThat(submissionDto.getSampleName(), equalTo(COLLABORATOR_SAMPLE_ID));
            assertThat(submissionDto.getResearchProject(), equalTo(RESEARCH_PROJECT_ID));
            assertThat(submissionDto.getFingerprintLOD(), equalTo(FINGERPRINT_LOD));
            assertThat(submissionDto.getProductOrders(), containsInAnyOrder(productOrder));
            assertThat(submissionDto.getLanesInAggregation(), Matchers.equalTo(2));
        }

        Map<SubmissionTuple, SubmissionDto> submissionDtoByTuple = new HashMap<>();
        for (SubmissionDto submissionDto : submissionDtoList) {
            submissionDtoByTuple.put(submissionDto.getSubmissionTuple(), submissionDto);
        }
        SubmissionDto submissionDto1 = submissionDtoByTuple.get(bassDTO1.getTuple());
        assertThat(submissionDto1.getAggregationProject(), equalTo(bassDTO1.getProject()));
        assertThat(submissionDto1.getSubmittedStatus(), equalTo(""));
        assertThat(submissionDto1.getSubmissionLibraryDescriptor(), equalTo(""));
        assertThat(submissionDto1.getSubmissionRepositoryName(), equalTo(""));
        assertThat(submissionDto1.getStatusDate(), equalTo(""));
        assertThat(submissionDto1.getSubmittedErrors(), Matchers.<String>empty());

        SubmissionDto submissionDto2 = submissionDtoByTuple.get(bassDTO2.getTuple());
        assertThat(submissionDto2.getAggregationProject(), equalTo(bassDTO2.getProject()));
        assertThat(submissionDto2.getSubmittedStatus(), equalTo(SubmissionStatusDetailBean.Status.FAILURE.getKey()));
        assertThat(submissionDto2.getSubmissionLibraryDescriptor(),
                equalTo(SubmissionLibraryDescriptor.WHOLE_GENOME_DESCRIPTION));
        assertThat(submissionDto2.getSubmissionRepositoryName(),
                equalTo(SubmissionRepository.DEFAULT_REPOSITORY_DESCRIPTOR));
        assertThat(submissionDto2.getStatusDate(), equalTo(SubmissionsServiceStub.STUB_UPDATE_DATE));
        assertThat(submissionDto2.getSubmittedErrors(), Matchers.contains(NCBI_ERROR));
    }

    /**
     * Create a BassDTO and Aggregation that will be returned by mock services in this test.
     *
     * @param project   the aggregation project
     * @param sample    the collaborator sample ID
     * @param version   the file version
     * @return the new BassDTO
     */
    private BassDTO addBassFile(String project, String sample, Integer version) {
        BassDTO bassDTO = BassDtoTestFactory.buildBassResults(project, sample, version.toString(), RESEARCH_PROJECT_ID);
        bassDTOs.add(bassDTO);
        Aggregation aggregation = AggregationTestFactory
                .buildAggregation(project, sample, version, CONTAMINATION, FINGERPRINT_LOD, DATA_TYPE, QUALITY_METRIC,
                        null, null);
        aggregations.add(aggregation);
        return bassDTO;
    }
}
