package org.broadinstitute.gpinformatics.athena.entity.common;

/**
 * Common interface for status fields in entities, typically implemented
 * using an enum.
 */
public interface StatusType {
    /**
     * @return the user visible name for the status
     */
    String getDisplayName();
}
