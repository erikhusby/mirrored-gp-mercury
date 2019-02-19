package org.broadinstitute.gpinformatics.mercury.test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ChildVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.StoolTNAJaxbBuilder;
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

/**
 * A database test of the Stool TNA Extraction process
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class StoolTNAExtractionDbTest extends StubbyContainerTest {

    public StoolTNAExtractionDbTest(){}

    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("MMddHHmmss");

    @Test(enabled = true, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEndToEnd(@ArquillianResource URL baseUrl) throws Exception {
        String timestamp = timestampFormat.format(new Date());

        ArrayList<ChildVesselBean> childVesselBeans = new ArrayList<>();
        childVesselBeans.add(new ChildVesselBean(null, "SM-1234" + timestamp, "Well [200uL]", "A01"));
        childVesselBeans.add(new ChildVesselBean(null, "SM-2345" + timestamp, "Well [200uL]", "A02"));
        ParentVesselBean parentVesselBean =
                new ParentVesselBean("P1234" + timestamp, null, "Plate96Well200PCR", childVesselBeans);

        String batchId = "BP-" + timestamp;

        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);
        StoolTNAJaxbBuilder stoolTNAJaxbBuilder = new StoolTNAJaxbBuilder(parentVesselBean.getManufacturerBarcode(),
                childVesselBeans.size(), bettaLimsMessageTestFactory, "StoolXTR" + timestamp).invoke();

        ClientConfig clientConfig = JerseyUtils.getClientConfigAcceptCertificate();

        Client client = Client.create(clientConfig);
        client.addFilter(new LoggingFilter(System.out));

        String batchResponse = createBatch(baseUrl, client, batchId, parentVesselBean);
        Assert.assertEquals(batchResponse, "Batch persisted");
        SamplesPicoDbTest.sendMessages(baseUrl, client, stoolTNAJaxbBuilder.getMessageList());
    }

    public static String createBatch(URL baseUrl, Client client, String batchId,
                                           ParentVesselBean parentVesselBean) throws Exception {
        LabBatchBean labBatchBean = new LabBatchBean(batchId, parentVesselBean, "jowalsh");

        String response = client.resource(RestServiceContainerTest.convertUrlToSecure(baseUrl) + "rest/labbatch")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .entity(labBatchBean)
                .post(String.class);
        return response;
    }
}
