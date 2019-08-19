package org.broadinstitute.gpinformatics.mercury.control.hsa.metrics;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;


@Test(groups = TestGroups.DATABASE_FREE)
public class DemultiplexStatsTest {

    public static final String DEMUX_FILE = "/dragen/DemuxStats.csv";
    public static final String DEMUX_NANS_FILE = "/dragen/DemuxStatsNan.csv";
    public static final String REPLAY_FILE = "/dragen/replay_nova.json";

    @Test
    public void testBasicParse() {
        InputStream is = VarioskanParserTest.getSpreadsheet(DEMUX_FILE);
        try {
            MessageCollection messageCollection = new MessageCollection();
            DemultiplexStatsParser parser = new DemultiplexStatsParser();
            List<DemultiplexStats> stats = parser.parseStats(is, messageCollection);
            Assert.assertEquals(stats.size(), 99);

            DemultiplexStats demultiplexStats = stats.get(0);
            Assert.assertEquals(demultiplexStats.getLane(), 1);
            Assert.assertEquals(demultiplexStats.getSampleID(), "TCGA-GU-A766-01A-11D-A32B-08");
            Assert.assertEquals(demultiplexStats.getNumberOfReads(), 164309338);
            Assert.assertEquals(demultiplexStats.getNumberOfPerfectIndexReads(), 160074550);
            Assert.assertEquals(demultiplexStats.getNumberOfOneMismatchIndexreads(), 4234788);
            Assert.assertEquals(demultiplexStats.getNumberOfQ30BasesPassingFilter(), new BigDecimal("45416310813"));
            Assert.assertEquals(demultiplexStats.getMeanQualityScorePassingFilter(), "35.52");
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    public void testNans() {
        InputStream is = VarioskanParserTest.getSpreadsheet(DEMUX_NANS_FILE);
        try {
            MessageCollection messageCollection = new MessageCollection();
            DemultiplexStatsParser parser = new DemultiplexStatsParser();
            List<DemultiplexStats> stats = parser.parseStats(is, messageCollection);
            Assert.assertEquals(stats.size(), 99);

            DemultiplexStats demultiplexStats = stats.get(0);
            Assert.assertEquals(demultiplexStats.getLane(), 1);
            Assert.assertEquals(demultiplexStats.getSampleID(), "SM-IN8EG");
            Assert.assertEquals(demultiplexStats.getMeanQualityScorePassingFilter(), "-nan");
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    public void testParseReplayFile() {
        InputStream is = VarioskanParserTest.getSpreadsheet(REPLAY_FILE);
        try {
            MessageCollection messageCollection = new MessageCollection();
            DemultiplexStatsParser parser = new DemultiplexStatsParser();
            DragenReplayInfo replayInfo = parser.parseReplayInfo(is, messageCollection);
            Assert.assertNotNull(replayInfo);
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }
}