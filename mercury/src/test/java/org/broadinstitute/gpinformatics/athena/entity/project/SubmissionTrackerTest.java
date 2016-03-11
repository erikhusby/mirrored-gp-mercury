package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionRepository;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionTrackerTest {

    public static String testAccessionID = "SA-2342";
    public static String testFileName = "/test/path/file2.bam";

    public static String testVersion = "1";
    public static SubmissionRepository testRepository = new SubmissionRepository(SubmissionRepository.DEFAULT_REPOSITORY_NAME,
                    "NCBI Controlled Access (dbGaP) submissions");

    public static SubmissionLibraryDescriptor testLibraryDescriptor = ProductFamily.defaultLibraryDescriptor();

    public void testBuildSubmissionTracker() {
        Date testStartDate = new Date();

        SubmissionTrackerStub tracker =
                new SubmissionTrackerStub(testAccessionID, testFileName, testVersion, testRepository,
                        testLibraryDescriptor);

        Assert.assertNotNull(tracker);

        Assert.assertEquals(tracker.getSubmittedSampleName(), testAccessionID);
        Assert.assertEquals(tracker.getFileName(), testFileName);

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

        public SubmissionTrackerStub(String submittedSampleName, String fileName, String version,
                                     SubmissionRepository repository, SubmissionLibraryDescriptor libraryDescriptor) {
            super(submittedSampleName, fileName, version, repository, libraryDescriptor);
        }

        public SubmissionTrackerStub(Long submissionTrackerId, String testAccessionID, String testFileName,
                                     String testVersion, SubmissionRepository repository,
                                     SubmissionLibraryDescriptor submissionLibraryDescriptor) {
            super(submissionTrackerId, testAccessionID, testFileName, testVersion, repository,
                    submissionLibraryDescriptor);
        }

        @Override
        public void setSubmissionTrackerId(Long id) {
            super.setSubmissionTrackerId(id);
        }
    }
}
