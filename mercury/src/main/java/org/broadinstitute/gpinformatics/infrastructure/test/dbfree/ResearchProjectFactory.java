package org.broadinstitute.gpinformatics.infrastructure.test.dbfree;

import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.Irb;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class ResearchProjectFactory {
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
}
