package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Test for GPIM-6250 - Adding automated rework disposition assignments to pico triplicate read logic <br/>
 * Puts together a series of vessels with various volumes and different combinations of triplicate reads
 * to test raw logic to  produce all expected Decision and ReworkDisposition outcomes
 */
@Test(groups = {TestGroups.DATABASE_FREE})
public class PicoTripleReworkEvalTest {

    LabVessel volumeIrrelevantVessel, okNormVessel, okHighNormVessel, overflowNormVessel;
    LabVessel okDiluteBadTripVessel, overflowDiluteBadTripVessel;
    Date now = new Date();

    @BeforeClass
    public void setUp() {
        volumeIrrelevantVessel = new BarcodedTube("passingVessel", BarcodedTube.BarcodedTubeType.MatrixTube075);
        volumeIrrelevantVessel.setVolume(BigDecimal.valueOf(30.0));
        okNormVessel = new BarcodedTube("okNormVessel", BarcodedTube.BarcodedTubeType.MatrixTube075);
        okNormVessel.setVolume(BigDecimal.valueOf(30.0));
        okHighNormVessel = new BarcodedTube("okHighNormVessel", BarcodedTube.BarcodedTubeType.MatrixTube075);
        okHighNormVessel.setVolume(BigDecimal.valueOf(300.0));
        overflowNormVessel = new BarcodedTube("overflowNormVessel", BarcodedTube.BarcodedTubeType.MatrixTube075);
        overflowNormVessel.setVolume(BigDecimal.valueOf(300.0));
        okDiluteBadTripVessel = new BarcodedTube("okDiluteBadTripVessel", BarcodedTube.BarcodedTubeType.MatrixTube075);
        okDiluteBadTripVessel.setVolume(BigDecimal.valueOf(30.0));
        overflowDiluteBadTripVessel = new BarcodedTube("overflowDiluteBadTripVessel", BarcodedTube.BarcodedTubeType.MatrixTube075);
        overflowDiluteBadTripVessel.setVolume(BigDecimal.valueOf(300.0));
    }

