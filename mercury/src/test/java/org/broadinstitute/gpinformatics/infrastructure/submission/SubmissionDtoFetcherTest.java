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

import com.google.common.collect.ImmutableMap;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTrackerTest;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTuple;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.cognos.entity.PicardAggregationSample;
import org.broadinstitute.gpinformatics.infrastructure.metrics.AggregationMetricsFetcher;
import org.broadinstitute.gpinformatics.infrastructure.metrics.AggregationTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationAlignment;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionDtoFetcherTest {
    private static final String TEST_SAMPLE = "SM-35BDA";
    private static final String COLLABORATOR_SAMPLE_ID = "BOT2365_T";
    private static final String RESEARCH_PROJECT_ID = "RP-YOMAMA";
    private static final String PRODUCT_ORDER_ID = "PD0-1EE";
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
    private ProductOrderSampleDao productOrderSampleDao = Mockito.mock(ProductOrderSampleDao.class);

    // Collections returned from mocks
    private List<Aggregation> aggregations;

    @BeforeMethod
    public void setUp() {
        // Prepare mocks
        aggregations = new ArrayList<>();
        Mockito
            .when(aggregationMetricsFetcher.fetch(Mockito.anyCollectionOf(SubmissionTuple.class)))
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
        Mockito.when(productOrderSampleDao.findSubmissionSamples(Mockito.anyString())).thenReturn(
                Collections.singletonList(productOrderSample)
        );

        // Create unit under test
        submissionDtoFetcher = new SubmissionDtoFetcher(aggregationMetricsFetcher, submissionService,
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
        researchProject.addSubmissionTracker(new SubmissionTrackerTest.SubmissionTrackerStub(1234L, RESEARCH_PROJECT_ID,
                COLLABORATOR_SAMPLE_ID, "1", FileType.BAM, SubmissionBioSampleBean.ON_PREM));

        Aggregation aggregation = AggregationTestFactory
                .buildAggregation(RESEARCH_PROJECT_ID, PRODUCT_ORDER_ID, COLLABORATOR_SAMPLE_ID, 1, CONTAMINATION, FINGERPRINT_LOD, DATA_TYPE, QUALITY_METRIC,
                        null, null, SubmissionBioSampleBean.ON_PREM);
        aggregations.add(aggregation);
        List<SubmissionDto> submissionDtoList = submissionDtoFetcher.fetch(researchProject, MessageReporter.UNUSED);

        assertThat(submissionDtoList, hasSize(1));
        for (SubmissionDto submissionDto : submissionDtoList) {
            assertThat(submissionDto.getSampleName(), equalTo(COLLABORATOR_SAMPLE_ID));
            assertThat(submissionDto.getAggregationProject(), equalTo(RESEARCH_PROJECT_ID));
            assertThat(submissionDto.getResearchProject(), equalTo(RESEARCH_PROJECT_ID));
            assertThat(submissionDto.getFingerprintLOD(), equalTo(FINGERPRINT_LOD));
            assertThat(submissionDto.getProductOrders(), containsInAnyOrder(productOrder.getJiraTicketKey()));
            assertThat(submissionDto.getLanesInAggregation(), Matchers.equalTo(2));
            assertThat(submissionDto.getSubmittedStatus(),
                    Matchers.equalTo(SubmissionStatusDetailBean.Status.FAILURE.getKey()));
            assertThat(submissionDto.getSubmissionLibraryDescriptor(), equalTo(SubmissionLibraryDescriptor.WHOLE_EXOME.getName()));
            assertThat(submissionDto.getSubmissionRepositoryName(), equalTo(SubmissionRepository.DEFAULT_REPOSITORY_DESCRIPTOR));

            assertThat(submissionDto.getStatusDate(), Matchers.notNullValue());
            assertThat(submissionDto.getSubmittedErrors(), Matchers.contains(NCBI_ERROR));
        }
    }

    public void testFetchAggregationDtos() {
        ProductOrder productOrder = ProductOrderTestFactory.createProductOrder("SM-1", "SM-2");
        List<ProductOrderSample> samples = productOrder.getSamples();
        ProductOrderSample sample1 = samples.get(0);
        ProductOrderSample sample2 = samples.get(1);

        sample1.setSampleData(new BspSampleData(
            ImmutableMap.of(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "COLAB-" + sample1.getSampleKey())));
        sample2.setSampleData(new BspSampleData(
            ImmutableMap.of(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "COLAB-" + sample2.getSampleKey())));

        String rpId = productOrder.getResearchProject().getBusinessKey();

        Aggregation testAggregation1 = getTestAggregation(rpId, productOrder.getBusinessKey(),
            sample1.getSampleData().getCollaboratorsSampleName(),
            SubmissionLibraryDescriptor.WHOLE_EXOME, SubmissionBioSampleBean.GCP);
        String pdoKey = String.format("%s,%s", productOrder.getBusinessKey(), "PDO-2");
        Aggregation testAggregation2 = getTestAggregation(rpId, pdoKey,
            sample2.getSampleData().getCollaboratorsSampleName(),
            SubmissionLibraryDescriptor.RNA_SEQ, SubmissionBioSampleBean.ON_PREM);

        aggregations = Arrays.asList(testAggregation1, testAggregation2);

        Mockito
            .when(aggregationMetricsFetcher.fetch(Mockito.anyCollectionOf(SubmissionTuple.class)))
            .thenReturn(aggregations);

        Map<SubmissionTuple, Aggregation> tupleMap =
            submissionDtoFetcher.fetchAggregationDtos(productOrder.getSamples());

        assertThat(tupleMap.size(), is(2));
        verifyTuple(tupleMap, testAggregation1, sample1);
        verifyTuple(tupleMap, testAggregation2, sample2);
    }

    private void verifyTuple(Map<SubmissionTuple, Aggregation> tupleMap, Aggregation aggregation,
                             ProductOrderSample productOrderSample) {
        SubmissionTuple tuple = aggregation.getSubmissionTuple();
        assertThat(tupleMap.get(tuple), is(aggregation));
        assertThat(tuple.getSampleName(), is(productOrderSample.getSampleData().getCollaboratorsSampleName()));
    }

    private Aggregation getTestAggregation(String project, String productOrder, String sample, SubmissionLibraryDescriptor libraryDescriptor,
                                           String processingLocation) {
        return new Aggregation(project, sample, null, 1, 1, libraryDescriptor.getName(),
            Collections.<AggregationAlignment>emptySet(), null, null,
            null, null, null, new PicardAggregationSample(project, project, productOrder, sample, libraryDescriptor.getName()), processingLocation);
    }

    /**
     * A MessageReporter that records its messages. This is useful for testing.
     */
    class StringReporter implements MessageReporter {
        private List<String> messages = new ArrayList<>();

        @Override
        public String addMessage(String message, Object... arguments) {
            messages.add(MessageFormat.format(message, arguments));
            return message;
        }

        public List<String> getMessages() {
            return messages;
        }
    }
}
