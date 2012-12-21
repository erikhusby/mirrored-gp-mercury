package org.broadinstitute.gpinformatics.athena.control.dao.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject_;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.List;
import java.util.Map;

/**
 * Tests for the research project dao
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled=true)
public class ResearchProjectDAOTest extends ContainerTest {

    @Inject
    private UserTransaction utx;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    @SuppressWarnings({"unchecked"})
    public void testFindMultipleAttribute() {
        final String searchString = "mouse";
        final List<ResearchProject> mies =
                researchProjectDao.findListWithWildcard(ResearchProject.class, searchString, true,
                        ResearchProject_.title,
                        ResearchProject_.synopsis,
                        ResearchProject_.irbNotes
                );

        Assert.assertTrue(mies.size() > 5, String.format("List should have returned a few values but only returned %d!",
                mies.size()));

        for (ResearchProject projectWithMouse : mies) {
            boolean found = projectWithMouse.getTitle().toLowerCase().contains(searchString) ||
                            projectWithMouse.getSynopsis().toLowerCase().contains(searchString) ||
                            projectWithMouse.getIrbNotes().toLowerCase().contains(searchString);

            Assert.assertTrue(found, String.format("Could not find searchString \"%s\" in results.", searchString));

        }
    }


    public void testFindResearchProjects() {
        // Try to find the created ProductOrder by its researchProject and title.
        List<ResearchProject> projects = researchProjectDao.findAllResearchProjects();
        Assert.assertNotNull(projects);
        Assert.assertFalse(projects.isEmpty());

        // Should get the same number of items
        List<ResearchProject> withOrders = researchProjectDao.findAllResearchProjectsWithOrders();
        Assert.assertNotNull(projects);
        Assert.assertFalse(projects.isEmpty());
        Assert.assertEquals(projects.size(), withOrders.size());
    }

    public void testOrderCounts() {
        Map<String, Long> projectOrderCounts = researchProjectDao.getProjectOrderCounts();
        Assert.assertNotNull(projectOrderCounts);
        Assert.assertFalse(projectOrderCounts.isEmpty());
    }
}
