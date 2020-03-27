package org.broadinstitute.gpinformatics.mercury.boundary.queue.validation;

/**
 * Denotes the result of a Queue validation.  Some might come back as unknown if they are not able to be determined.
 */
public enum ValidationResult {
    PASS,
    FAIL,
    UNKNOWN
}
