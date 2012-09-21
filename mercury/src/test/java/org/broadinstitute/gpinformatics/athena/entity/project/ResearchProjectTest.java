package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.TestGroups;
import org.broadinstitute.gpinformatics.athena.entity.bsp.BSPCollection;
import org.broadinstitute.gpinformatics.athena.entity.bsp.BSPCollectionID;
import org.broadinstitute.gpinformatics.athena.entity.common.Name;
import org.broadinstitute.gpinformatics.athena.entity.person.Person;
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

        Person programMgr = new Person("Erica", "Shefler", "shefler@broad", "1", RoleType.PROGRAM_PM );
        ResearchProject researchProject = new ResearchProject( programMgr,
                new Name("MyResearchProject"), "To study stuff."  );

        ResearchProject researchProject2 = new ResearchProject( programMgr,
                new Name("MyResearchProject"), "To study stuff."  );


        //assertReflectionEquals( researchProject, researchProject2);

        //Equal
        Assert.assertTrue(researchProject.equals(researchProject2));
        Assert.assertEquals(researchProject.hashCode(), researchProject2.hashCode());

        // test modification is same as creation
        Assert.assertTrue(researchProject.getCreation().equals(researchProject.getModification()));

        //Add a collection
        BSPCollection collection1 = new BSPCollection(new BSPCollectionID("12345"), "AlxCollection1");
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
        researchProject.addFunding(funding1);
        researchProject.addFunding(funding2);

        researchProject.addIrbNumber("irb123");
        researchProject.addIrbNumber("irb456");

        Person scientist1 = new Person("Adam", "Bass", "bass@broadinstitute.org", "2", RoleType.BROAD_SCIENTIST );
        Person scientist2 = new Person("Noel", "Burtt", "bass@broadinstitute.org", "3", RoleType.BROAD_SCIENTIST );
        researchProject.addSponsoringScientist( scientist1 );
        researchProject.addSponsoringScientist( scientist2 );

    }


    @Test
    public void testGettersSetters() throws Exception {

    }

}
