package org.broadinstitute.gpinformatics.athena.entity.orders;

/**
 * This adds abandonment tracking to the progress counter object.
 *
 * @author hrafal
 */
public class ProductOrderCompletionStatus {
    private final int total;
    private final int percentCompleted;
    private final int percentAbandoned;

    // Completed and Total known up front
    public ProductOrderCompletionStatus(int abandoned, int completed, int total) {
        this.total = total;
        if (total != 0) {
            percentCompleted = (completed * 100) / total;
            percentAbandoned = (abandoned * 100) / total;
        } else {
            percentCompleted = 0;
            percentAbandoned = 0;
        }
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
