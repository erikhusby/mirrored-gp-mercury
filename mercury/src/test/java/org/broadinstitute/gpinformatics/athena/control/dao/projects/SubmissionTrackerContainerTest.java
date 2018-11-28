package org.broadinstitute.gpinformatics.athena.control.dao.projects;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTrackerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD)
public class SubmissionTrackerContainerTest extends Arquillian {

    @Inject
    ResearchProjectDao researchProjectDao;

    private String jiraTicketId;
    private String testAccessionID;
    private ResearchProject testProject;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

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
        SubmissionTracker tracker =
            new SubmissionTracker(jiraTicketId, testAccessionID, SubmissionTrackerTest.TEST_VERSION,
                SubmissionTrackerTest.TEST_FILE_TYPE, SubmissionTrackerTest.TEST_PROCESSING_LOCATION,
                SubmissionTrackerTest.TEST_DATA_TYPE);
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
