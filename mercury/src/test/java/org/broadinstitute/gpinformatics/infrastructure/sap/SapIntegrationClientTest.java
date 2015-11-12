package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STANDARD;


/**
 * TODO scottmat fill in javadoc!!!
 */
@Test(groups = STANDARD)
public class SapIntegrationClientTest extends Arquillian {

    @Inject
    private SapIntegrationClient sapIntegrationClient;

//    private final static Log log = LogFactory.getLog(SapIntegrationClientTest.class);

    @BeforeMethod
    public void setUp() {
//        sapIntegrationClient = SapIntegrationClientProducer.testInstance();
        if (sapIntegrationClient == null) {
//            log.info("integration client is null");
            return;
        }
//        log.info("integration client is NOT null");
    }

    @AfterMethod
    public void tearDown() {
        if (sapIntegrationClient == null) {
//            log.info("integration client is null");
            return;
        }
//        log.info("integration client is NOT null");
    }

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void testConnection() {
        String result = sapIntegrationClient.testConnection("42");

        Assert.assertEquals(result, "What? Just 42 - Great !");
    }

}
