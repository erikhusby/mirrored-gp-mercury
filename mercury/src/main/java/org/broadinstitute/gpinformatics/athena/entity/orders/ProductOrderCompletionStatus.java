package org.broadinstitute.gpinformatics.athena.entity.orders;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds sample status counts for a single PDO and calculates percentages of total for those counts. Also determines
 * good display values for the calculated percentages to avoid confusion when rounding occurs.
 * <p>
 * The basic rule is to prepend "&gt;" when the value was rounded down and "&lt;" when the value was rounded up.
 * However, there are a few special cases:
 * <ul>
 *     <li>If a value is rounded down to zero, display "&lt;1" instead of "&gt;0"</li>
 *     <li>If a value is rounded up to 100, display "&gt;99" instead of "&lt;100"</li>
 *     <li>If a pair of values round up display the smaller one as "&lt;[rounded value]" and the larger one as
 *         "&gt;[rounded value - 1]"
 *     </li>
 * </ul>
 * The goal of these rules is to make the sum of the displayed numbers be 100 whenever possible and to include "&lt;"
 * and "&gt;" hints to clarify where and in which direction the numbers have been rounded.
 */
public class ProductOrderCompletionStatus {
    private final int abandoned;
    private final int completed;
    private final int inProgress;
    private final int total;
    private double percentAbandoned;
    private double percentCompleted;
    private double percentInProgress;
    private String percentAbandonedDisplay;
    private String percentCompletedDisplay;
    private String percentInProgressDisplay;

    /**
     * Create a new status object given the known sample/status counts on a PDO.
     *
     * @param abandoned    the number of abandoned samples
     * @param completed    the number of completed (billed) samples
     * @param total        the total number of samples
     */
    public ProductOrderCompletionStatus(int abandoned, int completed, int total) {
        if (abandoned < 0 || completed < 0 || total < 0) {
            throw new IllegalStateException(
                    String.format("All quantities must be >= 0: %d %d %d", abandoned, completed, total));
        }
        if (abandoned > total) {
            throw new IllegalStateException("Abandoned cannot be greater than total");
        }
        if (completed > total) {
            throw new IllegalStateException("Completed cannot be greater than total");
        }
        if (abandoned + completed > total) {
            throw new IllegalStateException("Sum of abandoned and completed cannot be greater than total");
        }

        this.abandoned = abandoned;
        this.completed = completed;
        this.total = total;

        if (total == 0) {
            inProgress = 0;
        } else {
            inProgress = total - (completed + abandoned);
        }

        percentAbandoned = total == 0 ? 0 : (double) abandoned / total;
        percentCompleted = total == 0 ? 0 : (double) completed / total;
        percentInProgress = total == 0 ? 0 : (double) inProgress / total;

        computeDisplayValues();
    }

    /**
     * It is possible that the sum of the rounded percentages is not 100. The range of cumulative rounding error is
     * [-1, floor(n/2)] where n is the number of components making up 100% (3 in this case: abandoned, completed, and in
     * progress). The lower bound occurs when all of the calculated percentages round down, in which case the sum of the
     * percentage removed due to rounding is -1. The upper bound occurs when pairs of percentages each have 0.5%, in
     * which case both will round up resulting in +1 per pair. In all other cases, the cumulative rounding error is 0.
     *
     * To avoid confusion when people mentally sum the percentages notice they don't add up quite right, we want to
     * adjust the displayed percentages to indicate where rounding occurred.
     */
    private void computeDisplayValues() {
        long percentAbandonedRounded = Math.round(percentAbandoned * 100);
        long percentCompletedRounded = Math.round(percentCompleted * 100);
        long percentInProgressRounded = Math.round(percentInProgress * 100);

        /*
         * In the case where a pair of calculated percentages each have 0.5% and would both round up, the decision about
         * what value to display cannot be made in isolation; one value needs to be chosen to "round down" instead.
         * Therefore, we need to gather this information to provide to the logic for adjusting the value for display.
         */
        List<Double> halfPercents = new ArrayList<>();
        if (Math.abs(percentAbandoned * 100 - percentAbandonedRounded) == .5) {
            halfPercents.add(percentAbandoned);
        }
        if (Math.abs(percentCompleted * 100 - percentCompletedRounded) == .5) {
            halfPercents.add(percentCompleted);
        }
        if (Math.abs(percentInProgress * 100 - percentInProgressRounded) == .5) {
            halfPercents.add(percentInProgress);
        }
        Collections.sort(halfPercents);

        percentAbandonedDisplay = getAdjustedDisplayValue(percentAbandoned, percentAbandonedRounded, halfPercents);
        percentCompletedDisplay = getAdjustedDisplayValue(percentCompleted, percentCompletedRounded, halfPercents);
        percentInProgressDisplay = getAdjustedDisplayValue(percentInProgress, percentInProgressRounded,
                halfPercents);
    }

