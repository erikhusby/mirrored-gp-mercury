package org.broadinstitute.gpinformatics.mercury.boundary.run;

import com.sun.jersey.api.client.Client;
import org.broadinstitute.gpinformatics.mercury.integration.ContainerTest;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;

import static org.broadinstitute.gpinformatics.mercury.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Test run registration web service
 */
public class SolexaRunResourceTest extends ContainerTest {

    @Test(enabled = false, groups = EXTERNAL_INTEGRATION, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testCreateRun(@ArquillianResource URL baseUrl) {
        try {
            String response = Client.create().resource(baseUrl.toExternalForm() + "rest/solexarun")
                    .type(MediaType.APPLICATION_XML_TYPE)
                    .accept(MediaType.APPLICATION_XML)
                    .entity(new SolexaRunBean("Flowcell0706153127", "Run20120706", new Date(), "SL-HAL",
                            File.createTempFile("RunDir", ".txt").getAbsolutePath(), null)).post(String.class);
            System.out.println(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
