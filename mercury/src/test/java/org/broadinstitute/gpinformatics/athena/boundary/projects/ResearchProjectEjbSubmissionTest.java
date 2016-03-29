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

@Test(groups = TestGroups.DATABASE_FREE)
public class ResearchProjectEjbSubmissionTest {
    private static String TEST_SAMPLE_1 = String.format("%d_E", System.currentTimeMillis());
    private static final int TEST_VERSION_1 = 1;
    private static final String PDO_99999 = "PDO-99999";

    public void testValidateSubmissionsDtoHasNullsDto() throws Exception {
        SubmissionDto submissionDto = new SubmissionDto(null,null,null,null);

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
        ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder(1, PDO_99999);
        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);
        Map<BassDTO.BassResultColumn, String> bassInfo = new HashMap<>();
        bassInfo.put(BassDTO.BassResultColumn.file_type, BassFileType.BAM.getBassValue());
        bassInfo.put(BassDTO.BassResultColumn.version, String.valueOf(9));
        bassInfo.put(BassDTO.BassResultColumn.sample, "ABC1234");

        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, bassInfo);

        Mockito.when(submissionTrackerDao
                .findSubmissionTrackers(Mockito.anyString(), Mockito.anyCollectionOf(SubmissionDto.class)))
                .thenReturn(Collections.<SubmissionTracker>emptyList());

        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto(dummyProductOrder.getResearchProject().getJiraTicketKey(),
                    Collections.singletonList(submissionDto));
        } catch (Exception e) {
            Assert.fail("A call to submissionTrackerDao.findSubmissionTrackers returning an empty list, " +
                        "should not have caused a submission failure.", e);
        }

        verifySubmissionTrackerMock(submissionTrackerDao);
    }


    private void verifySubmissionTrackerMock(SubmissionTrackerDao submissionTrackerDao) {
        Mockito.verify(submissionTrackerDao, Mockito.times(1)).
                findSubmissionTrackers(Mockito.anyString(), Mockito.anyCollectionOf(SubmissionDto.class));
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
        ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder(1, PDO_99999);
        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);
        Map<BassDTO.BassResultColumn, String> bassInfo = getBassResultMap();
        bassInfo.put(BassDTO.BassResultColumn.path, bassFilename);
        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, bassInfo);
        SubmissionTracker submissionTracker = getSubmissionTracker(submissionDto);
        submissionTracker.setFileName(trackerFileName);

        Mockito.when(submissionTrackerDao
                .findSubmissionTrackers(Mockito.anyString(), Mockito.anyCollectionOf(SubmissionDto.class)))
                .thenReturn(Collections.singletonList(submissionTracker));

        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto(dummyProductOrder.getResearchProject().getJiraTicketKey(),
                    Collections.singletonList(submissionDto));
            Assert.fail(String.format(
                    "submissionTracker returned %s and bass returned %s This should not have been allowed",
                    trackerFileName, bassFilename));
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains(submissionTracker.getTuple().toString()));
        }
        verifySubmissionTrackerMock(submissionTrackerDao);
    }

    @DataProvider(name = "fileTypeDataProvider")
    public Iterator<Object[]> fileTypeDataProvider() {
        List<Object[]> testCases = new ArrayList<>();
        String exceptionMessage = String.format("[{sampleName = %s; fileType = BAM; version = 1}]", TEST_SAMPLE_1);
        testCases.add(new Object[]{BassFileType.BAM, BassFileType.BAM, exceptionMessage});
        testCases.add(new Object[]{BassFileType.BAM, BassFileType.PICARD, ""});
        return testCases.iterator();
    }

    @Test(dataProvider = "fileTypeDataProvider")
    public void testFileTypeVariations(BassFileType trackerFileType, BassFileType bassFileType, String exceptionMessage)
            throws Exception {

        // data setup for dto result.
        ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder(1, PDO_99999);
        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);

        Map<BassDTO.BassResultColumn, String> bassInfo = getBassResultMap();
        bassInfo.put(BassDTO.BassResultColumn.file_type, getFileTypeValue(trackerFileType));
        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, bassInfo);
        SubmissionTracker submissionTracker = getSubmissionTracker(submissionDto);

        Mockito.when(submissionTrackerDao
                .findSubmissionTrackers(Mockito.anyString(), Mockito.anyCollectionOf(SubmissionDto.class)))
                .thenReturn(Collections.singletonList(submissionTracker));

        // data setup for new submission request.
        Map<BassDTO.BassResultColumn, String> submissionBassInfo = getBassResultMap();
        submissionBassInfo.put(BassDTO.BassResultColumn.file_type, getFileTypeValue(bassFileType));
        SubmissionDto newSubmissionDto = getSubmissionDto(dummyProductOrder, submissionBassInfo);
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto(dummyProductOrder.getResearchProject().getJiraTicketKey(),
                    Collections.singletonList(newSubmissionDto));

            Assert.assertTrue(exceptionMessage.isEmpty(), String.format(
                    "ValidationException was expected: submissionTracker returned %s and bass returned %s.",
                    trackerFileType.toString(), bassFileType.toString()));
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains(exceptionMessage));
        }

        verifySubmissionTrackerMock(submissionTrackerDao);
    }

    public void testFileTypeVariationNoTrackerFileType() throws Exception {
        String bassFileType = BassFileType.BAM.getBassValue();

        // data setup for dao result.
        ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder(1, PDO_99999);
        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);

        Map<BassDTO.BassResultColumn, String> bassInfo = getBassResultMap();
        bassInfo.put(BassDTO.BassResultColumn.file_type, null);
        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, bassInfo);
        SubmissionTracker submissionTracker =
                new SubmissionTracker(submissionDto.getSampleName(), null, String.valueOf(submissionDto.getVersion()));

        Mockito.when(submissionTrackerDao
                .findSubmissionTrackers(Mockito.anyString(), Mockito.anyCollectionOf(SubmissionDto.class)))
                .thenReturn(Collections.singletonList(submissionTracker));

        // data setup for new submission request.
        Map<BassDTO.BassResultColumn, String> submissionBassInfo = getBassResultMap();
        submissionBassInfo.put(BassDTO.BassResultColumn.file_type, bassFileType);
        SubmissionDto newSubmissionDto = getSubmissionDto(dummyProductOrder, submissionBassInfo);
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto(dummyProductOrder.getResearchProject().getJiraTicketKey(),
                    Collections.singletonList(newSubmissionDto));
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Null value not allowed."));
        }

        verifySubmissionTrackerMock(submissionTrackerDao);
    }

    public void testFileTypeVariationNoBassDTOFileType() throws Exception {
        // data setup for dao result.
        ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder(1, PDO_99999);
        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);

        Map<BassDTO.BassResultColumn, String> bassInfo = getBassResultMap();
        bassInfo.put(BassDTO.BassResultColumn.file_type, BassFileType.BAM.getBassValue());
        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, bassInfo);
        SubmissionTracker submissionTracker =
                new SubmissionTracker(submissionDto.getSampleName(), BassFileType.BAM,
                        String.valueOf(submissionDto.getVersion()));

        Mockito.when(submissionTrackerDao
                .findSubmissionTrackers(Mockito.anyString(), Mockito.anyCollectionOf(SubmissionDto.class)))
                .thenReturn(Collections.singletonList(submissionTracker));

        // data setup for submission request.
        Map<BassDTO.BassResultColumn, String> submissionBassInfo = getBassResultMap();
        submissionBassInfo.put(BassDTO.BassResultColumn.file_type, null);
        SubmissionDto newSubmissionDto = getSubmissionDto(dummyProductOrder, submissionBassInfo);
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto(dummyProductOrder.getResearchProject().getJiraTicketKey(),
                    Collections.singletonList(newSubmissionDto));
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("No enum constant for"));
        }
    }

    private String getFileTypeValue(BassFileType trackerFileType) {
        if (trackerFileType != null) {
            return trackerFileType.getBassValue();
        }
        return null;
    }

    public void testMultipleResultsFromDaoAlreadySubmitted()
            throws Exception {

        // data setup for dto result.
        ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder(1, PDO_99999);
        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);

        Map<BassDTO.BassResultColumn, String> bassInfo = getBassResultMap();
        bassInfo.put(BassDTO.BassResultColumn.file_type, getFileTypeValue(BassFileType.BAM));
        bassInfo.put(BassDTO.BassResultColumn.path, "/b/file.bam");
        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, bassInfo);
        SubmissionTracker submissionTrackerResult1 = getSubmissionTracker(submissionDto);

        Map<BassDTO.BassResultColumn, String> bassInfo2 = getBassResultMap();
        bassInfo2.put(BassDTO.BassResultColumn.file_type, getFileTypeValue(BassFileType.BAM));
        bassInfo2.put(BassDTO.BassResultColumn.path, "/a/file.bam");
        SubmissionDto submissionDto2 = getSubmissionDto(dummyProductOrder, bassInfo2);
        SubmissionTracker submissionTrackerResult2 = getSubmissionTracker(submissionDto2);

        Mockito.when(submissionTrackerDao
                .findSubmissionTrackers(Mockito.anyString(), Mockito.anyCollectionOf(SubmissionDto.class)))
                .thenReturn(Arrays.asList(submissionTrackerResult1, submissionTrackerResult2));

        // data setup for new submission request.
        Map<BassDTO.BassResultColumn, String> submissionBassInfo = getBassResultMap();
        submissionBassInfo.put(BassDTO.BassResultColumn.file_type, getFileTypeValue(BassFileType.BAM));
        SubmissionDto newSubmissionDto = getSubmissionDto(dummyProductOrder, submissionBassInfo);
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto(dummyProductOrder.getResearchProject().getJiraTicketKey(),
                    Collections.singletonList(newSubmissionDto));

            Assert.fail(String.format(
                    "ValidationException was expected: submissionTrackerResult1 returned %s and bass returned %s."));
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains(newSubmissionDto.getBassDTO().getTuple().toString()));
        }

        verifySubmissionTrackerMock(submissionTrackerDao);
    }

    public void testMultipleResultsFromDaoNewSubmissionDtoNotYetSubmitted()
            throws Exception {

        // data setup for dto result.
        ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder(1, PDO_99999);
        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);

        Map<BassDTO.BassResultColumn, String> bassInfo = getBassResultMap();
        bassInfo.put(BassDTO.BassResultColumn.file_type, getFileTypeValue(BassFileType.BAM));
        bassInfo.put(BassDTO.BassResultColumn.path, "/b/file.bam");
        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, bassInfo);
        SubmissionTracker submissionTrackerResult1 = getSubmissionTracker(submissionDto);

        Map<BassDTO.BassResultColumn, String> bassInfo2 = getBassResultMap();
        bassInfo2.put(BassDTO.BassResultColumn.file_type, getFileTypeValue(BassFileType.BAM));
        bassInfo2.put(BassDTO.BassResultColumn.path, "/a/file.bam");
        SubmissionDto submissionDto2 = getSubmissionDto(dummyProductOrder, bassInfo2);
        SubmissionTracker submissionTrackerResult2 = getSubmissionTracker(submissionDto2);

        Mockito.when(submissionTrackerDao
                .findSubmissionTrackers(Mockito.anyString(), Mockito.anyCollectionOf(SubmissionDto.class)))
                .thenReturn(Arrays.asList(submissionTrackerResult1, submissionTrackerResult2));

        // data setup for new submission request.
        Map<BassDTO.BassResultColumn, String> submissionBassInfo = getBassResultMap();
        submissionBassInfo.put(BassDTO.BassResultColumn.file_type, getFileTypeValue(BassFileType.PICARD));
        SubmissionDto newSubmissionDto = getSubmissionDto(dummyProductOrder, submissionBassInfo);
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto(dummyProductOrder.getResearchProject().getJiraTicketKey(),
                    Collections.singletonList(newSubmissionDto));
        } catch (Exception e) {
            Assert.fail(String.format("The DTO has a different file type and should have been accepted. (%s)",
                    newSubmissionDto.getBassDTO().getTuple().toString()));
        }

        verifySubmissionTrackerMock(submissionTrackerDao);
    }

    public void testValidateSubmissionsDtoDiffersTupleEqual() throws Exception {
        ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder(1, PDO_99999);

        Map<BassDTO.BassResultColumn, String> bassInfo = getBassResultMap();
        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, bassInfo);

        bassInfo = new HashMap<>();
        bassInfo.put(BassDTO.BassResultColumn.path, "/another/path/testFile1.bam");
        bassInfo.put(BassDTO.BassResultColumn.gssr_barcode, "8675309");
        bassInfo.put(BassDTO.BassResultColumn.file_type, BassFileType.BAM.getBassValue());
        bassInfo.put(BassDTO.BassResultColumn.version, String.valueOf(TEST_VERSION_1));
        bassInfo.put(BassDTO.BassResultColumn.sample, TEST_SAMPLE_1);

        SubmissionDto submissionDto2 = getSubmissionDto(dummyProductOrder, bassInfo);

        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(null);

        try {
            researchProjectEjb.validateSubmissionDto(dummyProductOrder.getResearchProject().getJiraTicketKey(),
                    Arrays.asList(submissionDto, submissionDto2));
            Assert.fail(
                    "Since the tuples for these two BassDTOs should be equal, an exception should have been thrown.");
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains(submissionDto.getBassDTO().getTuple().toString()));
        }
    }

    public void testValidateSubmissionsDtoEqual() throws Exception {
        ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder(1, PDO_99999);

        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, getBassResultMap());
        SubmissionDto submissionDto2 = getSubmissionDto(dummyProductOrder, getBassResultMap());
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(null);
        try {
            researchProjectEjb.validateSubmissionDto(dummyProductOrder.getResearchProject().getJiraTicketKey(),
                    Arrays.asList(submissionDto, submissionDto2));
            Assert.fail("You should not be able to submit two duplicate submissions.");
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains(submissionDto.getBassDTO().getTuple().toString()));
        }
    }

    private SubmissionTracker getSubmissionTracker(SubmissionDto submissionDto) {
        return new SubmissionTracker(submissionDto.getSampleName(), submissionDto.getFileTypeEnum(),
                String.valueOf(submissionDto.getVersion()));
    }

    private ResearchProjectEjb getResearchProjectEjb(SubmissionTrackerDao submissionTrackerDao) {
        return new ResearchProjectEjb(null, null, null, null, null, null, null, submissionTrackerDao);
    }

    private SubmissionDto getSubmissionDto(ProductOrder productOrder, Map<BassDTO.BassResultColumn, String> bassInfo) {

        return new SubmissionDto(new BassDTO(bassInfo), null, Collections.singletonList(productOrder), null);
    }

    private Map<BassDTO.BassResultColumn, String> getBassResultMap() {
        Map<BassDTO.BassResultColumn, String> bassInfo = new HashMap<>();
        bassInfo.put(BassDTO.BassResultColumn.path, "/your/path/testFile1.bam");
        bassInfo.put(BassDTO.BassResultColumn.file_type, BassFileType.BAM.getBassValue());
        bassInfo.put(BassDTO.BassResultColumn.version, String.valueOf(TEST_VERSION_1));
        bassInfo.put(BassDTO.BassResultColumn.sample, TEST_SAMPLE_1);

        return bassInfo;
    }

}
