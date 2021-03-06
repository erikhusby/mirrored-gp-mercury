package org.broadinstitute.gpinformatics.mercury.entity.queue;

/**
 * Queue Priority Types are utilized by the EnqueueOverride classes to jump new queuegroupings higher into the queue
 * based on Queue specific logic defined in their subclasses.
 *
 * NOTE:  This isn't just for Pico, all Queues can implement their own priority types by adding
 * them here.
 *
 * Because each defines the items themselves, we don't need to worry about Types which aren't used by a particular queue.
 */
public enum QueuePriority {
    STANDARD("Standard"),
    EXOME_EXPRESS("Exome Express"),
    CLIA("Clinical"),
    ALTERED("Expedited");

    private final String displayName;

    QueuePriority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * This defines what priorities should be skipped in the priority checks.  Should define here why each is in place.
     *
     * ALTERED:  This should be skipped for every process.  The altered priority is set when someone changes the
     *           placement of a QueueGrouping within the queue via the UI.
     *
     * @return  true if the priority check should be skipped.   false if the priority check should be done.
     */
    public boolean shouldSkipPriorityCheck() {
        return this == ALTERED;
    }
}
