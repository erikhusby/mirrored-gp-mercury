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

import java.util.List;

// setting the access order to alphabetical helps the tests pass more reliably.
@JsonPropertyOrder(alphabetic = true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SubmissionLibraryDescriptors {
    @JsonProperty("submissiondatatypes")
    private List<SubmissionLibraryDescriptor> submissionLibraryDescriptors;

    public List<SubmissionLibraryDescriptor> getSubmissionLibraryDescriptors() {
        return submissionLibraryDescriptors;
    }

    public void setSubmissionLibraryDescriptors(List<SubmissionLibraryDescriptor> submissionLibraryDescriptors) {
        this.submissionLibraryDescriptors = submissionLibraryDescriptors;
    }
}
