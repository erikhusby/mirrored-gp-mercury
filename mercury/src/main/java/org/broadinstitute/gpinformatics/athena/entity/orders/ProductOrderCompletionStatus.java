package org.broadinstitute.gpinformatics.athena.entity.orders;

/**
 * This adds abandonment tracking to the progress counter object.
 *
 * @author hrafal
 */
public class ProductOrderCompletionStatus {
    private final int total;
    private final int percentCompleted;
    private final int percentInProgress;
    private final int percentAbandoned;

    // Completed and Total known up front
    public ProductOrderCompletionStatus(int abandoned, int completed, int total) {
        this.total = total;
        if (total == 0) {
            percentInProgress = 0;
            percentCompleted = 0;
            percentAbandoned = 0;
        } else {
            percentInProgress = ((total - (completed + abandoned)) * 100) / total;
            percentCompleted = (completed * 100) / total;
            percentAbandoned = (abandoned * 100) / total;
        }
    }

    public int getPercentInProgress() {
        return percentInProgress;
    }

    public int getPercentCompleted() {
        int lostPercent = 100 - (percentCompleted + percentInProgress + percentAbandoned);
        return percentCompleted + lostPercent;
    }

    public int getTotal() {
        return total;
    }

    public int getPercentAbandoned() {
        return percentAbandoned;
    }
}