    /**
     * Adjust the displayed percentage so that it makes sense to read in the context of the other percentages in cases
     * where the numbers have been rounded.
     *
     * @see {@link ProductOrderCompletionStatus} javadoc for details of the rules
     *
     * @param value           the actual percentage value, range [0, 1]
     * @param roundedValue    the rounded percentage value, range [0, 100]
     * @param halfPercents    a sorted list of the actual percentages that have a fractional part of .5
     * @return an appropriate display string for the rounded percentage
     */
    @Nonnull
    private String getAdjustedDisplayValue(double value, long roundedValue, List<Double> halfPercents) {
        if (value * 100 < roundedValue) { // rounded up
            if (roundedValue == 100) { // special case to say ">99" instead of "<100"
                return ">99";
            } else if (roundedValue - value * 100 == .5) { // special case for splitting .5%
                if (halfPercents.size() % 2 == 0 &&
                    halfPercents.indexOf(value) != -1 &&
                    halfPercents.indexOf(value) >= halfPercents.size() / 2) {
                    return ">" + String.valueOf(roundedValue - 1);
                } else {
                    return "<" + String.valueOf(roundedValue);
                }
            } else {
                return "<" + String.valueOf(roundedValue);
            }
        } else if (value * 100 > roundedValue) { // rounded down
            if (roundedValue == 0) { // special case to say "<1" instead of ">0"
                return "<1";
            } else {
                return ">" + String.valueOf(roundedValue);
            }
        }
        return String.valueOf(roundedValue);
    }

    /**
     * @return the number of abandoned samples
     */
    public int getNumberAbandoned() {
        return abandoned;
    }

    /**
     * @return the number of samples in-progress (i.e., not complete nor abandoned)
     */
    public int getNumberInProgress() {
        return inProgress;
    }

    /**
     * @return the number of completed (billed) samples
     */
    public int getNumberCompleted() {
        return completed;
    }

    /**
     * @return the total number of samples
     */
    public int getTotal() {
        return total;
    }

    /**
     * @return the percentage of the total number of samples that are in progress (i.e., not complete nor abandoned)
     */
    public double getPercentInProgress() {
        return percentInProgress;
    }

    /**
     * @return the percentage of the total number of samples that are complete (billed)
     */
    public double getPercentCompleted() {
        return percentCompleted;
    }

    /**
     * @return the percentage of the total number of samples that have been abandoned
     */
    public double getPercentAbandoned() {
        return percentAbandoned;
    }

    /**
     * @return a String for displaying the percent abandoned
     */
    public String getPercentAbandonedDisplay() {
        return percentAbandonedDisplay;
    }

    /**
     * @return a String for displaying the percent completed
     */
    public String getPercentCompletedDisplay() {
        return percentCompletedDisplay;
    }

    /**
     * @return a String for displaying the percent in progress
     */
    public String getPercentInProgressDisplay() {
        return percentInProgressDisplay;
    }
}
