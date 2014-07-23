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
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.DATABASE_FREE)
public class LevelOfDetectionTest {
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

    public void testEquals() throws Exception {
        double maxLOD = 55.3455;
        double minLOD = 45.3171;
        AggregationReadGroup readGroup1 = createAggregationReadGroup("lane1", maxLOD, "HybridSelection");
        AggregationReadGroup readGroup2 = createAggregationReadGroup("lane2", minLOD, "HybridSelection");
        LevelOfDetection lod = LevelOfDetection.calculate(Arrays.asList(readGroup1, readGroup2));

        assertThat(lod, equalTo(new LevelOfDetection(minLOD, maxLOD)));
    }


    public void testEqualsThisNull() throws Exception {
        LevelOfDetection lodThis=null;
        LevelOfDetection lodThat=new LevelOfDetection(1.1, 2.2);

        assertThat(lodThis, not(equalTo(lodThat)));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testInitializationException() throws Exception {
        LevelOfDetection iWillNeverAmountToAnything = new LevelOfDetection(1d, 0d);
        Assert.fail("I should have thrown an IllegalStateException just now.");

    }

    public void testEqualsThatNull() throws Exception {
        LevelOfDetection lodThis=new LevelOfDetection(1.1, 2.2);
        LevelOfDetection lodThat=null;

        assertThat(lodThis, not(equalTo(lodThat)));
    }

    public void testConstructor() {
        double maxLOD = 55.3455;
        double minLOD = 45.3171;
        LevelOfDetection levelOfDetection = new LevelOfDetection(minLOD, maxLOD);
        assertThat(levelOfDetection.getMax(), equalTo(maxLOD));
        assertThat(levelOfDetection.getMin(), equalTo(minLOD));
    }

    private AggregationReadGroup createAggregationReadGroup(String laneName, Double lodExpected, String metricsType) {
        int picardAnalysisId = RandomUtils.nextInt();
        int aggregationId = RandomUtils.nextInt();
        String flowcellBarcode = RandomStringUtils.random(8);
        String libraryName = RandomStringUtils.random(8);
        String molecularBarcodeName = RandomStringUtils.random(8);

        PicardFingerprint picardFingerprint =
                new PicardFingerprint(picardAnalysisId, lodExpected);


        PicardAnalysis picardAnalysis =
                new PicardAnalysis(picardAnalysisId, flowcellBarcode, laneName,
                        libraryName, metricsType,
                        picardFingerprint);

        AggregationReadGroup aggregationReadGroup = new AggregationReadGroup(aggregationId, flowcellBarcode, 1,
                libraryName, molecularBarcodeName, true, Arrays.asList(picardAnalysis));

        return aggregationReadGroup;
    }
}
