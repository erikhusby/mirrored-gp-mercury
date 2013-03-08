package org.broadinstitute.gpinformatics.infrastructure.monitoring;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

public class HipChatMessageSenderTest extends ContainerTest {

    @Test(groups = EXTERNAL_INTEGRATION)
    public void test_send_message() {
        new HipChatMessageSender().postSimpleTextMessage("Testing hipchat message sender!","GPLIMS Testing");
    }
}
