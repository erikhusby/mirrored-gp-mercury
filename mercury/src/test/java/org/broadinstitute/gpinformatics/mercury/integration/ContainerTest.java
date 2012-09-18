package org.broadinstitute.gpinformatics.mercury.integration;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author breilly
 */
public class ContainerTest extends Arquillian {

    @Deployment
    public static WebArchive buildSequelWar() {
        return DeploymentBuilder.buildSequelWar();
    }
}
