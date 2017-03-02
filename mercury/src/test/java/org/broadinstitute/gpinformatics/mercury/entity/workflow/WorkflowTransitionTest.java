package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.SamplesDaughterPlateHandler;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

/**
 * Test jira transitions configured in workflow
 */
@Test(groups = TestGroups.STANDARD)
public class WorkflowTransitionTest extends Arquillian {

    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("MMddHHmmss");

    // For In Consolidation - AutomatedDaughter event
    private String bspTubeBarocde = "0213738634";

    private String genomeJiraTicket = "LCSET-10550";

    @Inject
    private JiraService jiraService;

    @Inject
    private JiraCommentUtil jiraCommentUtil;

    @Inject
    private LabEventFactory labEventFactory;

    private JiraIssue issue;

    private SamplesDaughterPlateHandler mockBspHandler;

    private BettaLimsMessageTestFactory bettaLimsMessageTestFactory;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @BeforeMethod
    public void setupJiraStatus() throws IOException {
        if (jiraService != null) {
            resetJiraTicketState();
        }
        if (labEventFactory != null && mockBspHandler == null){
            mockBspHandler = mock(SamplesDaughterPlateHandler.class);
            doNothing().when(mockBspHandler).postToBsp(any(BettaLIMSMessage.class), any(String.class));
            labEventFactory.setSamplesDaughterPlateHandler(mockBspHandler);
            bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);
        }
    }

    @Test
    public void testAutomatedDaughterShouldTransitionToInPlating() throws IOException {
        Assert.assertEquals(issue.getStatus(), "On Hold");
        issue.postTransition("Ready For Plating", null);

        String sourceRackBarcode = "InPlatingSource" + timestampFormat.format(new Date());
        String destRackBarcode = "InPlatingDest" + timestampFormat.format(new Date());
        String destTube = "InPlatingDestTube" + timestampFormat.format(new Date());

        List<String> sourceTubeBarcodes = Arrays.asList(bspTubeBarocde);
        List<String> destTubeBarcodes = Arrays.asList(destTube);
        PlateTransferEventType plateTransferEventType = bettaLimsMessageTestFactory.buildRackToRack(
                LabEventType.AUTO_DAUGHTER_PLATE_CREATION.getName(), sourceRackBarcode, sourceTubeBarcodes,
                destRackBarcode, destTubeBarcodes);

        BettaLIMSMessage message = new BettaLIMSMessage();
        message.getPlateTransferEvent().add(plateTransferEventType);
        List<LabEvent> labEvents = labEventFactory.buildFromBettaLims(message);
        LabEvent labEvent = labEvents.get(0);
        jiraCommentUtil.postUpdate(labEvent);
    }

    private void resetJiraTicketState() throws IOException {
        issue = jiraService.getIssue(genomeJiraTicket);
        Assert.assertNotNull(issue);
        Assert.assertEquals(issue.getKey(), genomeJiraTicket);
        issue.postTransition("Put On Hold", null);
        issue = jiraService.getIssue(genomeJiraTicket);
        Assert.assertEquals(issue.getStatus(), "On Hold");
    }
}
