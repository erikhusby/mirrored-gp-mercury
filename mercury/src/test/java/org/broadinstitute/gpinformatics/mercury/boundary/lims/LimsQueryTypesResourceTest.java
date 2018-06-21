package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author breilly
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class LimsQueryTypesResourceTest extends RestServiceContainerTest {

    public LimsQueryTypesResourceTest(){}

    public final String FLOWCELL_DESIGNATION_JSON =
            "{\"lanes\":[" +
            "{\"laneName\":\"1\",\"libraryData\":[" +
            "{\"wasFound\":true,\"libraryName\":\"Library-100\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000100\",\"sampleDetails\":["
            +
            "{\"sampleName\":\"SM-5000\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\",\"lsid\":\"broad.mit.edu:bsp.prod.sample:5000\"},"
            +
            "{\"sampleName\":\"SM-5001\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\",\"lsid\":\"broad.mit.edu:bsp.prod.sample:5001\"}],"
            +
            "\"dateCreated\":\"2012-07-12T11:23:45.000-0400\",\"discarded\":false,\"destroyed\":false,\"regulatoryDesignation\":[]}," +
            "{\"wasFound\":true,\"libraryName\":\"Library-101\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000101\",\"sampleDetails\":["
            +
            "{\"sampleName\":\"SM-5002\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\",\"lsid\":\"broad.mit.edu:bsp.prod.sample:5002\"},"
            +
            "{\"sampleName\":\"SM-5003\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\",\"lsid\":\"broad.mit.edu:bsp.prod.sample:5003\"}],"
            +
            "\"dateCreated\":\"2012-07-12T11:23:45.000-0400\",\"discarded\":false,\"destroyed\":false,\"regulatoryDesignation\":[]}]," +
            "\"loadingConcentration\":1.2,\"derivedLibraryData\":[" +
            "{\"wasFound\":true,\"libraryName\":\"Library-102\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000102\",\"sampleDetails\":["
            +
            "{\"sampleName\":\"SM-5004\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\",\"lsid\":\"broad.mit.edu:bsp.prod.sample:5004\"},"
            +
            "{\"sampleName\":\"SM-5005\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\",\"lsid\":\"broad.mit.edu:bsp.prod.sample:5005\"}],"
            +
            "\"dateCreated\":\"2012-07-12T11:23:45.000-0400\",\"discarded\":false,\"destroyed\":false,\"regulatoryDesignation\":[]}]}," +
            "{\"laneName\":\"2\",\"libraryData\":[" +
            "{\"wasFound\":true,\"libraryName\":\"Library-103\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000103\",\"sampleDetails\":["
            +
            "{\"sampleName\":\"SM-5006\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\",\"lsid\":\"broad.mit.edu:bsp.prod.sample:5006\"},"
            +
            "{\"sampleName\":\"SM-5007\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\",\"lsid\":\"broad.mit.edu:bsp.prod.sample:5007\"}],"
            +
            "\"dateCreated\":\"2012-07-12T11:23:45.000-0400\",\"discarded\":false,\"destroyed\":false,\"regulatoryDesignation\":[]}," +
            "{\"wasFound\":true,\"libraryName\":\"Library-104\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000104\",\"sampleDetails\":["
            +
            "{\"sampleName\":\"SM-5008\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\",\"lsid\":\"broad.mit.edu:bsp.prod.sample:5008\"},"
            +
            "{\"sampleName\":\"SM-5009\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\",\"lsid\":\"broad.mit.edu:bsp.prod.sample:5009\"}],"
            +
            "\"dateCreated\":\"2012-07-12T11:23:45.000-0400\",\"discarded\":false,\"destroyed\":false,\"regulatoryDesignation\":[]}]," +
            "\"loadingConcentration\":1.2,\"derivedLibraryData\":[" +
            "{\"wasFound\":true,\"libraryName\":\"Library-105\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000105\",\"sampleDetails\":["
            +
            "{\"sampleName\":\"SM-5010\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\",\"lsid\":\"broad.mit.edu:bsp.prod.sample:5010\"},"
            +
            "{\"sampleName\":\"SM-5011\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\",\"lsid\":\"broad.mit.edu:bsp.prod.sample:5011\"}],"
            +
            "\"dateCreated\":\"2012-07-12T11:23:45.000-0400\",\"discarded\":false,\"destroyed\":false,\"regulatoryDesignation\":[]}]}]," +
            "\"designationName\":\"Test Designation\",\"readLength\":101,\"pairedEndRun\":true,\"indexedRun\":true,\"controlLane\":2,\"keepIntensityFiles\":false,\"indexingReadConfiguration\":\"SINGLE\"}";


    /**
     * Need to override this since {@link RestServiceContainerTest} does not subclass
     * {@link StubbyContainerTest} and otherwise wouldn't have a
     * {@link Deployment} method
     *
     * @return {@link WebArchive} configured with the {@link org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment} STUBBY
     * specified within.
     */
    @Deployment
    public static WebArchive buildMercuryWar() {
        return StubbyContainerTest.buildMercuryWar();
    }

    @Override
    protected String getResourcePath() {
        return "limsQueryTypes";
    }

    @Test(groups = TestGroups.STUBBY, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoBooleanAsJson(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "echoBoolean");

        String result1 = get(resource.queryParam("value", "false"));
        assertThat(result1, equalTo("false"));

        String result2 = get(resource.queryParam("value", "true"));
        assertThat(result2, equalTo("true"));
    }

    @Test(groups = TestGroups.STUBBY, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoDoubleAsJson(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "echoDouble");

        String result1 = get(resource.queryParam("value", "1.234"));
        assertThat(result1, equalTo("1.234"));

        String result2 = get(resource.queryParam("value", "1.0"));
        assertThat(result2, equalTo("1.0"));
    }

    @Test(groups = TestGroups.STUBBY, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoStringAsJson(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "echoString");

        String result = get(resource.queryParam("value", "test"));
        assertThat(result, equalTo("test"));
    }

    @Test(groups = TestGroups.STUBBY, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoStringArrayAsJson(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "echoStringArray");

        List<String> values = Arrays.asList("value1", "value2");
        String result = get(addQueryParam(resource, "s", values));

        assertThat(result, equalTo(toJson(values)));
    }

    /**
     * Test that a list of 384 12-digit strings (i.e., tube barcodes) can be
     * sent to a service as query parameters. The alternative is falling back on
     * POST instead of GET even though it may be a query service call.
     *
     * @param baseUrl Base URL to use for calculating the URL.
     */
    @Test(groups = TestGroups.STUBBY, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoStringArrayLargeAsJson(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "echoStringArray");

        List<String> values = new ArrayList<>();
        for (int i = 0; i < 384; i++) {
            values.add(String.format("%012d", i));
        }
        String result = get(addQueryParam(resource, "s", values));

        assertThat(result, equalTo(toJson(values)));
    }

    @Test(groups = TestGroups.STUBBY, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoFlowcellDesignationAsJson(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "echoFlowcellDesignation");

        String result = post(resource, FLOWCELL_DESIGNATION_JSON);
        assertThat(result, equalTo(FLOWCELL_DESIGNATION_JSON));
    }

    @Test(groups = TestGroups.STUBBY, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoStringToBooleanMap(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "echoStringToBooleanMap");

        String request = "{\"result1\":false,\"result2\":true}";
        String result = post(resource, request);
        assertThat(result, equalTo(request));
    }

    @Test(groups = TestGroups.STUBBY, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoWellAndSourceTube(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "echoWellAndSourceTube");

        String request = "{\"wellName\":\"A01\",\"tubeBarcode\":\"tube_barcode1\"}";
        String result = post(resource, request);
        assertThat(result, equalTo(request));
    }

    @Test(groups = TestGroups.STUBBY, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoWellAndSourceTubeList(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "echoWellAndSourceTubeList");

        String request =
                "[{\"wellName\":\"A01\",\"tubeBarcode\":\"tube_barcode1\"},{\"wellName\":\"A02\",\"tubeBarcode\":\"tube_barcode2\"}]";
        String result = post(resource, request);
        assertThat(result, equalTo(request));
    }

    @Test(groups = TestGroups.STUBBY, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testThrowRuntimeException(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "throwRuntimeException");
        UniformInterfaceException caught = getWithError(resource.queryParam("message", "testThrowRuntimeException"));
        assertErrorResponse(caught, 500, "testThrowRuntimeException");
    }

    @Test(groups = TestGroups.STUBBY, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testThrowApplicationException(@ArquillianResource URL baseUrl)
            throws Exception {
        WebResource resource = makeWebResource(baseUrl, "throwApplicationException");
        UniformInterfaceException caught =
                getWithError(resource.queryParam("message", "testThrowApplicationException"));
        assertErrorResponse(caught, 500, "testThrowApplicationException");
    }
}
