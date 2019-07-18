package org.broadinstitute.gpinformatics.mercury.boundary.printing;

/**
 * Handles sending printing messages to Zebra printers.
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.printing.Printer;
import org.broadinstitute.bsp.client.printing.PrintingMessage;
import org.broadinstitute.gpinformatics.athena.boundary.BuildInfoBean;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jmx.PrintingControl;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.jboss.weld.context.bound.BoundSessionContext;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;

/**
 * A Message Driven Bean to receive JMS messages from Mercury to send printing messages to Zebra printers.
 */
@SuppressWarnings("UnusedDeclaration")
@MessageDriven(name = "PrintingMessageBean", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        // Increase probability that messages are read in the order they were sent
//        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1")
//        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/broad.queue.mercury.printing.dev"),
//        @ActivationConfigProperty(propertyName = "connectorClassName", propertyValue ="org.hornetq.core.remoting.impl.netty.NettyConnectorFactory"),
//        @ActivationConfigProperty(propertyName = "connectionParameters", propertyValue = "host=gpinfx-jms;port=5445")
})
public class PrintingMessageBean implements MessageListener {

    private static final Log logger = LogFactory.getLog(PrintingMessageBean.class);

    // todo this needs to be loaded from a configuration of some kind to ensure environment specific settings.
    public static final String QUEUE_NAME = "broad.queue.mercury.printing.dev";

    @Inject
    private BoundSessionContext sessionContext;

    @Inject
    private BeanManager beanManager;

    @Inject
    private SessionContextUtility sessionContextUtility;

    @Inject
    private AppConfig appConfig;

    @Inject
    private EmailSender emailSender;

    @Inject
    private BuildInfoBean buildInfoBean;

    public PrintingMessageBean() {
    }

    /**
     * Transaction is NOT_SUPPORTED because we don't want re-delivery in case of failure.  We store all messages
     * on the file system, and email in case of failure, so the recipient of the email can resubmit messages manually.
     *
     * @param message JMS message from deck
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void onMessage(final Message message) {
        sessionContextUtility.executeInContext(new SessionContextUtility.Function() {
            @Override
            public void apply() {
                // The deck side code is written in JavaScript, so it sends text messages, rather than object messages.
                if (message instanceof ObjectMessage) {
                    try {
                        ObjectMessage objMessage = (ObjectMessage) message;
                        sendPrinterMessage((PrintingMessage)objMessage.getObject());
                    } catch (Exception exception) {
                        emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(), Collections.<String>emptyList(),
                                "[Mercury] Failed to process and send a printing JMS message.", exception.getMessage(), false, true);
                        logger.error("Exception occurred with sending printer message. ", exception);
                    }
                }
            }
        });
    }

    /**
     * Takes a {@code PrintingMessage} and sends the containing ZPL script to the target printer using the hostname and port within the {@code Printer} object within the
     * PrintingMessage.
     *
     * @param message {@code PrintingMessage} object containing the ZPL script to send to a printer and the hostname & port of the printer to send the message.
     */
    private void sendPrinterMessage(PrintingMessage message) {
        // For each printer with content for printing open a direct socket and stream the ZPL

        if (!isPrintingEnabled()) {
            logger.info("Printing disabled, otherwise a label would be getting printed now.");
            logger.info("Sending to: "+ message.getPrinterHostName());
            logger.info("Printing ZPL: " + message.getZplScript());
            return;
        }

        Socket zebraSocket = null;
        DataOutputStream os = null;
        DataInputStream is = null;

        // Print out the ZPL message, for debuging the random missed label problem
        Printer printer = message.getPrinter();
        logger.info("Printing " +message.getLabelName() + " ZPL to " + printer.getHostName() + ":" + printer.getPort() + "\n" + message.getZplScript());

        // Initialization section:
        // Try to open a socket
        // Try to open input and output streams

        try {
            zebraSocket = new Socket(printer.getHostName(), printer.getPort());
            os = new DataOutputStream(zebraSocket.getOutputStream());
            is = new DataInputStream(zebraSocket.getInputStream());
        } catch (UnknownHostException e) {
            logger.error(String.format("Don't know about host: %s:%d", printer.getHostName(), printer.getPort(), e));
        } catch (IOException e) {
            logger.error(String.format("Couldn't get I/O for the connection to: %s:%d", printer.getHostName(),
                    printer.getPort(), e));
        }

        // If everything has been initialized then we want to write some data
        // to the socket we have opened a connection to on port passed

        if (zebraSocket != null && os != null && is != null) {
            try {

                os.writeBytes(message.getZplScript());

                // No point reading back, printer doesn't send any thing, humph!
            /*
             * String responseLine; while ((responseLine = is.readLine()) != null) {
             * log.debug("Server: " + responseLine); }
             */

                // clean up:
                // close the output stream
                // close the input stream
                // close the socket
                try {
                    Thread.sleep(1000);
                } catch (Exception ex) {
                }

                os.close();
                is.close();
                zebraSocket.close();
            } catch (UnknownHostException e) {
                logger.error("Trying to connect to unknown host: " + e);
            } catch (IOException e) {
                logger.error("IOException:  " + e);
            }
        }
    }

    /**
     * @return True if the environment is Production, if printing was enabled manually or if the setting to manually
     * enable printing hasn't been initialized.
     */
    private boolean isPrintingEnabled() {
        Deployment deployment = appConfig.getDeploymentConfig();
        boolean isProduction = deployment.equals(Deployment.PROD);
        // Printing is enabled if either the printing bean has it set to true or it's production and no one disabled printing.
        return ((isProduction && PrintingControl.isPrintingEnabled() == null) || (
                PrintingControl.isPrintingEnabled() != null && PrintingControl.isPrintingEnabled()));
    }
}
