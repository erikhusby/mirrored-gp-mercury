package org.broadinstitute.gpinformatics.mercury.test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.filter.LoggingFilter;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.test.builders.CadencePicoJaxbBuilder;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Tests Cadence Pico messaging, including persistence
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class CadencePicoDbTest extends ContainerTest {

    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("MMddHHmmss");

    @Test(enabled = true, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEndToEnd(@ArquillianResource URL baseUrl) {
        String testSuffix = timestampFormat.format(new Date());

        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);

        double dilutionFactor = 2;
        String sourceRackBarcode = "CadencePicoSamplesRack" + testSuffix;
        List<String> picoSampleTubeBarcodes = new ArrayList<>();
        for (int rackPosition = 1; rackPosition <= 96; rackPosition++) {
            picoSampleTubeBarcodes.add("CadencePico" + testSuffix + rackPosition);
        }

        Client client = Client.create();
        client.addFilter(new LoggingFilter(System.out));

        CadencePicoJaxbBuilder cadencePicoJaxbBuilder = new CadencePicoJaxbBuilder(
                bettaLimsMessageTestFactory, testSuffix, picoSampleTubeBarcodes, sourceRackBarcode, dilutionFactor
        ).invoke();

        String batchId = "BP-" + testSuffix;


        SamplesPicoDbTest.createBatch(baseUrl, client, batchId, picoSampleTubeBarcodes);
        SamplesPicoDbTest.sendMessages(baseUrl, client, cadencePicoJaxbBuilder.getMessageList());
    }
}