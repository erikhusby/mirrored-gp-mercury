package org.broadinstitute.gpinformatics.infrastructure.test.dbfree;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.Irb;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class ResearchProjectTestFactory {

    public static final long TEST_CREATOR = 10950;

    public static ResearchProject createDummyResearchProject(long createdBy, String title, String synopsis,
                                                             boolean irbNotEngaged) {
        ResearchProject researchProject = new ResearchProject(createdBy, title, synopsis, irbNotEngaged);

        Set<Funding> fundingList =
                Collections.singleton(new Funding(Funding.PURCHASE_ORDER, "A piece of Funding", "POFunding"));
        researchProject.populateFunding(fundingList);
        researchProject.addFunding(new ResearchProjectFunding(researchProject, "TheGrant"));
        researchProject.addFunding(new ResearchProjectFunding(researchProject, "ThePO"));

        Collection<Irb> irbs = Collections.singleton(new Irb("irbInitial", ResearchProjectIRB.IrbType.FARBER));
        researchProject.populateIrbs(irbs);
        researchProject
                .addIrbNumber(new ResearchProjectIRB(researchProject, ResearchProjectIRB.IrbType.BROAD, "irb123"));
        researchProject
                .addIrbNumber(new ResearchProjectIRB(researchProject, ResearchProjectIRB.IrbType.OTHER, "irb456"));

        researchProject.addPerson(RoleType.SCIENTIST, 111);
        researchProject.addPerson(RoleType.SCIENTIST, 222);
        researchProject.addPerson(RoleType.BROAD_PI, 10950);
        researchProject.addPerson(RoleType.BROAD_PI, 10951);
        return researchProject;
    }


    /**
     * This code is in the dbfree version of the ResearchProject factory despite the fact that
     * {@link org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList} is currently written as an EJB.
     * It's possible that test code could mock out BSPUserList, although at the time of this writing none of the
     * callers currently does.
     */
    public static ResearchProject createDummyResearchProject(
            BSPUserList userList, String researchProjectTitle) throws IOException {
        ResearchProject dummyProject =
                new ResearchProject(TEST_CREATOR, researchProjectTitle, "Simple test object for unit tests", true);

        BspUser user = userList.getById(TEST_CREATOR);
        dummyProject.addPeople(RoleType.PM, Collections.singletonList(user));
        dummyProject.submit();
        return dummyProject;
    }
}
