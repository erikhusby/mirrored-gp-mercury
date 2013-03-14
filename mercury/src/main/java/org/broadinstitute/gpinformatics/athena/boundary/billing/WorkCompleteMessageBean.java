package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.work.WorkCompleteMessage;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * A Message Driven Bean to receive JMS messages from liquid handling decks
 */
@SuppressWarnings("UnusedDeclaration")
@MessageDriven(name = "WorkCompleteMessageBean", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue") } )
public class WorkCompleteMessageBean implements MessageListener {

    private WorkCompleteMessageDao workCompleteMessageDao;

    @Inject
    public WorkCompleteMessageBean(WorkCompleteMessageDao workCompleteMessageDao) {
        this.workCompleteMessageDao = workCompleteMessageDao;
    }

    public WorkCompleteMessageBean() {
    }

    @Override
    public void onMessage(Message message) {
        try {
            // This pulls all the values out of the message.
            Map<String, Object> values = new HashMap<String, Object> ();

            Enumeration<?> mapNames = message.getPropertyNames();
            while (mapNames.hasMoreElements()) {
                String name  = (String) mapNames.nextElement();
                values.put(name, message.getObjectProperty(name));
            }

            String pdoName = message.getStringProperty(WorkCompleteMessage.Properties.PDO_NAME.name());
            String aliquotId = message.getStringProperty(WorkCompleteMessage.Properties.ALIQUOT_ID.name());
            long completedTime = message.getLongProperty(WorkCompleteMessage.Properties.COMPLETED_TIME.name());
            Date completedDate = new Date(completedTime);

            WorkCompleteMessage workComplete =
                    new WorkCompleteMessage(pdoName, aliquotId, completedDate, values);

            workCompleteMessageDao.persist(workComplete);
        } catch (JMSException jmse) {
            throw new RuntimeException("Got a jms exception processing work complete message", jmse);
        }
    }
}
