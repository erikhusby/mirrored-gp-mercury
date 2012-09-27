package org.broadinstitute.gpinformatics.mercury.entity.project;

public enum ProjectPriority {
    /**
     * Got some insanely high priority samples
     * that came from an outbreak?  Turn this
     * bit on.
     * @return
     */
    OUTBREAK,
    /**
     * Higher than normal
     */
    HIGH,
    /**
     * Meh, whatever
     */
    NORMAL
}
