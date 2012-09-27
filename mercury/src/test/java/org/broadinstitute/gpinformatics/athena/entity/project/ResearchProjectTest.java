package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.athena.entity.common.Name;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.testng.Assert;
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

    private static final Logger LOG = Logger.getLogger(ResearchProjectTest.class);


    public void testResearchProject() throws Exception {

        Date start = new Date();

        Person programMgr = new Person("shefler@broad", "Erica", "Shefler");

        ResearchProject researchProject = new ResearchProject( programMgr,
                new Name("MyResearchProject"), "To study stuff."  );

        ResearchProject researchProject2 = new ResearchProject( programMgr,
                new Name("MyResearchProject"), "To study stuff."  );


        //Equal
        Assert.assertTrue(researchProject.equals(researchProject2));
        Assert.assertEquals(researchProject.hashCode(), researchProject2.hashCode());

        // test modification is same as creation
        Assert.assertTrue(researchProject.getCreation().equals(researchProject.getModification()));

        //Add a collection
        Cohort collection1 = new Cohort(new CohortID("12345"), "AlxCollection1");
        researchProject.addBSPCollection(collection1);

        //No longer equal
        Assert.assertFalse(researchProject.equals(researchProject2));
        Assert.assertNotEquals(researchProject.hashCode(), researchProject2.hashCode());

        Date stop = new Date();
        Funding funding1 = new Funding(Funding.FUNDS_RESERVATION, "SmallGrant");
        // TODO PMB this looks like a bug, there is no grant stop setter
        funding1.setGrantStartDate(start);
        funding1.setGrantStartDate(stop);
        funding1.setGrantNumber("100");
        funding1.setInstitute("NIH");

        Funding funding2 = new Funding(Funding.FUNDS_RESERVATION, "LargeGrant");
        funding2.setGrantStartDate(start);
        funding2.setGrantStartDate(stop);
        funding1.setGrantNumber("200");
        funding1.setInstitute("NHGRI");
        researchProject.addFunding(funding1.getFundingID());
        researchProject.addFunding(funding2.getFundingID());

        researchProject.addIrbNumber("irb123");
        researchProject.addIrbNumber("irb456");

        Person scientist1 = new Person("bass@broadinstitute.org", "Adam", "Bass" );
        Person scientist2 = new Person("bass@broadinstitute.org", "Noel", "Burtt" );
        researchProject.addPerson(RoleType.SCIENTIST, scientist1 );
        researchProject.addPerson(RoleType.SCIENTIST, scientist2 );

    }


    @Test
    public void testGettersSetters() throws Exception {

    }

}
