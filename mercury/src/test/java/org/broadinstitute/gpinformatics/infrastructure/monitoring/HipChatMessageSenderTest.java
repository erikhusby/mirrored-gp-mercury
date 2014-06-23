package org.broadinstitute.gpinformatics.infrastructure.monitoring;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STANDARD;

public class HipChatMessageSenderTest extends Arquillian {

    @Inject
    HipChatConfig hipChatConfig;

    @Inject HipChatMessageSender messageSender;

    @org.jboss.arquillian.container.test.api.Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(Deployment.DEV);
    }

    @Test(groups = STANDARD)
    public void test_send_message() {
        messageSender.postMessageToGpLims("test message from " + getClass().getCanonicalName());
    }
}
