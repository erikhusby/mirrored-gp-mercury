package org.broadinstitute.gpinformatics.infrastructure.datawh;

import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URL;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.testng.Assert.assertTrue;

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

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @BeforeMethod
    public void beforeMethod() {
        // Deletes the etl .dat files.
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testAnalyze(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "analyze/sequencingRun/1");
        String result = resource.get(String.class);
        assertTrue(result.contains("flowcellBarcode"));

        WebResource resource2 = makeWebResource(baseUrl, "analyze/event/136213");
        String result2 = resource2.get(String.class);
        assertTrue(result2.contains("canEtl"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testIncrementalAndBackup(@ArquillianResource URL baseUrl) {
        String endTimestamp = "20121121000000";

        // Tests that incremental produces a .dat file
        WebResource resource = makeWebResource(baseUrl, "incremental/20121120000000/" + endTimestamp);
        resource.put();

        boolean found = false;
        for (File file : EtlTestUtilities.getEtlFiles(datafileDir)) {
            Assert.assertFalse(file.getName().contains("event_fact"));
            if (file.getName().contains(endTimestamp)) {
                found = true;
            }
        }
        Assert.assertTrue(found);

        // Tests that backfill produces a .dat file
        WebResource resource2 = makeWebResource(baseUrl,
                "backfill/org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent/136213/136213");
        resource2.put();

        found = false;
        for (File file : EtlTestUtilities.getEtlFiles(datafileDir)) {
            if (file.getName().contains("event_fact")) {
                found = true;
            }
        }
        Assert.assertTrue(found);

    }
}
