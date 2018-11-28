/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.presentation.cache;

public class SessionCacheException extends RuntimeException {
    private static final long serialVersionUID = -108797609166010007L;

    public SessionCacheException(String message, Throwable cause) {
        super(message, cause);
    }

    public SessionCacheException(String message) {
        super(message);
    }
}
