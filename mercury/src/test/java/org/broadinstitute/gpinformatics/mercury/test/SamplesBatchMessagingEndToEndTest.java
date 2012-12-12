package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test the messages sent by BSP during receipt, extraction, pico, normalization and plating.
 */
public class SamplesBatchMessagingEndToEndTest {
    @Test
    public void testEndToEnd() {
        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        List<String> sampleBarcodes = new ArrayList<String>();
        sampleBarcodes.add("SM-1001");
        sampleBarcodes.add("SM-1002");
        sampleBarcodes.add("SM-1003");

        // Receipt of sample - new web service with SM-ID and optional manufacturer barcode

        // Receipt of data ?

        // Start of extraction - call LabBatchResource
        BettaLIMSMessage extractionStartMsg = new BettaLIMSMessage();
        for (String sampleBarcode : sampleBarcodes) {
            extractionStartMsg.getReceptacleEvent().add(
                    bettaLimsMessageFactory.buildReceptacleEvent("SamplesExtractionStart", sampleBarcode));
        }

        // End of extraction - Cherry pick, 1 tube to 15 tubes
        List<String> sourceRackBarcodes = new ArrayList<String>();
        sourceRackBarcodes.add("ExtStartRack");
        List<List<String>> sourceTubeBarcodes = new ArrayList<List<String>>();
        String targetRackBarcode = "ExtEndRack";
        List<String> extractionEndTubeBarcodes = new ArrayList<String>();
        List<BettaLimsMessageFactory.CherryPick> cherryPicks = new ArrayList<BettaLimsMessageFactory.CherryPick>();
        bettaLimsMessageFactory.buildCherryPick("SamplesExtractionEndTransfer", sourceRackBarcodes, sourceTubeBarcodes,
                targetRackBarcode, extractionEndTubeBarcodes, cherryPicks);

        // Pico - messages from deck

        // Normalization - in place dilution or rack to rack
        List<String> normTubeBarcodes = new ArrayList<String>();
        bettaLimsMessageFactory.buildRackToRack("SamplesNormalizationTransfer", "ExtEndRack", extractionEndTubeBarcodes,
                "NormRack", normTubeBarcodes);

        // Plating - rack to Covaris "plate"?  (The Covaris rack is considered to be a plate, because the tubes don't
        // have barcodes)
        bettaLimsMessageFactory.buildRackToPlate("SamplesPlatingToCovaris", "NormRack", normTubeBarcodes, "CovarisRack");
    }
}
