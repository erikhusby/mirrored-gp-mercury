package org.broadinstitute.gpinformatics.mercury.boundary.search;

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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test Configurable Search web service
 */
@Test(groups = TestGroups.STANDARD)
public class SearchResourceTest extends RestServiceContainerTest {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @RunAsClient
    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    public void testLcsetSearch(@ArquillianResource URL baseUrl) throws MalformedURLException {
        SearchRequest searchRequest = new SearchRequest("LabVessel", "LCSET Data", Collections.singletonList(
                new SearchValue("LCSET", Collections.singletonList("LCSET-8333"))));
        WebResource resource = makeWebResource(baseUrl, "run");
        SearchResponse response = resource.type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML_TYPE).entity(searchRequest)
                .post(SearchResponse.class);
        Assert.assertEquals(response.getSearchRows().size(), 95);
    }

    @Override
    protected String getResourcePath() {
        return "search";
    }
}