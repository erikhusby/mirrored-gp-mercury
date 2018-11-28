/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2017 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.submission;

public enum FileType {
    BAM("bam"),
    PICARD("picard"),
    READ_GROUP_BAM("read_group_bam"),
    ALL("all");
    private String bassValue;

    FileType(String bassValue) {
        this.bassValue = bassValue;
    }

    public String getBassValue() {
        return bassValue;
    }

    public static FileType byBassValue(String fileType) {
        for (FileType type : FileType.values()) {
            if (fileType.equals(type.getBassValue())) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant for " + fileType);
    }
}
