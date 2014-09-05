package org.broadinstitute.gpinformatics.mercury.boundary.zims;

/**
 * Exception thrown when the pipeline API (and others) detects
 * a problem querying data from BSP
 */
public class BSPLookupException extends RuntimeException {

    public BSPLookupException(String message) {
        super(message);
    }

    public BSPLookupException(String message,Throwable cause) {
        super(message,cause);
    }

}
