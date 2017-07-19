package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.infrastructure.submission.FileType;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionBioSampleBean;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionTrackerTest {

    public static String testAccessionID = "SA-2342";
    public static String testProjectId = "P123";
    public static String testProcessingLocation = SubmissionBioSampleBean.ON_PREM;
    public static FileType testFileType = FileType.BAM;

    public static String testVersion = "v1";

    public void testBuildSubmissionTracker() {
        Date testStartDate = new Date();

        SubmissionTrackerStub tracker =
                new SubmissionTrackerStub(testProjectId, testAccessionID, testVersion, testFileType);

        Assert.assertNotNull(tracker);

        Assert.assertEquals(tracker.getSubmittedSampleName(), testAccessionID);

        Assert.assertEquals(tracker.getVersion(), testVersion);

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
            super(project, submittedSampleName, version, fileType, SubmissionBioSampleBean.ON_PREM);
        }

        public SubmissionTrackerStub(Long submissionTrackerId, String project, String testAccessionID,
                                     String testVersion, FileType fileType, String processingLocation) {
            super(submissionTrackerId, project, testAccessionID, testVersion, fileType, processingLocation);
        }

        @Override
        public void setSubmissionTrackerId(Long id) {
            super.setSubmissionTrackerId(id);
        }
    }
}
