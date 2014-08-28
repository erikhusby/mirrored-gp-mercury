package org.broadinstitute.gpinformatics.mercury.boundary.transfervis;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Start a server that deploys the EJB
 */
@Test(groups = TestGroups.STANDARD)
public class TransferEntityGrapherTest extends Arquillian {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false, groups = TestGroups.STANDARD)
    public void testRun() {
        try {
            // Put the test to sleep, to keep the EJB deployed
            Thread.sleep(1000 * 60 * 10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
