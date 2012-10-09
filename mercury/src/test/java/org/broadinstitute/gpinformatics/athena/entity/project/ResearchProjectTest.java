package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


/**
 * Simple test of the research project without any database connection
 */
@Test(groups = {TestGroups.DATABASE_FREE})
public class ResearchProjectTest {

    private final static String RESEARCH_PROJ_JIRA_KEY = "RP-1";

    private ResearchProject researchProject;

    @BeforeMethod
    public void setUp() throws Exception {
        researchProject = new ResearchProject(1111L, "MyResearchProject", "To study stuff.", ResearchProject.IRB_ENGAGED);

        researchProject.addFunding(new ResearchProjectFunding(researchProject, "TheGrant"));
        researchProject.addFunding(new ResearchProjectFunding(researchProject, "ThePO"));

        researchProject.addIrbNumber(new ResearchProjectIRB(researchProject, ResearchProjectIRB.IrbType.BROAD, "irb123"));
        researchProject.addIrbNumber(new ResearchProjectIRB(researchProject, ResearchProjectIRB.IrbType.OTHER, "irb456"));

        researchProject.addPerson(RoleType.SCIENTIST, 111L);
        researchProject.addPerson(RoleType.SCIENTIST, 222L);
    }

    @Test(groups = {TestGroups.DATABASE_FREE})
    public void manageRPTest() {
        Assert.assertNotNull(researchProject.getPeople(RoleType.SCIENTIST));
        Assert.assertTrue(researchProject.getPeople(RoleType.PM).length == 0);

        researchProject.addPerson(RoleType.PM, 333L);
        Assert.assertNotNull(researchProject.getPeople(RoleType.PM));

        //Add a collection
        ResearchProjectCohort collection = new ResearchProjectCohort(researchProject, "BSPCollection");
        researchProject.addCohort(collection);
        Assert.assertTrue(researchProject.getCohortIds().length == 1);

        // Add a second and check size
        collection = new ResearchProjectCohort(researchProject, "AlxCollection2");
        researchProject.addCohort(collection);
        Assert.assertTrue(researchProject.getCohortIds().length == 2);

        // remove second and check size
        researchProject.removeCohort(collection);
        Assert.assertTrue(researchProject.getCohortIds().length == 1);

        Assert.assertNull(researchProject.getJiraTicketKey());

        Assert.assertEquals(researchProject.fetchJiraIssueType(), CreateIssueRequest.Fields.Issuetype.Research_Project);

        Assert.assertEquals(researchProject.fetchJiraProject(), CreateIssueRequest.Fields.ProjectType.Research_Projects);

        try {
            researchProject.setJiraTicketKey(null);
            Assert.fail();
        } catch(NullPointerException npe) {
            /*
            Ensuring Null is thrown for setting null
             */
        } finally {
            researchProject.setJiraTicketKey(RESEARCH_PROJ_JIRA_KEY);
        }

        Assert.assertNotNull(researchProject.getJiraTicketKey());

        Assert.assertEquals(researchProject.getJiraTicketKey(),RESEARCH_PROJ_JIRA_KEY);


    }
}
