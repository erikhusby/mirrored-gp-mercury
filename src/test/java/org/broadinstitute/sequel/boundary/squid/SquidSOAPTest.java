package org.broadinstitute.sequel.boundary.squid;


import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.infrastructure.deployment.TestInstance;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfiguration;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;

public class SquidSOAPTest extends ContainerTest {

    @Inject
    @TestInstance
    private SquidConfiguration squidConfiguration;


    @Inject
    private Log log;


    private SquidTopicPortype getPMBridgeServicePort() throws Exception {
        String namespace = "urn:SquidTopic";
        QName serviceName = new QName(namespace, "SquidTopicService");

        String wsdlURL = squidConfiguration.getBaseUrl() + "/services/SquidTopicService?WSDL";
        URL url = new URL(wsdlURL);

        Service service = Service.create(url, serviceName);
        return service.getPort(serviceName, SquidTopicPortype.class);

    }


    @Test
    public void smokeTest() throws Exception {

        log.info("In the smokeTest!");

        final SquidTopicPortype pmBridgeServicePort = getPMBridgeServicePort();
        String ret = pmBridgeServicePort.getGreeting();

        log.info("ret is " + ret);

        Assert.assertEquals(ret, "Hello SquidTopic!");

        log.info("Leaving the smokeTest");

    }

}
