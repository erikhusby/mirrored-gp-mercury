package org.broadinstitute.gpinformatics.athena.entity.project;

import org.testng.Assert;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * @author Scott Matthews
 *         Date: 10/12/12
 *         Time: 7:50 AM
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class ResearchProjectContainerTest extends Arquillian {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }


    public void testJiraSubmission() throws IOException {

        ResearchProject dummy = AthenaClientServiceStub
                .createDummyResearchProject(10950, "MyResearchProject", "To Study Stuff", ResearchProject.IRB_ENGAGED);

        dummy.submit();

        Assert.assertTrue(StringUtils.isNotEmpty(dummy.getJiraTicketKey()));
    }

}