    @DataProvider(name = "ReadData")
    public Object[][] buildReadData() {
        return new Object[][]{
                // 3 reads within 10%, concentration > 5.0
                {volumeIrrelevantVessel, "A01", now, Arrays.asList(BigDecimal.valueOf(23.0), BigDecimal.valueOf(27.0), BigDecimal.valueOf(25.0))
                        , LabMetric.MetricType.PLATING_PICO, 2L, 0.10f, LabMetricDecision.Decision.PASS, null}
                // 2 reads within 10%, concentration > 5.0
                , {volumeIrrelevantVessel, "A01", now, Arrays.asList(BigDecimal.valueOf(23.0), BigDecimal.valueOf(25.0))
                , LabMetric.MetricType.PLATING_PICO, 2L, 0.10f, LabMetricDecision.Decision.PASS, null}
                // 2 of 3 reads within 10%, concentration > 5.0
                , {volumeIrrelevantVessel, "A01", now, Arrays.asList(BigDecimal.valueOf(23.0), BigDecimal.valueOf(25.0), BigDecimal.valueOf(38.46))
                , LabMetric.MetricType.PLATING_PICO, 2L, 0.10f, LabMetricDecision.Decision.PASS, MetricReworkDisposition.AUTO_SELECT}
                // Concentration less than 5
                , {volumeIrrelevantVessel, "A01", now, Arrays.asList(BigDecimal.valueOf(4.9), BigDecimal.valueOf(5.00), BigDecimal.valueOf(5.00))
                , LabMetric.MetricType.PLATING_PICO, 2L, 0.10f, LabMetricDecision.Decision.REPEAT, MetricReworkDisposition.UNDILUTED}
                // Less than 2 reads, concentration irrelevant
                , {volumeIrrelevantVessel, "A01", now, Arrays.asList(BigDecimal.valueOf(1100.0))
                , LabMetric.MetricType.PLATING_PICO, 2L, 0.10f, LabMetricDecision.Decision.REPEAT, MetricReworkDisposition.BAD_TRIP_READS}
                // Concentration over 100, norm to 50 works w/out tube split
                , {okNormVessel, "A01", now, Arrays.asList(BigDecimal.valueOf(101.0), BigDecimal.valueOf(101.0))
                , LabMetric.MetricType.PLATING_PICO, 2L, 0.10f, LabMetricDecision.Decision.REPEAT, MetricReworkDisposition.NORM_IN_TUBE}
                // Concentration over 100, norm to 50 causes overflow, norm to < 100 does not cause overflow
                , {okHighNormVessel, "A01", now, Arrays.asList(BigDecimal.valueOf(170.0), BigDecimal.valueOf(180.0), BigDecimal.valueOf(190.0))
                , LabMetric.MetricType.PLATING_PICO, 2L, 0.10f, LabMetricDecision.Decision.REPEAT, MetricReworkDisposition.NORM_ADJUSTED_DOWN}
                // Concentration over 100, norm to 50 causes overflow, norm to < 100 causes overflow
                , {overflowNormVessel, "A01", now, Arrays.asList(BigDecimal.valueOf(185.0), BigDecimal.valueOf(200.0), BigDecimal.valueOf(215.0))
                , LabMetric.MetricType.PLATING_PICO, 2L, 0.10f, LabMetricDecision.Decision.REPEAT, MetricReworkDisposition.TUBE_SPLIT}
                // No 2 reads within 10%, concentration over 40
                , {okDiluteBadTripVessel, "A01", now, Arrays.asList(BigDecimal.valueOf(175.0), BigDecimal.valueOf(200.0), BigDecimal.valueOf(225.0))
                , LabMetric.MetricType.PLATING_PICO, 2L, 0.10f, LabMetricDecision.Decision.REPEAT, MetricReworkDisposition.BAD_TRIP_HIGH}
                // No 2 reads within 10%, concentration over 40
                , {okDiluteBadTripVessel, "A01", now, Arrays.asList(BigDecimal.valueOf(170.0), BigDecimal.valueOf(200.0))
                , LabMetric.MetricType.PLATING_PICO, 2L, 0.10f, LabMetricDecision.Decision.REPEAT, MetricReworkDisposition.BAD_TRIP_HIGH}
                // No 2 reads within 10%, concentration below 40
                , {okDiluteBadTripVessel, "A01", now, Arrays.asList(BigDecimal.valueOf(30.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0))
                , LabMetric.MetricType.PLATING_PICO, 2L, 0.10f, LabMetricDecision.Decision.REPEAT, MetricReworkDisposition.BAD_TRIP_LOW}
                // No 2 reads within 10%, concentration below 40
                , {okDiluteBadTripVessel, "A01", now, Arrays.asList(BigDecimal.valueOf(30.0), BigDecimal.valueOf(40.0))
                , LabMetric.MetricType.PLATING_PICO, 2L, 0.10f, LabMetricDecision.Decision.REPEAT, MetricReworkDisposition.BAD_TRIP_LOW}
        };
    }


    @Test(dataProvider = "ReadData")
    public void testEvalBadTriplicate(LabVessel tube, String tubePosition, Date runStarted, List<BigDecimal> concList,
                                      LabMetric.MetricType metricType, Long decidingUser, float maxPercentDiff,
                                      LabMetricDecision.Decision expectedDecision,
                                      MetricReworkDisposition expectedDisposition) {
        // Arrays.asList() doesn't support remove()
        ArrayList<BigDecimal> nonAbstractConcList = new ArrayList<>(concList);

        Optional<LabMetric> possibleLabMetric = PicoTripleReworkEval.evalBadTriplicate(tube, tubePosition, runStarted, nonAbstractConcList,
                metricType, decidingUser, maxPercentDiff);
        LabMetric labMetric = possibleLabMetric.get();
        Assert.assertEquals(labMetric.getLabMetricDecision().getDecision(), expectedDecision);
        if (expectedDisposition == null) {
            Assert.assertNull(labMetric.getLabMetricDecision().getReworkDisposition());
        } else {
            Assert.assertEquals(labMetric.getLabMetricDecision().getReworkDisposition(), expectedDisposition);
        }

    }
}