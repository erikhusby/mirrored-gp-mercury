package org.broadinstitute.gpinformatics.mercury.entity.queue;

/**
 * Defines the origin of wh
 */
public enum QueueOrigin {
    EXTRACTION("Extraction"),
    RECEIVING("Receiving"),
    OTHER("Other");

    private final String displayName;

    QueueOrigin(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
