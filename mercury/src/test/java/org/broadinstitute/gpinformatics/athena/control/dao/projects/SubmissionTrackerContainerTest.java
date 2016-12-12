package org.broadinstitute.gpinformatics.athena.control.dao.projects;

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

@Test(groups = TestGroups.STANDARD)
public class SubmissionTrackerContainerTest extends ContainerTest {
    @Inject
    ResearchProjectDao researchProjectDao;

    private String jiraTicketId;
    private String testAccessionID;
    private static final BassFileType testFileType = BassFileType.BAM;
    private static final String testVersion = "v1";
    private ResearchProject testProject;

    @BeforeMethod
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (researchProjectDao == null) {
            return;
        }
        String testId = String.valueOf(System.currentTimeMillis());
        testAccessionID  = "SA-"+testId;
        jiraTicketId = "RP" + testId;
        testProject = ResearchProjectTestFactory.createTestResearchProject(jiraTicketId);

    }

    @AfterMethod
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (researchProjectDao == null) {
            return;
        }
        researchProjectDao.remove(testProject);
    }

    public void testTrackerConfiguration() throws Exception {
        SubmissionTracker tracker = new SubmissionTracker(jiraTicketId, testAccessionID, testVersion, testFileType);
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
