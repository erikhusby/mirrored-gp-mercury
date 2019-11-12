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

package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.StringUtils;

/**
 * Messages accumulated in a billing session. Allows an entire session handle failures without failing the whole
 * session.
 */
public class BillingMessage {
    private Throwable throwable;
    private String validationError;

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public String getValidationError() {
        return validationError;
    }

    public void setValidationError(String validationError) {
        this.validationError = validationError;
    }

    boolean hasException() {
        return null != throwable;
    }

    public boolean hasValidationError() {
        return StringUtils.isNotEmpty(validationError);
    }

    public static boolean hasErrors(BillingMessage billingMessage) {
        return billingMessage.hasException() || billingMessage.hasValidationError();
    }
}
