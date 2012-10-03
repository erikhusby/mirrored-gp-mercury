package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

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

    private Long testResearchProjectId;
    private String testTitle;

    public void setUp() throws Exception {
        testTitle = "MyResearchProject_" + UUID.randomUUID();
        ResearchProject researchProject =
            new ResearchProject(TEST_CREATOR, testTitle, "To study stuff.");

        researchProject.addFunding(new ResearchProjectFunding(researchProject, "TheGrant_" + UUID.randomUUID()));
        researchProject.addFunding(new ResearchProjectFunding(researchProject, "ThePO_" + UUID.randomUUID()));

        researchProject.addIrbNumber(new ResearchProjectIRB(researchProject, "irb123_" + UUID.randomUUID()));
        researchProject.addIrbNumber(new ResearchProjectIRB(researchProject, "irb456_" + UUID.randomUUID()));

        researchProject.addPerson(RoleType.SCIENTIST, TestScientist1);
        researchProject.addPerson(RoleType.SCIENTIST, TestScientist2);

        researchProjectDao.persist(researchProject);

        testResearchProjectId = researchProject.getId();
    }

    public void tearDown() throws Exception {
        ResearchProject researchProject = researchProjectResource.findResearchProjectById(testResearchProjectId);
        researchProjectDao.delete(researchProject);
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testFindResearchProjectById() throws Exception {
        setUp();
        ResearchProject researchProject = researchProjectResource.findResearchProjectById(testResearchProjectId);
        Assert.assertNotNull(researchProject);
        Assert.assertNotNull(researchProject.getTitle());
        Assert.assertEquals(researchProject.getTitle(), testTitle);
        tearDown();
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testFindAllResearchProjects() throws Exception {
        setUp();

        try {
            List<ResearchProject> researchProjects =
                researchProjectResource.findAllResearchProjectsByCreator(TEST_CREATOR);
            Assert.assertNotNull(researchProjects);
            Assert.assertEquals(researchProjects.size(), 1);
        } finally {
            tearDown();
        }
    }
}
