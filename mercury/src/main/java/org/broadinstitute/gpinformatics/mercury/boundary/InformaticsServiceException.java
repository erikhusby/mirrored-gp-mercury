package org.broadinstitute.gpinformatics.mercury.boundary;

import javax.annotation.Nonnull;
import javax.ejb.ApplicationException;
import java.util.IllegalFormatException;

/**
 * InformaticsServiceException is intended to be thrown from our Service/Boundary layer (EJBs) and is an extension of
 * RuntimeException that is defined as an Application Exception.  This approach:
 * <ul>
 * <li>Allows us to have a runtime exception that can be distinguished from other Runtime Exceptions if it needs
 * to be handled</li>
 * <li>Have a runtime exception thrown from the EJB layer that will not be Wrapped in an EJBException (because
 * of the ApplicationException annotation</li>
 * </ul>
 * <p/>
 * InformaticsServiceException is marked to rollback any transaction it is thrown from.  If that is not desirable
 * then you must find a different approach for your EJB Method such as have a non rollback exception object defined
 * that has the ApplicationException defined as rollback = false.  Make sure you have a good reason for doing so if
 * you do this.
 */
@ApplicationException(rollback = true)
public class InformaticsServiceException extends RuntimeException {

    /**
     * Exception with a template String and variable number of arguments.
     */
    public InformaticsServiceException(@Nonnull String template, Object... args) {
        super(safeStringFormat(template, args));
    }

    public InformaticsServiceException(Throwable cause, @Nonnull String template, Object... args) {
        super(safeStringFormat(template, args), cause);
    }

    public InformaticsServiceException(Throwable throwableIn) {
        super(throwableIn);
    }

    /**
     * "Safe" wrapper around String.format that will drop back to returning the unsubstituted template
     * in the event that String.format would throw an IllegalFormatException due to more template parameters
     * than substitution arguments.
     */
    private static String safeStringFormat(String template, Object... args) {
        try {
            return String.format(template, args);
        } catch (IllegalFormatException e) {
            return template;
        }
    }
}
