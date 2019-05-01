package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.EntityLoggingFilter;
import org.broadinstitute.gpinformatics.mercury.control.JaxRsUtils;
import org.broadinstitute.gpinformatics.mercury.test.builders.DriedBloodSpotJaxbBuilder;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Tests Dried Blood Spot messaging, including persistence
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class DriedBloodSpotDbTest extends StubbyContainerTest {

    public DriedBloodSpotDbTest(){}

    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("MMddHHmmss");

    @Test(enabled = true, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEndToEnd(@ArquillianResource URL baseUrl) throws Exception {
        String timestamp = timestampFormat.format(new Date());

        List<String> ftaPaperBarcodes = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            ftaPaperBarcodes.add("FTA" + i + "_" + timestamp);
        }
        String batchId = "BP-" + timestamp;

        ClientBuilder clientBuilder = JaxRsUtils.getClientBuilderAcceptCertificate();

        Client client = clientBuilder.build();
        client.register(new EntityLoggingFilter());

        DriedBloodSpotJaxbBuilder driedBloodSpotJaxbBuilder =
                new DriedBloodSpotJaxbBuilder(ftaPaperBarcodes, batchId, timestamp);
        driedBloodSpotJaxbBuilder.buildJaxb();

        SamplesPicoDbTest.createBatch(baseUrl, client, batchId, ftaPaperBarcodes);
        SamplesPicoDbTest.sendMessages(baseUrl, client, driedBloodSpotJaxbBuilder.getMessageList());
    }
}
