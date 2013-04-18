package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.jboss.weld.context.bound.BoundSessionContext;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

// todo jmt see if there's a way to reduce copy and paste between this and BettalimsMessageBean
// onMessage could call a method that takes the resource method as a callback.
// Can't use command pattern, because we're transmitting XML, not objects.

/**
 * JMS message driven bean for messages from BSP to record receipt of samples.
 * Some ActivationConfigProperties are set in ejb-jar.xml, to allow different values in different environments.
 */
@SuppressWarnings("UnusedDeclaration")
@MessageDriven(name = "SampleReceiptMessageBean", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        // Increase probability that messages are read in the order they were sent
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1")
//        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/broad.queue.mercury.bettalims.production"),
//        @ActivationConfigProperty(propertyName = "connectorClassName", propertyValue ="org.hornetq.core.remoting.impl.netty.NettyConnectorFactory"),
//        @ActivationConfigProperty(propertyName = "connectionParameters", propertyValue = "host=gpinfx-jms;port=5445")
})
public class SampleReceiptMessageBean implements MessageListener {

    private static final Log log = LogFactory.getLog(SampleReceiptMessageBean.class);

    @Inject
    private BoundSessionContext sessionContext;

    @Inject
    private BeanManager beanManager;

    @Inject
    private SampleReceiptResource sampleReceiptResource;

    @Inject
    private SessionContextUtility sessionContextUtility;

    /**
     * Transaction is NOT_SUPPORTED because we don't want re-delivery in case of failure.  We store all messages
     * on the file system, and email in case of failure, so the recipient of the email can resubmit messages manually.
     * @param message JMS message from BSP
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void onMessage(final Message message) {
        sessionContextUtility.executeInContext(new SessionContextUtility.Function() {
            @Override
            public void apply() {
                // The deck side code is written in JavaScript, so it sends text messages, rather than object messages.
                if (message instanceof TextMessage) {
                    //noinspection OverlyBroadCatchBlock
                    try {
                        String text = ((TextMessage) message).getText();
                        sampleReceiptResource.notifyOfReceipt(text);
                    } catch (Exception e) {
                        log.error("Failed to receive SampleReceipt message", e);
                        // todo jmt email LIMS oddities
                    }
                } else {
                    // todo jmt email LIMS oddities
                    //"Expected TextMessage, received " + message.getClass().getName()
                }
            }
        });
    }
}
