package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.testng.annotations.Test;

/**
 * Tests the upload of a spreadsheet containing sample IDs and 2D tube barcodes.
 */
public class UploadSampleTubesTest {
    @Test
    public void test() {
        // Sample ID, Manufacturer Tube Barcode, Sample Kit ID
        // SampleImportResource uses labVesselFactory.buildLabVessels
        // BillingTrackerProcessor extends TableProcessor
        // PoiSpreadsheetParser
    }
}
