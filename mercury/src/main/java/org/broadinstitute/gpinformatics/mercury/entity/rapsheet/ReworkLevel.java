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

public enum ReworkLevel {
    ONE_SAMPLE_HOLD_REST_BATCH("Type 1", "Rework one sample and hold up the rest of the batch."),
    ONE_SAMPLE_RELEASE_REST_BATCH("Type 2", "Rework one sample let the rest of the batch proceed "),
    ENTIRE_BATCH("Type 3", "Rework all samples in the batch.");

    private String value;
    private String description;

    private ReworkLevel(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}

