package org.broadinstitute.gpinformatics.infrastructure.submission;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.util.Date;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class SubmissionStatusDetailBean implements Serializable {

    private String uuid;
    private Status status;
    private String[] errors;
    private Date lastStatusUpdate;

    public SubmissionStatusDetailBean() {
    }

    public SubmissionStatusDetailBean(String uuid, String status) {
        this.uuid = uuid;
        setStatus(status);
    }

    public SubmissionStatusDetailBean(String uuid, String status, String... errors) {
        this(uuid, status);
        setErrors(errors);
    }

    public String getUuid ()
    {
        return uuid;
    }

    @XmlElement
    public void setUuid (String uuid)
    {
        this.uuid = uuid;
    }

    public String getStatus() {
        return (status != null)?status.getDescription():null;
    }

    @XmlElement
    public void setStatus(String status) {
        this.status = Status.fromDescription( status);
    }

    public String[] getErrors() {
        return errors;
    }

    @XmlElement
    public void setErrors(String... errors) {
        this.errors = errors;
    }


    public Date getLastStatusUpdate() {
        return lastStatusUpdate;
    }

    @XmlElement
    public void setLastStatusUpdate(Date lastStatusUpdate) {
        this.lastStatusUpdate = lastStatusUpdate;
    }

    /**
     * TODO scottmat fill in javadoc!!!
     */
    public static enum Status {
        IN_TRANSIT("InTransit"),
        SUBMITTED("Submitted"),
        FAILURE("Failure"),
        READY_FOR_SUBMISSION("ReadyForSubmission"),
        PROCESSING("Processing");

        Status(String description) {
            this.description = description;
        }

        private String description;

        public String getDescription() {
            return description;
        }

        public static Status fromDescription(String status) {
            for(Status testValue:values()) {
                if(testValue.getDescription().equals(status)) {
                    return testValue;
                }
            }
            throw new RuntimeException("Unable to find a matching Status");
        }
    }
}
