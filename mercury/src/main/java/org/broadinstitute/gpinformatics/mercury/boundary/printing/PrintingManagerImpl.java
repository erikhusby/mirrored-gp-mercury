package org.broadinstitute.gpinformatics.mercury.boundary.printing;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.printing.PrintingManager;
import org.broadinstitute.bsp.client.printing.PrintingMessage;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
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
import java.util.Collections;
import java.util.List;

/**
 * Resource used for connecting to the PrintingMessageBean queue.
 */
@Singleton
public class PrintingManagerImpl extends PrintingManager {

    private static final Log log = LogFactory.getLog(PrintingManagerImpl.class);

    private boolean loadedSettings = false;

    @Inject
    private AppConfig appConfig;

    @Inject
    private EmailSender emailSender;

    @Resource(lookup="java:/jms/RemoteMercuryConnectionFactory")
    private ConnectionFactory connectionFactory;

    public PrintingManagerImpl() {
    }

    @Override
    protected void handleLoadingSettings() {

    }

    @PostConstruct
    public void loadSettingsPostConstruct() {
        loadingSettings();
    }

    /**
     * Attempt to handle loading of settings, which is done immediately after constructor.
     */
    public void loadingSettings(){
        try {

            MessageCollection messageCollection = loadAllSettings();

            if (messageCollection.hasErrors()) {
                loadedSettings = false;
                log.error("Unable to load printer and label detail settings.");
                String errors = StringUtils.join(messageCollection.getErrors());
                emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(), Collections.<String>emptyList(),
                        "[Mercury] Failed to load settings for printer and label details", errors, false, true);
            } else {
                loadedSettings = true;
            }
        } catch (Exception e) {
            loadedSettings = false;
            log.error("Unable to load printer and label detail settings.");
            emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(), Collections.<String>emptyList(),
                    "[Mercury] Failed to load settings for printer and label details", e.getMessage(), false, true);
        }
    }

    public void sendPrintMessages(PrintingMessage message) throws Exception {
        sendPrintMessages(Collections.singletonList(message));
    }

    @Override
    protected void sendPrintMessages(List<PrintingMessage> printingMessages) throws Exception {
        if(loadedSettings) {
            Session session = null;
            try (Connection connection = connectionFactory.createConnection()) {
                connection.start();

                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createQueue(getQueueName());
                MessageProducer producer = session.createProducer(destination);
                producer.setDeliveryMode(DeliveryMode.PERSISTENT);

                for (PrintingMessage printingMessage : printingMessages) {
                    Message message = createMessage(session, printingMessage);
                    producer.send(message);
                }
            } catch (JMSException jmse) {
                log.error("Failed to send printer message.", jmse);
                emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(), Collections.<String>emptyList(),
                        "[Mercury] Failed to send JMS message for printing", jmse.getMessage(), false, true);
                throw new RuntimeException(jmse.getMessage(), jmse.getLinkedException());
            } catch (Exception e) {
                log.error("Failed to send printer message.", e);
                emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(), Collections.<String>emptyList(),
                        "[Mercury] Failed to send JMS message for printing", e.getMessage(), false, true);
                throw new RuntimeException(e);
            } finally {
                if (session != null) {
                    try {
                        session.close();
                    } catch (JMSException jmse) {
                    }
                }
            }
        } else {
            throw new Exception("Printing is unable to work as settings did not load properly.");
        }
    }

    public static Message createMessage(Session session, PrintingMessage printingMessage) throws JMSException {
        ObjectMessage message = session.createObjectMessage();
        message.setObject(printingMessage);
        return message;
    }

    /**
     * @return Get the queue name for the environment loaded.
     */
    private String getQueueName() {
        return appConfig.getPrintingQueue();
    }

    /**
     * Re-load the printer and label details from their files.
     */
    public void reloadPrinterSettings() {
        loadAllSettings();
    }
}
