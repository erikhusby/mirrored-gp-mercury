package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.SamplesDaughterPlateHandler;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private String bspTubeBarocde = "1124883373";

    private String shearingTubeBarcode = "0209086751";

    // For In LC - EndRepair event
    private String endRepairPlate = "000009167473";

    private String genomeJiraTicket = "LCSET-9712";

    private String crspJiraTicket = "LCSET-10359";

    private String exexJiraTicket = "LCSET-10343";

    @Inject
    private JiraService jiraService;

    @Inject
    private JiraCommentUtil jiraCommentUtil;

    @Inject
    private LabEventFactory labEventFactory;

    private JiraIssue genomeIssue;

    private JiraIssue crspIssue;

    private JiraIssue exexIssue;

    private SamplesDaughterPlateHandler mockBspHandler;

    private BettaLimsMessageTestFactory bettaLimsMessageTestFactory;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @BeforeMethod
    public void setupJiraStatus() throws IOException {
        if (jiraService != null) {
            genomeIssue = resetJiraTicketState(genomeJiraTicket);
            crspIssue = resetJiraTicketState(crspJiraTicket);
            exexIssue = resetJiraTicketState(exexJiraTicket);
        }
        if (labEventFactory != null && mockBspHandler == null){
            mockBspHandler = mock(SamplesDaughterPlateHandler.class);
            doNothing().when(mockBspHandler).postToBsp(any(BettaLIMSMessage.class), any(String.class));
            labEventFactory.setSamplesDaughterPlateHandler(mockBspHandler);
            bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);
        }
    }

    @Test
    public void testHandleMultipleLabEventNodes() throws Exception {
        String sourceRackBarcode = "InPlatingSource" + timestampFormat.format(new Date());
        String destPlateBarcode = "InPlatingPicoPlate" + timestampFormat.format(new Date());
        List<String> sourceTubeBarcodes = Arrays.asList("1125699249"); //Crsp plating tube
        PlateTransferEventType plateTransferEventType = bettaLimsMessageTestFactory.buildRackToPlate(
                LabEventType.PICO_TRANSFER.getName(), sourceRackBarcode, sourceTubeBarcodes,
                destPlateBarcode);
        BettaLIMSMessage message = new BettaLIMSMessage();
        message.getPlateTransferEvent().add(plateTransferEventType);
        List<LabEvent> labEvents = labEventFactory.buildFromBettaLims(message);
        LabEvent labEvent = labEvents.get(0);
        jiraCommentUtil.postUpdate(labEvent);
    }

    @Test
    public void testAutomatedDaughterShouldTransitionToInPlating() throws IOException {
        Assert.assertEquals(genomeIssue.getStatus(), "On Hold");
        genomeIssue.postTransition("Ready for Plating", null);

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
        genomeIssue = jiraService.getIssue(genomeJiraTicket);
        Assert.assertEquals(genomeIssue.getStatus(), "In Plating");
    }

    @Test
    public void testShearingTransferTransitionsToInShearing() throws IOException {
        Assert.assertEquals(genomeIssue.getStatus(), "On Hold");
        genomeIssue.postTransition("Ready for Booking", null);

        String sourceRackBarcode = "ShearingRackSource" + timestampFormat.format(new Date());
        String destPlateBarcode = "CovarisRackPlateBarcode" + timestampFormat.format(new Date());

        List<String> sourceTubeBarcodes = Arrays.asList(shearingTubeBarcode);
        PlateTransferEventType plateTransferEventType = bettaLimsMessageTestFactory
                .buildRackToPlate(LabEventType.SHEARING_TRANSFER.getName(), sourceRackBarcode,
                        sourceTubeBarcodes, destPlateBarcode);

        BettaLIMSMessage message = new BettaLIMSMessage();
        message.getPlateTransferEvent().add(plateTransferEventType);
        List<LabEvent> labEvents = labEventFactory.buildFromBettaLims(message);
        LabEvent labEvent = labEvents.get(0);
        jiraCommentUtil.postUpdate(labEvent);
        genomeIssue = jiraService.getIssue(genomeJiraTicket);
        Assert.assertEquals(genomeIssue.getStatus(), "In Shearing");
    }

    @Test
    public void testEndRepairTransitionsToInLC() throws IOException {
        Assert.assertEquals(genomeIssue.getStatus(), "On Hold");
        genomeIssue.postTransition("In Shearing", null);

        PlateEventType endRepairEventType =
                bettaLimsMessageTestFactory.buildPlateEvent(LabEventType.END_REPAIR.getName(), endRepairPlate);

        BettaLIMSMessage message = new BettaLIMSMessage();
        message.getPlateEvent().add(endRepairEventType);
        List<LabEvent> labEvents = labEventFactory.buildFromBettaLims(message);
        LabEvent labEvent = labEvents.get(0);
        jiraCommentUtil.postUpdate(labEvent);
        genomeIssue = jiraService.getIssue(genomeJiraTicket);
        Assert.assertEquals(genomeIssue.getStatus(), "In LC");
    }

    @Test
    public void testPoolCorrectionTransitionsToSeq() throws IOException {
        Assert.assertEquals(genomeIssue.getStatus(), "On Hold");
        genomeIssue.postTransition("Return to Ready For Sequencing", null);
        genomeIssue.postTransition("Pool Correction", null);
        genomeIssue = jiraService.getIssue(genomeJiraTicket);
        Assert.assertEquals(genomeIssue.getStatus(), "Pool Correction");

        String poolCorrectionTube = "0203813597";
        String rackPlateBarcode = "PoolCorrectionRackBarcode_" + timestampFormat.format(new Date());
        PlateEventType poolCorrectionEvent =
                bettaLimsMessageTestFactory.buildRackEvent(LabEventType.POOL_CORRECTION.getName(), rackPlateBarcode,
                        Collections.singletonList(poolCorrectionTube));

        BettaLIMSMessage message = new BettaLIMSMessage();
        message.getPlateEvent().add(poolCorrectionEvent);
        List<LabEvent> labEvents = labEventFactory.buildFromBettaLims(message);
        LabEvent labEvent = labEvents.get(0);
        jiraCommentUtil.postUpdate(labEvent);
        genomeIssue = jiraService.getIssue(genomeJiraTicket);
        Assert.assertEquals(genomeIssue.getStatus(), "Ready for Sequencing");
    }

    @Test
    public void testInLCToInPlexPooling() throws IOException {
        Assert.assertEquals(crspIssue.getStatus(), "On Hold");
        crspIssue.postTransition("In Library Construction", null);
        crspIssue = jiraService.getIssue(crspJiraTicket);
        Assert.assertEquals(crspIssue.getStatus(), "In LC");

        String sourceRackBarcode = "InPlexingSource" + timestampFormat.format(new Date());
        String targetRackBarcode = "InPlexingDest" + timestampFormat.format(new Date());
        String targetTubeBarcode = "InPlexingDestTube" + timestampFormat.format(new Date());

        List<List<String>> sourceTubeBarcodes = new ArrayList<>();
        sourceTubeBarcodes.add(Arrays.asList("0214238488"));

        List<List<String>> targetTubeBarcodes = new ArrayList<>();
        targetTubeBarcodes.add(Arrays.asList(targetTubeBarcode));


        BettaLimsMessageTestFactory.CherryPick cherryPick = new BettaLimsMessageTestFactory.CherryPick(sourceRackBarcode,
                "A01", targetRackBarcode, "A01");
        PlateCherryPickEvent plateCherryPickEvent =
                bettaLimsMessageTestFactory.buildCherryPick(LabEventType.ICE_POOLING_TRANSFER.getName(),
                        Arrays.asList(sourceRackBarcode), sourceTubeBarcodes, Arrays.asList(targetRackBarcode),
                        targetTubeBarcodes, Arrays.asList(cherryPick));

        BettaLIMSMessage message = new BettaLIMSMessage();
        message.getPlateCherryPickEvent().add(plateCherryPickEvent);
        List<LabEvent> labEvents = labEventFactory.buildFromBettaLims(message);
        LabEvent labEvent = labEvents.get(0);
        jiraCommentUtil.postUpdate(labEvent);
        crspIssue = jiraService.getIssue(crspJiraTicket);
        Assert.assertEquals(crspIssue.getStatus(), "In Plex Pooling");
    }

    @Test
    public void testInPlexPoolingToPostLC() throws IOException {
        Assert.assertEquals(exexIssue.getStatus(), "On Hold");
        exexIssue.postTransition("In Plex Pooling", null);
        exexIssue = jiraService.getIssue(exexJiraTicket);
        Assert.assertEquals(exexIssue.getStatus(), "In Plex Pooling");

        String sourceRackBarcode = "Ice96PlexSpriConcentrationSource" + timestampFormat.format(new Date());
        String targetRackBarcode = "Ice96PlexSpriConcentrationDest" + timestampFormat.format(new Date());
        String targetTubeBarcode = "Ice96PlexSpriTransitionTestDest" + timestampFormat.format(new Date());

        List<List<String>> sourceTubeBarcodes = new ArrayList<>();
        sourceTubeBarcodes.add(Arrays.asList("AB56984661"));

        List<List<String>> targetTubeBarcodes = new ArrayList<>();
        targetTubeBarcodes.add(Arrays.asList(targetTubeBarcode));


        BettaLimsMessageTestFactory.CherryPick cherryPick = new BettaLimsMessageTestFactory.CherryPick(sourceRackBarcode,
                "A01", targetRackBarcode, "A01");
        PlateCherryPickEvent plateCherryPickEvent =
                bettaLimsMessageTestFactory.buildCherryPick(LabEventType.ICE_96_PLEX_SPRI_CONCENTRATION.getName(),
                        Arrays.asList(sourceRackBarcode), sourceTubeBarcodes, Arrays.asList(targetRackBarcode),
                        targetTubeBarcodes, Arrays.asList(cherryPick));

        BettaLIMSMessage message = new BettaLIMSMessage();
        message.getPlateCherryPickEvent().add(plateCherryPickEvent);
        List<LabEvent> labEvents = labEventFactory.buildFromBettaLims(message);
        LabEvent labEvent = labEvents.get(0);
        jiraCommentUtil.postUpdate(labEvent);
        exexIssue = jiraService.getIssue(exexJiraTicket);
        Assert.assertEquals(exexIssue.getStatus(), "In Post LC");
    }

    @Test
    public void testPoolingTransferTransitionsToInSequencingPrep() throws IOException {
        Assert.assertEquals(exexIssue.getStatus(), "On Hold");
        exexIssue.postTransition("In Post LC", null);

        String sourceRackBarcode = "PoolingSource" + timestampFormat.format(new Date());
        String targetRackBarcode = "PoolingDest" + timestampFormat.format(new Date());
        String targetTubeBarcode = "PoolingDestTube" + timestampFormat.format(new Date());

        List<List<String>> sourceTubeBarcodes = new ArrayList<>();
        sourceTubeBarcodes.add(Arrays.asList("0219096826"));

        List<List<String>> targetTubeBarcodes = new ArrayList<>();
        targetTubeBarcodes.add(Arrays.asList(targetTubeBarcode));


        BettaLimsMessageTestFactory.CherryPick cherryPick = new BettaLimsMessageTestFactory.CherryPick(sourceRackBarcode,
                "A01", targetRackBarcode, "A01");
        PlateCherryPickEvent plateCherryPickEvent =
                bettaLimsMessageTestFactory.buildCherryPick(LabEventType.POOLING_TRANSFER.getName(),
                        Arrays.asList(sourceRackBarcode), sourceTubeBarcodes, Arrays.asList(targetRackBarcode),
                        targetTubeBarcodes, Arrays.asList(cherryPick));

        BettaLIMSMessage message = new BettaLIMSMessage();
        message.getPlateCherryPickEvent().add(plateCherryPickEvent);
        List<LabEvent> labEvents = labEventFactory.buildFromBettaLims(message);
        LabEvent labEvent = labEvents.get(0);
        jiraCommentUtil.postUpdate(labEvent);
        exexIssue = jiraService.getIssue(exexJiraTicket);
        Assert.assertEquals(exexIssue.getStatus(), "In Sequencing Prep");
    }

    @Test
    public void testDilutionToFlowcell() throws IOException {
        Assert.assertEquals(exexIssue.getStatus(), "On Hold");
        exexIssue.postTransition("Norm and Pool", null);

        String targetFlowcellBarcode = "FlowcellBarcodeFromInSeqPrep" + timestampFormat.format(new Date());

        String dilutionTubeBarcode = "0192198284";

        ReceptaclePlateTransferEvent flowcellTransferJaxb =
                bettaLimsMessageTestFactory.buildTubeToPlate("DilutionToFlowcellTransfer",
                        dilutionTubeBarcode, targetFlowcellBarcode, LabEventTest.PHYS_TYPE_FLOWCELL_2_LANE,
                        LabEventTest.SECTION_ALL_2, "tube");

        BettaLIMSMessage message = new BettaLIMSMessage();
        message.getReceptaclePlateTransferEvent().add(flowcellTransferJaxb);
        List<LabEvent> labEvents = labEventFactory.buildFromBettaLims(message);
        LabEvent labEvent = labEvents.get(0);
        jiraCommentUtil.postUpdate(labEvent);
        exexIssue = jiraService.getIssue(exexJiraTicket);
        Assert.assertEquals(exexIssue.getStatus(), "In Sequencing");
    }

    private JiraIssue resetJiraTicketState(String ticketKey) throws IOException {
        JiraIssue issue = jiraService.getIssue(ticketKey);
        Assert.assertNotNull(issue);
        Assert.assertEquals(issue.getKey(), ticketKey);
        issue.postTransition("Put On Hold", null);
        issue = jiraService.getIssue(ticketKey);
        Assert.assertEquals(issue.getStatus(), "On Hold");
        return issue;
    }
}
