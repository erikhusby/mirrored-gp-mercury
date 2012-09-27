package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjects;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

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
        Assert.assertEquals(researchProject.getTitle().name, "FakeResearchProject111");

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
        ResearchProjects researchProjects = researchProjectResource.findAllResearchProjects("");
        Assert.assertNotNull(researchProjects);
        Assert.assertNotNull(researchProjects.getProjects());
        Assert.assertEquals(5, researchProjects.getProjects().size());
//        Assert.assertEquals(researchProjects.getProjects().get(0).getTitle().name + " " +
//                researchProjects.getProjects().get(0).getId().longValue()
//                , "FakeResearchProject 111" );
//
//        Assert.assertEquals(researchProjects.getProjects().get(3).getTitle().name + " " +
//                researchProjects.getProjects().get(3).getId().longValue()
//                , "FakeResearchProject 444" );


    }
}
