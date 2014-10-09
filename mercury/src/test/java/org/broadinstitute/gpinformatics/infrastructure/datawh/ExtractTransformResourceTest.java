package org.broadinstitute.gpinformatics.infrastructure.datawh;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.aerogear.arquillian.test.smarturl.SchemeName;
import org.jboss.aerogear.arquillian.test.smarturl.UriScheme;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URL;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STANDARD;
import static org.testng.Assert.assertTrue;

@Test(groups = TestGroups.STANDARD)
public class ExtractTransformResourceTest extends RestServiceContainerTest {
    private String datafileDir = System.getProperty("java.io.tmpdir");

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Override
    protected String getResourcePath() {
        return "etl";
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @BeforeMethod
    public void beforeMethod() {
        // Deletes the etl .dat files.
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testAnalyze(@ArquillianResource @UriScheme(name = SchemeName.HTTPS, port = 8443) URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "analyze/sequencingRun/1");
        ClientResponse response = resource.type("text/html").get(ClientResponse.class);
        Assert.assertEquals(response.getClientResponseStatus(), ClientResponse.Status.OK);
        String result = response.getEntity(String.class);
        assertTrue(result.contains("canEtl"));

        resource = makeWebResource(baseUrl, "analyze/event/136213");
        response = resource.type("text/html").get(ClientResponse.class);
        Assert.assertEquals(response.getClientResponseStatus(), ClientResponse.Status.OK);
        result = response.getEntity(String.class);
        assertTrue(result.contains("canEtl"));
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testIncrementalAndBackup(
            @ArquillianResource @UriScheme(name = SchemeName.HTTPS, port = 8443) URL baseUrl) {

        // Tests incremental.
        WebResource resource = makeWebResource(baseUrl, "incremental/20121120000000/20121120000001");
        ClientResponse response = resource.type("text/plain").put(ClientResponse.class);
        Assert.assertEquals(response.getClientResponseStatus(), ClientResponse.Status.OK);

        // Tests incremental.
        resource = makeWebResource(baseUrl, "incremental/20121120000001/20121120000000");
        response = resource.type("text/plain").put(ClientResponse.class);
        Assert.assertEquals(response.getClientResponseStatus(), ClientResponse.Status.INTERNAL_SERVER_ERROR);

        // Tests backfill.
        resource = makeWebResource(baseUrl,
                "backfill/org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent/136213/136213");
        response = resource.type("text/plain").put(ClientResponse.class);
        Assert.assertEquals(response.getClientResponseStatus(), ClientResponse.Status.OK);

        // Tests invalid class.
        resource = makeWebResource(baseUrl,
                "backfill/org.broadinstitute.gpinformatics.shouldNotHaveThisClassName/136213/136213");
        response = resource.type("text/plain").put(ClientResponse.class);
        Assert.assertEquals(response.getClientResponseStatus(), ClientResponse.Status.NOT_FOUND);

        // Tests invalid range.
        resource = makeWebResource(baseUrl,
                "backfill/org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent/2/1");
        response = resource.type("text/plain").put(ClientResponse.class);
        Assert.assertEquals(response.getClientResponseStatus(), ClientResponse.Status.BAD_REQUEST);
        Assert.assertTrue(response.getEntity(String.class).startsWith("Invalid"));
    }
}
