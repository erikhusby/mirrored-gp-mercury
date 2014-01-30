package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ResearchProjectEjb;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * @author Scott Matthews
 *         Date: 10/12/12
 *         Time: 7:50 AM
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class ResearchProjectContainerTest extends Arquillian {
    @Inject
    private ResearchProjectEjb researchProjectEjb;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }


    public void testJiraSubmission() throws IOException, JiraIssue.NoTransitionException {

        ResearchProject dummy = ResearchProjectTestFactory
                .createDummyResearchProject(10950, "MyResearchProject", "To Study Stuff", ResearchProject.IRB_ENGAGED);

        researchProjectEjb.submitToJira(dummy);

        Assert.assertTrue(StringUtils.isNotEmpty(dummy.getJiraTicketKey()));
    }

}
