package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.sapservices.SapIntegrationClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STANDARD;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Test(groups = STANDARD)
public class SapIntegrationClientTest extends Arquillian {

    @Inject
    SapIntegrationService sapIntegrationClient;

    @BeforeMethod
    public void setUp() {
        if (sapIntegrationClient == null) {
            return;
        }
    }

    @AfterMethod
    public void tearDown() {
        if (sapIntegrationClient == null) {
            return;
        }
    }

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void testConnection() throws IOException {
        String age = "42";
        String result = sapIntegrationClient.submitAge(age);

        Assert.assertEquals(result, "What? Just "+age+" - Great !");
    }
}
