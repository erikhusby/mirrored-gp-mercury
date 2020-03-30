package org.broadinstitute.gpinformatics.mercury.entity.queue;

/**
 * Determines whether we want to allow the queue to have a container as the labvesselid, or if we reqquire a tube / well to be the labvesselid.
 */
public enum QueueContainerRule {
    ALLOW_ANYTHING,
    TUBES_ONLY
}
