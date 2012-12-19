package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.work.WorkCompleteMessage;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.MapMessage;
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
@MessageDriven(name = "WorkReporting", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue") } )
public class WorkCompleteMessageBean implements MessageListener {

    @Inject
    private WorkCompleteMessageDao workCompleteMessageDao;

    public WorkCompleteMessageBean() {
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof MapMessage) {
                MapMessage mapMessage = (MapMessage) message;

                // This pulls all the values out of the message
                Map<String, Object> values = new HashMap<String, Object> ();

                Enumeration<?> mapNames = mapMessage.getMapNames();
                while (mapNames.hasMoreElements()) {
                    String name  = (String) mapNames.nextElement();
                    values.put(name, mapMessage.getObject(name));
                }

                String pdoName = mapMessage.getString(WorkCompleteMessage.REQUIRED_NAMES.PDO_NAME.name());
                String sampleName = mapMessage.getString(WorkCompleteMessage.REQUIRED_NAMES.SAMPLE_NAME.name());
                Long sampleIndex = mapMessage.getLong(WorkCompleteMessage.REQUIRED_NAMES.SAMPLE_INDEX.name());
                String aliquotLsid = mapMessage.getString(WorkCompleteMessage.REQUIRED_NAMES.ALIQUOT_LSID.name());
                long completedTime = mapMessage.getLong(WorkCompleteMessage.REQUIRED_NAMES.COMPLETED_TIME.name());
                Date completedDate = new Date(completedTime);

                WorkCompleteMessage workComplete =
                    new WorkCompleteMessage(pdoName, sampleName, sampleIndex, aliquotLsid, completedDate, values);

                workCompleteMessageDao.persist(workComplete);
            } else {
                throw new RuntimeException("Expected MapMessage, received " + message.getClass().getName());
            }
        } catch (JMSException jmse) {
            throw new RuntimeException("Got a jms exception processing work complete message", jmse);
        }
    }
}
