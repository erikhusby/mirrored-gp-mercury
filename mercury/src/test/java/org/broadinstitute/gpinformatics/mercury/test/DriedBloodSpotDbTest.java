package org.broadinstitute.gpinformatics.mercury.test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;
import org.broadinstitute.gpinformatics.mercury.test.builders.DriedBloodSpotJaxbBuilder;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
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

        ClientConfig clientConfig = JerseyUtils.getClientConfigAcceptCertificate();

        Client client = Client.create(clientConfig);
        client.addFilter(new LoggingFilter(System.out));

        DriedBloodSpotJaxbBuilder driedBloodSpotJaxbBuilder =
                new DriedBloodSpotJaxbBuilder(ftaPaperBarcodes, batchId, timestamp);
        driedBloodSpotJaxbBuilder.buildJaxb();

        SamplesPicoDbTest.createBatch(baseUrl, client, batchId, ftaPaperBarcodes);
        SamplesPicoDbTest.sendMessages(baseUrl, client, driedBloodSpotJaxbBuilder.getMessageList());
    }
}
