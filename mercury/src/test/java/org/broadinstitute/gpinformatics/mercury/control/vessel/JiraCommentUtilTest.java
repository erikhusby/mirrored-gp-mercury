package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test sending messaging status updates to JIRA
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class JiraCommentUtilTest {
    private JiraConfig jiraConfig = new JiraConfig(DEV);
    private JiraService jiraService = new JiraServiceImpl(jiraConfig);
    private AppConfig appConfig = AppConfig.produce(DEV);
    private BSPUserList bspUserList = new BSPUserList(BSPManagerFactoryProducer.testInstance());
    private WorkflowConfig workflowConfig = new WorkflowLoader().load();
    private JiraCommentUtil jiraCommentUtil=new JiraCommentUtil(jiraService, appConfig, bspUserList, workflowConfig);

    @Test(enabled = true, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testMessaging() {
        BarcodedTube barcodedTube = new BarcodedTube("1234");

        HashSet<LabVessel> starters = new HashSet<>();
        starters.add(barcodedTube);
        LabBatch labBatch = new LabBatch("LCSET-2690", starters, LabBatch.LabBatchType.WORKFLOW);
        labBatch.setJiraTicket(new JiraTicket(jiraService, "LCSET-2690"));
        barcodedTube.addNonReworkLabBatch(labBatch);

        List<LabVessel> labVessels = new ArrayList<>();
        labVessels.add(barcodedTube);
        jiraCommentUtil.postUpdate("test", labVessels, null);
    }
}
