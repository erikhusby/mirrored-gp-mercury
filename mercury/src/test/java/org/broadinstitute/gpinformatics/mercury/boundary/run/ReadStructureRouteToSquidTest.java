package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * Test that confirms the default
 * routing of read structure data to squid
 */
public class ReadStructureRouteToSquidTest extends Arquillian {

    @Inject SolexaRunResource solexaRunResource;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST);
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testSquidReadStructureRouting() {
        ReadStructureRequest readStructureRequest = new ReadStructureRequest();
        // this run barcode should be present in various squid databases (dev, test, prod)
        readStructureRequest.setRunBarcode("A07KD111031");
        readStructureRequest.setLanesSequenced("2,5");
        solexaRunResource.storeRunReadStructure(readStructureRequest);
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testSquidReadStructureStorageFailure() {
        ReadStructureRequest readStructureRequest = new ReadStructureRequest();
        // this run barcode should be present in various squid databases (dev, test, prod)
        readStructureRequest.setRunBarcode("FunkyColdMedina");
        readStructureRequest.setLanesSequenced("2,5");

        try {
            solexaRunResource.storeRunReadStructure(readStructureRequest);
            Assert.fail("This should have thrown an exception");
        }
        catch(RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("is not registered in squid"));
        }
    }
}
