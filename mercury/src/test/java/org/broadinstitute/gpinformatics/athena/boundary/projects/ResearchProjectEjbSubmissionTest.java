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

import org.apache.commons.lang3.StringUtils;
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

    public void testValidateSubmissionsEmtpyDtoList() throws Exception {
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

        SubmissionDto submissionDto = getSubmissionDto("ABC1234", BassFileType.BAM, 9, DEFAULT_FILE_PATH);

        try {
            researchProjectEjb.validateSubmissionDto("RP-1234", Collections.singletonList(submissionDto));
        } catch (Exception e) {
            Assert.fail("A call to submissionTrackerDao.findSubmissionTrackers returning an empty list, " +
                        "should not have caused a submission failure.", e);
        }

        verifySubmissionTrackerMock(submissionTrackerDao);
    }

    @DataProvider(name = "fileNameDataProvider")
    public Iterator<Object[]> fileNameDataProvider() {
        List<Object[]> testCases = new ArrayList<>();
        testCases.add(new Object[]{"file a", "file a"});
        testCases.add(new Object[]{"file a", "file b"});
        testCases.add(new Object[]{"", "file a"});
        testCases.add(new Object[]{null, "file a"});
        testCases.add(new Object[]{null, null});
        testCases.add(new Object[]{"", ""});

        return testCases.iterator();
    }

    @Test(dataProvider = "fileNameDataProvider")
    public void testFileNameVariations(String trackerFileName, String bassFilename) {
        SubmissionDto submissionDto =
                getSubmissionDto(TEST_SAMPLE_1, BassFileType.BAM, TEST_VERSION_1, trackerFileName);
        SubmissionTracker submissionTracker = getSubmissionTracker(submissionDto);
        submissionTracker.setFileName(trackerFileName);

        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);
        setupSubmissionTrackerMock(submissionTrackerDao, Collections.singletonList(submissionTracker));

        SubmissionDto newSubmissionDto =
                getSubmissionDto(TEST_SAMPLE_1, BassFileType.BAM, TEST_VERSION_1, bassFilename);
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto("RP-1234", Collections.singletonList(newSubmissionDto));
            Assert.fail(String.format(
                    "submissionTracker returned %s and bass returned %s This should not have been allowed",
                    trackerFileName, bassFilename));
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains(submissionTracker.getTuple().toString()));
        }
        verifySubmissionTrackerMock(submissionTrackerDao);
    }

    public void testBassSameAsBass() throws Exception {
        BassFileType trackerFileType = BassFileType.BAM;
        BassFileType bassFileType = BassFileType.BAM;

        // data setup for dto result.
        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);

        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, trackerFileType, "/some/file");
        SubmissionTracker submissionTracker = getSubmissionTracker(submissionDto);

        setupSubmissionTrackerMock(submissionTrackerDao, Collections.singletonList(submissionTracker));

        // data setup for new submission request.
        SubmissionDto newSubmissionDto = getSubmissionDto(dummyProductOrder, bassFileType, "/some/file");
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto("RP-1234", Collections.singletonList(newSubmissionDto));

        } catch (Exception e) {
            String exceptionMessage = String.format("[{sampleName = %s; fileType = BAM; version = 1}]", TEST_SAMPLE_1);
            Assert.assertTrue(e.getMessage().contains(exceptionMessage));
        }

        verifySubmissionTrackerMock(submissionTrackerDao);
    }

    public void testFileTypeVariationNoBassDTOFileType() throws Exception {
        // data setup for dao result.
        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, BassFileType.BAM, "/some/file");
        SubmissionTracker submissionTracker = new SubmissionTracker(submissionDto.getSampleName(), BassFileType.BAM,
                        String.valueOf(submissionDto.getVersion()));

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
        String anotherFilename = "/not" + DEFAULT_FILE_PATH;
        SubmissionDto bA = getSubmissionDto("A", BassFileType.BAM, TEST_VERSION_1, DEFAULT_FILE_PATH);
        SubmissionDto bB = getSubmissionDto("A", BassFileType.BAM, TEST_VERSION_1, anotherFilename);
        SubmissionDto bA2 = getSubmissionDto("A", BassFileType.BAM, TEST_VERSION_2, DEFAULT_FILE_PATH);
        SubmissionDto bApicard = getSubmissionDto("A", BassFileType.PICARD, TEST_VERSION_1, DEFAULT_FILE_PATH);

        SubmissionTracker stA =
                new SubmissionTracker(bA.getSampleName(), bA.getFileTypeEnum(), String.valueOf(bA.getVersion()));
        stA.setFileName(bA.getFileName());

        SubmissionTracker stB =
                new SubmissionTracker(bB.getSampleName(), bB.getFileTypeEnum(), String.valueOf(bB.getVersion()));
        stB.setFileName(bB.getFileName());

        SubmissionTracker stA2 =
                new SubmissionTracker(bA2.getSampleName(), bA2.getFileTypeEnum(), String.valueOf(bA2.getVersion()));
        stA2.setFileName(bA2.getFileName());

        SubmissionTracker stBa2_NullFileType =
                new SubmissionTracker(bA2.getSampleName(), null, String.valueOf(bA2.getVersion()));
        stA2.setFileName(bA2.getFileName());

        List<Object[]> testCases = new ArrayList<>();
        testCases.add(new Object[]{"TEST-1", Arrays.asList(bA), Collections.emptyList(), true});
        testCases.add(new Object[]{"TEST-1", Arrays.asList(bA, bApicard), Collections.emptyList(), true});
        testCases.add(new Object[]{"TEST-2", Arrays.asList(bA), Arrays.asList(stA), false});
        testCases.add(new Object[]{"TEST-3", Arrays.asList(bA), Arrays.asList(stB), false});
        testCases.add(new Object[]{"TEST-4", Arrays.asList(bA2), Arrays.asList(stA), false});
        testCases.add(new Object[]{"TEST-5", Arrays.asList(bA2), Arrays.asList(stBa2_NullFileType), false});

        return testCases.iterator();
    }

    @Test(dataProvider = "manyDtosManyTrackers")
    public void testValidateSubmissionDtoVariations(String label, List<SubmissionDto> submisisonDTOs,
                                                    List<SubmissionTracker> submissionTrackers, boolean willPass) {
        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);
        setupSubmissionTrackerMock(submissionTrackerDao, submissionTrackers);
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto(PDO_99999, submisisonDTOs);
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

    public void testMultipleResultsFromDaoAlreadySubmitted() throws Exception {

        // data setup for dto result.
        SubmissionDto submissionDto = getSubmissionDto(TEST_SAMPLE_1, BassFileType.BAM, TEST_VERSION_1, "/b/file.bam");
        SubmissionTracker submissionTrackerResult1 = getSubmissionTracker(submissionDto);

        SubmissionDto submissionDto2 = getSubmissionDto(TEST_SAMPLE_1, BassFileType.BAM, TEST_VERSION_1, "/a/file.bam");
        SubmissionTracker submissionTrackerResult2 = getSubmissionTracker(submissionDto2);

        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);
        setupSubmissionTrackerMock(submissionTrackerDao,
                Arrays.asList(submissionTrackerResult1, submissionTrackerResult2));

        // data setup for new submission request.
        SubmissionDto newSubmissionDto =
                getSubmissionDto(TEST_SAMPLE_1, BassFileType.BAM, TEST_VERSION_1, "/b/file.bam");
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto("RP-1234", Collections.singletonList(newSubmissionDto));

            Assert.fail(String.format(
                    "ValidationException was expected: submissionTrackerResult1 returned %s and bass returned %s.",
                    submissionTrackerResult1.getTuple(), newSubmissionDto.getBassDTO().getTuple()));
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains(newSubmissionDto.getBassDTO().getTuple().toString()));
        }

        verifySubmissionTrackerMock(submissionTrackerDao);
    }

    public void testValidateSubmissionsDtoDiffersTupleEqual() throws Exception {
        SubmissionDto submissionDto =
                getSubmissionDto(TEST_SAMPLE_1, BassFileType.BAM, TEST_VERSION_1, DEFAULT_FILE_PATH);

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
                getSubmissionDto(TEST_SAMPLE_1, BassFileType.BAM, TEST_VERSION_1, DEFAULT_FILE_PATH);
        SubmissionDto submissionDto2 =
                getSubmissionDto(TEST_SAMPLE_1, BassFileType.BAM, TEST_VERSION_1, DEFAULT_FILE_PATH);

        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);
        try {
            researchProjectEjb.validateSubmissionDto("RP-1234", Arrays.asList(submissionDto, submissionDto2));
            Assert.fail("You should not be able to submit two duplicate submissions.");
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains(submissionDto.getBassDTO().getTuple().toString()));
        }
    }

    private SubmissionTracker getSubmissionTracker(SubmissionDto submissionDto) {
        BassFileType bassFileType =
                StringUtils.isBlank(submissionDto.getFileType()) ? null : submissionDto.getFileTypeEnum();
        return new SubmissionTracker(submissionDto.getSampleName(), bassFileType,
                String.valueOf(submissionDto.getVersion()));
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

    public SubmissionDto getSubmissionDto(final String sampleName,
                                           final BassFileType fileType, final int version, final String path) {
        ProductOrder productOrder = ProductOrderTestFactory.createDummyProductOrder(1, PDO_99999);
        return getSubmissionDto(productOrder, new HashMap<BassDTO.BassResultColumn, String>() {{
                    put(BassDTO.BassResultColumn.sample, sampleName);
                    put(BassDTO.BassResultColumn.file_type, fileType == null ? null : fileType.getBassValue());
                    put(BassDTO.BassResultColumn.version, String.valueOf(version));
                    put(BassDTO.BassResultColumn.path, path);
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
