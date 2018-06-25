package org.broadinstitute.gpinformatics.mercury.test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventBean;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventResponseBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.TubeBean;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.SamplesPicoJaxbBuilder;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A database test of the Samples Pico process
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class SamplesPicoDbTest extends StubbyContainerTest {

    public SamplesPicoDbTest(){}

    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("MMddHHmmss");

    @Test(enabled = true, groups = TestGroups.STUBBY, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEndToEnd(@ArquillianResource URL baseUrl) throws Exception {
        String timestamp = timestampFormat.format(new Date());

        ClientConfig clientConfig = JerseyUtils.getClientConfigAcceptCertificate();
        clientConfig.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, Boolean.TRUE);

        Client client = Client.create(clientConfig);
        client.addFilter(new LoggingFilter(System.out));

        String batchId = "BP-" + timestamp;
        ArrayList<String> tubeBarcodes = new ArrayList<>();
        for (int i = 1; i <= 96; i++) {
            tubeBarcodes.add("PICO" + i + "_" + timestamp);
        }
        createBatch(baseUrl, client, batchId, tubeBarcodes);

        SamplesPicoJaxbBuilder samplesPicoJaxbBuilder =
                new SamplesPicoJaxbBuilder(tubeBarcodes, batchId, timestamp);
        samplesPicoJaxbBuilder.buildJaxb();
        List<BettaLIMSMessage> messageList = samplesPicoJaxbBuilder.getMessageList();
        sendMessages(baseUrl, client, messageList);

        LabEventResponseBean labEventResponseBean =
                client.resource(RestServiceContainerTest.convertUrlToSecure(baseUrl) + "rest/labevent/batch")
                        .path(batchId)
                        .get(LabEventResponseBean.class);
        List<LabEventBean> labEventBeans = labEventResponseBean.getLabEventBeans();
        Assert.assertEquals(10, labEventBeans.size(), "Wrong number of lab events");
        SamplesPicoEndToEndTest.printLabEvents(labEventBeans);
        // todo jmt more asserts
    }

    /**
     * Call the web service to create a lab batch
     *
     * @param baseUrl      server
     * @param client       jersey
     * @param batchId      id of the batch to create
     * @param tubeBarcodes barcodes of the tubes to associate with the batch
     *
     * @return bean sent to the web service
     */
    public static LabBatchBean createBatch(URL baseUrl, Client client, String batchId,
                                           List<String> tubeBarcodes) throws Exception {
        ArrayList<TubeBean> tubeBeans = new ArrayList<>();
        for (String tubeBarcode : tubeBarcodes) {
            tubeBeans.add(new TubeBean(tubeBarcode, null));
        }
        LabBatchBean labBatchBean = new LabBatchBean(batchId, null, tubeBeans);

        String response = client.resource(RestServiceContainerTest.convertUrlToSecure(baseUrl) + "rest/labbatch")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .entity(labBatchBean)
                .post(String.class);
        System.out.println(response);
        return labBatchBean;
    }

    /**
     * Calls the web service that accepts BettaLIMS messages
     *
     * @param baseUrl     server
     * @param client      jersey
     * @param messageList list of messages to send
     */
    public static String sendMessages(URL baseUrl, Client client, List<BettaLIMSMessage> messageList)
            throws Exception {
        String response = null;
        for (BettaLIMSMessage bettaLIMSMessage : messageList) {
            response =
                    client.resource(RestServiceContainerTest.convertUrlToSecure(baseUrl) + "rest/bettalimsmessage")
                            .type(MediaType.APPLICATION_XML_TYPE)
                            .accept(MediaType.APPLICATION_XML)
                            .entity(bettaLIMSMessage)
                            .post(String.class);
            System.out.println(response);
        }
        return response;
    }
}
