package org.broadinstitute.gpinformatics.athena.control.dao.projects;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject_;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

/**
 * Tests for the research project dao
 */
@Test(groups = TestGroups.STUBBY, enabled = true)
@Dependent
public class ResearchProjectDaoTest extends StubbyContainerTest {

    public ResearchProjectDaoTest(){}

    @Inject
    private UserTransaction utx;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @BeforeMethod(groups = TestGroups.STUBBY)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();
    }

    @AfterMethod(groups = TestGroups.STUBBY)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    public void testFindMultipleAttribute() {
        String searchString = "mouse";
        @SuppressWarnings("unchecked")
        List<ResearchProject> mies =
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

    public void testFindResearchProjectsLike() {
        List<ResearchProject> projects = researchProjectDao.findLikeJiraTicketKey("RP-3");
        Assert.assertNotNull(projects);
        Assert.assertFalse(projects.isEmpty());

        projects = researchProjectDao.findLikeTitle("WGS");
        Assert.assertNotNull(projects);
        Assert.assertFalse(projects.isEmpty());
    }

    /**
     * Ugly positive test for the presence of PMs, this should be creating its own test data.
     */
    public void testPMs() {

        long JAMES_BOCHICCHIO_ID = 11144;
        long LAUREN_AMBROGIO_ID = 11137;

        Long[] pms = {JAMES_BOCHICCHIO_ID, LAUREN_AMBROGIO_ID};

        List<ResearchProject> researchProjects = researchProjectDao.findByProjectManagerIds(pms);

        Assert.assertNotNull(researchProjects);
        Assert.assertNotEquals(researchProjects.size(), 0);

        for (ResearchProject rp : researchProjects) {
            boolean contains = false;
            Long[] actualPMs = rp.getProjectManagers();
            for (long pm : pms) {
                if (ArrayUtils.contains(actualPMs, pm)) {
                    contains = true;
                    break;
                }
            }

            Assert.assertTrue(contains,
                    MessageFormat.format("{0} did contain any of the expected PMs: {1}", rp.getJiraTicketKey(),
                            StringUtils.join(pms)));
        }

    }
}
