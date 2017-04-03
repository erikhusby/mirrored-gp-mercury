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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        SearchRequestBean searchRequestBean = new SearchRequestBean("LabVessel", "LCSET Data", Collections.singletonList(
                new SearchValueBean("LCSET", Collections.singletonList("LCSET-8333"))));
        WebResource resource = makeWebResource(baseUrl, "run");
        SearchResponseBean response = resource.type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML_TYPE).entity(searchRequestBean)
                .post(SearchResponseBean.class);
        Assert.assertEquals(response.getSearchRowBeans().size(), 95);
    }

    @RunAsClient
    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    public void testEmerge(@ArquillianResource URL baseUrl) throws MalformedURLException {
        SearchRequestBean aliquotsSearchRequestBean = new SearchRequestBean("LabVessel", "eMERGE Aliquots Web Service",
                Collections.singletonList(new SearchValueBean("PDO", Collections.singletonList("PDO-8927"))));
        WebResource resource = makeWebResource(baseUrl, "run");
        SearchResponseBean aliquotsResponse = resource.type(MediaType.APPLICATION_XML_TYPE).
                accept(MediaType.APPLICATION_XML_TYPE).entity(aliquotsSearchRequestBean).
                post(SearchResponseBean.class);

        Assert.assertEquals(aliquotsResponse.getSearchRowBeans().size(), 94);
        Assert.assertEquals(aliquotsResponse.getHeaders().size(), 3);
        Assert.assertEquals(aliquotsResponse.getHeaders().get(0), "Root Sample ID");
        Assert.assertEquals(aliquotsResponse.getHeaders().get(1), "EmergeVolumeTransfer SM-ID");
        List<String> aliquotIds = new ArrayList<>();
        for (SearchRowBean searchRowBean : aliquotsResponse.getSearchRowBeans()) {
            aliquotIds.add(searchRowBean.getFields().get(1));
        }

        SearchRequestBean manifestSearchRequestBean = new SearchRequestBean("LabVessel", "eMERGE Manifest Web Service",
                Collections.singletonList(new SearchValueBean("Mercury Sample ID", aliquotIds)));
        SearchResponseBean manifestResponse = resource.type(MediaType.APPLICATION_XML_TYPE).
                accept(MediaType.APPLICATION_XML_TYPE).entity(manifestSearchRequestBean).
                post(SearchResponseBean.class);
        Assert.assertEquals(manifestResponse.getHeaders().size(), 10);
        // One was initially not transferred, because of bad molecular index well, but it was included in a later batch.
        Assert.assertEquals(manifestResponse.getSearchRowBeans().size(), 94);
    }

    @Override
    protected String getResourcePath() {
        return "search";
    }
}