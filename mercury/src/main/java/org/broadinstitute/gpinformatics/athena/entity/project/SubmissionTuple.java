/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import java.io.Serializable;

public class SubmissionTuple implements Serializable {
    private final String sampleName;
    private final String fileName;
    private final String version;

    public SubmissionTuple(String sampleName, String fileName, String version) {
        this.sampleName = sampleName;
        this.fileName = fileName;
        this.version = version;
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
                .append(this.fileName, that.fileName)
                .append(this.version, that.version).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.sampleName).append(this.fileName).append(this.version).hashCode();
    }
}
