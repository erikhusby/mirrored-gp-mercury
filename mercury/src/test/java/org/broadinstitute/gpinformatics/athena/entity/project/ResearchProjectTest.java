package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;


/**
 * Simple test of the research project without any database connection.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ResearchProjectTest {

    private static final String RESEARCH_PROJ_JIRA_KEY = "RP-1";

    private ResearchProject researchProject;

    @BeforeMethod
    public void setUp() throws Exception {
        researchProject = AthenaClientServiceStub
                .createDummyResearchProject(10950, "MyResearchProject", "To Study Stuff", ResearchProject.IRB_ENGAGED);
    }

    @Test
    public void manageRPTest() {
        Assert.assertNotNull(researchProject.getPeople(RoleType.SCIENTIST));
        // A new RP is initialized with the creator as its PM.
        Assert.assertTrue(researchProject.getPeople(RoleType.PM).length == 1);

        // Add a collection.
        ResearchProjectCohort collection = new ResearchProjectCohort(researchProject, "BSPCollection");
        researchProject.addCohort(collection);
        Assert.assertTrue(researchProject.getCohortIds().length == 1);

        // Add a second and check size.
        collection = new ResearchProjectCohort(researchProject, "AlxCollection2");
        researchProject.addCohort(collection);
        Assert.assertTrue(researchProject.getCohortIds().length == 2);

        // Remove second and check size.
        researchProject.removeCohort(collection);
        Assert.assertTrue(researchProject.getCohortIds().length == 1);

        Assert.assertNull(researchProject.getJiraTicketKey());

        Assert.assertEquals(researchProject.fetchJiraIssueType(), CreateFields.IssueType.RESEARCH_PROJECT );

        Assert.assertEquals(researchProject.fetchJiraProject(), CreateFields.ProjectType.Research_Projects);

        Assert.assertTrue(researchProject.getProjectManagers().length > 0);
        Assert.assertTrue(researchProject.getBroadPIs().length > 0);
        Assert.assertTrue(researchProject.getScientists().length > 0);
        Assert.assertTrue(researchProject.getExternalCollaborators().length == 0);

        Assert.assertTrue(researchProject.getStatus() == ResearchProject.Status.Open);

        researchProject.clearPeople();
        researchProject.addPeople(RoleType.PM, Collections.singletonList(new BspUser()));
        Assert.assertTrue(researchProject.getProjectManagers().length == 1);
        Assert.assertTrue(researchProject.getBroadPIs().length == 0);

        Assert.assertEquals(researchProject, researchProject);

        try {
            researchProject.setJiraTicketKey(null);
            Assert.fail();
        } catch (NullPointerException npe) {
            // Ensuring Null is thrown for setting null.
        } finally {
            researchProject.setJiraTicketKey(RESEARCH_PROJ_JIRA_KEY);
        }

        Assert.assertNotNull(researchProject.getJiraTicketKey());

        Assert.assertEquals(researchProject.getJiraTicketKey(), RESEARCH_PROJ_JIRA_KEY);
    }
}
