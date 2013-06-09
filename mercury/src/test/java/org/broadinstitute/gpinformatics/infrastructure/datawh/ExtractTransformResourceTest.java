package org.broadinstitute.gpinformatics.infrastructure.datawh;

import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import java.net.URL;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

public class ExtractTransformResourceTest extends RestServiceContainerTest {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Override
    protected String getResourcePath() {
        return "etl";
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testAnalyzeEvent(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "analyze/event/1776");
        String result = resource.get(String.class);
        assert(result.length() > 50);
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testIncremental(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "incremental/20121120000000/20121121000000");
        resource.put();
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testBackfill(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl,
                "backfill/org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent/1/1");
        resource.put();
    }

}
