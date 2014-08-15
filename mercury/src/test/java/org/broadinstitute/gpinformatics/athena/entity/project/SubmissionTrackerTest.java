package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionTrackerTest {

    public static String testAccessionID = "SA-2342";
    public static String testFileName = "/test/path/file2.bam";

    public static String testVersion = "v1";

    public void testBuildSubmissionTracker() {

        SubmissionTrackerStub tracker = new SubmissionTrackerStub(testAccessionID, testFileName, testVersion);

        Assert.assertNotNull(tracker);

        Assert.assertEquals(tracker.getSubmittedSampleName(), testAccessionID);
        Assert.assertEquals(tracker.getFileName(), testFileName);

        Assert.assertEquals(tracker.getVersion() , testVersion);

        Assert.assertNull(tracker.createSubmissionIdentifier());

        tracker.setSubmissionTrackerId(345L);

        Assert.assertEquals(tracker.createSubmissionIdentifier(), SubmissionTracker.MERCURY_SUBMISSION_ID_PREFIX +345L);

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

        public SubmissionTrackerStub(String submittedSampleName, String fileName, String version) {
            super(submittedSampleName, fileName, version);
        }

        public SubmissionTrackerStub(Long submissionTrackerId, String testAccessionID, String testFileName, String testVersion) {
            super(submissionTrackerId, testAccessionID, testFileName, testVersion);
        }

        @Override
        public void setSubmissionTrackerId(Long id) {
            super.setSubmissionTrackerId(id);
        }
    }
}
