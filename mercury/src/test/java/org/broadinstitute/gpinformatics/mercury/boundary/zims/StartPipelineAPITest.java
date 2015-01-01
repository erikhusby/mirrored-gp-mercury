package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD;

@Test(groups = TestGroups.STANDARD)
public class StartPipelineAPITest extends Arquillian {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(PROD);
    }

    @RunAsClient
    @Test(enabled = false)
    public void test_start_server_for_kt() {
        try {
            System.out.println("Starting up");
            Thread.sleep(1000 * 60 * 60 * 96);
        }
        catch(InterruptedException e) {
            System.out.println("Shutting down");
        }
    }

    }
