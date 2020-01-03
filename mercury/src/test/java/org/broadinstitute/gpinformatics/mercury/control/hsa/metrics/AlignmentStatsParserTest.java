package org.broadinstitute.gpinformatics.mercury.control.hsa.metrics;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;

@Test(groups = TestGroups.DATABASE_FREE)
public class AlignmentStatsParserTest {

    @Test
    public void testAggregation() throws IOException {
        AlignmentStatsParser parser = new AlignmentStatsParser();
        File folder = new File("src/test/resources/testdata/dragen");
        DragenReplayInfo replayInfo = new DragenReplayInfo();
        DragenReplayInfo.System system = new DragenReplayInfo.System();
        system.setDragenVersion("1");
        system.setNodename("1");
        system.setKernelRelease("1");
        replayInfo.setSystem(system);
        AlignmentStatsParser.AlignmentDataFiles alignmentDataFiles =
                parser.parseFolder("runName", new Date(), "analysistest", replayInfo, folder, "SM-1",
                        "SM-1");
        Assert.assertNotNull(alignmentDataFiles);
    }
}