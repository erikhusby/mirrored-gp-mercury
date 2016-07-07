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

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

public class SubmissionLibraryDescriptors {
    private List<SubmissionLibraryDescriptor> submissionLibraryDescriptors;

    public List<SubmissionLibraryDescriptor> getSubmissionLibraryDescriptors() {
        return submissionLibraryDescriptors;
    }

    @JsonProperty("submissiondatatypes")
    public void setSubmissionLibraryDescriptors(List<SubmissionLibraryDescriptor> submissionLibraryDescriptors) {
        this.submissionLibraryDescriptors = submissionLibraryDescriptors;
    }
}
