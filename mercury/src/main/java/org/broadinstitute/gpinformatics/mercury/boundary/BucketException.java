package org.broadinstitute.gpinformatics.mercury.boundary;

import javax.ejb.ApplicationException;

/**
 * Extension of InformaticsServiceException to allow developers to distinguish between this and either
 * InformaticsServiceExceptions or RuntimeExceptions
 */
@ApplicationException(rollback = true)
public class BucketException extends InformaticsServiceException {
    public BucketException(String s) {
        super(s);
    }

    public BucketException(String s, Throwable throwableIn) {
        super(s, throwableIn);
    }

    public BucketException(Throwable throwableIn) {
        super(throwableIn);
    }
}
