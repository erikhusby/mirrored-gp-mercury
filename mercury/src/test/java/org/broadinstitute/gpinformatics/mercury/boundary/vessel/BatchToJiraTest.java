package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
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
    ReworkEjb reworkEjb;

    @Inject
    JiraService jiraService;

    @Inject
    LabVesselDao labVesselDao;

    @Inject
    private UserTransaction transaction;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        if (transaction == null) {
            return;
        }
        transaction.begin();
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, since we're not running in container.
        if (transaction == null) {
            return;
        }
        transaction.rollback();
    }

    /**
     * Use test deployment here to talk to the actual jira
     *
     * @return
     */
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST);
    }

    private String getGssrFieldFromJiraTicket(JiraIssue issue) throws IOException {
        Map<String, CustomFieldDefinition> gssrField =
                jiraService.getCustomFields(LabBatch.RequiredSubmissionFields.GSSR_IDS.getFieldName());
        String gssrIdsText =
                (String) jiraService.getIssueFields(issue.getKey(), gssrField.values()).getFields().values().iterator()
                        .next();
        return gssrIdsText;
    }

    @Test(enabled = true)
    public void testJiraCreationFromBatch() throws Exception {
        String expectedGssrText = "SM-01\n\nSM-02 (rework)";
        Set<LabVessel> startingVessels = new HashSet<LabVessel>();
        String tube1Label = "Starter01";
        String tube2Label = "Rework01";

        LabVessel tube1 = new TwoDBarcodedTube(tube1Label);
        tube1.addSample(new MercurySample("SM-01"));
        startingVessels.add(tube1);
        Set<LabVessel> reworkVessels = new HashSet<LabVessel>();
        LabVessel tube2 = new TwoDBarcodedTube(tube2Label);
        tube2.addSample(new MercurySample("SM-02"));
        labVesselDao.persistAll(Arrays.asList(tube1, tube2));

        labVesselDao.flush();
        labVesselDao.clear();

        tube1 = labVesselDao.findByIdentifier(tube1Label);
        tube2 = labVesselDao.findByIdentifier(tube2Label);

        LabEvent event = new LabEvent(LabEventType.DENATURE_TO_FLOWCELL_TRANSFER, new Date(), "TEST-LAND", 0L, 101L);
        tube2.addInPlaceEvent(event);
        LabBatch batch = new LabBatch("Test batch 2", startingVessels, LabBatch.LabBatchType.WORKFLOW);

        batchEjb.batchToJira("andrew", null, batch);

        JiraIssue ticket = jiraService.getIssue(batch.getJiraTicket().getTicketId());

        String gssrIdsText = getGssrFieldFromJiraTicket(ticket);

        assertThat(gssrIdsText, notNullValue());
        assertThat(gssrIdsText.trim(), equalTo("SM-01"));

        // now try it with SM-02 as a rework
        reworkEjb.addReworkToBatch(batch, tube2Label, ReworkEntry.ReworkReason.MACHINE_ERROR,
                LabEventType.PICO_PLATING_BUCKET, "I am reworking this", WorkflowName.EXOME_EXPRESS.getWorkflowName(),
                "scottmat");
        batchEjb.batchToJira("andrew", null, batch);

        ticket = jiraService.getIssue(batch.getJiraTicket().getTicketId());
        gssrIdsText = getGssrFieldFromJiraTicket(ticket);
        assertThat(gssrIdsText.trim(), equalTo(expectedGssrText.trim()));
    }

}
