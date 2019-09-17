package org.broadinstitute.gpinformatics.athena.boundary.orders;


import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STANDARD;

@Test(groups = TestGroups.STANDARD)
public class PDOSampleBillingStatusResourceTest extends RestServiceContainerTest {

    private static final String PDO_SAMPLE_STATUS = "pdoSampleStatus";

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Override
    protected String getResourcePath() {
        return "productOrders";
    }


    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testPDOSampleBilling(@ArquillianResource URL baseUrl)
            throws Exception {
        List<PDOSample> pdoSamplesList = new ArrayList<>();
        Date receiptDate = new Date();
        PDOSample pdoSample1 = new PDOSample("PDO-872", "SM-47KKU", null, receiptDate);
        PDOSample pdoSample2 = new PDOSample("PDO-1133", "0113404606", null, receiptDate);
        PDOSample pdoSample3 = new PDOSample("PDO-ONE_BILLION", "DooDoo", null, receiptDate);
        pdoSamplesList.add(pdoSample1);
        pdoSamplesList.add(pdoSample2);
        pdoSamplesList.add(pdoSample3);

        PDOSamples pdoSamples = new PDOSamples();
        pdoSamples.setPdoSamples(pdoSamplesList);

        PDOSamples returnedPdoSamples = makeWebResource(baseUrl, PDO_SAMPLE_STATUS)
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.json(pdoSamples), PDOSamples.class);

        boolean foundPdoSample1 = false;
        boolean foundPdoSample2 = false;
        boolean foundPdoSample3 = false;
        for (PDOSample pdoSample : returnedPdoSamples.getPdoSamples()) {
            if (pdoSample.getSampleName().equals(pdoSample1.getSampleName())) {
                foundPdoSample1 = true;
                Assert.assertTrue(pdoSample.isHasPrimaryPriceItemBeenBilled());
            }
            if (pdoSample.getSampleName().equals(pdoSample2.getSampleName())) {
                foundPdoSample2 = true;
                Assert.assertFalse(pdoSample.isHasPrimaryPriceItemBeenBilled());
            }
            if (pdoSample.getSampleName().equals(pdoSample3.getSampleName())) {
                foundPdoSample3 = true;
                Assert.assertNull(pdoSample.isHasPrimaryPriceItemBeenBilled());
            }
        }

        Assert.assertTrue(foundPdoSample1);
        Assert.assertTrue(foundPdoSample2);
        Assert.assertTrue(foundPdoSample3);

        Assert.assertEquals(returnedPdoSamples.getErrors().size(), 1);

    }

}
