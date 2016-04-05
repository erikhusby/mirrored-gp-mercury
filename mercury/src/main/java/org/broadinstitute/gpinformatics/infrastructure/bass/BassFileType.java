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

package org.broadinstitute.gpinformatics.infrastructure.bass;

public enum BassFileType {
    BAM("bam"),
    PICARD("picard"),
    READ_GROUP_BAM("read_group_bam"),
    ALL("all");
    private String bassValue;

    BassFileType(String bassValue) {
        this.bassValue = bassValue;
    }

    public String getBassValue() {
        return bassValue;
    }

    public static BassFileType byBassValue(String fileType) {
        for (BassFileType type : BassFileType.values()) {
            if (fileType.equals(type.getBassValue())) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant for " + fileType);
    }
}
