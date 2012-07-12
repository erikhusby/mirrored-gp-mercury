package org.broadinstitute.sequel.boundary.vessel;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
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

    private ClientConfig clientConfig;

    @BeforeMethod
    public void setUp() throws Exception {
        clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoBooleanAsJson(@ArquillianResource URL baseUrl) {
        String url = baseUrl + basePath + "/echoBoolean";

        String result1 = Client.create(clientConfig).resource(url).queryParam("value", "false").accept(MediaType.APPLICATION_JSON).get(String.class);
        assertThat(result1, equalTo(jsonForValue(false)));

        String result2 = Client.create(clientConfig).resource(url).queryParam("value", "true").accept(MediaType.APPLICATION_JSON).get(String.class);
        assertThat(result2, equalTo(jsonForValue(true)));
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoBooleanAsXml(@ArquillianResource URL baseUrl) {
        String url = baseUrl + basePath + "/echoBoolean";

        String result1 = Client.create(clientConfig).resource(url).queryParam("value", "false").accept(MediaType.APPLICATION_XML).get(String.class);
        assertThat(result1, equalTo(xmlForValue(false)));

        String result2 = Client.create(clientConfig).resource(url).queryParam("value", "true").accept(MediaType.APPLICATION_XML).get(String.class);
        assertThat(result2, equalTo(xmlForValue(true)));
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoDoubleAsJson(@ArquillianResource URL baseUrl) {
        String url = baseUrl + basePath + "/echoDouble";

        String result = Client.create(clientConfig).resource(url).queryParam("value", "1.234").accept(MediaType.APPLICATION_JSON).get(String.class);
        assertThat(result, equalTo(jsonForValue(1.234)));

        String result2 = Client.create(clientConfig).resource(url).queryParam("value", "1.0").accept(MediaType.APPLICATION_JSON).get(String.class);
        assertThat(result2, equalTo(jsonForValue(1.0)));
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoDoubleAsXml(@ArquillianResource URL baseUrl) {
        String url = baseUrl + basePath + "/echoDouble";

        String result1 = Client.create(clientConfig).resource(url).queryParam("value", "1.234").accept(MediaType.APPLICATION_XML).get(String.class);
        assertThat(result1, equalTo(xmlForValue(1.234)));

        String result2 = Client.create(clientConfig).resource(url).queryParam("value", "1.0").accept(MediaType.APPLICATION_XML).get(String.class);
        assertThat(result2, equalTo(xmlForValue(1.0)));
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoStringAsJson(@ArquillianResource URL baseUrl) {
        String url = baseUrl + basePath + "/echoString";

        String result = Client.create(clientConfig).resource(url).queryParam("value", "test").accept(MediaType.APPLICATION_JSON).get(String.class);
        assertThat(result, equalTo(jsonForValue("test")));
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEchoStringAsXml(@ArquillianResource URL baseUrl) {
        String url = baseUrl + basePath + "/echoString";

        String result = Client.create(clientConfig).resource(url).queryParam("value", "test").accept(MediaType.APPLICATION_XML).get(String.class);
        assertThat(result, equalTo(xmlForValue("test")));
    }

    private String jsonForValue(boolean value) {
        return "{\"booleanValue\":" + value +",\"doubleValue\":null,\"stringValue\":null,\"flowcellDesignation\":null}";
    }

    private String jsonForValue(double value) {
        return "{\"booleanValue\":null,\"doubleValue\":" + value + ",\"stringValue\":null,\"flowcellDesignation\":null}";
    }

    private String jsonForValue(String value) {
        return "{\"booleanValue\":null,\"doubleValue\":null,\"stringValue\":\"" + value + "\",\"flowcellDesignation\":null}";
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
}
