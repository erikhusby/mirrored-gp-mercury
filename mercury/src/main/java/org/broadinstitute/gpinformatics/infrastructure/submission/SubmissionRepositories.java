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

public class SubmissionRepositories {
    private List<SubmissionRepository> submissionRepositories;

    public List<SubmissionRepository> getSubmissionRepositories() {
        return submissionRepositories;
    }

    @JsonProperty("sites")
    public void setSubmissionRepositories(List<SubmissionRepository> submissionRepositories) {
        this.submissionRepositories = submissionRepositories;
    }
}
