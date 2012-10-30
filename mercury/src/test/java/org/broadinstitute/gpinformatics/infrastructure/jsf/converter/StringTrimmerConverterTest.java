package org.broadinstitute.gpinformatics.infrastructure.jsf.converter;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * @author breilly
 */
public class StringTrimmerConverterTest extends ContainerTest {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return DeploymentBuilder.buildMercuryWar();
    }

    @Drone
    private FirefoxDriver driver;
}
