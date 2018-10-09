package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.GapHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowValidator;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Test(groups = TestGroups.STANDARD)
public class WorkflowContainerTest extends Arquillian {

    @Inject
    private LabEventFactory labEventFactory;

    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;

    @Inject
    private WorkflowValidator workflowValidator;

    private GapHandler mockGapHandler;

    public static final String INFINIUM_INPUT_PLATE = "CO-19456459";

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testMultiplePlateEvents() throws Exception {
        mockGapHandler = mock(GapHandler.class);
        doNothing().when(mockGapHandler).postToGap(any(BettaLIMSMessage.class));
        labEventFactory.setGapHandler(mockGapHandler);
        bettaLimsMessageResource.setLabEventFactory(labEventFactory);

        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);
        String ampPlate = "WorkflowAmpPlate" + timestamp;
        PlateTransferEventType infiniumAmplificationJaxb =
                bettaLimsMessageTestFactory.buildPlateToPlate("InfiniumAmplification", INFINIUM_INPUT_PLATE, ampPlate);

        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateTransferEvent().add(infiniumAmplificationJaxb);
        bettaLimsMessageResource.processMessage(bettaLIMSMessage);
        bettaLimsMessageTestFactory.advanceTime();

        PlateEventType infiniumAmplificationReagentAdditionJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("InfiniumAmplificationReagentAddition", ampPlate);
        bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateEvent().add(infiniumAmplificationReagentAdditionJaxb);
        bettaLimsMessageResource.processMessage(bettaLIMSMessage);
        bettaLimsMessageTestFactory.advanceTime();

        PlateEventType infiniumFragmentationJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "InfiniumFragmentation", ampPlate);

        PlateEventType infiniumPostFragmentationHybOvenLoadedJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("InfiniumPostFragmentationHybOvenLoaded", ampPlate);
        infiniumPostFragmentationHybOvenLoadedJaxb.setStation("Hyb Oven #1");
        infiniumPostFragmentationHybOvenLoadedJaxb.setDisambiguator(2L);
        bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateEvent().add(infiniumFragmentationJaxb);
        bettaLIMSMessage.getPlateEvent().add(infiniumPostFragmentationHybOvenLoadedJaxb);


        EmailSender emailSender = mock(EmailSender.class);
        workflowValidator.setEmailSender(emailSender);

        workflowValidator.validateWorkflow(bettaLIMSMessage);
        verify(emailSender, times(0)).
                sendHtmlEmail(any(AppConfig.class), anyString(), anyCollection(), anyString(), anyString(), anyBoolean(), anyBoolean());

        //Send again with some event that should fail
        PlateEventType endRepair =
                bettaLimsMessageTestFactory.buildPlateEvent("EndRepair", ampPlate);
        endRepair.setDisambiguator(3L);
        bettaLIMSMessage.getPlateEvent().add(endRepair);
        workflowValidator.validateWorkflow(bettaLIMSMessage);
        verify(emailSender, times(1)).
                sendHtmlEmail(any(AppConfig.class), anyString(), anyCollection(), anyString(), anyString(), anyBoolean(), anyBoolean());

    }
}
