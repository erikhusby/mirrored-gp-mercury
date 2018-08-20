package org.broadinstitute.gpinformatics.mercury.test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventBean;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventResponseBean;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.MetadataBean;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.ReagentBean;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.CadencePicoJaxbBuilder;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Tests Cadence Pico messaging, including persistence
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class CadencePicoDbTest extends StubbyContainerTest {

    public CadencePicoDbTest(){}

    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("MMddHHmmss");

    @Test(enabled = true, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    //@RunAsClient
    public void testEndToEnd(@ArquillianResource URL baseUrl) throws Exception {
        String testSuffix = timestampFormat.format(new Date());

        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);

        double dilutionFactor = 2;
        String sourceRackBarcode = "CadencePicoSamplesRack" + testSuffix;
        List<String> picoSampleTubeBarcodes = new ArrayList<>();
        for (int rackPosition = 1; rackPosition <= 96; rackPosition++) {
            picoSampleTubeBarcodes.add("CadencePico" + testSuffix + rackPosition);
        }

        ClientConfig clientConfig = JerseyUtils.getClientConfigAcceptCertificate();
        clientConfig.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, Boolean.TRUE);

        Client client = Client.create(clientConfig);
        client.addFilter(new LoggingFilter(System.out));

        CadencePicoJaxbBuilder cadencePicoJaxbBuilder = new CadencePicoJaxbBuilder(
                bettaLimsMessageTestFactory, testSuffix, picoSampleTubeBarcodes, sourceRackBarcode, dilutionFactor
        ).invoke();

        String batchId = "BP-" + testSuffix;

        SamplesPicoDbTest.createBatch(baseUrl, client, batchId, picoSampleTubeBarcodes);
        SamplesPicoDbTest.sendMessages(baseUrl, client, cadencePicoJaxbBuilder.getMessageList());

        //fetches plate transfers for batchless
        LabEventResponseBean labEventResponseBean =
                client.resource(RestServiceContainerTest.convertUrlToSecure(baseUrl)
                                + "rest/labevent/transfersToFirstAncestorRack")
                        .queryParam("plateBarcodes", cadencePicoJaxbBuilder.getPicoMicrofluorBarcode())
                        .get(LabEventResponseBean.class);
        List<LabEventBean> labEventBeans = labEventResponseBean.getLabEventBeans();
        Assert.assertEquals(2, labEventBeans.size(), "Wrong number of lab events");
        SamplesPicoEndToEndTest.printLabEvents(labEventBeans);

        LabEventBean dilutionLabEvent = labEventBeans.get(0);
        List<MetadataBean> metadataBeans = dilutionLabEvent.getMetadatas();
        Assert.assertEquals(1, metadataBeans.size());
        MetadataBean metadataBean = metadataBeans.get(0);
        Assert.assertEquals("DilutionFactor", metadataBean.getName());

        //Fetch reagent addition message for batchless
        labEventResponseBean = client.resource(
                RestServiceContainerTest.convertUrlToSecure(baseUrl) + "rest/labevent/inPlaceReagentEvents")
                .queryParam("plateBarcodes", cadencePicoJaxbBuilder.getPicoMicrofluorBarcode())
                .get(LabEventResponseBean.class);
        labEventBeans = labEventResponseBean.getLabEventBeans();
        Assert.assertEquals(1, labEventBeans.size(), "Wrong number of lab events");
        LabEventBean labEventBean = labEventBeans.get(0);
        Assert.assertEquals(1, labEventBean.getReagents().size(), "Wrong number of reagents");
        ReagentBean reagentBean = labEventBean.getReagents().get(0);
        Assert.assertNotNull(reagentBean.getExpiration(), "Expiration should be set");

    }
}