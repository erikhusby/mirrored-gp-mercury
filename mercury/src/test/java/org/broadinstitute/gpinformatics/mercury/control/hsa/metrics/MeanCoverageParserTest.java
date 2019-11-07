package org.broadinstitute.gpinformatics.mercury.control.hsa.metrics;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.InputStream;

import static org.testng.Assert.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class MeanCoverageParserTest {

    public static final String MEAN_COV_FILE = "/dragen/SM-G9483.qc-coverage-region-1_overall_mean_cov.csv";

    @Test
    public void testParseMeanCoverage() {
        InputStream is = VarioskanParserTest.getSpreadsheet(MEAN_COV_FILE);
        MessageCollection messageCollection = new MessageCollection();
        MeanCoverageParser parser = new MeanCoverageParser();
        Float aFloat = parser.parseMeanCoverage(is, messageCollection);
        Assert.assertEquals(32.12, aFloat, 0.1);
    }
}