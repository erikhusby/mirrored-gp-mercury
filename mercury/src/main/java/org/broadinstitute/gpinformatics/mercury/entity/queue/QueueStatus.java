package org.broadinstitute.gpinformatics.mercury.entity.queue;

public enum QueueStatus {
    Active,
    Completed,
    Excluded,
    Repeat;

    public String getName() {
        return name();
    }
}
