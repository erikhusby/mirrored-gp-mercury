package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
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
    public void testAnalyze(@ArquillianResource URL baseUrl)
            throws Exception {
        WebTarget resource = makeWebResource(baseUrl, "analyze/sequencingRun/1");
        Response response = resource.request("text/html").get();
        Assert.assertEquals(response.getStatusInfo(), Response.Status.OK);
        String result = response.readEntity(String.class);
        assertTrue(result.contains("canEtl"));

        resource = makeWebResource(baseUrl, "analyze/event/136213");
        response = resource.request("text/html").get();
        Assert.assertEquals(response.getStatusInfo(), Response.Status.OK);
        result = response.readEntity(String.class);
        assertTrue(result.contains("canEtl"));
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testIncrementalAndBackup(@ArquillianResource URL baseUrl)
            throws Exception {

        // Tests incremental.
        WebTarget resource = makeWebResource(baseUrl, "incremental/20121120000000/20121120000001");
        Response response = resource.request("text/plain").put(null);
        Assert.assertEquals(response.getStatusInfo(), Response.Status.OK);

        // Tests incremental.
        resource = makeWebResource(baseUrl, "incremental/20121120000001/20121120000000");
        response = resource.request("text/plain").put(null);
        Assert.assertEquals(response.getStatusInfo(), Response.Status.INTERNAL_SERVER_ERROR);

        // Tests backfill.
        resource = makeWebResource(baseUrl,
                "backfill/org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent/136213/136213");
        response = resource.request("text/plain").put(null);
        Assert.assertEquals(response.getStatusInfo(), Response.Status.OK);

        // Tests invalid class.
        resource = makeWebResource(baseUrl,
                "backfill/org.broadinstitute.gpinformatics.shouldNotHaveThisClassName/136213/136213");
        response = resource.request("text/plain").put(null);
        Assert.assertEquals(response.getStatusInfo(), Response.Status.NOT_FOUND);

        // Tests invalid range.
        resource = makeWebResource(baseUrl,
                "backfill/org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent/2/1");
        response = resource.request("text/plain").put(null);
        Assert.assertEquals(response.getStatusInfo(), Response.Status.BAD_REQUEST);
        Assert.assertTrue(response.readEntity(String.class).startsWith("Invalid"));
    }
}
