package org.broadinstitute.sequel.boundary.lims;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.sequel.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Test;

import java.net.URL;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author breilly
 */
public class LimsQueryTypesResourceTest extends RestServiceContainerTest {

    public final String FLOWCELL_DESIGNATION_JSON =
            "{\"lanes\":[" +
            "{\"laneName\":\"1\",\"libraryData\":[" +
            "{\"wasFound\":true,\"libraryName\":\"Library-100\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000100\",\"sampleDetails\":[" +
            "{\"sampleName\":\"SM-5000\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}," +
            "{\"sampleName\":\"SM-5001\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}]," +
            "\"dateCreated\":\"2012-07-12T11:23:45.000-0400\",\"discarded\":false,\"destroyed\":false}," +
            "{\"wasFound\":true,\"libraryName\":\"Library-101\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000101\",\"sampleDetails\":[" +
            "{\"sampleName\":\"SM-5002\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}," +
            "{\"sampleName\":\"SM-5003\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}]," +
            "\"dateCreated\":\"2012-07-12T11:23:45.000-0400\",\"discarded\":false,\"destroyed\":false}]," +
            "\"loadingConcentration\":1.2,\"derivedLibraryData\":[" +
            "{\"wasFound\":true,\"libraryName\":\"Library-102\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000102\",\"sampleDetails\":[" +
            "{\"sampleName\":\"SM-5004\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}," +
            "{\"sampleName\":\"SM-5005\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}]," +
            "\"dateCreated\":\"2012-07-12T11:23:45.000-0400\",\"discarded\":false,\"destroyed\":false}]}," +
            "{\"laneName\":\"2\",\"libraryData\":[" +
            "{\"wasFound\":true,\"libraryName\":\"Library-103\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000103\",\"sampleDetails\":[" +
            "{\"sampleName\":\"SM-5006\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}," +
            "{\"sampleName\":\"SM-5007\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}]," +
            "\"dateCreated\":\"2012-07-12T11:23:45.000-0400\",\"discarded\":false,\"destroyed\":false}," +
            "{\"wasFound\":true,\"libraryName\":\"Library-104\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000104\",\"sampleDetails\":[" +
            "{\"sampleName\":\"SM-5008\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}," +
            "{\"sampleName\":\"SM-5009\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}]," +
            "\"dateCreated\":\"2012-07-12T11:23:45.000-0400\",\"discarded\":false,\"destroyed\":false}]," +
            "\"loadingConcentration\":1.2,\"derivedLibraryData\":[" +
            "{\"wasFound\":true,\"libraryName\":\"Library-105\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000105\",\"sampleDetails\":[" +
            "{\"sampleName\":\"SM-5010\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}," +
            "{\"sampleName\":\"SM-5011\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}]," +
            "\"dateCreated\":\"2012-07-12T11:23:45.000-0400\",\"discarded\":false,\"destroyed\":false}]}]," +
            "\"designationName\":\"Test Designation\",\"readLength\":101,\"pairedEndRun\":true,\"indexedRun\":true,\"controlLane\":2,\"keepIntensityFiles\":false}";

    @Override
    protected String getResourcePath() {
        return "limsQueryTypes";
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoBooleanAsJson(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "echoBoolean");

        String result1 = get(resource.queryParam("value", "false"));
        assertThat(result1, equalTo("false"));

        String result2 = get(resource.queryParam("value", "true"));
        assertThat(result2, equalTo("true"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoDoubleAsJson(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "echoDouble");

        String result1 = get(resource.queryParam("value", "1.234"));
        assertThat(result1, equalTo("1.234"));

        String result2 = get(resource.queryParam("value", "1.0"));
        assertThat(result2, equalTo("1.0"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoStringAsJson(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "echoString");

        String result = get(resource.queryParam("value", "test"));
        assertThat(result, equalTo("test"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoFlowcellDesignationAsJson(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "echoFlowcellDesignation");

        String result = post(resource, FLOWCELL_DESIGNATION_JSON);
        assertThat(result, equalTo(FLOWCELL_DESIGNATION_JSON));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoStringToBooleanMap(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "echoStringToBooleanMap");

        String request = "{\"result1\":false,\"result2\":true}";
        String result = post(resource, request);
        assertThat(result, equalTo(request));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testThrowRuntimeException(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "throwRuntimeException");

        String error = null;
        try {
            resource.queryParam("message", "testThrowRuntimeException").accept(APPLICATION_JSON_TYPE).get(String.class);
        } catch (UniformInterfaceException e) {
            error = e.getResponse().getEntity(String.class);
        }
        assertThat(error, equalTo("testThrowRuntimeException"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testThrowTException(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "throwTException");

        String error = null;
        try {
            resource.queryParam("message", "testThrowTException").accept(APPLICATION_JSON_TYPE).get(String.class);
        } catch (UniformInterfaceException e) {
            error = e.getResponse().getEntity(String.class);
        }
        assertThat(error, equalTo("testThrowTException"));
    }

    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testThrowTZIMSException(@ArquillianResource URL baseUrl) {
        WebResource resource = makeWebResource(baseUrl, "throwTZIMSException");

        String error = null;
        try {
            resource.queryParam("details", "testThrowTZIMSException").accept(APPLICATION_JSON_TYPE).get(String.class);
        } catch (UniformInterfaceException e) {
            error = e.getResponse().getEntity(String.class);
        }
        assertThat(error, equalTo("testThrowTZIMSException"));
    }
}
