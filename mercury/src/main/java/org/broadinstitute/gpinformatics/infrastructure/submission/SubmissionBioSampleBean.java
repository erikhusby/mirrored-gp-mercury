
package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SubmissionBioSampleBean implements Serializable {

    private static final long serialVersionUID = -8865203109332598495L;
    private String sampleId;
    private String filePath;
    private SubmissionContactBean contact;

    public SubmissionBioSampleBean() {
    }

    public SubmissionBioSampleBean(String sampleId, String filePath) {
        this.sampleId = sampleId;
        this.filePath = filePath;
    }

    public SubmissionBioSampleBean(String sampleId, String filePath,
                                   SubmissionContactBean contact) {
        this(sampleId, filePath);
        this.contact = contact;
    }

    public String getSampleId() {
        return sampleId;
    }

    @XmlElement
    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getFilePath() {
        return filePath;
    }

    @XmlElement
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public SubmissionContactBean getContact() {
        return contact;
    }

    @XmlElement
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
                                  .append(getFilePath(), castOther.getFilePath())
                                  .append(getContact(), castOther.getContact()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getSampleId()).append(getFilePath()).append(getContact()).toHashCode();
    }

}
