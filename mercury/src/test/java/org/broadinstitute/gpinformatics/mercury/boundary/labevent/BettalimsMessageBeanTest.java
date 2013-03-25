package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageFactory;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.testng.annotations.Test;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Test Message Driven Bean
 */
public class BettalimsMessageBeanTest {

    @Test(enabled = false)
    public void testJms() {
        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        PlateTransferEventType plateTransferEventType = bettaLimsMessageFactory.buildPlateToPlate(
                LabEventType.POST_SHEARING_TRANSFER_CLEANUP.getName(), "x", "y");
        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateTransferEvent().add(plateTransferEventType);
        String message = BettaLimsMessageFactory.marshal(bettaLIMSMessage);
        sendJmsMessage(message);
    }

    public static String marshalMessage(BettaLIMSMessage bettaLIMSMessage) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(BettaLIMSMessage.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            StringWriter stringWriter = new StringWriter();
            marshaller.marshal(bettaLIMSMessage, stringWriter);
            return stringWriter.toString();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendJmsMessage(String message) {
        Connection connection = null;
        Session session = null;
        try{
            Map<String, Object> connectionParams = new HashMap<String, Object>();
            connectionParams.put(TransportConstants.PORT_PROP_NAME, 5445);
            connectionParams.put(TransportConstants.HOST_PROP_NAME, "localhost");
//            connectionParams.put(TransportConstants.HOST_PROP_NAME, "gpinfx-jms");
            TransportConfiguration transportConfiguration = new TransportConfiguration(
                    NettyConnectorFactory.class.getName(), connectionParams);
            HornetQConnectionFactory connectionFactory = HornetQJMSClient.createConnectionFactoryWithoutHA(
                    JMSFactoryType.CF, transportConfiguration);

            connection = connectionFactory.createConnection();
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue("broad.queue.mercury.bettalims.dev");
//            Destination destination = session.createQueue("broad.queue.mercury.bettalims.production");
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            TextMessage textMessage = session.createTextMessage(message);
            System.out.println("Sent message: " + textMessage.hashCode() + " : " + Thread.currentThread().getName());
            producer.send(textMessage);

        } catch (JMSException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (JMSException e) {
                // do nothing
            }
        }
    }
}
