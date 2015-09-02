package org.broadinstitute.gpinformatics.athena.entity.orders;

/**
 * Holds sample status counts for a single PDO and calculates percentages of total for those counts.
 */
public class ProductOrderCompletionStatus {
    private final int abandoned;
    private final int completed;
    private final int inProgress;
    private final int total;

    /**
     * Create a new status object given the known sample/status counts on a PDO.
     *
     * @param abandoned    the number of abandoned samples
     * @param completed    the number of completed (billed) samples
     * @param total        the total number of samples
     */
    public ProductOrderCompletionStatus(int abandoned, int completed, int total) {
        this.abandoned = abandoned;
        this.completed = completed;
        this.total = total;

        if (total == 0) {
            inProgress = 0;
        } else {
            inProgress = total - (completed + abandoned);
        }
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
     * @return the percentage of the total number of samples that are in progress (i.e., not complete nor abandoned)
     */
    public double getPercentInProgress() {
        return total == 0 ? 0 : (double) inProgress / total;
    }

    /**
     * @return the percentage of the total number of samples that are complete (billed)
     */
    public double getPercentCompleted() {
        return total == 0 ? 0 : (double) completed / total;
    }

    /**
     * @return the total number of samples
     */
    public int getTotal() {
        return total;
    }

    /**
     * @return the percentage of the total number of samples that have been abandoned
     */
    public double getPercentAbandoned() {
        return total == 0 ? 0 : (double) abandoned / total;
    }
}
