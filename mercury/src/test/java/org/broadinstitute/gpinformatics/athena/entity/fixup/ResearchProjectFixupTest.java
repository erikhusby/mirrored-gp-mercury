package org.broadinstitute.gpinformatics.athena.entity.fixup;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
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
}
