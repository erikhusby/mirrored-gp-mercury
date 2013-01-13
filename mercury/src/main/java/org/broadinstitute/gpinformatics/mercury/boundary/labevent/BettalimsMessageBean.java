package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.jboss.weld.context.bound.BoundSessionContext;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.HashMap;

/**
* A Message Driven Bean to receive JMS messages from liquid handling decks.
* The destination property is overridden in ejb-jar.xml, to allow different values in different environments.
*/
@SuppressWarnings("UnusedDeclaration")
@MessageDriven(name = "BettalimsMessageBean", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        // Increase probability that messages are read in the order they were sent
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1")
//        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/broad.queue.mercury.bettalims.production"),
//        @ActivationConfigProperty(propertyName = "connectorClassName", propertyValue ="org.hornetq.core.remoting.impl.netty.NettyConnectorFactory"),
//        @ActivationConfigProperty(propertyName = "connectionParameters", propertyValue = "host=gpinfx-jms;port=5445")
})
public class BettalimsMessageBean implements MessageListener {

    @Inject
    private BoundSessionContext sessionContext;

    @Inject
    private BettalimsMessageResource bettalimsMessageResource;

    public BettalimsMessageBean() {
    }

    /**
     * Transaction is NOT_SUPPORTED because we don't want re-delivery in case of failure.  We store all messages
     * on the file system, and email in case of failure, so the recipient of the email can resubmit messages manually.
     * @param message JMS message from deck
     */
    @TransactionAttribute(value= TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void onMessage(Message message) {
        try {
            // Need to associate this JMS message with a session, to allow injection of Session scoped beans, e.g.
            // UserBean (if there is no session, the UserBean proxy will be injected, but calling any of its methods
            // causes an exception).
            sessionContext.associate(new HashMap<String, Object>());
            sessionContext.activate();
            // The deck side code is written in JavaScript, so it sends text messages, rather than object messages.
            if (message instanceof TextMessage) {
                //noinspection OverlyBroadCatchBlock
                try {
                    String text = ((TextMessage) message).getText();
                    bettalimsMessageResource.storeAndProcess(text);
                } catch (Exception e) {
                    // todo jmt email LIMS oddities
                }
            } else {
                // todo jmt email LIMS oddities
                //"Expected TextMessage, received " + message.getClass().getName()
            }
        } finally {
            sessionContext.invalidate();
            sessionContext.deactivate();
        }
    }
}
