package org.broadinstitute.gpinformatics.mercury.test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.filter.LoggingFilter;
import org.broadinstitute.gpinformatics.infrastructure.test.BettaLimsMessageFactory;
import org.testng.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventBean;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventResponseBean;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Test the messages sent by BSP during receipt, extraction, pico, normalization and plating.
 */
public class SamplesBatchMessagingEndToEndTest extends ContainerTest {

    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("MMddHHmmss");

    @Test(enabled = true, groups = TestGroups.EXTERNAL_INTEGRATION, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testEndToEnd(@ArquillianResource URL baseUrl) {
        String timestamp = timestampFormat.format(new Date());
        Client client = Client.create();
        client.addFilter(new LoggingFilter(System.out));

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        List<String> sampleBarcodes = new ArrayList<String>();
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
            ReceptacleEventType receptacleEventType = bettaLimsMessageFactory.buildReceptacleEvent(
                    "SamplesExtractionStart", sampleBarcode, "Conical50");
            receptacleEventType.setBatchId(batchId);
            // todo jmt restore batch IDs.
//            receptacleEventType.setBatchId("BP-2001");
            extractionStartMsg.getReceptacleEvent().add(
                    // todo different plastic types - falcon, FFPE
                    receptacleEventType);
        }
        SamplesPicoDbTest.sendMessages(baseUrl, client, Arrays.asList(extractionStartMsg));
        bettaLimsMessageFactory.advanceTime();

        // End of extraction - Cherry pick, 1 tube to 15 tubes
        List<String> sourceRackBarcodes = new ArrayList<String>();
        String extStartRackBarcode = "ExtStartRack" + timestamp;
        sourceRackBarcodes.add(extStartRackBarcode);

        List<List<String>> sourceTubeBarcodes = new ArrayList<List<String>>();
        sourceTubeBarcodes.add(sampleBarcodes);

        String extEndRackBarcode = "ExtEndRack" + timestamp;
        List<String> extractionEndTubeBarcodes = new ArrayList<String>();
        List<BettaLimsMessageFactory.CherryPick> cherryPicks = new ArrayList<BettaLimsMessageFactory.CherryPick>();
        for(int i = 0; i < sampleBarcodes.size() * 15; i++){
            extractionEndTubeBarcodes.add("2DExt" + i + timestamp);
            // todo jmt different source tube types - falcon?
            cherryPicks.add(new BettaLimsMessageFactory.CherryPick(extStartRackBarcode, "A0" + ((i / 15) + 1),
                    extEndRackBarcode, bettaLimsMessageFactory.buildWellName(i + 1)));
        }

        BettaLIMSMessage extractionEndMsg = new BettaLIMSMessage();
        PlateCherryPickEvent plateCherryPickEvent = bettaLimsMessageFactory.buildCherryPick(
                "SamplesExtractionEndTransfer", sourceRackBarcodes, sourceTubeBarcodes,
                extEndRackBarcode, extractionEndTubeBarcodes, cherryPicks);
        plateCherryPickEvent.setBatchId(batchId);
        extractionEndMsg.getPlateCherryPickEvent().add(plateCherryPickEvent);
        SamplesPicoDbTest.sendMessages(baseUrl, client, Arrays.asList(extractionEndMsg));
        bettaLimsMessageFactory.advanceTime();

        // Pico - messages from deck

        // Normalization - in place dilution or rack to rack
        List<String> normTubeBarcodes = new ArrayList<String>();
        for(int i = 0; i < extractionEndTubeBarcodes.size(); i++) {
            normTubeBarcodes.add("2DNorm" + i + timestamp);
        }
        String normRackBarcode = "NormRack" + timestamp;
        BettaLIMSMessage normMsg = new BettaLIMSMessage();
        PlateTransferEventType plateTransferEventType = bettaLimsMessageFactory.buildRackToRack("SamplesNormalizationTransfer",
                extEndRackBarcode, extractionEndTubeBarcodes, normRackBarcode, normTubeBarcodes);
        plateTransferEventType.setBatchId(batchId);
        normMsg.getPlateTransferEvent().add(plateTransferEventType);
        SamplesPicoDbTest.sendMessages(baseUrl, client, Arrays.asList(normMsg));
        bettaLimsMessageFactory.advanceTime();

        // Plating - rack to Covaris "plate"?  (The Covaris rack is considered to be a plate, because the tubes don't
        // have barcodes)
        BettaLIMSMessage platingMsg = new BettaLIMSMessage();
        PlateTransferEventType rackToCovaris = bettaLimsMessageFactory.buildRackToPlate("SamplesPlatingToCovaris",
                normRackBarcode, normTubeBarcodes, "CovarisRack" + timestamp);
        rackToCovaris.getPlate().setPhysType("CovarisRack");
        rackToCovaris.setBatchId(batchId);
        platingMsg.getPlateTransferEvent().add(rackToCovaris);
        SamplesPicoDbTest.sendMessages(baseUrl, client, Arrays.asList(platingMsg));
        bettaLimsMessageFactory.advanceTime();

        LabEventResponseBean labEventResponseBean = client.resource(baseUrl.toExternalForm() + "rest/labevent/batch")
                .path(batchId)
                .get(LabEventResponseBean.class);
        List<LabEventBean> labEventBeans = labEventResponseBean.getLabEventBeans();
        Assert.assertEquals(labEventBeans.size(), 6, "Wrong number of lab events");
        SamplesPicoEndToEndTest.printLabEvents(labEventBeans);
    }
}
