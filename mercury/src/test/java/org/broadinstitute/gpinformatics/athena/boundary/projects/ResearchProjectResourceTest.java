package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.Assert;
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

    @Inject
    ResearchProjectResource researchProjectResource;


    @Test
    public void testFindResearchProjectById() throws Exception {
        ResearchProject researchProject = researchProjectResource.findResearchProjectById("111");
        Assert.assertNotNull(researchProject);
        Assert.assertNotNull(researchProject.getTitle());
        Assert.assertEquals(researchProject.getTitle(), "FakeResearchProjectId{value='111'}");

        researchProject = null;
        // Try to get rp using an invalid rpid - empty string
        try {
            researchProject = researchProjectResource.findResearchProjectById(" ");
            fail("Should throw exception");
        } catch ( Exception e ) {
            // pass
            assertNull(researchProject);
        }

        researchProject = null;
        // Try to get rp using an invalid rpid - non-numeric
        try {
            researchProject = researchProjectResource.findResearchProjectById("abc");
            fail("Should throw exception");
        } catch ( Exception e ) {
            // pass
            assertNull(researchProject);
        }

    }

    @Test
    public void testFindAllResearchProjects() throws Exception {
        List<ResearchProject> researchProjects = researchProjectResource.findAllResearchProjects("");
        Assert.assertNotNull(researchProjects);
        Assert.assertEquals(5, researchProjects.size());
    }
}
