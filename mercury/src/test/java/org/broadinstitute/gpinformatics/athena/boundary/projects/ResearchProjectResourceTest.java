package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDAO;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/9/12
 * Time: 1:56 PM
 */
public class ResearchProjectResourceTest extends ContainerTest {

    private static final Long TEST_CREATOR = 10L;
    private static final Long TestScientist1 = 111L;
    private static final Long TestScientist2 = 222L;

    @Inject
    ResearchProjectResource researchProjectResource;

    @Inject
    private ResearchProjectDAO researchProjectDAO;

    private Long testResearchProjectId;
    private String testTitle;

    @BeforeMethod
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

        researchProjectDAO.persist(researchProject);

        testResearchProjectId = researchProject.getId();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        ResearchProject researchProject = researchProjectResource.findResearchProjectById(testResearchProjectId);
        researchProjectDAO.delete(researchProject);
    }

    @Test
    public void testFindResearchProjectById() throws Exception {
        ResearchProject researchProject = researchProjectResource.findResearchProjectById(testResearchProjectId);
        Assert.assertNotNull(researchProject);
        Assert.assertNotNull(researchProject.getTitle());
        Assert.assertEquals(researchProject.getTitle(), testTitle);
    }

    @Test
    public void testFindAllResearchProjects() throws Exception {
        List<ResearchProject> researchProjects = researchProjectResource.findAllResearchProjects(TEST_CREATOR);
        Assert.assertNotNull(researchProjects);
        Assert.assertEquals(5, researchProjects.size());
    }
}
