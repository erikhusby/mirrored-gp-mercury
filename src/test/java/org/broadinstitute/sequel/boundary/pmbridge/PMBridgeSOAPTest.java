package org.broadinstitute.sequel.boundary.pmbridge;

import junit.framework.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.broadinstitute.sequel.pmbridge.SquidTopicPortype;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;


/**
 * Integration test to connect to the PMBridge webservice in SequeL.
 */
public class PMBridgeSOAPTest extends ContainerTest {

    private static Log gLog = LogFactory.getLog(PMBridgeSOAPTest.class);

    /**
     * This only appears to be injected properly if the @Test is also annotated with @RunAsClient
     */
    @ArquillianResource
    private URL deploymentURL;


    /**
     * 'Topic' was one of the many names PMBridge had in its past.  I've transitioned SequeL's URL to PMBridge,
     * but I'm keeping all the other stuff the same for now so PMBridge can try connecting to SequeL as if it were Squid
     * with just a configuration change to the URL.
     *
     * @return
     *
     * @throws Exception
     */
    private SquidTopicPortype getPMBridgeServicePort() throws Exception {
        String namespace = "urn:SquidTopic";
        QName serviceName = new QName(namespace, "SquidTopicService");

        String wsdlURL = deploymentURL.toString() + "PMBridge?WSDL";
        URL url = new URL(wsdlURL);

        Service service = Service.create(url, serviceName);
        return service.getPort(serviceName, SquidTopicPortype.class);

    }


    @Test
    @RunAsClient
    public void smokeTest() throws Exception{

        final SquidTopicPortype pmBridgeServicePort = getPMBridgeServicePort();
        Assert.assertEquals("Hello PMBridge!", pmBridgeServicePort.getGreeting());

    }
}
