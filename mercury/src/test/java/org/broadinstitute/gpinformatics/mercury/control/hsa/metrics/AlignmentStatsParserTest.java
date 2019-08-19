package org.broadinstitute.gpinformatics.mercury.control.hsa.metrics;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

public class AlignmentStatsParserTest {

    @Test
    public void testBasicParse() throws IOException {
        AlignmentStatsParser parser = new AlignmentStatsParser();
        URL resource = Thread.currentThread().getContextClassLoader().getResource("testdata/dragen");
        File dragenDir = new File(resource.getFile());
        MessageCollection messageCollection = new MessageCollection();
        DragenReplayInfo dragenReplayInfo = new DragenReplayInfo();
        dragenReplayInfo.getSystem().setNodename("dragen01");
        dragenReplayInfo.getSystem().setDragenVersion("1.0.0.1");

        Map<String, String> mapReadGroupToSample = new HashMap<>();
        mapReadGroupToSample.put("CAATTAAC.CGAGATAT.2", "TCGA-CF-A9FH-01A-11D-A38G-08");
        parser.parseStats(dragenDir, "TCGA-CF-A9FH-01A-11D-A38G-08", dragenReplayInfo, messageCollection,
                mapReadGroupToSample);
    }
}