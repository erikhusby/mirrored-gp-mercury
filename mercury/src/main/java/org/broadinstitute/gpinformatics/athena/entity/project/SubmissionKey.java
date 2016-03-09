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

public class SubmissionKey implements Serializable {
    private static final long serialVersionUID = 1262062294730627888L;
    private final String sampleName;
    private final String fileName;
    private final String version;
    private final String repository;
    private final String libraryDescriptor;

    public SubmissionKey(String sampleName, String fileName, String version,
                         String repository,
                         String libraryDescriptor) {
        this.sampleName = sampleName;
        this.fileName = fileName;
        this.version = version;
        this.repository = repository;
        this.libraryDescriptor = libraryDescriptor;
    }

    public SubmissionKey(String sampleName, String fileName, String version) {
        this(sampleName, fileName, version, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SubmissionKey that = OrmUtil.proxySafeCast(o, SubmissionKey.class);
        return new EqualsBuilder()
                .append(this.sampleName, that.sampleName)
                .append(this.fileName, that.fileName)
                .append(this.version, that.version)
                .append(this.repository, that.repository)
                .append(this.libraryDescriptor, that.libraryDescriptor).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.sampleName).append(this.fileName).append(this.version)
                .append(repository).append(libraryDescriptor).hashCode();
    }
}
