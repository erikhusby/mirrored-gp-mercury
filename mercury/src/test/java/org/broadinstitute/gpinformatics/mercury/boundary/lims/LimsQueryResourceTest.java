package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.Arrays;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author breilly
 */
public class LimsQueryResourceTest extends RestServiceContainerTest {

    @Deployment
    public static WebArchive buildMercuryWar() {
        // need TEST here for now because there's no STUBBY version of ThriftConfig
        // see ThriftServiceProducer.produce()
        return DeploymentBuilder.buildMercuryWar(TEST);
    }

    @Override
    protected String getResourcePath() {
        return "limsQuery";
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchLibraryDetailsByTubeBarcode(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchLibraryDetailsByTubeBarcode").queryParam("includeWorkRequestDetails", "true");

        String result1 = get(addQueryParam(resource, "q", Arrays.asList("0099443960", "406164")));
        assertThat(result1, notNullValue());
        int index = result1.indexOf("\"wasFound\":true");
        assertThat(index, not(equalTo(-1)));
        index = result1.indexOf("\"wasFound\":true", index+1);
        assertThat(index, not(equalTo(-1)));
        index = result1.indexOf("\"wasFound\":true", index+1);
        assertThat(index, equalTo(-1));

        String result2 = get(addQueryParam(resource, "q", Arrays.asList("0099443960", "unknown_barcode")));
        assertThat(result2, notNullValue());
        index = result2.indexOf("\"wasFound\":true");
        assertThat(index, not(equalTo(-1)));
        index = result2.indexOf("\"wasFound\":true", index+1);
        assertThat(index, equalTo(-1));

        index = result2.indexOf("\"wasFound\":false");
        assertThat(index, not(equalTo(-1)));
        index = result2.indexOf("\"wasFound\":false", index+1);
        assertThat(index, equalTo(-1));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testDoesLimsRecognizeAllTubes(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "doesLimsRecognizeAllTubes");

        String result1 = get(addQueryParam(resource, "q", Arrays.asList("0099443960", "406164")));
        assertThat(result1, equalTo("true"));

        String result2 = get(addQueryParam(resource, "q", Arrays.asList("0099443960", "unknown_barcode")));
        assertThat(result2, equalTo("false"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchMaterialTypesForTubeBarcodes(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchMaterialTypesForTubeBarcodes");

        String result1 = get(addQueryParam(resource, "q", Arrays.asList("0099443960", "406164")));
        assertThat(result1, equalTo("[\"454 Material-Diluted ssDNA Library\",\"454 Beads-Recovered Sequencing Beads\"]"));
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
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("Designation not found for task name: invalid_task"));
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
        WebResource resource = makeWebResource(baseUrl, "findFlowcellDesignationByFlowcellBarcode").queryParam("flowcellBarcode", "invalid_flowcell");
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("Designation not found for flowcell barcode: invalid_flowcell"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindFlowcellDesignationByReagentBlockBarcode(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "findFlowcellDesignationByReagentBlockBarcode").queryParam("reagentBlockBarcode", "MS0000252-50");
        String result = get(resource);
        assertThat(result, notNullValue());
        assertThat(result, containsString("9A_10.26.2011"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindImmediatePlateParents(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "findImmediatePlateParents").queryParam("plateBarcode", "000001383666");
        String result = get(resource);
        assertThat(result, containsString("\"000000010208\""));
        assertThat(result, containsString("\"000002458823\""));
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
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("User not found for badge ID: invalid_badge_id"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchParentRackContentsForPlate(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchParentRackContentsForPlate").queryParam("plateBarcode", "000003343552");
        String result = get(resource);
        assertThat(result, containsString("\"A01\":true"));
        assertThat(result, not(containsString("\"A01\":false")));
        assertThat(result, containsString("\"B01\":true"));
        assertThat(result, not(containsString("\"B01\":false")));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchParentRackContentsForPlateNotFound(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchParentRackContentsForPlate").queryParam("plateBarcode", "invalid_plate");
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("Plate not found for barcode: invalid_plate"));
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
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("Tube or QPCR not found for barcode: invalid_tube"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQpcrForTubeNoQpcr(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchQpcrForTube").queryParam("tubeBarcode", "000001848862");
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("Tube or QPCR not found for barcode: 000001848862"));
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
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("Tube or quant not found for barcode: invalid_tube, quant type: Catch Pico"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQuantForTubeUnknownQuant(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchQuantForTube").queryParam("tubeBarcode", "0108462600").queryParam("quantType", "Bogus Pico");
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("Tube or quant not found for barcode: 0108462600, quant type: Bogus Pico"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQuantForTubeNoQuant(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchQuantForTube").queryParam("tubeBarcode", "000001859062").queryParam("quantType", "Catch Pico");
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("Tube or quant not found for barcode: 000001859062, quant type: Catch Pico"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchUnfulfilledDesignations(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchUnfulfilledDesignations");
        String result = get(resource);
        // This is about all we can do because the result is going to change over time
        assertThat(result, notNullValue());
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindRelatedDesignationsForAnyTube(@ArquillianResource URL baseUrl) {
        Exception caught = null;
        WebResource resource = makeWebResource(baseUrl, "findRelatedDesignationsForAnyTube").queryParam("q", "0115399989");
        String result = null;
        try {
            result = get(resource);
        } catch (Exception e) {
            caught = e;
        }
        // TODO: this is tough to test because it only returns open designations, or no content if the isn't one
        assertThat(caught, instanceOf(UniformInterfaceException.class));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchSourceTubesForPlate(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchSourceTubesForPlate").queryParam("plateBarcode", "000009873173");
        String result = get(resource);
        // quick spot check of one of the (191) source tubes
        assertThat(result, containsString("0116240473"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchTransfersForPlate(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchTransfersForPlate").queryParam("plateBarcode", "000009873173").queryParam("depth", "2");
        String result = get(resource);
        assertThat(result, containsString("000009873173"));
        assertThat(result, containsString("000009891873"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchPoolGroups(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "fetchPoolGroups").queryParam("q", "0089526681").queryParam("q", "0089526682");
        String result = get(resource);
        assertThat(result, equalTo("[{\"name\":\"21490_pg\",\"tubeBarcodes\":[\"0089526682\",\"0089526681\"]}]"));
    }
}
