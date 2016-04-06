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

package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassFileType;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import java.io.Serializable;

public class SubmissionTuple implements Serializable {
    private static final long serialVersionUID = 1262062294730627888L;
    private final String sampleName;
    private final BassFileType fileType;
    private final String version;

    public SubmissionTuple(String sampleName, BassFileType fileType, String version) {
        this.sampleName = sampleName;
        this.fileType = fileType;
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SubmissionTuple that = OrmUtil.proxySafeCast(o, SubmissionTuple.class);
        return new EqualsBuilder()
                .append(this.sampleName, that.sampleName)
                .append(this.fileType, that.fileType)
                .append(this.version, that.version).isEquals();
    }

    @Override
    public String toString() {
        return String.format("{sampleName = %s; fileType = %s; version = %s}",sampleName, fileType, version);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.sampleName).append(this.fileType).append(this.version).hashCode();
    }
}
