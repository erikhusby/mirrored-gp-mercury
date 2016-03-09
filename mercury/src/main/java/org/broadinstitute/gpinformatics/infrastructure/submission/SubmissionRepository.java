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

package org.broadinstitute.gpinformatics.infrastructure.submission;

import com.google.common.base.Predicate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlType(name = "site")
public class SubmissionRepository implements Serializable {
    private static final long serialVersionUID = -3831990214996752938L;
    public static final String DEFAULT_REPOSITORY_NAME = "NCBI_PROTECTED";

    private String name;
    private String description;
    @Transient
    private boolean active;

    public SubmissionRepository(String name, String description, Boolean active) {
        this.name = name;
        this.description = description;
        this.active = active;
    }

    public SubmissionRepository(String name, String description) {
        this(name, description, true);
    }

    public SubmissionRepository() {
    }

    public String getName() {
        return name;
    }

    @XmlElement
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    @XmlElement
    public void setDescription(String description) {
        this.description = description;
    }

    @XmlElement
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public static Predicate<SubmissionRepository> activeRepositoryPredicate = new Predicate<SubmissionRepository>() {
        @Override
        public boolean apply(SubmissionRepository submissionRepository) {
            return submissionRepository.isActive();
        }
    };

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getName()).append(getDescription()).append(isActive()).hashCode();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        if (other == null || !OrmUtil.proxySafeIsInstance(other, SubmissionRepository.class)) {
            return false;
        }

        SubmissionRepository castOther = OrmUtil.proxySafeCast(other, SubmissionRepository.class);
        return new EqualsBuilder().append(getName(), castOther.getName())
                .append(getDescription(), castOther.getDescription())
                .append(isActive(), castOther.isActive()).isEquals();
    }
}
