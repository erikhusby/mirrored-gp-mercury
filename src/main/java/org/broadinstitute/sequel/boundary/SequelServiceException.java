package org.broadinstitute.sequel.boundary;

/**
 * @author Scott Matthews
 *         Date: 5/8/12
 *         Time: 11:56 AM
 */
public class SequelServiceException extends RuntimeException {

    public SequelServiceException(String s) {
        super(s);
    }

    public SequelServiceException(String s, Throwable throwableIn) {
        super(s, throwableIn);
    }

    public SequelServiceException(Throwable throwableIn) {
        super(throwableIn);
    }
}
