package org.broadinstitute.gpinformatics.mercury.entity.queue;

/**
 * The statuses for QueueEntities.
 */
public enum QueueStatus {
    // This is the one of two statuses which will allow QueueEntities to show as currently in the queue.  This means not yet run.
    Active,
    // Completed successfully and automatically removed from the queue at the end of its process.
    Completed,
    // Manually removed from the queue
    Excluded,
    // This is the one of two statuses which will allow QueueEntities to show as currently in the queue.  This means run at least once, but initial one failed.
    Repeat;

    public String getName() {
        return name();
    }

    public boolean isStillInQueue() {
        return this == Active || this == Repeat;
    }
}
