package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.test.ResearchProjectFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


/**
 * Simple test of the research project without any database connection.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ResearchProjectTest {

    private static final String RESEARCH_PROJ_JIRA_KEY = "RP-1";

    private ResearchProject researchProject;

    @BeforeMethod
    public void setUp() throws Exception {
        researchProject = ResearchProjectFactory
                .createDummyResearchProject(10950, "MyResearchProject", "To Study Stuff", ResearchProject.IRB_ENGAGED);
    }

    @Test
    public void manageRPTest() {
        assertThat(researchProject.getPeople(RoleType.SCIENTIST), is(not(nullValue())));
        // A new RP is initialized with the creator as its PM.
        assertThat(researchProject.getPeople(RoleType.PM), is(arrayWithSize(1)));

        // Add a collection.
        ResearchProjectCohort collection = new ResearchProjectCohort(researchProject, "BSPCollection");
        researchProject.addCohort(collection);
        assertThat(researchProject.getCohortIds(), is(arrayWithSize(1)));

        // Add a second and check size.
        collection = new ResearchProjectCohort(researchProject, "AlxCollection2");
        researchProject.addCohort(collection);
        assertThat(researchProject.getCohortIds(), is(arrayWithSize(2)));

        // Remove second and check size.
        researchProject.removeCohort(collection);
        assertThat(researchProject.getCohortIds(), is(arrayWithSize(1)));

        assertThat(researchProject.getJiraTicketKey(), is(nullValue()));

        assertThat(researchProject.fetchJiraIssueType(), is(equalTo(CreateFields.IssueType.RESEARCH_PROJECT)));

        assertThat(researchProject.fetchJiraProject(), is(equalTo(CreateFields.ProjectType.Research_Projects)));

        assertThat(researchProject.getProjectManagers(), is(arrayWithSize(greaterThan(0))));
        assertThat(researchProject.getBroadPIs(), is(arrayWithSize(greaterThan(0))));
        assertThat(researchProject.getScientists(), is(arrayWithSize(greaterThan(0))));
        assertThat(researchProject.getExternalCollaborators(), is(arrayWithSize(0)));

        assertThat(researchProject.getStatus(), is(equalTo(ResearchProject.Status.Open)));

        researchProject.clearPeople();
        researchProject.addPeople(RoleType.PM, Collections.singletonList(new BspUser()));
        assertThat(researchProject.getProjectManagers(), is(arrayWithSize(1)));
        assertThat(researchProject.getBroadPIs(), is(arrayWithSize(0)));
        assertThat(researchProject, is(equalTo(researchProject)));

        try {
            researchProject.setJiraTicketKey(null);
            Assert.fail();
        } catch (NullPointerException npe) {
            // Ensuring Null is thrown for setting null.
        } finally {
            researchProject.setJiraTicketKey(RESEARCH_PROJ_JIRA_KEY);
        }

        assertThat(researchProject.getJiraTicketKey(), is(notNullValue()));

        assertThat(researchProject.getJiraTicketKey(), is(equalTo(RESEARCH_PROJ_JIRA_KEY)));
    }
}
