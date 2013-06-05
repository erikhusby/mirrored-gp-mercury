package org.broadinstitute.gpinformatics.infrastructure.jpa;

/**
 * This exception is for cases where a business key is used that is not valid for some reason. Typically, this will
 * be because a find results in no value.
 */
public class BadBusinessKeyException extends Exception {
    public BadBusinessKeyException(String message) {
        super(message);
    }

    public BadBusinessKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
