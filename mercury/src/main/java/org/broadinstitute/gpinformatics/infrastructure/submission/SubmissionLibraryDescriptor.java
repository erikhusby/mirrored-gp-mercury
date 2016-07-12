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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlType(name = "submissiondatatype")
public class SubmissionLibraryDescriptor implements Serializable {
    private static final long serialVersionUID = 2140289148823382712L;
    public static final String WHOLE_GENOME_NAME = "Whole Genome";
    public static final String WHOLE_GENOME_DESCRIPTION = "Human Whole Genome";

    private String name;

    private String description;

    public SubmissionLibraryDescriptor(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public SubmissionLibraryDescriptor() {
    }

    @JsonProperty
    @XmlElement
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    @JsonProperty
    @XmlElement(required = false)
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getName()).append(getDescription()).hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SubmissionLibraryDescriptor)){
            return false;
        }
        SubmissionLibraryDescriptor castOther = (SubmissionLibraryDescriptor) other;
        if (this == castOther) {
            return true;
        }

        return new EqualsBuilder().append(getName(), castOther.getName())
                .append(getDescription(), castOther.getDescription()).isEquals();
    }
}
