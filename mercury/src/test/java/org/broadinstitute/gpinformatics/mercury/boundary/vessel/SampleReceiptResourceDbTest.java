package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import com.sun.jersey.api.client.Client;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Database test of receiving samples from BSP
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class SampleReceiptResourceDbTest extends ContainerTest {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMddHHmmss");

    @Test(enabled=true, groups=EXTERNAL_INTEGRATION, dataProvider= Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testReceiveTubes(@ArquillianResource URL baseUrl) {
        SampleReceiptBean sampleReceiptBean = SampleReceiptResourceTest.buildTubes(dateFormat.format(new Date()));
        String response= Client.create().resource(baseUrl.toExternalForm() + "rest/samplereceipt")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .entity(sampleReceiptBean)
                .post(String.class);
        System.out.println(response);
    }
}
