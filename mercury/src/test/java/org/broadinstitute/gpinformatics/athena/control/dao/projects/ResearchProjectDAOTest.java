package org.broadinstitute.gpinformatics.athena.control.dao.projects;

import org.apache.commons.lang.math.RandomUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
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
    public static final String TITLE_PREFIX = "TestResearchProject_";
    public static final long CREATOR_ID = RandomUtils.nextInt(Integer.MAX_VALUE);

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
