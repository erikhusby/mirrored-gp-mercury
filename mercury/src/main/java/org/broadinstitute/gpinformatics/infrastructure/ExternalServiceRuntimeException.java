/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2019 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure;

public class ExternalServiceRuntimeException extends RuntimeException {

    public ExternalServiceRuntimeException(final String s) {
        super(s);
    }

    public ExternalServiceRuntimeException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    public ExternalServiceRuntimeException(final Throwable throwable) {
        super(throwable);
    }
}
