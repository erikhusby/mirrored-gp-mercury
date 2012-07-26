package org.broadinstitute.sequel.boundary.lims;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.broadinstitute.sequel.integration.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URL;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;
import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.TEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.testng.Assert.fail;

/**
 * @author breilly
 */
public class LimsQueryResourceTest extends ContainerTest {

    private static final String BASE_PATH = "rest/limsQuery/";

    private ClientConfig clientConfig;

    @Deployment
    public static WebArchive buildSequelWar() {
        // need TEST here for now because there's no STUBBY version of ThriftConfig
        // see ThriftServiceProducer.produce()
        return DeploymentBuilder.buildSequelWar(TEST);
    }

    @BeforeMethod(groups = EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindFlowcellDesignationByTaskName(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "findFlowcellDesignationByTaskName").queryParam("taskName", "14A_03.19.2012");
        String result = get(resource);
        assertThat(result, notNullValue());
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testDoesLimsRecognizeAllTubes(@ArquillianResource URL baseUrl) {
        WebResource webResource = makeWebResource(baseUrl, "doesLimsRecognizeAllTubes");

        String result1 = post(webResource, "[\"0099443960\",\"406164\"]");
        assertThat(result1, equalTo("true"));

        String result2 = post(webResource, "[\"0099443960\",\"406164\",\"unknown_barcode\"]");
        assertThat(result2, equalTo("false"));
    }

    private WebResource makeWebResource(URL baseUrl, String serviceUrl) {
        return Client.create(clientConfig).resource(baseUrl + BASE_PATH + serviceUrl);
    }

    private String get(WebResource resource) {
        try {
            return resource.accept(APPLICATION_JSON_TYPE).get(String.class);
        } catch (UniformInterfaceException e) {
            fail(e.getResponse().getEntity(String.class));
        }
        return null;
    }

    private String post(WebResource resource, String request) {
        try {
            return resource.type(APPLICATION_JSON_TYPE).accept(APPLICATION_JSON_TYPE).post(String.class, request);
        } catch (UniformInterfaceException e) {
            fail(e.getResponse().getEntity(String.class));
        }
        return null;
    }
}
