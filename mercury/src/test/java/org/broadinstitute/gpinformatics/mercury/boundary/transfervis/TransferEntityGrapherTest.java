package org.broadinstitute.gpinformatics.mercury.boundary.transfervis;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.annotations.Test;

/**
 * Start a server that deploys the EJB
 */
public class TransferEntityGrapherTest extends ContainerTest {
    @Test(enabled = false)
    public void testRun() {
        try {
            // Put the test to sleep, to keep the EJB deployed
            Thread.sleep(1000 * 60 * 10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
