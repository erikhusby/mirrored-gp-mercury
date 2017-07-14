package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * TODO scottmat fill in javadoc!!!
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
public class SubmissionStatusDetailBean implements Serializable {
    private static final long serialVersionUID = 6352810343445206054L;

    @JsonProperty
    private String uuid;
    @JsonProperty
    private Status status;
    @JsonProperty
    private List<String> errors=new ArrayList<>();
    @JsonProperty
    private Date lastStatusUpdate;
    @JsonProperty
    private BioProject bioProject;
    @JsonProperty
    private String site;
    @JsonProperty
    private String submissionDatatype;
    @JsonProperty
    private String submittedVersion;

    public SubmissionStatusDetailBean() {
    }

    public SubmissionStatusDetailBean(String uuid, String status, String site, String submissionDatatype,
                                      Date lastStatusUpdate, String... errors) {
        this.uuid = uuid;
        this.site = site;
        this.submissionDatatype = submissionDatatype;
        this.lastStatusUpdate = lastStatusUpdate;
        setStatus(status);
        setErrors(Arrays.asList(errors));
    }

    public SubmissionStatusDetailBean(String uuid,
                                      String status, List<String> errors) {
        this(uuid, status, null, null, null, errors.toArray(new String[errors.size()]));
    }

    //{"submissionStatuses":[{
    // "uuid":"MERCURY_TEST_SUB_1499710803691_0001",
    // "status":"Failure",
    // "errors":["Invalid V2","Unable to access bam path for PRJNA75723 4304714212_K RP-418 V2 located GCP."]},{"uuid":"MERCURY_TEST_SUB_1499710803692_0002","status":"Failure","errors":["Invalid V2","Unable to access bam path for PRJNA75723 4377315018_E RP-418 V2 located OnPrem."]}]}
    public String getUuid ()
    {
        return uuid;
    }

    public void setUuid (String uuid)
    {
        this.uuid = uuid;
    }

    public String getStatus() {
        return (status != null)?status.getLabel():null;
    }

    public void setStatus(String status) {
        this.status = Status.fromKey(status);
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public Date getLastStatusUpdate() {
        return lastStatusUpdate;
    }

    public void setLastStatusUpdate(Date lastStatusUpdate) {
        this.lastStatusUpdate = lastStatusUpdate;
    }

    public BioProject getBioProject() {
        return bioProject;
    }

    public void setBioProject(BioProject bioProject) {
        this.bioProject = bioProject;
    }

    public String getSite() {
        return site;
    }



    public void setSite(String site) {
        this.site = site;
    }


    public String getSubmissionDatatype() {
        return submissionDatatype;
    }

    public void setSubmissionDatatype(String submissionDatatype) {
        this.submissionDatatype = submissionDatatype;
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
                                  .append(getBioProject(), castOther.getBioProject()).isEquals();
    }



    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getUuid()).append(getStatus()).append(getErrors())
                                    .append(getLastStatusUpdate()).append(getBioProject()).toHashCode();
    }

    @JsonIgnore
    public void setSubmittedVersion(String submittedVersion) {
        this.submittedVersion = submittedVersion;
    }

    public String getSubmittedVersion() {
        return submittedVersion;
    }

    /**
     * TODO scottmat fill in javadoc!!!
     */
    public static enum Status {
        IN_TRANSIT("InTransit", "In Transit"),
        SUBMITTED("Submitted", "Submitted"),
        FAILURE("Failure", "Failure"),
        READY_FOR_SUBMISSION("ReadyForSubmission", "Ready For Submission"),
        VALIDATED("Validated", "Validated"),
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
