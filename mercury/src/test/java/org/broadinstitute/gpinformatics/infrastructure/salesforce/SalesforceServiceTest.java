package org.broadinstitute.gpinformatics.infrastructure.salesforce;

import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Test(groups = TestGroups.STANDARD)
public class SalesforceServiceTest extends Arquillian{

    @Inject
    private SalesforceService salesforceService;

    @Deployment
    public static WebArchive buildMercuryWar() {
        // need TEST here for now because there's no STUBBY version of ThriftConfig
        // see ThriftServiceProducer.produce()
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test(groups = TestGroups.STANDARD)
    public void testPushProducts() throws URISyntaxException, IOException {
        salesforceService.pushProduct(Product.EXOME_EXPRESS_V2_PART_NUMBER);
//        salesforceService.pushProduct(Product.SAMPLE_INITIATION_PART_NUMBER);
    }
}
