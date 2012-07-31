package org.broadinstitute.sequel.boundary.lims;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.sequel.integration.DeploymentBuilder;
import org.broadinstitute.sequel.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
    public void testFetchLibraryDetailsByTubeBarcode(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchLibraryDetailsByTubeBarcode").queryParam("includeWorkRequestDetails", "true");

        String result1 = post(resource, "[\"0099443960\",\"406164\"]");
        assertThat(result1, notNullValue());

        String result2 = post(resource, "[\"0099443960\",\"406164\",\"unknown_barcode\"]");
        assertThat(result2, notNullValue());
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

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindFlowcellDesignationByTaskName(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "findFlowcellDesignationByTaskName").queryParam("taskName", "14A_03.19.2012");
        String result = get(resource);
        assertThat(result, notNullValue());
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindFlowcellDesignationByTaskNameInvalid(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "findFlowcellDesignationByTaskName").queryParam("taskName", "invalid_task");
        try {
            resource.accept(APPLICATION_JSON_TYPE).get(String.class);
            fail("Expected UniformInterfaceException not thrown");
        } catch (UniformInterfaceException e) {
            assertThat(e.getResponse().getStatus(), equalTo(204));
        }
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindFlowcellDesignationByFlowcellBarcode(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "findFlowcellDesignationByFlowcellBarcode").queryParam("flowcellBarcode", "C0GHCACXX");
        String result = get(resource);
        assertThat(result, notNullValue());
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindFlowcellDesignationByFlowcellBarcodeInvalid(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "findFlowcellDesignationByFlowcellBarcode").queryParam("taskName", "invalid_flowcell");
        try {
            resource.accept(APPLICATION_JSON_TYPE).get(String.class);
            fail("Expected UniformInterfaceException not thrown");
        } catch (UniformInterfaceException e) {
            assertThat(e.getResponse().getStatus(), equalTo(204));
        }
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchUserIdForBadgeId(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchUserIdForBadgeId").queryParam("badgeId", "8f03f000f7ff12e0");
        String result = get(resource);
        assertThat(result, equalTo("breilly"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchUserIdForBadgeIdNotFound(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchUserIdForBadgeId").queryParam("badgeId", "invalid_badge_id");
        UniformInterfaceException caught = getError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(caught.getResponse().getEntity(String.class), equalTo("fetchUserIdForBadgeId failed: unknown result"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQpcrForTube(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchQpcrForTube").queryParam("tubeBarcode", "0075414288");
        String result = get(resource);
        assertThat(result, equalTo("19.37698653"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQpcrForTubeNotFound(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchQpcrForTube").queryParam("tubeBarcode", "invalid_tube");
        try {
            resource.accept(APPLICATION_JSON_TYPE).get(String.class);
            fail("Expected UniformInterfaceException not thrown");
        } catch (UniformInterfaceException e) {
            assertThat(e.getResponse().getStatus(), equalTo(204));
        }
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQpcrForTubeNoQpcr(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchQpcrForTube").queryParam("tubeBarcode", "000001848862");
        try {
            resource.accept(APPLICATION_JSON_TYPE).get(String.class);
            fail("Expected UniformInterfaceException not thrown");
        } catch (UniformInterfaceException e) {
            assertThat(e.getResponse().getStatus(), equalTo(204));
        }
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQuantForTube(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchQuantForTube").queryParam("tubeBarcode", "0108462600").queryParam("quantType", "Catch Pico");
        String result = get(resource);
        assertThat(result, equalTo("5.33803"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQuantForTubeNotFound(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchQuantForTube").queryParam("tubeBarcode", "invalid_tube").queryParam("quantType", "Catch Pico");
        try {
            resource.accept(APPLICATION_JSON_TYPE).get(String.class);
            fail("Expected UniformInterfaceException not thrown");
        } catch (UniformInterfaceException e) {
            assertThat(e.getResponse().getStatus(), equalTo(204));
        }
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQuantForTubeUnknownQuant(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchQuantForTube").queryParam("tubeBarcode", "0108462600").queryParam("quantType", "Bogus Pico");
        try {
            resource.accept(APPLICATION_JSON_TYPE).get(String.class);
            fail("Expected UniformInterfaceException not thrown");
        } catch (UniformInterfaceException e) {
            assertThat(e.getResponse().getStatus(), equalTo(204));
        }
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQuantForTubeNoQuant(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchQuantForTube").queryParam("tubeBarcode", "000001859062").queryParam("quantType", "Catch Pico");
        try {
            resource.accept(APPLICATION_JSON_TYPE).get(String.class);
            fail("Expected UniformInterfaceException not thrown");
        } catch (UniformInterfaceException e) {
            assertThat(e.getResponse().getStatus(), equalTo(204));
        }
    }
}
