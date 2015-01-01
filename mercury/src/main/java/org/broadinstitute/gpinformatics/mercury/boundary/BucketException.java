package org.broadinstitute.gpinformatics.mercury.boundary;

import javax.ejb.ApplicationException;

/**
 * Bucket Exception is intended to be used to represent an error when interacting with the bucket.  This may include:
 * <ul>
 *     <li>Creating a bucket</li>
 *     <li>Creating/Manipulating bucket entries</li>
 * </ul>
 * BucketException is an extension of InformaticsServiceException to allow developers to distinguish between this and
 * either InformaticsServiceExceptions or RuntimeExceptions
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
