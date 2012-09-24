package org.broadinstitute.gpinformatics.mercury.boundary.squid;


import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.mercury.control.pass.PassService;
import org.broadinstitute.gpinformatics.infrastructure.deployment.TestInstance;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

/**
 * External integration test to connect to Squid's PASS related webservices
 */
public class SquidSOAPTest extends ContainerTest {

    @Inject
    @TestInstance
    private SquidConfig squidConfig;


    @Inject
    @TestInstance
    private PassService passService;


    @Inject
    private Log log;


    private SquidTopicPortype getPMBridgeServicePort() throws Exception {
        String namespace = "urn:SquidTopic";
        QName serviceName = new QName(namespace, "SquidTopicService");

        String wsdlURL = squidConfig.getUrl() + "/services/SquidTopicService?WSDL";
        URL url = new URL(wsdlURL);

        Service service = Service.create(url, serviceName);
        return service.getPort(serviceName, SquidTopicPortype.class);

    }


    @Test(groups = {EXTERNAL_INTEGRATION})
    public void smokeTest() throws Exception {

        log.info("In the smokeTest!");

        final SquidTopicPortype pmBridgeServicePort = getPMBridgeServicePort();
        String ret = pmBridgeServicePort.getGreeting();

        log.info("ret is " + ret);

        Assert.assertEquals(ret, "Hello SquidTopic!");

        log.info("Leaving the smokeTest");

    }


    @Test(groups = {EXTERNAL_INTEGRATION})
    public void serviceTest() {

        // this is actually the one method we can't do since Mercury does not proxy this
        // Assert.assertEquals(passService.getGreeting(), "Hello SquidTopic!");

        Assert.assertNotNull(passService.searchPasses());
    }

}
