
package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public class SubmissionBioSampleBean implements Serializable {

    private static final long serialVersionUID = -8865203109332598495L;
    @XmlElement
    private String sampleId;
    @XmlElement
    private String processingLocation;
    @XmlElement
    private SubmissionContactBean contact;

    @XmlTransient
    public static final String GCP = "GCP";
    @XmlTransient
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
