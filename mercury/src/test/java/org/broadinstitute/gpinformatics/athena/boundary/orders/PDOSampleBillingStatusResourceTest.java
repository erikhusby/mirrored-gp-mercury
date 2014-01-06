package org.broadinstitute.gpinformatics.athena.boundary.orders;


import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

public class PDOSampleBillingStatusResourceTest extends RestServiceContainerTest {


    @Deployment
    public static WebArchive buildMercuryWar() {
       return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Override
    protected String getResourcePath() {
        return "productOrders";
    }


    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testPDOSampleBilling(@ArquillianResource URL baseUrl) throws Exception {
        List<PDOSamplePair> pdoSamplesList = new ArrayList<>();
        PDOSamplePair pdoSample1 = new PDOSamplePair("PDO-872", "SM-47KKU",null);
        PDOSamplePair pdoSample2 = new PDOSamplePair("PDO-1133","0113404606",null);
        PDOSamplePair pdoSample3 = new PDOSamplePair("PDO-ONE_BILLION","DooDoo",null);
        pdoSamplesList.add(pdoSample1);
        pdoSamplesList.add(pdoSample2);
        pdoSamplesList.add(pdoSample3);

        PDOSamplePairs pdoSamplePairs = new PDOSamplePairs();
        pdoSamplePairs.setPdoSamplePairs(pdoSamplesList);

        PDOSamplePairs returnedPdoSamples = makeWebResource(baseUrl,"pdoSampleBillingStatus")
                .type(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .entity(pdoSamplePairs)
                .post(PDOSamplePairs.class);

        boolean foundPdoSample1 = false;
        boolean foundPdoSample2 = false;
        boolean foundPdoSample3 = false;
        for (PDOSamplePair pdoSamplePair : returnedPdoSamples.getPdoSamplePairs()) {
            if (pdoSamplePair.getSampleName().equals(pdoSample1.getSampleName())) {
                foundPdoSample1 = true;
                Assert.assertTrue(pdoSamplePair.isHasPrimaryPriceItemBeenBilled());
            }
            if (pdoSamplePair.getSampleName().equals(pdoSample2.getSampleName())) {
                foundPdoSample2 = true;
                Assert.assertFalse(pdoSamplePair.isHasPrimaryPriceItemBeenBilled());
            }
            if (pdoSamplePair.getSampleName().equals(pdoSample3.getSampleName())) {
                foundPdoSample3 = true;
                Assert.assertNull(pdoSamplePair.isHasPrimaryPriceItemBeenBilled());
            }
        }

        Assert.assertTrue(foundPdoSample1);
        Assert.assertTrue(foundPdoSample2);
        Assert.assertTrue(foundPdoSample3);

        Assert.assertEquals(returnedPdoSamples.getErrors().size(),1);

    }

}
