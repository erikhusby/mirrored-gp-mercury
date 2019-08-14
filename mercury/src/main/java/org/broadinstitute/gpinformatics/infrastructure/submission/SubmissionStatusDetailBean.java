package org.broadinstitute.gpinformatics.infrastructure.submission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * TODO scottmat fill in javadoc!!!
 */

// setting the access order to alphabetical helps the tests pass more reliably.
@JsonPropertyOrder(alphabetic = true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SubmissionStatusDetailBean implements Serializable {
    private static final long serialVersionUID = 6352810343445206054L;
    private static final String STATUS_FIELD = "status";
    @JsonProperty
    protected String uuid;
    @JsonProperty
    private Date lastStatusUpdate;
    @JsonProperty
    private String site;

    // The status webservice uses all lower case but the submitrequest uses camel case for this field
    @JsonProperty("submissiondatatype")
    private String submissionDatatype;
    @JsonProperty
    private String submittedVersion;
    @JsonProperty
    private Status status;
    @JsonProperty
    private List<String> errors = new ArrayList<>();
    @JsonProperty
    private BioProject bioproject;

    public SubmissionStatusDetailBean() {
    }

    public SubmissionStatusDetailBean(String uuid, Status status, String site, String submissionDatatype,
                                      Date lastStatusUpdate, String... errors) {
        this.uuid = uuid;
        this.site = site;
        this.submissionDatatype = submissionDatatype;
        this.lastStatusUpdate = lastStatusUpdate;
        this.status = status;
        setErrors(Arrays.asList(errors));
    }

    public SubmissionStatusDetailBean(String uuid, Status status, String... errors) {
        this(uuid, status, null, null, null, errors);
    }

    public SubmissionStatusDetailBean(String uuid,
                                      Status status, List<String> errors) {
        this(uuid, status, null, null, null, errors.toArray(new String[errors.size()]));
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

    public void setSubmissionDatatype(String submissiondatatype) {
        this.submissionDatatype = submissiondatatype;
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


    public void setSubmittedVersion(String submittedVersion) {
        this.submittedVersion = submittedVersion;
    }

    public String getSubmittedVersion() {
        return submittedVersion;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @JsonIgnore
    public String getStatusString() {
        return (status != null) ? status.getLabel() : null;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    public BioProject getBioproject() {
        return bioproject;
    }

    public void setBioproject(BioProject bioproject) {
        this.bioproject = bioproject;
    }

    @JsonIgnore
    public boolean submissionServiceHasRequest() {
        return getUuid() != null && getStatus() != null && getBioproject() != null && getLastStatusUpdate() != null
               && getSite() != null && getSubmissionDatatype() != null;
    }

    /**
     * TODO scottmat fill in javadoc!!!
     */
    public enum Status {
        IN_TRANSIT("InTransit", "In Transit"),
        SUBMITTED("Submitted", "Submitted"),
        FAILURE("Failure", "Failure"),
        READY_FOR_SUBMISSION("ReadyForSubmission", "Ready For Submission"),
        VALIDATED("Validated", "Validated"),
        PROCESSING("Processing", "Processing"),
        OTHER("Other", "Unknown Status");

        Status(String key, String label) {
            this.key = key;
            this.label = label;
        }

        private String key;
        private String label;

        public String getKey() {
            return key;
        }

        @JsonValue
        public String getLabel() {
            return label;
        }

        @JsonCreator
        public static Status fromKey(String status) {
            for (Status testValue : values()) {
                if (testValue.getKey().equals(status)) {
                    return testValue;
                }
            }
            return OTHER;
        }
    }
}
