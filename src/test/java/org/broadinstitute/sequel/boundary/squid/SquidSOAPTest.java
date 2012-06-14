package org.broadinstitute.sequel.boundary.squid;


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
    private SquidConfiguration squidConfiguration;


    private SquidTopicPortype getPMBridgeServicePort() throws Exception {
        String namespace = "urn:SquidTopic";
        QName serviceName = new QName(namespace, "SquidTopicService");

        String wsdlURL = squidConfiguration.getBaseURL() + "services/SquidTopicService?WSDL";
        URL url = new URL(wsdlURL);

        Service service = Service.create(url, serviceName);
        return service.getPort(serviceName, SquidTopicPortype.class);

    }


    @Test
    public void smokeTest() throws Exception {

        final SquidTopicPortype pmBridgeServicePort = getPMBridgeServicePort();
        String ret = pmBridgeServicePort.getGreeting();
        Assert.assertEquals("Hello SquidTopic!", ret);

    }

}
