package org.broadinstitute.sequel.boundary.pass;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.boundary.SquidTopicPortype;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;


/**
 * Integration test to connect to the PASS webservice in SequeL.
 */
public class PassSOAPServiceTest extends ContainerTest {

    private static Log gLog = LogFactory.getLog(PassSOAPServiceTest.class);


    /**
     * 'Topic' was one of the many names PMBridge had in its past.  I've transitioned SequeL's URL to PMBridge,
     * but I'm keeping all the other stuff the same for now so PMBridge can try connecting to SequeL as if it were Squid
     * with just a configuration change to the URL.
     *
     * @return
     *
     * @throws Exception
     */
    private SquidTopicPortype getPMBridgeServicePort(URL baseURL) throws Exception {
        String namespace = "urn:SquidTopic";
        QName serviceName = new QName(namespace, "SquidTopicService");

        String wsdlURL = baseURL.toString() + "PASS?WSDL";
        URL url = new URL(wsdlURL);

        Service service = Service.create(url, serviceName);
        return service.getPort(serviceName, SquidTopicPortype.class);

    }


    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void smokeTest(@ArquillianResource URL baseURL) throws Exception{

        final SquidTopicPortype pmBridgeServicePort = getPMBridgeServicePort(baseURL);
        String ret = pmBridgeServicePort.getGreeting();
        Assert.assertEquals("Hello PMBridge!", ret);

    }
}
