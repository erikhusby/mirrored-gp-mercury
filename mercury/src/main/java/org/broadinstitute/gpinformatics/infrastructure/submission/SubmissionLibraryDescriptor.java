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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

// setting the access order to alphabetical helps the tests pass more reliably.
@JsonPropertyOrder(alphabetic = true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SubmissionLibraryDescriptor implements Serializable {
    private static final long serialVersionUID = 2140289148823382712L;
    public static final String WHOLE_GENOME_DESCRIPTION = "Human Whole Genome";


    public static final SubmissionLibraryDescriptor WHOLE_EXOME =
        new SubmissionLibraryDescriptor("Whole Exome", "Whole Exome Sequencing");
    public static final SubmissionLibraryDescriptor WHOLE_GENOME =
        new SubmissionLibraryDescriptor("Whole Genome", "Human Whole Genome");
    public static final SubmissionLibraryDescriptor RNA_SEQ =
        new SubmissionLibraryDescriptor("RNA Seq", "RNA Sequencing");


    @JsonProperty
    private String name;
    @JsonProperty
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

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("{\'submissiondatatype\': "
                             + "[{\'name\': \'%s\', \'description\': \'%s\'}]}", name, description);
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

    /**
     * Attempt to align library name/datatype with the names that Mercury and Epsilon 9 uses. This is especially
     * important when comparing Tuples.
     */
    public static String getNormalizedLibraryName(String dataType) {
        SubmissionLibraryDescriptor descriptor = null;
        if (StringUtils.isNotBlank(dataType)) {
            switch (dataType) {
            case "Exome":
            case "Whole Exome":
            case "Whole Exome Sequencing":
                descriptor = WHOLE_EXOME;
                break;
            case "WGS":
            case "Genome":
            case "Whole Genome":
            case "Human Whole Genome":
                descriptor = WHOLE_GENOME;
                break;
            case "RNA":
            case "RNA Seq":
            case "RNA Sequencing":
                descriptor = RNA_SEQ;
                break;
            default:
                descriptor = new SubmissionLibraryDescriptor(dataType, dataType);
            }
        }
        if (descriptor != null) {
            return descriptor.getName();
        }
        return null;
    }
}
