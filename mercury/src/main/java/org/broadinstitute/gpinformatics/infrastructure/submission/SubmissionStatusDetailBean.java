package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class SubmissionStatusDetailBean implements Serializable {

    private static final long serialVersionUID = 6352810343445206054L;
    private String uuid;
    private Status status;
    private List<String> errors=new ArrayList<>();
    private Date lastStatusUpdate;
    private BioProject bioproject;

    public SubmissionStatusDetailBean() {
    }


    public SubmissionStatusDetailBean(String uuid, String status, Date lastStatusUpdate, String... errors) {
        this.uuid = uuid;
        this.lastStatusUpdate = lastStatusUpdate;
        setStatus(status);
        setErrors(Arrays.asList(errors));
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
        return (status != null)?status.getLabel():null;
    }

    @XmlElement
    public void setStatus(String status) {
        this.status = Status.fromKey(status);
    }

    public List<String> getErrors() {
        return errors;
    }

    @XmlElement
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }


    public Date getLastStatusUpdate() {
        return lastStatusUpdate;
    }

    @XmlElement
    public void setLastStatusUpdate(Date lastStatusUpdate) {
        this.lastStatusUpdate = lastStatusUpdate;
    }

    public BioProject getBioproject() {
        return bioproject;
    }

    @XmlElement
    public void setBioproject(BioProject bioproject) {
        this.bioproject = bioproject;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || !OrmUtil.proxySafeIsInstance(other, SubmissionStatusDetailBean.class)) {
            return false;
        }

        SubmissionStatusDetailBean castOther = OrmUtil.proxySafeCast(other, SubmissionStatusDetailBean.class);
        return new EqualsBuilder().append(getUuid(), castOther.getUuid()).append(getStatus(), castOther.getStatus())
                                  .append(getErrors(), castOther.getErrors())
                                  .append(getLastStatusUpdate().getTime(), castOther.getLastStatusUpdate().getTime())
                                  .append(getBioproject(), castOther.getBioproject()).isEquals();
    }



    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getUuid()).append(getStatus()).append(getErrors())
                                    .append(getLastStatusUpdate()).append(getBioproject()).toHashCode();
    }
    /**
     * TODO scottmat fill in javadoc!!!
     */
    public static enum Status {
        IN_TRANSIT("InTransit", "In Transit"),
        SUBMITTED("Submitted", "Submitted"),
        FAILURE("Failure", "Failure"),
        READY_FOR_SUBMISSION("ReadyForSubmission", "Ready For Submission"),
        PROCESSING("Processing", "Processing");

        Status(String key, String label) {
            this.key = key;
            this.label = label;
        }

        private String key;
        private String label;

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }

        public static Status fromKey(String status) {
            for(Status testValue:values()) {
                if(testValue.getKey().equals(status)) {
                    return testValue;
                }
            }
            throw new RuntimeException("Unable to find a matching Status");
        }
    }
}
