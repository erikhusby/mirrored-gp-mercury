package org.broadinstitute.gpinformatics.athena.entity.orders;

/**
 * This adds the abandon track to the progress counter object
 *
 * @author hrafal
 */
public class ProductOrderCompletionStatus {
    private final int total;
    private final int percentComplete;
    private final int percentAbandoned;

    // Completed and Total known up front
    public ProductOrderCompletionStatus(int abandoned, int completed, int total) {
        this.total = total;
        if (total != 0) {
            percentComplete = (completed * 100) / total;
            percentAbandoned = (abandoned * 100) / total;
        } else {
            percentComplete = 0;
            percentAbandoned = 0;
        }
    }

    public int getPercentComplete() {
        return percentComplete;
    }

    public int getTotal() {
        return total;
    }

    public int getPercentAbandoned() {
        return percentAbandoned;
    }
}
