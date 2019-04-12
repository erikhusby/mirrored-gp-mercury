
package org.broadinstitute.gpinformatics.infrastructure.submission;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import java.io.Serializable;

// setting the access order to alphabetical helps the tests pass more reliably.
@JsonPropertyOrder(alphabetic = true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SubmissionBioSampleBean implements Serializable {
    private static final long serialVersionUID = -8865203109332598495L;
    @JsonProperty
    private String sampleId;
    @JsonProperty
    private String processingLocation;
    @JsonProperty
    private SubmissionContactBean contact;

    @JsonIgnore
    public static final String GCP = "GCP";
    @JsonIgnore
    public static final String ON_PREM = "OnPrem";

    public SubmissionBioSampleBean() {
    }

    public SubmissionBioSampleBean(String sampleId, String processingLocation, SubmissionContactBean contact) {
        this.sampleId = sampleId;
        this.processingLocation = processingLocation;
        this.contact = contact;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getProcessingLocation() {
        return processingLocation;
    }

    public void setProcessingLocation(String processingLocation) {
        this.processingLocation = processingLocation;
    }

    public SubmissionContactBean getContact() {
        return contact;
    }

    public void setContact(SubmissionContactBean contact) {
        this.contact = contact;
    }

    @Override
    public boolean equals (Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || !OrmUtil.proxySafeIsInstance(other, SubmissionBioSampleBean.class)) {
            return false;
        }

        SubmissionBioSampleBean castOther = OrmUtil.proxySafeCast(other, SubmissionBioSampleBean.class);

        return new EqualsBuilder().append(getSampleId(), castOther.getSampleId())
                                  .append(getProcessingLocation(), castOther.getProcessingLocation())
                                  .append(getContact(), castOther.getContact()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getSampleId()).append(getProcessingLocation()).append(getContact()).toHashCode();
    }

}
