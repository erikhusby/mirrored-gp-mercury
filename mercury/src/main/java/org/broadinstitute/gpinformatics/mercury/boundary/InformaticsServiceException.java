package org.broadinstitute.gpinformatics.mercury.boundary;

import javax.ejb.ApplicationException;

/**
 * InformaticsServiceException is intended to be thrown from our Service/Boundary layer (EJBs) and is an extension of
 * RuntimeException that is defined as an Application Exception.  This approach:
 * <ul>
 *     <li>Allows us to have a runtime exception that can be distinguished from other Runtime Exceptions if it needs
 *     to be handled</li>
 *     <li>Have a runtime exception thrown from the EJB layer that will not be Wrapped in an EJBException (because
 *     of the ApplicationException annotation</li>
 * </ul>
 */
@ApplicationException(rollback = true)
public class InformaticsServiceException extends RuntimeException {

    public InformaticsServiceException ( String s ) {
        super(s);
    }

    public InformaticsServiceException ( String s, Throwable throwableIn ) {
        super(s, throwableIn);
    }

    public InformaticsServiceException ( Throwable throwableIn ) {
        super(throwableIn);
    }
}
