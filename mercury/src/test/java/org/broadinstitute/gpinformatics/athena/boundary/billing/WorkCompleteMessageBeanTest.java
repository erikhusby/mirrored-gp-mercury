package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.testng.Assert;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.work.WorkCompleteMessage;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.transaction.UserTransaction;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class WorkCompleteMessageBeanTest {

    public static final String TEST_PDO_NAME = "PDO-xxx";
    public static final String TEST_SAMPLE_NAME = "SM-xxx";

    @Inject
    WorkCompleteMessageDao workCompleteMessageDao;

    @Inject
    ProductOrderSampleDao productOrderSampleDao;

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    BillingLedgerDao billingLedgerDao;

    @Inject
    UserTransaction utx;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, since we're not running in container.
        if (utx == null) {
            return;
        }

        utx.begin();

        try {
            WorkCompleteMessageBean workCompleteMessageBean = new WorkCompleteMessageBean(workCompleteMessageDao);
            workCompleteMessageBean.onMessage(createMessage());

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

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testOnMessage() throws Exception {
        List<WorkCompleteMessage> messages = workCompleteMessageDao.getNewMessages();
        Assert.assertTrue(!messages.isEmpty(), "Should be at least one message in new message queue");
        boolean found = false;
        for (WorkCompleteMessage message : messages) {
            if (message.getPdoName().equals(TEST_PDO_NAME)) {
                found = true;
                Assert.assertEquals(message.getSampleName(), TEST_SAMPLE_NAME);
                break;
            }
        }
        Assert.assertTrue(found, "Should find our message in message queue");
    }

    // FIXME: expand to test creating ledger entries from message
    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testOnMessageReadBack() throws Exception {
        AutomatedBiller automatedBiller = new AutomatedBiller(workCompleteMessageDao, productOrderSampleDao, productOrderDao, billingLedgerDao);
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

    public static Message createMessage() throws JMSException {
        // This test doesn't (yet) actually connect to the JMS queue.  The test hands the message directly
        // to the MDB handler method.  So the values used here are not important and may not be correct.
        HornetQConnectionFactory cf = HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF,
                new TransportConfiguration(NettyConnectorFactory.class.getName(),
                        new HashMap<String, Object>() {{
                            put(TransportConstants.PORT_PROP_NAME, 5445);
                            put(TransportConstants.HOST_PROP_NAME, "localhost");
                        }}
                ));
        Connection connection = cf.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Message testMessage = session.createMapMessage();
        testMessage.clearBody();
        testMessage.setStringProperty(WorkCompleteMessage.REQUIRED_NAMES.PDO_NAME.name(), TEST_PDO_NAME);
        testMessage.setStringProperty(WorkCompleteMessage.REQUIRED_NAMES.SAMPLE_NAME.name(), TEST_SAMPLE_NAME);
        testMessage.setIntProperty(WorkCompleteMessage.REQUIRED_NAMES.SAMPLE_INDEX.name(), 1);
        testMessage.setLongProperty(WorkCompleteMessage.REQUIRED_NAMES.COMPLETED_TIME.name(), new Date().getTime());
        try {
            connection.close();
        } catch (Exception e) {
            // Ignore exceptions on close. We're just using it to create a dummy message.
        }
        return testMessage;
    }
}
