package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * Forward finished Infinium chip wells to the pipeline for analysis.
 */
public class InfiniumPipelineClient {

    private static final Log log = LogFactory.getLog(InfiniumPipelineClient.class);

    @Inject
    private InfiniumStarterConfig infiniumStarterConfig;

    public boolean callStarterOnWell(StaticPlate staticPlate, VesselPosition vesselPosition) {
        String chipWellBarcode = String.format("%s_%s", staticPlate.getLabel(), vesselPosition.name());
        return sendJmsMessage(chipWellBarcode);
    }

    private boolean sendJmsMessage(String chipWellBarcode) {
        Connection connection = null;
        Session session = null;
        try {
            String url = String.format(
                    "tcp://%s:%d", infiniumStarterConfig.getJmsHost(), infiniumStarterConfig.getJmsPort());
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
            connectionFactory.setUseAsyncSend(false);
            connection = connectionFactory.createQueueConnection();

            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(infiniumStarterConfig.getJmsQueue());
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            MapMessage message = session.createMapMessage();
            message.setString("chipWellBarcode", chipWellBarcode);

            producer.send(message);

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
                log.error("Exception occurred when closing JMS Session and connection", e);
            }
        }
    }
}
