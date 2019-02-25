package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventBean;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventResponseBean;
import org.broadinstitute.gpinformatics.mercury.control.EntityLoggingFilter;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Test the messages sent by BSP during receipt, extraction, pico, normalization and plating.
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class SamplesBatchMessagingEndToEndTest extends StubbyContainerTest {

    public SamplesBatchMessagingEndToEndTest(){}

    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("MMddHHmmss");

    @Test(enabled = true, groups = TestGroups.STUBBY, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEndToEnd(@ArquillianResource URL baseUrl) throws Exception {
        String timestamp = timestampFormat.format(new Date());
        ClientBuilder clientBuilder = JerseyUtils.getClientBuilderAcceptCertificate();
//        clientConfig.property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE);

        Client client = clientBuilder.build();
        client.register(new EntityLoggingFilter());

        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);
        List<String> sampleBarcodes = new ArrayList<>();
        sampleBarcodes.add("SM-1001-" + timestamp);
        sampleBarcodes.add("SM-1002-" + timestamp);
        sampleBarcodes.add("SM-1003-" + timestamp);

        String batchId = "BP-" + timestamp;
        SamplesPicoDbTest.createBatch(baseUrl, client, batchId, sampleBarcodes);

        // Receipt of sample - new web service with SM-ID and optional manufacturer barcode

        // Receipt of data ?

        // Start of extraction - call LabBatchResource
        BettaLIMSMessage extractionStartMsg = new BettaLIMSMessage();
        for (String sampleBarcode : sampleBarcodes) {
            ReceptacleEventType receptacleEventType = bettaLimsMessageTestFactory.buildReceptacleEvent(
                    "SamplesExtractionStart", sampleBarcode, "Conical50");
            receptacleEventType.setBatchId(batchId);
            // todo jmt restore batch IDs.
//            receptacleEventType.setBatchId("BP-2001");
            extractionStartMsg.getReceptacleEvent().add(
                    // todo different plastic types - falcon, FFPE
                    receptacleEventType);
        }
        SamplesPicoDbTest.sendMessages(baseUrl, client, Arrays.asList(extractionStartMsg));
        bettaLimsMessageTestFactory.advanceTime();

        // End of extraction - Cherry pick, 1 tube to 15 tubes
        List<String> sourceRackBarcodes = new ArrayList<>();
        String extStartRackBarcode = "ExtStartRack" + timestamp;
        sourceRackBarcodes.add(extStartRackBarcode);

        List<List<String>> sourceTubeBarcodes = new ArrayList<>();
        sourceTubeBarcodes.add(sampleBarcodes);

        String extEndRackBarcode = "ExtEndRack" + timestamp;
        List<String> extractionEndTubeBarcodes = new ArrayList<>();
        List<BettaLimsMessageTestFactory.CherryPick> cherryPicks = new ArrayList<>();
        for (int i = 0; i < sampleBarcodes.size() * 15; i++) {
            extractionEndTubeBarcodes.add("2DExt" + i + timestamp);
            // todo jmt different source tube types - falcon?
            cherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(extStartRackBarcode, "A0" + ((i / 15) + 1),
                    extEndRackBarcode, bettaLimsMessageTestFactory.buildWellName(i + 1,
                    BettaLimsMessageTestFactory.WellNameType.SHORT)));
        }

        BettaLIMSMessage extractionEndMsg = new BettaLIMSMessage();
        PlateCherryPickEvent plateCherryPickEvent = bettaLimsMessageTestFactory.buildCherryPick(
                "SamplesExtractionEndTransfer", sourceRackBarcodes, sourceTubeBarcodes,
                Collections.singletonList(extEndRackBarcode), Collections.singletonList(extractionEndTubeBarcodes),
                cherryPicks);
        plateCherryPickEvent.setBatchId(batchId);
        extractionEndMsg.getPlateCherryPickEvent().add(plateCherryPickEvent);
        SamplesPicoDbTest.sendMessages(baseUrl, client, Arrays.asList(extractionEndMsg));
        bettaLimsMessageTestFactory.advanceTime();

        // Pico - messages from deck

        // Normalization - in place dilution or rack to rack
        List<String> normTubeBarcodes = new ArrayList<>();
        for (int i = 0; i < extractionEndTubeBarcodes.size(); i++) {
            normTubeBarcodes.add("2DNorm" + i + timestamp);
        }
        String normRackBarcode = "NormRack" + timestamp;
        BettaLIMSMessage normMsg = new BettaLIMSMessage();
        PlateTransferEventType plateTransferEventType =
                bettaLimsMessageTestFactory.buildRackToRack("SamplesNormalizationTransfer",
                        extEndRackBarcode, extractionEndTubeBarcodes, normRackBarcode, normTubeBarcodes);
        plateTransferEventType.setBatchId(batchId);
        normMsg.getPlateTransferEvent().add(plateTransferEventType);
        SamplesPicoDbTest.sendMessages(baseUrl, client, Arrays.asList(normMsg));
        bettaLimsMessageTestFactory.advanceTime();

        // Plating - rack to Covaris "plate"?  (The Covaris rack is considered to be a plate, because the tubes don't
        // have barcodes)
        BettaLIMSMessage platingMsg = new BettaLIMSMessage();
        PlateTransferEventType rackToCovaris = bettaLimsMessageTestFactory.buildRackToPlate("SamplesPlatingToCovaris",
                normRackBarcode, normTubeBarcodes, "CovarisRack" + timestamp);
        rackToCovaris.getPlate().setPhysType("CovarisRack");
        rackToCovaris.setBatchId(batchId);
        platingMsg.getPlateTransferEvent().add(rackToCovaris);
        SamplesPicoDbTest.sendMessages(baseUrl, client, Arrays.asList(platingMsg));
        bettaLimsMessageTestFactory.advanceTime();

        LabEventResponseBean labEventResponseBean =
                client.target(RestServiceContainerTest.convertUrlToSecure(baseUrl) + "rest/labevent/batch")
                        .path(batchId)
                        .request()
                        .get(LabEventResponseBean.class);
        List<LabEventBean> labEventBeans = labEventResponseBean.getLabEventBeans();
        Assert.assertEquals(labEventBeans.size(), 6, "Wrong number of lab events");
        SamplesPicoEndToEndTest.printLabEvents(labEventBeans);
    }
}
