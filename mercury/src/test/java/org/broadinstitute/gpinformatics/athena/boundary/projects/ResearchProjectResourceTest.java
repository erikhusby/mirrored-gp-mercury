package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDAO;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertNull;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/9/12
 * Time: 1:56 PM
 */
public class ResearchProjectResourceTest extends ContainerTest {

    private static final Long TEST_CREATOR = 10L;
    private static final String TEST_TITLE = "MyResearchProject";

    @Inject
    ResearchProjectResource researchProjectResource;

    @Inject
    private ResearchProjectDAO researchProjectDAO;

    private Long testResearchProjectId;

    @BeforeMethod
    public void setUp() throws Exception {
        ResearchProject researchProject = new ResearchProject(TEST_CREATOR, TEST_TITLE, "To study stuff.");

        researchProject.addFunding(new ResearchProjectFunding(researchProject, "TheGrant"));
        researchProject.addFunding(new ResearchProjectFunding(researchProject, "ThePO"));

        researchProject.addIrbNumber(new ResearchProjectIRB(researchProject, "irb123"));
        researchProject.addIrbNumber(new ResearchProjectIRB(researchProject, "irb456"));

        researchProject.addPerson(RoleType.SCIENTIST, 111L);
        researchProject.addPerson(RoleType.SCIENTIST, 222L);

        researchProjectDAO.persist(researchProject);

        testResearchProjectId = researchProject.getId();
    }

    @Test
    public void testFindResearchProjectById() throws Exception {
        ResearchProject researchProject = researchProjectResource.findResearchProjectById(testResearchProjectId);
        Assert.assertNotNull(researchProject);
        Assert.assertNotNull(researchProject.getTitle());
        Assert.assertEquals(researchProject.getTitle(), TEST_TITLE);

        // Try to get rp using an invalid rpid - empty string
        try {
            researchProject = researchProjectResource.findResearchProjectById(null);
            fail("Should throw exception");
        } catch ( Exception e ) {
            // pass
            assertNull(researchProject);
        }
    }

    @Test
    public void testFindAllResearchProjects() throws Exception {
        List<ResearchProject> researchProjects = researchProjectResource.findAllResearchProjects(TEST_CREATOR);
        Assert.assertNotNull(researchProjects);
        Assert.assertEquals(5, researchProjects.size());
    }
}
