package org.broadinstitute.gpinformatics.athena.boundary.util;

/**
 * This object keeps track of the total number of items and the count of progress on those items
 * so that a percent complete can be calculated
 *
 * @author hrafal
 */
public class ProgressCounter {
    private int total;

    private int completed;

    // Incrementing totals as we go
    public ProgressCounter() {
    }

    // Total is fixed for duration and only increment completed
    public ProgressCounter(int total) {
        this.total = total;
    }

    // Completed and Total known up front
    public ProgressCounter(int completed, int total) {
        this.completed = completed;
        this.total = total;
    }

    public int getPercentComplete() {
        return (completed * 100)/total;
    }

    public void incrementCompletedAndTotal() {
        incrementCompleted();
        incrementNotCompleted();
    }

    public void incrementCompleted() {
        completed++;
    }

    public void incrementNotCompleted() {
        total++;
    }
}
