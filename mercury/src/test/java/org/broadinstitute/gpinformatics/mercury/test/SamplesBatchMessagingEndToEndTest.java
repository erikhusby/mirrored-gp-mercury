package org.broadinstitute.gpinformatics.mercury.test;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test the messages sent by BSP during receipt, extraction, pico, normalization and plating.
 */
public class SamplesBatchMessagingEndToEndTest {
    @Test
    public void testEndToEnd() {
        // Receipt of sample - new web service with SM-ID and optional manufacturer barcode
        // Receipt of data ?
        // Start of extraction - call LabBatchResource
        // End of extraction - Cherry pick, 1 tube to 15 tubes
        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        List<String> sourceRackBarcodes = new ArrayList<String>();
        List<List<String>> sourceTubeBarcodes = new ArrayList<List<String>>();
        String targetRackBarcode ;
        List<String> targetTubeBarcodes = new ArrayList<String>();
        List<BettaLimsMessageFactory.CherryPick> cherryPicks = new ArrayList<BettaLimsMessageFactory.CherryPick>();
        bettaLimsMessageFactory.buildCherryPick("ExtractionEndTransfer", sourceRackBarcodes, sourceTubeBarcodes,
                targetRackBarcode, targetTubeBarcodes, cherryPicks);
        // Pico - done?
        // Normalization - in place dilution or rack to rack
        // Plating - rack to rack
        // Covaris Transfer?
    }
}
