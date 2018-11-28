package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.infrastructure.submission.FileType;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionBioSampleBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionTrackerTest {

    public static final String TEST_ACCESSION_ID = "SA-2342";
    public static final String TEST_PROJECT_ID = "P123";
    public static final String TEST_PROCESSING_LOCATION = SubmissionBioSampleBean.ON_PREM;
    public static final String TEST_DATA_TYPE = SubmissionLibraryDescriptor.WHOLE_EXOME.getName();
    public static final FileType TEST_FILE_TYPE = FileType.BAM;
    public static final String TEST_VERSION = "v1";

    public void testBuildSubmissionTracker() {
        Date testStartDate = new Date();

        SubmissionTrackerStub tracker =
                new SubmissionTrackerStub(TEST_PROJECT_ID, TEST_ACCESSION_ID, TEST_VERSION, TEST_FILE_TYPE);

        Assert.assertNotNull(tracker);

        Assert.assertEquals(tracker.getSubmittedSampleName(), TEST_ACCESSION_ID);

        Assert.assertEquals(tracker.getVersion(), TEST_VERSION);

        Assert.assertTrue(tracker.getRequestDate().getTime() >= testStartDate.getTime(),
                "SubmissionTracker's requestDate should be after the time that the test method started");
        Assert.assertTrue(tracker.getRequestDate().getTime() <= new Date().getTime(),
                "SubmissionTracker's requestDate should be before the time of this assert");

        Assert.assertNull(tracker.createSubmissionIdentifier());

        tracker.setSubmissionTrackerId(345L);

        Assert.assertEquals(tracker.createSubmissionIdentifier(),
                SubmissionTracker.MERCURY_SUBMISSION_ID_PREFIX + new SimpleDateFormat("YYYYMMdd").format(testStartDate)
                + 345L);

        Assert.assertNull(tracker.getResearchProject());

        ResearchProject testProject = ResearchProjectTestFactory.createTestResearchProject();

        tracker.setResearchProject(testProject);
        testProject.addSubmissionTracker(tracker);
        Assert.assertEquals(tracker.getResearchProject(), testProject);
        Assert.assertTrue(testProject.getSubmissionTrackers().contains(tracker));

        Assert.assertTrue(testProject.getSubmissionTrackers().contains(tracker));

    }

    public static class SubmissionTrackerStub extends SubmissionTracker {
        protected SubmissionTrackerStub() {
            super();
        }

        public SubmissionTrackerStub(String project, String submittedSampleName, String version,
                                     FileType fileType) {
            super(project, submittedSampleName, version, fileType, SubmissionBioSampleBean.ON_PREM, TEST_DATA_TYPE);
        }

        public SubmissionTrackerStub(Long submissionTrackerId, String project, String testAccessionID,
                                     String TEST_VERSION, FileType fileType, String processingLocation) {
            super(submissionTrackerId, project, testAccessionID, TEST_VERSION, fileType, processingLocation,
                TEST_DATA_TYPE);
        }

        @Override
        public void setSubmissionTrackerId(Long id) {
            super.setSubmissionTrackerId(id);
        }
    }
}
