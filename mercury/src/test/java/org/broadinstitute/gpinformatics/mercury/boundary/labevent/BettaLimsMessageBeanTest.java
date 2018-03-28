package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.apache.activemq.ActiveMQConnectionFactory;
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

/**
 * Test Message Driven Bean
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BettaLimsMessageBeanTest {

    @Test(enabled = false)
    public static void main( String[] args ) {
        sendJmsMessage("This is a JMS configuration test message", "broad.queue.mercury.bettalims.dev");
    }

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
        String url = "tcp://localhost:5445";
        try {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
            connection = connectionFactory.createConnection();
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(queueName);
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
