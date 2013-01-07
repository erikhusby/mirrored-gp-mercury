package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.work.WorkCompleteMessage;
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
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import java.util.Date;
import java.util.HashMap;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class WorkCompleteMessageBeanTest extends Arquillian {

    @Inject
    WorkCompleteMessageDao workCompleteMessageDao;

    @Deployment
    public static WebArchive deployment() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testOnMessage() throws Exception {
        WorkCompleteMessageBean workCompleteMessageBean = new WorkCompleteMessageBean(workCompleteMessageDao);
        workCompleteMessageBean.onMessage(createMessage());
    }

    public static Message createMessage() throws JMSException {
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
        testMessage.setStringProperty(WorkCompleteMessage.REQUIRED_NAMES.PDO_NAME.name(), "PDO-123");
        testMessage.setStringProperty(WorkCompleteMessage.REQUIRED_NAMES.SAMPLE_NAME.name(), "SM-123");
        testMessage.setIntProperty(WorkCompleteMessage.REQUIRED_NAMES.SAMPLE_INDEX.name(), 1);
        testMessage.setLongProperty(WorkCompleteMessage.REQUIRED_NAMES.COMPLETED_TIME.name(), new Date().getTime());
        return testMessage;
    }
}
