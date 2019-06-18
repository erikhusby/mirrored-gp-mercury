package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.NoJiraTransitionException;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.UUID;

import static org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB.IrbType.BROAD;
import static org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB.IrbType.FARBER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@Test(groups = TestGroups.STUBBY)
@Dependent
public class ResearchProjectResourceTest extends StubbyContainerTest {

    public ResearchProjectResourceTest(){}

    private Date now;
    private static final long TEST_CREATOR = 10;
    private static final long TEST_SCIENTIST_1 = 14567;
    private static final long TEST_SCIENTIST_2 = 11908;

    // Transaction is used for this test just to set up data for the 'real' test
    @Inject
    private UserTransaction utx;

    @Inject
    ResearchProjectResource researchProjectResource;

    @Inject
    ResearchProjectDao researchProjectDao;

    private String testTitle;

    @BeforeMethod(groups = TestGroups.STUBBY)
    public void setUp() throws Exception {
        now = new Date();
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();
        // Only do this if the server is calling this and thus, injection worked
        if (researchProjectResource != null) {
            testTitle = "MyResearchProject_" + UUID.randomUUID();
            ResearchProject researchProject = createDummyResearchProject(testTitle);

            // Persist research project, which should succeed assuming all validation has been done up-front
            try {
                researchProjectDao.persist(researchProject);
                researchProjectDao.flush();
            } catch (RuntimeException e) {
                researchProject.rollbackPersist();
                throw e;
            }
        }
    }

    public static ResearchProject createDummyResearchProject(String title) {
        ResearchProject researchProject =
                new ResearchProject(TEST_CREATOR, title, "To study stuff.", ResearchProject.IRB_NOT_ENGAGED,
                        ResearchProject.RegulatoryDesignation.RESEARCH_ONLY);
        researchProject.setJiraTicketKey(title);

        researchProject.addFunding(new ResearchProjectFunding(researchProject, "TheGrant_" + UUID.randomUUID()));
        researchProject.addFunding(new ResearchProjectFunding(researchProject, "ThePO_" + UUID.randomUUID()));

        researchProject.addIrbNumber(
                new ResearchProjectIRB(researchProject, FARBER, "irb123_" + UUID.randomUUID()));
        researchProject.addIrbNumber(
                new ResearchProjectIRB(researchProject, BROAD, "irb456_" + UUID.randomUUID()));

        researchProject.addPerson(RoleType.SCIENTIST, TEST_SCIENTIST_1);
        researchProject.addPerson(RoleType.SCIENTIST, TEST_SCIENTIST_2);

        return researchProject;
    }

    @AfterMethod(groups = TestGroups.STUBBY)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }
        utx.rollback();
    }

    @Test(groups = TestGroups.STUBBY)
    public void testFindResearchProjectById() throws Exception {
        ResearchProjectResource.ResearchProjects researchProject = researchProjectResource.findByIds(testTitle);
        Assert.assertEquals(researchProject.projects.size(), 1);
        Assert.assertEquals(researchProject.projects.get(0).title, testTitle);
    }

    @Test(groups = TestGroups.STUBBY)
    public void testCreateResearchProject() throws Exception {
        ResearchProjectResource.ResearchProjectData data =
                new ResearchProjectResource.ResearchProjectData("Test Research Project " + now.getTime(),
                        "Small test project to test logging", "scottmat");
        ResearchProjectResource.ResearchProjectData researchProjectData = researchProjectResource.create(data);
        assertThat(researchProjectData.id, is(notNullValue()));
    }

    @Test(groups = TestGroups.STUBBY)
    public void testCreateResearchProjectJiraException() throws Exception {
        ResearchProjectResource.ResearchProjectData data =
                new ResearchProjectResource.ResearchProjectData("Test Research Project " + now.getTime(),
                        "Small test project to test logging", "scottmat");
        String jiraTransitionError = "Some Jira Error";

        try {
            ResearchProjectEjb mockResearchProjectEjb = Mockito.mock(ResearchProjectEjb.class);
            Mockito.doThrow(new NoJiraTransitionException(jiraTransitionError))
                    .when(mockResearchProjectEjb).submitToJira(Mockito.any(ResearchProject.class));
            researchProjectResource.setResearchProjectEjb(mockResearchProjectEjb);
            researchProjectResource.create(data);
            Assert.fail();
        } catch (InformaticsServiceException e) {
            Assert.assertEquals(e.getMessage(),
                    String.format(ResearchProjectResource.SUBMIT_TO_JIRA_FORMAT_STRING, jiraTransitionError));
        } catch (Exception e) {
            Assert.fail(String.format("Expected an %s but instead got a %s",
                    InformaticsServiceException.class.getName(), e.getClass().getName()));
        }
    }

    @Test(groups = TestGroups.STUBBY)
    public void testCreateResearchProjectNoUser() throws Exception {
        ResearchProjectResource.ResearchProjectData data =
                new ResearchProjectResource.ResearchProjectData("Test Research Project " + now.getTime(),
                        "Small test project to test logging", " ");
        try {
            researchProjectResource.create(data);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(e.getCause().getClass(), ResourceException.class);
            ResourceException resourceException = (ResourceException) e.getCause();
            Assert.assertEquals(resourceException.getMessage(), ResearchProjectResource.VALID_USERNAME_REQUIRED_MESSAGE);
            Assert.assertEquals(resourceException.getStatus(), Response.Status.UNAUTHORIZED);
        }
    }

    @Test(groups = TestGroups.STUBBY)
    public void testCreateResearchProjectNoGoodUser() throws Exception {
        ResearchProjectResource.ResearchProjectData data =
                new ResearchProjectResource.ResearchProjectData("Test Research Project " + now.getTime(),
                        "Small test project to test logging", "scottMatthewes");
        try {
            researchProjectResource.create(data);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(e.getCause().getClass(), ResourceException.class);
            ResourceException resourceException = (ResourceException) e.getCause();
            Assert.assertEquals(resourceException.getMessage(), ResearchProjectResource.VALID_USERNAME_REQUIRED_MESSAGE);
            Assert.assertEquals(resourceException.getStatus(), Response.Status.UNAUTHORIZED);
        }
    }
}
