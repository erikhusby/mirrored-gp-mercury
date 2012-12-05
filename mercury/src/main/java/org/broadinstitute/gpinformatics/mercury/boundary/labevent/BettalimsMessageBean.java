package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

/**
 * A Message Driven Bean to receive JMS messages from liquid handling decks
 */
@MessageDriven(name = "BettalimsMessageBean", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/mercuryBettalims"),
        @ActivationConfigProperty(propertyName = "connectorClassName", propertyValue ="org.hornetq.core.remoting.impl.netty.NettyConnectorFactory"),
        @ActivationConfigProperty(propertyName = "connectionParameters", propertyValue = "host=vseqlims;port=5445")})
public class BettalimsMessageBean implements MessageListener {

//    @Resource
//    private MessageDrivenContext messageDrivenContext;

    @Inject
    private BettalimsMessageResource bettalimsMessageResource;

    public BettalimsMessageBean() {
    }

    @Override
    public void onMessage(Message message) {
        // The deck side code is written in JavaScript, so it sends text messages, rather than object messages.
        if (message instanceof TextMessage) {
            try {
                String text = ((TextMessage) message).getText();
                // todo jmt refactor called method to isolate stuff that's common to JAX-RS and JMS, and void ResourceException in JMS
                bettalimsMessageResource.processMessage(text);
            } catch (Exception e) {
                // todo jmt email LIMS oddities
            }
        } else {
            // todo jmt email LIMS oddities
            throw new RuntimeException("Expected TextMessage, received " + message.getClass().getName());
        }
    }
}
