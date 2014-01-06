package org.broadinstitute.gpinformatics.athena.boundary.orders;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.broadinstitute.gpinformatics.mocks.ExceptionThrowingPDOSampleDao;
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

/**
 * Uses a DAO that throws exceptions to confirm the
 * exception handling of the pdoSampleBillingStatus web service.
 */
public class PdoSampleBillingExceptionHandlingTest extends RestServiceContainerTest {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(DEV,ExceptionThrowingPDOSampleDao.class);
    }

    @Override
    protected String getResourcePath() {
        return "productOrders";
    }


    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testThatAnExceptionThrownInTheWebServiceIsCaughtAndAddedToTheListOfErrors(@ArquillianResource URL baseUrl) throws Exception {
        List<PDOSamplePair> pdoSamplesList = new ArrayList<>();
        PDOSamplePair pdoSample1 = new PDOSamplePair("PDO-872", "SM-47KKU",null);
        pdoSamplesList.add(pdoSample1);

        PDOSamplePairs pdoSamplePairs = new PDOSamplePairs();
        pdoSamplePairs.setPdoSamplePairs(pdoSamplesList);

        PDOSamplePairs returnedPdoSamples = makeWebResource(baseUrl,"pdoSampleBillingStatus")
                .type(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .entity(pdoSamplePairs)
                .post(PDOSamplePairs.class);

        Assert.assertTrue(returnedPdoSamples.getPdoSamplePairs().isEmpty());
        Assert.assertEquals(returnedPdoSamples.getErrors().size(),1);
    }
}
