package org.broadinstitute.gpinformatics.athena.control.dao.projects;

import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassFileType;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class SubmissionTrackerContainerTest extends ContainerTest {

    @Inject
    ResearchProjectDao researchProjectDao;

    private static String testAccessionID = "SA-2342";
    private static BassFileType testFileType = BassFileType.BAM;
    private static String testVersion = "v1";

    public static SubmissionRepository
            testRepository=new SubmissionRepository(SubmissionRepository.DEFAULT_REPOSITORY_NAME, "description");
    public static SubmissionLibraryDescriptor testLibraryDescriptor= ProductFamily.defaultLibraryDescriptor();

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (researchProjectDao == null) {
            return;
        }

    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (researchProjectDao == null) {
            return;
        }

    }

    public void testTrackerConfiguration() throws Exception {
        ResearchProject testProject = ResearchProjectTestFactory.createTestResearchProject();
        testProject.setJiraTicketKey("RP-testRP");
        SubmissionTracker tracker = new SubmissionTracker(testAccessionID, testFileType, testVersion);

        testProject.addSubmissionTracker(tracker);

        researchProjectDao.persist(testProject);

        Assert.assertNotNull(tracker.createSubmissionIdentifier());
        Assert.assertNotNull(tracker.getSubmissionTrackerId());

        int dateLength = new SimpleDateFormat("YYYYMMdd").format(new Date()).length();
        Assert.assertEquals(String.valueOf(tracker.getSubmissionTrackerId()),
                tracker.createSubmissionIdentifier()
                        .substring(SubmissionTracker.MERCURY_SUBMISSION_ID_PREFIX.length() + dateLength));
    }

}
