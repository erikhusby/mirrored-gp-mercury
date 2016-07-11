package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.jms.client.HornetQConnectionFactory;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.util.HashMap;
import java.util.Map;

/**
 * Forward finished Infinium chip wells to the pipeline for analysis.
 */
public class InfiniumPipelineClient {

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
            Map<String, Object> connectionParams = new HashMap<>();
            connectionParams.put(TransportConstants.PORT_PROP_NAME, infiniumStarterConfig.getJmsPort());
            connectionParams.put(TransportConstants.HOST_PROP_NAME, infiniumStarterConfig.getJmsHost());
            TransportConfiguration transportConfiguration = new TransportConfiguration(
                    NettyConnectorFactory.class.getName(), connectionParams);
            HornetQConnectionFactory connectionFactory = HornetQJMSClient.createConnectionFactoryWithoutHA(
                    JMSFactoryType.CF, transportConfiguration);

            connectionFactory.setConnectionTTL(-1);
            connectionFactory.setClientFailureCheckPeriod(Long.MAX_VALUE);

            connection = connectionFactory.createConnection();
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
                // do nothing
            }
        }
    }
}
