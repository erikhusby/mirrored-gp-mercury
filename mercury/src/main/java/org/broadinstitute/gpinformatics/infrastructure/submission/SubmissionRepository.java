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
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;

// setting the access order to alphabetical helps the tests pass more reliably.
@JsonPropertyOrder(alphabetic = true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SubmissionRepository implements Serializable {
    private static final long serialVersionUID = -3831990214996752938L;
    public static final String DEFAULT_REPOSITORY_NAME = "NCBI_PROTECTED";
    public static final String DEFAULT_REPOSITORY_DESCRIPTOR = "NCBI Controlled Access (dbGaP) submissions";

    @JsonProperty
    private String name;
    @JsonProperty
    private String description;
    @JsonProperty
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

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

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
