package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

/**
 * Test that confirms the default
 * routing of read structure data to squid
 */
@Test(groups = TestGroups.ALTERNATIVES)
@Dependent
public class ReadStructureRouteToSquidTest extends Arquillian {

    public ReadStructureRouteToSquidTest(){}

    @Inject SolexaRunResource solexaRunResource;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST);
    }

    @Test(groups = TestGroups.ALTERNATIVES)
    public void testSquidReadStructureRouting() {
        ReadStructureRequest readStructureRequest = new ReadStructureRequest();
        // this run barcode should be present in various squid databases (dev, test, prod)
        readStructureRequest.setRunBarcode("A07KD111031");
        readStructureRequest.setLanesSequenced("2,5");
        solexaRunResource.storeRunReadStructure(readStructureRequest);
    }

    @Test(groups = TestGroups.ALTERNATIVES)
    public void testSquidReadStructureStorageFailure() {
        ReadStructureRequest readStructureRequest = new ReadStructureRequest();
        // this run barcode should be present in various squid databases (dev, test, prod)
        readStructureRequest.setRunBarcode("FunkyColdMedina");
        readStructureRequest.setLanesSequenced("2,5");

        Response readStructResponse = solexaRunResource.storeRunReadStructure(readStructureRequest);
        ReadStructureRequest result = (ReadStructureRequest) readStructResponse.getEntity();
        Assert.assertNotEquals(readStructResponse.getStatus(), Response.Status.OK);
//        Assert.assertTrue(result.getError().contains("is not registered in squid"));
    }
}
