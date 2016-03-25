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
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class ResearchProjectEjbSubmissionTest {
    private static String TEST_SAMPLE_1 = String.format("%d_E", System.currentTimeMillis());
    private static final int TEST_VERSION_1 = 1;
    private static final String PDO_99999 = "PDO-99999";

    public void testValidateSubmissionsAlreadySubmitted() throws Exception {
        ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder(1, PDO_99999);
        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);
        Map<BassDTO.BassResultColumn, String> bassInfo = getBassResultMap();

        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, bassInfo);
        SubmissionTracker submissionTracker = getSubmissionTracker(submissionDto);
        submissionTracker.setFileName("/i/am/a/file");

        Mockito.when(submissionTrackerDao
                .findSubmissionTrackers(Mockito.anyString(), Mockito.anyCollectionOf(SubmissionDto.class)))
                .thenReturn(Collections.singletonList(submissionTracker));

        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto(dummyProductOrder.getResearchProject().getJiraTicketKey(),
                    Collections.singletonList(submissionDto));
            Assert.fail("An exception should have ben thrown.");
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains(submissionTracker.getTuple().toString()));
        }
        verifySubmissionTrackerMock(submissionTrackerDao);
    }

    public void testValidateSubmissionsDtoHasNullsDto() throws Exception {
        SubmissionDto submissionDto = new SubmissionDto(null,null,null,null);

        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(null);

        try {
            researchProjectEjb.validateSubmissionDto(PDO_99999, Collections.singletonList(submissionDto));
            Assert.fail("An exception should have ben thrown.");
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains("No data was found in submission request."));
        }
    }

    public void testValidateSubmissionsEmtpyDtoList() throws Exception {
        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(null);
        try {
            researchProjectEjb.validateSubmissionDto(PDO_99999, Collections.<SubmissionDto>emptyList());
            Assert.fail("An exception should have ben thrown.");
        } catch (InformaticsServiceException e) {
            Assert.assertTrue(e.getMessage().equals("At least one selection is needed to post submissions"));
        }
    }


    public void testValidateSubmissions_PreviousSubmissionTrackerHasEmptyPath() throws Exception {
        ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder(1, PDO_99999);
        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);
        Map<BassDTO.BassResultColumn, String> bassInfo = getBassResultMap();

        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, bassInfo);
        SubmissionTracker submissionTracker = getSubmissionTracker(submissionDto);
        submissionTracker.setFileName("");

        Mockito.when(submissionTrackerDao
                .findSubmissionTrackers(Mockito.anyString(), Mockito.anyCollectionOf(SubmissionDto.class)))
                .thenReturn(Collections.singletonList(submissionTracker));

        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto(dummyProductOrder.getResearchProject().getJiraTicketKey(),
                    Collections.singletonList(submissionDto));
            Assert.fail("An exception should have ben thrown.");
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains(submissionTracker.getTuple().toString()));
        }
        verifySubmissionTrackerMock(submissionTrackerDao);
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
            Assert.fail("This should not happen", e);
        }

        verifySubmissionTrackerMock(submissionTrackerDao);
    }

    public void testValidateSubmissionsDtoWithDaoResultPass() {
        ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder(1, PDO_99999);
        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);

        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, getBassResultMap());
        SubmissionTracker submissionTracker = getSubmissionTracker(submissionDto);

        Mockito.when(submissionTrackerDao
                .findSubmissionTrackers(Mockito.anyString(), Mockito.anyCollectionOf(SubmissionDto.class)))
                .thenReturn(Collections.singletonList(submissionTracker));

        Map<BassDTO.BassResultColumn, String> bassInfo = new HashMap<>();
        bassInfo.put(BassDTO.BassResultColumn.file_type, BassFileType.BAM.getBassValue());
        bassInfo.put(BassDTO.BassResultColumn.version, String.valueOf(9));
        bassInfo.put(BassDTO.BassResultColumn.sample, "ABC1234");

        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto(dummyProductOrder.getResearchProject().getJiraTicketKey(),
                    Collections.singletonList(getSubmissionDto(dummyProductOrder, bassInfo)));
        } catch (Exception e) {
            Assert.fail("This should not happen", e);
        }
        verifySubmissionTrackerMock(submissionTrackerDao);
    }

    public void testValidateSubmissions_PreviousSubmissionTrackerHasNullPath() throws Exception {
        ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder(1, PDO_99999);
        SubmissionTrackerDao submissionTrackerDao = Mockito.mock(SubmissionTrackerDao.class);
        Map<BassDTO.BassResultColumn, String> bassInfo = getBassResultMap();

        SubmissionDto submissionDto = getSubmissionDto(dummyProductOrder, bassInfo);
        SubmissionTracker submissionTracker = getSubmissionTracker(submissionDto);

        Assert.assertNull(submissionTracker.getFileName());

        Mockito.when(submissionTrackerDao
                .findSubmissionTrackers(Mockito.anyString(), Mockito.anyCollectionOf(SubmissionDto.class)))
                .thenReturn(Collections.singletonList(submissionTracker));

        ResearchProjectEjb researchProjectEjb = getResearchProjectEjb(submissionTrackerDao);

        try {
            researchProjectEjb.validateSubmissionDto(dummyProductOrder.getResearchProject().getJiraTicketKey(),
                    Collections.singletonList(submissionDto));
            Assert.fail("An exception should have ben thrown.");
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains(submissionTracker.getTuple().toString()));
        }
        verifySubmissionTrackerMock(submissionTrackerDao);
    }

    private void verifySubmissionTrackerMock(SubmissionTrackerDao submissionTrackerDao) {
        Mockito.verify(submissionTrackerDao, Mockito.times(1)).
        findSubmissionTrackers(Mockito.anyString(), Mockito.anyCollectionOf(SubmissionDto.class));
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
            Assert.fail("An exception should have ben thrown.");
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
            Assert.fail("An exception should have ben thrown.");
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
