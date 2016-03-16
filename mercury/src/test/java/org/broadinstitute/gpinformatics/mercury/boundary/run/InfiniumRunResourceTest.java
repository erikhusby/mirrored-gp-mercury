package org.broadinstitute.gpinformatics.mercury.boundary.run;

import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;

import java.net.URL;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test Infinium Run web service.
 */
@Test(groups = TestGroups.STANDARD)
public class InfiniumRunResourceTest extends RestServiceContainerTest {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @RunAsClient
    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    public void testBasics(@ArquillianResource URL baseUrl) throws Exception {
        WebResource resource = makeWebResource(baseUrl, "query");

        InfiniumRunBean response = resource.queryParam("chipWellBarcode", "3999582166_R01C01").
                type(MediaType.APPLICATION_JSON_TYPE).
                accept(MediaType.APPLICATION_JSON_TYPE).
                get(InfiniumRunBean.class);
        Assert.assertEquals(response.getCollaboratorSampleId(), "TREDAP123");
        Assert.assertEquals(response.getParticipentId(), "PT-1RVAV");
        Assert.assertFalse(response.isProcessControl());

        // Test a control
        response = resource.queryParam("chipWellBarcode", "3999582166_R05C01").
                type(MediaType.APPLICATION_JSON_TYPE).
                accept(MediaType.APPLICATION_JSON_TYPE).
                get(InfiniumRunBean.class);
        Assert.assertEquals(response.getCollaboratorSampleId(), "NA12878");
        Assert.assertTrue(response.isProcessControl());
    }

    @Override
    protected String getResourcePath() {
        return "infiniumrun";
    }
}