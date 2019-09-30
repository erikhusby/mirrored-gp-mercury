package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Pico triplicate read logic to identify and assign rework dispositions <br/>
 * Logic shared between INITIAL_PICO and PLATING_PICO, the difference being PLATING_PICO does not have ReworkDisposition assigned automatically
 */
public class PicoTripleReworkEval {

    @DaoFree
    public static Optional<LabMetric> evalBadTriplicate(LabVessel tube, String tubePosition, Date runStarted, List<BigDecimal> concList,
                                                        LabMetric.MetricType metricType, Long decidingUser, float maxPercentDiff) {
        LabMetric labMetric = null;
        BigDecimal quant;
        int originalReadCount = concList.size();

        // Less than 2 quants in a triplicate read results in a fail/repeat.
        if (originalReadCount < 2) {
            String note = concList.size() == 1 ? "Only one read." : "No reads";
            LabMetric mostRecentConcentration = tube.getMostRecentConcentration();
            quant = mostRecentConcentration != null ? mostRecentConcentration.getValue() : BigDecimal.ZERO;
            labMetric = new LabMetric(quant, metricType, metricType.getLabUnit(),
                    tubePosition, runStarted);

            LabMetricDecision decision = new LabMetricDecision(
                    LabMetricDecision.Decision.REPEAT, new Date(), decidingUser, labMetric, note);
            decision.setReworkDisposition(MetricReworkDisposition.BAD_TRIP_READS);
            labMetric.setLabMetricDecision(decision);
            return Optional.of(labMetric);
        }

        // If still here, evaluate the 2 or 3 read values
        boolean tooFarApart = false;
        if (originalReadCount == 2) {
            float resultA = concList.get(0).floatValue();
            float resultB = concList.get(1).floatValue();
            quant = MathUtils.scaleTwoDecimalPlaces(new BigDecimal((resultA + resultB) / 2));
            labMetric = new LabMetric(quant, metricType, LabMetric.LabUnit.NG_PER_UL,
                    tubePosition, runStarted);
            tooFarApart = MathUtils.areTooFarApart(resultA, resultB, maxPercentDiff);
        } else if (originalReadCount == 3) {
            tooFarApart = evalReadsForDeterminingCurve(concList, maxPercentDiff);
            // Note: Read values may have a bad value outlier removed
            quant = MathUtils.scaleTwoDecimalPlaces(new BigDecimal(concList.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0.00)));
            labMetric = new LabMetric(quant, metricType, LabMetric.LabUnit.NG_PER_UL,
                    tubePosition, runStarted);
        } else {
            throw new IllegalStateException("Evaluation of greater than 3 reads not handled.");
        }

        // Failure will have removed worst of 3 values, leaving 2 present
        if (tooFarApart) {
            String note = "Closest reads: " + concList.get(0) + ", " + concList.get(1) + " not within " + (maxPercentDiff * 100) + "%.";
            LabMetricDecision decision = new LabMetricDecision(
                    LabMetricDecision.Decision.REPEAT, new Date(), decidingUser, labMetric, note);
            if (quant.floatValue() >= 40.0) {
                if (canVolumeBeAdded(tube, 0.50, 599.0)) {
                    decision.setReworkDisposition(MetricReworkDisposition.BAD_TRIP_HIGH);
                } else {
                    decision.setReworkDisposition(MetricReworkDisposition.BAD_TRIP_OVERFLOW);
                }
            } else {
                decision.setReworkDisposition(MetricReworkDisposition.BAD_TRIP_LOW);
            }
            labMetric.setLabMetricDecision(decision);
        } else {
            if (quant.compareTo(BigDecimal.valueOf(1000)) > 0) {
                LabMetricDecision decision = new LabMetricDecision(
                        LabMetricDecision.Decision.REPEAT, new Date(), decidingUser, labMetric);
                decision.setReworkDisposition(MetricReworkDisposition.TUBE_SPLIT_ADJUSTED_DOWN);
                labMetric.setLabMetricDecision(decision);
            } else if (quant.floatValue() > 100.0) {
                if (canNormWithoutOverflow(tube, quant, 50.0, 599.0)) {
                    LabMetricDecision decision = new LabMetricDecision(
                            LabMetricDecision.Decision.REPEAT, new Date(), decidingUser, labMetric);
                    decision.setReworkDisposition(MetricReworkDisposition.NORM_IN_TUBE);
                    labMetric.setLabMetricDecision(decision);
                } else if (canNormWithoutOverflow(tube, quant, 99.99, 599.0)) {
                    LabMetricDecision decision = new LabMetricDecision(
                            LabMetricDecision.Decision.REPEAT, new Date(), decidingUser, labMetric);
                    decision.setReworkDisposition(MetricReworkDisposition.NORM_ADJUSTED_DOWN);
                    labMetric.setLabMetricDecision(decision);
                } else {
                    LabMetricDecision decision = new LabMetricDecision(
                            LabMetricDecision.Decision.REPEAT, new Date(), decidingUser, labMetric);
                    decision.setReworkDisposition(MetricReworkDisposition.TUBE_SPLIT);
                    labMetric.setLabMetricDecision(decision);
                }
            } else if (quant.floatValue() < 5.0) {
                LabMetricDecision decision = new LabMetricDecision(
                        LabMetricDecision.Decision.REPEAT, new Date(), decidingUser, labMetric);
                decision.setReworkDisposition(MetricReworkDisposition.UNDILUTED);
                labMetric.setLabMetricDecision(decision);
            } else if (originalReadCount > concList.size()) {
                LabMetricDecision decision = new LabMetricDecision(
                        LabMetricDecision.Decision.PASS, new Date(), decidingUser, labMetric);
                decision.setReworkDisposition(MetricReworkDisposition.AUTO_SELECT);
                labMetric.setLabMetricDecision(decision);
            } else {
                LabMetricDecision decision = new LabMetricDecision(
                        LabMetricDecision.Decision.PASS, new Date(), decidingUser, labMetric);
                labMetric.setLabMetricDecision(decision);
            }
        }

