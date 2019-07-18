package org.broadinstitute.gpinformatics.mercury.boundary.printing;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.broadinstitute.bsp.client.location.LabLocation;
import org.broadinstitute.bsp.client.printing.LabelData;
import org.broadinstitute.bsp.client.printing.LabelDetail;
import org.broadinstitute.bsp.client.printing.PaperType;
import org.broadinstitute.bsp.client.printing.Printer;
import org.broadinstitute.bsp.client.printing.PrintingManager;
import org.broadinstitute.bsp.client.printing.PrintingMessage;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.PrintingMessageTestFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test process for triggering printing.
 */
@Dependent
@Test(groups = TestGroups.STANDARD)
public class PrintingMessageBeanTest extends Arquillian {

    @Inject
    AppConfig appConfig;

    @Inject
    SessionContextUtility sessionContextUtility;

    @Inject
    PrintingMessageResource printingMessageResource;

    private PrintingManager printingManager = new PrintingManager();

    // Required to get the correct configuration for running JMS queue tests on the bamboo server.  In that case,
    // we can't use localhost.
    //
    // NOTE: To run locally, you must change this to DEV.  Make sure you change it back before checking in!
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test(enabled = true)
    public void testCreatingPrintingMessage() {
        PaperType testPaper = PaperType.SAMPLE_WRAP_AROUND;
        String testBarcode = "SM-1234";

        // Get the expected paper type we'll use for the test and ensure it had the coded data in it.
        LabelDetail labelDetailByPaperType = PrintingManager.getLabelDetailByPaperType(testPaper);
        Assert.assertTrue(labelDetailByPaperType.getZplScript().contains("#root[\"labelData\"]"));
        // Check to ensure the test barcode isn't magically in the script
        Assert.assertFalse(labelDetailByPaperType.getZplScript().contains(testBarcode));

        PrintingMessage printerMessage = null;
        try {
            printerMessage = printingManager
                    .createPrintingMessage(new LabelData(testBarcode), testPaper, LabLocation.LAB_1182);
        } catch (Exception exception) {
            Assert.fail("Unexpected failure when attempting to create a printer message.", exception);
        }

        // Ensure that the zpl message no longer has the coded text.
        Assert.assertFalse(printerMessage.getZplScript().contains("#root[\"labelData\"]"));
        // Ensure that the test barcode is in the message being printed.
        Assert.assertTrue(printerMessage.getZplScript().contains(testBarcode));
    }

    /**
     * Note: this is an arquillian test, so requires a empty Wildfly server running to be able to run.
     */
    @Test(enabled = false)
    public void testActualPrinting() throws Exception {

        printingMessageResource.sendPrinterMessage(printingManager
                .createPrintingMessage(new LabelData("CO-44556677"), PaperType.PLATE_MATRIX, LabLocation.LAB_1182));

        // Sleep for 5-10 secs to give queue time to process the message.
        Thread.sleep(1000 * 5);
    }

   /**
     * Creates several printer message and adds them to the queue using the JMS API (local configuration).
     */
    @Test(enabled = false)
    public void testSendingMultiplePrintingMessages() {
        PrintingMessageTestFactory printingMessageTestFactory = new PrintingMessageTestFactory();
        try {
            List<PrintingMessage> messages = new ArrayList<>();

            messages.add(printingMessageTestFactory
                        .createPrintingMessage("CO-44556677", null, PaperType.PLATE_MATRIX, LabLocation.LAB_1182));
            messages.add(printingMessageTestFactory
                    .createPrintingMessage("SM-112233", null, PaperType.SAMPLE_WRAP_AROUND, LabLocation.LAB_1182));
            messages.add(printingMessageTestFactory
                    .createPrintingMessage("SM-445566", null, PaperType.SAMPLE_PAPER, LabLocation.LAB_1182));

            sendMessages(messages);
        } catch (Exception exception) {
            Assert.fail("Unexpected exception when printing.", exception);
        }
    }

    private void sendMessages(List<PrintingMessage> messages) {
        Session session = null;
        Connection connection = null;

        try {

            Map<String, Object> connectionParams = new HashMap<>();
            connectionParams.put(TransportConstants.PORT_PROP_NAME, 5445);
            connectionParams.put(TransportConstants.HOST_PROP_NAME, "localhost");
            connectionParams.put(TransportConstants.PROTOCOLS_PROP_NAME, "tcp" );

            TransportConfiguration transportConfiguration =
                    new TransportConfiguration(
                            "org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory",
                            connectionParams);
            ConnectionFactory connectionFactory = ActiveMQJMSClient
                    .createConnectionFactoryWithoutHA(JMSFactoryType.QUEUE_CF, transportConfiguration);

            connection = connectionFactory.createConnection();
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue("broad.queue.mercury.printing.dev");
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            for (PrintingMessage printingMessage : messages) {
                Message message = createMessage(session, printingMessage);
                producer.send(message);
            }

        } catch ( JMSException jmse ) {
            throw new RuntimeException( jmse.getMessage(), jmse.getLinkedException() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch ( JMSException jmse ) {}
            }

            if(connection != null) {
                try {
                    connection.close();
                } catch ( JMSException jmse ) {}
            }
        }
    }

    public static Message createMessage(Session session, PrintingMessage printingMessage) throws JMSException {
        ObjectMessage message = session.createObjectMessage();
        message.setObject(printingMessage);
        return message;
    }
}
