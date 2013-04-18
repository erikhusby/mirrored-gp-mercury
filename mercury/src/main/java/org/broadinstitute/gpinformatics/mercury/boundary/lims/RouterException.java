package org.broadinstitute.gpinformatics.mercury.boundary.lims;

/**
 *
 * Represents an error that occurred while determining which system of record to route LIMS requests
 *
 * @author Scott Matthews
 *         Date: 4/17/13
 *         Time: 10:07 PM
 */
public class RouterException extends RuntimeException {

    public RouterException(String s) {
        super(s);
    }

    public RouterException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public RouterException(Throwable throwable) {
        super(throwable);
    }
}
