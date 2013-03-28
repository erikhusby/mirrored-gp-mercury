package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
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
        // good lord, looking up a custom field in jira is hard
        String gssrIdsText = (String)jiraService.getIssueFields(issue.getKey(),gssrField.values()).getFields().values().iterator().next();
        return gssrIdsText;
    }

    @Test
    public void test_jira_creation_from_batch() throws Exception {
        String expectedGssrText = "Starter1\n\nRework1 (rework)";
        Set<LabVessel> startingVessels = new HashSet<LabVessel>();
        startingVessels.add(new TwoDBarcodedTube("Starter1"));
        Set<LabVessel> reworkVessels = new HashSet<LabVessel>();
        reworkVessels.add(new TwoDBarcodedTube("Rework1"));

        LabBatch batch = new LabBatch("Test batch",startingVessels, LabBatch.LabBatchType.WORKFLOW);
        batch.addReworks(reworkVessels);
        batchEjb.batchToJira("andrew",null,batch);

        JiraIssue ticket = jiraService.getIssue(batch.getJiraTicket().getTicketId());

        //Map<String,CustomFieldDefinition> gssrField = jiraService.getCustomFields(LabBatch.RequiredSubmissionFields.GSSR_IDS.getFieldName());
        // good lord, looking up a custom field in jira is hard
        //String gssrIdsText = (String)jiraService.getIssueFields(ticket.getKey(),gssrField.values()).getFields().values().iterator().next();
        String gssrIdsText = getGssrFieldFromJiraTicket(ticket);

        assertThat(gssrIdsText,notNullValue());
        assertThat(gssrIdsText.trim(), equalTo(expectedGssrText.trim()));

        // now try it without a rework
        batch.getReworks().clear();
        batchEjb.batchToJira("andrew",null,batch);

        ticket = jiraService.getIssue(batch.getJiraTicket().getTicketId());
        gssrIdsText = getGssrFieldFromJiraTicket(ticket);
        assertThat("Starter1",equalTo(gssrIdsText.trim()));
    }

}
