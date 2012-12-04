package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

/**
 * A Message Driven Bean to receive JMS messages from liquid handling decks
 */
@MessageDriven(name = "MessageMDBSample", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/sampleQueue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class BettalimsMessageBean implements MessageListener {

//    @Resource
//    private MessageDrivenContext messageDrivenContext;

    public BettalimsMessageBean() {
    }

    @Override
    public void onMessage(Message message) {
        if(message instanceof TextMessage) {

        } else {

        }
    }
}
