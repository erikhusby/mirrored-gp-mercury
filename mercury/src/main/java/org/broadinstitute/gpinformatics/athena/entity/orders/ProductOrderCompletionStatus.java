package org.broadinstitute.gpinformatics.athena.entity.orders;

/**
 * This adds the abandon track to the progress counter object
 *
 * @author hrafal
 */
public class ProductOrderCompletionStatus {
    private final int total;
    private final int abandoned;
    private final int completed;

    // Completed and Total known up front
    public ProductOrderCompletionStatus(int abandoned, int completed, int total) {
        this.abandoned = abandoned;
        this.completed = completed;
        this.total = total;
    }

    public int getPercentComplete() {
        // protect against divide by 0 error
        if (total == 0) {
            return 0;
        }

        return (completed * 100)/total;
    }

    public int getTotal() {
        return total;
    }

    public int getPercentAbandoned() {
        // protect against divide by 0 error
        if (total == 0) {
            return 0;
        }

        return (abandoned * 100)/getTotal();
    }
}
