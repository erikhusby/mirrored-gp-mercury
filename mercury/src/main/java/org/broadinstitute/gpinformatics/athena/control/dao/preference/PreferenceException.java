/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2009 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */
package org.broadinstitute.gpinformatics.athena.control.dao.preference;

/**
 * Preference exception.
 * 
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
public class PreferenceException extends Exception {
    private static final long serialVersionUID = 20091005L;

    public PreferenceException() {
        super();
    }

    public PreferenceException(String message, Throwable e) {
        super(message, e);
    }

    public PreferenceException(String message) {
        super(message);
    }

    public PreferenceException(Throwable cause) {
        super(cause);
    }
}