        return Optional.of(labMetric);
    }

    /**
     * Is it possible to add a percentage volume to a tube and not overflow a maximum volume?
     */
    private static boolean canVolumeBeAdded(LabVessel tube, double pctToAdd, double maxVolume) {
        // No volume?  Don't allow addition by default
        BigDecimal currentVolume = tube.getVolume();
        if (currentVolume == null) {
            return false;
        }
        BigDecimal newVolume = currentVolume.multiply(BigDecimal.valueOf(1.0 + pctToAdd));
        if (newVolume.compareTo(BigDecimal.valueOf(maxVolume)) > 0) {
            return false;
        } else {
            return true;
        }
    }

    private static boolean canNormWithoutOverflow(LabVessel tube, BigDecimal quant, double target, double maxVolume) {
        // No volume?  Don't allow addition by default
        BigDecimal currentVolume = tube.getVolume();
        if (currentVolume == null) {
            return false;
        }

        BigDecimal ng = currentVolume.multiply(quant);
        BigDecimal newVolume = ng.divide(BigDecimal.valueOf(target).setScale(3), RoundingMode.HALF_UP);
        if (newVolume.compareTo(BigDecimal.valueOf(maxVolume)) > 0) {
            return false;
        }

        return true;
    }

    /**
     * Sort the list <br/>
     * If the diff between max/middle or middle/min is bad, drop the max or min outlier and return false.<br/>
     * If the diff between max/middle and middle/min are both bad, remove whichever of max or min is the worst.
     * Return true (too far apart)<br/>
     * If the diff between max/middle and middle/min are both good, return faLse.
     *
     * @param readsForDeterminingCurve  The reads - depends on always 3 or IndexOutOfBoundsException, passed  by reference
     * @param maxPercentageBetweenReads The maximum allowed percent difference between two reads
     */
    private static boolean evalReadsForDeterminingCurve(List<BigDecimal> readsForDeterminingCurve,
                                                        float maxPercentageBetweenReads) {
        Collections.sort(readsForDeterminingCurve);
        int minIndex = 0;
        int middleIndex = 1;
        int maxIndex = 2;
        BigDecimal minVal = readsForDeterminingCurve.get(minIndex);
        BigDecimal midVal = readsForDeterminingCurve.get(middleIndex);
        BigDecimal maxVal = readsForDeterminingCurve.get(maxIndex);
        boolean minTooFar = MathUtils.areTooFarApart(minVal.floatValue(), midVal.floatValue(), maxPercentageBetweenReads);
        boolean maxTooFar = MathUtils.areTooFarApart(midVal.floatValue(), maxVal.floatValue(), maxPercentageBetweenReads);
        LabMetricDecision.Decision decision = null;
        if (minTooFar && maxTooFar) {
            // Remove worst measurement
            if (maxVal.subtract(midVal).compareTo(midVal.subtract(minVal)) > 0) {
                readsForDeterminingCurve.remove(maxIndex);
            } else {
                readsForDeterminingCurve.remove(minIndex);
            }
            return true;  // Fail
        } else if (minTooFar && !maxTooFar) {
            // Remove the bad min value
            readsForDeterminingCurve.remove(minIndex);
            return false; // OK
        } else if (maxTooFar && !minTooFar) {
            // Remove the bad max value
            readsForDeterminingCurve.remove(maxIndex);
            return false; // OK
        }
        return false; // OK
    }

}
