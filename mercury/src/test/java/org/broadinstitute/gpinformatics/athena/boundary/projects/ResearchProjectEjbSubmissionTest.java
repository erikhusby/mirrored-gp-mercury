/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.SubmissionTrackerDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.metrics.AggregationTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDto;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionStatusDetailBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsService;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@Test(groups = TestGroups.DATABASE_FREE)
public class ResearchProjectEjbSubmissionTest {
    private static final int TEST_VERSION_2 = 2;
    private static final String ON_PREM = "OnPrem";
    private static final String GCP = "GCP";
    private static String TEST_SAMPLE_1 = String.format("%d_E", System.currentTimeMillis());
    private static String TEST_SAMPLE_2 = TEST_SAMPLE_1 + "_2";
    private static final int TEST_VERSION_1 = 1;
    private static final String PDO_99999 = "PDO-99999";
    private ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder(1, PDO_99999);

    @SuppressWarnings("ConstantConditions")
    public void testValidateSubmissionsDtoHasNullsDto() throws Exception {
        SubmissionDto submissionDto = new SubmissionDto(null, null);

        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(null, null);

        try {
            researchProjectEjb.validateSubmissionDto(Collections.singletonList(submissionDto));
            Assert.fail("The data sources for this submissionDTO are all null, why was an exception not thrown?");
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains("No data was found in submission request."));
        }
    }

    public void testValidateSubmissionsEmptyDtoList() throws Exception {
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(null, null);
        try {
            researchProjectEjb.validateSubmissionDto(Collections.<SubmissionDto>emptyList());
            Assert.fail("Since a list of empty submissionDTOs was passed in, an exception should have ben thrown.");
        } catch (InformaticsServiceException e) {
            Assert.assertTrue(e.getMessage().equals("At least one selection is needed to post submissions"));
        }
    }

    public void testValidateSubmissionsDtoWithNoDaoResultPass() {
        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);
        setupSubmissionTrackerMock(submissionTrackerDao, Collections.<SubmissionTracker>emptyList());
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao, null);

        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, TEST_SAMPLE_1, ON_PREM, TEST_VERSION_1);

        try {
            researchProjectEjb.validateSubmissionDto(Collections.singletonList(submissionDto));
        } catch (Exception e) {
            Assert.fail("A call to submissionTrackerDao.findSubmissionTrackers returning an empty list, " +
                        "should not have caused a submission failure.", e);
        }

        verifySubmissionTrackerMock(submissionTrackerDao);
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    @DataProvider(name = "manyDtosManyTrackers")
    public Iterator<Object[]> manyDtosManyTrackers() {
        SubmissionDto bA = getSubmissionDto(dummyProductOrder, "A", ON_PREM, TEST_VERSION_1);
        SubmissionDto bA2 = getSubmissionDto(dummyProductOrder, "A", ON_PREM, TEST_VERSION_2);
        SubmissionDto bApicard = getSubmissionDto(dummyProductOrder, "A", GCP, TEST_VERSION_1);
        ResearchProject testResearchProject = ResearchProjectTestFactory.createTestResearchProject("P123");
        SubmissionTracker stA =
                new SubmissionTracker("P123", bA.getSampleName(), String.valueOf(bA.getVersion()), bA.getFileType(),
                    bA.getProcessingLocation(), bA.getDataType());
        stA.setResearchProject(testResearchProject);

        List<Object[]> testCases = new ArrayList<>();
        testCases.add(new Object[]{"TEST-1", Collections.singletonList(bA), Collections.emptyList(), true});
        testCases.add(new Object[]{"TEST-2", Arrays.asList(bA, bApicard), Collections.emptyList(), true});
        testCases.add(new Object[]{"TEST-3", Collections.singletonList(bA), Collections.singletonList(stA), false});
        testCases.add(new Object[]{"TEST-4", Collections.singletonList(bA2), Collections.singletonList(stA), false});

        return testCases.iterator();
    }

    @Test(dataProvider = "manyDtosManyTrackers")
    public void testValidateSubmissionDtoVariations(String label, List<SubmissionDto> submissionDTOs,
                                                    List<SubmissionTracker> submissionTrackers, boolean willPass) {
        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);
        setupSubmissionTrackerMock(submissionTrackerDao, submissionTrackers);
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao, null);

        try {
            researchProjectEjb.validateSubmissionDto(submissionDTOs);
            if (!willPass) {
                Assert.fail(String.format("ValidationException Expected on %s", label));
            }
        } catch (ValidationException e) {
            if (willPass) {
                Assert.fail(String.format("Expected ValidationException on %s", label), e);
            }
        }
        verifySubmissionTrackerMock(submissionTrackerDao);
    }

    public void testValidateSubmissionsDtoDiffersTupleEqual() throws Exception {
        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, TEST_SAMPLE_1, ON_PREM, TEST_VERSION_1);
        submissionDto.setUuid("Diff't Uuid");
        SubmissionDto submissionDto2 = getSubmissionDto(dummyProductOrder, TEST_SAMPLE_1, ON_PREM, TEST_VERSION_1);

        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao, null);

        try {
            researchProjectEjb.validateSubmissionDto(Arrays.asList(submissionDto, submissionDto2));
            Assert.fail(
                    "Since the tuples for these two SubmissionDTO's should be equal, an exception should have been thrown.");
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains(submissionDto.getSubmissionTuple().toString()));
        }
    }

    public void testValidateSubmissionsDtoEqual() throws Exception {
        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, TEST_SAMPLE_1, ON_PREM, TEST_VERSION_1);
        SubmissionDto submissionDto2 = getSubmissionDto(dummyProductOrder, TEST_SAMPLE_1, ON_PREM, TEST_VERSION_1);

        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao, null);
        try {
            researchProjectEjb.validateSubmissionDto(Arrays.asList(submissionDto, submissionDto2));
            Assert.fail("You should not be able to submit two duplicate submissions.");
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains(submissionDto.getSubmissionTuple().toString()));
        }
    }

    @DataProvider
    public Iterator<Object[]> updateSubmissionDtoFromResultsProvider() {
        String location = "aLocation";
        String type = "aType";
        String errorMessage = "a error";
        final String myUuid = "myUuid";
        final String myOtherUuid = "myOtherUuid";

        SubmissionTracker testTracker = SubmissionTrackerTestFactory.getTracker(myUuid, TEST_SAMPLE_1, ON_PREM, TEST_VERSION_1,
            SubmissionLibraryDescriptor.WHOLE_GENOME.getName());

        SubmissionTracker testTracker2 = SubmissionTrackerTestFactory.getTracker(myOtherUuid, TEST_SAMPLE_2, ON_PREM, TEST_VERSION_1,
            SubmissionLibraryDescriptor.WHOLE_GENOME.getName());

        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, TEST_SAMPLE_1, ON_PREM, TEST_VERSION_1);
        SubmissionDto submissionDto2 = getSubmissionDto(dummyProductOrder, TEST_SAMPLE_2, ON_PREM, TEST_VERSION_1);

        Map<SubmissionTracker, SubmissionDto> submisionDtoMap = new HashMap<>();
        submisionDtoMap.put(testTracker, submissionDto);
        submisionDtoMap.put(testTracker2, submissionDto2);

        Map<String, SubmissionTracker> idToTracker = SubmissionTracker.uuidMap(Arrays.asList(testTracker, testTracker2));

        List<Object[]> testCases = new ArrayList<>();
        testCases.add(new Object[]{testTracker.getResearchProject(), Collections.singleton(
            new SubmissionStatusDetailBean(myUuid, SubmissionStatusDetailBean.Status.FAILURE, location, type, new Date(),
                errorMessage)), submisionDtoMap, idToTracker, String.format("%s: %s", TEST_SAMPLE_1, errorMessage), 0, 1});

        // Tests case where mercury persists but e9 fails.
        testCases.add(new Object[]{testTracker2.getResearchProject(), Collections.singleton(
            new SubmissionStatusDetailBean(null, SubmissionStatusDetailBean.Status.FAILURE, location, type, new Date(),
                errorMessage)), submisionDtoMap, idToTracker, errorMessage, 1, 2});

        return testCases.iterator();
    }

    /**
     * Tests an oddball case where E9 returns an error with no sample id.
     */
    @Test(dataProvider = "updateSubmissionDtoFromResultsProvider")
    public void testUpdateSubmissionDtoFromResults(ResearchProject testResearchProject,
                                                   Collection<SubmissionStatusDetailBean> results,
                                                   Map<SubmissionTracker, SubmissionDto> submisionDtoMap,
                                                   Map<String, SubmissionTracker> idToTracker,
                                                   String resultErrorMessage, int expectedTrackerCountInResearchProject,
                                                   int expectedErrorCount) {
        SubmissionsService submissionsService = Mockito.mock(SubmissionsService.class);
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(null, submissionsService);
        assertThat(testResearchProject.getSubmissionTrackers(), hasSize(1));
        List<String> errors = new ArrayList<>();
        researchProjectEjb.updateSubmissionDtoStatusFromResults(testResearchProject, submisionDtoMap, results,
            idToTracker, errors);

        assertThat(errors, hasSize(expectedErrorCount));
        assertThat(testResearchProject.getSubmissionTrackers(), hasSize(expectedTrackerCountInResearchProject));
        assertThat(errors.iterator().next(), equalTo(resultErrorMessage));
    }

    private ResearchProjectEjb getResearchProjectEjb(SubmissionTrackerDao submissionTrackerDao,
                                                     SubmissionsService submissionsService) {
        return new ResearchProjectEjb(null, null, null, null, null, null, submissionsService, submissionTrackerDao);
    }

    private SubmissionDto getSubmissionDto(ProductOrder productOrder, String sample, String processingLocation, int version) {
        Aggregation aggregation = AggregationTestFactory
            .buildAggregation(productOrder.getResearchProject().getBusinessKey(), productOrder.getJiraTicketKey(),
                sample,
                version, null, null, null, null, null, null, processingLocation);

        SubmissionDto submissionDto = new SubmissionDto(aggregation, null);
        return submissionDto;
    }

    private void setupSubmissionTrackerMock(SubmissionTrackerDao submissionTrackerDao,
                                            List<SubmissionTracker> submissionTrackers) {
        Mockito.when(submissionTrackerDao.findSubmissionTrackers(
                Mockito.anyCollectionOf(SubmissionDto.class))).thenReturn(submissionTrackers);
    }

    private void verifySubmissionTrackerMock(SubmissionTrackerDao submissionTrackerDao) {
        Mockito.verify(submissionTrackerDao, Mockito.times(1)).
                findSubmissionTrackers(Mockito.anyCollectionOf(SubmissionDto.class));
    }
}
