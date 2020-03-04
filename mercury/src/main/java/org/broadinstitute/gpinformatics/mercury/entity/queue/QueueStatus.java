package org.broadinstitute.gpinformatics.mercury.entity.queue;

/**
 * The statuses for QueueEntities.
 */
public enum QueueStatus {
    // This is the one of two statuses which will allow QueueEntities to show as currently in the queue.  This means not yet run.
    ACTIVE("Active"),
    // Completed successfully and automatically removed from the queue at the end of its process.
    COMPLETED("Completed"),
    // Manually removed from the queue
    EXCLUDED("Excluded"),
    // This is the one of two statuses which will allow QueueEntities to show as currently in the queue.  This means run at least once, but initial one failed.
    REPEAT("Repeat");

    private String displayName;

    QueueStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isStillInQueue() {
        return this == ACTIVE || this == REPEAT;
    }
}
