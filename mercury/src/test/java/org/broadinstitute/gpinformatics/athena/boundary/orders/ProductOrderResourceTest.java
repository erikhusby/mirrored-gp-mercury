package org.broadinstitute.gpinformatics.athena.boundary.orders;

import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.AUTO_BUILD;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class ProductOrderResourceTest extends RestServiceContainerTest {
    @Deployment
    public static WebArchive buildMercuryWar() {
        // need TEST here for now because there's no STUBBY version of ThriftConfig
        // see ThriftServiceProducer.produce()
        return DeploymentBuilder.buildMercuryWar(AUTO_BUILD);
    }

    @Override
    protected String getResourcePath() {
        return "productOrders";
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = false)
    @RunAsClient
    public void testFetchLibraryDetailsByTubeBarcode(@ArquillianResource URL baseUrl) {
        Date testDate = new Date();

        ProductOrderData data = new ProductOrderData();
        data.setProductName("Scott Test Product"+testDate.getTime());
        data.setTitle("test product anme"+testDate.getTime());
        data.setQuoteId("MMMAC1");
        List<String> sampleIds = new ArrayList<>();
        Collections.addAll(sampleIds, "CSM-tsgm1"+testDate.getTime(), "CSM-tsgm2"+testDate.getTime());
        data.setSamples(sampleIds);

        WebResource resource = makeWebResource(baseUrl, "create");

        resource.post(data);
    }
}
