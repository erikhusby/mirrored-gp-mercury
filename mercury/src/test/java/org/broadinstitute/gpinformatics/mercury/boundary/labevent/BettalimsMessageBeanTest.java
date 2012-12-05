package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.test.BettaLimsMessageFactory;
import org.hornetq.jms.client.HornetQJMSConnectionFactory;
import org.testng.annotations.Test;
import sun.rmi.transport.TransportConstants;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.HashMap;

/**
 * Test JMS
 */
public class BettalimsMessageBeanTest extends ContainerTest {
    @Test
    public void testJms() {
        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        PlateTransferEventType plateTransferEventType = bettaLimsMessageFactory.buildPlateToPlate(
                LabEventType.POST_SHEARING_TRANSFER_CLEANUP.getName(), "x", "y");
        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateTransferEvent().add(plateTransferEventType);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(BettaLIMSMessage.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            StringWriter stringWriter = new StringWriter();
            marshaller.marshal(bettaLIMSMessage, stringWriter);
            sendJmsMessage(stringWriter.toString());
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendJmsMessage(String message) {
        Context jndiContext = null;
        Connection connection = null;
        Session session = null;
        try{
//            jndiContext = new InitialContext();
//            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("jms/ConnectionFactory");
//            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://node115:61616");
            HashMap<String, Object> connectionParams = new HashMap<String, Object>();

            connectionParams.put(TransportConstants.HOST_PROP_NAME, "localhost");
            connectionParams.put(TransportConstants.PORT_PROP_NAME, "5445");

            new HornetQJMSConnectionFactory(false, );

            ServerLocator locator =
                    HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(
                            NettyConnectorFactory.class.getName(), connectionParams));
            // Create a Connection
            connection = connectionFactory.createConnection();
            connection.start();

            // Create a Session
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create the destination (Topic or Queue)
            Destination destination = session.createQueue("broad.queue.bettalims.thompson");

            // Create a MessageProducer from the Session to the Topic or Queue
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            TextMessage textMessage = session.createTextMessage(message);

            // Tell the producer to send the message
            System.out.println("Sent message: " + textMessage.hashCode() + " : " + Thread.currentThread().getName());
            producer.send(textMessage);

            // Clean up

        } catch (NamingException e) {
            throw new RuntimeException(e);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (jndiContext != null) {
                    jndiContext.close();
                }
                if (session != null) {
                    session.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (NamingException e) {
                // do nothing
            } catch (JMSException e) {
                // do nothing
            }
        }
    }
}
