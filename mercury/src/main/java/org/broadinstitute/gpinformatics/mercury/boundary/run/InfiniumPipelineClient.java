package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * Forward finished Infinium chip wells to the pipeline for analysis.
 */
@Dependent
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

        // Including ActiveMQ 'failover' URI will retry on comma delimited list of servers (or same url if only one)
        String url = String.format(
                "failover://(ssl://%s:%d)", infiniumStarterConfig.getJmsHost(), infiniumStarterConfig.getJmsPort() );

        try {

            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(url);

            connection = factory.createQueueConnection(infiniumStarterConfig.getLogin(), infiniumStarterConfig.getPassword());
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(infiniumStarterConfig.getJmsQueue());
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            log.info("Sending arrays starter message for " + chipWellBarcode);

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
                log.warn( "Ignoring failure to close JMS session: " + e.getMessage(), e.getLinkedException() );
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (JMSException e) {
                log.warn( "Ignoring failure to close JMS connection: " + e.getMessage(), e.getLinkedException() );
            }
        }

    }

    /**
     * TESTING ONLY - Allows mock to be used
     */
    public void setInfiniumStarterConfig(
            InfiniumStarterConfig infiniumStarterConfig) {
        this.infiniumStarterConfig = infiniumStarterConfig;
    }
}
