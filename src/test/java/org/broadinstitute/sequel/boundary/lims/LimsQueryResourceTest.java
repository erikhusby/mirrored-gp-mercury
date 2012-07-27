package org.broadinstitute.sequel.boundary.lims;

import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.sequel.integration.DeploymentBuilder;
import org.broadinstitute.sequel.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import java.net.URL;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;
import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.TEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author breilly
 */
public class LimsQueryResourceTest extends RestServiceContainerTest {

    @Deployment
    public static WebArchive buildSequelWar() {
        // need TEST here for now because there's no STUBBY version of ThriftConfig
        // see ThriftServiceProducer.produce()
        return DeploymentBuilder.buildSequelWar(TEST);
    }

    @Override
    protected String getResourcePath() {
        return "limsQuery";
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
}
