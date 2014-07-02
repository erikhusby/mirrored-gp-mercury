/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.metrics;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationReadGroup;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.PicardAnalysis;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.PicardFingerprint;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

public class LevelOfDetectionTest {

    @Test(enabled = true)
    public void testCalculate() throws Exception {
        double maxLOD = 55.3455;
        double minLOD = 45.3171;
        AggregationReadGroup readGroup1 = createAggregationReadGroup("lane1", maxLOD, "HybridSelection");
        AggregationReadGroup readGroup2 = createAggregationReadGroup("lane2", minLOD, "HybridSelection");
        LevelOfDetection lod = LevelOfDetection.calculate(Arrays.asList(readGroup1, readGroup2));
        assertThat(lod.getMin(), lessThan(lod.getMax()));
        assertThat(lod.getMax(), equalTo(maxLOD));
        assertThat(lod.getMin(), equalTo(minLOD));

    }

    @Test(enabled = true)
    public void testEquals() throws Exception {
        double maxLOD = 55.3455;
        double minLOD = 45.3171;
        AggregationReadGroup readGroup1 = createAggregationReadGroup("lane1", maxLOD, "HybridSelection");
        AggregationReadGroup readGroup2 = createAggregationReadGroup("lane2", minLOD, "HybridSelection");
        LevelOfDetection lod = LevelOfDetection.calculate(Arrays.asList(readGroup1, readGroup2));

        assertThat(lod, equalTo(new LevelOfDetection(minLOD, maxLOD)));
    }

    private AggregationReadGroup createAggregationReadGroup(String laneName, Double lodExpected, String metricsType) {
        int picardAnalysisId = RandomUtils.nextInt();
        int aggregationId = RandomUtils.nextInt();
        String flowcellBarcode = RandomStringUtils.random(8);
        String libraryName = RandomStringUtils.random(8);
        String molecularBarcodeName = RandomStringUtils.random(8);

        PicardFingerprint picardFingerprint =
                new PicardFingerprint(picardAnalysisId, null, null, randomDouble(-50, 50), randomDouble(-50, 50),
                        lodExpected, randomInt(), randomInt(), randomInt());


        PicardAnalysis picardAnalysis =
                new PicardAnalysis(picardAnalysisId, flowcellBarcode, laneName, molecularBarcodeName,
                        libraryName, null, null, null, null, RandomStringUtils.random(8), metricsType,
                        picardFingerprint);

        AggregationReadGroup aggregationReadGroup = new AggregationReadGroup(aggregationId, flowcellBarcode, 1,
                libraryName, molecularBarcodeName, true, Arrays.asList(picardAnalysis));

        return aggregationReadGroup;
    }





    private static double randomDouble(int min, int max) {
        Random r = new Random();
        return min + (max - min) * r.nextDouble();
    }

    private static int randomInt() {
        return randomInt(100);
    }

    private static int randomInt(int max) {
        Random r = new Random();
        return r.nextInt(max);
    }
}
