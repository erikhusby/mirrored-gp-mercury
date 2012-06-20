package org.broadinstitute.sequel.boundary.squid;


import org.broadinstitute.sequel.infrastructure.squid.SquidConfiguration;
import org.broadinstitute.sequel.integration.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;


/**
 *
 * Integration test of a SequeL client consuming a Squid webservice.
 *
 */
public class SquidSOAPTest extends Arquillian {

    @Deployment
    public static WebArchive buildSequelWar() {
        return DeploymentBuilder.buildSequelWar();
    }

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
