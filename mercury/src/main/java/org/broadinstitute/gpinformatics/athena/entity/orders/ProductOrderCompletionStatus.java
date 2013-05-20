package org.broadinstitute.gpinformatics.athena.entity.orders;

/**
 * This adds abandonment tracking to the progress counter object.
 */
public class ProductOrderCompletionStatus {
    private final int total;

    private final int percentCompleted;
    private final int percentInProgress;
    private final int percentAbandoned;

    // Completed and Total are known up front.
    public ProductOrderCompletionStatus(int abandoned, int completed, int total) {
        this.total = total;

        if (total == 0) {
            percentInProgress = 0;
            percentCompleted = 0;
            percentAbandoned = 0;
        } else {
            percentInProgress = ((total - (completed + abandoned)) * 100) / total;
            int calculatedCompleted = (completed * 100) / total;
            percentAbandoned = (abandoned * 100) / total;

            // Sanity check that if no samples are completed then percentCompleted is zero.
            if (completed == 0) {
                percentCompleted = 0;
            } else {
                int lostPercent = 100 - (calculatedCompleted + percentInProgress + percentAbandoned);

                // Adjust the updated percentage to have anything lost by rounding pushed into the complete.
                int updatedPercentComplete = calculatedCompleted + lostPercent;

                // Don't mind rounding up percent complete EXCEPT when the total is going to 100 and we are not really done.
                if ((updatedPercentComplete >= 100) && ((abandoned + completed) != total)) {
                    updatedPercentComplete = 99;
                }

                percentCompleted = updatedPercentComplete;
            }
        }
    }

    public int getPercentInProgress() {
        return percentInProgress;
    }

    public int getPercentCompleted() {
        return percentCompleted;
    }

    public int getTotal() {
        return total;
    }

    public int getPercentAbandoned() {
        return percentAbandoned;
    }
}
