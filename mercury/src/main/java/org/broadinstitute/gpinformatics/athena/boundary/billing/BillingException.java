package org.broadinstitute.gpinformatics.athena.boundary.billing;

import javax.ejb.ApplicationException;

/**
 * Represents an error that occurred during a billing session
 */
@ApplicationException(rollback = true)
public class BillingException extends RuntimeException {
    private static final long serialVersionUID = 8086517907405100511L;

    public BillingException(String message) {
        super(message);
    }

    public BillingException(String message, Throwable cause) {
        super(message, cause);
    }

}
