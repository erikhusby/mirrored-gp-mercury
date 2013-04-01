/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.entity.rapsheet;

public enum ReworkReason {
    MACHINE_ERROR("Machine Error"),
    UNKNOWN_ERROR("Unknown Error");

    private String value;

    ReworkReason(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
