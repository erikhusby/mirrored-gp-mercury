package org.broadinstitute.sequel.test;

import com.sun.jersey.api.client.Client;
import junit.framework.Assert;
import org.broadinstitute.sequel.TestGroups;
import org.broadinstitute.sequel.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.sequel.boundary.labevent.LabEventBean;
import org.broadinstitute.sequel.boundary.labevent.LabEventResponseBean;
import org.broadinstitute.sequel.boundary.vessel.LabBatchBean;
import org.broadinstitute.sequel.boundary.vessel.TubeBean;
import org.broadinstitute.sequel.integration.ContainerTest;
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
 * A database test of the Samples Pico
 */
public class SamplesPicoDbTest extends ContainerTest {

    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("MMddHHmmss");

    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEndToEnd(@ArquillianResource URL baseUrl) {
        String timestamp = timestampFormat.format(new Date());

        ArrayList<TubeBean> tubeBeans = new ArrayList<TubeBean>();
        for(int i = 1; i <= 96; i++) {
            tubeBeans.add(new TubeBean("SM-PICO" + i + "_" + timestamp, null));
        }
        String batchId = "BP-" + timestamp;
        LabBatchBean labBatchBean = new LabBatchBean(batchId, null, tubeBeans);

        Client client = Client.create();
        String response = client.resource(baseUrl.toExternalForm() + "rest/labbatch")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .entity(labBatchBean)
                .post(String.class);
        System.out.println(response);

        List<String> tubeBarcodes = new ArrayList<String>();
        for (TubeBean tubeBean : tubeBeans) {
            tubeBarcodes.add(tubeBean.getBarcode());
        }

        SamplesPicoEndToEndTest.SamplesPicoJaxbBuilder samplesPicoJaxbBuilder =
                new SamplesPicoEndToEndTest.SamplesPicoJaxbBuilder(tubeBarcodes, batchId, timestamp);
        samplesPicoJaxbBuilder.buildJaxb();
        for (BettaLIMSMessage bettaLIMSMessage : samplesPicoJaxbBuilder.getMessageList()) {
            response = client.resource(baseUrl.toExternalForm() + "rest/bettalimsmessage")
                    .type(MediaType.APPLICATION_XML_TYPE)
                    .accept(MediaType.APPLICATION_XML)
                    .entity(bettaLIMSMessage)
                    .post(String.class);
            System.out.println(response);
        }

        LabEventResponseBean labEventResponseBean = client.resource(baseUrl.toExternalForm() + "rest/labevent/batch")
                .path(batchId)
                .get(LabEventResponseBean.class);
        List<LabEventBean> labEventBeans = labEventResponseBean.getLabEventBeans();
        Assert.assertEquals("Wrong number of lab events", 10, labEventBeans.size());
        SamplesPicoEndToEndTest.printLabEvents(labEventBeans);
        // todo jmt more asserts
    }
}
