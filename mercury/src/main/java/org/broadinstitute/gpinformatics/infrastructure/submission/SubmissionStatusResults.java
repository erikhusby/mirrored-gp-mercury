package org.broadinstitute.gpinformatics.infrastructure.submission;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * TODO scottmat fill in javadoc!!!
 */
@XmlRootElement()
public class SubmissionStatusResults implements Serializable {
    private SubmissionStatusDetails[] submissionStatuses;

    public SubmissionStatusDetails[] getSubmissionStatuses ()
    {
        return submissionStatuses;
    }

    @XmlElement
    public void setSubmissionStatuses (SubmissionStatusDetails... submissionStatuses)
    {
        this.submissionStatuses = submissionStatuses;
    }
}
