package org.broadinstitute.gpinformatics.mercury.boundary.printing;

import org.broadinstitute.bsp.client.printing.PrintingMessage;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;

import javax.annotation.Resource;
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
import java.util.Collections;
import java.util.List;

/**
 * Resource used for connecting to the PrintingMessageBean queue.
 */
@Dependent
public class PrintingMessageResource {

    @Inject
    private AppConfig appConfig;

    @Inject
    SessionContextUtility sessionContextUtility;

    @Resource(lookup="java:/jms/RemoteMercuryConnectionFactory")
    private ConnectionFactory connectionFactory;

    public void sendPrinterMessage(PrintingMessage message) throws JMSException {
        sendPrinterMessages(Collections.singletonList(message));
    }

    public void sendPrinterMessages(List<PrintingMessage> printingMessages) throws JMSException {
        Session session = null;
        try ( Connection connection = connectionFactory.createConnection())  {
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(getQueueName());
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            for (PrintingMessage printingMessage : printingMessages) {
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
}
