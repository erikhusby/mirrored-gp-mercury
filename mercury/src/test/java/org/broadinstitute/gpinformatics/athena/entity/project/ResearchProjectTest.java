package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;


/**
 * Simple test of the research project without any database connection.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ResearchProjectTest {

    private static final String RESEARCH_PROJ_JIRA_KEY = "RP-1";

    private ResearchProject researchProject;

    @BeforeMethod
    public void setUp() throws Exception {
        researchProject = ResearchProjectTestFactory
                .createDummyResearchProject(10950, "MyResearchProject", "To Study Stuff", ResearchProject.IRB_ENGAGED);
    }

    @Test
    public void testProjectHierarchy() {
        /**
         * Create a self-join association.
         */
        researchProject.setParentResearchProject(researchProject);
        try {
            researchProject.prePersist();
            Assert.fail("Should have thrown an exception about improper hierarchy!");
        } catch (Exception e) {
        }

        /**
         * Create a looped association.
         */
        ResearchProject anotherResearchProject = ResearchProjectTestFactory
                .createDummyResearchProject(10950, "AnotherResearchProject", "To Study Stuff",
                        ResearchProject.IRB_ENGAGED);
        researchProject.setParentResearchProject(anotherResearchProject);
        anotherResearchProject.setParentResearchProject(researchProject);

        try {
            researchProject.prePersist();
            Assert.fail("Should have thrown an exception about improper hierarchy!");
        } catch (Exception e) {
        }

        /**
         * Positive test -- this should work fine, no loops.
         */
        anotherResearchProject.setParentResearchProject(null);
        researchProject.setParentResearchProject(anotherResearchProject);
        researchProject.prePersist();

        // Using title to discern projects from each other because the equals() is just looking at business key.
        Assert.assertEquals(anotherResearchProject.getTitle(),
                anotherResearchProject.getRootResearchProject().getTitle(), "There is no parent Research Project, so " +
                                                                            "the root should be itself.");

        ResearchProject rootResearchProject = researchProject.getRootResearchProject();
        Assert.assertNotEquals(researchProject.getTitle(), rootResearchProject.getTitle(), "The parent Research " +
                                                                                           "Project should not be the same the root.");

        Assert.assertEquals(researchProject.getRootResearchProject().getTitle(), anotherResearchProject.getTitle(),
                "The parent Research Project should be the same same as the root.");
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

        assertThat(researchProject.fetchJiraIssueType(), is(equalTo(
                (Deployment.isCRSP) ? CreateFields.IssueType.CLIA_RESEARCH_PROJECT :
                        CreateFields.IssueType.RESEARCH_PROJECT)));

        assertThat(researchProject.fetchJiraProject(), is(equalTo(
                (Deployment.isCRSP) ? CreateFields.ProjectType.CRSP_RESEARCH_PROJECTS :
                        CreateFields.ProjectType.RESEARCH_PROJECTS)));

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

    private static ResearchProject createResearchProjectForACLTest(ResearchProject parent, boolean aclEnabled,
                                                                   Long testUser) {
        ResearchProject project = ResearchProjectTestFactory.createTestResearchProject();
        project.setJiraTicketKey("TEST-" + UUID.randomUUID().toString());
        project.setParentResearchProject(parent);
        project.setAccessControlEnabled(aclEnabled);
        if (testUser != null) {
            project.addPerson(RoleType.BROAD_PI, testUser);
        }
        return project;
    }

    @DataProvider(name = "collectAccessible")
    public Object[][] testCollectAccessibleDataProvider() {

        long testUser = 1234;

        // Trivial tests
        ResearchProject anyAllowed = createResearchProjectForACLTest(null, false, null);
        ResearchProject noneAllowed = createResearchProjectForACLTest(null, true, null);
        ResearchProject oneAllowed = createResearchProjectForACLTest(null, true, testUser);
        ResearchProject otherAllowed = createResearchProjectForACLTest(null, true, testUser + 1);

        // Create test tree:
        // root - ACL enabled, doesn't have user
        // - p1 - ACL enabled, has user
        //   - p11 - ACL enabled, doesn't have user
        //   - p12 - ACL enabled, has user
        // - p2 - ACL not enabled, doesn't have user
        //   - p21 - ACL enabled, doesn't have user
        //     - p31 - ACL enabled, has user
        //   - p22 - ACL not enabled, has user

        ResearchProject root = createResearchProjectForACLTest(null, true, null);
        ResearchProject p1 = createResearchProjectForACLTest(root, true, testUser);
        ResearchProject p11 = createResearchProjectForACLTest(p1, true, null);
        ResearchProject p12 = createResearchProjectForACLTest(p1, true, testUser);
        ResearchProject p2 = createResearchProjectForACLTest(root, false, null);
        ResearchProject p21 = createResearchProjectForACLTest(p2, true, null);
        ResearchProject p31 = createResearchProjectForACLTest(p21, true, testUser);
        ResearchProject p22 = createResearchProjectForACLTest(p2, false, testUser);

        return new Object[][]{
                {anyAllowed, testUser, Collections.singleton(anyAllowed)},
                {anyAllowed, testUser + 1, Collections.singleton(anyAllowed)},
                {noneAllowed, testUser, Collections.emptySet()},
                {oneAllowed, testUser, Collections.singleton(oneAllowed)},
                {oneAllowed, testUser + 1, Collections.emptySet()},
                {otherAllowed, testUser, Collections.emptySet()},
                {root, testUser, new HashSet<>(Arrays.asList(p1, p11, p12, p2, p31, p22))},
        };
    }

    @Test(dataProvider = "collectAccessible")
    public void testCollectAccessibleByUser(ResearchProject root, long userId, Set<ResearchProject> expected) {
        Set<ResearchProject> found = new HashSet<>();
        root.collectAccessibleByUser(userId, found);
        Assert.assertEquals(found, expected);
    }
}

