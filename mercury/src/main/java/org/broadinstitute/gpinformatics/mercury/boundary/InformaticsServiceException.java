package org.broadinstitute.gpinformatics.mercury.boundary;

/**
 * @author Scott Matthews
 *         Date: 5/8/12
 *         Time: 11:56 AM
 */
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
