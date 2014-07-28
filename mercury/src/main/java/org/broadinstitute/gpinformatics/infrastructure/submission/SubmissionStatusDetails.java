package org.broadinstitute.gpinformatics.infrastructure.submission;

import java.io.Serializable;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class SubmissionStatusDetails implements Serializable {

    private String uuid;
    private Status status;
    private String[] errors;

    public SubmissionStatusDetails() {
    }

    public String getUuid ()
    {
        return uuid;
    }

    public void setUuid (String uuid)
    {
        this.uuid = uuid;
    }

    public String getStatus() {
        return (status != null)?status.getDescription():null;
    }

    public void setStatus(String status) {
        this.status = Status.fromDescription( status);
    }

    public String[] getErrors() {
        return errors;
    }

    public void setErrors(String... errors) {
        this.errors = errors;
    }

    /**
     * TODO scottmat fill in javadoc!!!
     */
    public static enum Status {
        INPROGRESS("InProgress");

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
