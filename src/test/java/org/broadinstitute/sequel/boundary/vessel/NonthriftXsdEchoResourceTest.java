package org.broadinstitute.sequel.boundary.vessel;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.broadinstitute.sequel.nonthrift.jaxb.FlowcellDesignationType;
import org.broadinstitute.sequel.nonthrift.jaxb.LaneType;
import org.broadinstitute.sequel.nonthrift.jaxb.LibraryDataType;
import org.broadinstitute.sequel.nonthrift.jaxb.SampleInfoType;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.net.URL;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author breilly
 */
public class NonthriftXsdEchoResourceTest extends ContainerTest {

    private static final String basePath = "rest/nonthrift";

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
    private FlowcellDesignationType flowcellDesignation;
    private StringBuilder flowcellDesignationJson = new StringBuilder();
    private StringBuilder flowcellDesignationXml = new StringBuilder();
    private int libraryNumber = 100;
    private int sampleNumber = 5000;

    private ClientConfig clientConfig;

    public NonthriftXsdEchoResourceTest() {
        flowcellDesignation = makeFlowcellDesignation();
    }

    @BeforeMethod
    public void setUp() throws Exception {
        clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoBooleanAsJson(@ArquillianResource URL baseUrl) {
        String url = baseUrl + basePath + "/echoBoolean";

        String result1 = Client.create(clientConfig).resource(url).queryParam("value", "false").accept(MediaType.APPLICATION_JSON).get(String.class);
        assertThat(result1, equalTo("false"));

        String result2 = Client.create(clientConfig).resource(url).queryParam("value", "true").accept(MediaType.APPLICATION_JSON).get(String.class);
        assertThat(result2, equalTo("true"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoDoubleAsJson(@ArquillianResource URL baseUrl) {
        String url = baseUrl + basePath + "/echoDouble";

        String result1 = Client.create(clientConfig).resource(url).queryParam("value", "1.234").accept(MediaType.APPLICATION_JSON).get(String.class);
        assertThat(result1, equalTo("1.234"));

        String result2 = Client.create(clientConfig).resource(url).queryParam("value", "1.0").accept(MediaType.APPLICATION_JSON).get(String.class);
        assertThat(result2, equalTo("1.0"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoStringAsJson(@ArquillianResource URL baseUrl) {
        String url = baseUrl + basePath + "/echoString";

        String result = Client.create(clientConfig).resource(url).queryParam("value", "test").accept(MediaType.APPLICATION_JSON).get(String.class);
        assertThat(result, equalTo("test"));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoFlowcellDesignationAsJson(@ArquillianResource URL baseUrl) {
        String url = baseUrl + basePath + "/echoFlowcellDesignation";

        String result = Client.create(clientConfig).resource(url).type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(String.class, FLOWCELL_DESIGNATION_JSON);
        assertThat(result, equalTo(FLOWCELL_DESIGNATION_JSON));
    }

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoStringToBooleanMap(@ArquillianResource URL baseUrl) {
        String url = baseUrl + basePath + "/echoStringToBooleanMap";

        String request = "{\"result1\":false,\"result2\":true}";
        String result = Client.create(clientConfig).resource(url).type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(String.class, request);
        assertThat(result, equalTo(request));
    }

    private String jsonForValue(boolean value) {
        return "{\"booleanValue\":" + value +",\"doubleValue\":null,\"stringValue\":null,\"booleanMap\":null,\"flowcellDesignation\":null}";
    }

    private String jsonForValue(double value) {
        return "{\"booleanValue\":null,\"doubleValue\":" + value + ",\"stringValue\":null,\"booleanMap\":null,\"flowcellDesignation\":null}";
    }

    private String jsonForValue(String value) {
        return "{\"booleanValue\":null,\"doubleValue\":null,\"stringValue\":\"" + value + "\",\"booleanMap\":null,\"flowcellDesignation\":null}";
    }

    private String xmlForValue(boolean value) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><response><booleanValue>" + value + "</booleanValue></response>";
    }

    private String xmlForValue(double value) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><response><doubleValue>" + value + "</doubleValue></response>";
    }

    private String xmlForValue(String value) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><response><stringValue>" + value + "</stringValue></response>";
    }

    private FlowcellDesignationType makeFlowcellDesignation() {
        FlowcellDesignationType designation = new FlowcellDesignationType();
        flowcellDesignationJson.append("{\"booleanValue\":null,\"doubleValue\":null,\"stringValue\":null,\"booleanMap\":null,\"flowcellDesignation\":{");
        flowcellDesignationXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><response><flowcellDesignation>");

        flowcellDesignationJson.append("\"lanes\":[");
        for (int i = 1; i <= 2; i++) {
            if (i > 1) {
                flowcellDesignationJson.append(",");
            }
            designation.getLanes().add(makeLane(i));
        }
        flowcellDesignationJson.append("],");

        designation.setDesignationName("Test Designation");
        flowcellDesignationJson.append("\"designationName\":\"Test Designation\",");
        flowcellDesignationXml.append("<designationName>Test Designation</designationName>");

        designation.setReadLength(101);
        flowcellDesignationJson.append("\"readLength\":101,");
        flowcellDesignationXml.append("<readLength>101</readLength>");

        designation.setPairedEndRun(true);
        flowcellDesignationJson.append("\"pairedEndRun\":true,");
        flowcellDesignationXml.append("<pairedEndRun>true</pairedEndRun>");

        designation.setIndexedRun(true);
        flowcellDesignationJson.append("\"indexedRun\":true,");
        flowcellDesignationXml.append("<indexedRun>true</indexedRun>");

        designation.setControlLane(2);
        flowcellDesignationJson.append("\"controlLane\":2,");
        flowcellDesignationXml.append("<controlLane>2</controlLane>");

        designation.setKeepIntensityFiles(false);
        flowcellDesignationJson.append("\"keepIntensityFiles\":false");
        flowcellDesignationXml.append("<keepIntensityFiles>false</keepIntensityFiles>");

        flowcellDesignationJson.append("}}");
        flowcellDesignationXml.append("</flowcellDesignation></response>");
        return designation;
    }

    private LaneType makeLane(int laneNum) {
        LaneType lane = new LaneType();
        lane.setLaneName(Integer.toString(laneNum));
        flowcellDesignationJson.append("{\"laneName\":\"" + laneNum + "\"");
        flowcellDesignationXml.append("<lanes><laneName>" + laneNum + "</laneName>");

        flowcellDesignationJson.append(",\"libraryData\":[");
        for (int i = 0; i < 2; i++) {
            if (i > 0) {
                flowcellDesignationJson.append(",");
            }
            lane.getLibraryData().add(makeLibraryData());
        }
        flowcellDesignationJson.append("],");

        lane.setLoadingConcentration(1.2);
        flowcellDesignationJson.append("\"loadingConcentration\":1.2,");
        flowcellDesignationXml.append("<loadingConcentration>1.2</loadingConcentration>");

        flowcellDesignationJson.append("\"derivedLibraryData\":[");
        lane.getDerivedLibraryData().add(makeLibraryData("derivedLibraryData"));
        flowcellDesignationJson.append("]");

        flowcellDesignationJson.append("}");
        flowcellDesignationXml.append("</lanes>");
        return lane;
    }

    private LibraryDataType makeLibraryData() {
        return makeLibraryData("libraryData");
    }

    private LibraryDataType makeLibraryData(String tag) {
        LibraryDataType libraryData = new LibraryDataType();
        flowcellDesignationJson.append("{");
        flowcellDesignationXml.append("<" + tag + ">");

        libraryData.setWasFound(true);
        flowcellDesignationJson.append("\"wasFound\":true,");
        flowcellDesignationXml.append("<wasFound>true</wasFound>");

        String libraryName = "Library-" + libraryNumber;
        libraryData.setLibraryName(libraryName);
        flowcellDesignationJson.append("\"libraryName\":\"" + libraryName + "\",");
        flowcellDesignationXml.append("<libraryName>" + libraryName + "</libraryName>");

        libraryData.setLibraryType("Test Library");
        flowcellDesignationJson.append("\"libraryType\":\"Test Library\",");
        flowcellDesignationXml.append("<libraryType>Test Library</libraryType>");

        String tubeBarcode = "00000" + libraryNumber;
        libraryData.setTubeBarcode(tubeBarcode);
        flowcellDesignationJson.append("\"tubeBarcode\":\"" + tubeBarcode + "\",");
        flowcellDesignationXml.append("<tubeBarcode>" + tubeBarcode + "</tubeBarcode>");

        flowcellDesignationJson.append("\"sampleDetails\":[");
        for (int i = 0; i < 2; i++) {
            if (i > 0) {
                flowcellDesignationJson.append(",");
            }
            libraryData.getSampleDetails().add(makeSampleInfo());
        }
        flowcellDesignationJson.append("],");

        libraryData.setDateCreated(XMLGregorianCalendarImpl.createDateTime(2012, 7, 12, 11, 23, 45));
        flowcellDesignationJson.append("\"dateCreated\":\"2012-07-12T11:23:45.000-0400\",");
        // TODO: fix date formatting in XML
        flowcellDesignationXml.append("<dateCreated>2012-07-12T11:23:45</dateCreated>");

        libraryData.setDiscarded(false);
        flowcellDesignationJson.append("\"discarded\":false,");
        flowcellDesignationXml.append("<discarded>false</discarded>");

        libraryData.setDestroyed(false);
        flowcellDesignationJson.append("\"destroyed\":false");
        flowcellDesignationXml.append("<destroyed>false</destroyed>");

        flowcellDesignationJson.append("}");
        flowcellDesignationXml.append("</" + tag + ">");
        libraryNumber++;
        return libraryData;
    }

    private SampleInfoType makeSampleInfo() {
        SampleInfoType sampleInfo = new SampleInfoType();
        flowcellDesignationJson.append("{");
        flowcellDesignationXml.append("<sampleDetails>");

        String sampleName = "SM-" + sampleNumber;
        sampleInfo.setSampleName(sampleName);
        flowcellDesignationJson.append("\"sampleName\":\"" + sampleName + "\",");
        flowcellDesignationXml.append("<sampleName>" + sampleName + "</sampleName>");

        sampleInfo.setSampleType("Test Sample");
        flowcellDesignationJson.append("\"sampleType\":\"Test Sample\",");
        flowcellDesignationXml.append("<sampleType>Test Sample</sampleType>");

        sampleInfo.setIndexLength(6);
        flowcellDesignationJson.append("\"indexLength\":6,");
        flowcellDesignationXml.append("<indexLength>6</indexLength>");

        sampleInfo.setIndexSequence("TACTAG");
        flowcellDesignationJson.append("\"indexSequence\":\"TACTAG\",");
        flowcellDesignationXml.append("<indexSequence>TACTAG</indexSequence>");

        sampleInfo.setReferenceSequence("Test Reference");
        flowcellDesignationJson.append("\"referenceSequence\":\"Test Reference\"");
        flowcellDesignationXml.append("<referenceSequence>Test Reference</referenceSequence>");

        flowcellDesignationJson.append("}");
        flowcellDesignationXml.append("</sampleDetails>");
        sampleNumber++;
        return sampleInfo;
    }
}
