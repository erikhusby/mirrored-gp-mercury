package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.broadinstitute.gpinformatics.infrastructure.ws.WsMessageStore;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.Date;

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
    private WsMessageStore wsMessageStore;

    public BettalimsMessageBean() {
    }

    @Override
    public void onMessage(Message message) {
        // The deck side code is written in JavaScript, so it sends text messages, rather than object messages.
        if (message instanceof TextMessage) {
            try {
                String text = ((TextMessage) message).getText();
                wsMessageStore.store(text, new Date());
            } catch (Exception e) {
                // todo jmt email LIMS oddities
            }
        } else {
            throw new RuntimeException("Expected TextMessage, received " + message.getClass().getName());
        }
    }
}
