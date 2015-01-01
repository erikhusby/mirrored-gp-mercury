package org.broadinstitute.gpinformatics.infrastructure.monitoring;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class HipChatMessageSenderTest {
    HipChatConfig hipChatConfig=new HipChatConfig(Deployment.DEV);
    HipChatMessageSender messageSender=new HipChatMessageSender(hipChatConfig);

    @Test(groups = EXTERNAL_INTEGRATION)
    public void test_send_message() {
        messageSender.postMessageToGpLims("test message from " + getClass().getCanonicalName());
    }
}
