package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactoryTest;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.aerogear.arquillian.test.smarturl.SchemeName;
import org.jboss.aerogear.arquillian.test.smarturl.UriScheme;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Database test of receiving samples from BSP
 */
@Test(groups = TestGroups.STUBBY)
public class SampleReceiptResourceDbTest extends ContainerTest {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMddHHmmss");

    @Test(enabled = true, groups = TestGroups.STUBBY, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testReceiveTubes(@ArquillianResource @UriScheme(name = SchemeName.HTTPS, port = 8443) URL baseUrl) {
        SampleReceiptBean sampleReceiptBean = LabVesselFactoryTest.buildTubes(dateFormat.format(new Date()));
        // POST to the resource

        ClientConfig clientConfig = new DefaultClientConfig();
        RestServiceContainerTest.acceptAllServerCertificates(clientConfig);

        WebResource resource = Client.create(clientConfig).resource(baseUrl.toExternalForm() + "rest/samplereceipt");
        String response = resource.type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .entity(sampleReceiptBean)
                .post(String.class);

        // GET from the resource to verify persistence
        String batchName = response.substring(response.lastIndexOf(": ") + 2);
        SampleReceiptBean sampleReceiptBeanGet = resource.path(batchName).get(SampleReceiptBean.class);
        Assert.assertEquals(sampleReceiptBeanGet.getParentVesselBeans().size(),
                sampleReceiptBean.getParentVesselBeans().size(), "Wrong number of tubes");
    }
}
