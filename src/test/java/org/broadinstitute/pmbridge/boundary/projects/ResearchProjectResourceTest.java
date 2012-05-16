package org.broadinstitute.pmbridge.boundary.projects;

import org.broadinstitute.pmbridge.DeploymentBuilder;
import org.broadinstitute.pmbridge.entity.project.ResearchProject;
import org.broadinstitute.pmbridge.entity.project.ResearchProjects;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertNull;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/9/12
 * Time: 1:56 PM
 */
public class ResearchProjectResourceTest extends Arquillian {

    @Inject
    ResearchProjectResource researchProjectResource;

    @Deployment
    public static WebArchive buildBridgeWar() {
        WebArchive war = DeploymentBuilder.buildBridgeWar();
        return war;
    }

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
        //TODO - should we change this api call to that the search switches to look up by name ?
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
        Assert.assertEquals(4, researchProjects.getProjects().size());
//        Assert.assertEquals(researchProjects.getProjects().get(0).getTitle().name + " " +
//                researchProjects.getProjects().get(0).getId().longValue()
//                , "FakeResearchProject 111" );
//
//        Assert.assertEquals(researchProjects.getProjects().get(3).getTitle().name + " " +
//                researchProjects.getProjects().get(3).getId().longValue()
//                , "FakeResearchProject 444" );
    }
}
