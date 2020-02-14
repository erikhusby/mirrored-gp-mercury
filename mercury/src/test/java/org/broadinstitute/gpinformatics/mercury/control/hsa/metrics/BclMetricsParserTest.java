package org.broadinstitute.gpinformatics.mercury.control.hsa.metrics;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.testng.Assert.*;

public class BclMetricsParserTest {

    @Test
    public void testParseStats() {
        InputStream inputStream = VarioskanParserTest.getTestResource("dragen/Stats.json");
        BclMetricsParser metricsParser = new BclMetricsParser();
        MessageCollection messageCollection = new MessageCollection();
        List<DemultiplexStats> demultiplexStats = metricsParser.parseStats(inputStream, messageCollection);
        Assert.assertEquals(messageCollection.getErrors().size(), 0);
        Assert.assertNotNull(demultiplexStats);

        DemultiplexStats next = demultiplexStats.iterator().next();
        Assert.assertEquals(next.getSampleID(), "SM-JFU9F");
        Assert.assertEquals(next.getIndex(), "CATGCTTA-ATGAATTA");
        Assert.assertEquals(next.getNumberOfReads(), 128519833);
        Assert.assertEquals(next.getNumberOfPerfectIndexReads(), 122150302);
        Assert.assertEquals(next.getNumberOfOneMismatchIndexreads(), 6369531);
        Assert.assertEquals(next.getNumberOfQ30BasesPassingFilter(), new BigDecimal("36123706714"));
        Assert.assertEquals(next.getMeanQualityScorePassingFilter(), "35.75");
    }
}