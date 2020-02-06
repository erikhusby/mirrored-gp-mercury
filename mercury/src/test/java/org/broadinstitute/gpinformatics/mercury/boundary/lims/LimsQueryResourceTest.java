package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.LibraryDataType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SampleInfoType;
import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
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

    @Inject
    private LimsQueryResource limsQueryResource;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Override
    protected String getResourcePath() {
        return "limsQuery";
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchLibraryDetailsByTubeBarcode(@ArquillianResource URL baseUrl)
            throws Exception {
        WebTarget resource = makeWebResource(baseUrl, "fetchLibraryDetailsByTubeBarcode")
                .queryParam("includeWorkRequestDetails", "true");

        // The Squid tubes are technically in lims but inaccessible to LimsQuery.
        String result1 = get(addQueryParam(resource, "q", asList("0099443960", "406164", "0124675527")));
        assertThat(result1, notNullValue());
        int index = result1.indexOf("\"wasFound\":true");
        assertThat(index, equalTo(-1));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchLibraryDetailsObjectByTubeBarcodeMercury(@ArquillianResource URL baseUrl) throws MalformedURLException {
        WebTarget webResource = makeWebResource(baseUrl, "fetchLibraryDetailsByTubeBarcode")
                .queryParam("includeWorkRequestDetails", "true");
        webResource = webResource.queryParam("q", "0177174735");

        List<LibraryDataType> libraryDataTypes = webResource.queryParam("includeWorkRequestDetails", "true").
                request(APPLICATION_JSON_TYPE).get(new GenericType<List<LibraryDataType>>() {});
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
        WebTarget resource = makeWebResource(baseUrl, "doesLimsRecognizeAllTubes");

        // The Squid tubes are technically in lims but inaccessible to LimsQuery.
        String result1 = get(addQueryParam(resource, "q", asList("0099443960", "406164")));
        assertThat(result1, equalTo("false"));

        String result2 = get(addQueryParam(resource, "q", asList("unknown_barcode")));
        assertThat(result2, equalTo("false"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindImmediatePlateParents(@ArquillianResource URL baseUrl)
            throws Exception {
        WebTarget resource =
                makeWebResource(baseUrl, "findImmediatePlateParents").queryParam("plateBarcode", "000001383666");
        WebApplicationException caught = getWithError(resource);
        // The Squid tubes are technically in lims but inaccessible to LimsQuery.
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught),containsString("Plate not found"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchUserIdForBadgeId(@ArquillianResource URL baseUrl)
            throws Exception {

        assertThat(get(makeWebResource(baseUrl, "fetchUserIdForBadgeId").
                queryParam("badgeId", "bsptestuser_badge_id_1234")),
                equalTo("tester"));

        assertThat(get(makeWebResource(baseUrl, "fetchUserIdForBadgeId").queryParam("badgeId", "Test101010101")),
                equalTo("QADudeTest"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchUserIdForBadgeIdNotFound(@ArquillianResource URL baseUrl)
            throws Exception {
        WebTarget resource =
                makeWebResource(baseUrl, "fetchUserIdForBadgeId").queryParam("badgeId", "invalid_badge_id");
        WebApplicationException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("User not found for badge ID: invalid_badge_id"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchParentRackContentsForPlateNotFound(@ArquillianResource URL baseUrl)
            throws Exception {
        WebTarget resource =
                makeWebResource(baseUrl, "fetchParentRackContentsForPlate").queryParam("plateBarcode", "invalid_plate");
        WebApplicationException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("Plate not found for barcode: invalid_plate"));
    }

    /**
     * MERCURY tube - LimsQueries CDI Bean
     */
    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchMercuryQpcrForTube(@ArquillianResource URL baseUrl)
            throws Exception {
        WebTarget resource = makeWebResource(baseUrl, "fetchQpcrForTube")
                .queryParam("tubeBarcode", "0212942357")
                .queryParam("quantType", "VIIA QPCR");
        String result = get(resource);
        assertThat(result, equalTo("1.38"));

        resource = makeWebResource(baseUrl, "fetchQpcrForTube")
                .queryParam("tubeBarcode", "0212942357")
                .queryParam("quantType", "VIIA QPCR")
                .queryParam("onTubeOnly", "true");
        result = get(resource);
        assertThat(result, equalTo("1.38"));

        resource = makeWebResource(baseUrl, "fetchQpcrForTube")
                .queryParam("tubeBarcode", "0212942357")
                .queryParam("quantType", "Catch Pico");
        WebApplicationException exception = getWithError(resource);
        assertErrorResponse(exception, 500, "Tube or quant not found for barcode: 0212942357, quant type: Catch Pico");

        resource = makeWebResource(baseUrl, "fetchQpcrForTube")
                .queryParam("tubeBarcode", "1142063551")
                .queryParam("quantType", "VIIA QPCR");
        exception = getWithError(resource);
        assertErrorResponse(exception, 500, "Tube or quant not found for barcode: 1142063551, quant type: VIIA QPCR");
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQpcrForTubeNotFound(@ArquillianResource URL baseUrl)
            throws Exception {
        WebTarget resource = makeWebResource(baseUrl, "fetchQpcrForTube").queryParam("tubeBarcode", "invalid_tube");
        WebApplicationException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught), equalTo("Tube or quant not found for barcode: invalid_tube, quant type: null"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQuantForTubeNotFound(@ArquillianResource URL baseUrl)
            throws Exception {
        WebTarget resource = makeWebResource(baseUrl, "fetchQuantForTube").queryParam("tubeBarcode", "000001848862")
                .queryParam("quantType", "Catch Pico");
        WebApplicationException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught),
                equalTo("Tube or quant not found for barcode: 000001848862, quant type: Catch Pico"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchQuantForTubeNoQuant(@ArquillianResource URL baseUrl)
            throws Exception {
        WebTarget resource = makeWebResource(baseUrl, "fetchQuantForTube").queryParam("tubeBarcode", "000001859062")
                .queryParam("quantType", "Catch Pico");
        WebApplicationException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught),
                equalTo("Tube or quant not found for barcode: 000001859062, quant type: Catch Pico"));
    }

    /**
     * MERCURY tube - LimsQueries CDI Bean
     */
    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchMercuryQuantForTube(@ArquillianResource URL baseUrl)
            throws Exception {
        WebTarget resource = makeWebResource(baseUrl, "fetchQuantForTube")
                .queryParam("tubeBarcode", "0212942357")
                .queryParam("quantType", "VIIA QPCR");
        String result = get(resource);
        assertThat(result, equalTo("1.38"));

        resource = makeWebResource(baseUrl, "fetchQuantForTube")
                .queryParam("tubeBarcode", "0212942357")
                .queryParam("quantType", "VIIA QPCR")
                .queryParam("onTubeOnly", "true");
        result = get(resource);
        assertThat(result, equalTo("1.38"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchPoolGroups(@ArquillianResource URL baseUrl)
            throws Exception {
        WebTarget resource =
                makeWebResource(baseUrl, "fetchPoolGroups").queryParam("q", "0089526681").queryParam("q", "0089526682");
        String result = get(resource);
        // Squid tubes are inaccessible to LimsQuery.
        assertThat(result, equalTo("[]"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = false) //todo emp enable after ProductFixupText.gplim4159()
    @RunAsClient
    public void testFetchIlluminaSeqTemplateWithFlowCell(@ArquillianResource URL baseUrl) throws Exception {
        WebTarget resource = makeWebResource(baseUrl, "fetchIlluminaSeqTemplate").
                queryParam("id", "HF7MVBBXX").queryParam("idType", "FLOWCELL").queryParam("isPoolTest", "false");
        String result = get(resource);
        assertThat(result, containsString("\"barcode\":\"HF7MVBBXX\""));
        assertThat(result, containsString("\"pairedRun\":true"));
        assertThat(result, containsString("\"onRigWorkflow\":null"));
        assertThat(result, containsString("\"onRigChemistry\":null"));
        assertThat(result, containsString("\"concentration\":null"));
        assertThat(result, containsString("\"name\":\"Express Human WES (Deep Coverage) v1\""));
        assertThat(result, containsString("\"name\":\"NCP Human WES - Normal (150xMTC)\""));
        assertThat(result, containsString("\"name\":\"NCP Human WES - Tumor (150xMTC)\""));
        assertThat(result, containsString("\"name\":\"CP Human WES (85/50)\""));
        assertThat(result, containsString("\"regulatoryDesignation\":[\"RESEARCH_ONLY\"]"));
        assertThat(result, containsString("\"readStructure\":\"76T8B8B76T\""));
        String[] lanes = result.split("\"laneName\"");
        assertThat(lanes.length, CoreMatchers.equalTo(9));
        for (int i = 1; i < lanes.length; ++i) {
            assertThat(lanes[i], containsString("\"LANE"));
            assertThat(lanes[i], containsString("\"loadingVesselLabel\":\"11283899"));  // the last two digits vary
            assertThat(lanes[i], containsString("\"derivedVesselLabel\":\"0185941272\""));
            assertThat(lanes[i], containsString("\"loadingConcentration\":225"));
        }
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = false) //todo emp enable after ProductFixupText.gplim4159()
    @RunAsClient
    public void testFetchIlluminaSeqTemplateWithStripTube(@ArquillianResource URL baseUrl) throws Exception {
        WebTarget resource = makeWebResource(baseUrl, "fetchIlluminaSeqTemplate").
                queryParam("id", "000006113311").queryParam("idType", "TUBE").queryParam("isPoolTest", "false");
        String result = get(resource);
        // A denature tube query would happen before the flowcell is loaded, so expect null flowcell barcode
        // and loading vessels. But the setup read structure should be present along with the lane concentration.
        assertThat(result, containsString("\"barcode\":null"));
        assertThat(result, containsString("\"pairedRun\":true"));
        assertThat(result, containsString("\"readStructure\":\"76T8B8B76T\""));
        String[] lanes = result.split("\"laneName\"");
        assertThat(lanes.length, CoreMatchers.equalTo(9));
        for (int i = 1; i < lanes.length; ++i) {
            assertThat(lanes[i], containsString("\"LANE"));
            assertThat(lanes[i], containsString("\"loadingVesselLabel\":\"\""));
            assertThat(lanes[i], containsString("\"derivedVesselLabel\":\"0185942015\""));
            assertThat(lanes[i], containsString("\"loadingConcentration\":225"));
        }
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = false) //todo emp enable after ProductFixupText.gplim4159()
    @RunAsClient
    public void testFetchIlluminaSeqTemplatePoolTest(@ArquillianResource URL baseUrl) throws Exception {
        WebTarget resource = makeWebResource(baseUrl, "fetchIlluminaSeqTemplate").
                queryParam("id", "000006113311").queryParam("idType", "TUBE").queryParam("isPoolTest", "true");
        String result = get(resource);
        assertThat(result, containsString("\"barcode\":null"));
        assertThat(result, containsString("\"pairedRun\":true"));
        assertThat(result, containsString("\"readStructure\":\"8B8B\""));
        String[] lanes = result.split("\"laneName\"");
        assertThat(lanes.length, CoreMatchers.equalTo(9));
        for (int i = 1; i < lanes.length; ++i) {
            assertThat(lanes[i], containsString("\"LANE"));
            assertThat(lanes[i], containsString("\"loadingVesselLabel\":\"\""));
            assertThat(lanes[i], containsString("\"derivedVesselLabel\":\"0185942015\""));
            assertThat(lanes[i], containsString("\"loadingConcentration\":225"));
        }
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchIlluminaSeqTemplateBadEnum(@ArquillianResource URL baseUrl)
            throws Exception {
        WebTarget resource =
                makeWebResource(baseUrl, "fetchIlluminaSeqTemplate").queryParam("id", "0089526681")
                        .queryParam("idType",
                                "THISWILLFAIL").queryParam("isPoolTest", "true");

        WebApplicationException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught),
                containsString(
                        "Unable to extract parameter from http request: javax.ws.rs.QueryParam"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchConcentrationAndVolumeAndWeightForTubeBarcodes(@ArquillianResource URL baseUrl)
            throws Exception {
        WebTarget resource = makeWebResource(baseUrl, "fetchConcentrationAndVolumeAndWeightForTubeBarcodes");

        String result1 = get(addQueryParam(resource, "q", asList("1075671760", "1075671761")));
        assertThat(result1, notNullValue());
        int index = result1.indexOf("\"wasFound\":true");
        assertThat(index, not(equalTo(-1)));
        index = result1.indexOf("\"wasFound\":true", index + 1);
        assertThat(index, not(equalTo(-1)));
        index = result1.indexOf("\"wasFound\":true", index + 1);
        assertThat(index, equalTo(-1));

        String result2 = get(addQueryParam(resource, "q", asList("1075671760", "unknown_barcode")));
        assertThat(result2, notNullValue());
        index = result2.indexOf("\"wasFound\":true");
        assertThat(index, not(equalTo(-1)));
        index = result2.indexOf("\"wasFound\":true", index + 1);
        assertThat(index, equalTo(-1));

        index = result2.indexOf("\"wasFound\":false");
        assertThat(index, not(equalTo(-1)));
        index = result2.indexOf("\"wasFound\":false", index + 1);
        assertThat(index, equalTo(-1));

        resource =
                makeWebResource(baseUrl, "fetchConcentrationAndVolumeAndWeightForTubeBarcodes")
                        .queryParam("q", "1125628279")
                        .queryParam("labMetricsFirst","false");
        String result3 = get(resource);
        index = result3.indexOf("\"wasFound\":true");
        assertThat(index, not(equalTo(-1)));
        index = result3.indexOf("\"concentration\":3");
        assertThat(index, not(equalTo(-1)));

        // Based off most recent pico
        resource =
                makeWebResource(baseUrl, "fetchConcentrationAndVolumeAndWeightForTubeBarcodes")
                        .queryParam("q", "1125628279")
                        .queryParam("labMetricsFirst","true");
        String result4 = get(resource);
        index = result4.indexOf("\"wasFound\":true");
        assertThat(index, not(equalTo(-1)));
        index = result4.indexOf("\"concentration\":4.7");
        assertThat(index, not(equalTo(-1)));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFetchIlluminaSeqTemplateByDilutionTube(@ArquillianResource URL baseUrl)
            throws Exception {
        WebTarget resource =
                makeWebResource(baseUrl, "fetchIlluminaSeqTemplate").queryParam("id", "0115229204")
                        .queryParam("idType",
                                "TUBE").queryParam("isPoolTest", "false");
        String result = get(resource);
        assertThat(result, containsString("\"readStructure\":\"76T8B8B76T\""));
        assertThat(result, containsString("\"derivedVesselLabel\":\"AB56835527\""));
        assertThat(result, containsString("\"name\":\"Express Somatic Human WES (Deep Coverage) v1\""));
        assertThat(result, containsString("\"regulatoryDesignation\":[\"RESEARCH_ONLY\"]"));
        for (String varToTest :
                asList("barcode", "name", "onRigWorkflow", "onRigChemistry")) {
            assertThat(result, containsString(String.format("\"%s\":null,", varToTest)));
        }
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindAllReagentsListedInEventWithReagent(@ArquillianResource URL baseUrl)
            throws Exception {
        WebTarget resource = makeWebResource(baseUrl, "findAllReagentsListedInEventWithReagent")
                .queryParam("name", "TruSeq Rapid SBS Kit").queryParam("lot", "15L03A0047")
                .queryParam("expiration", "2016-06-28");
        String result = get(resource);
        assertThat(result, containsString("\"kitType\":\"Universal Sequencing Buffer 2\""));
        assertThat(result, containsString("\"kitType\":\"Universal Sequencing Buffer 1\""));
        assertThat(result, containsString("\"kitType\":\"Incorporation Master Mix\""));
        assertThat(result, containsString("\"kitType\":\"Cleavage Reagent Master Mix\""));
        assertThat(result, containsString("\"kitType\":\"Scan Reagent\","));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testValidateWorkflow(@ArquillianResource URL baseUrl)
            throws Exception {
        String pondRegistrationSourcePlate = "000009163073";
        WebTarget resource = makeWebResource(baseUrl, "validateWorkflow")
                .queryParam("nextEventTypeName", "IceCatchEnrichmentCleanup")
                .queryParam("q", pondRegistrationSourcePlate);
        String result = get(resource);
        assertThat(result, containsString("\"hasErrors\":true"));

        resource = makeWebResource(baseUrl, "validateWorkflow")
                .queryParam("nextEventTypeName", "PondRegistration")
                .queryParam("q", pondRegistrationSourcePlate);
        String result2 = get(resource);
        assertThat(result2, containsString("\"hasErrors\":false"));

        resource = makeWebResource(baseUrl, "validateWorkflow")
                .queryParam("nextEventTypeName", "PondRegistration")
                .queryParam("q", "IamAnUnknownBarcode");

        WebApplicationException caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught),
                startsWith(
                        "Failed to find lab vessels with barcodes: [IamAnUnknownBarcode]"));

        resource = makeWebResource(baseUrl, "validateWorkflow")
                .queryParam("nextEventTypeName", "PondRegistration")
                .queryParam("q", "000006893901");

        caught = getWithError(resource);
        assertThat(caught.getResponse().getStatus(), equalTo(500));
        assertThat(getResponseContent(caught),
                startsWith(
                        "Incompatible vessel types: [000006893901]"));
    }

    @Test
    public void testVerifyChipTypes() {
        /*
        Chip Type Infinium-MethylationEPIC
        */
        String ampPlateInf = "000017236009";
        String chipBarcodeInf = "203027390034";
        /*
        Chip Type Multi-EthnicGlobal
        */
        String ampPlateME = "000016899009";
        String chipBarcodeME = "200803750060";
        /*
        Chip type GSA
         */
        String ampPlateGSA = "000017296709";
        String chipBarcodeGSA = "202995720243";

        boolean resultPos = limsQueryResource.verifyChipTypes(ampPlateME, Collections.singletonList(chipBarcodeME));
        Assert.assertTrue(resultPos);

        resultPos = limsQueryResource.verifyChipTypes(ampPlateInf, Collections.singletonList(chipBarcodeInf));
        Assert.assertTrue(resultPos);

        resultPos = limsQueryResource.verifyChipTypes(ampPlateGSA, Collections.singletonList(chipBarcodeGSA));
        Assert.assertTrue(resultPos);

        boolean resultNeg = limsQueryResource.verifyChipTypes(ampPlateInf, Collections.singletonList(chipBarcodeME));
        Assert.assertFalse(resultNeg);

    }
}
