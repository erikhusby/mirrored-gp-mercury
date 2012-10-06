package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

import static org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB.IrbType.*;

/**
 *
 */
public class ResearchProjectResourceTest extends ContainerTest {

    private static final Long TEST_CREATOR = 10L;
    private static final Long TestScientist1 = 111L;
    private static final Long TestScientist2 = 222L;

    @Inject
    ResearchProjectResource researchProjectResource;

    @Inject
    private ResearchProjectDao researchProjectDao;

    private String testTitle;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Only do this if the server is calling this and thus, injection worked
        if (researchProjectResource != null) {
            testTitle = "MyResearchProject_" + UUID.randomUUID();
            ResearchProject researchProject =
                    new ResearchProject(TEST_CREATOR, testTitle, "To study stuff.");

            researchProject.addFunding(new ResearchProjectFunding(researchProject, "TheGrant_" + UUID.randomUUID()));
            researchProject.addFunding(new ResearchProjectFunding(researchProject, "ThePO_" + UUID.randomUUID()));

            researchProject.addIrbNumber(
                new ResearchProjectIRB(researchProject, FARBER, "irb123_" + UUID.randomUUID()));
            researchProject.addIrbNumber(
                new ResearchProjectIRB(researchProject, BROAD, "irb456_" + UUID.randomUUID()));

            researchProject.addPerson(RoleType.SCIENTIST, TestScientist1);
            researchProject.addPerson(RoleType.SCIENTIST, TestScientist2);

            researchProjectDao.persist(researchProject);
        }
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Only do this if the server is calling this and thus, injection worked
        if (researchProjectResource != null) {
            ResearchProject researchProject = researchProjectResource.findResearchProjectByTitle(testTitle);
            researchProjectDao.delete(researchProject);
        }
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testFindResearchProjectById() throws Exception {
        ResearchProject researchProject = researchProjectResource.findResearchProjectByTitle(testTitle);
        Assert.assertNotNull(researchProject);
        Assert.assertNotNull(researchProject.getTitle());
        Assert.assertEquals(researchProject.getTitle(), testTitle);
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testFindAllResearchProjects() throws Exception {
        List<ResearchProject> researchProjects =
                researchProjectResource.findAllResearchProjectsByCreator(TEST_CREATOR);
        Assert.assertNotNull(researchProjects);
        Assert.assertEquals(researchProjects.size(), 1);
    }
}
