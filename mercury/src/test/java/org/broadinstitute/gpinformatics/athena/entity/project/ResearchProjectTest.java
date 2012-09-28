package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;


/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/12/12
 * Time: 11:26 AM
 */
@Test(groups = {TestGroups.DATABASE_FREE})
public class ResearchProjectTest {

    private final Person programMgr = new Person("shefler@broad", "Erica", "Shefler");
    private ResearchProject researchProject;

    @BeforeMethod
    public void setUp() throws Exception {

        Date start = new Date();

        researchProject = new ResearchProject( programMgr, "MyResearchProject", "To study stuff.");

        Date stop = new Date();
        Funding funding1 = new Funding(Funding.FUNDS_RESERVATION, "TheGrant");
        funding1.setGrantStartDate(start);
        funding1.setGrantEndDate(stop);
        funding1.setGrantNumber("100");
        funding1.setInstitute("NIH");
        researchProject.addFunding(funding1.getFundingID());

        Funding funding2 = new Funding(Funding.PURCHASE_ORDER, "ThePO");
        funding2.setBroadName("BroadNameOfPO");
        funding2.setGrantStartDate(start);
        funding2.setGrantEndDate(stop);
        funding2.setPurchaseOrderNumber("200");
        funding2.setInstitute("NHGRI");
        researchProject.addFunding(funding2.getFundingID());

        researchProject.addIrbNumber("irb123");
        researchProject.addIrbNumber("irb456");

        Person scientist1 = new Person("bass@broadinstitute.org", "Adam", "Bass" );
        Person scientist2 = new Person("bass@broadinstitute.org", "Noel", "Burtt" );
        researchProject.addPerson(RoleType.SCIENTIST, scientist1 );
        researchProject.addPerson(RoleType.SCIENTIST, scientist2 );
    }

    @Test(groups = {TestGroups.DATABASE_FREE})
    public void manageRPTest() {
        Assert.assertNotNull(researchProject.getPeople(RoleType.SCIENTIST));
        Assert.assertTrue(researchProject.getPeople(RoleType.PM).isEmpty());

        Person scientist = new Person("bass@broadinstitute.org", "Noel", "Burtt" );
        researchProject.addPerson(RoleType.PM, scientist );
        Assert.assertNotNull(researchProject.getPeople(RoleType.PM));

        //Add a collection
        Cohort collection = new Cohort(new CohortID("12345"), "AlxCollection1");
        researchProject.addCohort(collection);
        Assert.assertTrue(researchProject.getSampleCohorts().size() == 1);
        collection = new Cohort(new CohortID("123456"), "AlxCollection2");
        researchProject.addCohort(collection);
        Assert.assertTrue(researchProject.getSampleCohorts().size() == 2);
        researchProject.removeCohort(collection);
        Assert.assertTrue(researchProject.getSampleCohorts().size() == 1);
    }
}
