package org.broadinstitute.gpinformatics.infrastructure.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterTest;

public class ContainerTest extends Arquillian {
    private static final Log log = LogFactory.getLog(ContainerTest.class);

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar();
    }

    @AfterTest
    public void tearDown() throws Exception {
        log.debug("Trying to force Clover to flush");
        ///CLOVER:FLUSH
    }
}
