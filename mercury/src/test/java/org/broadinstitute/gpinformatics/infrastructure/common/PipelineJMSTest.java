package org.broadinstitute.gpinformatics.infrastructure.common;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * This class is put together to validate any picard connectivity issues without the need to spin up the container.
 * Edit password as required before running, unless you're simply testing connectivity/protocol
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class PipelineJMSTest {

    @Test(enabled = false)
    public static void main( String[] args ) {
        PipelineJMSTest messageTester = new PipelineJMSTest();
        String url = String.format("failover://(ssl://picard-jms-dev.broadinstitute.org:61616)");
        String user = "mercury-pipeline";
        String password = "change-to-the-real-password";
        String queueName = "broad.arrays.enqueue.staging";
        String chipWellBarcode = "TEST-TEST-TEST";
        messageTester.sendJmsMessage(url, user, password, queueName, chipWellBarcode);
    }

    /**
     * Picard ActiveMQ transport configuration (March 2018):
     * <transportConnector name="ssl" uri="ssl://0.0.0.0:61616?maximumConnections=1000&amp;wireFormat.maxFrameSize=104857600"/>
     * <transportConnector name="tcp" uri="tcp://0.0.0.0:61617?maximumConnections=1000&amp;wireFormat.maxFrameSize=104857600"/>
     */
    private boolean sendJmsMessage(String url, String user, String password, String queueName, String chipWellBarcode) {
        Connection connection = null;
        Session session = null;
        try {

            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(url);

            connection = factory.createQueueConnection(user, password);
            connection.start();

            session = connection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(queueName);
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            Message message = session.createMessage();
            message.setStringProperty("chipWellBarcode", chipWellBarcode);
            producer.send(message);

            return true;
        } catch (JMSException jmse) {
            throw new RuntimeException(jmse.getMessage(), jmse.getLinkedException());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (JMSException e) {
                System.out.println("Failed to close session: " + e.getMessage() );
                System.out.println(e.getLinkedException().getMessage());
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (JMSException e) {
                System.out.println("Failed to close connection: " + e.getMessage() );
                System.out.println(e.getLinkedException().getMessage());
            }
        }
    }
}
