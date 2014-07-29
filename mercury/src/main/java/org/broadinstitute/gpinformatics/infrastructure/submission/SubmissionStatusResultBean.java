package org.broadinstitute.gpinformatics.infrastructure.submission;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * TODO scottmat fill in javadoc!!!
 */
@XmlRootElement()
public class SubmissionStatusResultBean implements Serializable {
    private SubmissionStatusDetailBean[] submissionStatuses;

    public SubmissionStatusDetailBean[] getSubmissionStatuses ()
    {
        return submissionStatuses;
    }

    @XmlElement
    public void setSubmissionStatuses (SubmissionStatusDetailBean... submissionStatuses)
    {
        this.submissionStatuses = submissionStatuses;
    }
}
