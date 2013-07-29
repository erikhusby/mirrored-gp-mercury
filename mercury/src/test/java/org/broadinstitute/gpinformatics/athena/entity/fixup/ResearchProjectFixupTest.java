package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
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
        List<ResearchProject> rpListToPersist = new ArrayList<>();
        int count = 0;
        StringBuilder userTitleList = new StringBuilder();
        for (ResearchProject rp : rpList) {
            if (StringUtils.isBlank(rp.getJiraTicketKey())) {
                try {
                    rp.submitToJira();
                    rpListToPersist.add(rp);
                } catch (Exception e) {
                    count++;
                    userTitleList.append(rp.getTitle()).append(":").append(rp.getCreatedBy()).append(" , ");
                }
            }
        }
        if (count > 0) {
            Assert.fail(count + " exceptions occurred. " + userTitleList);
        }

        // The entity is already persistent, this call to persist is solely to begin and end a transaction, so the
        // change gets flushed.  This is an artifact of the test environment.
        rpDao.persistAll(rpListToPersist);
    }

    /**
     * Helper method to change the owner of a research project.
     * @param newOwnerUsername new owner's username
     * @param projectKeys list of RP keys
     */
    private void changeProjectOwner(String newOwnerUsername, String... projectKeys) {
        for (BspUser user : bspUserList.find(newOwnerUsername)) {
            if (user.getUsername().equals(newOwnerUsername)) {
                for (String key : projectKeys) {
                    ResearchProject researchProject = rpDao.findByBusinessKey(key);
                    researchProject.setCreatedBy(user.getUserId());
                    rpDao.persist(researchProject);
                }
                return;
            }
        }

        throw new RuntimeException("No " + newOwnerUsername + " Found!");
    }

    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void reassignRPUser() {
        changeProjectOwner("stevens", "RP-28", "RP-30", "RP-80", "RP-81");
    }

    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void reassignRPUserGPLIM_1156() {
        changeProjectOwner("namrata", "RP-57");
    }
}
