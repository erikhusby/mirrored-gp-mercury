package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.work.WorkCompleteMessage;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.transaction.UserTransaction;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.AUTO_BUILD;

@Test(groups = TestGroups.STANDARD)
public class WorkCompleteMessageBeanTest extends Arquillian {

    // These objects are not persisted, so using non-unique names is OK here.
    public static final String TEST_PDO_NAME = "PDO-xxx";
    public static final String TEST_ALIQUOT_ID = "SM-xxx";

    @Inject
    AppConfig appConfig;

    @Inject
    WorkCompleteMessageDao workCompleteMessageDao;

    @Inject
    BillingEjb billingEjb;

    @Inject
    SessionContextUtility sessionContextUtility;

    @Inject
    UserTransaction utx;

    // Required to get the correct configuration for running JMS queue tests on the bamboo server.  In that case,
    // we can't use localhost.
    //
    // NOTE: To run locally, you must change this to DEV.  Make sure you change it back before checking in!
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(AUTO_BUILD);
    }

    @BeforeMethod(groups = TestGroups.STANDARD)
    public void setUp() throws Exception {
        // Skip if no injections, since we're not running in container.
        if (utx == null) {
            return;
        }

        utx.begin();
    }

    @AfterMethod(groups = TestGroups.STANDARD)
    public void tearDown() throws Exception {
        // Skip if no injections, since we're not running in container.
        if (utx == null) {
            return;
        }

        utx.rollback();
    }


    /**
     * Test sending a message to the JMS queue. We currently don't test to see if the message was received. This is
     * tricky to test since JMS messages are received and processed in a different thread, so we don't know when it's
     * OK to check and see if the messages are in the queue.
     * <p/>
     * This test is only checking to see if the queue is present on the server at the specified port and host name.
     */
    @Test(groups = TestGroups.STANDARD)
    public void testSendMessage() throws Exception {
        sendMessage();
    }

    /**
     * Test to make sure that messages are stored as DB objects.
     * <p/>
     * This test doesn't actually connect to the JMS queue.  The test hands the message directly
     * to the MDB handler method.
     */
    @Test(groups = TestGroups.STANDARD)
    public void testOnMessage() throws Exception {
        deliverMessage();
        List<WorkCompleteMessage> messages = workCompleteMessageDao.getNewMessages();
        Assert.assertTrue(!messages.isEmpty(), "Should be at least one message in new message queue");
        boolean found = false;
        for (WorkCompleteMessage message : messages) {
            if (message.getPdoName().equals(TEST_PDO_NAME)) {
                found = true;
                Assert.assertEquals(message.getAliquotId(), TEST_ALIQUOT_ID);
                break;
            }
        }
        Assert.assertTrue(found, "Should find our message in message queue");

        List<WorkCompleteMessage>
                foundMessages = workCompleteMessageDao.findByPDOAndAliquot(TEST_PDO_NAME, TEST_ALIQUOT_ID);
        Assert.assertEquals(foundMessages.size(), 1);
        WorkCompleteMessage foundMessage = foundMessages.get(0);
        Assert.assertEquals(foundMessage.getPdoName(), TEST_PDO_NAME);
        Assert.assertEquals(foundMessage.getAliquotId(), TEST_ALIQUOT_ID);
    }

    // TODO: expand to test creating ledger entries from message
    @Test(groups = TestGroups.STANDARD, enabled = false)
    public void testOnMessageReadBack() throws Exception {
        deliverMessage();
        AutomatedBiller automatedBiller =
                new AutomatedBiller(workCompleteMessageDao, billingEjb, sessionContextUtility);
        automatedBiller.processMessages();
        workCompleteMessageDao.flush();
        workCompleteMessageDao.clear();
        List<WorkCompleteMessage> messages = workCompleteMessageDao.getNewMessages();
        for (WorkCompleteMessage message : messages) {
            if (message.getPdoName().equals(TEST_PDO_NAME)) {
                Assert.fail("AutomatedBiller.processesMessages() failed to remove a message from the queue.");
            }
        }
    }

    public Session createSession() throws JMSException {
        Connection connection = getConnection();
        return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    private Connection getConnection() throws JMSException {
        HornetQConnectionFactory cf = HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF,
                new TransportConfiguration(NettyConnectorFactory.class.getName(),
                        new HashMap<String, Object>() {{
                            put(TransportConstants.PORT_PROP_NAME, appConfig.getJmsPort());
                            put(TransportConstants.HOST_PROP_NAME, appConfig.getHost());
                        }}
                ));

        cf.setClientFailureCheckPeriod(Long.MAX_VALUE);
        cf.setConnectionTTL(-1);
        // This connection is never closed, which is probably Bad but it doesn't seem to break anything.
        return cf.createConnection();
    }

    /**
     * Create a message and send it using the JMS API.  The message is created with flag so that if the JMS
     * listener reads it, it won't get written to the database.
     */
    public void sendMessage() throws JMSException {
        Session session = null;
        Connection connection = null;

        try {
            connection = getConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue("broad.queue.athena.workreporting.dev");
            MessageProducer producer = session.createProducer(destination);
            Message message = createMessage(session, false);
            producer.send(destination, message);
        } finally {
            if (session != null) {
                session.close();
            }

            if(connection != null) {
                connection.close();
            }
        }
    }

    /**
     * Create a message and deliver it by calling the JMS message handler directly.  This allows us to test the code
     * that creates a message entity, and the code that reads the created message entity, but doesn't cause the
     * entity to be persisted.
     */
    public void deliverMessage() throws JMSException {
        Session session = null;
        Connection connection = null;
        try {
            WorkCompleteMessageBean workCompleteMessageBean = new WorkCompleteMessageBean(workCompleteMessageDao, sessionContextUtility);
            connection = getConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            createMessage(session);
            workCompleteMessageDao.flush();
            workCompleteMessageDao.clear();
        } finally {
            if (session != null) {
                session.close();
            }

            if(connection != null) {
                connection.close();
            }
        }
    }

    public static Message createMessage(Session session, boolean persist) throws JMSException {
        Message message = session.createMapMessage();
        message.clearBody();
        message.setStringProperty(WorkCompleteMessage.Properties.PDO_NAME.name(), TEST_PDO_NAME);
        message.setStringProperty(WorkCompleteMessage.Properties.ALIQUOT_ID.name(), TEST_ALIQUOT_ID);
        message.setLongProperty(WorkCompleteMessage.Properties.COMPLETED_TIME.name(), new Date().getTime());
        if (!persist) {
            message.setBooleanProperty(WorkCompleteMessageBean.TEST_DATA_FLAG, true);
        }
        return message;
    }

    public static Message createMessage(Session session) throws JMSException {
        return createMessage(session, true);
    }
}
