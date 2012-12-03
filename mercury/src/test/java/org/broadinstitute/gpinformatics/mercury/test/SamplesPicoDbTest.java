package org.broadinstitute.gpinformatics.mercury.test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.filter.LoggingFilter;
import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventBean;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventResponseBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.TubeBean;
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
 * A database test of the Samples Pico process
 */
public class SamplesPicoDbTest extends ContainerTest {

    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("MMddHHmmss");

    @Test(enabled = true, groups = TestGroups.EXTERNAL_INTEGRATION, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEndToEnd(@ArquillianResource URL baseUrl) {
        String timestamp = timestampFormat.format(new Date());

        Client client = Client.create();
        client.addFilter(new LoggingFilter(System.out));

        String batchId = "BP-" + timestamp;
        ArrayList<String> tubeBarcodes = new ArrayList<String>();
        for(int i = 1; i <= 96; i++) {
            tubeBarcodes.add("PICO" + i + "_" + timestamp);
        }
        createBatch(baseUrl, client, batchId, tubeBarcodes);

        SamplesPicoEndToEndTest.SamplesPicoJaxbBuilder samplesPicoJaxbBuilder =
                new SamplesPicoEndToEndTest.SamplesPicoJaxbBuilder(tubeBarcodes, batchId, timestamp);
        samplesPicoJaxbBuilder.buildJaxb();
        List<BettaLIMSMessage> messageList = samplesPicoJaxbBuilder.getMessageList();
        sendMessages(baseUrl, client, messageList);

        LabEventResponseBean labEventResponseBean = client.resource(baseUrl.toExternalForm() + "rest/labevent/batch")
                .path(batchId)
                .get(LabEventResponseBean.class);
        List<LabEventBean> labEventBeans = labEventResponseBean.getLabEventBeans();
        Assert.assertEquals("Wrong number of lab events", 10, labEventBeans.size());
        SamplesPicoEndToEndTest.printLabEvents(labEventBeans);
        // todo jmt more asserts
    }

    /**
     * Call the web service to create a lab batch
     * @param baseUrl server
     * @param client jersey
     * @param batchId id of the batch to create
     * @param tubeBarcodes barcodes of the tubes to associate with the batch
     * @return bean sent to the web service
     */
    public static LabBatchBean createBatch(URL baseUrl, Client client, String batchId,
            List<String> tubeBarcodes) {
        ArrayList<TubeBean> tubeBeans = new ArrayList<TubeBean>();
        for (String tubeBarcode : tubeBarcodes) {
            tubeBeans.add(new TubeBean(tubeBarcode, null, null));
        }
        LabBatchBean labBatchBean = new LabBatchBean(batchId, null, tubeBeans);

        String response = client.resource(baseUrl.toExternalForm() + "rest/labbatch")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .entity(labBatchBean)
                .post(String.class);
        System.out.println(response);
        return labBatchBean;
    }

    /**
     * Calls the web service that accepts BettaLIMS messages
     * @param baseUrl server
     * @param client jersey
     * @param messageList list of messages to send
     */
    public static void sendMessages(URL baseUrl, Client client, List<BettaLIMSMessage> messageList) {
        for (BettaLIMSMessage bettaLIMSMessage : messageList) {
            String response;
            response = client.resource(baseUrl.toExternalForm() + "rest/bettalimsmessage")
                    .type(MediaType.APPLICATION_XML_TYPE)
                    .accept(MediaType.APPLICATION_XML)
                    .entity(bettaLIMSMessage)
                    .post(String.class);
            System.out.println(response);
        }
    }
}
