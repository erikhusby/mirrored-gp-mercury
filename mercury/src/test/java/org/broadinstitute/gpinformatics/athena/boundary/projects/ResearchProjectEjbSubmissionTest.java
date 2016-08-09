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
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassFileType;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDto;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO.BassResultColumn.path;

@Test(groups = TestGroups.DATABASE_FREE)
public class ResearchProjectEjbSubmissionTest {
    private static final String DEFAULT_FILE_PATH = "/some/file.bam";
    private static final int TEST_VERSION_2 = 2;
    private static String TEST_SAMPLE_1 = String.format("%d_E", System.currentTimeMillis());
    private static final int TEST_VERSION_1 = 1;
    private static final String PDO_99999 = "PDO-99999";
    private ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder(1, PDO_99999);

    @SuppressWarnings("ConstantConditions")
    public void testValidateSubmissionsDtoHasNullsDto() throws Exception {
        SubmissionDto submissionDto = new SubmissionDto(null, null, null, null);

        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(null);

        try {
            researchProjectEjb.validateSubmissionDto(PDO_99999, Collections.singletonList(submissionDto));
            Assert.fail("The data sources for this submissionDTO are all null, why was an exception not thrown?");
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains("No data was found in submission request."));
        }
    }

    public void testValidateSubmissionsEmptyDtoList() throws Exception {
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(null);
        try {
            researchProjectEjb.validateSubmissionDto(PDO_99999, Collections.<SubmissionDto>emptyList());
            Assert.fail("Since a list of empty submissionDTOs was passed in, an exception should have ben thrown.");
        } catch (InformaticsServiceException e) {
            Assert.assertTrue(e.getMessage().equals("At least one selection is needed to post submissions"));
        }
    }

    public void testValidateSubmissionsDtoWithNoDaoResultPass() {
        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);
        setupSubmissionTrackerMock(submissionTrackerDao, Collections.<SubmissionTracker>emptyList());
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        SubmissionDto submissionDto = getSubmissionDto("ABC1234", BassFileType.BAM, 9);

        try {
            researchProjectEjb.validateSubmissionDto("RP-1234", Collections.singletonList(submissionDto));
        } catch (Exception e) {
            Assert.fail("A call to submissionTrackerDao.findSubmissionTrackers returning an empty list, " +
                        "should not have caused a submission failure.", e);
        }

        verifySubmissionTrackerMock(submissionTrackerDao);
    }


    public void testFileTypeVariationNoBassDTOFileType() throws Exception {
        // data setup for dao result.
        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, BassFileType.BAM, "/some/file");
        SubmissionTracker submissionTracker = new SubmissionTracker(submissionDto.getSampleName(), "P123",
                BassFileType.BAM, String.valueOf(submissionDto.getVersion()));

        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);
        setupSubmissionTrackerMock(submissionTrackerDao, Collections.singletonList(submissionTracker));

        // data setup for submission request.
        SubmissionDto newSubmissionDto = getSubmissionDto(dummyProductOrder, null, "/some/file");
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto("RP-1234", Collections.singletonList(newSubmissionDto));
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("No enum constant for"));
        }
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    @DataProvider(name = "manyDtosManyTrackers")
    public Iterator<Object[]> manyDtosManyTrackers() {
        SubmissionDto bA = getSubmissionDto("A", BassFileType.BAM, TEST_VERSION_1);
        SubmissionDto bA2 = getSubmissionDto("A", BassFileType.BAM, TEST_VERSION_2);
        SubmissionDto bApicard = getSubmissionDto("A", BassFileType.PICARD, TEST_VERSION_1);

        SubmissionTracker stA =
                new SubmissionTracker(bA.getSampleName(), "P123", bA.getFileType(), String.valueOf(bA.getVersion()));

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
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto(PDO_99999, submissionDTOs);
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
        SubmissionDto submissionDto =
                getSubmissionDto(TEST_SAMPLE_1, BassFileType.BAM, TEST_VERSION_1);

        Map<BassDTO.BassResultColumn, String> bassInfo = new HashMap<>(submissionDto.getBassDTO().getColumnToValue());
        bassInfo.put(path, "not"+DEFAULT_FILE_PATH);
        bassInfo.put(BassDTO.BassResultColumn.gssr_barcode, "8675309");

        SubmissionDto submissionDto2 = getSubmissionDto(dummyProductOrder, bassInfo);

        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto("RP-1234", Arrays.asList(submissionDto, submissionDto2));
            Assert.fail(
                    "Since the tuples for these two BassDTOs should be equal, an exception should have been thrown.");
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains(submissionDto.getBassDTO().getTuple().toString()));
        }
    }

    public void testValidateSubmissionsDtoEqual() throws Exception {
        SubmissionDto submissionDto =
                getSubmissionDto(TEST_SAMPLE_1, BassFileType.BAM, TEST_VERSION_1);
        SubmissionDto submissionDto2 =
                getSubmissionDto(TEST_SAMPLE_1, BassFileType.BAM, TEST_VERSION_1);

        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);
        try {
            researchProjectEjb.validateSubmissionDto("RP-1234", Arrays.asList(submissionDto, submissionDto2));
            Assert.fail("You should not be able to submit two duplicate submissions.");
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains(submissionDto.getBassDTO().getTuple().toString()));
        }
    }

    private ResearchProjectEjb getResearchProjectEjb(SubmissionTrackerDao submissionTrackerDao) {
        return new ResearchProjectEjb(null, null, null, null, null, null, null, submissionTrackerDao);
    }

    private SubmissionDto getSubmissionDto(ProductOrder productOrder, Map<BassDTO.BassResultColumn, String> bassInfo) {
        return new SubmissionDto(new BassDTO(bassInfo), null, Collections.singleton(productOrder), null);
    }

    private SubmissionDto getSubmissionDto(ProductOrder productOrder, BassFileType fileType, String bassFilePath) {
        Map<BassDTO.BassResultColumn, String> bassInfo = new HashMap<>();
        bassInfo.put(path, bassFilePath);
        bassInfo.put(BassDTO.BassResultColumn.file_type, fileType == null ? null : fileType.getBassValue());
        bassInfo.put(BassDTO.BassResultColumn.version, String.valueOf(TEST_VERSION_1));
        bassInfo.put(BassDTO.BassResultColumn.sample, TEST_SAMPLE_1);

        return new SubmissionDto(new BassDTO(bassInfo), null,
                Collections.singletonList(productOrder), null);
    }

    @SuppressWarnings("serial")
    public SubmissionDto getSubmissionDto(final String sampleName,
                                          final BassFileType fileType, final int version) {
        ProductOrder productOrder = ProductOrderTestFactory.createDummyProductOrder(1, PDO_99999);
        return getSubmissionDto(productOrder, new HashMap<BassDTO.BassResultColumn, String>() {{
                    put(BassDTO.BassResultColumn.sample, sampleName);
                    put(BassDTO.BassResultColumn.file_type, fileType == null ? null : fileType.getBassValue());
                    put(BassDTO.BassResultColumn.version, String.valueOf(version));
                }}
        );
    }

    private void setupSubmissionTrackerMock(SubmissionTrackerDao submissionTrackerDao,
                                            List<SubmissionTracker> submissionTrackers) {
        Mockito.when(submissionTrackerDao.findSubmissionTrackers(Mockito.anyString(),
                Mockito.anyCollectionOf(SubmissionDto.class))).thenReturn(submissionTrackers);
    }

    private void verifySubmissionTrackerMock(SubmissionTrackerDao submissionTrackerDao) {
        Mockito.verify(submissionTrackerDao, Mockito.times(1)).
                findSubmissionTrackers(Mockito.anyString(), Mockito.anyCollectionOf(SubmissionDto.class));
    }
}
