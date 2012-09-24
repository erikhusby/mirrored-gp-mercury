package org.broadinstitute.gpinformatics.mercury.integration;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.net.URL;

/**
 * @author breilly
 */
public class RunEmbeddedSequel extends Arquillian {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return DeploymentBuilder.buildMercuryWar();
    }

    @ArquillianResource
    protected URL baseURL;

    //@Test
    public void run() throws InterruptedException {
        System.out.println("Embedded Mercury started\n" + baseURL + "index.xhtml");
        // run for a year!
        Thread.sleep(365 * 24 * 60 * 60 * 1000L);
    }
}
