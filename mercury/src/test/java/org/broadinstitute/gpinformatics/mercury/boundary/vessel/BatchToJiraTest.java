package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BatchToJiraTest extends Arquillian {

    @Inject
    LabBatchEjb batchEjb;

    @Inject
    JiraService jiraService;

    /**
     * Use test deployment here to talk to the actual jira
     * @return
     */
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST);
    }

    private String getGssrFieldFromJiraTicket(JiraIssue issue) throws IOException {
        Map<String,CustomFieldDefinition> gssrField = jiraService.getCustomFields(LabBatch.RequiredSubmissionFields.GSSR_IDS.getFieldName());
        String gssrIdsText = (String)jiraService.getIssueFields(issue.getKey(),gssrField.values()).getFields().values().iterator().next();
        return gssrIdsText;
    }

    @Test
    public void test_jira_creation_from_batch() throws Exception {
        String expectedGssrText = "SM-1\n\nSM-2 (rework)";
        Set<LabVessel> startingVessels = new HashSet<LabVessel>();
        LabVessel tube1 = new TwoDBarcodedTube("Starter1");
        tube1.addSample(new MercurySample("SM-1"));
        startingVessels.add(tube1);
        Set<LabVessel> reworkVessels = new HashSet<LabVessel>();
        LabVessel tube2 = new TwoDBarcodedTube("Rework1");
        tube2.addSample(new MercurySample("SM-2"));
        reworkVessels.add(tube2);

        LabBatch batch = new LabBatch("Test batch",startingVessels, LabBatch.LabBatchType.WORKFLOW);
        batch.addReworks(reworkVessels);
        batchEjb.batchToJira("andrew",null,batch);

        JiraIssue ticket = jiraService.getIssue(batch.getJiraTicket().getTicketId());

         String gssrIdsText = getGssrFieldFromJiraTicket(ticket);

        assertThat(gssrIdsText,notNullValue());
        assertThat(gssrIdsText.trim(), equalTo(expectedGssrText.trim()));

        // now try it without a rework
        batch.getReworks().clear();
        batchEjb.batchToJira("andrew",null,batch);

        ticket = jiraService.getIssue(batch.getJiraTicket().getTicketId());
        gssrIdsText = getGssrFieldFromJiraTicket(ticket);
        assertThat("SM-1",equalTo(gssrIdsText.trim()));
    }

}
