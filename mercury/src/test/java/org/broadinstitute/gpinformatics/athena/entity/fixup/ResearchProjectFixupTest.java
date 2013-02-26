package org.broadinstitute.gpinformatics.athena.entity.fixup;

import junit.framework.Assert;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * This "test" is an example of how to fixup some data.  Each fix method includes the JIRA ticket ID.
 * Set @Test(enabled=false) after running once.
 */
public class ResearchProjectFixupTest extends Arquillian {

    @Inject
    private ResearchProjectDao rpDao;

    @Inject
    private BSPUserList bspUserList;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    /**
     * For each Project missing a JIRA ticket poke the service to create & associate one
     */
    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void fixupGpLim95() throws IOException{
        List<ResearchProject> rpList = rpDao.findAllResearchProjects();
        List<ResearchProject> rpListToPersist = new ArrayList<ResearchProject>();
        int count =0;
        StringBuilder userTitleList = new StringBuilder();
        for (ResearchProject rp : rpList)
        {
            if (rp.getJiraTicketKey() == null || rp.getJiraTicketKey().trim().length() == 0)
            {
                // Create the JIRA
                try {
                    rp.submit();
                    rpListToPersist.add(rp);
                } catch ( Exception e ) {
                    count++;
                    userTitleList.append(rp.getTitle() +":" + rp.getCreatedBy() +" , ");
                }
            }
        }
        if (count > 0 ) {
            Assert.fail(count + " exceptions occurred. " + userTitleList.toString());
        }

        // The entity is already persistent, this call to persist is solely to begin and end a transaction, so the
        // change gets flushed.  This is an artifact of the test environment.
        rpDao.persistAll(rpListToPersist);
    }

    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void reassignRPUser() {
        String[] jiraKeys = new String[]{
                "RP-28",
                "RP-30",
                "RP-80",
                "RP-81"
        };

        // at the time of this writing this resolves to the one user we want, but add some checks to make sure that
        // remains the case
        List<BspUser> bspUsers = bspUserList.find("Christine Stevens");

        if (bspUsers == null || bspUsers.isEmpty()) {
            throw new RuntimeException("No Christine Stevens Found!");
        }

        if (bspUsers.size() > 1) {
            throw new RuntimeException("Too many Christine Stevens found!");
        }

        BspUser bspUser = bspUsers.get(0);
        if (!bspUser.getFirstName().equals("Christine") || !bspUser.getLastName().equals("Stevens")) {
            throw new RuntimeException("Wrong person found: " + bspUser);
        }

        for (String jiraKey : jiraKeys) {
            ResearchProject researchProject = rpDao.findByBusinessKey(jiraKey);
            researchProject.setCreatedBy(bspUser.getUserId());

            rpDao.persist(researchProject);
        }

    }
}
