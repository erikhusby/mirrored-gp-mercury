package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.LibraryDataType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SampleInfoType;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 * @author breilly
 */
@Test(groups = TestGroups.STANDARD)
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

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchLibraryDetailsByTubeBarcode(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "fetchLibraryDetailsByTubeBarcode")
                .queryParam("includeWorkRequestDetails", "true");

        String result1 = get(addQueryParam(resource, "q", Arrays.asList("0099443960", "406164")));
        assertThat(result1, notNullValue());
        int index = result1.indexOf("\"wasFound\":true");
        assertThat(index, not(equalTo(-1)));
        index = result1.indexOf("\"wasFound\":true", index + 1);
        assertThat(index, not(equalTo(-1)));
        index = result1.indexOf("\"wasFound\":true", index + 1);
        assertThat(index, equalTo(-1));

        String result2 = get(addQueryParam(resource, "q", Arrays.asList("0099443960", "unknown_barcode")));
        assertThat(result2, notNullValue());
        index = result2.indexOf("\"wasFound\":true");
        assertThat(index, not(equalTo(-1)));
        index = result2.indexOf("\"wasFound\":true", index + 1);
        assertThat(index, equalTo(-1));

        index = result2.indexOf("\"wasFound\":false");
        assertThat(index, not(equalTo(-1)));
        index = result2.indexOf("\"wasFound\":false", index + 1);
        assertThat(index, equalTo(-1));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchLibraryDetailsObjectByTubeBarcode(@ArquillianResource URL baseUrl) throws MalformedURLException {
        WebResource webResource = makeWebResource(baseUrl, "fetchLibraryDetailsByTubeBarcode")
                .queryParam("includeWorkRequestDetails", "true");
        webResource = webResource.queryParam("q", "0124675527");

        List<LibraryDataType> libraryDataTypes = webResource.queryParam("includeWorkRequestDetails", "true").
                accept(APPLICATION_JSON_TYPE).get(new GenericType<List<LibraryDataType>>() {});
        Assert.assertEquals(libraryDataTypes.size(), 1);
        List<SampleInfoType> sampleDetails = libraryDataTypes.get(0).getSampleDetails();
        Assert.assertEquals(sampleDetails.size(), 19);
        SampleInfoType sampleInfoType = sampleDetails.get(0);
        Assert.assertEquals(sampleInfoType.getSampleName(), "238468.0");
        Assert.assertEquals(sampleInfoType.getLsid(), "broadinstitute.org:bsp.prod.sample:3HR2S");
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchLibraryDetailsObjectByTubeBarcodeMercury(@ArquillianResource URL baseUrl) throws MalformedURLException {
        WebResource webResource = makeWebResource(baseUrl, "fetchLibraryDetailsByTubeBarcode")
                .queryParam("includeWorkRequestDetails", "true");
        webResource = webResource.queryParam("q", "0177174735");

        List<LibraryDataType> libraryDataTypes = webResource.queryParam("includeWorkRequestDetails", "true").
                accept(APPLICATION_JSON_TYPE).get(new GenericType<List<LibraryDataType>>() {});
        Assert.assertEquals(libraryDataTypes.size(), 1);
        List<SampleInfoType> sampleDetails = libraryDataTypes.get(0).getSampleDetails();
        Assert.assertEquals(sampleDetails.size(), 1);
        SampleInfoType sampleInfoType = sampleDetails.get(0);
        Assert.assertEquals(sampleInfoType.getSampleName(), "SM-7QK86");
        Assert.assertEquals(sampleInfoType.getIndexSequence(), "AATTGGCCCCTATGCC");
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testDoesLimsRecognizeAllTubes(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "doesLimsRecognizeAllTubes");

        String result1 = get(addQueryParam(resource, "q", Arrays.asList("0099443960", "406164")));
        assertThat(result1, equalTo("true"));

        String result2 = get(addQueryParam(resource, "q", Arrays.asList("0099443960", "unknown_barcode")));
        assertThat(result2, equalTo("false"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchMaterialTypesForTubeBarcodes(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "fetchMaterialTypesForTubeBarcodes");

        String result1 = get(addQueryParam(resource, "q", Arrays.asList("0099443960", "406164")));
        assertThat(result1,
                equalTo("[\"454 Material-Diluted ssDNA Library\",\"454 Beads-Recovered Sequencing Beads\"]"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindFlowcellDesignationByTaskName(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource =
                makeWebResource(baseUrl, "findFlowcellDesignationByTaskName").queryParam("taskName", "14A_03.19.2012");
        String result = get(resource);
        assertThat(result, notNullValue());
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindFlowcellDesignationByTaskNameInvalid(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource =
                makeWebResource(baseUrl, "findFlowcellDesignationByTaskName").queryParam("taskName", "invalid_task");
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("Designation not found for task name: invalid_task"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindFlowcellDesignationByFlowcellBarcode(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "findFlowcellDesignationByFlowcellBarcode")
                .queryParam("flowcellBarcode", "C0GHCACXX");
        String result = get(resource);
        assertThat(result, notNullValue());
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindFlowcellDesignationByFlowcellBarcodeInvalid(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "findFlowcellDesignationByFlowcellBarcode")
                .queryParam("flowcellBarcode", "invalid_flowcell");
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("Designation not found for flowcell barcode: invalid_flowcell"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindFlowcellDesignationByReagentBlockBarcode(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "findFlowcellDesignationByReagentBlockBarcode")
                .queryParam("reagentBlockBarcode", "MS0000252-50");
        String result = get(resource);
        assertThat(result, notNullValue());
        assertThat(result, containsString("9A_10.26.2011"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindImmediatePlateParents(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource =
                makeWebResource(baseUrl, "findImmediatePlateParents").queryParam("plateBarcode", "000001383666");
        String result = get(resource);
        assertThat(result, containsString("\"000000010208\""));
        assertThat(result, containsString("\"000002458823\""));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchUserIdForBadgeId(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource =
                makeWebResource(baseUrl, "fetchUserIdForBadgeId").queryParam("badgeId", "8f03f000f7ff12e0");
        String result = get(resource);
        assertThat(result, equalTo("breilly"));

//        // TODO: after ~2/19/13 BSP release, use tester/bsptestuser_badge_id_1234 data below for a better integration test
////        WebResource resource = makeWebResource(baseUrl, "fetchUserIdForBadgeId").queryParam("badgeId", "bsptestuser_badge_id_1234");
//        WebResource resource = makeWebResource(baseUrl, "fetchUserIdForBadgeId").queryParam("badgeId", "Test101010101");
//        String result = get(resource);
////        assertThat(result, equalTo("tester"));
//        assertThat(result, equalTo("QADudeTest"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchUserIdForBadgeIdNotFound(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource =
                makeWebResource(baseUrl, "fetchUserIdForBadgeId").queryParam("badgeId", "invalid_badge_id");
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("User not found for badge ID: invalid_badge_id"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchParentRackContentsForPlate(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource =
                makeWebResource(baseUrl, "fetchParentRackContentsForPlate").queryParam("plateBarcode", "000003343552");
        String result = get(resource);
        assertThat(result, containsString("\"A01\":true"));
        assertThat(result, not(containsString("\"A01\":false")));
        assertThat(result, containsString("\"B01\":true"));
        assertThat(result, not(containsString("\"B01\":false")));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchParentRackContentsForPlateNotFound(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource =
                makeWebResource(baseUrl, "fetchParentRackContentsForPlate").queryParam("plateBarcode", "invalid_plate");
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("Plate not found for barcode: invalid_plate"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQpcrForTube(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "fetchQpcrForTube").queryParam("tubeBarcode", "0075414288");
        String result = get(resource);
        assertThat(result, equalTo("19.37698653"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void fetchQpcrForTubeAndType(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource =
                makeWebResource(baseUrl, "fetchQpcrForTubeAndType").queryParam("tubeBarcode", "1037346690").
                        queryParam("qpcrType", "Denatured Library");
        String result = get(resource);
        assertThat(result, equalTo("38.87261345"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQpcrForTubeNotFound(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "fetchQpcrForTube").queryParam("tubeBarcode", "invalid_tube");
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("Tube or QPCR not found for barcode: invalid_tube"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQpcrForTubeNoQpcr(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "fetchQpcrForTube").queryParam("tubeBarcode", "000001848862");
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("Tube or QPCR not found for barcode: 000001848862"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQuantForTube(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "fetchQuantForTube").queryParam("tubeBarcode", "0108462600")
                .queryParam("quantType", "Catch Pico");
        String result = get(resource);
        assertThat(result, equalTo("5.33803"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQuantForTubeNotFound(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "fetchQuantForTube").queryParam("tubeBarcode", "invalid_tube")
                .queryParam("quantType", "Catch Pico");
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught),
                equalTo("Tube or quant not found for barcode: invalid_tube, quant type: Catch Pico"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQuantForTubeUnknownQuant(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "fetchQuantForTube").queryParam("tubeBarcode", "0108462600")
                .queryParam("quantType", "Bogus Pico");
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught),
                equalTo("Tube or quant not found for barcode: 0108462600, quant type: Bogus Pico"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQuantForTubeNoQuant(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "fetchQuantForTube").queryParam("tubeBarcode", "000001859062")
                .queryParam("quantType", "Catch Pico");
        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught),
                equalTo("Tube or quant not found for barcode: 000001859062, quant type: Catch Pico"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchUnfulfilledDesignations(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "fetchUnfulfilledDesignations");
        String result = get(resource);
        // This is about all we can do because the result is going to change over time
        assertThat(result, notNullValue());
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchSourceTubesForPlate(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource =
                makeWebResource(baseUrl, "fetchSourceTubesForPlate").queryParam("plateBarcode", "000009873173");
        String result = get(resource);
        // quick spot check of one of the (191) source tubes
        assertThat(result, containsString("0116240473"));
    }

    /**
     * Test that fetchTransfersForPlate fetches and returns the correct data from Squid. Relies on some existing data in
     * the Squid database.
     */
    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchTransfersForPlateFromSquid(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource =
                makeWebResource(baseUrl, "fetchTransfersForPlate").queryParam("plateBarcode", "000009873173")
                        .queryParam("depth", "2");
        String result = get(resource);
        assertThat(result, containsString("000009873173"));
        assertThat(result, containsString("000009891873"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchPoolGroups(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource =
                makeWebResource(baseUrl, "fetchPoolGroups").queryParam("q", "0089526681").queryParam("q", "0089526682");
        String result = get(resource);
        assertThat(result, equalTo("[{\"name\":\"21490_pg\",\"tubeBarcodes\":[\"0089526682\",\"0089526681\"]}]"));
    }


    // todo: Re-enable when we have some test data
    @Test(enabled = false, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchIlluminaSeqTemplateWithFlowCell(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource =
                makeWebResource(baseUrl, "fetchIlluminaSeqTemplate").queryParam("id", "Flowcell0528112517")
                        .queryParam("idType",
                                "FLOWCELL").queryParam("isPoolTest", "true");
        String result = get(resource);
        assertThat(result, containsString("\"barcode\":\"Flowcell0528112517\""));
        assertThat(result, containsString("{\"laneName\":\"LANE1\""));
        assertThat(result, containsString("{\"laneName\":\"LANE2\""));
        for (String varToTest : Arrays
                .asList("name", "pairedRun", "onRigWorkflow", "onRigChemistry", "readStructure")) {
            assertThat(result, containsString(String.format("\"%s\":null,", varToTest)));
        }
    }

    // todo: Re-enable when we have some test data
    @Test(enabled = false, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchIlluminaSeqTemplateWithStripTube(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource =
                makeWebResource(baseUrl, "fetchIlluminaSeqTemplate").queryParam("id", "DenatureTube05131701450")
                        .queryParam("idType",
                                "STRIP_TUBE").queryParam("isPoolTest", "true");
        String result = get(resource);
        assertThat(result, containsString("{\"sequence\":\"CTACCAGG\",\"position\":\"P_7\"}"));
        assertThat(result, containsString("{\"laneName\":\"A01\""));
        assertThat(result, containsString("{\"laneName\":\"H12\""));
        for (String varToTest : Arrays
                .asList("barcode", "name", "pairedRun", "onRigWorkflow", "onRigChemistry", "readStructure")) {
            assertThat(result, containsString(String.format("\"%s\":null,", varToTest)));
        }
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchIlluminaSeqTemplateBadEnum(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource =
                makeWebResource(baseUrl, "fetchIlluminaSeqTemplate").queryParam("id", "0089526681")
                        .queryParam("idType",
                                "THISWILLFAIL").queryParam("isPoolTest", "true");

        UniformInterfaceException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught),
                startsWith(
                        "Unable to extract parameter from http request: javax.ws.rs.QueryParam(\"idType\") value is 'THISWILLFAIL'"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchConcentrationAndVolumeAndWeightForTubeBarcodes(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "fetchConcentrationAndVolumeAndWeightForTubeBarcodes");

        String result1 = get(addQueryParam(resource, "q", Arrays.asList("1075671760", "1075671761")));
        assertThat(result1, notNullValue());
        int index = result1.indexOf("\"wasFound\":true");
        assertThat(index, not(equalTo(-1)));
        index = result1.indexOf("\"wasFound\":true", index + 1);
        assertThat(index, not(equalTo(-1)));
        index = result1.indexOf("\"wasFound\":true", index + 1);
        assertThat(index, equalTo(-1));

        String result2 = get(addQueryParam(resource, "q", Arrays.asList("1075671760", "unknown_barcode")));
        assertThat(result2, notNullValue());
        index = result2.indexOf("\"wasFound\":true");
        assertThat(index, not(equalTo(-1)));
        index = result2.indexOf("\"wasFound\":true", index + 1);
        assertThat(index, equalTo(-1));

        index = result2.indexOf("\"wasFound\":false");
        assertThat(index, not(equalTo(-1)));
        index = result2.indexOf("\"wasFound\":false", index + 1);
        assertThat(index, equalTo(-1));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchIlluminaSeqTemplateByDilutionTube(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource =
                makeWebResource(baseUrl, "fetchIlluminaSeqTemplate").queryParam("id", "0115229204")
                        .queryParam("idType",
                                "TUBE").queryParam("isPoolTest", "false");
        String result = get(resource);
        assertThat(result, containsString("\"readStructure\":\"76T8B8B76T\""));
        assertThat(result, containsString("\"derivedVesselLabel\":\"AB56835527\""));
        assertThat(result, containsString("\"name\":\"Express Human WES (Deep Coverage) v1\""));
        assertThat(result, containsString("\"regulatoryDesignation\":[\"RESEARCH_ONLY\",\"RESEARCH_ONLY\"]"));
        for (String varToTest : Arrays
                .asList("barcode", "name", "onRigWorkflow", "onRigChemistry")) {
            assertThat(result, containsString(String.format("\"%s\":null,", varToTest)));
        }
    }
}
