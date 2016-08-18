package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.testng.annotations.Test;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.HashMap;
import java.util.Map;

/**
 * Test Message Driven Bean
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BettaLimsMessageBeanTest {

    @Test(enabled = false)
    public void testJms() {
        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);
        PlateTransferEventType plateTransferEventType = bettaLimsMessageTestFactory.buildPlateToPlate(
                LabEventType.POST_SHEARING_TRANSFER_CLEANUP.getName(), "x", "y");
        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateTransferEvent().add(plateTransferEventType);
        String message = BettaLimsMessageTestFactory.marshal(bettaLIMSMessage);
        sendJmsMessage(message, "broad.queue.mercury.bettalims.dev");
    }

    // Have to return something other than void, otherwise TestNG will think it's a test.
    public static boolean sendJmsMessage(String message, String queueName) {
        Connection connection = null;
        Session session = null;
        try {
            Map<String, Object> connectionParams = new HashMap<>();
            connectionParams.put(TransportConstants.PORT_PROP_NAME, 5445);
            connectionParams.put(TransportConstants.HOST_PROP_NAME, "localhost");
//            connectionParams.put(TransportConstants.HOST_PROP_NAME, "gpinfx-jms");
            TransportConfiguration transportConfiguration = new TransportConfiguration(
                    NettyConnectorFactory.class.getName(), connectionParams);
            ActiveMQConnectionFactory connectionFactory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(
                    JMSFactoryType.CF, transportConfiguration);

            connectionFactory.setConnectionTTL(-1);
            connectionFactory.setClientFailureCheckPeriod(Long.MAX_VALUE);

            connection = connectionFactory.createConnection();
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(queueName);
//            Destination destination = session.createQueue("broad.queue.mercury.bettalims.production");
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            TextMessage textMessage = session.createTextMessage(message);
            System.out.println("Sent message: " + textMessage.hashCode() + " : " + Thread.currentThread().getName());
            producer.send(textMessage);

            return true;
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
