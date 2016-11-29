package org.broadinstitute.gpinformatics.infrastructure.sap;

import javax.ejb.ApplicationException;

/**
 * TODO scottmat fill in javadoc!!!
 */
@ApplicationException(rollback = true)
public class SAPInterfaceException extends Exception {

    public SAPInterfaceException(String message) {
        super(message);
    }

    public SAPInterfaceException(String message, Throwable cause) {
        super(message, cause);
    }
}
