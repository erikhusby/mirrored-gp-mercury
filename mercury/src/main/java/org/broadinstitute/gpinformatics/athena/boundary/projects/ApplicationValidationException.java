package org.broadinstitute.gpinformatics.athena.boundary.projects;

import javax.ejb.ApplicationException;

/**
 * @author breilly
 */
@ApplicationException(rollback = true)
public class ApplicationValidationException extends Exception {

    public ApplicationValidationException(String s) {
        super(s);
    }

    public ApplicationValidationException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
