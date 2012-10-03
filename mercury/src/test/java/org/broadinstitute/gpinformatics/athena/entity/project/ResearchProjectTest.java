package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


/**
 * Simple test of the research project without any database connection
 */
@Test(groups = {TestGroups.DATABASE_FREE})
public class ResearchProjectTest {

    private ResearchProject researchProject;

    @BeforeMethod
    public void setUp() throws Exception {
        researchProject = new ResearchProject(1111L, "MyResearchProject", "To study stuff.");

        researchProject.addFunding(new ResearchProjectFunding(researchProject, "TheGrant"));
        researchProject.addFunding(new ResearchProjectFunding(researchProject, "ThePO"));

        researchProject.addIrbNumber(new ResearchProjectIRB(researchProject, "irb123"));
        researchProject.addIrbNumber(new ResearchProjectIRB(researchProject, "irb456"));

        researchProject.addPerson(RoleType.SCIENTIST, 111L);
        researchProject.addPerson(RoleType.SCIENTIST, 222L);
    }

    @Test(groups = {TestGroups.DATABASE_FREE})
    public void manageRPTest() {
        Assert.assertNotNull(researchProject.getPeople(RoleType.SCIENTIST));
        Assert.assertTrue(researchProject.getPeople(RoleType.PM).isEmpty());

        researchProject.addPerson(RoleType.PM, 333L);
        Assert.assertNotNull(researchProject.getPeople(RoleType.PM));

        //Add a collection
        ResearchProjectCohort collection = new ResearchProjectCohort(researchProject, "BSPCollection");
        researchProject.addCohort(collection);
        Assert.assertTrue(researchProject.getSampleCohorts().size() == 1);

        // Add a second and check size
        collection = new ResearchProjectCohort(researchProject, "AlxCollection2");
        researchProject.addCohort(collection);
        Assert.assertTrue(researchProject.getSampleCohorts().size() == 2);

        // remove second and check size
        researchProject.removeCohort(collection);
        Assert.assertTrue(researchProject.getSampleCohorts().size() == 1);
    }
}
