package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

// todo jmt see if there's a way to reduce copy and paste between this and BettalimsMessageBean
// onMessage could call a method that takes the resource method as a callback.
// Can't use command pattern, because we're transmitting XML, not objects.
/**
 * JMS message driven bean for messages from BSP to record receipt of samples.
 * Some ActivationConfigProperties are set in ejb-jar.xml, to allow different values in different environments.
 */
@SuppressWarnings("UnusedDeclaration")
@MessageDriven(name = "SampleReceiptMessageBean", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue")
//        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/broad.queue.mercury.bettalims.production"),
//        @ActivationConfigProperty(propertyName = "connectorClassName", propertyValue ="org.hornetq.core.remoting.impl.netty.NettyConnectorFactory"),
//        @ActivationConfigProperty(propertyName = "connectionParameters", propertyValue = "host=gpinfx-jms;port=5445")
})
public class SampleReceiptMessageBean implements MessageListener {

    private static final Log LOG = LogFactory.getLog(SampleReceiptMessageBean.class);

    @Inject
    private BoundSessionContext sessionContext;

    @Inject
    private SampleReceiptResource sampleReceiptResource;

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
                    sampleReceiptResource.notifyOfReceipt(text);
                } catch (Exception e) {
                    LOG.error("Failed to receive SampleReceipt message", e);
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
