
package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SubmissionBioSampleBean implements Serializable {

    private String sampleId;
    private String filePath;
    private SubmissionContactBean contact;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

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
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
