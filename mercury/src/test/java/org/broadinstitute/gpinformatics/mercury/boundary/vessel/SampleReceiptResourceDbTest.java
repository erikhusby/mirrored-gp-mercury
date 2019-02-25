package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactoryTest;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Database test of receiving samples from BSP
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class SampleReceiptResourceDbTest extends StubbyContainerTest {

    public SampleReceiptResourceDbTest(){}

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMddHHmmss");

    @Test(enabled = true, groups = TestGroups.STUBBY, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testReceiveTubes(@ArquillianResource URL baseUrl) throws Exception {
        SampleReceiptBean sampleReceiptBean = LabVesselFactoryTest.buildTubes(dateFormat.format(new Date()));
        // POST to the resource

        ClientBuilder clientBuilder = JerseyUtils.getClientBuilderAcceptCertificate();

        WebTarget resource = clientBuilder.build()
                .target(RestServiceContainerTest.convertUrlToSecure(baseUrl) + "rest/samplereceipt");
        String response = resource.request(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .post(Entity.xml(sampleReceiptBean), String.class);

        // GET from the resource to verify persistence
        String batchName = response.substring(response.lastIndexOf(": ") + 2);
        SampleReceiptBean sampleReceiptBeanGet = resource.path(batchName).request().get(SampleReceiptBean.class);
        Assert.assertEquals(sampleReceiptBeanGet.getParentVesselBeans().size(),
                sampleReceiptBean.getParentVesselBeans().size(), "Wrong number of tubes");
    }

}
