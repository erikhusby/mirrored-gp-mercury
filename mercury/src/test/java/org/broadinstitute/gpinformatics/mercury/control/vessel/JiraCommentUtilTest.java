package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST;

/**
 * Test sending messaging status updates to JIRA
 */
public class JiraCommentUtilTest extends Arquillian {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(TEST);
    }

    @Inject
    private JiraService jiraService;

    @Inject
    private JiraCommentUtil jiraCommentUtil;

    @Test(enabled = true, groups = TestGroups.ALTERNATIVES)
    public void testMessaging() {
        BarcodedTube barcodedTube = new BarcodedTube("1234");

        HashSet<LabVessel> starters = new HashSet<>();
        starters.add(barcodedTube);
        LabBatch labBatch = new LabBatch("LCSET-2690", starters, LabBatch.LabBatchType.WORKFLOW);
        labBatch.setJiraTicket(new JiraTicket(jiraService, "LCSET-2690"));
        barcodedTube.addNonReworkLabBatch(labBatch);

        List<LabVessel> labVessels = new ArrayList<>();
        labVessels.add(barcodedTube);
        jiraCommentUtil.postUpdate("test", labVessels);
    }
}
