package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
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
import java.util.Collections;

/**
 * A Message Driven Bean to receive JMS messages from liquid handling decks.
 * The destination property is overridden in ejb-jar.xml, to allow different values in different environments.
 */
@SuppressWarnings("UnusedDeclaration")
@MessageDriven(name = "BettaLimsMessageBean", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        // Increase probability that messages are read in the order they were sent
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1")
//        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/broad.queue.mercury.bettalims.production"),
//        @ActivationConfigProperty(propertyName = "connectorClassName", propertyValue ="org.hornetq.core.remoting.impl.netty.NettyConnectorFactory"),
//        @ActivationConfigProperty(propertyName = "connectionParameters", propertyValue = "host=gpinfx-jms;port=5445")
})
public class BettaLimsMessageBean implements MessageListener {

    @Inject
    private BoundSessionContext sessionContext;

    @Inject
    private BeanManager beanManager;

    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;

    @Inject
    private SessionContextUtility sessionContextUtility;

    @Inject
    private AppConfig appConfig;

    @Inject
    private EmailSender emailSender;

    public BettaLimsMessageBean() {
    }

    /**
     * Transaction is NOT_SUPPORTED because we don't want re-delivery in case of failure.  We store all messages
     * on the file system, and email in case of failure, so the recipient of the email can resubmit messages manually.
     *
     * @param message JMS message from deck
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
                        bettaLimsMessageResource.storeAndProcess(text);
                    } catch (Exception e) {
                        emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(), Collections.<String>emptyList(),
                                "[Mercury] Failed to process JMS message", e.getMessage(), false, true);
                    }
                } else {
                    // todo jmt email LIMS oddities
                    //"Expected TextMessage, received " + message.getClass().getName()
                }
            }
        });
    }
}
