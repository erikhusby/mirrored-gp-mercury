package org.broadinstitute.gpinformatics.mercury.test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.filter.LoggingFilter;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Tests Dried Blood Spot messaging, including persistence
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class DriedBloodSpotDbTest extends ContainerTest{

    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("MMddHHmmss");

    @Test(enabled = true, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEndToEnd(@ArquillianResource URL baseUrl) {
        String timestamp = timestampFormat.format(new Date());

        List<String> ftaPaperBarcodes = new ArrayList<String>();
        for(int i = 1; i <= 4; i++) {
            ftaPaperBarcodes.add("FTA" + i + "_" + timestamp);
        }
        DriedBloodSpotDbFreeTest.DriedBloodSpotJaxbBuilder driedBloodSpotJaxbBuilder =
                new DriedBloodSpotDbFreeTest.DriedBloodSpotJaxbBuilder(ftaPaperBarcodes, "BP-1234");
        driedBloodSpotJaxbBuilder.buildJaxb();

        Client client = Client.create();
        client.addFilter(new LoggingFilter(System.out));
        for (BettaLIMSMessage bettaLIMSMessage : driedBloodSpotJaxbBuilder.getMessageList()) {
            String response = client.resource(baseUrl.toExternalForm() + "rest/bettalimsmessage")
                    .type(MediaType.APPLICATION_XML_TYPE)
                    .accept(MediaType.APPLICATION_XML)
                    .entity(bettaLIMSMessage)
                    .post(String.class);
            System.out.println(response);
        }
    }
}
