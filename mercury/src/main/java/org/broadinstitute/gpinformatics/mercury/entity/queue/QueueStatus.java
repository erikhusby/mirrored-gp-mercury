package org.broadinstitute.gpinformatics.mercury.entity.queue;

public enum QueueStatus {
    Active,
    Completed,
    Excluded;

    public String getName() {
        return name();
    }
}
