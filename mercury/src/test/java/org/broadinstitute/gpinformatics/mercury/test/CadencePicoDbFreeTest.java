package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.test.builders.CadencePicoJaxbBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Test messaging for BSP Cadence Pico
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class CadencePicoDbFreeTest {
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("MMddHHmmss");

    public void testEndToEnd() {
        String testSuffix = timestampFormat.format(new Date());

        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);

        double dilutionFactor = 2;
        String sourceRackBarcode = "CadencePicoSamplesRack" + testSuffix;
        List<String> picoSampleTubeBarcodes = new ArrayList<>();
        for (int rackPosition = 1; rackPosition <= 96; rackPosition++) {
            picoSampleTubeBarcodes.add("CadencePico" + testSuffix + rackPosition);
        }

        CadencePicoJaxbBuilder cadencePicoJaxbBuilder = new CadencePicoJaxbBuilder(
            bettaLimsMessageTestFactory, testSuffix, picoSampleTubeBarcodes, sourceRackBarcode, dilutionFactor
        ).invoke();

        Assert.assertEquals(3, cadencePicoJaxbBuilder.getMessageList().size(), "Wrong number of messages");
    }
}
