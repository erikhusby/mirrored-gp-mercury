package org.broadinstitute.gpinformatics.infrastructure.submission;

import java.io.Serializable;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class SubmissionStatusResults implements Serializable {
    private SubmissionStatusDetails[] submissionStatuses;

    public SubmissionStatusDetails[] getSubmissionStatuses ()
    {
        return submissionStatuses;
    }

    public void setSubmissionStatuses (SubmissionStatusDetails... submissionStatuses)
    {
        this.submissionStatuses = submissionStatuses;
    }
}
