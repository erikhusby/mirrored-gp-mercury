package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.work.WorkCompleteMessage;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfig;
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

public class WorkCompleteMessageBeanTest extends Arquillian {

    public static final String TEST_PDO_NAME = "PDO-xxx";
    public static final String TEST_ALIQUOT_ID = "SM-xxx";

    @Inject
    MercuryConfig mercuryConfig;

    @Inject
    WorkCompleteMessageDao workCompleteMessageDao;

    @Inject
    ProductOrderSampleDao productOrderSampleDao;

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    LedgerEntryDao ledgerEntryDao;

    @Inject
    ProductOrderEjb productOrderEjb;

    @Inject
    UserTransaction utx;

    // Required to get the correct configuration for running JMS queue tests on the bamboo server.  In that case,
    // we can't use localhost.
    //
    // NOTE: To run locally, you must change this to DEV.  Make sure you change it to BAMBOO before checking in!
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.BAMBOO);
    }

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, since we're not running in container.
        if (utx == null) {
            return;
        }

        utx.begin();

        try {
            // We send the JMS message object directly to our message handler for the purposes of this test.
            WorkCompleteMessageBean workCompleteMessageBean = new WorkCompleteMessageBean(workCompleteMessageDao);
            workCompleteMessageBean.onMessage(createMessage(createSession()));
            workCompleteMessageDao.flush();
            workCompleteMessageDao.clear();

        } catch (Exception e) {
            // Make sure we rollback if this code fails, otherwise we can cause other tests to fail.
            utx.rollback();
            throw e;
        }
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, since we're not running in container.
        if (utx == null) {
            return;
        }

        utx.rollback();
    }


    /**
     * Test sending a message to the JMS queue. We currently don't try to test to see if the message was
     * received. This is tricky to test since JMS messages are received and processed in a different thread,
     * so we don't know when it's OK to check and see if the messages are in the queue.
     */
    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testSendMessage() throws Exception {
        sendMessage();
    }

    /**
     * Test to make sure that messages are stored as DB objects.
     */
    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testOnMessage() throws Exception {
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
    }

    // FIXME: expand to test creating ledger entries from message
    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testOnMessageReadBack() throws Exception {
        AutomatedBiller automatedBiller = new AutomatedBiller(workCompleteMessageDao, productOrderDao, productOrderEjb);
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
        // This test doesn't (yet) actually connect to the JMS queue.  The test hands the message directly
        // to the MDB handler method.  So the values used here are not important and may not be correct.
        HornetQConnectionFactory cf = HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF,
                new TransportConfiguration(NettyConnectorFactory.class.getName(),
                        new HashMap<String, Object>() {{
                            put(TransportConstants.PORT_PROP_NAME, mercuryConfig.getJmsPort());
                            put(TransportConstants.HOST_PROP_NAME, mercuryConfig.getHost());
                        }}
                ));
        // This connection is never closed, which may be Bad but it doesn't seem to break anything.
        Connection connection = cf.createConnection();
        return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    public void sendMessage() throws JMSException {
        Session session = createSession();
        Destination destination = session.createQueue("broad.queue.athena.workreporting.dev");
        MessageProducer producer = session.createProducer(destination);
        Message message = createMessage(session);
        producer.send(destination, message);
    }

    public static Message createMessage(Session session) throws JMSException {
        Message message = session.createMapMessage();
        message.clearBody();
        message.setStringProperty(WorkCompleteMessage.Properties.PDO_NAME.name(), TEST_PDO_NAME);
        message.setStringProperty(WorkCompleteMessage.Properties.ALIQUOT_ID.name(), TEST_ALIQUOT_ID);
        message.setLongProperty(WorkCompleteMessage.Properties.COMPLETED_TIME.name(), new Date().getTime());
        return message;
    }
}
