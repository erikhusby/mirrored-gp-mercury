package org.broadinstitute.gpinformatics.mercury.boundary.pass;

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.mercury.boundary.SquidTopicPortype;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;


/**
 * <b>INTERNAL</b> integration test to connect to the PASS webservice in Mercury.  The test method calls the one
 * method in Mercury's PASS webservices that does not proxy the equivalent service in Squid.
 */
public class PassServiceImplTest extends ContainerTest {

    @Inject
    private Log log;


    @Inject
    private Deployment deployment;


    /**
     * 'Topic' was one of the many names PMBridge had in its past.  I've transitioned Mercury's URL to PMBridge,
     * but I'm keeping all the other stuff the same for now so PMBridge can try connecting to Mercury as if it were Squid
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
        Assert.assertEquals(ret, "Hello PMBridge!");

    }


    @Test
    public void deploymentTest() {
        Assert.assertEquals(deployment, Deployment.STUBBY, "For Arquillian tests deployment should be STUBBY!");
    }

}
