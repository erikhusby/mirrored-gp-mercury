package org.broadinstitute.gpinformatics.infrastructure.monitoring;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

public class HipChatMessageSenderTest extends Arquillian {

    @Inject
    HipChatConfig hipChatConfig;

    @Inject HipChatMessageSender messageSender;

    @org.jboss.arquillian.container.test.api.Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(Deployment.DEV);
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void test_send_message() {
        //hipChatConfig = HipChatConfigProducer.getConfig(Deployment.DEV);
        messageSender.postSimpleTextMessage("Testing hipchat message sender!","GPLIMS Testing");
    }
}
